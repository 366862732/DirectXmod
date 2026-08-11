// dx12-mc 原生 D3D12 层（C++）
// P5 目标：DXGI swapchain（GpuSurfaceBackend 对应物）。
// 设计参考：官方 com.mojang.blaze3d.vulkan.VulkanGpuSurface。
//
// PresentMode 序数 = 官方枚举 ordinal：
//   IMMEDIATE=0, MAILBOX=1, FIFO=2, FIFO_RELAXED=3
// DXGI FLIP 模型支持 {IMMEDIATE, FIFO, FIFO_RELAXED}（MAILBOX 无直接对应）。

#include "dx12_device.h"

#include <dxgi.h>
#include <dxgi1_4.h>

#include <cstdio>
#include <cstring>
#include <sstream>
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

// 取 surface 的 back buffer index 对应的 RTV（blit 后可用；P5 自检不用）。
}  // namespace

Dx12Surface* createSurface(uintptr_t hwnd, std::string& err) {
    DeviceContext& ctx = deviceContextForJni();
    if (!ctx.device || !ctx.queue) {
        err = "device not initialized (call dx12CreateDevice first)";
        return nullptr;
    }

    ComPtr<IDXGIFactory4> factory;
    HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(&factory));
    if (FAILED(hr)) {
        err = "CreateDXGIFactory1 failed " + hrText(hr);
        return nullptr;
    }

    DXGI_SWAP_CHAIN_DESC1 sd{};
    sd.Width = 1;                    // 占位；configure() 时 ResizeBuffers 到实际尺寸
    sd.Height = 1;
    sd.Format = DXGI_FORMAT_R8G8B8A8_UNORM;  // 与 MC RGBA8 中间纹理同族，CopyTextureRegion 可直接拷贝
    sd.Stereo = FALSE;
    sd.SampleDesc.Count = 1;
    sd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    sd.BufferCount = kSurfaceBufferCount;
    sd.Scaling = DXGI_SCALING_STRETCH;
    sd.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
    sd.AlphaMode = DXGI_ALPHA_MODE_UNSPECIFIED;
    sd.Flags = DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING;  // FIFO_RELAXED 需要

    ComPtr<IDXGISwapChain1> swapChain1;
    hr = factory->CreateSwapChainForHwnd(ctx.queue.Get(), reinterpret_cast<HWND>(hwnd),
        &sd, nullptr, nullptr, &swapChain1);
    if (FAILED(hr)) {
        err = "CreateSwapChainForHwnd failed " + hrText(hr);
        return nullptr;
    }

    ComPtr<IDXGISwapChain3> swapChain3;
    if (FAILED(swapChain1.As(&swapChain3))) {
        err = "swapchain does not support IDXGISwapChain3";
        return nullptr;
    }

    Dx12Surface* s = new Dx12Surface();
    s->swapChain = swapChain3;
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
    if (width <= 0 || height <= 0) {
        s->presentMode = presentMode;
        return true;
    }
    s->width = (UINT)width;
    s->height = (UINT)height;
    s->presentMode = presentMode;

    // ResizeBuffers 前必须等 GPU 完全空闲：FLIP model 下 backbuffer 仍被
    // 上一帧命令队列引用时，ResizeBuffers 返回 DXGI_ERROR_NOT_CURRENTLY_AVAILABLE
    // (0x887A0001)——游戏启动/窗口调整时多次 configure 失败即此原因。
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
    // 使用 swap chain 创建时的格式，而非 s->format（后者可能因内存损坏/误用而变为无效值，
    // 导致 ResizeBuffers 以 0x887A0001 (DXGI_ERROR_INVALID_CALL) 失败）。
    const DXGI_FORMAT scFmt = DXGI_FORMAT_R8G8B8A8_UNORM;
    HRESULT hr = S_OK;
    // deviceWaitIdle 完成后 DWM 合成器可能仍在异步持有 backbuffer 引用（flip model
    // + 窗口模式的已知竞态）。等待几毫秒后重试，最多 3 次。
    for (int retry = 0; retry < 3; ++retry) {
        if (retry > 0) {
            dbgLog("configureSurface: retry %d after %dms", retry, retry * 16);
            std::this_thread::sleep_for(std::chrono::milliseconds(retry * 16));
        }
        hr = s->swapChain->ResizeBuffers(kSurfaceBufferCount, (UINT)width, (UINT)height,
            scFmt, DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING);
        if (SUCCEEDED(hr)) {
            dbgLog("configureSurface: ResizeBuffers ok (retry=%d)", retry);
            break;
        }
        dbgLog("configureSurface: ResizeBuffers failed %s (retry=%d)", hrText(hr).c_str(), retry);
        if (hr != DXGI_ERROR_NOT_CURRENTLY_AVAILABLE) break;  // 非竞态错误，直接失败
    }
    // 重试全部失败：DWM 仍持有 backbuffer。获取窗口句柄并 destroy+recreate swapchain。
    if (FAILED(hr)) {
        HWND hwnd = nullptr;
        s->swapChain->GetHwnd(&hwnd);
        dbgLog("configureSurface: recreate swapchain hwnd=%p w=%d h=%d", (void*)hwnd, width, height);
        ComPtr<IDXGIFactory4> factory;
        HRESULT hrFactory = CreateDXGIFactory1(IID_PPV_ARGS(&factory));
        if (SUCCEEDED(hrFactory) && hwnd) {
            DXGI_SWAP_CHAIN_DESC1 sd{};
            sd.Width = 1;
            sd.Height = 1;
            sd.Format = scFmt;
            sd.Stereo = FALSE;
            sd.SampleDesc.Count = 1;
            sd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
            sd.BufferCount = kSurfaceBufferCount;
            sd.Scaling = DXGI_SCALING_STRETCH;
            sd.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
            sd.AlphaMode = DXGI_ALPHA_MODE_UNSPECIFIED;
            sd.Flags = DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING;
            ComPtr<IDXGISwapChain1> newSwapChain1;
            HRESULT hrNew = factory->CreateSwapChainForHwnd(
                ctx.queue.Get(), hwnd, &sd, nullptr, nullptr, &newSwapChain1);
            if (SUCCEEDED(hrNew)) {
                ComPtr<IDXGISwapChain3> newSwapChain3;
                if (SUCCEEDED(newSwapChain1.As(&newSwapChain3))) {
                    s->swapChain = newSwapChain3;
                    hr = s->swapChain->ResizeBuffers(
                        kSurfaceBufferCount, (UINT)width, (UINT)height,
                        scFmt, DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING);
                    if (SUCCEEDED(hr)) {
                        dbgLog("configureSurface: swapchain recreated ok");
                    } else {
                        dbgLog("configureSurface: recreate resize failed %s", hrText(hr).c_str());
                    }
                }
            }
        }
    }
    if (FAILED(hr)) {
        err = "ResizeBuffers/recreate failed w=" + std::to_string(width) +
            " h=" + std::to_string(height) +
            " count=" + std::to_string(kSurfaceBufferCount) +
            " fmt=" + std::to_string((int)scFmt);
        dbgLog("configureSurface: FAILED %s", err.c_str());
        return false;
    }
    dbgLog("configureSurface: ResizeBuffers ok");

    // 重新取 back buffers + RTV
    s->backBuffers.resize(kSurfaceBufferCount);
    s->rtvHandles.clear();
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
    return true;
}

bool acquireSurface(Dx12Surface* s, std::string& err) {
    if (!s || !s->swapChain) {
        err = "surface not created";
        return false;
    }
    s->currentImageIndex = (int)s->swapChain->GetCurrentBackBufferIndex();
    if (s->currentImageIndex < 0 ||
        s->currentImageIndex >= (int)s->backBuffers.size()) {
        err = "GetCurrentBackBufferIndex returned an invalid index";
        return false;
    }
    // 同步 lastBlitIndex：当游戏直接通过 beginRenderPass 渲染到表面纹理
    //（而非走 blitSurface 路径）时，readbackSurfacePixels 需要用最新的
    // currentImageIndex 才能读到正确的 backBuffer，否则会读到上一帧 blit 的
    // 旧数据（表现为黑屏/错误内容）。
    s->lastBlitIndex = s->currentImageIndex;
    return true;
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
    if (!srcTex || !srcTex->resource) {
        err = "null source texture";
        return false;
    }
    if (!ctx->listOpen) {
        err = "command list not open (call dx12BeginCommandList first)";
        return false;
    }

    ID3D12GraphicsCommandList* cmd = ctx->commandList.Get();
    ID3D12Resource* dst = s->backBuffers[(size_t)s->currentImageIndex].Get();
    UINT w = s->width;
    UINT h = s->height;

    // 源纹理可能是本帧渲染 pass 的输出（RENDER_TARGET/DEPTH_WRITE），或刚
    // 上传完的 COMMON；按跟踪状态过渡到 COPY_SOURCE 再拷贝。
    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COPY_SOURCE);

    D3D12_RESOURCE_BARRIER barrier{};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrier.Transition.pResource = dst;
    barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    barrier.Transition.StateBefore = D3D12_RESOURCE_STATE_PRESENT;
    barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_DEST;
    cmd->ResourceBarrier(1, &barrier);

    D3D12_TEXTURE_COPY_LOCATION srcLoc{};
    srcLoc.pResource = srcTex->resource.Get();
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    srcLoc.SubresourceIndex = 0;

    D3D12_TEXTURE_COPY_LOCATION dstLoc{};
    dstLoc.pResource = dst;
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    D3D12_BOX srcBox{0, 0, 0, w, h, 1};
    cmd->CopyTextureRegion(&dstLoc, 0, 0, 0, &srcLoc, &srcBox);

    barrier.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_DEST;
    barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
    cmd->ResourceBarrier(1, &barrier);
    // P11：源纹理显式回切 COMMON。上方是"显式进入" COPY_SOURCE（非隐式提升），
    // 显式进入的状态不会随命令列表完成 decay，若残留到下一 command list，
    // 其 beginRenderPass/blit 按 COMMON 写 Before 状态就会错配（验证 ERROR →
    // GPU 状态错乱 → TDR 冻结）。与 dbgReadbackTexturePixels 的
    // COMMON→COPY_SOURCE→COMMON 显式配对同一模式。
    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COMMON);
    // P6 诊断：源纹理实际尺寸（拷贝是 min(源, backbuffer) 区域，若源比窗口小
    // 画面会留黑边；若源未渲染则纯色）。
    D3D12_RESOURCE_DESC srcDesc = srcTex->resource->GetDesc();
    dbgLog("blitSurface: src=%p srcW=%llu srcH=%llu -> backbuf=%ux%u",
        (void*)srcTex, (unsigned long long)srcDesc.Width,
        (unsigned long long)srcDesc.Height, w, h);
    // 记录本帧 blit 写入的 back buffer 下标（present 后 currentImageIndex=-1，
    // readback 必须用此值才能读到真实画面）。
    s->lastBlitIndex = s->currentImageIndex;
    return true;
}

void presentSurface(Dx12Surface* s) {
    if (!s || !s->swapChain) {
        return;
    }
    // IMMEDIATE=0 -> Present(0,0)；FIFO_RELAXED=3 -> Present(1, ALLOW_TEARING)；
    // 其余（FIFO=2 等）-> Present(1, 0)。
    UINT syncInterval = (s->presentMode == 0) ? 0 : 1;
    UINT flags = (s->presentMode == 3) ? DXGI_PRESENT_ALLOW_TEARING : 0;
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
        dbgLog("presentSurface: ok (syncInterval=%u)", syncInterval);
        s->suboptimal = false;  // 正常 present 清除 suboptimal 标记
    }
}

void destroySurface(Dx12Surface* s) {
    if (!s) return;
    dbgLog("destroySurface: enter surface=%p", (void*)s);
    // GPU 可能仍在写入 backbuffer；先等队列空闲再销毁 swapchain，
    // 否则资源在使用中被释放会触发 DXGI_ERROR_DEVICE_REMOVED。
    std::string err;
    deviceWaitIdle(err);
    delete s;
    dbgLog("destroySurface: done surface=%p", (void*)s);
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
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_PRESENT;
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
