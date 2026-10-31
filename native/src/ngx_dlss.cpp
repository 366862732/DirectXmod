// NGX / DLSS 接入层实现。
//
// 这里只负责：读取 App ID/Project ID → 初始化 NGX → 查询能力位 → 写日志。
// 真正的 DLSS 超分需要额外提供 深度、运动矢量、jitter 以及 HUDless 颜色，
// 那些输入本模组目前还没有产出，所以即使初始化成功也不会改变渲染结果。

#include "ngx_dlss.h"

#include <windows.h>
#include <d3d12.h>

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <string>

#include "nvsdk_ngx.h"
#include "nvsdk_ngx_defs.h"
#include "nvsdk_ngx_defs_dlssg.h"

#include "dx12_device.h"
#include "ngx_dlss_render.h"

using namespace dx12mc;

namespace {

struct NgxConfig {
    std::string appId;
    std::string projectId;
    std::string appDataPath;
};

NgxConfig gCfg;
NgxState gState;
ID3D12Device* gDevice = nullptr;

std::wstring widen(const std::string& s) {
    if (s.empty()) {
        return std::wstring();
    }
    int n = MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int)s.size(), nullptr, 0);
    if (n <= 0) {
        return std::wstring();
    }
    std::wstring w((size_t)n, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int)s.size(), w.data(), n);
    return w;
}

// NGX 需要一个可写目录放日志与 nvngx_*.dll；配置为空时用进程当前目录。
std::wstring resolveAppDataPath() {
    if (!gCfg.appDataPath.empty()) {
        return widen(gCfg.appDataPath);
    }
    wchar_t buf[MAX_PATH] = {};
    DWORD n = GetCurrentDirectoryW(MAX_PATH, buf);
    if (n == 0 || n >= MAX_PATH) {
        return L".";
    }
    return std::wstring(buf, n);
}

void setMessage(const std::string& msg) {
    gState.message = msg;
    dbgLog("ngx: %s", msg.c_str());
}

bool parseAppId(const std::string& text, unsigned long long& out) {
    if (text.empty()) {
        return false;
    }
    char* end = nullptr;
    errno = 0;
    unsigned long long v = std::strtoull(text.c_str(), &end, 10);
    if (errno != 0 || end == text.c_str() || *end != '\0') {
        return false;
    }
    out = v;
    // 0 是 NGX 文档允许的开发期占位值（"Until NVIDIA has assigned you an
    // application ID, use 0"），因此不当作解析失败。
    return true;
}

// NGX 只做"形似 GUID"的校验，格式错了会统一返回 InvalidParameter；
// 在这里先拦一次，错误信息才说得清是哪个值不对。
bool isGuidLike(const std::string& text) {
    if (text.size() != 36) {
        return false;
    }
    for (size_t i = 0; i < text.size(); ++i) {
        const char c = text[i];
        const bool separator = (i == 8 || i == 13 || i == 18 || i == 23);
        if (separator ? c != '-' : !std::isxdigit(static_cast<unsigned char>(c))) {
            return false;
        }
    }
    return true;
}

// 自定义引擎（NVSDK_NGX_ENGINE_TYPE_CUSTOM）的版本号，随项目随意填写。
constexpr const char* kEngineVersion = "1.0";

// 内置 Project ID：没有 NVIDIA 分配的 App ID 时用它走 ProjectID + CUSTOM 引擎，
// 用户无需在 config/gl4dx12.properties 里手写 ngx_project_id。
// 配置里显式填了 ngx_project_id 时仍以配置值为准。
constexpr const char* kDefaultProjectId = "123e4567-e89b-12d3-a456-426614174000";

void tryInit() {
    if (gState.initialized || !gDevice) {
        return;
    }

    gState.configured = true;
    const std::wstring dataPath = resolveAppDataPath();
    NVSDK_NGX_Result result = NVSDK_NGX_Result_FAIL_InvalidParameter;
    std::string mode = "ProjectID";

    // NGX 默认只在进程目录里找 nvngx_dlss.dll；把数据目录（模组的 dx12mod/）
    // 一并加入搜索路径，这样 DLSS 运行库可以跟 dx12_mc.dll 放在一起。
    const wchar_t* searchPaths[1] = {dataPath.c_str()};
    NVSDK_NGX_FeatureCommonInfo featureInfo{};
    featureInfo.PathListInfo.Path = searchPaths;
    featureInfo.PathListInfo.Length = 1;

    unsigned long long appId = 0;
    const bool appIdParsed = parseAppId(gCfg.appId, appId);
    // 0 只是文档允许的开发期占位值，实测拿不到 DLSS 超分能力；没有 NVIDIA
    // 分配的真实 App ID 时改走 ProjectID + CUSTOM 引擎（无需审核）。
    if (appIdParsed && appId != 0) {
        mode = "AppID";
        result = NVSDK_NGX_D3D12_Init(appId, dataPath.c_str(), gDevice, &featureInfo);
    } else {
        // 没有 NVIDIA 分配的 App ID 时走 ProjectID + CUSTOM 引擎（无需审核）。
        // 未配置时使用内置 GUID，用户不必手写 ngx_project_id。
        const std::string projectId =
            gCfg.projectId.empty() ? kDefaultProjectId : gCfg.projectId;
        if (!isGuidLike(projectId)) {
            setMessage("NGX Project ID 格式无效：应为 GUID 形式"
                       "（如 a0f57b54-1daf-4934-90ae-c4035c19df04），当前值："
                       + projectId);
            return;
        }
        result = NVSDK_NGX_D3D12_Init_with_ProjectID(projectId.c_str(),
            NVSDK_NGX_ENGINE_TYPE_CUSTOM, kEngineVersion,
            dataPath.c_str(), gDevice, &featureInfo);
    }

    if (NVSDK_NGX_FAILED(result)) {
        char buf[192];
        std::snprintf(buf, sizeof(buf),
            "NGX 初始化失败（%s，result=0x%08x）。常见原因：Project ID 未被驱动接受、"
            "驱动不支持 NVNGX，或 nvngx_dlss.dll 不在工作目录",
            mode.c_str(), (unsigned)result);
        setMessage(buf);
        return;
    }

    gState.initialized = true;

    NVSDK_NGX_Parameter* params = nullptr;
    if (!NVSDK_NGX_FAILED(NVSDK_NGX_D3D12_GetCapabilityParameters(&params)) && params) {
        int available = 0;
        if (!NVSDK_NGX_FAILED(params->Get(NVSDK_NGX_Parameter_SuperSampling_Available, &available))) {
            gState.dlssAvailable = available != 0;
        }
        available = 0;
        if (!NVSDK_NGX_FAILED(params->Get(NVSDK_NGX_Parameter_FrameGeneration_Available, &available))) {
            gState.frameGenAvailable = available != 0;
        }
        NVSDK_NGX_D3D12_DestroyParameters(params);
    }

    char buf[192];
    std::snprintf(buf, sizeof(buf),
        "NGX 初始化成功（%s）。DLSS 超分可用=%s，DLSS 帧生成可用=%s",
        mode.c_str(), gState.dlssAvailable ? "是" : "否",
        gState.frameGenAvailable ? "是" : "否");
    setMessage(buf);
}

}  // namespace

void ngxSetConfig(const std::string& appId, const std::string& projectId,
                  const std::string& appDataPath) {
    gCfg.appId = appId;
    gCfg.projectId = projectId;
    gCfg.appDataPath = appDataPath;
    // 设备已创建时立刻重试：配置是在设备创建之后由 Java 侧推送的。
    tryInit();
}

void ngxInitForDevice(void* d3d12Device) {
    gDevice = reinterpret_cast<ID3D12Device*>(d3d12Device);
    tryInit();
}

void ngxShutdown() {
    if (gState.initialized) {
        // 先释放 SR/FG feature 并等待 GPU 空闲，再关闭 NGX。
        ngxRenderShutdown();
        NVSDK_NGX_Result r = NVSDK_NGX_D3D12_Shutdown1(gDevice);
        dbgLog("ngx: shutdown result=0x%08x", (unsigned)r);
    }
    gState = NgxState{};
    gDevice = nullptr;
}

bool ngxIsInitialized() {
    return gState.initialized;
}

void ngxSetSrAvailable(bool available) {
    if (gState.dlssAvailable == available) {
        return;
    }
    gState.dlssAvailable = available;
    if (available) {
        setMessage("DLSS 超分实测可用（CreateFeature 成功；"
                   "初始化阶段的能力位曾报不可用，已按实测结果覆盖）");
    }
}

NgxState ngxGetState() {
    return gState;
}
