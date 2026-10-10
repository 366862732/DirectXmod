// dx12-mc 原生 D3D12 层（C++）
// P5 目标：DXGI swapchain（GpuSurfaceBackend 对应物）。
// 设计参考：官方 com.mojang.blaze3d.vulkan.VulkanGpuSurface。
//
// PresentMode 序数 = 官方枚举 ordinal：
//   IMMEDIATE=0, MAILBOX=1, FIFO=2, FIFO_RELAXED=3
// DXGI FLIP 模型支持 {IMMEDIATE, FIFO, FIFO_RELAXED}（MAILBOX 无直接对应）。

#include "dx12_device.h"

#include <dxgi.h>
#include <dxgi1_2.h>
#include <dxgi1_4.h>

#include <cstdio>
#include <cstring>
#include <sstream>
// Diagnostic: fill backbuffer with colored clear instead of copying src texture.
// Uncomment to test whether the backbuffer通路 itself works.
// 屏幕变绿 => 通路正常，问题在 src 纹理内容；仍黑屏 => 跳到第三阶段检查 Present。
// 启用方式：改为 #define DIAG_CLEAR_BACKBUFFER_TO_GREEN 1 并重新编译。
// 注意：DIAG_CLEAR 路径仅应在 selfTestSurface / testRenderLoop 中临时启用。
// 游戏正常流程必须保持为 0，否则每帧清屏会覆盖渲染内容。
#define DIAG_CLEAR_BACKBUFFER_TO_GREEN 0
// P20: 启用后每 30 帧在 blit 前读回源纹理像素，诊断着色器输出颜色。
// 结论（已获取）：纯绿 = shader 正确；全黑 = shader 未写入或深度/裁剪问题；旧数据 = 渲染 pass 未执行。
// P49：默认关闭。该诊断每次执行都 deviceWaitIdle（整卡同步）+ 分配一整个 back buffer
// 大小的 staging 缓冲 + 9 条 dbgLog（每条含 stderr 与日志文件 flush），且在 Release 中
// 每 30 帧无条件触发——会周期性打断 P48 多帧飞行的 CPU/GPU 重叠。需要时改为 1 重编译。
#define DIAG_READBACK_COLOR_TEX 0

#include <string>
#include <vector>
#include <thread>
#include <chrono>

namespace dx12mc {

namespace {

std::string hrText(HRESULT hr) {
    char buf[64];
    snprintf(buf, sizeof(buf), "HRESULT 0x%08lX", (unsigned long)hr);
    return buf;
}

// 把当前线程消息队列抽干 ms 毫秒。CreateSwapChainForHwnd 返回 E_ACCESSDENIED
// (0x80070005) 时多为瞬态：窗口刚由 GLFW 创建、DWM 尚未完成重定向，或被第三方
// 覆盖层/输入法短暂占用。泵消息 + 短等待后重试即可成功（本机实测偶发，重试必成）。
void pumpWindowMessages(int ms) {
    const DWORD deadline = GetTickCount() + (DWORD)(ms > 0 ? ms : 0);
    MSG msg;
    for (;;) {
        while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }
        if (GetTickCount() >= deadline) break;
        Sleep(8);
    }
}

// swapchain 创建结果（含 DirectComposition 回退资源）。
struct SwapChainBundle {
    ComPtr<IDXGISwapChain1> swapChain;
    bool composition = false;   // true = 由 CreateSwapChainForComposition 创建
    bool allowTearing = false;  // 仅 HWND + ALLOW_TEARING tier 为 true
    ComPtr<IDCompositionDevice> dcompDevice;
    ComPtr<IDCompositionTarget> dcompTarget;
    ComPtr<IDCompositionVisual> dcompVisual;
};

// 动态解析 dcomp.dll 的 DCompositionCreateDevice（避免链接期依赖 dcomp.lib）。
typedef HRESULT(WINAPI* PFN_DCompositionCreateDevice)(IUnknown*, REFIID, void**);
PFN_DCompositionCreateDevice resolveDCompCreateDevice() {
    static PFN_DCompositionCreateDevice fn = []() -> PFN_DCompositionCreateDevice {
        HMODULE mod = LoadLibraryW(L"dcomp.dll");
        if (!mod) return nullptr;
        return reinterpret_cast<PFN_DCompositionCreateDevice>(
            GetProcAddress(mod, "DCompositionCreateDevice"));
    }();
    return fn;
}

// 解绑/绑定 composition visual 的内容。DirectComposition 会持有 swapchain 的
// backbuffer 引用，因此在 ResizeBuffers / 释放 swapchain 前必须先
// SetContent(nullptr) + Commit，否则 ResizeBuffers 返回 DXGI_ERROR_INVALID_CALL。
bool setCompositionContent(IDCompositionVisual* visual, IDCompositionDevice* dev,
    IUnknown* content, std::string& err) {
    if (!visual || !dev) { err = "composition visual/device null"; return false; }
    HRESULT hr = visual->SetContent(content);
    if (FAILED(hr)) { err = "IDCompositionVisual::SetContent failed " + hrText(hr); return false; }
    hr = dev->Commit();
    if (FAILED(hr)) { err = "IDCompositionDevice::Commit failed " + hrText(hr); return false; }
    return true;
}

// 为本 HWND 建立 DirectComposition 合成链（device/target/visual）。
bool initComposition(HWND win, SwapChainBundle& b, std::string& err) {
    PFN_DCompositionCreateDevice createDevice = resolveDCompCreateDevice();
    if (!createDevice) {
        err = "dcomp.dll / DCompositionCreateDevice unavailable";
        return false;
    }
    HRESULT hr = createDevice(nullptr, __uuidof(IDCompositionDevice),
        reinterpret_cast<void**>(b.dcompDevice.GetAddressOf()));
    if (FAILED(hr)) { err = "DCompositionCreateDevice failed " + hrText(hr); return false; }
    hr = b.dcompDevice->CreateTargetForHwnd(win, TRUE, b.dcompTarget.GetAddressOf());
    if (FAILED(hr)) { err = "IDCompositionDevice::CreateTargetForHwnd failed " + hrText(hr); return false; }
    hr = b.dcompDevice->CreateVisual(b.dcompVisual.GetAddressOf());
    if (FAILED(hr)) { err = "IDCompositionDevice::CreateVisual failed " + hrText(hr); return false; }
    hr = b.dcompTarget->SetRoot(b.dcompVisual.Get());
    if (FAILED(hr)) { err = "IDCompositionTarget::SetRoot failed " + hrText(hr); return false; }
    return true;
}

// 创建 composition swapchain 并把内容绑定到 visual 上。
bool createCompositionSwapChain(IDXGIFactory2* factory2, ID3D12CommandQueue* queue,
    UINT width, UINT height, SwapChainBundle& b, std::string& err) {
    DXGI_SWAP_CHAIN_DESC1 sd{};
    sd.Width = width > 0 ? width : 1;
    sd.Height = height > 0 ? height : 1;
    sd.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    sd.Stereo = FALSE;
    sd.SampleDesc.Count = 1;
    sd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    sd.BufferCount = kSurfaceBufferCount;
    sd.Scaling = DXGI_SCALING_STRETCH;                 // composition 仅支持 STRETCH
    sd.SwapEffect = DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL;  // composition 支持 SEQUENTIAL/DISCARD
    sd.AlphaMode = DXGI_ALPHA_MODE_PREMULTIPLIED;      // composition 不接受 UNSPECIFIED
    sd.Flags = 0;                                      // composition 不支持 ALLOW_TEARING
    HRESULT hr = factory2->CreateSwapChainForComposition(queue, &sd, nullptr, &b.swapChain);
    if (FAILED(hr)) { err = "CreateSwapChainForComposition failed " + hrText(hr); return false; }
    if (!setCompositionContent(b.dcompVisual.Get(), b.dcompDevice.Get(), b.swapChain.Get(), err))
        return false;
    b.composition = true;
    b.allowTearing = false;
    return true;
}

// 为窗口创建 swapchain：
//   1) 首选 CreateSwapChainForHwnd：多 tier（FLIP_DISCARD±TEARING / FLIP_SEQUENTIAL）
//      并重试 3 轮（E_ACCESSDENIED 多为瞬态）；
//   2) 全部被拒时回退 CreateSwapChainForComposition + DirectComposition（不绑定 HWND）。
// reuse 非空且已是 composition 模式时直接复用其 composition 对象重建——对同一 HWND
// 重复 CreateTargetForHwnd 会返回 DCOMPOSITION_ERROR_WINDOW_ALREADY_COMPOSED。
bool createSwapChainForWindow(IDXGIFactory4* factory, ID3D12CommandQueue* queue,
    HWND win, UINT width, UINT height, const SwapChainBundle* reuse,
    SwapChainBundle& out, std::string& err) {
    static constexpr struct { DXGI_SWAP_EFFECT effect; UINT flags; const char* label; } kTiers[] = {
        { DXGI_SWAP_EFFECT_FLIP_DISCARD,    DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING, "FLIP_DISCARD+TEARING" },
        { DXGI_SWAP_EFFECT_FLIP_DISCARD,    0,                                  "FLIP_DISCARD"         },
        { DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL, 0,                                  "FLIP_SEQUENTIAL"      },
    };

    if (!reuse || !reuse->composition) {
        DXGI_SWAP_CHAIN_DESC1 sd{};
        sd.Width = 1;   // 占位；configureSurface 时 ResizeBuffers 到实际尺寸
        sd.Height = 1;
        sd.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        sd.Stereo = FALSE;
        sd.SampleDesc.Count = 1;
        sd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        sd.BufferCount = kSurfaceBufferCount;
        sd.Scaling = DXGI_SCALING_STRETCH;
        sd.AlphaMode = DXGI_ALPHA_MODE_UNSPECIFIED;

        const int kPasses = 3;
        HRESULT lastHr = E_FAIL;
        for (int pass = 0; pass < kPasses && !out.swapChain; ++pass) {
            if (pass > 0) {
                dbgLog("createSwapChainForWindow: all HWND tiers failed (last hr=0x%08X), "
                    "retry pass=%d (pump %dms)", (unsigned)lastHr, pass, pass * 60);
                pumpWindowMessages(pass * 60);
            }
            for (const auto& tier : kTiers) {
                sd.SwapEffect = tier.effect;
                sd.Flags = tier.flags;
                HRESULT hr = factory->CreateSwapChainForHwnd(queue, win, &sd, nullptr, nullptr,
                    &out.swapChain);
                if (SUCCEEDED(hr)) {
                    out.allowTearing = (tier.flags & DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING) != 0;
                    std::fprintf(stderr, "[dx12] swapchain OK (tier=%s pass=%d)\n", tier.label, pass);
                    break;
                }
                lastHr = hr;
                std::fprintf(stderr, "[dx12] swapchain tier %s failed hr=%08X (pass=%d)\n",
                    tier.label, (unsigned)hr, pass);
            }
        }
    } else {
        dbgLog("createSwapChainForWindow: composition surface -> recreate composition directly");
    }

    if (out.swapChain) return true;

    // DirectComposition 回退
    if (win == nullptr) {
        err = "swapchain creation failed: HWND invalid, cannot use composition fallback";
        return false;
    }
    ComPtr<IDXGIFactory2> factory2;
    if (FAILED(factory->QueryInterface(IID_PPV_ARGS(&factory2)))) {
        err = "swapchain creation failed (HWND tiers) and factory lacks IDXGIFactory2";
        return false;
    }

    std::string cErr;
    if (reuse && reuse->composition) {
        out.dcompDevice = reuse->dcompDevice;
        out.dcompTarget = reuse->dcompTarget;
        out.dcompVisual = reuse->dcompVisual;
        setCompositionContent(out.dcompVisual.Get(), out.dcompDevice.Get(), nullptr, cErr);
    } else if (!initComposition(win, out, cErr)) {
        err = "swapchain HWND tiers failed; composition fallback failed: " + cErr;
        return false;
    }
    if (!createCompositionSwapChain(factory2.Get(), queue, width, height, out, cErr)) {
        err = "swapchain HWND tiers failed; composition fallback failed: " + cErr;
        return false;
    }
    std::fprintf(stderr, "[dx12] composition swapchain OK (%ux%u)\n", width, height);
    return true;
}

// 取 surface 的 back buffer index 对应的 RTV（blit 后可用；P5 自检不用）。
}  // namespace

Dx12Surface* createSurface(uintptr_t hwnd, std::string& err) {
    DeviceContext& ctx = deviceContextForJni();
    if (!ctx.device || !ctx.queue) {
        err = "device not initialized (call dx12CreateDevice first)";
        return nullptr;
    }

    // P33 诊断：清空调试层消息（防止自测期间累积的旧消息干扰本次判断）
    if (ctx.infoQueue) {
        UINT64 n = ctx.infoQueue->GetNumStoredMessages();
        if (n > 0) {
            for (UINT64 i = 0; i < n; ++i) {
                SIZE_T len = 0;
                if (FAILED(ctx.infoQueue->GetMessage((UINT)i, nullptr, &len))) continue;
                std::vector<char> buf(len > 0 ? len : 1);
                D3D12_MESSAGE* msg = reinterpret_cast<D3D12_MESSAGE*>(buf.data());
                if (SUCCEEDED(ctx.infoQueue->GetMessage((UINT)i, msg, &len))) {
                    const char* sev = "?";
                    switch (msg->Severity) {
                        case D3D12_MESSAGE_SEVERITY_CORRUPTION: sev = "CORRUPTION"; break;
                        case D3D12_MESSAGE_SEVERITY_ERROR:    sev = "ERROR";    break;
                        case D3D12_MESSAGE_SEVERITY_WARNING:  sev = "WARNING";  break;
                        default: break;
                    }
                    std::fprintf(stderr, "[dx12] PreSwapInfoQueue[%s] %s\n",
                        sev, msg->pDescription ? msg->pDescription : "");
                }
            }
            ctx.infoQueue->ClearStoredMessages();
        }
    }
    DXGI_ADAPTER_DESC devDesc{};
    if (!ctx.adapter) {
        err = "createSurface: ctx.adapter is null";
        return nullptr;
    }
    ctx.adapter->GetDesc(&devDesc);
    std::fprintf(stderr, "[dx12] createSurface: devLuid=%08X%08X desc=%S\n",
        devDesc.AdapterLuid.LowPart, devDesc.AdapterLuid.HighPart, devDesc.Description);
    std::fflush(stderr);
    // #region debug-point A:hwnd-validation
    HWND win = nullptr;
    LONG_PTR classStyle = 0;
    if (hwnd != 0) {
        win = reinterpret_cast<HWND>(hwnd);
        DWORD wndPid = 0;
        DWORD wndTid = GetWindowThreadProcessId(win, &wndPid);
        RECT rc{};
        BOOL hasRect = GetClientRect(win, &rc);
        wchar_t className[256]{};
        GetClassNameW(win, className, 255);
        classStyle = GetClassLongPtrW(win, GCL_STYLE);
        std::fprintf(stderr,
            "[dx12] createSurface: isWindow=%d wndTid=%lu wndPid=%lu curTid=%lu curPid=%lu clientRect=%ld,%ld-%ld,%ld hasRect=%d classStyle=0x%llx CS_OWNDC=%d CS_CLASSDC=%d CS_PARENTDC=%d class=%S\n",
            IsWindow(win) ? 1 : 0,
            (unsigned long)wndTid,
            (unsigned long)wndPid,
            (unsigned long)GetCurrentThreadId(),
            (unsigned long)GetCurrentProcessId(),
            (long)rc.left, (long)rc.top, (long)rc.right, (long)rc.bottom,
            hasRect ? 1 : 0,
            (unsigned long long)classStyle,
            (classStyle & CS_OWNDC) ? 1 : 0,
            (classStyle & CS_CLASSDC) ? 1 : 0,
            (classStyle & CS_PARENTDC) ? 1 : 0,
            className);
    }
    // #endregion

    // P33：CreateSwapChainForHwnd 在部分环境下对所有 tier 返回 E_ACCESSDENIED
    // (0x80070005，见 Issue #11)。GLFW 窗口类固定带 CS_OWNDC，此处仅作信息记录；
    // 真正的兜底是 createSwapChainForWindow() 内的多 tier 重试 + Composition 回退。
    if (win != nullptr && (classStyle & CS_OWNDC) != 0) {
        std::fprintf(stderr, "[dx12] createSurface: window class has CS_OWNDC (GLFW); "
            "composition fallback available if HWND tiers are denied\n");
    }

    ComPtr<IDXGIFactory4> factory;
    // 诊断：上一次运行在「CS_OWNDC 提示」之后、configureSurface 打印之前静默中断且无任何
    // 错误输出（见 debug.log）。此处按步骤打点并 fflush，以便把中断位置精确定位到
    // 具体调用（CreateDXGIFactory1 / EnumAdapters / ctx.adapter 赋值）。
    std::fprintf(stderr, "[dx12] createSurface: step=CreateDXGIFactory1 begin\n");
    std::fflush(stderr);
    HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(&factory));
    std::fprintf(stderr, "[dx12] createSurface: step=CreateDXGIFactory1 done hr=%08X\n",
        (unsigned)hr);
    std::fflush(stderr);
    if (FAILED(hr)) {
        err = "CreateDXGIFactory1 failed " + hrText(hr);
        return nullptr;
    }
    // 确保 factory 能访问 device 所在的 adapter（防止多 GPU 系统上 factory/adapter 不匹配）
    {
        LUID devLuid = ctx.device->GetAdapterLuid();
        for (UINT i = 0; ; ++i) {
            ComPtr<IDXGIAdapter> adj;
            if (FAILED(factory->EnumAdapters(i, &adj))) break;
            DXGI_ADAPTER_DESC desc{};
            if (SUCCEEDED(adj->GetDesc(&desc)) &&
                desc.AdapterLuid.LowPart == devLuid.LowPart &&
                desc.AdapterLuid.HighPart == devLuid.HighPart) {
                ctx.adapter = adj;
                break;
            }
        }
    }
    std::fprintf(stderr, "[dx12] createSurface: step=adapter-scan done adapter=%p\n",
        (void*)ctx.adapter.Get());
    std::fflush(stderr);

    // P3.1 诊断：打印 SwapChain 格式，确认不是深度/单通道格式
    dbgLog("configureSurface: swapchain format=DXGI_FORMAT_R8G8B8A8_UNORM (scFmt=%d)",
        (int)DXGI_FORMAT_R8G8B8A8_UNORM);

    // P33：诊断——打印 HWND 值，帮助排查 E_ACCESSDENIED 问题。
    std::fprintf(stderr, "[dx12] dx12CreateSurface: hwnd=0x%llx queue=0x%p\n",
        (unsigned long long)hwnd, (void*)ctx.queue.Get());
    std::fflush(stderr);

    // 初始尺寸：composition 回退路径需要真实客户区尺寸；HWND 路径仍用 1x1 占位
    // （由 configureSurface 的 ResizeBuffers 调整到目标尺寸）。
    UINT initW = 1, initH = 1;
    if (win != nullptr) {
        RECT crc{};
        if (GetClientRect(win, &crc) && crc.right > crc.left && crc.bottom > crc.top) {
            initW = (UINT)(crc.right - crc.left);
            initH = (UINT)(crc.bottom - crc.top);
        }
    }

    // P33：CreateSwapChainForHwnd 在部分环境（Issue #11：NVIDIA 独显 + Intel 核显
    // 笔记本，所有 tier 返回 E_ACCESSDENIED 0x80070005）会被拒绝。先多 tier + 重试，
    // 仍失败则回退 CreateSwapChainForComposition + DirectComposition（不绑定 HWND）。
    SwapChainBundle bundle;
    std::string scErr;
    if (!createSwapChainForWindow(factory.Get(), ctx.queue.Get(), win,
            initW, initH, nullptr, bundle, scErr)) {
        // 打印调试层消息辅助诊断
        if (ctx.infoQueue) {
            UINT64 n = ctx.infoQueue->GetNumStoredMessages();
            for (UINT64 i = 0; i < n && i < 20; ++i) {
                SIZE_T len = 0;
                if (FAILED(ctx.infoQueue->GetMessage((UINT)i, nullptr, &len))) continue;
                std::vector<char> buf(len > 0 ? len : 1);
                D3D12_MESSAGE* msg = reinterpret_cast<D3D12_MESSAGE*>(buf.data());
                if (SUCCEEDED(ctx.infoQueue->GetMessage((UINT)i, msg, &len))) {
                    std::fprintf(stderr, "[dx12] InfoQueue[%u] %s\n",
                        (unsigned)i, msg->pDescription ? msg->pDescription : "");
                }
            }
            ctx.infoQueue->ClearStoredMessages();
        }
        err = scErr;
        return nullptr;
    }
    ComPtr<IDXGISwapChain1> swapChain1 = bundle.swapChain;

    ComPtr<IDXGISwapChain3> swapChain3;
    if (FAILED(swapChain1.As(&swapChain3))) {
        err = "swapchain does not support IDXGISwapChain3";
        return nullptr;
    }

    Dx12Surface* s = new Dx12Surface();
    s->hwnd = hwnd;
    s->swapChain = swapChain3;
    s->compositionMode = bundle.composition;
    s->dcompDevice = bundle.dcompDevice;
    s->dcompTarget = bundle.dcompTarget;
    s->dcompVisual = bundle.dcompVisual;
    // P65：记录实际创建是否带 ALLOW_TEARING（composition swapchain 不支持 tearing）。
    s->allowTearing = bundle.allowTearing;
    // 注意：不在此处 setActiveSurface！surface 必须在 configureSurface 完成后
    // 才设为 active，否则渲染线程会在 backBuffers 为空时尝试 acquire 导致无限循环。
    return s;
}

std::vector<int> surfacePresentModes() {
    // IMMEDIATE=0, FIFO=2, FIFO_RELAXED=3（MAILBOX=1 在 DXGI FLIP 下无直接对应）
    return {0, 2, 3};
}

bool configureSurface(Dx12Surface* s, int width, int height, int presentMode,
    std::string& err) {
    DeviceContext& ctx = deviceContextForJni();
    if (!s || !s->swapChain) {
        err = "surface not created";
        return false;
    }
    // 窗口最小化/边框切换瞬间 WM_SIZE 可能传 0 尺寸——ResizeBuffers 对 0 尺寸
    // 返回 DXGI_ERROR_INVALID_CALL (0x887A0001)，保持旧尺寸继续，不视为错误。
    // 非 0 尺寸时也偶发 INVALID_CALL（DWM 仍持有 backbuffer 引用，与 NOT_CURRENTLY_AVAILABLE
    // 同源竞态），交由下方重试循环处理。
    if (width <= 0 || height <= 0) {
        s->presentMode = presentMode;
        return true;
    }
    // 防御：glfwGetFramebufferSize 在某些时机（DWM 切换/显示器热插拔瞬间）会返回错误
    // 尺寸（如 854x1048 而非实际的 854x480）。若新高度相对旧高度偏差超过 2 倍且宽度
    // 未变化，视为无效回调，保留旧尺寸避免 swapchain 重建为错误比例导致黑屏。
    if (s->width > 0 && s->height > 0
        && width == s->width && height > static_cast<int>(s->height) * 2) {
        dbgLog("configureSurface: rejecting invalid height %d (current %d), keeping old",
            height, s->height);
        s->presentMode = presentMode;
        return true;
    }
    s->presentMode = presentMode;

    // 新尺寸与当前 swapchain 一致时，无需 ResizeBuffers（避免已重建的 swapchain
    // 被错误尺寸锁死后再调 ResizeBuffers 仍报 INVALID_CALL）。
    if ((UINT)width == s->width && (UINT)height == s->height) {
        dbgLog("configureSurface: size unchanged %dx%d, skip ResizeBuffers", width, height);
        return true;
    }

    // ResizeBuffers 前必须等 GPU 完全空闲：FLIP model 下 backbuffer 仍被
    // 上一帧命令队列引用时，ResizeBuffers 返回 DXGI_ERROR_NOT_CURRENTLY_AVAILABLE
    // (0x887A0001)——游戏启动/窗口调整时多次 configure 失败即此原因。
    // deviceWaitIdle 现已确保等待一个绝对高于 GPU 已完成值的 fence，
    // 避免提前返回导致 DWM 仍持有 backbuffer 引用而失败（DXGI_ERROR_INVALID_CALL）。
    // 镜像官方 VulkanGpuSurface 调整 swapchain 前的 waitIdle 语义。
    if (!deviceWaitIdle(err)) {
        err = "deviceWaitIdle before ResizeBuffers failed: " + err;
        return false;
    }
    dbgLog("configureSurface: idle ok, ResizeBuffers %dx%d mode=%d", width, height, presentMode);
    // 防御：vanilla 在 acquire 与 present 之间若被窗口事件触发 configure，
    // 会残留"已 acquire 未 present"的 backbuffer；ResizeBuffers 对其返回
    // DXGI_ERROR_NOT_CURRENTLY_AVAILABLE -> MC surfaceIsInvalid=true -> 之后
    // 不再 acquire/blit/present -> 画面冻结（渲染仍继续）。先 Present 释放。
    if (s->currentImageIndex >= 0) {
        dbgLog("configureSurface: releasing acquired backbuffer idx=%d before ResizeBuffers",
            s->currentImageIndex);
        s->swapChain->Present(0, 0);
        s->currentImageIndex = -1;
    }
    // composition 模式：DirectComposition 持有 backbuffer 引用，ResizeBuffers /
    // 释放 swapchain 前必须先解绑 visual 内容并 Commit，否则报 INVALID_CALL。
    if (s->compositionMode) {
        std::string uErr;
        if (!setCompositionContent(s->dcompVisual.Get(), s->dcompDevice.Get(), nullptr, uErr)) {
            dbgLog("configureSurface: unbind composition content failed: %s", uErr.c_str());
        }
    }
    // 使用 swap chain 创建时的格式，而非 s->format（后者可能因内存损坏/误用而变为无效值，
    // 导致 ResizeBuffers 以 0x887A0001 (DXGI_ERROR_INVALID_CALL) 失败）。
    const DXGI_FORMAT scFmt = DXGI_FORMAT_R8G8B8A8_UNORM;
    // composition swapchain 不支持 ALLOW_TEARING，ResizeBuffers 必须传 0。
    const UINT scFlags = s->compositionMode ? 0 : DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING;
    HRESULT hr = S_OK;
    // deviceWaitIdle 完成后 DWM 合成器可能仍在异步持有 backbuffer 引用（flip model
    // + 窗口/全屏切换时常见竞态）。多次重试 + 递增等待，最多等 ~1s。
    for (int retry = 0; retry < 10; ++retry) {
        if (retry > 0) {
            int waitMs = (1 << retry);  // 2,4,8,16,32,64,128,256,512 ms
            dbgLog("configureSurface: retry %d after %dms", retry, waitMs);
            std::this_thread::sleep_for(std::chrono::milliseconds(waitMs));
        }
        hr = s->swapChain->ResizeBuffers(kSurfaceBufferCount, (UINT)width, (UINT)height,
            scFmt, scFlags);
        if (SUCCEEDED(hr)) {
            dbgLog("configureSurface: ResizeBuffers ok (retry=%d)", retry);
            break;
        }
        dbgLog("configureSurface: ResizeBuffers failed %s (retry=%d)", hrText(hr).c_str(), retry);
        // INVALID_CALL 与 NOT_CURRENTLY_AVAILABLE 同源：DWM 异步持有 backbuffer 引用时
        // 均可能出现，统一重试（递增等待，最多 ~1s）。
        if (hr != DXGI_ERROR_NOT_CURRENTLY_AVAILABLE && hr != DXGI_ERROR_INVALID_CALL) break;
    }
    if (FAILED(hr)) {
        // ResizeBuffers 全部失败（flip model 限制）：参照 VulkanGpuSurface.configure()
        // 的做法——销毁旧 swapchain 后重建全新 swapchain（同样尺寸、同样 HWND）。
        // 必须先 Release 旧 swapchain，否则 CreateSwapChainForHwnd 返回 E_ACCESSDENIED。
        dbgLog("configureSurface: ResizeBuffers FAILED after 10 retries, falling back to recreate");
        // 先 Present 释放任何残留的 acquired backbuffer
        if (s->currentImageIndex >= 0) {
            s->swapChain->Present(0, 0);
            s->currentImageIndex = -1;
        }
        deviceWaitIdle(err);
        s->swapChain.Reset();
        s->backBuffers.clear();
        s->rtvHandles.clear();
        s->currentImageIndex = -1;
        s->lastBlitIndex = -1;

        // 重建 swapchain（与 createSurface 相同的策略：HWND 多 tier + 重试，
        // 全部被拒则回退 composition）。
        ComPtr<IDXGIFactory4> factory;
        hr = CreateDXGIFactory1(IID_PPV_ARGS(&factory));
        if (FAILED(hr)) {
            err = "CreateDXGIFactory1 failed " + hrText(hr);
            return false;
        }
        // 已是 composition 模式时复用现有 composition 对象（对同一 HWND 重复
        // CreateTargetForHwnd 会返回 WINDOW_ALREADY_COMPOSED），直接重建 swapchain。
        SwapChainBundle reuse;
        if (s->compositionMode) {
            reuse.composition = true;
            reuse.dcompDevice = s->dcompDevice;
            reuse.dcompTarget = s->dcompTarget;
            reuse.dcompVisual = s->dcompVisual;
        }
        SwapChainBundle bundle;
        std::string rErr;
        if (!createSwapChainForWindow(factory.Get(), ctx.queue.Get(),
                reinterpret_cast<HWND>(s->hwnd), (UINT)width, (UINT)height,
                s->compositionMode ? &reuse : nullptr, bundle, rErr)) {
            err = rErr + " (recreate)";
            return false;
        }
        if (FAILED(bundle.swapChain.As(&s->swapChain))) {
            err = "swapchain does not support IDXGISwapChain3";
            return false;
        }
        s->compositionMode = bundle.composition;
        s->dcompDevice = bundle.dcompDevice;
        s->dcompTarget = bundle.dcompTarget;
        s->dcompVisual = bundle.dcompVisual;
        s->allowTearing = bundle.allowTearing;
        dbgLog("configureSurface: recreated swapchain %dx%d (composition=%d)",
            width, height, (int)s->compositionMode);
    }

    // composition 模式：把 visual 内容重新绑回（ResizeBuffers 后的同一 swapchain，
    // 或重建后的新 swapchain）。
    if (s->compositionMode) {
        std::string bErr;
        if (!setCompositionContent(s->dcompVisual.Get(), s->dcompDevice.Get(),
                s->swapChain.Get(), bErr)) {
            err = "configureSurface: rebind composition content failed: " + bErr;
            return false;
        }
    }

    // 仅在成功路径上更新尺寸：失败时保持旧尺寸，避免后续调用因 s->width/s->height
    // 已被设为错误值而反复尝试无效 ResizeBuffers。
    s->width = (UINT)width;
    s->height = (UINT)height;

    // 重新取 back buffers + RTV
    s->backBuffers.resize(kSurfaceBufferCount);
    s->rtvHandles.clear();
    s->surfaceFences.assign(kSurfaceBufferCount, 0);  // P18：per-backbuffer fence 初值
    for (UINT i = 0; i < kSurfaceBufferCount; ++i) {
        hr = s->swapChain->GetBuffer(i, IID_PPV_ARGS(&s->backBuffers[i]));
        if (FAILED(hr)) {
            err = "GetBuffer failed " + hrText(hr);
            return false;
        }
        D3D12_CPU_DESCRIPTOR_HANDLE rtv = allocRtvHandle(err);
        if (rtv.ptr == 0) {
            return false;
        }
        ctx.device->CreateRenderTargetView(s->backBuffers[i].Get(), nullptr, rtv);
        s->rtvHandles.push_back(rtv);
    }
    // P35：配置完成后才设为 active surface，确保渲染线程不会在 backBuffers 未就绪时 acquire。
    setActiveSurface(s);
    dbgLogInfo("configureSurface: registered active surface=%p bbCount=%u", (void*)s,
        (UINT)s->backBuffers.size());
    return true;
}

bool acquireSurface(Dx12Surface* s, std::string& err) {
    if (!s || !s->swapChain) {
        err = "surface not created";
        return false;
    }
    // P48（多帧飞行）：Present 尚未完成时 GetCurrentBackBufferIndex 不会轮转，会返回
    // 上一帧已 Present 的同一个 back buffer（两帧写同一处 → 闪帧/撕裂）。真实 acquire
    // 已推迟到 blit 之前（Java Dx12GpuSurface.ensureAcquired），此时本帧录制已完成，
    // 上一帧 Present 通常早已结束 → 通常 0ms。同步/SYNC 路径（渲染线程未运行）下
    // waitForPendingPresents 内部直接返回，无额外开销。
    waitForPendingPresents();
    // 以下两条为逐帧诊断（原为 dbgLog，每帧 2 次 stderr+文件 flush，实测 ~0.1-0.2ms/条，
    // 属帧关键路径纯开销）→ 降为 DEBUG，需 DX12_LOG_VERBOSE=1 才输出。
    dbgLogDebug("acquireSurface: enter surface=%p", (void*)s);
    // P34 诊断：记录 GetCurrentBackBufferIndex 的原始返回值和 HRESULT
    UINT rawIdx = s->swapChain->GetCurrentBackBufferIndex();
    s->currentImageIndex = (int)rawIdx;
    UINT bbCount = (UINT)s->backBuffers.size();
    dbgLogDebug("acquireSurface: rawIdx=%u bbCount=%u currentImageIndex=%d",
        rawIdx, bbCount, s->currentImageIndex);
    if (s->currentImageIndex < 0 ||
        s->currentImageIndex >= (int)bbCount) {
        // P34 容错：若 backBuffers 为空说明 surface 尚未完成配置，直接返回 false
        // 让渲染线程稍后重试；若 backBuffers 已就绪但 index 异常，尝试通过
        // GetBufferCount 交叉验证 swapchain 状态。
        if (bbCount == 0) {
            err = "acquireSurface: backBuffers empty (surface not configured yet)";
        } else {
            // 交叉验证：从 swapchain desc 获取实际 buffer 数
            DXGI_SWAP_CHAIN_DESC1 sd{};
            HRESULT hr = s->swapChain->GetDesc1(&sd);
            err = "GetCurrentBackBufferIndex returned invalid index=" + std::to_string(rawIdx)
                + " bbCount=" + std::to_string(bbCount)
                + " scGetDesc_hr=" + hrText(hr) + " BufferCount=" + std::to_string(sd.BufferCount);
        }
        return false;
    }
    // P18：如果重用的是上一帧的 back buffer（上次 blit 可能还没完成），
    // 等待该 buffer 对应的 fence 完成，再允许 CPU 写入。
    // 对于 3-buffer swapchain 正常情况，GPU 早已完成，等待为 0ms。
    // 仅在 CPU 跑太快追上 GPU 时才短暂等待（比 submitCommandList 阻塞好，
    // 因为只在真正需要重用时才等，且等的是已提交的 blit 命令而非当前帧）。
    {
        UINT64 needed = s->surfaceFences.empty() ? 0 : s->surfaceFences[(size_t)s->currentImageIndex];
        DeviceContext& ctx = deviceContextForJni();
        if (needed > 0 && ctx.queueFence) {
            UINT64 cv = ctx.queueFence->GetCompletedValue();
            if (cv < needed) {
                dbgLog("acquireSurface: wait fence idx=%d needed=%llu cv=%llu",
                    s->currentImageIndex, (unsigned long long)needed, (unsigned long long)cv);
                std::string w;
                if (!waitForQueueFenceValue(needed, 5000000000ULL, w)) {
                    err = "acquireSurface: " + w;
                    return false;
                }
            }
        }
    }
    // 同步 lastBlitIndex：当游戏直接通过 beginRenderPass 渲染到表面纹理
    //（而非走 blitSurface 路径）时，readbackSurfacePixels 需要用最新的
    // currentImageIndex 才能读到正确的 backBuffer，否则会读到上一帧 blit 的
    // 旧数据（表现为黑屏/错误内容）。
    s->lastBlitIndex = s->currentImageIndex;
    return true;
}

// 返回当前 acquire 的 back buffer 原始 ID3D12Resource 指针。
uintptr_t getBackBufferHandle(Dx12Surface* s) {
    if (!s || s->currentImageIndex < 0 ||
        s->currentImageIndex >= (int)s->backBuffers.size()) {
        return 0ULL;
    }
    return reinterpret_cast<uintptr_t>(s->backBuffers[(size_t)s->currentImageIndex].Get());
}

// 返回当前渲染 pass 中第一个活跃颜色附件的纹理句柄（在 pass 内调用有效）。
uintptr_t getActiveColorTextureHandle(CommandContext* ctx) {
    if (!ctx || !ctx->inRenderPass || ctx->activeColorTargets.empty()) {
        return 0ULL;
    }
    return reinterpret_cast<uintptr_t>(ctx->activeColorTargets[0]->resource.Get());
}

bool blitSurface(CommandContext* ctx, Dx12Surface* s, Dx12Object* srcTex,
    std::string& err) {
    if (!ctx || !ctx->commandList) {
        err = "no command list";
        return false;
    }
    if (!s || s->currentImageIndex < 0) {
        err = "no acquired back buffer";
        return false;
    }
    if (!ctx->listOpen) {
        err = "command list not open (call dx12BeginCommandList first)";
        return false;
    }
    // srcTex 为 null 时仅做纯红色 clear（无 copy），用于渲染循环自检。
    if (!srcTex || !srcTex->resource) {
        srcTex = nullptr;  // 标记为无源纹理，走纯 clear 路径
    }

    ID3D12GraphicsCommandList* cmd = ctx->commandList;
    ID3D12Resource* dst = s->backBuffers[(size_t)s->currentImageIndex].Get();
    UINT w = s->width;
    UINT h = s->height;
    dbgLogDebug("blitSurface: cmd=%p dst=%p idx=%d w=%u h=%u srcTex=%p",
        (void*)cmd, (void*)dst, s->currentImageIndex, w, h, (void*)srcTex);

    // 源纹理可能是本帧渲染 pass 的输出（RENDER_TARGET/DEPTH_WRITE），或刚
    // 上传完的 COMMON；按跟踪状态过渡到 COPY_SOURCE 再拷贝。
    if (srcTex) transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COPY_SOURCE);
#if DIAG_READBACK_COLOR_TEX
    // P20 诊断：在 CopyTextureRegion 之前读回源纹理像素，确认 shader 输出颜色。
    // 必须在拷贝前执行——CopyTextureRegion 是"读源写目标"原子操作，拷贝后源
    // 内容已不可读；且后续帧可能复用该纹理导致 COMMON→COPY_SOURCE barrier 错配。
    // 仅每 ~30 帧执行（deviceWaitIdle 同步开销）。
    // 注意：srcTex 可能在 self-test 路径中为 null（纯 clear），需先判断。
    if (srcTex) {
        static int rbCount = 0;
        if (++rbCount % 30 == 0) {
            dbgReadbackTexturePixels(srcTex, "colorTex-before-copy");
        }
    }
#endif

    D3D12_RESOURCE_BARRIER barrier{};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrier.Transition.pResource = dst;
    barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    // Diagnostic: 用纯红色填充 Backbuffer，验证 backbuffer 通路是否工作。
    // 屏幕变红 => 通路正常，问题在 src 纹理内容；仍黑屏 => 跳到第三阶段检查 Present。
#if DIAG_CLEAR_BACKBUFFER_TO_GREEN
    D3D12_RESOURCE_BARRIER clearBarrier{};
    clearBarrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    clearBarrier.Transition.pResource = dst;
    clearBarrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    clearBarrier.Transition.StateBefore = D3D12_RESOURCE_STATE_COMMON;
    clearBarrier.Transition.StateAfter = D3D12_RESOURCE_STATE_RENDER_TARGET;
    cmd->ResourceBarrier(1, &clearBarrier);
    dbgLogDebug("blitSurface: after clear barrier transition");
    float greenColor[4] = {0.0f, 1.0f, 0.0f, 1.0f};
    D3D12_CPU_DESCRIPTOR_HANDLE rtv = s->rtvHandles[(size_t)s->currentImageIndex];
    dbgLogDebug("blitSurface: rtv ptr=%p idx=%d", (void*)rtv.ptr, s->currentImageIndex);
    cmd->ClearRenderTargetView(rtv, greenColor, 0, nullptr);
    dbgLogDebug("blitSurface: after ClearRenderTargetView");
    clearBarrier.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
    clearBarrier.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_DEST;
    cmd->ResourceBarrier(1, &clearBarrier);
    dbgLogDebug("DIAG_CLEAR_BACKBUFFER_TO_GREEN: backbuffer filled green, w=%u h=%u", w, h);
    // P11：与 #else 路径保持一致——源纹理显式回切 COMMON，避免残留 COPY_SOURCE
    // 状态导致下一帧 beginRenderPass 的 COMMON→RENDER_TARGET barrier 错配（ERROR）。
    if (srcTex) transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COMMON);
    // backbuffer 需回退到 PRESENT，否则下一帧 beginRenderPass 按 COMMON/PRESENT
    // 写 StateBefore 会与实际的 COPY_DEST 错配（ERROR）。
    D3D12_RESOURCE_BARRIER backToPresent{};
    backToPresent.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    backToPresent.Transition.pResource = dst;
    backToPresent.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    backToPresent.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_DEST;
    backToPresent.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
    cmd->ResourceBarrier(1, &backToPresent);
#else
    // srcTex 为 null 时（首次/自检路径）：backbuffer 内容未定义（驱动常显示
    // 红/杂色），用纯黑 clear 兜底避免启动红屏，再回切 PRESENT。
    if (!srcTex) {
        transitionTo(ctx, dst, D3D12_RESOURCE_STATE_COMMON, D3D12_RESOURCE_STATE_RENDER_TARGET);
        D3D12_CPU_DESCRIPTOR_HANDLE rtv = s->rtvHandles[(size_t)s->currentImageIndex];
        float black[4] = { 0.0f, 0.0f, 0.0f, 1.0f };
        cmd->ClearRenderTargetView(rtv, black, 0, nullptr);
        transitionTo(ctx, dst, D3D12_RESOURCE_STATE_RENDER_TARGET, D3D12_RESOURCE_STATE_PRESENT);
        dbgLogDebug("blitSurface: srcTex=null — clear backbuffer to black");
        return true;
    }

    // P22: Draw-based blit（全屏四边形）— 绕过 CopyTextureRegion 格式不兼容问题。
    // srcTex 格式（如 R32G32B32A32_FLOAT）与 backbuffer（R8G8B8A8_UNORM）不同，
    // CopyTextureRegion 静默失败；改用 GPU 光栅器采样+格式转换。
    //
    // 步骤：
    //   1. PRESENT → RENDER_TARGET（backbuffer）
    //   2. OMSetRenderTargets + SetGraphicsRootSignature + SetPipelineState
    //   3. 源纹理 → PIXEL_SHADER_RESOURCE（采样前必须过渡）
    //   4. 在 srvHeap 分配 SRV 槽位，绑定到根描述符表
    //   5. 绑定顶点/索引缓冲，DrawIndexedInstanced(6,1,0,0,0)
    //   6. RENDER_TARGET → PRESENT（backbuffer）
    //   7. 源纹理 → COMMON（下一帧准备）
    {
        // 1. PRESENT → RENDER_TARGET
        // 使用 transitionTo 而非裸 barrier：将 dst 纳入 ctx->resourceState 跟踪，
        // 使 endCommandList 的 PRESENT 清理循环能正确回切到 COMMON。
        // 初始锚点用 COMMON（flip model 下实际为 PRESENT，但 PRESENT→RENDER_TARGET
        // 与 COMMON→RENDER_TARGET 等价，driver 会忽略冗余 PRESENT→PRESENT barrier）。
        transitionTo(ctx, dst, D3D12_RESOURCE_STATE_COMMON, D3D12_RESOURCE_STATE_RENDER_TARGET);

        // 2. 绑定 RTV 和根签名
        D3D12_CPU_DESCRIPTOR_HANDLE rtv = s->rtvHandles[(size_t)s->currentImageIndex];
        cmd->OMSetRenderTargets(1, &rtv, FALSE, nullptr);
        // 确保 blit 管线已初始化（首帧调用时 initBlitPipeline 尚未运行）
        std::string blitErr;
        initBlitPipeline(blitErr);
        if (!blitErr.empty()) {
            dbgLog("blitSurface: initBlitPipeline FAILED: %s", blitErr.c_str());
            err = "blitSurface: " + blitErr; return false;
        }
        const BlitPipeline* bp = getBlitPipeline();
        cmd->SetGraphicsRootSignature(bp->rootSig.Get());
        cmd->SetPipelineState(bp->pso.Get());
        dbgLogDebug("blitSurface: set blit rootSig=%p pso=%p",
            (void*)bp->rootSig.Get(), (void*)bp->pso.Get());

        // 3+4. 源纹理过渡 + SRV 分配 + 根描述符表绑定（一步完成）
        if (!blitBindSourceTexture(ctx, srcTex, cmd, err)) {
            dbgLog("blitSurface: blitBindSourceTexture FAILED: %s", err.c_str());
            return false;
        }

        // 5. 绑定顶点/索引缓冲并绘制
        cmd->IASetVertexBuffers(0, 1, &bp->vbView);
        cmd->IASetIndexBuffer(&bp->ibView);
        dbgLogDebug("blitSurface: drawIndexed inst=1 firstIdx=0 firstVert=0");
        cmd->DrawIndexedInstanced(6, 1, 0, 0, 0);
        dbgLogDebug("blitSurface: drawIndexed done");

        // 6. RENDER_TARGET → PRESENT
        transitionTo(ctx, dst, D3D12_RESOURCE_STATE_RENDER_TARGET, D3D12_RESOURCE_STATE_PRESENT);

        // 7. 源纹理 → COMMON（为下一帧 beginRenderPass 准备）
        if (srcTex) transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COMMON);
    }
#endif
    // P6 诊断：源纹理实际尺寸（拷贝是 min(源, backbuffer) 区域，若源比窗口小
    // 画面会留黑边；若源未渲染则纯色）。
    // srcTex 为 null 时（纯 clear 路径）跳过诊断日志，避免解引用空指针。
    if (srcTex && srcTex->resource) {
        D3D12_RESOURCE_DESC srcDesc = srcTex->resource->GetDesc();
        bool srcWasWritten = ctx->colorTargetsWritten;
        dbgLogDebug("blitSurface: ctx=%p ctxW=%d src=%p -> backbuf=%ux%u",
            (void*)ctx, (int)ctx->colorTargetsWritten, (void*)srcTex, w, h);
        dbgLogDebug("blitSurface: src=%p srcW=%llu srcH=%llu fmt=%d wasWritten=%d -> backbuf=%ux%u",
            (void*)srcTex, (unsigned long long)srcDesc.Width,
            (unsigned long long)srcDesc.Height, (int)srcTex->dxgiFormat,
            (int)srcWasWritten, w, h);
    } else {
        dbgLogDebug("blitSurface: srcTex=null (pure red clear), no src diagnostic");
    }
    // 记录本帧 blit 写入的 back buffer 下标（present 后 currentImageIndex=-1，
    // readback 必须用此值才能读到真实画面）。
    s->lastBlitIndex = s->currentImageIndex;
    return true;
}

void presentSurface(Dx12Surface* s) {
    if (!s || !s->swapChain) {
        return;
    }
    // IMMEDIATE=0 -> Present(0, ALLOW_TEARING)；FIFO_RELAXED=3 -> Present(1, 0)；
    // 其余（FIFO=2 等）-> Present(1, 0)。
    //
    // P65（全屏掉帧根因）：flip model 下，**全屏**（独立翻转/独占）时 syncInterval=0
    // 但不带 DXGI_PRESENT_ALLOW_TEARING 的 Present 仍被 DXGI 同步到垂直刷新——
    // 实测 1920x1081 每帧 Present 阻塞 ~5.6ms（~150FPS），而窗口模式走 DWM 合成
    // Present 立即返回（~0.15ms）故无此问题。传入 ALLOW_TEARING 后全屏不再同步到
    // vblank（等价官方 Vulkan 的 IMMEDIATE present，实测官方全屏 1022FPS）。
    // 前提：swapchain 创建时带 ALLOW_TEARING（s->allowTearing），且 syncInterval==0。
    UINT syncInterval = (s->presentMode == 0) ? 0 : 1;
    UINT flags = (s->presentMode == 0 && s->allowTearing) ? DXGI_PRESENT_ALLOW_TEARING : 0;
    // P15 诊断：每 30 帧打印 present 摘要（含 back buffer index + 结果）
    // P3.2 诊断：每帧检查 Backbuffer 格式和尺寸是否与窗口匹配
    if (!s->backBuffers.empty()) {
        ID3D12Resource* bb = s->backBuffers[(size_t)(s->currentImageIndex >= 0 ? s->currentImageIndex : 0)].Get();
        if (bb) {
            D3D12_RESOURCE_DESC bbDesc = bb->GetDesc();
            if ((s->currentImageIndex + 1) % 30 == 0) {
                dbgLogInfo("presentSurface: idx=%d sync=%u suboptimal=%d bbFmt=%d bbW=%llu bbH=%llu winW=%u winH=%u",
                    (int)s->currentImageIndex, syncInterval, (int)s->suboptimal,
                    (int)bbDesc.Format, (unsigned long long)bbDesc.Width, (unsigned long long)bbDesc.Height,
                    (unsigned)s->width, (unsigned)s->height);
            }
        }
    }
    if ((s->currentImageIndex + 1) % 30 == 0) {
        dbgLogInfo("presentSurface: idx=%d sync=%u flags=%u tearing=%d suboptimal=%d",
            (int)s->currentImageIndex, syncInterval, flags, (int)s->allowTearing, (int)s->suboptimal);
    }
    HRESULT hr = s->swapChain->Present(syncInterval, flags);
    // present 后 backbuffer 所有权已释放（vanilla 每帧 acquire->blit->present，
    // 若此处不重置，configureSurface 的 ResizeBuffers 会误以为仍有 acquired
    // backbuffer 而返回 DXGI_ERROR_NOT_CURRENTLY_AVAILABLE -> 画面冻结）。
    s->currentImageIndex = -1;
    if (hr == DXGI_STATUS_OCCLUDED) {
        dbgLog("presentSurface: OCCLUDED (window fully occluded) -> suboptimal");
        s->suboptimal = true;
    } else if (hr == DXGI_STATUS_MODE_CHANGED) {
        dbgLog("presentSurface: MODE_CHANGED -> suboptimal");
        s->suboptimal = true;
    } else if (FAILED(hr)) {
        dbgLog("presentSurface: FAILED %s", hrText(hr).c_str());
        s->suboptimal = true;
    } else {
        // 每帧成功路径：默认级别下静默（stderr/文件写位于帧关键路径上）。
        dbgLogDebug("presentSurface: ok (syncInterval=%u)", syncInterval);
        s->suboptimal = false;  // 正常 present 清除 suboptimal 标记
    }
}

void destroySurface(Dx12Surface* s) {
    if (!s) return;
    dbgLog("destroySurface: enter surface=%p", (void*)s);
    // P33 修复：async render thread 可能在 destroySurface 期间仍在操作同一 swapchain。
    // 先等待渲染线程完成当前帧（仅同步，不销毁线程），再等 GPU 空闲，
    // 避免 deviceWaitIdle → queue->Signal 与渲染线程的 Present 并发访问 swapchain 导致崩溃。
    waitForRenderThreadSubmit();
    std::string err;
    deviceWaitIdle(err);
    // P33 fix：等待 per-surface present fence，确保 Display Controller 已完成
    // flip 并释放 backbuffer 引用。IMMEDIATE 模式 + FLIP_DISCARD 下，deviceWaitIdle
    // 不等待 display controller，backbuffer 仍可能被 DWM 异步读取。
    if (!s->surfacePresentFences.empty()) {
        UINT64 pv = 0;
        for (UINT64 v : s->surfacePresentFences) if (v > pv) pv = v;
        if (pv > 0) {
            DeviceContext& dc = deviceContextForJni();
            if (dc.queueFence) {
                HANDLE hEvt = CreateEventW(nullptr, FALSE, FALSE, nullptr);
                if (hEvt) {
                    HRESULT hr = dc.queueFence->SetEventOnCompletion(pv, hEvt);
                    if (SUCCEEDED(hr)) {
                        WaitForSingleObject(hEvt, 5000);
                    }
                    CloseHandle(hEvt);
                }
            }
        }
    }
    // P33 fix：present fence 等 GPU 完成所有命令（含 present），此时再 flush
    // 延迟删除对象，确保不在 GPU 仍引用资源时释放它们（消除 PreSwapInfoQueue
    // CORRUPTION 警告的根本原因）。
    flushPendingDeletes();
    if (getActiveSurface() == s) setActiveSurface(nullptr);
    delete s;
    dbgLog("destroySurface: done surface=%p", (void*)s);
}

// 仅清理表面，不调用 deviceWaitIdle（供 C++ 内部使用：render thread 已自行退出后调用）
void destroySurfaceNoWaitIdle(Dx12Surface* s) {
    if (!s) return;
    if (getActiveSurface() == s) setActiveSurface(nullptr);
    delete s;
}

// P6 诊断：读回 back buffer 采样像素。内部先等 GPU 完全空闲（同步一帧），
// 用一次性命令列表拷贝到 readback staging，Map 后打印 3x3 网格 RGBA。
// 每 ~60 帧调用一次，同步开销可忽略。结论判读：
//   纯红/纯黑/纯灰 = 画面只有 clear 色（绘制内容不可见/未生效）
//   多色且中心有内容 = 渲染正常，问题在别处（blit 区域/尺寸等）。
bool readbackSurfacePixels(Dx12Surface* s, std::string& err) {
    DeviceContext& ctx = deviceContextForJni();
    if (!ctx.device || !ctx.queue) { err = "device not initialized"; return false; }
    if (!s || s->backBuffers.empty()) { err = "surface has no back buffers"; return false; }
    if (!deviceWaitIdle(err)) { err = "deviceWaitIdle failed: " + err; return false; }

    // 选择要读回的 back buffer：
    // 1. 优先 currentImageIndex：表示当前已 acquire、本帧正在使用的 backBuffer。
    //    游戏可能直接通过 beginRenderPass 渲染到表面纹理而非走 blit 路径，
    //    此时 lastBlitIndex 是旧帧的残留值，用它会读到过时数据（黑屏/错误内容）。
    // 2. 其次 lastBlitIndex：当 currentImageIndex == -1（present 后尚未 acquire
    //    新帧）时，用最近一次 blit 的 backBuffer 作为兜底，保证测试自检能读到
    //    真实画面——GetCurrentBackBufferIndex 会跳到下一帧未写入的 buffer（全 0 假黑屏）。
    int idx = s->currentImageIndex;
    if (idx < 0 || idx >= (int)s->backBuffers.size()) {
        if (s->lastBlitIndex >= 0 && s->lastBlitIndex < (int)s->backBuffers.size()) {
            idx = s->lastBlitIndex;
        } else {
            idx = (int)s->swapChain->GetCurrentBackBufferIndex();
        }
    }
    if (idx < 0 || idx >= (int)s->backBuffers.size()) {
        err = "no valid back buffer for readback (idx=" + std::to_string(idx) + ")";
        return false;
    }
    ID3D12Resource* bb = s->backBuffers[(size_t)idx].Get();
    D3D12_RESOURCE_DESC bd = bb->GetDesc();
    UINT w = (UINT)bd.Width, h = bd.Height;
    UINT64 rowBytes = (UINT64)w * 4;
    UINT64 pitch = (rowBytes + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1)
        & ~(UINT64)(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    UINT64 total = pitch * h;

    static ComPtr<ID3D12Resource> staging;
    if (!staging || staging->GetDesc().Width < total) {
        D3D12_RESOURCE_DESC desc{};
        desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        desc.Width = total;
        desc.Height = 1;
        desc.DepthOrArraySize = 1;
        desc.MipLevels = 1;
        desc.SampleDesc.Count = 1;
        desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        desc.Flags = D3D12_RESOURCE_FLAG_NONE;
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_READBACK;
        hp.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
        hp.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
        hp.CreationNodeMask = 0;
        hp.VisibleNodeMask = 0;
        if (FAILED(ctx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&staging)))) {
            err = "CreateCommittedResource(readback staging) failed";
            return false;
        }
    }

    static ComPtr<ID3D12CommandAllocator> alloc;
    static ComPtr<ID3D12GraphicsCommandList> cl;
    if (!alloc) {
        if (FAILED(ctx.device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
            IID_PPV_ARGS(&alloc)))) { err = "CreateCommandAllocator failed"; return false; }
    }
    if (!cl) {
        if (FAILED(ctx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&cl)))) { err = "CreateCommandList failed"; return false; }
    } else {
        alloc->Reset();
        cl->Reset(alloc.Get(), nullptr);
    }

    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = bb;
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COMMON;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_SOURCE;
    cl->ResourceBarrier(1, &b);

    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = bb;
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = 0;
    D3D12_TEXTURE_COPY_LOCATION dst{};
    dst.pResource = staging.Get();
    dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    dst.PlacedFootprint.Offset = 0;
    dst.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    dst.PlacedFootprint.Footprint.Width = w;
    dst.PlacedFootprint.Footprint.Height = h;
    dst.PlacedFootprint.Footprint.Depth = 1;
    dst.PlacedFootprint.Footprint.RowPitch = (UINT)pitch;
    cl->CopyTextureRegion(&dst, 0, 0, 0, &src, nullptr);

    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_SOURCE;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
    cl->ResourceBarrier(1, &b);
    cl->Close();

    ID3D12CommandList* lists[] = { cl.Get() };
    ctx.queue->ExecuteCommandLists(1, lists);
    UINT64 fv = ++ctx.queueFenceValue;
    if (FAILED(ctx.queue->Signal(ctx.queueFence.Get(), fv))) {
        err = "Signal(readback) failed"; return false;
    }
    if (!waitForQueueFenceValue(fv, 5'000'000'000ULL, err)) {
        err = "readback wait timeout: " + err; return false;
    }

    void* ptr = nullptr;
    if (FAILED(staging->Map(0, nullptr, &ptr))) { err = "staging Map failed"; return false; }
    const uint8_t* base = (const uint8_t*)ptr;
    int xs[3] = { 0, (int)w / 2, (int)w - 1 };
    int ys[3] = { 0, (int)h / 2, (int)h - 1 };
    for (int yi = 0; yi < 3; ++yi) {
        for (int xi = 0; xi < 3; ++xi) {
            const uint8_t* p = base + (UINT64)ys[yi] * pitch + (UINT64)xs[xi] * 4;
            dbgLog("readback[%ux%u] (%d,%d) = RGBA(%3d,%3d,%3d,%3d)",
                w, h, xs[xi], ys[yi], p[0], p[1], p[2], p[3]);
        }
    }
    // P6 可视化：整帧 dump 成 BMP + ASCII 缩略图，直接看 backbuffer 实际画面。
    dbgDumpPixelsToFile(base, w, h, pitch, "backbuf");
    staging->Unmap(0, nullptr);
    return true;
}

}  // namespace dx12mc
