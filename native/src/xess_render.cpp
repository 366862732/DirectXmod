// Intel XeSS 超分（XeSS-SR）实现。
//
// 资源与数据流：
//   1. Java 侧每帧在世界渲染完成后调用统一的捕获入口（ngx_dlss_render.cpp），
//      生成渲染分辨率的深度与相机运动矢量。
//   2. 最终 blit 前调用 xessEvaluateSr：以低分辨率主颜色 + 深度 + 运动矢量
//      调用 xessD3D12Execute，输出全分辨率纹理；blit 改用它作为源。
//   3. 输出纹理按命令列表槽位（3 个）复用，避免与 3 帧飞行冲突。
//
// 注意：XeSS 只做超分，不做帧生成。帧生成仍由 DLSS-G 或自研插值负责。

#include "xess_render.h"

#include <windows.h>

#include <algorithm>
#include <atomic>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <string>
#include <type_traits>

#include "xess.h"
#include "xess_d3d12.h"

#include "dx12_device.h"
#include "ngx_dlss_render.h"

namespace dx12mc {
namespace {

// ---------------------------------------------------------------------------
// 动态加载的 XeSS API
// ---------------------------------------------------------------------------

struct XessApi {
    HMODULE module = nullptr;
    bool loadTried = false;
    bool loadFailed = false;

    decltype(&xessGetVersion) getVersion = nullptr;
    decltype(&xessD3D12CreateContext) createContext = nullptr;
    decltype(&xessD3D12Init) init = nullptr;
    decltype(&xessD3D12Execute) execute = nullptr;
    decltype(&xessDestroyContext) destroyContext = nullptr;
    decltype(&xessGetInputResolution) getInputResolution = nullptr;
    decltype(&xessSetLoggingCallback) setLoggingCallback = nullptr;
    decltype(&xessIsOptimalDriver) isOptimalDriver = nullptr;
};

XessApi gApi;

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------

XessConfig gCfg;
std::string gStatus = "XeSS: 未初始化";

xess_context_handle_t gContext = nullptr;
bool gContextCreateFailed = false;
bool gInitialized = false;
UINT gOutW = 0, gOutH = 0;
int gInitQuality = -1;
bool gNeedsReset = true;

struct OutputTexture {
    ComPtr<ID3D12Resource> tex;
    UINT w = 0, h = 0;
    DXGI_FORMAT fmt = DXGI_FORMAT_UNKNOWN;
};

OutputTexture gOutputs[3];

std::mutex gMtx;

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

std::wstring moduleDirectory() {
    HMODULE mod = nullptr;
    if (!GetModuleHandleExW(
            GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
            reinterpret_cast<LPCWSTR>(&moduleDirectory), &mod)) {
        return std::wstring();
    }
    wchar_t path[MAX_PATH] = {};
    const DWORD n = GetModuleFileNameW(mod, path, MAX_PATH);
    if (n == 0 || n >= MAX_PATH) return std::wstring();
    std::wstring full(path, n);
    const size_t slash = full.find_last_of(L"\\/");
    return slash == std::wstring::npos ? std::wstring() : full.substr(0, slash);
}

void logCallback(const char* message, xess_logging_level_t level) {
    if (!message) return;
    if (level >= XESS_LOGGING_LEVEL_WARNING) {
        dbgLog("XeSS[%d]: %s", (int)level, message);
    } else {
        dbgLogDebug("XeSS[%d]: %s", (int)level, message);
    }
}

std::string resultText(xess_result_t r) {
    switch (r) {
        case XESS_RESULT_SUCCESS: return "success";
        case XESS_RESULT_WARNING_NONEXISTING_FOLDER: return "warning: nonexisting folder";
        case XESS_RESULT_WARNING_OLD_DRIVER: return "warning: old driver";
        case XESS_RESULT_ERROR_UNSUPPORTED_DEVICE: return "unsupported device (SM 6.4 required)";
        case XESS_RESULT_ERROR_UNSUPPORTED_DRIVER: return "unsupported driver";
        case XESS_RESULT_ERROR_UNINITIALIZED: return "uninitialized";
        case XESS_RESULT_ERROR_INVALID_ARGUMENT: return "invalid argument";
        case XESS_RESULT_ERROR_DEVICE_OUT_OF_MEMORY: return "device out of memory";
        case XESS_RESULT_ERROR_DEVICE: return "device error";
        case XESS_RESULT_ERROR_NOT_IMPLEMENTED: return "not implemented";
        case XESS_RESULT_ERROR_INVALID_CONTEXT: return "invalid context";
        case XESS_RESULT_ERROR_OPERATION_IN_PROGRESS: return "operation in progress";
        case XESS_RESULT_ERROR_UNSUPPORTED: return "unsupported";
        case XESS_RESULT_ERROR_CANT_LOAD_LIBRARY: return "cannot load library";
        case XESS_RESULT_ERROR_WRONG_CALL_ORDER: return "wrong call order";
        default: {
            char buf[32];
            std::snprintf(buf, sizeof(buf), "error %d", (int)r);
            return buf;
        }
    }
}

xess_quality_settings_t mapQuality(int q) {
    switch (q) {
        case 0: return XESS_QUALITY_SETTING_AA;
        case 2: return XESS_QUALITY_SETTING_BALANCED;
        case 3: return XESS_QUALITY_SETTING_PERFORMANCE;
        case 4: return XESS_QUALITY_SETTING_ULTRA_PERFORMANCE;
        default: return XESS_QUALITY_SETTING_QUALITY;
    }
}

bool ensureLibrary() {
    std::lock_guard<std::mutex> lk(gMtx);
    if (gApi.module) return true;
    if (gApi.loadTried) return false;
    gApi.loadTried = true;

    const std::wstring dir = moduleDirectory();
    const std::wstring local = dir.empty() ? std::wstring(L"libxess.dll")
                                           : dir + L"\\libxess.dll";
    HMODULE module = LoadLibraryW(local.c_str());
    if (!module) {
        // 回退到系统搜索路径（PATH / 进程目录）。
        module = LoadLibraryW(L"libxess.dll");
    }
    if (!module) {
        gApi.loadFailed = true;
        gStatus = "XeSS: 未找到 libxess.dll（放到 dx12mod/ 后重启）";
        dbgLog("XeSS: LoadLibrary failed (err=%lu)", GetLastError());
        return false;
    }

    auto resolve = [&](auto& fn, const char* name) {
        fn = reinterpret_cast<std::remove_reference_t<decltype(fn)>>(
            GetProcAddress(module, name));
        return fn != nullptr;
    };

    bool ok = true;
    ok = resolve(gApi.getVersion, "xessGetVersion") && ok;
    ok = resolve(gApi.createContext, "xessD3D12CreateContext") && ok;
    ok = resolve(gApi.init, "xessD3D12Init") && ok;
    ok = resolve(gApi.execute, "xessD3D12Execute") && ok;
    ok = resolve(gApi.destroyContext, "xessDestroyContext") && ok;
    ok = resolve(gApi.getInputResolution, "xessGetInputResolution") && ok;
    // 可选接口：缺失不影响超分。
    resolve(gApi.setLoggingCallback, "xessSetLoggingCallback");
    resolve(gApi.isOptimalDriver, "xessIsOptimalDriver");

    if (!ok) {
        FreeLibrary(module);
        gApi.module = nullptr;
        gApi.loadFailed = true;
        gStatus = "XeSS: libxess.dll 缺少必需导出";
        dbgLog("XeSS: libxess.dll missing required exports");
        return false;
    }

    gApi.module = module;
    gStatus = "XeSS: libxess.dll 已加载";
    dbgLog("XeSS: libxess.dll loaded from %ls", local.c_str());
    return true;
}

ID3D12Resource* createTex2D(DXGI_FORMAT fmt, UINT w, UINT h, std::string& err) {
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device) {
        err = "XeSS: device not initialized";
        return nullptr;
    }
    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = std::max(1u, w);
    desc.Height = std::max(1u, h);
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = fmt;
    desc.SampleDesc.Count = 1;
    desc.Flags = D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;

    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_DEFAULT;

    ID3D12Resource* res = nullptr;
    HRESULT hr = dc.device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE,
        &desc, D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(&res));
    if (FAILED(hr)) {
        char buf[32];
        std::snprintf(buf, sizeof(buf), "0x%08x", (unsigned)hr);
        err = std::string("XeSS: CreateCommittedResource failed ") + buf;
        return nullptr;
    }
    return res;
}

void releaseOutputs() {
    for (auto& o : gOutputs) {
        o.tex.Reset();
        o.w = o.h = 0;
        o.fmt = DXGI_FORMAT_UNKNOWN;
    }
}

bool ensureOutputs(DXGI_FORMAT fmt, UINT w, UINT h, std::string& err) {
    bool needCreate = false;
    for (const auto& o : gOutputs) {
        if (!o.tex || o.w != w || o.h != h || o.fmt != fmt) {
            needCreate = true;
            break;
        }
    }
    if (!needCreate) return true;

    // 旧纹理可能仍被前几帧的 GPU 命令引用，重建前必须等 GPU 空闲。
    std::string waitErr;
    if (!deviceWaitIdle(waitErr)) {
        err = "XeSS: deviceWaitIdle before output recreate failed: " + waitErr;
        return false;
    }
    releaseOutputs();
    for (auto& o : gOutputs) {
        o.tex.Attach(createTex2D(fmt, w, h, err));
        if (!o.tex) return false;
        o.w = w;
        o.h = h;
        o.fmt = fmt;
    }
    dbgLog("XeSS: output textures created %ux%u fmt=%d", w, h, (int)fmt);
    return true;
}

void destroyContextLocked() {
    if (gContext && gApi.destroyContext) {
        gApi.destroyContext(gContext);
    }
    gContext = nullptr;
    gInitialized = false;
    gOutW = gOutH = 0;
    gInitQuality = -1;
}

// 确保 context 已按当前输出尺寸/质量档初始化。
bool ensureContext(UINT outW, UINT outH, std::string& err) {
    std::lock_guard<std::mutex> lk(gMtx);
    if (gContextCreateFailed) {
        err = gStatus;
        return false;
    }
    if (gContext && gInitialized && gOutW == outW && gOutH == outH
        && gInitQuality == gCfg.quality) {
        return true;
    }

    DeviceContext& dc = deviceContextForJni();
    if (!dc.device) {
        err = "XeSS: device not initialized";
        return false;
    }

    // 重新初始化前必须保证没有仍在执行的 XeSS 命令。
    if (gContext) {
        std::string waitErr;
        if (!deviceWaitIdle(waitErr)) {
            err = "XeSS: deviceWaitIdle before re-init failed: " + waitErr;
            return false;
        }
        destroyContextLocked();
    }

    if (!gContext) {
        xess_context_handle_t ctx = nullptr;
        xess_result_t r = gApi.createContext(dc.device.Get(), &ctx);
        if (r != XESS_RESULT_SUCCESS || !ctx) {
            gContextCreateFailed = true;
            gStatus = std::string("XeSS: 创建 context 失败（")
                + resultText(r) + "）";
            dbgLog("XeSS: xessD3D12CreateContext failed: %s", resultText(r).c_str());
            err = gStatus;
            return false;
        }
        gContext = ctx;
        if (gApi.setLoggingCallback) {
            gApi.setLoggingCallback(gContext, XESS_LOGGING_LEVEL_WARNING, logCallback);
        }
        if (gApi.isOptimalDriver && gApi.isOptimalDriver(gContext)
                == XESS_RESULT_WARNING_OLD_DRIVER) {
            dbgLog("XeSS: driver may be older than recommended");
        }
    }

    xess_d3d12_init_params_t params{};
    params.outputResolution.x = std::max(1u, outW);
    params.outputResolution.y = std::max(1u, outH);
    params.qualitySetting = mapQuality(gCfg.quality);
    params.initFlags = XESS_INIT_FLAG_INVERTED_DEPTH | XESS_INIT_FLAG_LDR_INPUT_COLOR;
    params.creationNodeMask = 1;
    params.visibleNodeMask = 1;

    dbgLog("XeSS: initializing context out=%ux%u quality=%d (may take a moment)",
        outW, outH, gCfg.quality);
    xess_result_t r = gApi.init(gContext, &params);
    if (r != XESS_RESULT_SUCCESS) {
        gStatus = std::string("XeSS: 初始化失败（") + resultText(r) + "）";
        dbgLog("XeSS: xessD3D12Init failed: %s", resultText(r).c_str());
        destroyContextLocked();
        err = gStatus;
        return false;
    }

    gInitialized = true;
    gOutW = outW;
    gOutH = outH;
    gInitQuality = gCfg.quality;
    gNeedsReset = true;
    gStatus = "XeSS: 已就绪";
    dbgLog("XeSS: context initialized out=%ux%u quality=%d", outW, outH, gCfg.quality);
    return true;
}

}  // namespace

// ---------------------------------------------------------------------------
// 公开 API
// ---------------------------------------------------------------------------

void xessSetConfig(const XessConfig& cfg) {
    XessConfig next = cfg;
    next.quality = std::max(0, std::min(4, next.quality));

    bool changed = false;
    {
        std::lock_guard<std::mutex> lk(gMtx);
        changed = next.enabled != gCfg.enabled || next.quality != gCfg.quality;
        gCfg = next;
        if (changed) gNeedsReset = true;
    }
    // 捕获管线需要知道是否还要每帧生成深度/运动矢量（XeSS 独立于 NGX）。
    ngxDlssSetExternalCapture(next.enabled);
    if (changed) {
        dbgLog("XeSS config: enabled=%d quality=%d", (int)next.enabled, next.quality);
    }
}

bool xessIsAvailable() {
    if (!ensureLibrary()) return false;
    if (gContext) return true;
    if (gContextCreateFailed) return false;

    DeviceContext& dc = deviceContextForJni();
    if (!dc.device) return false;

    std::lock_guard<std::mutex> lk(gMtx);
    if (gContext) return true;
    if (gContextCreateFailed) return false;
    xess_context_handle_t ctx = nullptr;
    xess_result_t r = gApi.createContext(dc.device.Get(), &ctx);
    if (r != XESS_RESULT_SUCCESS || !ctx) {
        gContextCreateFailed = true;
        gStatus = std::string("XeSS: 设备不可用（") + resultText(r) + "）";
        dbgLog("XeSS: availability probe failed: %s", resultText(r).c_str());
        return false;
    }
    gContext = ctx;
    if (gApi.setLoggingCallback) {
        gApi.setLoggingCallback(gContext, XESS_LOGGING_LEVEL_WARNING, logCallback);
    }
    gStatus = "XeSS: 已就绪（首次启用时初始化）";
    dbgLog("XeSS: context created");
    return true;
}

bool xessIsActive() {
    std::lock_guard<std::mutex> lk(gMtx);
    return gCfg.enabled && gInitialized && gContext != nullptr;
}

void xessReset() {
    std::lock_guard<std::mutex> lk(gMtx);
    gNeedsReset = true;
}

bool xessEvaluateSr(CommandContext* ctx, Dx12Surface* s, Dx12Object* colorTex,
    ID3D12Resource** outSrTex, std::string& err) {
    *outSrTex = nullptr;
    {
        std::lock_guard<std::mutex> lk(gMtx);
        if (!gCfg.enabled) return false;
    }
    if (!ctx || !ctx->listOpen || !ctx->commandList || !s || !colorTex
        || !colorTex->resource) {
        return false;
    }

    // 捕获资源由统一管线管理；首次调用会创建纹理（本帧无数据，下一帧生效）。
    std::string capErr;
    if (!ngxDlssEnsureCaptureResources(ctx, capErr)) {
        if (!capErr.empty()) err = capErr;
        return false;
    }

    NgxDlssCapturedInputs in;
    if (!ngxDlssGetCapturedInputs(ctx, in)) {
        std::lock_guard<std::mutex> lk(gMtx);
        gNeedsReset = true;
        err = "XeSS: no depth/MV capture for this frame";
        return false;
    }

    if (!ensureLibrary()) {
        std::lock_guard<std::mutex> lk(gMtx);
        err = gStatus;
        return false;
    }
    if (!ensureContext(s->width, s->height, err)) {
        return false;
    }

    const D3D12_RESOURCE_DESC colorDesc = colorTex->resource->GetDesc();
    if (!ensureOutputs(colorDesc.Format, s->width, s->height, err)) {
        return false;
    }

    OutputTexture& o = gOutputs[ctx->currentListSlot];
    transitionTextureTo(ctx, colorTex,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, in.depth, D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, in.mv, D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, o.tex.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_UNORDERED_ACCESS);

    xess_d3d12_execute_params_t p{};
    p.pColorTexture = colorTex->resource.Get();
    p.pVelocityTexture = in.mv;
    p.pDepthTexture = in.depth;
    p.pOutputTexture = o.tex.Get();
    p.jitterOffsetX = in.jitterPx;
    p.jitterOffsetY = in.jitterPy;
    p.exposureScale = 1.0f;
    {
        std::lock_guard<std::mutex> lk(gMtx);
        p.resetHistory = gNeedsReset ? 1 : 0;
    }
    p.inputWidth = in.renderW;
    p.inputHeight = in.renderH;

    xess_result_t r = gApi.execute(gContext, ctx->commandList, &p);

    // XeSS 会切换描述符堆 / 根签名 / PSO，恢复本模组的绘制堆。
    DeviceContext& dc = deviceContextForJni();
    if (dc.drawHeap) {
        ID3D12DescriptorHeap* heaps[] = { dc.drawHeap.Get() };
        ctx->commandList->SetDescriptorHeaps(1, heaps);
    }
    ctx->cachedPso = nullptr;
    ctx->currentPipeline = nullptr;

    if (r != XESS_RESULT_SUCCESS) {
        static bool logged = false;
        if (!logged) {
            logged = true;
            dbgLog("XeSS: Execute failed: %s", resultText(r).c_str());
        }
        std::lock_guard<std::mutex> lk(gMtx);
        gNeedsReset = true;
        err = std::string("XeSS: Execute failed ") + resultText(r);
        return false;
    }

    {
        std::lock_guard<std::mutex> lk(gMtx);
        gNeedsReset = false;
    }
    transitionTo(ctx, o.tex.Get(), D3D12_RESOURCE_STATE_UNORDERED_ACCESS,
        D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);
    *outSrTex = o.tex.Get();

    static bool loggedOk = false;
    if (!loggedOk) {
        loggedOk = true;
        int quality = 0;
        {
            std::lock_guard<std::mutex> lk(gMtx);
            quality = gCfg.quality;
        }
        dbgLog("XeSS SR: ok render=%ux%u out=%ux%u quality=%d",
            in.renderW, in.renderH, s->width, s->height, quality);
    }
    return true;
}

std::string xessStatusText() {
    std::lock_guard<std::mutex> lk(gMtx);
    if (!gApi.module && gApi.loadFailed) {
        return "XeSS: 未找到 libxess.dll";
    }
    return gStatus;
}

void xessShutdown() {
    std::string waitErr;
    deviceWaitIdle(waitErr);
    {
        std::lock_guard<std::mutex> lk(gMtx);
        destroyContextLocked();
        gContextCreateFailed = false;
        gStatus = "XeSS: 已关闭";
        gNeedsReset = true;
    }
    releaseOutputs();
    ngxDlssSetExternalCapture(false);
}

}  // namespace dx12mc
