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

void tryInit() {
    if (gState.initialized || !gDevice) {
        return;
    }
    if (gCfg.appId.empty() && gCfg.projectId.empty()) {
        gState.configured = false;
        // 设备创建与配置推送各触发一次初始化，避免把同一条提示写两遍。
        if (gState.message.empty()) {
            setMessage("未配置 NGX App ID / Project ID，DLSS 通道保持关闭"
                       "（在 config/gl4dx12.properties 填写 ngx_app_id 后重启生效）");
        }
        return;
    }

    gState.configured = true;
    const std::wstring dataPath = resolveAppDataPath();
    NVSDK_NGX_Result result = NVSDK_NGX_Result_FAIL_InvalidParameter;
    std::string mode = "AppID";

    // NGX 默认只在进程目录里找 nvngx_dlss.dll；把数据目录（模组的 dx12mod/）
    // 一并加入搜索路径，这样 DLSS 运行库可以跟 dx12_mc.dll 放在一起。
    const wchar_t* searchPaths[1] = {dataPath.c_str()};
    NVSDK_NGX_FeatureCommonInfo featureInfo{};
    featureInfo.PathListInfo.Path = searchPaths;
    featureInfo.PathListInfo.Length = 1;

    if (!gCfg.appId.empty()) {
        unsigned long long appId = 0;
        if (!parseAppId(gCfg.appId, appId)) {
            // nvapi- 开头的是 NVIDIA build/API 密钥，NGX 不接受。
            const bool nvapiKey = gCfg.appId.rfind("nvapi-", 0) == 0;
            setMessage("NGX App ID 解析失败：应为 NVIDIA 分配的十进制数字（开发期可填 0）"
                       + std::string(nvapiKey
                             ? "；当前填的是 nvapi- 开头的 NVIDIA API 密钥，不是 NGX App ID"
                             : "；当前值：" + gCfg.appId));
            return;
        }
        result = NVSDK_NGX_D3D12_Init(appId, dataPath.c_str(), gDevice, &featureInfo);
    } else {
        mode = "ProjectID";
        // 没有 NVIDIA 分配的 App ID 时走这条路：自己生成一个 GUID 当项目标识，
        // 引擎类型固定 CUSTOM（NGX 会校验 GUID 格式）。
        if (!isGuidLike(gCfg.projectId)) {
            setMessage("NGX Project ID 格式无效：应为 GUID 形式"
                       "（如 a0f57b54-1daf-4934-90ae-c4035c19df04），当前值："
                       + gCfg.projectId);
            return;
        }
        result = NVSDK_NGX_D3D12_Init_with_ProjectID(gCfg.projectId.c_str(),
            NVSDK_NGX_ENGINE_TYPE_CUSTOM, kEngineVersion,
            dataPath.c_str(), gDevice, &featureInfo);
    }

    if (NVSDK_NGX_FAILED(result)) {
        char buf[160];
        std::snprintf(buf, sizeof(buf),
            "NGX 初始化失败（%s，result=0x%08x）。常见原因：App ID 未生效、"
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

NgxState ngxGetState() {
    return gState;
}
