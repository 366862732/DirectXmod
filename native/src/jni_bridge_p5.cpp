// JNI 桥（P5：DXGI swapchain surface）。
// 与 Java 侧 com.dx12.dx12.Dx12Native 的 native 声明一一对应。
// 独立文件避免与 jni_bridge.cpp 冲突（IDE 曾锁定该文件）。

#include <jni.h>

#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

#include "dx12_device.h"

using namespace dx12mc;

namespace {

Dx12Surface* toSurface(jlong handle) {
    return reinterpret_cast<Dx12Surface*>(static_cast<uintptr_t>(handle));
}

CommandContext* toCtx(jlong handle) {
    return reinterpret_cast<CommandContext*>(static_cast<uintptr_t>(handle));
}

Dx12Object* toObject(jlong handle) {
    return reinterpret_cast<Dx12Object*>(static_cast<uintptr_t>(handle));
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12CreateSurface(
    JNIEnv* env, jclass, jlong hwnd) {
    std::string err;
    Dx12Surface* s = createSurface(static_cast<uintptr_t>(hwnd), err);
    if (!s) {
        std::fprintf(stderr, "[dx12] dx12CreateSurface: %s\n", err.c_str());
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(s));
}

JNIEXPORT jintArray JNICALL Java_com_dx12_dx12_Dx12Native_dx12SurfacePresentModes(
    JNIEnv* env, jclass) {
    std::vector<int> modes = surfacePresentModes();
    jintArray out = env->NewIntArray((jsize)modes.size());
    if (out) {
        env->SetIntArrayRegion(out, 0, (jsize)modes.size(), modes.data());
    }
    return out;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12ConfigureSurface(
    JNIEnv* env, jclass, jlong surface, jint width, jint height, jint presentMode) {
    std::string err;
    if (!configureSurface(toSurface(surface), width, height, presentMode, err)) {
        // 此前仅 fprintf(stderr) -> 不进 dx12-native.log；configure 失败正是
        // MC surfaceIsInvalid -> 画面冻结的根因，必须镜像到 native log。
        dbgLog("dx12ConfigureSurface FAILED %dx%d mode=%d: %s",
            width, height, presentMode, err.c_str());
        return JNI_FALSE;
    }
    dbgLogInfo("dx12ConfigureSurface: ok %dx%d mode=%d", width, height, presentMode);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12AcquireSurface(
    JNIEnv* env, jclass, jlong surface) {
    std::string err;
    if (!acquireSurface(toSurface(surface), err)) {
        std::fprintf(stderr, "[dx12] dx12AcquireSurface: %s\n", err.c_str());
        return JNI_FALSE;
    }
    dbgLog("acquireSurface: ok surface=%p", toSurface(surface));
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12BlitSurface(
    JNIEnv* env, jclass, jlong ctx, jlong surface, jlong texture) {
    std::string err;
    if (!blitSurface(toCtx(ctx), toSurface(surface), toObject(texture), err)) {
        std::fprintf(stderr, "[dx12] dx12BlitSurface: %s\n", err.c_str());
        return;
    }
    dbgLogInfo("blitSurface: ok ctx=%p surface=%p texture=%p",
        toCtx(ctx), toSurface(surface), toObject(texture));
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12PresentSurface(
    JNIEnv*, jclass, jlong surface) {
    presentSurface(toSurface(surface));
    // 注意：present 的真实结果（ok / OCCLUDED / MODE_CHANGED / FAILED）由
    // presentSurface() 内部 dbgLog 打印；此处不再无条件打 "ok"（会掩盖
    // OCCLUDED/MODE_CHANGED 导致 MC 误判 suboptimal 的根因）。
}

// 返回当前 acquire 的 back buffer 原始资源指针，供 Java 侧直接 blit/readback。
JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12GetBackBufferHandle(
    JNIEnv*, jclass, jlong surface) {
    return (jlong)getBackBufferHandle(toSurface(surface));
}

// P6 诊断：读回指定纹理 3x3 采样像素，返回 Java int[]（[r,g,b,a] × 9）。
// 用此函数直接读 render pass 的 colorTexture，判断渲染写入是否成功。
JNIEXPORT jintArray JNICALL Java_com_dx12_dx12_Dx12Native_dx12ReadbackTexturePixels(
    JNIEnv* env, jclass, jlong texHandle) {
    std::string err;
    DeviceContext& ctx = deviceContextForJni();
    Dx12Object* tex = toObject(texHandle);
    if (!ctx.device || !ctx.queue || !tex || tex->kind != Dx12Object::Kind::Texture || !tex->resource) {
        std::fprintf(stderr, "[dx12] dx12ReadbackTexturePixels: invalid handle=%p kind=%d\n",
            tex, (int)(tex ? (int)tex->kind : -1));
        return nullptr;
    }
    if (!deviceWaitIdle(err)) {
        std::fprintf(stderr, "[dx12] dx12ReadbackTexturePixels: waitIdle failed: %s\n", err.c_str());
        return nullptr;
    }
    ID3D12Resource* r = tex->resource.Get();
    D3D12_RESOURCE_DESC td = r->GetDesc();
    UINT w = (UINT)td.Width, h = (UINT)td.Height;
    if (w == 0 || h == 0 || w > 16384 || h > 16384) {
        std::fprintf(stderr, "[dx12] dx12ReadbackTexturePixels: bad dims %ux%u\n", w, h);
        return nullptr;
    }
    DXGI_FORMAT fmt = td.Format;
    UINT bpp = 4;
    DXGI_FORMAT fbFmt = DXGI_FORMAT_R8G8B8A8_UNORM;
    switch (fmt) {
        case DXGI_FORMAT_R8G8B8A8_UNORM:
        case DXGI_FORMAT_R8G8B8A8_UINT:
        case DXGI_FORMAT_B8G8R8A8_UNORM:
        case DXGI_FORMAT_R8G8B8A8_SNORM:
        case DXGI_FORMAT_R8G8B8A8_SINT:
            bpp = 4; fbFmt = DXGI_FORMAT_R8G8B8A8_UNORM; break;
        case DXGI_FORMAT_R16G16B16A16_UNORM:
        case DXGI_FORMAT_R16G16B16A16_SNORM:
        case DXGI_FORMAT_R16G16B16A16_UINT:
        case DXGI_FORMAT_R16G16B16A16_SINT:
        case DXGI_FORMAT_R16G16B16A16_FLOAT:
            bpp = 8; fbFmt = DXGI_FORMAT_R16G16B16A16_UNORM; break;
        case DXGI_FORMAT_R32G32B32A32_FLOAT:
        case DXGI_FORMAT_R32G32B32A32_UINT:
        case DXGI_FORMAT_R32G32B32A32_SINT:
            bpp = 16; fbFmt = DXGI_FORMAT_R32G32_FLOAT; break;
        default:
            bpp = 4; fbFmt = DXGI_FORMAT_R8G8B8A8_UNORM; break;
    }
    UINT64 rowBytes = (UINT64)w * bpp;
    UINT64 pitch = (rowBytes + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1)
        & ~(UINT64)(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    UINT64 total = pitch * h;

    static ComPtr<ID3D12Resource> staging;
    static ComPtr<ID3D12CommandAllocator> alloc;
    static ComPtr<ID3D12GraphicsCommandList> cl;
    if (!staging || staging->GetDesc().Width < total) {
        D3D12_RESOURCE_DESC desc{};
        desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        desc.Width = total; desc.Height = 1; desc.DepthOrArraySize = 1;
        desc.MipLevels = 1; desc.SampleDesc.Count = 1;
        desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        desc.Flags = D3D12_RESOURCE_FLAG_NONE;
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_READBACK;
        if (FAILED(ctx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&staging)))) {
            std::fprintf(stderr, "[dx12] readback staging create failed\n");
            return nullptr;
        }
    }
    if (!alloc) {
        ctx.device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&alloc));
    }
    if (!cl) {
        ctx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&cl));
    } else {
        alloc->Reset();
        cl->Reset(alloc.Get(), nullptr);
    }
    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = r;
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COMMON;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_SOURCE;
    cl->ResourceBarrier(1, &b);
    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = r;
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = 0;
    D3D12_TEXTURE_COPY_LOCATION dst{};
    dst.pResource = staging.Get();
    dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    dst.PlacedFootprint.Offset = 0;
    dst.PlacedFootprint.Footprint.Format = fbFmt;
    dst.PlacedFootprint.Footprint.Width = w;
    dst.PlacedFootprint.Footprint.Height = h;
    dst.PlacedFootprint.Footprint.Depth = 1;
    dst.PlacedFootprint.Footprint.RowPitch = (UINT)pitch;
    cl->CopyTextureRegion(&dst, 0, 0, 0, &src, nullptr);
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_SOURCE;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_COMMON;
    cl->ResourceBarrier(1, &b);
    cl->Close();
    ID3D12CommandList* lists[] = { cl.Get() };
    ctx.queue->ExecuteCommandLists(1, lists);
    UINT64 fv = ++ctx.queueFenceValue;
    ctx.queue->Signal(ctx.queueFence.Get(), fv);
    if (!waitForQueueFenceValue(fv, 5'000'000'000ULL, err)) {
        std::fprintf(stderr, "[dx12] dx12ReadbackTexturePixels: wait timeout\n");
        return nullptr;
    }
    void* ptr = nullptr;
    if (FAILED(staging->Map(0, nullptr, &ptr))) {
        std::fprintf(stderr, "[dx12] dx12ReadbackTexturePixels: Map failed\n");
        return nullptr;
    }
    const uint8_t* base = (const uint8_t*)ptr;
    int xs[3] = { 0, (int)w / 2, (int)w - 1 };
    int ys[3] = { 0, (int)h / 2, (int)h - 1 };
    jintArray arr = env->NewIntArray(36);
    if (arr) {
        jint* pixels = env->GetIntArrayElements(arr, nullptr);
        for (int yi = 0; yi < 3; ++yi) {
            for (int xi = 0; xi < 3; ++xi) {
                const uint8_t* p = base + (UINT64)ys[yi] * pitch + (UINT64)xs[xi] * bpp;
                int idx2 = (yi * 3 + xi) * 4;
                pixels[idx2 + 0] = p[0];
                pixels[idx2 + 1] = p[1];
                pixels[idx2 + 2] = p[2];
                pixels[idx2 + 3] = p[3];
            }
        }
        env->ReleaseIntArrayElements(arr, pixels, 0);
        std::fprintf(stderr, "[dx12-java] rbTex %dx%d center=(%d,%d) RGBA=(%d,%d,%d,%d) black=%d\n",
            w, h, xs[1], ys[1],
            base[ys[1]*pitch+xs[1]*bpp], base[ys[1]*pitch+xs[1]*bpp+1],
            base[ys[1]*pitch+xs[1]*bpp+2], base[ys[1]*pitch+xs[1]*bpp+3],
            (base[0]==0&&base[1]==0&&base[2]==0&&base[3]==0) ? 1 : 0);
    }
    staging->Unmap(0, nullptr);
    return arr;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12IsSurfaceSuboptimal(
    JNIEnv* env, jclass, jlong surface) {
    Dx12Surface* s = toSurface(surface);
    return s ? (s->suboptimal ? JNI_TRUE : JNI_FALSE) : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12DestroySurface(
    JNIEnv*, jclass, jlong surface) {
    destroySurface(toSurface(surface));
}

// P6 诊断：返回当前 active surface 的 native handle（用于 readback 游戏实际画面）。
// getActiveSurface() 在 acquireSurface 时被设置为最近创建/acquire 的 surface。
JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12GetActiveSurfaceHandle(
    JNIEnv*, jclass) {
    Dx12Surface* s = getActiveSurface();
    return s ? static_cast<jlong>(reinterpret_cast<uintptr_t>(s)) : 0L;
}

// P6 诊断：读回当前 back buffer 采样像素，返回 Java int[]（[r,g,b,a] × 9）。
// 用 JNI 直接传值进 Java，避免 fprintf(stderr) 被 PCL 启动器丢弃。
JNIEXPORT jintArray JNICALL Java_com_dx12_dx12_Dx12Native_dx12ReadbackSurfacePixels(
    JNIEnv* env, jclass, jlong surface) {
    std::string err;
    DeviceContext& ctx = deviceContextForJni();
    Dx12Surface* s = toSurface(surface);
    if (!ctx.device || !ctx.queue || !s || s->backBuffers.empty()) {
        std::fprintf(stderr, "[dx12] dx12ReadbackSurfacePixels: %s\n",
            (!ctx.device || !ctx.queue) ? "device not initialized" : "surface has no back buffers");
        return nullptr;
    }
    if (!deviceWaitIdle(err)) {
        std::fprintf(stderr, "[dx12] dx12ReadbackSurfacePixels: deviceWaitIdle failed: %s\n", err.c_str());
        return nullptr;
    }
    int idx = s->currentImageIndex;
    if (idx < 0 || idx >= (int)s->backBuffers.size()) {
        if (s->lastBlitIndex >= 0 && s->lastBlitIndex < (int)s->backBuffers.size()) {
            idx = s->lastBlitIndex;
        } else {
            idx = (int)s->swapChain->GetCurrentBackBufferIndex();
        }
    }
    if (idx < 0 || idx >= (int)s->backBuffers.size()) {
        std::fprintf(stderr, "[dx12] dx12ReadbackSurfacePixels: no valid back buffer (idx=%d)\n", idx);
        return nullptr;
    }
    ID3D12Resource* bb = s->backBuffers[(size_t)idx].Get();
    D3D12_RESOURCE_DESC bd = bb->GetDesc();
    UINT w = (UINT)bd.Width, h = bd.Height;
    // P6 诊断：打印当前读回的 surface 地址和尺寸，确认是哪个 surface
    std::fprintf(stderr, "[dx12] readback surf=0x%p idx=%d w=%u h=%u\n",
        (void*)s, idx, w, h);

    // 复用 staging + allocator（静态缓存）
    static ComPtr<ID3D12Resource> staging;
    static ComPtr<ID3D12CommandAllocator> alloc;
    static ComPtr<ID3D12GraphicsCommandList> cl;
    UINT64 rowBytes = (UINT64)w * 4;
    UINT64 pitch = (rowBytes + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1)
        & ~(UINT64)(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    UINT64 total = pitch * h;
    if (!staging || staging->GetDesc().Width < total) {
        D3D12_RESOURCE_DESC desc{};
        desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        desc.Width = total; desc.Height = 1; desc.DepthOrArraySize = 1;
        desc.MipLevels = 1; desc.SampleDesc.Count = 1;
        desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        desc.Flags = D3D12_RESOURCE_FLAG_NONE;
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_READBACK;
        if (FAILED(ctx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&staging)))) {
            std::fprintf(stderr, "[dx12] readback staging create failed\n");
            return nullptr;
        }
    }
    if (!alloc) {
        ctx.device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&alloc));
    }
    if (!cl) {
        ctx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&cl));
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
    ctx.queue->Signal(ctx.queueFence.Get(), fv);
    if (!waitForQueueFenceValue(fv, 5'000'000'000ULL, err)) {
        std::fprintf(stderr, "[dx12] readback wait timeout\n");
        return nullptr;
    }
    void* ptr = nullptr;
    if (FAILED(staging->Map(0, nullptr, &ptr))) {
        std::fprintf(stderr, "[dx12] readback Map failed\n");
        return nullptr;
    }
    const uint8_t* base = (const uint8_t*)ptr;
    int xs[3] = { 0, (int)w / 2, (int)w - 1 };
    int ys[3] = { 0, (int)h / 2, (int)h - 1 };
    jintArray arr = env->NewIntArray(36);  // 9 pixels × 4 components
    if (!arr) {
        std::fprintf(stderr, "[dx12] readback: NewIntArray(36) failed\n");
    } else {
        jint* pixels = env->GetIntArrayElements(arr, nullptr);
        for (int yi = 0; yi < 3; ++yi) {
            for (int xi = 0; xi < 3; ++xi) {
                const uint8_t* p = base + (UINT64)ys[yi] * pitch + (UINT64)xs[xi] * 4;
                int idx2 = (yi * 3 + xi) * 4;
                pixels[idx2 + 0] = p[0];   // R
                pixels[idx2 + 1] = p[1];   // G
                pixels[idx2 + 2] = p[2];   // B
                pixels[idx2 + 3] = p[3];   // A
            }
        }
        env->ReleaseIntArrayElements(arr, pixels, 0);
        std::fprintf(stderr, "[dx12-java] readback %dx%d center=(%d,%d)=(%d,%d,%d,%d) corners=%d black=%d\n",
            w, h, xs[1], ys[1],
            base[ys[1]*pitch+xs[1]*4], base[ys[1]*pitch+xs[1]*4+1],
            base[ys[1]*pitch+xs[1]*4+2], base[ys[1]*pitch+xs[1]*4+3],
            (base[0]!=0||base[1]!=0||base[2]!=0) ? 1 : 0,
            (base[0]==0&&base[1]==0&&base[2]==0&&base[3]==0) ? 1 : 0);
    }
    staging->Unmap(0, nullptr);
    return arr;
}

}  // extern "C"
