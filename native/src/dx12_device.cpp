#include "dx12_device.h"

#include <dxgi.h>

#include <cstdio>
#include <cstring>
#include <memory>
#include <sstream>
#include <vector>

namespace dx12mc {

namespace {

DeviceContext gCtx;

// 描述符堆槽位分配（P2 简单递增；P3 渲染层再做槽位回收）
UINT gNextSrv = 0;
UINT gNextRtv = 0;
UINT gNextDsv = 0;
UINT gNextSampler = 0;

constexpr UINT kSrvHeapSize = 4096;
constexpr UINT kRtvHeapSize = 512;
constexpr UINT kDsvHeapSize = 64;
constexpr UINT kSamplerHeapSize = 256;

bool createHeap(ID3D12Device* dev, D3D12_DESCRIPTOR_HEAP_TYPE type,
    UINT count, D3D12_DESCRIPTOR_HEAP_FLAGS flags, ComPtr<ID3D12DescriptorHeap>& out) {
    D3D12_DESCRIPTOR_HEAP_DESC desc{};
    desc.Type = type;
    desc.NumDescriptors = count;
    desc.Flags = flags;
    return SUCCEEDED(dev->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&out)));
}

std::string hrText(HRESULT hr) {
    char buf[64];
    snprintf(buf, sizeof(buf), "HRESULT 0x%08lX", (unsigned long)hr);
    return buf;
}

// 创建 adapter 查询用的 factory + LUID 适配器枚举
std::string queryAdapterName(ID3D12Device* dev) {
    try {
        ComPtr<IDXGIFactory4> factory;
        if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) return "<factory failed>";
        ComPtr<IDXGIAdapter> adapter;
        if (FAILED(factory->EnumAdapterByLuid(dev->GetAdapterLuid(), IID_PPV_ARGS(&adapter))))
            return "<enum adapter failed>";
        DXGI_ADAPTER_DESC desc{};
        if (FAILED(adapter->GetDesc(&desc))) return "<get desc failed>";
        char name[256] = {};
        WideCharToMultiByte(CP_UTF8, 0, desc.Description, -1, name, sizeof(name), nullptr, nullptr);
        return name;
    } catch (...) {
        return "<exception>";
    }
}

}  // namespace

// ---------------------------------------------------------------------------
// 设备生命周期
// ---------------------------------------------------------------------------

bool ensureDevice(std::string& errorOut) {
    if (gCtx.device) return true;

    const D3D_FEATURE_LEVEL levels[] = {
        D3D_FEATURE_LEVEL_12_1, D3D_FEATURE_LEVEL_12_0,
        D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_11_0,
    };
    ComPtr<ID3D12Device> device;
    for (auto level : levels) {
        if (SUCCEEDED(D3D12CreateDevice(nullptr, level, IID_PPV_ARGS(&device)))) {
            gCtx.featureLevel = level;
            break;
        }
    }
    if (!device) {
        errorOut = "D3D12CreateDevice failed at all feature levels";
        return false;
    }
    gCtx.device = device;
    gCtx.adapterName = queryAdapterName(device.Get());

    // 命令队列（P3 提交用）
    D3D12_COMMAND_QUEUE_DESC qd{};
    qd.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    if (FAILED(device->CreateCommandQueue(&qd, IID_PPV_ARGS(&gCtx.queue)))) {
        errorOut = "CreateCommandQueue failed";
        return false;
    }
    // 时间戳频率（DeviceInfo.timestampPeriod = 1/freq 用）；失败则保持 0。
    gCtx.queue->GetTimestampFrequency(&gCtx.timestampFrequency);

    // 描述符堆
    if (!createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV,
            kSrvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.srvHeap) ||
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_RTV,
            kRtvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_NONE, gCtx.rtvHeap) ||
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_DSV,
            kDsvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_NONE, gCtx.dsvHeap) ||
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER,
            kSamplerHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.samplerHeap)) {
        errorOut = "CreateDescriptorHeap failed";
        return false;
    }
    gCtx.srvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    gCtx.rtvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    gCtx.dsvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_DSV);
    gCtx.samplerInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER);
    return true;
}

void destroyDevice() {
    gCtx = DeviceContext{};
}

DeviceContext& deviceContextForJni() {
    return gCtx;
}

// ---------------------------------------------------------------------------
// 格式映射：官方 GpuFormat ordinal -> DXGI_FORMAT
// 注意：DXGI 无 RGB/RGB16/RGB32 三通道格式，近似为 RGBA（同位数）。
// ---------------------------------------------------------------------------

DXGI_FORMAT toDxgiFormat(int gpuFormat) {
    switch (gpuFormat) {
        case 0:  return DXGI_FORMAT_R8_UNORM;              // R8_UNORM
        case 1:  return DXGI_FORMAT_R8_SNORM;              // R8_SNORM
        case 2:  return DXGI_FORMAT_R8G8_UNORM;            // RG8_UNORM
        case 3:  return DXGI_FORMAT_R8G8_SNORM;            // RG8_SNORM
        case 4:  return DXGI_FORMAT_R8G8B8A8_UNORM;        // RGB8_UNORM (近似)
        case 5:  return DXGI_FORMAT_R8G8B8A8_SNORM;        // RGB8_SNORM (近似)
        case 6:  return DXGI_FORMAT_R8G8B8A8_UNORM;        // RGBA8_UNORM
        case 7:  return DXGI_FORMAT_R8G8B8A8_SNORM;        // RGBA8_SNORM
        case 8:  return DXGI_FORMAT_R16_UNORM;             // R16_UNORM
        case 9:  return DXGI_FORMAT_R16_SNORM;             // R16_SNORM
        case 10: return DXGI_FORMAT_R16G16_UNORM;          // RG16_UNORM
        case 11: return DXGI_FORMAT_R16G16_SNORM;          // RG16_SNORM
        case 12: return DXGI_FORMAT_R16G16B16A16_UNORM;    // RGB16_UNORM (近似)
        case 13: return DXGI_FORMAT_R16G16B16A16_SNORM;    // RGB16_SNORM (近似)
        case 14: return DXGI_FORMAT_R16G16B16A16_UNORM;    // RGBA16_UNORM
        case 15: return DXGI_FORMAT_R16G16B16A16_SNORM;    // RGBA16_SNORM
        case 16: return DXGI_FORMAT_R8_UINT;               // R8_UINT
        case 17: return DXGI_FORMAT_R8_SINT;               // R8_SINT
        case 18: return DXGI_FORMAT_R8G8_UINT;             // RG8_UINT
        case 19: return DXGI_FORMAT_R8G8_SINT;             // RG8_SINT
        case 20: return DXGI_FORMAT_R8G8B8A8_UINT;         // RGB8_UINT (近似)
        case 21: return DXGI_FORMAT_R8G8B8A8_SINT;         // RGB8_SINT (近似)
        case 22: return DXGI_FORMAT_R8G8B8A8_UINT;         // RGBA8_UINT
        case 23: return DXGI_FORMAT_R8G8B8A8_SINT;         // RGBA8_SINT
        case 24: return DXGI_FORMAT_R16_UINT;              // R16_UINT
        case 25: return DXGI_FORMAT_R16_SINT;              // R16_SINT
        case 26: return DXGI_FORMAT_R16G16_UINT;           // RG16_UINT
        case 27: return DXGI_FORMAT_R16G16_SINT;           // RG16_SINT
        case 28: return DXGI_FORMAT_R16G16B16A16_UINT;     // RGB16_UINT (近似)
        case 29: return DXGI_FORMAT_R16G16B16A16_SINT;     // RGB16_SINT (近似)
        case 30: return DXGI_FORMAT_R16G16B16A16_UINT;     // RGBA16_UINT
        case 31: return DXGI_FORMAT_R16G16B16A16_SINT;     // RGBA16_SINT
        case 32: return DXGI_FORMAT_R32_UINT;              // R32_UINT
        case 33: return DXGI_FORMAT_R32_SINT;              // R32_SINT
        case 34: return DXGI_FORMAT_R32G32_UINT;           // RG32_UINT
        case 35: return DXGI_FORMAT_R32G32_SINT;           // RG32_SINT
        case 36: return DXGI_FORMAT_R32G32B32A32_UINT;     // RGB32_UINT (近似)
        case 37: return DXGI_FORMAT_R32G32B32A32_SINT;     // RGB32_SINT (近似)
        case 38: return DXGI_FORMAT_R32G32B32A32_UINT;     // RGBA32_UINT
        case 39: return DXGI_FORMAT_R32G32B32A32_SINT;     // RGBA32_SINT
        case 40: return DXGI_FORMAT_R16_FLOAT;             // R16_FLOAT
        case 41: return DXGI_FORMAT_R16G16_FLOAT;          // RG16_FLOAT
        case 42: return DXGI_FORMAT_R16G16B16A16_FLOAT;    // RGB16_FLOAT (近似)
        case 43: return DXGI_FORMAT_R16G16B16A16_FLOAT;    // RGBA16_FLOAT
        case 44: return DXGI_FORMAT_R32_FLOAT;             // R32_FLOAT
        case 45: return DXGI_FORMAT_R32G32_FLOAT;          // RG32_FLOAT
        case 46: return DXGI_FORMAT_R32G32B32A32_FLOAT;    // RGB32_FLOAT (近似)
        case 47: return DXGI_FORMAT_R32G32B32A32_FLOAT;    // RGBA32_FLOAT
        case 48: return DXGI_FORMAT_R10G10B10A2_UNORM;     // RGB10A2_UNORM
        case 49: return DXGI_FORMAT_R10G10B10A2_UINT;      // RGB10A2_UINT
        case 50: return DXGI_FORMAT_R11G11B10_FLOAT;       // RG11B10_FLOAT
        case 51: return DXGI_FORMAT_D32_FLOAT;             // D32_FLOAT
        case 52: return DXGI_FORMAT_D32_FLOAT_S8X24_UINT;  // D32_FLOAT_S8_UINT
        case 53: return DXGI_FORMAT_D24_UNORM_S8_UINT;     // D24_UNORM_S8_UINT
        case 54: return DXGI_FORMAT_D16_UNORM;             // D16_UNORM
        case 55: return DXGI_FORMAT_UNKNOWN;               // S8_UINT (无独立格式)
        default: return DXGI_FORMAT_UNKNOWN;
    }
}

namespace {

// 是否有 depth 面（官方 GpuFormat.hasDepthAspect）
bool hasDepthAspect(int fmt) { return fmt == 51 || fmt == 52 || fmt == 53 || fmt == 54; }

D3D12_HEAP_TYPE pickBufferHeapType(int usage) {
    if (usage & 2) return D3D12_HEAP_TYPE_UPLOAD;     // MAP_WRITE
    if (usage & 1) return D3D12_HEAP_TYPE_READBACK;   // MAP_READ（无 MAP_WRITE）
    return D3D12_HEAP_TYPE_DEFAULT;
}

D3D12_RESOURCE_STATES initialStateFor(D3D12_HEAP_TYPE heap) {
    switch (heap) {
        case D3D12_HEAP_TYPE_UPLOAD:   return D3D12_RESOURCE_STATE_GENERIC_READ;
        case D3D12_HEAP_TYPE_READBACK: return D3D12_RESOURCE_STATE_COPY_DEST;
        default:                       return D3D12_RESOURCE_STATE_COMMON;
    }
}

D3D12_RESOURCE_FLAGS textureFlags(int usage, int format) {
    D3D12_RESOURCE_FLAGS flags = D3D12_RESOURCE_FLAG_NONE;
    if (usage & 8) {  // RENDER_ATTACHMENT
        flags |= hasDepthAspect(format) ? D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL
                                        : D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;
    }
    return flags;
}

}  // namespace

// ---------------------------------------------------------------------------
// 资源创建
// ---------------------------------------------------------------------------

Dx12Object* createTexture(int usage, int format, int width, int height,
    int depthOrLayers, int mipLevels, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }

    DXGI_FORMAT dxgi = toDxgiFormat(format);
    if (dxgi == DXGI_FORMAT_UNKNOWN) { err = "unsupported format ordinal " + std::to_string(format); return nullptr; }

    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = std::max(1u, (UINT)width);
    desc.Height = std::max(1u, (UINT)height);
    desc.DepthOrArraySize = std::max(1u, (UINT)depthOrLayers);
    desc.MipLevels = std::max(1u, (UINT)mipLevels);
    desc.Format = dxgi;
    desc.SampleDesc.Count = 1;
    desc.Flags = textureFlags(usage, format);

    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_DEFAULT;

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::Texture;
    obj->usage = usage;
    obj->size = (long long)desc.Width * desc.Height;
    obj->dxgiFormat = dxgi;
    HRESULT hr = gCtx.device->CreateCommittedResource(
        &heap, D3D12_HEAP_FLAG_NONE, &desc, D3D12_RESOURCE_STATE_COMMON,
        nullptr, IID_PPV_ARGS(&obj->resource));
    if (FAILED(hr)) { err = "CreateCommittedResource(texture): " + hrText(hr); return nullptr; }
    return obj.release();
}

Dx12Object* createBuffer(int usage, long long size, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }
    if (size <= 0) { err = "buffer size must be positive"; return nullptr; }

    D3D12_HEAP_TYPE heapType = pickBufferHeapType(usage);
    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = heapType;

    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = (UINT64)size;
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::Buffer;
    obj->usage = usage;
    obj->size = size;
    obj->heapType = heapType;
    HRESULT hr = gCtx.device->CreateCommittedResource(
        &heap, D3D12_HEAP_FLAG_NONE, &desc, initialStateFor(heapType),
        nullptr, IID_PPV_ARGS(&obj->resource));
    if (FAILED(hr)) { err = "CreateCommittedResource(buffer): " + hrText(hr); return nullptr; }
    return obj.release();
}

Dx12Object* createSampler(int addressU, int addressV, int minFilter,
    int magFilter, int maxAnisotropy, float maxLod, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }

    D3D12_SAMPLER_DESC desc{};
    // address: 0=REPEAT(WRAP), 1=CLAMP_TO_EDGE(CLAMP)
    desc.AddressU = (addressU == 0) ? D3D12_TEXTURE_ADDRESS_MODE_WRAP : D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    desc.AddressV = (addressV == 0) ? D3D12_TEXTURE_ADDRESS_MODE_WRAP : D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    desc.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    desc.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
    desc.MinLOD = 0.0f;
    desc.MaxLOD = maxLod > 0.0f ? maxLod : 16.0f;
    if (maxAnisotropy > 1) {
        desc.Filter = D3D12_FILTER_ANISOTROPIC;
        desc.MaxAnisotropy = (UINT)maxAnisotropy;
    } else {
        bool linear = (minFilter == 1) || (magFilter == 1);
        desc.Filter = linear ? D3D12_FILTER_MIN_MAG_MIP_LINEAR : D3D12_FILTER_MIN_MAG_MIP_POINT;
    }

    if (gNextSampler >= kSamplerHeapSize) { err = "sampler heap exhausted"; return nullptr; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.samplerHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)gNextSampler * gCtx.samplerInc;
    D3D12_GPU_DESCRIPTOR_HANDLE gpu = gCtx.samplerHeap->GetGPUDescriptorHandleForHeapStart();
    gpu.ptr += (SIZE_T)gNextSampler * gCtx.samplerInc;
    gNextSampler++;

    gCtx.device->CreateSampler(&desc, cpu);

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::Sampler;
    obj->cpuHandle = cpu;
    obj->gpuHandle = gpu;
    return obj.release();
}

Dx12Object* createTextureView(Dx12Object* texture, int baseMipLevel,
    int mipLevels, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }
    if (!texture || texture->kind != Dx12Object::Kind::Texture) {
        err = "createTextureView: invalid texture handle"; return nullptr;
    }
    if (!(texture->usage & 4)) {  // TEXTURE_BINDING
        err = "createTextureView: texture lacks TEXTURE_BINDING usage"; return nullptr;
    }

    if (gNextSrv >= kSrvHeapSize) { err = "srv heap exhausted"; return nullptr; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.srvHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)gNextSrv * gCtx.srvInc;
    D3D12_GPU_DESCRIPTOR_HANDLE gpu = gCtx.srvHeap->GetGPUDescriptorHandleForHeapStart();
    gpu.ptr += (SIZE_T)gNextSrv * gCtx.srvInc;
    gNextSrv++;

    D3D12_SHADER_RESOURCE_VIEW_DESC srv{};
    srv.Format = texture->resource->GetDesc().Format;
    srv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    if (texture->usage & 16) {  // CUBEMAP_COMPATIBLE
        srv.ViewDimension = D3D12_SRV_DIMENSION_TEXTURECUBE;
        srv.TextureCube.MipLevels = (UINT)std::max(1, mipLevels);
        srv.TextureCube.MostDetailedMip = (UINT)std::max(0, baseMipLevel);
    } else {
        srv.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
        srv.Texture2D.MipLevels = (UINT)std::max(1, mipLevels);
        srv.Texture2D.MostDetailedMip = (UINT)std::max(0, baseMipLevel);
    }
    gCtx.device->CreateShaderResourceView(texture->resource.Get(), &srv, cpu);

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::TextureView;
    obj->cpuHandle = cpu;
    obj->gpuHandle = gpu;
    return obj.release();
}

void destroyObject(Dx12Object* obj) {
    if (!obj) return;
    if (obj->mappedPtr) {
        if (obj->resource) obj->resource->Unmap(0, nullptr);
        obj->mappedPtr = nullptr;
    }
    delete obj;
}

// ---------------------------------------------------------------------------
// Buffer 映射（对应官方 GpuBuffer.map：read 需 MAP_READ，write 需 MAP_WRITE）
// ---------------------------------------------------------------------------

void* mapBuffer(Dx12Object* buffer, long long offset, long long length,
    bool read, bool write, std::string& err) {
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) {
        err = "mapBuffer: invalid buffer handle"; return nullptr;
    }
    if (read && !(buffer->usage & 1)) { err = "mapBuffer: buffer lacks MAP_READ"; return nullptr; }
    if (write && !(buffer->usage & 2)) { err = "mapBuffer: buffer lacks MAP_WRITE"; return nullptr; }
    if (offset < 0 || length < 0 || offset + length > buffer->size) {
        err = "mapBuffer: range out of bounds"; return nullptr;
    }
    if (buffer->heapType != D3D12_HEAP_TYPE_UPLOAD && buffer->heapType != D3D12_HEAP_TYPE_READBACK) {
        err = "mapBuffer: buffer is not host-visible (use MAP_WRITE/MAP_READ usage)"; return nullptr;
    }

    if (!buffer->mappedPtr) {
        void* base = nullptr;
        HRESULT hr = buffer->resource->Map(0, nullptr, &base);
        if (FAILED(hr)) { err = "Map: " + hrText(hr); return nullptr; }
        buffer->mappedPtr = base;
    }
    return static_cast<char*>(buffer->mappedPtr) + offset;
}

void unmapBuffer(Dx12Object* buffer) {
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) return;
    if (buffer->mappedPtr) {
        buffer->resource->Unmap(0, nullptr);
        buffer->mappedPtr = nullptr;
    }
}

// ---------------------------------------------------------------------------
// 资源层自检：创建 texture/buffer/sampler/view，map 写读验证，再销毁
// ---------------------------------------------------------------------------

std::string runResourceSelfTest() {
    std::string err;
    if (!ensureDevice(err)) return "SELF-TEST FAILED: " + err;

    // 1) texture: RGBA8_UNORM(6), 64x64, TEXTURE_BINDING|RENDER_ATTACHMENT
    Dx12Object* tex = createTexture(4 | 8, 6, 64, 64, 1, 1, err);
    if (!tex) return "SELF-TEST FAILED: texture - " + err;

    // 2) buffer: VERTEX|COPY_DST|MAP_WRITE, 1024
    Dx12Object* buf = createBuffer(32 | 8 | 2, 1024, err);
    if (!buf) { destroyObject(tex); return "SELF-TEST FAILED: buffer - " + err; }

    // 3) map 写入并读回验证
    void* p = mapBuffer(buf, 0, 1024, false, true, err);
    if (!p) { destroyObject(buf); destroyObject(tex); return "SELF-TEST FAILED: map - " + err; }
    memset(p, 0x5A, 1024);
    bool dataOk = (static_cast<unsigned char*>(p)[0] == 0x5A) &&
                  (static_cast<unsigned char*>(p)[1023] == 0x5A);
    unmapBuffer(buf);

    // 4) sampler: REPEAT/REPEAT, NEAREST/NEAREST, aniso=1, maxLod=16
    Dx12Object* sampler = createSampler(0, 0, 0, 0, 1, 16.0f, err);
    if (!sampler) { destroyObject(buf); destroyObject(tex); return "SELF-TEST FAILED: sampler - " + err; }

    // 5) texture view
    Dx12Object* view = createTextureView(tex, 0, 1, err);
    if (!view) { destroyObject(sampler); destroyObject(buf); destroyObject(tex);
        return "SELF-TEST FAILED: texture view - " + err; }

    std::string result = "SELF-TEST OK (map write/read " + std::string(dataOk ? "verified" : "MISMATCH") + ")";

    destroyObject(view);
    destroyObject(sampler);
    destroyObject(buf);
    destroyObject(tex);
    return result;
}

// ---------------------------------------------------------------------------
// 命令层（P3）
// ---------------------------------------------------------------------------

namespace {

// initialStateFor 已在 P2 资源层匿名 namespace 中定义（同一翻译单元内不得重复）。
// GENERIC_READ 已包含 COPY_SOURCE 等只读状态，无需 transition；
// COPY_DEST（READBACK 初始态）无需 transition。
bool needTransition(D3D12_RESOURCE_STATES from, D3D12_RESOURCE_STATES to) {
    if (from == to) return false;
    if (from == D3D12_RESOURCE_STATE_GENERIC_READ) return false;
    return true;
}

void resourceBarrier(ID3D12GraphicsCommandList* list, ID3D12Resource* res,
    D3D12_RESOURCE_STATES from, D3D12_RESOURCE_STATES to) {
    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = res;
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = from;
    b.Transition.StateAfter = to;
    list->ResourceBarrier(1, &b);
}

// buffer 按需 transition：进入 to，随后立刻回到初始状态（以初始状态为锚点）。
void transitionBufferOnce(CommandContext* ctx, Dx12Object* buf,
    D3D12_RESOURCE_STATES to) {
    D3D12_RESOURCE_STATES from = initialStateFor(buf->heapType);
    if (!needTransition(from, to)) return;
    resourceBarrier(ctx->commandList.Get(), buf->resource.Get(), from, to);
    resourceBarrier(ctx->commandList.Get(), buf->resource.Get(), to, from);
}

// texture 以 COMMON 为锚点：COMMON -> to，随后回 COMMON。
void transitionTextureOnce(CommandContext* ctx, Dx12Object* tex,
    D3D12_RESOURCE_STATES to) {
    resourceBarrier(ctx->commandList.Get(), tex->resource.Get(),
        D3D12_RESOURCE_STATE_COMMON, to);
    resourceBarrier(ctx->commandList.Get(), tex->resource.Get(),
        to, D3D12_RESOURCE_STATE_COMMON);
}

// DXGI_FORMAT -> 每 texel 字节数（仅非压缩格式；压缩格式返回 16 = 4x4 块）。
// 用于 CopyTextureRegion 的 footprint 行距计算。
UINT blockSizeFor(DXGI_FORMAT f) {
    switch (f) {
        case DXGI_FORMAT_R32G32B32A32_TYPELESS:
        case DXGI_FORMAT_R32G32B32A32_FLOAT:
        case DXGI_FORMAT_R32G32B32A32_UINT:
        case DXGI_FORMAT_R32G32B32A32_SINT: return 16;
        case DXGI_FORMAT_R32G32B32_TYPELESS:
        case DXGI_FORMAT_R32G32B32_FLOAT:
        case DXGI_FORMAT_R32G32B32_UINT:
        case DXGI_FORMAT_R32G32B32_SINT: return 12;
        case DXGI_FORMAT_R16G16B16A16_TYPELESS:
        case DXGI_FORMAT_R16G16B16A16_FLOAT:
        case DXGI_FORMAT_R16G16B16A16_UNORM:
        case DXGI_FORMAT_R16G16B16A16_UINT:
        case DXGI_FORMAT_R16G16B16A16_SNORM:
        case DXGI_FORMAT_R16G16B16A16_SINT:
        case DXGI_FORMAT_R32G32_TYPELESS:
        case DXGI_FORMAT_R32G32_FLOAT:
        case DXGI_FORMAT_R32G32_UINT:
        case DXGI_FORMAT_R32G32_SINT:
        case DXGI_FORMAT_D32_FLOAT: return 8;
        case DXGI_FORMAT_R32_FLOAT:
        case DXGI_FORMAT_R32_UINT:
        case DXGI_FORMAT_R32_SINT:
        case DXGI_FORMAT_D32_FLOAT_S8X24_UINT: return 4;
        case DXGI_FORMAT_R8G8B8A8_TYPELESS:
        case DXGI_FORMAT_R8G8B8A8_UNORM:
        case DXGI_FORMAT_R8G8B8A8_UNORM_SRGB:
        case DXGI_FORMAT_R8G8B8A8_UINT:
        case DXGI_FORMAT_R8G8B8A8_SNORM:
        case DXGI_FORMAT_R8G8B8A8_SINT:
        case DXGI_FORMAT_B8G8R8A8_UNORM:
        case DXGI_FORMAT_B8G8R8A8_UNORM_SRGB:
        case DXGI_FORMAT_R16G16_TYPELESS:
        case DXGI_FORMAT_R16G16_FLOAT:
        case DXGI_FORMAT_R16G16_UNORM:
        case DXGI_FORMAT_R16G16_UINT:
        case DXGI_FORMAT_R16G16_SNORM:
        case DXGI_FORMAT_R16G16_SINT:
        case DXGI_FORMAT_D24_UNORM_S8_UINT:
        case DXGI_FORMAT_R10G10B10A2_UNORM:
        case DXGI_FORMAT_R11G11B10_FLOAT: return 4;
        case DXGI_FORMAT_R16_FLOAT:
        case DXGI_FORMAT_R16_UNORM:
        case DXGI_FORMAT_R16_UINT:
        case DXGI_FORMAT_R16_SNORM:
        case DXGI_FORMAT_R16_SINT:
        case DXGI_FORMAT_R8G8_TYPELESS:
        case DXGI_FORMAT_R8G8_UNORM:
        case DXGI_FORMAT_R8G8_UINT:
        case DXGI_FORMAT_R8G8_SNORM:
        case DXGI_FORMAT_R8G8_SINT: return 2;
        case DXGI_FORMAT_R8_UNORM:
        case DXGI_FORMAT_R8_UINT:
        case DXGI_FORMAT_R8_SNORM:
        case DXGI_FORMAT_R8_SINT:
        case DXGI_FORMAT_A8_UNORM: return 1;
        case DXGI_FORMAT_BC1_TYPELESS:
        case DXGI_FORMAT_BC1_UNORM:
        case DXGI_FORMAT_BC1_UNORM_SRGB:
        case DXGI_FORMAT_BC4_TYPELESS:
        case DXGI_FORMAT_BC4_UNORM:
        case DXGI_FORMAT_BC4_SNORM: return 8;   // 4x4 块
        case DXGI_FORMAT_BC2_TYPELESS:
        case DXGI_FORMAT_BC2_UNORM:
        case DXGI_FORMAT_BC2_UNORM_SRGB:
        case DXGI_FORMAT_BC3_TYPELESS:
        case DXGI_FORMAT_BC3_UNORM:
        case DXGI_FORMAT_BC3_UNORM_SRGB:
        case DXGI_FORMAT_BC5_TYPELESS:
        case DXGI_FORMAT_BC5_UNORM:
        case DXGI_FORMAT_BC5_SNORM:
        case DXGI_FORMAT_BC6H_TYPELESS:
        case DXGI_FORMAT_BC6H_UF16:
        case DXGI_FORMAT_BC6H_SF16:
        case DXGI_FORMAT_BC7_TYPELESS:
        case DXGI_FORMAT_BC7_UNORM:
        case DXGI_FORMAT_BC7_UNORM_SRGB: return 16;  // 4x4 块
        default: return 4;
    }
}

// 在指定队列上执行瞬时提交并等待完成（query 读回 / getTimestampNow 用）。
bool flushAndWait(ID3D12CommandList* list, std::string& err) {
    ID3D12CommandList* lists[] = { list };
    gCtx.queue->ExecuteCommandLists(1, lists);
    ComPtr<ID3D12Fence> fence;
    if (FAILED(gCtx.device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence)))) {
        err = "flushAndWait: CreateFence failed"; return false;
    }
    UINT64 fv = 1;
    if (FAILED(gCtx.queue->Signal(fence.Get(), fv))) {
        err = "flushAndWait: Signal failed"; return false;
    }
    HANDLE evt = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!evt) { err = "flushAndWait: CreateEvent failed"; return false; }
    while (fence->GetCompletedValue() < fv) {
        fence->SetEventOnCompletion(fv, evt);
        WaitForSingleObject(evt, INFINITE);
    }
    CloseHandle(evt);
    return true;
}

}  // namespace

CommandContext* createCommandEncoder(std::string& err) {
    if (!ensureDevice(err)) return nullptr;
    auto ctx = std::make_unique<CommandContext>();
    for (int i = 0; i < 2; ++i) {
        if (FAILED(gCtx.device->CreateCommandAllocator(
                D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&ctx->allocators[i])))) {
            err = "createCommandEncoder: CreateCommandAllocator failed"; return nullptr;
        }
    }
    if (FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            ctx->allocators[0].Get(), nullptr, IID_PPV_ARGS(&ctx->commandList)))) {
        err = "createCommandEncoder: CreateCommandList failed"; return nullptr;
    }
    // 初始 closed 状态；beginCommandList 时 Reset。
    ctx->commandList->Close();
    if (FAILED(gCtx.device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&ctx->fence)))) {
        err = "createCommandEncoder: CreateFence failed"; return nullptr;
    }
    ctx->fenceEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!ctx->fenceEvent) { err = "createCommandEncoder: CreateEvent failed"; return nullptr; }
    return ctx.release();
}

void destroyCommandEncoder(CommandContext* ctx) {
    if (!ctx) return;
    if (ctx->fenceEvent) CloseHandle(ctx->fenceEvent);
    delete ctx;
}

bool beginCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "beginCommandList: null ctx"; return false; }
    HRESULT hr = ctx->currentAllocator()->Reset();
    if (FAILED(hr)) { err = "beginCommandList: allocator Reset " + hrText(hr); return false; }
    hr = ctx->commandList->Reset(ctx->currentAllocator().Get(), nullptr);
    if (FAILED(hr)) { err = "beginCommandList: list Reset " + hrText(hr); return false; }
    ctx->listOpen = 1;
    return true;
}

bool endCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "endCommandList: null ctx"; return false; }
    if (!ctx->listOpen) return true;  // 幂等
    HRESULT hr = ctx->commandList->Close();
    if (FAILED(hr)) { err = "endCommandList: Close " + hrText(hr); return false; }
    ctx->listOpen = 0;
    return true;
}

UINT64 submitCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "submitCommandList: null ctx"; return 0; }
    if (!endCommandList(ctx, err)) return 0;
    ID3D12CommandList* lists[] = { ctx->commandList.Get() };
    gCtx.queue->ExecuteCommandLists(1, lists);
    UINT64 value = ++ctx->fenceValue;
    if (FAILED(gCtx.queue->Signal(ctx->fence.Get(), value))) {
        err = "submitCommandList: Signal failed"; return 0;
    }
    // 等 value-2 完成（对应官方 awaitSubmitCompletion(currentSubmitIndex - 2)）
    if (value >= 2) {
        std::string w;
        if (!waitForFenceValue(ctx, value - 2, 5000000000ULL, w)) {
            err = "submitCommandList: " + w; return 0;
        }
    }
    return value;
}

bool waitForFenceValue(CommandContext* ctx, UINT64 value, UINT64 timeoutNs,
    std::string& err) {
    if (!ctx) { err = "waitForFenceValue: null ctx"; return false; }
    if (ctx->fence->GetCompletedValue() >= value) return true;
    HRESULT hr = ctx->fence->SetEventOnCompletion(value, ctx->fenceEvent);
    if (FAILED(hr)) { err = "waitForFenceValue: SetEventOnCompletion " + hrText(hr); return false; }
    DWORD ms = (DWORD)((timeoutNs + 999999ULL) / 1000000ULL);
    if (WaitForSingleObject(ctx->fenceEvent, ms) != WAIT_OBJECT_0) {
        err = "waitForFenceValue: timed out after " + std::to_string(timeoutNs) + "ns";
        return false;
    }
    return true;
}

UINT64 currentFenceValue(CommandContext* ctx) {
    return ctx ? ctx->fenceValue : 0;
}

// 读回单个 timestamp 的通用实现：录制 EndQuery + ResolveQueryData 到 READBACK
// buffer，提交并等待，然后 map 读。list 在调用前必须已 Close。
bool resolveQuery(QueryPool* pool, int start, int count, long long* out,
    std::string& err) {
    if (!pool) { err = "resolveQuery: null pool"; return false; }
    if (start < 0 || count <= 0 || start + count > pool->size) {
        err = "resolveQuery: invalid range"; return false;
    }
    ComPtr<ID3D12CommandAllocator> alloc;
    ComPtr<ID3D12GraphicsCommandList> list;
    if (FAILED(gCtx.device->CreateCommandAllocator(
            D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&alloc))) ||
        FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&list)))) {
        err = "resolveQuery: create command list failed"; return false;
    }
    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_READBACK;
    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = (UINT64)count * sizeof(long long);
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    ComPtr<ID3D12Resource> readback;
    if (FAILED(gCtx.device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&readback)))) {
        err = "resolveQuery: readback alloc failed"; return false;
    }
    list->ResolveQueryData(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP,
        (UINT)start, (UINT)count, readback.Get(), 0);
    list->Close();
    if (!flushAndWait(list.Get(), err)) return false;
    void* p = nullptr;
    readback->Map(0, nullptr, &p);
    memcpy(out, p, (size_t)count * sizeof(long long));
    readback->Unmap(0, nullptr);
    return true;
}

long long getTimestampNow(CommandContext* ctx, std::string& err) {
    (void)ctx;
    if (!ensureDevice(err)) return 0;
    QueryPool* pool = createQueryPool(1, err);
    if (!pool) return 0;
    ComPtr<ID3D12CommandAllocator> alloc;
    ComPtr<ID3D12GraphicsCommandList> list;
    if (FAILED(gCtx.device->CreateCommandAllocator(
            D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&alloc))) ||
        FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&list)))) {
        destroyQueryPool(pool); err = "getTimestampNow: create command list failed"; return 0;
    }
    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_READBACK;
    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = sizeof(long long);
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    ComPtr<ID3D12Resource> readback;
    if (FAILED(gCtx.device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&readback)))) {
        destroyQueryPool(pool); err = "getTimestampNow: readback alloc failed"; return 0;
    }
    list->EndQuery(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP, 0);
    list->ResolveQueryData(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP, 0, 1,
        readback.Get(), 0);
    list->Close();
    if (!flushAndWait(list.Get(), err)) { destroyQueryPool(pool); return 0; }
    long long ts = 0;
    void* p = nullptr;
    readback->Map(0, nullptr, &p);
    memcpy(&ts, p, sizeof(ts));
    readback->Unmap(0, nullptr);
    destroyQueryPool(pool);
    return ts;
}

// ---------------------------------------------------------------------------
// 命令录制（P3）：copy / clear / render pass / timestamp
// ---------------------------------------------------------------------------

bool copyBufferToBuffer(CommandContext* ctx, Dx12Object* src, long long srcOffset,
    Dx12Object* dst, long long dstOffset, long long size, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyBufferToBuffer: no open command list"; return false; }
    if (!src || !dst || src->kind != Dx12Object::Kind::Buffer ||
        dst->kind != Dx12Object::Kind::Buffer) {
        err = "copyBufferToBuffer: invalid buffer handle"; return false;
    }
    if (srcOffset < 0 || dstOffset < 0 || size < 0 ||
        srcOffset + size > src->size || dstOffset + size > dst->size) {
        err = "copyBufferToBuffer: range out of bounds"; return false;
    }
    transitionBufferOnce(ctx, src, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionBufferOnce(ctx, dst, D3D12_RESOURCE_STATE_COPY_DEST);
    ctx->commandList->CopyBufferRegion(dst->resource.Get(), (UINT64)dstOffset,
        src->resource.Get(), (UINT64)srcOffset, (UINT64)size);
    return true;
}

bool clearColorTexture(CommandContext* ctx, Dx12Object* tex,
    float r, float g, float b, float a, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "clearColorTexture: no open command list"; return false; }
    if (!tex || tex->kind != Dx12Object::Kind::Texture) {
        err = "clearColorTexture: invalid texture"; return false;
    }
    if (!(tex->usage & 8)) {  // RENDER_ATTACHMENT
        err = "clearColorTexture: texture lacks RENDER_ATTACHMENT"; return false;
    }
    if (gNextRtv >= kRtvHeapSize) { err = "clearColorTexture: rtv heap exhausted"; return false; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.rtvHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)gNextRtv * gCtx.rtvInc;
    ++gNextRtv;
    gCtx.device->CreateRenderTargetView(tex->resource.Get(), nullptr, cpu);
    transitionTextureOnce(ctx, tex, D3D12_RESOURCE_STATE_RENDER_TARGET);
    const float color[4] = { r, g, b, a };
    ctx->commandList->ClearRenderTargetView(cpu, color, 0, nullptr);
    return true;
}

bool clearDepthTexture(CommandContext* ctx, Dx12Object* tex, double depth,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "clearDepthTexture: no open command list"; return false; }
    if (!tex || tex->kind != Dx12Object::Kind::Texture) {
        err = "clearDepthTexture: invalid texture"; return false;
    }
    if (gNextDsv >= kDsvHeapSize) { err = "clearDepthTexture: dsv heap exhausted"; return false; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.dsvHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)gNextDsv * gCtx.dsvInc;
    ++gNextDsv;
    gCtx.device->CreateDepthStencilView(tex->resource.Get(), nullptr, cpu);
    transitionTextureOnce(ctx, tex, D3D12_RESOURCE_STATE_DEPTH_WRITE);
    ctx->commandList->ClearDepthStencilView(cpu,
        D3D12_CLEAR_FLAG_DEPTH | D3D12_CLEAR_FLAG_STENCIL, (FLOAT)depth, 0, 0, nullptr);
    return true;
}

bool beginRenderPass(CommandContext* ctx, Dx12Object* const* colorViews,
    int colorCount, const int* colorClearFlags, const float* clearColors,
    Dx12Object* depthView, int depthClearFlag, double depthClearValue,
    int x, int y, int w, int h, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "beginRenderPass: no open command list"; return false; }
    if (ctx->inRenderPass) { err = "beginRenderPass: render pass already open"; return false; }
    if (colorCount < 0) { err = "beginRenderPass: negative color count"; return false; }

    std::vector<D3D12_CPU_DESCRIPTOR_HANDLE> rtvs;
    rtvs.reserve((size_t)colorCount);
    for (int i = 0; i < colorCount; ++i) {
        Dx12Object* tex = colorViews[i];
        if (!tex) continue;  // withUnusedColorAttachment 占位
        if (tex->kind != Dx12Object::Kind::Texture || !(tex->usage & 8)) {
            err = "beginRenderPass: invalid color attachment (needs RENDER_ATTACHMENT)"; return false;
        }
        if (gNextRtv >= kRtvHeapSize) { err = "beginRenderPass: rtv heap exhausted"; return false; }
        D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.rtvHeap->GetCPUDescriptorHandleForHeapStart();
        cpu.ptr += (SIZE_T)gNextRtv * gCtx.rtvInc;
        ++gNextRtv;
        gCtx.device->CreateRenderTargetView(tex->resource.Get(), nullptr, cpu);
        rtvs.push_back(cpu);
        transitionTextureOnce(ctx, tex, D3D12_RESOURCE_STATE_RENDER_TARGET);
        if (colorClearFlags && colorClearFlags[i] && clearColors) {
            const float* c = clearColors + i * 4;
            ctx->commandList->ClearRenderTargetView(cpu, c, 0, nullptr);
        }
    }

    D3D12_CPU_DESCRIPTOR_HANDLE dsv{};
    bool hasDsv = false;
    if (depthView) {
        if (depthView->kind != Dx12Object::Kind::Texture) {
            err = "beginRenderPass: invalid depth attachment"; return false;
        }
        if (gNextDsv >= kDsvHeapSize) { err = "beginRenderPass: dsv heap exhausted"; return false; }
        D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.dsvHeap->GetCPUDescriptorHandleForHeapStart();
        cpu.ptr += (SIZE_T)gNextDsv * gCtx.dsvInc;
        ++gNextDsv;
        gCtx.device->CreateDepthStencilView(depthView->resource.Get(), nullptr, cpu);
        dsv = cpu;
        hasDsv = true;
        transitionTextureOnce(ctx, depthView, D3D12_RESOURCE_STATE_DEPTH_WRITE);
        if (depthClearFlag) {
            ctx->commandList->ClearDepthStencilView(cpu,
                D3D12_CLEAR_FLAG_DEPTH | D3D12_CLEAR_FLAG_STENCIL,
                (FLOAT)depthClearValue, 0, 0, nullptr);
        }
    }

    ctx->commandList->OMSetRenderTargets((UINT)rtvs.size(),
        rtvs.empty() ? nullptr : rtvs.data(), false, hasDsv ? &dsv : nullptr);
    D3D12_VIEWPORT vp{ (FLOAT)x, (FLOAT)y, (FLOAT)w, (FLOAT)h, 0.0f, 1.0f };
    ctx->commandList->RSSetViewports(1, &vp);
    D3D12_RECT scissor{ x, y, x + w, y + h };
    ctx->commandList->RSSetScissorRects(1, &scissor);
    ctx->inRenderPass = 1;
    return true;
}

bool endRenderPass(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "endRenderPass: null ctx"; return false; }
    if (!ctx->inRenderPass) return true;  // 幂等
    // P3 简化：附件 barrier 回切在真实渲染层（P4）统一管理。
    ctx->inRenderPass = 0;
    return true;
}

// ---------------------------------------------------------------------------
// Timestamp query pool（P3）
// ---------------------------------------------------------------------------

QueryPool* createQueryPool(int size, std::string& err) {
    if (!ensureDevice(err)) return nullptr;
    if (size <= 0) { err = "createQueryPool: size must be positive"; return nullptr; }
    D3D12_QUERY_HEAP_DESC desc{};
    desc.Type = D3D12_QUERY_HEAP_TYPE_TIMESTAMP;
    desc.Count = (UINT)size;
    auto pool = std::make_unique<QueryPool>();
    if (FAILED(gCtx.device->CreateQueryHeap(&desc, IID_PPV_ARGS(&pool->heap)))) {
        err = "createQueryPool: CreateQueryHeap failed"; return nullptr;
    }
    pool->size = size;
    return pool.release();
}

void destroyQueryPool(QueryPool* pool) {
    delete pool;
}

bool writeTimestampToPool(CommandContext* ctx, QueryPool* pool, int index,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "writeTimestampToPool: no open command list"; return false; }
    if (!pool || index < 0 || index >= pool->size) {
        err = "writeTimestampToPool: invalid index"; return false;
    }
    ctx->commandList->EndQuery(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP, (UINT)index);
    return true;
}

bool readQueryValue(QueryPool* pool, int index, long long& out, std::string& err) {
    return readQueryValues(pool, index, 1, &out, err);
}

bool readQueryValues(QueryPool* pool, int start, int count, long long* out,
    std::string& err) {
    return resolveQuery(pool, start, count, out, err);
}

}  // namespace dx12mc
