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
    s->width = (UINT)width;
    s->height = (UINT)height;
    s->presentMode = presentMode;

    HRESULT hr = s->swapChain->ResizeBuffers(kSurfaceBufferCount, (UINT)width, (UINT)height,
        s->format, DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING);
    if (FAILED(hr)) {
        err = "ResizeBuffers failed " + hrText(hr);
        return false;
    }

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
    if (hr == DXGI_STATUS_OCCLUDED || hr == DXGI_STATUS_MODE_CHANGED) {
        s->suboptimal = true;
    } else if (FAILED(hr)) {
        std::fprintf(stderr, "[dx12] Present failed %s\n", hrText(hr).c_str());
    }
}

void destroySurface(Dx12Surface* s) {
    if (!s) return;
    // GPU 可能仍在写入 backbuffer；先等队列空闲再销毁 swapchain，
    // 否则资源在使用中被释放会触发 DXGI_ERROR_DEVICE_REMOVED。
    std::string err;
    deviceWaitIdle(err);
    delete s;
}

}  // namespace dx12mc
