#include "dx12_device.h"

#include <dxgi.h>

#include <cstdio>
#include <cstdarg>
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

// P6：瞬时描述符堆（drawHeap）每帧半区容量 + 总数（ring x2，配合双 allocator）
constexpr UINT kDrawHeapPerFrame = 32768;
constexpr UINT kDrawHeapSize = kDrawHeapPerFrame * 2;

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

// 设备移除诊断：GetDeviceRemovedReason + 调试层最近消息（定位 0x887A0005 根因）。
std::string deviceStatusText() {
    std::string s;
    if (!gCtx.device) return s;
    HRESULT reason = gCtx.device->GetDeviceRemovedReason();
    s += "GetDeviceRemovedReason=" + hrText(reason);
    if (gCtx.infoQueue) {
        UINT64 count = gCtx.infoQueue->GetNumStoredMessages();
        UINT64 start = count > 24 ? count - 24 : 0;
        for (UINT64 i = start; i < count; ++i) {
            SIZE_T len = 0;
            if (FAILED(gCtx.infoQueue->GetMessage((UINT)i, nullptr, &len))) continue;
            std::vector<char> buf(len > 0 ? len : 1);
            D3D12_MESSAGE* msg = reinterpret_cast<D3D12_MESSAGE*>(buf.data());
            if (SUCCEEDED(gCtx.infoQueue->GetMessage((UINT)i, msg, &len))) {
                s += " | msg[" + std::to_string((long long)i) + "](";
                s += msg->pDescription ? msg->pDescription : "";
                s += ")";
            }
        }
    }
    return s;
}

}  // namespace

// 毫秒时间戳（QPC），供诊断插桩打印精确阻塞点（渲染线程卡死排查用）。
double nowMs() {
    LARGE_INTEGER f, c;
    QueryPerformanceFrequency(&f);
    QueryPerformanceCounter(&c);
    return (double)c.QuadPart * 1000.0 / (double)f.QuadPart;
}

// 诊断插桩：打印到 stderr（PCL 启动器会写入游戏日志；不影响 debug.log）。
void dbgLog(const char* fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    std::fprintf(stderr, "[dx12][t=%8.1fms] ", nowMs());
    std::vfprintf(stderr, fmt, ap);
    std::fprintf(stderr, "\n");
    va_end(ap);
    fflush(stderr);
}

// ---------------------------------------------------------------------------
// 设备生命周期
// ---------------------------------------------------------------------------

bool ensureDevice(std::string& errorOut) {
    if (gCtx.device) return true;

    // 诊断：先启用 D3D12 调试层（Win10/11 自带；失败则无调试层继续）。
    // 启用后非法 API 调用 / 描述符越界等会写入 InfoQueue，设备移除时可读回定位。
    bool debugEnabled = false;
    {
        ComPtr<ID3D12Debug> debug;
        if (SUCCEEDED(D3D12GetDebugInterface(IID_PPV_ARGS(&debug)))) {
            debug->EnableDebugLayer();
            debugEnabled = true;
        }
    }

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
    if (!device && debugEnabled) {
        // 部分系统缺少 SDK 调试层：D3D12CreateDevice 会失败，回退到无调试层重试。
        debugEnabled = false;
        for (auto level : levels) {
            if (SUCCEEDED(D3D12CreateDevice(nullptr, level, IID_PPV_ARGS(&device)))) {
                gCtx.featureLevel = level;
                break;
            }
        }
    }
    if (!device) {
        errorOut = "D3D12CreateDevice failed at all feature levels";
        return false;
    }
    gCtx.device = device;
    gCtx.adapterName = queryAdapterName(device.Get());
    // 取 InfoQueue（调试层启用后可用）用于设备移除时回读验证消息。
    if (debugEnabled) {
        if (FAILED(device->QueryInterface(IID_PPV_ARGS(&gCtx.infoQueue)))) {
            gCtx.infoQueue = nullptr;
        }
    }

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
            kSamplerHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.samplerHeap) ||
        // P6：瞬时 draw 描述符堆（ring x2，两帧在飞行时安全交替）
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV,
            kDrawHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.drawHeap)) {
        errorOut = "CreateDescriptorHeap failed";
        return false;
    }
    gCtx.srvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    gCtx.rtvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    gCtx.dsvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_DSV);
    gCtx.samplerInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER);
    gCtx.drawInc = gCtx.srvInc;  // 同属 CBV_SRV_UAV 类型，增量相同

    // P6：ExecuteIndirect 用 command signature
    {
        D3D12_INDIRECT_ARGUMENT_DESC arg{};
        arg.Type = D3D12_INDIRECT_ARGUMENT_TYPE_DRAW_INDEXED;
        D3D12_COMMAND_SIGNATURE_DESC cs{};
        cs.NumArgumentDescs = 1;
        cs.pArgumentDescs = &arg;
        cs.ByteStride = 20;  // indexCount, instanceCount, startIndexLocation, baseVertexLocation, startInstanceLocation
        if (FAILED(device->CreateCommandSignature(&cs, nullptr, IID_PPV_ARGS(&gCtx.cmdSigIndexed)))) {
            errorOut = "CreateCommandSignature(indexed) failed";
            return false;
        }
        arg.Type = D3D12_INDIRECT_ARGUMENT_TYPE_DRAW;
        cs.ByteStride = 16;  // vertexCount, instanceCount, startVertexLocation, startInstanceLocation
        if (FAILED(device->CreateCommandSignature(&cs, nullptr, IID_PPV_ARGS(&gCtx.cmdSigNonIndexed)))) {
            errorOut = "CreateCommandSignature(non-indexed) failed";
            return false;
        }
    }
    return true;
}

void destroyDevice() {
    gCtx = DeviceContext{};
}

DeviceContext& deviceContextForJni() {
    return gCtx;
}

D3D12_CPU_DESCRIPTOR_HANDLE allocRtvHandle(std::string& err) {
    if (gNextRtv >= kRtvHeapSize) {
        err = "RTV descriptor heap exhausted";
        return D3D12_CPU_DESCRIPTOR_HANDLE{};
    }
    D3D12_CPU_DESCRIPTOR_HANDLE h = gCtx.rtvHeap->GetCPUDescriptorHandleForHeapStart();
    h.ptr += (SIZE_T)gNextRtv * gCtx.rtvInc;
    ++gNextRtv;
    return h;
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

// 顶点输入布局格式：RGB32_* 用精确三分量格式（DXGI 仅对 32 位三分量提供
// R32G32B32_* 输入格式；纹理用途的 toDxgiFormat 会把 RGB32 加宽成 RGBA32，
// 但输入布局加宽会导致与 shader 语义分量数不匹配，PSO 创建失败）。
DXGI_FORMAT toDxgiVertexFormat(int gpuFormat) {
    switch (gpuFormat) {
        case 36: return DXGI_FORMAT_R32G32B32_UINT;   // RGB32_UINT
        case 37: return DXGI_FORMAT_R32G32B32_SINT;   // RGB32_SINT
        case 46: return DXGI_FORMAT_R32G32B32_FLOAT;  // RGB32_FLOAT
        default: return toDxgiFormat(gpuFormat);
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
    if (FAILED(hr)) {
        err = "CreateCommittedResource(texture): " + hrText(hr);
        if (hr == DXGI_ERROR_DEVICE_REMOVED || hr == DXGI_ERROR_DEVICE_RESET ||
            hr == DXGI_ERROR_DEVICE_HUNG) {
            err += " — " + deviceStatusText();
        }
        return nullptr;
    }
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
    // P6：统一 256 对齐分配（D3D12 资源分配粒度本就 64KB，无额外浪费），
    // 保证任意 uniform 切片的 CBV（SizeInBytes 向上取整 256）不越界。
    desc.Width = ((UINT64)size + 255) & ~255ULL;
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
    if (FAILED(hr)) {
        err = "CreateCommittedResource(buffer): " + hrText(hr);
        if (hr == DXGI_ERROR_DEVICE_REMOVED || hr == DXGI_ERROR_DEVICE_RESET ||
            hr == DXGI_ERROR_DEVICE_HUNG) {
            err += " — " + deviceStatusText();
        }
        return nullptr;
    }
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
    // 深度格式（D32_FLOAT/D16_UNORM/D32_FLOAT_S8X24_UINT/D24_UNORM_S8_UINT）
    // 不能直接作为 SRV 格式（调试层：'The format cannot be used with a
    // ShaderResource view'）——需换成同格式族内的只读视图格式（读深度为
    // float/uint），纹理本体仍保持深度格式用于 DSV。
    DXGI_FORMAT viewFormat = texture->resource->GetDesc().Format;
    switch (viewFormat) {
        case DXGI_FORMAT_D32_FLOAT:            viewFormat = DXGI_FORMAT_R32_FLOAT; break;
        case DXGI_FORMAT_D16_UNORM:            viewFormat = DXGI_FORMAT_R16_UNORM; break;
        case DXGI_FORMAT_D32_FLOAT_S8X24_UINT: viewFormat = DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS; break;
        case DXGI_FORMAT_D24_UNORM_S8_UINT:    viewFormat = DXGI_FORMAT_R24_UNORM_X8_TYPELESS; break;
        default: break;
    }
    srv.Format = viewFormat;
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
// 等待有界（5s）：GPU 队列卡住时返回错误而不是无限挂起（黑屏无响应根因嫌疑）。
bool flushAndWait(ID3D12CommandList* list, std::string& err) {
    dbgLog("flushAndWait: enter");
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
        if (WaitForSingleObject(evt, 5000) != WAIT_OBJECT_0) {
            CloseHandle(evt);
            err = "flushAndWait: timed out waiting for queue completion";
            dbgLog("flushAndWait: TIMEOUT (queue stalled)");
            return false;
        }
    }
    CloseHandle(evt);
    dbgLog("flushAndWait: done");
    return true;
}

}  // namespace

// 按本 command list 已跟踪状态把资源过渡到 to。fromInitial 为资源创建时的
// 初始状态（texture=COMMON，buffer=initialStateFor(heapType)）；若本 list 内
// 已跟踪过该资源则以跟踪状态为准。绝不在 list 内回退到 COMMON（D3D12 禁止
// 从已提升状态显式回 COMMON；decay 由命令列表执行完成时隐式处理）。
// beginCommandList 清空 tracking 后一切资源视为初始态——前提是 submit 同步
// 等待完成（保证上一 command list 已执行完并 decay，见 submitCommandList）。
// 定义在匿名 namespace 之外：transitionTextureTo 供 dx12_surface.cpp 使用。
void transitionTo(CommandContext* ctx, ID3D12Resource* res,
    D3D12_RESOURCE_STATES fromInitial, D3D12_RESOURCE_STATES to) {
    if (!ctx || !res) return;
    auto& m = ctx->resourceState;
    D3D12_RESOURCE_STATES from;
    auto it = m.find(res);
    if (it != m.end()) {
        from = it->second;
    } else {
        from = fromInitial;
        if (!needTransition(from, to)) return;
    }
    if (from == to) return;
    resourceBarrier(ctx->commandList.Get(), res, from, to);
    m[res] = to;
}

// buffer 按跟踪状态过渡（不回落初始状态）。
void transitionBufferTo(CommandContext* ctx, Dx12Object* buf,
    D3D12_RESOURCE_STATES to) {
    if (!buf || !buf->resource) return;
    transitionTo(ctx, buf->resource.Get(), initialStateFor(buf->heapType), to);
}

// texture 按跟踪状态过渡（初始锚点 COMMON；不回落 COMMON）。
void transitionTextureTo(CommandContext* ctx, Dx12Object* tex,
    D3D12_RESOURCE_STATES to) {
    if (!tex || !tex->resource) return;
    transitionTo(ctx, tex->resource.Get(), D3D12_RESOURCE_STATE_COMMON, to);
}

// 阻塞等待 GPU 队列空闲：Signal 新 fence 并等待其完成。
// 用于销毁 swapchain/设备前，确保 backbuffer 等资源不再被 GPU 使用。
// 等待有界（5s）：队列卡住时返回错误，避免销毁路径无限挂起。
bool deviceWaitIdle(std::string& err) {
    if (!gCtx.device || !gCtx.queue) {
        err = "deviceWaitIdle: device not initialized";
        return false;
    }
    dbgLog("deviceWaitIdle: enter");
    ComPtr<ID3D12Fence> fence;
    if (FAILED(gCtx.device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence)))) {
        err = "deviceWaitIdle: CreateFence failed";
        return false;
    }
    UINT64 fv = 1;
    if (FAILED(gCtx.queue->Signal(fence.Get(), fv))) {
        err = "deviceWaitIdle: Signal failed";
        return false;
    }
    HANDLE evt = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!evt) {
        err = "deviceWaitIdle: CreateEvent failed";
        return false;
    }
    while (fence->GetCompletedValue() < fv) {
        fence->SetEventOnCompletion(fv, evt);
        if (WaitForSingleObject(evt, 5000) != WAIT_OBJECT_0) {
            CloseHandle(evt);
            err = "deviceWaitIdle: timed out waiting for queue completion";
            dbgLog("deviceWaitIdle: TIMEOUT (queue stalled)");
            return false;
        }
    }
    CloseHandle(evt);
    dbgLog("deviceWaitIdle: done");
    return true;
}

CommandContext* createCommandEncoder(std::string& err) {
    if (!ensureDevice(err)) return nullptr;
    dbgLog("createCommandEncoder: enter");
    auto ctx = std::make_unique<CommandContext>();
    for (int i = 0; i < 3; ++i) {
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
    dbgLog("createCommandEncoder: done ctx=%p", (void*)ctx.get());
    return ctx.release();
}

void destroyCommandEncoder(CommandContext* ctx) {
    if (!ctx) return;
    dbgLog("destroyCommandEncoder: enter fenceValue=%llu", (unsigned long long)ctx->fenceValue);
    // 该 ctx 可能仍有未执行的已提交命令（如 createBuffer(data) 的 submit 后
    // 立即 close）；先等它全部完成再销毁 allocator，否则 GPU 在使用已释放的
    // allocator 会导致 DXGI_ERROR_DEVICE_REMOVED。
    if (ctx->fenceValue > 0) {
        std::string err;
        if (!waitForFenceValue(ctx, ctx->fenceValue, 5000000000ULL, err)) {
            dbgLog("destroyCommandEncoder: wait FAILED: %s", err.c_str());
        }
    }
    if (ctx->fenceEvent) CloseHandle(ctx->fenceEvent);
    delete ctx;
}

bool beginCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "beginCommandList: null ctx"; return false; }
    dbgLog("beginCommandList: fenceValue=%llu", (unsigned long long)ctx->fenceValue);
    HRESULT hr = ctx->currentAllocator()->Reset();
    if (FAILED(hr)) { err = "beginCommandList: allocator Reset " + hrText(hr); return false; }
    hr = ctx->commandList->Reset(ctx->currentAllocator().Get(), nullptr);
    if (FAILED(hr)) { err = "beginCommandList: list Reset " + hrText(hr); return false; }
    ctx->listOpen = 1;
    ctx->inRenderPass = 0;
    // 新 command list 从“所有资源处于初始态”开始：submit 同步等待完成保证
    // 上一 list 已执行完，提升状态已隐式 decay 回 COMMON（见 submitCommandList）。
    ctx->resourceState.clear();
    // P6：draw 瞬时描述符 ring 半区（fenceValue%2 与 allocator 同步交替；
    // 帧 N+2 重写帧 N 半区时，帧 N 的 GPU 工作已完成）
    ctx->drawHeapSlotBase = (UINT)(ctx->fenceValue % 2) * kDrawHeapPerFrame;
    ctx->nextDrawSlot = 0;
    // RTV/DSV 是 CPU-only 描述符堆：命令列表提交时驱动已捕获描述符内容，
    // 双 allocator + value-2 完成等待保证旧命令已结束，可每帧从 0 复用。
    gNextRtv = 0;
    gNextDsv = 0;
    ID3D12DescriptorHeap* drawHeaps[] = { gCtx.drawHeap.Get() };
    ctx->commandList->SetDescriptorHeaps(1, drawHeaps);
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
    UINT64 value = ctx->fenceValue + 1;
    dbgLog("submit: ExecuteCommandLists enter (v->%llu)", (unsigned long long)value);
    ID3D12CommandList* lists[] = { ctx->commandList.Get() };
    gCtx.queue->ExecuteCommandLists(1, lists);
    value = ++ctx->fenceValue;
    dbgLog("submit: executed, Signal v=%llu", (unsigned long long)value);
    if (FAILED(gCtx.queue->Signal(ctx->fence.Get(), value))) {
        err = "submitCommandList: Signal failed"; return 0;
    }
    // 提交后同步等待本次 Signal 的值完成（而非 value-2）：保证返回时上一
    // command list 已执行完，所有提升状态已隐式 decay 回 COMMON。这样
    // beginCommandList 清空 resourceState 后一切资源都从初始态开始是正确
    // 的（否则跨 list 的提升状态残留会让后续 barrier 的 Before 状态不匹配）。
    // P6 以首帧正确性优先；多帧在飞行（双缓冲）的优化留待后续阶段。
    if (value >= 1) {
        std::string w;
        if (!waitForFenceValue(ctx, value, 5000000000ULL, w)) {
            err = "submitCommandList: " + w;
            dbgLog("submit: WAIT FAILED: %s", w.c_str());
            return 0;
        }
    }
    dbgLog("submit: done v=%llu", (unsigned long long)value);
    return value;
}

bool waitForFenceValue(CommandContext* ctx, UINT64 value, UINT64 timeoutNs,
    std::string& err) {
    if (!ctx) { err = "waitForFenceValue: null ctx"; return false; }
    UINT64 cv = ctx->fence->GetCompletedValue();
    dbgLog("waitFence: value=%llu completed=%llu", (unsigned long long)value, (unsigned long long)cv);
    if (cv >= value) return true;
    HRESULT hr = ctx->fence->SetEventOnCompletion(value, ctx->fenceEvent);
    if (FAILED(hr)) { err = "waitForFenceValue: SetEventOnCompletion " + hrText(hr); return false; }
    DWORD ms = (DWORD)((timeoutNs + 999999ULL) / 1000000ULL);
    if (WaitForSingleObject(ctx->fenceEvent, ms) != WAIT_OBJECT_0) {
        err = "waitForFenceValue: timed out after " + std::to_string(timeoutNs) + "ns";
        dbgLog("waitFence: TIMEOUT value=%llu completed=%llu",
            (unsigned long long)value,
            (unsigned long long)ctx->fence->GetCompletedValue());
        return false;
    }
    dbgLog("waitFence: OK value=%llu", (unsigned long long)value);
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
    transitionBufferTo(ctx, src, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionBufferTo(ctx, dst, D3D12_RESOURCE_STATE_COPY_DEST);
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
    transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_RENDER_TARGET);
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
    transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_DEPTH_WRITE);
    ctx->commandList->ClearDepthStencilView(cpu,
        D3D12_CLEAR_FLAG_DEPTH | D3D12_CLEAR_FLAG_STENCIL, (FLOAT)depth, 0, 0, nullptr);
    return true;
}

bool copyBufferToTexture(CommandContext* ctx, Dx12Object* srcBuf, long long srcOffset,
    int srcWidth, int srcHeight, Dx12Object* dstTex, int mip, int layer,
    int dstX, int dstY, int w, int h, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyBufferToTexture: no open command list"; return false; }
    if (!srcBuf || srcBuf->kind != Dx12Object::Kind::Buffer ||
        !dstTex || dstTex->kind != Dx12Object::Kind::Texture) {
        err = "copyBufferToTexture: invalid handle"; return false;
    }
    if (w <= 0 || h <= 0 || srcWidth <= 0 || srcHeight <= 0) {
        err = "copyBufferToTexture: non-positive size"; return false;
    }
    UINT block = blockSizeFor(dstTex->dxgiFormat);
    UINT rowBytes = (UINT)w * block;
    UINT srcRowBytes = (UINT)srcWidth * block;  // 源 buffer 每行实际间距（texel*block）

    // D3D12 要求 footprint RowPitch 为 256 的倍数；Minecraft 的 CPU 侧数据是
    // 紧凑打包的，因此整块 RowPitch 未对齐时逐行拷贝以保证正确。
    constexpr UINT kPitchAlign = D3D12_TEXTURE_DATA_PITCH_ALIGNMENT;
    bool aligned = (srcRowBytes % kPitchAlign) == 0 && srcWidth == w;

    transitionBufferTo(ctx, srcBuf, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionTextureTo(ctx, dstTex, D3D12_RESOURCE_STATE_COPY_DEST);

    UINT subresource = (UINT)(mip + layer * dstTex->resource->GetDesc().MipLevels);
    if (aligned) {
        D3D12_TEXTURE_COPY_LOCATION src{};
        src.pResource = srcBuf->resource.Get();
        src.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
        src.PlacedFootprint.Offset = (UINT64)srcOffset;
        src.PlacedFootprint.Footprint.Format = dstTex->dxgiFormat;
        src.PlacedFootprint.Footprint.Width = (UINT)w;
        src.PlacedFootprint.Footprint.Height = (UINT)h;
        src.PlacedFootprint.Footprint.Depth = 1;
        src.PlacedFootprint.Footprint.RowPitch = srcRowBytes;
        D3D12_TEXTURE_COPY_LOCATION dst{};
        dst.pResource = dstTex->resource.Get();
        dst.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
        dst.SubresourceIndex = subresource;
        ctx->commandList->CopyTextureRegion(&dst, dstX, dstY, 0, &src, nullptr);
    } else {
        // 逐行拷贝：每行 1 像素高，footprint 行距 = 源行距（可能未 256 对齐，
        // 部分驱动允许；这是紧凑数据上传的常见做法）。
        for (int row = 0; row < h; ++row) {
            D3D12_TEXTURE_COPY_LOCATION src{};
            src.pResource = srcBuf->resource.Get();
            src.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
            src.PlacedFootprint.Offset = (UINT64)srcOffset + (UINT64)row * srcRowBytes;
            src.PlacedFootprint.Footprint.Format = dstTex->dxgiFormat;
            src.PlacedFootprint.Footprint.Width = (UINT)w;
            src.PlacedFootprint.Footprint.Height = 1;
            src.PlacedFootprint.Footprint.Depth = 1;
            src.PlacedFootprint.Footprint.RowPitch = rowBytes;
            D3D12_TEXTURE_COPY_LOCATION dst{};
            dst.pResource = dstTex->resource.Get();
            dst.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
            dst.SubresourceIndex = subresource;
            ctx->commandList->CopyTextureRegion(&dst, dstX, dstY + row, 0, &src, nullptr);
        }
    }
    return true;
}

bool copyTextureToBuffer(CommandContext* ctx, Dx12Object* srcTex, int mip, int layer,
    int srcX, int srcY, int w, int h, Dx12Object* dstBuf, long long dstOffset,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyTextureToBuffer: no open command list"; return false; }
    if (!srcTex || srcTex->kind != Dx12Object::Kind::Texture ||
        !dstBuf || dstBuf->kind != Dx12Object::Kind::Buffer) {
        err = "copyTextureToBuffer: invalid handle"; return false;
    }
    if (w <= 0 || h <= 0) { err = "copyTextureToBuffer: non-positive size"; return false; }
    UINT block = blockSizeFor(srcTex->dxgiFormat);
    UINT rowBytes = (UINT)w * block;

    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionBufferTo(ctx, dstBuf, D3D12_RESOURCE_STATE_COPY_DEST);

    UINT subresource = (UINT)(mip + layer * srcTex->resource->GetDesc().MipLevels);
    // 客户端（Minecraft）按紧凑行距访问读回数据，且读回 buffer 只分配了
    // 紧凑大小（rowBytes*h）。D3D12 要求 footprint RowPitch 为 256 的倍数，
    // 因此行距未对齐时逐行紧凑拷贝（每行 Height=1）。
    constexpr UINT kPitchAlign = D3D12_TEXTURE_DATA_PITCH_ALIGNMENT;
    bool aligned = (rowBytes % kPitchAlign) == 0;
    if (dstOffset + (UINT64)rowBytes * h > (UINT64)dstBuf->size) {
        err = "copyTextureToBuffer: destination buffer too small";
        return false;
    }

    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = srcTex->resource.Get();
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = subresource;
    if (aligned) {
        D3D12_TEXTURE_COPY_LOCATION dst{};
        dst.pResource = dstBuf->resource.Get();
        dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
        dst.PlacedFootprint.Offset = (UINT64)dstOffset;
        dst.PlacedFootprint.Footprint.Format = srcTex->dxgiFormat;
        dst.PlacedFootprint.Footprint.Width = (UINT)w;
        dst.PlacedFootprint.Footprint.Height = (UINT)h;
        dst.PlacedFootprint.Footprint.Depth = 1;
        dst.PlacedFootprint.Footprint.RowPitch = rowBytes;
        D3D12_BOX srcBox{ (UINT)srcX, (UINT)srcY, 0, (UINT)(srcX + w), (UINT)(srcY + h), 1 };
        ctx->commandList->CopyTextureRegion(&dst, 0, 0, 0, &src, &srcBox);
    } else {
        // 逐行拷贝：目标 buffer 紧凑布局，RowPitch=rowBytes（每行 1 像素高）。
        for (int row = 0; row < h; ++row) {
            D3D12_TEXTURE_COPY_LOCATION dst{};
            dst.pResource = dstBuf->resource.Get();
            dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
            dst.PlacedFootprint.Offset = (UINT64)dstOffset + (UINT64)row * rowBytes;
            dst.PlacedFootprint.Footprint.Format = srcTex->dxgiFormat;
            dst.PlacedFootprint.Footprint.Width = (UINT)w;
            dst.PlacedFootprint.Footprint.Height = 1;
            dst.PlacedFootprint.Footprint.Depth = 1;
            dst.PlacedFootprint.Footprint.RowPitch = rowBytes;
            D3D12_BOX srcBox{ (UINT)srcX, (UINT)(srcY + row), 0,
                (UINT)(srcX + w), (UINT)(srcY + row + 1), 1 };
            ctx->commandList->CopyTextureRegion(&dst, 0, 0, 0, &src, &srcBox);
        }
    }
    return true;
}

bool copyTextureToTexture(CommandContext* ctx, Dx12Object* srcTex, Dx12Object* dstTex,
    int mip, int layer, int srcX, int srcY, int dstX, int dstY, int w, int h,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyTextureToTexture: no open command list"; return false; }
    if (!srcTex || srcTex->kind != Dx12Object::Kind::Texture ||
        !dstTex || dstTex->kind != Dx12Object::Kind::Texture) {
        err = "copyTextureToTexture: invalid handle"; return false;
    }
    if (w <= 0 || h <= 0) { err = "copyTextureToTexture: non-positive size"; return false; }
    if (srcTex->dxgiFormat != dstTex->dxgiFormat) {
        err = "copyTextureToTexture: format mismatch"; return false;
    }
    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionTextureTo(ctx, dstTex, D3D12_RESOURCE_STATE_COPY_DEST);

    UINT srcSub = (UINT)(mip + layer * srcTex->resource->GetDesc().MipLevels);
    UINT dstSub = (UINT)(mip + layer * dstTex->resource->GetDesc().MipLevels);
    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = srcTex->resource.Get();
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = srcSub;
    D3D12_TEXTURE_COPY_LOCATION dst{};
    dst.pResource = dstTex->resource.Get();
    dst.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dst.SubresourceIndex = dstSub;
    D3D12_BOX srcBox{ (UINT)srcX, (UINT)srcY, 0, (UINT)(srcX + w), (UINT)(srcY + h), 1 };
    ctx->commandList->CopyTextureRegion(&dst, dstX, dstY, 0, &src, &srcBox);
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
        transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_RENDER_TARGET);
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
        transitionTextureTo(ctx, depthView, D3D12_RESOURCE_STATE_DEPTH_WRITE);
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

unsigned long long getTimestampFrequency() {
    return gCtx.timestampFrequency;
}

// ---------------------------------------------------------------------------
// P4: 图形管线（D3DCompile + root signature + 双 PSO）
// ---------------------------------------------------------------------------

namespace {

// MC BlendFactor ordinal -> D3D12_BLEND（枚举声明顺序）
D3D12_BLEND toD3d12BlendFactor(uint8_t f) {
    switch (f) {
        case 0: return D3D12_BLEND_BLEND_FACTOR;      // CONSTANT_ALPHA
        case 1: return D3D12_BLEND_BLEND_FACTOR;      // CONSTANT_COLOR
        case 2: return D3D12_BLEND_DEST_ALPHA;        // DST_ALPHA
        case 3: return D3D12_BLEND_DEST_COLOR;        // DST_COLOR
        case 4: return D3D12_BLEND_ONE;               // ONE
        case 5: return D3D12_BLEND_INV_BLEND_FACTOR;  // ONE_MINUS_CONSTANT_ALPHA
        case 6: return D3D12_BLEND_INV_BLEND_FACTOR;  // ONE_MINUS_CONSTANT_COLOR
        case 7: return D3D12_BLEND_INV_DEST_ALPHA;    // ONE_MINUS_DST_ALPHA
        case 8: return D3D12_BLEND_INV_DEST_COLOR;    // ONE_MINUS_DST_COLOR
        case 9: return D3D12_BLEND_INV_SRC_ALPHA;     // ONE_MINUS_SRC_ALPHA
        case 10: return D3D12_BLEND_INV_SRC_COLOR;    // ONE_MINUS_SRC_COLOR
        case 11: return D3D12_BLEND_SRC_ALPHA;        // SRC_ALPHA
        case 12: return D3D12_BLEND_SRC_ALPHA_SAT;    // SRC_ALPHA_SATURATE
        case 13: return D3D12_BLEND_SRC_COLOR;        // SRC_COLOR
        case 14: return D3D12_BLEND_ZERO;             // ZERO
        default: return D3D12_BLEND_ONE;
    }
}

// MC BlendOp ordinal -> D3D12_BLEND_OP
D3D12_BLEND_OP toD3d12BlendOp(uint8_t op) {
    switch (op) {
        case 0: return D3D12_BLEND_OP_ADD;            // ADD
        case 1: return D3D12_BLEND_OP_SUBTRACT;       // SUBTRACT
        case 2: return D3D12_BLEND_OP_REV_SUBTRACT;   // REVERSE_SUBTRACT
        case 3: return D3D12_BLEND_OP_MIN;            // MIN
        case 4: return D3D12_BLEND_OP_MAX;            // MAX
        default: return D3D12_BLEND_OP_ADD;
    }
}

// MC CompareOp ordinal -> D3D12_COMPARISON_FUNC
D3D12_COMPARISON_FUNC toD3d12Compare(uint8_t c) {
    switch (c) {
        case 0: return D3D12_COMPARISON_FUNC_ALWAYS;         // ALWAYS_PASS
        case 1: return D3D12_COMPARISON_FUNC_LESS;           // LESS_THAN
        case 2: return D3D12_COMPARISON_FUNC_LESS_EQUAL;     // LESS_THAN_OR_EQUAL
        case 3: return D3D12_COMPARISON_FUNC_EQUAL;          // EQUAL
        case 4: return D3D12_COMPARISON_FUNC_NOT_EQUAL;      // NOT_EQUAL
        case 5: return D3D12_COMPARISON_FUNC_GREATER_EQUAL;  // GREATER_THAN_OR_EQUAL
        case 6: return D3D12_COMPARISON_FUNC_GREATER;        // GREATER_THAN
        case 7: return D3D12_COMPARISON_FUNC_NEVER;          // NEVER_PASS
        default: return D3D12_COMPARISON_FUNC_ALWAYS;
    }
}

// MC PrimitiveTopology ordinal -> D3D12_PRIMITIVE_TOPOLOGY_TYPE
D3D12_PRIMITIVE_TOPOLOGY_TYPE toTopologyType(int t) {
    switch (t) {
        case 0: case 1: case 2: return D3D12_PRIMITIVE_TOPOLOGY_TYPE_LINE;   // LINES/DEBUG_LINES/DEBUG_LINE_STRIP
        case 3: return D3D12_PRIMITIVE_TOPOLOGY_TYPE_POINT;                  // POINTS
        default: return D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;              // TRIANGLES/STRIP/FAN/QUADS
    }
}

bool compileShaderBytecode(const std::vector<uint8_t>& src, const char* stageName,
    const char* target, ComPtr<ID3DBlob>& out, std::string& err) {
    ComPtr<ID3DBlob> errBlob;
    HRESULT hr = D3DCompile(src.data(), src.size(), stageName, nullptr, nullptr,
        "main", target, 0, 0, &out, &errBlob);
    if (FAILED(hr)) {
        std::string msg;
        if (errBlob && errBlob->GetBufferSize() > 0) {
            const char* m = static_cast<const char*>(errBlob->GetBufferPointer());
            msg.assign(m, errBlob->GetBufferSize());
        }
        err = std::string("D3DCompile(") + target + ") hr=0x" + hrText(hr)
            + (msg.empty() ? "" : "\n" + msg);
        return false;
    }
    return true;
}

}  // namespace

Dx12Pipeline* createGraphicsPipeline(const PipelineDesc& desc, std::string& err) {
    if (!ensureDevice(err)) return nullptr;

    // 1) HLSL -> DXBC（vs_5_1 / ps_5_1，入口 main）
    ComPtr<ID3DBlob> vsBlob, psBlob;
    if (!compileShaderBytecode(desc.vsBytes, "vertex", "vs_5_1", vsBlob, err)) return nullptr;
    if (!compileShaderBytecode(desc.psBytes, "fragment", "ps_5_1", psBlob, err)) return nullptr;

    // 2) root signature：单 descriptor table（CBV/SRV 混合，register=条目序号）
    //    + static sampler（仅 SAMPLED_IMAGE 条目，register=同一序号）
    std::vector<D3D12_DESCRIPTOR_RANGE> ranges;
    std::vector<D3D12_STATIC_SAMPLER_DESC> staticSamplers;
    ranges.reserve(desc.bindings.size());
    for (const PipelineDesc::Binding& b : desc.bindings) {
        D3D12_DESCRIPTOR_RANGE r{};
        r.NumDescriptors = 1;
        r.RegisterSpace = 0;
        r.OffsetInDescriptorsFromTableStart = D3D12_DESCRIPTOR_RANGE_OFFSET_APPEND;
        r.BaseShaderRegister = b.reg;
        r.RangeType = (b.type == 0) ? D3D12_DESCRIPTOR_RANGE_TYPE_CBV
                                    : D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
        ranges.push_back(r);
        if (b.type == 1) {
            D3D12_STATIC_SAMPLER_DESC s{};
            s.Filter = D3D12_FILTER_MIN_MAG_MIP_LINEAR;
            s.AddressU = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
            s.AddressV = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
            s.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
            s.MipLODBias = 0.0f;
            s.MaxAnisotropy = 1;
            s.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
            s.BorderColor = D3D12_STATIC_BORDER_COLOR_OPAQUE_BLACK;
            s.MinLOD = 0.0f;
            s.MaxLOD = D3D12_FLOAT32_MAX;
            s.ShaderRegister = b.reg;
            s.RegisterSpace = 0;
            s.ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
            staticSamplers.push_back(s);
        }
    }
    D3D12_ROOT_DESCRIPTOR_TABLE table{};
    table.NumDescriptorRanges = (UINT)ranges.size();
    table.pDescriptorRanges = ranges.empty() ? nullptr : ranges.data();
    D3D12_ROOT_PARAMETER param{};
    param.ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    param.DescriptorTable = table;
    param.ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
    D3D12_ROOT_SIGNATURE_DESC rsDesc{};
    rsDesc.NumParameters = 1;
    rsDesc.pParameters = &param;
    rsDesc.NumStaticSamplers = (UINT)staticSamplers.size();
    rsDesc.pStaticSamplers = staticSamplers.empty() ? nullptr : staticSamplers.data();
    rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;

    ComPtr<ID3DBlob> rsBlob, rsErr;
    HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1_0,
        &rsBlob, &rsErr);
    if (FAILED(hr)) {
        err = "createGraphicsPipeline: D3D12SerializeRootSignature hr=0x" + hrText(hr);
        return nullptr;
    }
    ComPtr<ID3D12RootSignature> rootSig;
    hr = gCtx.device->CreateRootSignature(0, rsBlob->GetBufferPointer(),
        rsBlob->GetBufferSize(), IID_PPV_ARGS(&rootSig));
    if (FAILED(hr)) {
        err = "createGraphicsPipeline: CreateRootSignature hr=0x" + hrText(hr);
        return nullptr;
    }

    // 3) 输入布局：语义 = TEXCOORD<location>（spvc 对顶点输入按 location 生成）
    std::vector<D3D12_INPUT_ELEMENT_DESC> inputLayout;
    inputLayout.reserve(desc.inputElements.size());
    for (const PipelineDesc::InputElement& el : desc.inputElements) {
        D3D12_INPUT_ELEMENT_DESC ie{};
        ie.SemanticName = "TEXCOORD";
        ie.SemanticIndex = (UINT)el.location;
        ie.Format = toDxgiVertexFormat(el.format);
        ie.InputSlot = (UINT)el.binding;
        ie.AlignedByteOffset = (UINT)el.offset;
        if (el.stepRate > 0) {
            ie.InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_INSTANCE_DATA;
            ie.InstanceDataStepRate = (UINT)el.stepRate;
        } else {
            ie.InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
            ie.InstanceDataStepRate = 0;
        }
        inputLayout.push_back(ie);
    }

    // 4) PSO 共享状态：混合 + 光栅化
    D3D12_BLEND_DESC blend{};
    blend.AlphaToCoverageEnable = FALSE;
    blend.IndependentBlendEnable = TRUE;
    int numRT = 0;
    for (int i = 0; i < desc.colorCount; ++i) {
        const PipelineDesc::ColorTarget& ct = desc.colorTargets[i];
        if (ct.format < 0) continue;  // 未使用槽位
        numRT = i + 1;
        D3D12_RENDER_TARGET_BLEND_DESC& rt = blend.RenderTarget[i];
        rt.BlendEnable = ct.blendEnabled;
        rt.LogicOpEnable = FALSE;
        rt.SrcBlend = toD3d12BlendFactor(ct.srcColor);
        rt.DestBlend = toD3d12BlendFactor(ct.dstColor);
        rt.BlendOp = toD3d12BlendOp(ct.colorOp);
        rt.SrcBlendAlpha = toD3d12BlendFactor(ct.srcAlpha);
        rt.DestBlendAlpha = toD3d12BlendFactor(ct.dstAlpha);
        rt.BlendOpAlpha = toD3d12BlendOp(ct.alphaOp);
        rt.LogicOp = D3D12_LOGIC_OP_NOOP;
        rt.RenderTargetWriteMask = ct.writeMask;  // MC 掩码位序与 D3D12 一致（R=1,G=2,B=4,A=8）
    }

    D3D12_RASTERIZER_DESC rs{};
    rs.FillMode = (desc.polygonMode == 1) ? D3D12_FILL_MODE_WIREFRAME : D3D12_FILL_MODE_SOLID;
    rs.CullMode = desc.cullEnabled ? D3D12_CULL_MODE_BACK : D3D12_CULL_MODE_NONE;
    // MC 前端为逆时针（官方 Vulkan frontFace=CCW），D3D12 默认按顺时针判定，需翻转。
    rs.FrontCounterClockwise = TRUE;
    rs.DepthBias = 0;                    // P6 细化 depthBiasConstant/ScaleFactor
    rs.DepthBiasClamp = 0.0f;
    rs.SlopeScaledDepthBias = 0.0f;
    rs.DepthClipEnable = TRUE;
    rs.MultisampleEnable = FALSE;
    rs.AntialiasedLineEnable = FALSE;
    rs.ForcedSampleCount = 0;
    rs.ConservativeRaster = D3D12_CONSERVATIVE_RASTERIZATION_MODE_OFF;

    // 5) 双 PSO：withDepth 总是创建（DSV=D32_FLOAT，镜像官方 depthAttachmentFormat=126）；
    //    仅当管线无深度状态时再创建 withoutDepth（DSV=UNKNOWN，无深度附件）。
    auto pipeline = std::make_unique<Dx12Pipeline>();
    pipeline->rootSignature = rootSig;

    auto buildPso = [&](DXGI_FORMAT dsvFormat, bool depthEnable,
        D3D12_DEPTH_WRITE_MASK depthWrite, D3D12_COMPARISON_FUNC depthFunc,
        ComPtr<ID3D12PipelineState>& out, std::string& e) -> bool {
        D3D12_DEPTH_STENCIL_DESC ds{};
        ds.DepthEnable = depthEnable;
        ds.DepthWriteMask = depthWrite;
        ds.DepthFunc = depthFunc;
        ds.StencilEnable = FALSE;
        ds.StencilReadMask = D3D12_DEFAULT_STENCIL_READ_MASK;
        ds.StencilWriteMask = D3D12_DEFAULT_STENCIL_WRITE_MASK;

        D3D12_GRAPHICS_PIPELINE_STATE_DESC pso{};
        pso.pRootSignature = rootSig.Get();
        pso.VS.pShaderBytecode = vsBlob->GetBufferPointer();
        pso.VS.BytecodeLength = vsBlob->GetBufferSize();
        pso.PS.pShaderBytecode = psBlob->GetBufferPointer();
        pso.PS.BytecodeLength = psBlob->GetBufferSize();
        pso.InputLayout.NumElements = (UINT)inputLayout.size();
        pso.InputLayout.pInputElementDescs =
            inputLayout.empty() ? nullptr : inputLayout.data();
        pso.BlendState = blend;
        pso.RasterizerState = rs;
        pso.DepthStencilState = ds;
        pso.PrimitiveTopologyType = toTopologyType(desc.topology);
        pso.NumRenderTargets = (UINT)numRT;
        for (int i = 0; i < desc.colorCount; ++i) {
            const PipelineDesc::ColorTarget& ct = desc.colorTargets[i];
            pso.RTVFormats[i] = ct.format < 0 ? DXGI_FORMAT_UNKNOWN
                                              : toDxgiFormat(ct.format);
        }
        pso.DSVFormat = dsvFormat;
        pso.SampleDesc.Count = 1;
        pso.SampleDesc.Quality = 0;
        pso.SampleMask = UINT_MAX;
        HRESULT h = gCtx.device->CreateGraphicsPipelineState(&pso, IID_PPV_ARGS(&out));
        if (FAILED(h)) {
            e = "createGraphicsPipeline: CreateGraphicsPipelineState hr=0x" + hrText(h);
            return false;
        }
        return true;
    };

    D3D12_DEPTH_WRITE_MASK depthWrite = (desc.hasDepth && desc.depthWrite)
        ? D3D12_DEPTH_WRITE_MASK_ALL : D3D12_DEPTH_WRITE_MASK_ZERO;
    D3D12_COMPARISON_FUNC depthFunc = desc.hasDepth
        ? toD3d12Compare((uint8_t)desc.depthCompareOp) : D3D12_COMPARISON_FUNC_ALWAYS;

    if (!buildPso(DXGI_FORMAT_D32_FLOAT, desc.hasDepth, depthWrite, depthFunc,
        pipeline->withDepth, err)) {
        return nullptr;
    }
    if (!desc.hasDepth) {
        if (!buildPso(DXGI_FORMAT_UNKNOWN, FALSE, D3D12_DEPTH_WRITE_MASK_ZERO,
            D3D12_COMPARISON_FUNC_ALWAYS, pipeline->withoutDepth, err)) {
            return nullptr;
        }
    }
    return pipeline.release();
}

void destroyPipeline(Dx12Pipeline* pipeline) {
    delete pipeline;
}

// ---------------------------------------------------------------------------
// P6: draw 命令录制（渲染 pass 内）
// ---------------------------------------------------------------------------

bool setPipeline(CommandContext* ctx, Dx12Pipeline* pipeline, bool hasDepth, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setPipeline: no open command list"; return false; }
    if (!pipeline) { err = "setPipeline: null pipeline"; return false; }
    ID3D12PipelineState* pso = hasDepth ? pipeline->withDepth.Get() : pipeline->withoutDepth.Get();
    if (!pso) pso = pipeline->withDepth.Get();  // 无深度渲染但管线未建 withoutDepth 时回退
    if (!pipeline->rootSignature) { err = "setPipeline: null root signature"; return false; }
    // P6 崩溃修复（第 5 轮）：D3D12 规定使用任何 root 参数（SetGraphicsRootDescriptorTable
    // 等）前必须先 SetGraphicsRootSignature。此前缺失 → UMD 首次真实 draw 时对 NULL root
    // signature 解引用崩溃（hs_err：NVIDIA UMD 内读 NULL+0x2b88，AV，PC 0x7ffcd70e8fa3）。
    ctx->commandList->SetGraphicsRootSignature(pipeline->rootSignature.Get());
    ctx->commandList->SetPipelineState(pso);
    std::fprintf(stdout, "[dx12] setPipeline rootSig=%p pso=%p hasDepth=%d\n",
        (void*)pipeline->rootSignature.Get(), (void*)pso, (int)hasDepth);
    return true;
}

bool setScissor(CommandContext* ctx, int x, int y, int w, int h, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setScissor: no open command list"; return false; }
    D3D12_RECT rect{ (LONG)x, (LONG)y, (LONG)x + w, (LONG)y + h };
    ctx->commandList->RSSetScissorRects(1, &rect);
    return true;
}

bool setVertexBuffer(CommandContext* ctx, int slot, Dx12Object* buffer, long long offset, int stride, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setVertexBuffer: no open command list"; return false; }
    if (slot < 0 || slot >= 16) { err = "setVertexBuffer: slot out of range"; return false; }
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) {
        err = "setVertexBuffer: invalid buffer handle"; return false;
    }
    if (offset < 0 || offset >= buffer->size) { err = "setVertexBuffer: offset out of bounds"; return false; }
    transitionBufferTo(ctx, buffer, D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER);
    D3D12_VERTEX_BUFFER_VIEW vb{};
    vb.BufferLocation = buffer->resource->GetGPUVirtualAddress() + (UINT64)offset;
    vb.SizeInBytes = (UINT)(buffer->size - offset);
    vb.StrideInBytes = (UINT)stride;
    ctx->commandList->IASetVertexBuffers((UINT)slot, 1, &vb);
    return true;
}

bool setIndexBuffer(CommandContext* ctx, Dx12Object* buffer, int indexType, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setIndexBuffer: no open command list"; return false; }
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) {
        err = "setIndexBuffer: invalid buffer handle"; return false;
    }
    transitionBufferTo(ctx, buffer, D3D12_RESOURCE_STATE_INDEX_BUFFER);
    D3D12_INDEX_BUFFER_VIEW ib{};
    ib.BufferLocation = buffer->resource->GetGPUVirtualAddress();
    ib.SizeInBytes = (UINT)buffer->size;
    ib.Format = indexType == 1 ? DXGI_FORMAT_R32_UINT : DXGI_FORMAT_R16_UINT;
    ctx->commandList->IASetIndexBuffer(&ib);
    return true;
}

bool pushDescriptors(CommandContext* ctx, const std::vector<DrawBinding>& bindings, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "pushDescriptors: no open command list"; return false; }
    UINT count = (UINT)bindings.size();
    if (count == 0) return true;
    if (ctx->nextDrawSlot + count > kDrawHeapPerFrame) {
        err = "pushDescriptors: draw descriptor heap exhausted for this frame";
        return false;
    }
    SIZE_T base = (SIZE_T)(ctx->drawHeapSlotBase + ctx->nextDrawSlot) * gCtx.drawInc;
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.drawHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += base;
    for (UINT i = 0; i < count; ++i) {
        D3D12_CPU_DESCRIPTOR_HANDLE dst{ cpu.ptr + (SIZE_T)i * gCtx.drawInc };
        const DrawBinding& b = bindings[i];
        // P6 诊断：每个 binding 的关键句柄都打到 stdout（游戏日志可见）——
        // 若此处发生原生 AV（悬垂 Dx12Object* 等），下一轮日志能直接看出哪个
        // 句柄非法。
        std::fprintf(stdout, "[dx12] pushDesc[%u] type=%d buf=%p view=%p off=%lld len=%lld texel=%d\n",
            (unsigned)i, (int)b.type, (void*)b.buffer, (void*)b.view,
            (long long)b.offset, (long long)b.length, b.texelFormat);
        switch (b.type) {
            case 0: {  // CBV（offset 须 256 对齐；SizeInBytes 向上取整 256）
                if (!b.buffer || b.buffer->kind != Dx12Object::Kind::Buffer || !b.buffer->resource) {
                    std::fprintf(stdout, "[dx12] pushDesc[%u] INVALID CBV buffer\n", (unsigned)i);
                    err = "pushDescriptors: invalid buffer for CBV entry " + std::to_string(i);
                    return false;
                }
                // UPLOAD(GENERIC_READ) 直接可读；DEFAULT 需显式过渡（CBV 属于
                // VERTEX_AND_CONSTANT_BUFFER 状态位，D3D12 无独立 CONSTANT_BUFFER 位）
                transitionBufferTo(ctx, b.buffer, D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER);
                D3D12_CONSTANT_BUFFER_VIEW_DESC cbv{};
                cbv.BufferLocation = b.buffer->resource->GetGPUVirtualAddress() + (UINT64)b.offset;
                UINT64 cbvSize = (UINT64)b.length;
                cbvSize = (cbvSize + 255) & ~255ULL;
                if (cbvSize == 0) cbvSize = 256;
                cbv.SizeInBytes = (UINT)cbvSize;
                gCtx.device->CreateConstantBufferView(&cbv, dst);
                break;
            }
            case 1: {  // SRV：复制 texture view 的现有描述符
                if (!b.view || b.view->cpuHandle.ptr == 0) {
                    std::fprintf(stdout, "[dx12] pushDesc[%u] INVALID view\n", (unsigned)i);
                    err = "pushDescriptors: missing view for SRV entry " + std::to_string(i);
                    return false;
                }
                gCtx.device->CopyDescriptorsSimple(1, dst, b.view->cpuHandle,
                    D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
                break;
            }
            case 2: {  // SRV：texel buffer
                if (!b.buffer || b.buffer->kind != Dx12Object::Kind::Buffer || !b.buffer->resource) {
                    std::fprintf(stdout, "[dx12] pushDesc[%u] INVALID texel buffer\n", (unsigned)i);
                    err = "pushDescriptors: invalid texel buffer handle";
                    return false;
                }
                transitionBufferTo(ctx, b.buffer,
                    D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE
                        | D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);
                D3D12_SHADER_RESOURCE_VIEW_DESC srv{};
                srv.Format = toDxgiFormat(b.texelFormat);
                if (srv.Format == DXGI_FORMAT_UNKNOWN) {
                    err = "pushDescriptors: unsupported texel buffer format " + std::to_string(b.texelFormat);
                    return false;
                }
                srv.ViewDimension = D3D12_SRV_DIMENSION_BUFFER;
                srv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
                UINT elementBytes = std::max<UINT>(1u, blockSizeFor(srv.Format));
                srv.Buffer.FirstElement = (UINT)(b.offset / elementBytes);
                srv.Buffer.NumElements = (UINT)(b.length / elementBytes);
                gCtx.device->CreateShaderResourceView(b.buffer->resource.Get(), &srv, dst);
                break;
            }
            default:
                err = "pushDescriptors: unknown binding type " + std::to_string(b.type);
                return false;
        }
    }
    D3D12_GPU_DESCRIPTOR_HANDLE gpu = gCtx.drawHeap->GetGPUDescriptorHandleForHeapStart();
    gpu.ptr += base;
    ctx->nextDrawSlot += count;
    ctx->commandList->SetGraphicsRootDescriptorTable(0, gpu);
    return true;
}

bool drawIndexedInstanced(CommandContext* ctx, UINT indexCount, UINT instanceCount,
    INT startIndexLocation, INT baseVertexLocation, UINT startInstanceLocation, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "drawIndexed: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "drawIndexed: no open render pass"; return false; }
    ctx->commandList->DrawIndexedInstanced(indexCount, instanceCount,
        startIndexLocation, baseVertexLocation, startInstanceLocation);
    return true;
}

bool drawInstanced(CommandContext* ctx, UINT vertexCount, UINT instanceCount,
    UINT firstVertex, UINT startInstanceLocation, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "draw: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "draw: no open render pass"; return false; }
    ctx->commandList->DrawInstanced(vertexCount, instanceCount, firstVertex, startInstanceLocation);
    return true;
}

bool drawIndexedIndirect(CommandContext* ctx, Dx12Object* commands, long long offset,
    UINT drawCount, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "drawIndexedIndirect: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "drawIndexedIndirect: no open render pass"; return false; }
    if (!commands || commands->kind != Dx12Object::Kind::Buffer) {
        err = "drawIndexedIndirect: invalid buffer handle"; return false;
    }
    if (!gCtx.cmdSigIndexed) { err = "drawIndexedIndirect: no indexed command signature"; return false; }
    ctx->commandList->ExecuteIndirect(gCtx.cmdSigIndexed.Get(), drawCount,
        commands->resource.Get(), (UINT64)offset, nullptr, 0);
    return true;
}

bool drawIndirect(CommandContext* ctx, Dx12Object* commands, long long offset,
    UINT drawCount, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "drawIndirect: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "drawIndirect: no open render pass"; return false; }
    if (!commands || commands->kind != Dx12Object::Kind::Buffer) {
        err = "drawIndirect: invalid buffer handle"; return false;
    }
    if (!gCtx.cmdSigNonIndexed) { err = "drawIndirect: no non-indexed command signature"; return false; }
    ctx->commandList->ExecuteIndirect(gCtx.cmdSigNonIndexed.Get(), drawCount,
        commands->resource.Get(), (UINT64)offset, nullptr, 0);
    return true;
}

}  // namespace dx12mc
