// ===========================================================================
// P68：世界几何光追场景（DXR）
//
// 数据流：
//   Java（区块网格 worker 线程）
//     -> rtUpdateSection(key, section 坐标, 顶点/索引字节)
//     -> 每个 section 一份 UPLOAD 堆顶点/索引 + 一个 BLAS
//   native（渲染线程，每帧）
//     -> rtBuildAccelerationStructures：分帧构建 BLAS，脏时重建 TLAS
//     -> rtDispatch：深度重建世界位置 -> 向太阳发 shadow ray -> UAV 可见性
//
// 只在 RT 可用（ID3D12Device5 非空）时工作；否则各入口直接返回 false，
// 光影包会退化为不带光追阴影的画面。
// ===========================================================================

#include "dx12_rt_scene.h"
#include "rt_shadow_bytes.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <functional>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <unordered_set>
#include <vector>

namespace dx12mc {
namespace {

// 原版地形 BLOCK 顶点：stride 40，position 为 RGB32_FLOAT @ offset 0。
constexpr int kRtVertexStride = 40;
// 每帧最多新建多少个 BLAS：初次进入世界时 section 数量很大，分帧构建避免
// 长时间占用 scene 锁阻塞区块 worker 线程。
constexpr int kBlasBuildsPerFrame = 24;
// BLAS 建好后顶点/索引还会被 GPU 读取；延迟这么多帧再释放 UPLOAD 缓冲。
constexpr int kGeomFreeDelayFrames = 4;
// TLAS 双缓冲：重建时写另一份，避免与上一帧仍在执行的 DispatchRays 抢同一资源。
constexpr int kTlasSlotCount = 2;

bool rtAvailable(DeviceContext*& dev)
{
    DeviceContext& d = deviceContextForJni();
    dev = &d;
    return d.device && d.device5;
}

std::string hrText(HRESULT hr)
{
    char buf[16];
    snprintf(buf, sizeof(buf), "%08X", (unsigned)hr);
    return buf;
}

bool createRtBuffer(ID3D12Device* device, UINT64 size, D3D12_HEAP_TYPE heap,
    D3D12_RESOURCE_STATES state, D3D12_RESOURCE_FLAGS flags,
    ID3D12Resource** out, std::string& err, const char* what)
{
    D3D12_HEAP_PROPERTIES hp{};
    hp.Type = heap;
    hp.CreationNodeMask = 1;
    hp.VisibleNodeMask = 1;

    D3D12_RESOURCE_DESC rd{};
    rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rd.Width = (size + 255) & ~255ull;
    rd.Height = 1;
    rd.DepthOrArraySize = 1;
    rd.MipLevels = 1;
    rd.SampleDesc.Count = 1;
    rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    rd.Flags = flags;

    HRESULT hr = device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE, &rd,
        state, nullptr, IID_PPV_ARGS(out));
    if (FAILED(hr)) {
        err = std::string(what) + " hr=0x" + hrText(hr);
        return false;
    }
    return true;
}

struct RtSection {
    long long key = 0;
    int layer = 0;
    int cx = 0, cy = 0, cz = 0;
    int vertexCount = 0;
    int vertexStride = kRtVertexStride;
    int indexCount = 0;
    bool index32 = false;

    ComPtr<ID3D12Resource> vb;  // UPLOAD
    ComPtr<ID3D12Resource> ib;  // UPLOAD
    ComPtr<ID3D12Resource> blas;
    bool blasBuilt = false;
};

// section 的每一渲染层是独立网格，所以场景键 = (SectionPos, layer)。
struct RtKey {
    long long node = 0;
    int layer = 0;
    bool operator==(const RtKey& o) const { return node == o.node && layer == o.layer; }
};

struct RtKeyHash {
    size_t operator()(const RtKey& k) const
    {
        return std::hash<long long>()(k.node) * 31u + (size_t)(unsigned)k.layer;
    }
};

// 等待释放的几何缓冲 / BLAS（TLAS 与非 PixelShader 阶段的 GPU 可能仍在引用）。
struct PendingFree {
    ComPtr<ID3D12Resource> vb;
    ComPtr<ID3D12Resource> ib;
    ComPtr<ID3D12Resource> blas;
    int framesLeft = 0;
};

std::mutex gSceneMutex;
std::unordered_map<RtKey, std::unique_ptr<RtSection>, RtKeyHash> gSections;
std::vector<PendingFree> gPendingFree;
bool gTlasDirty = true;
int gStatsFrame = 0;

// ---- TLAS ----
struct TlasSlot {
    ComPtr<ID3D12Resource> tlas;
    ComPtr<ID3D12Resource> scratch;
    UINT64 tlasSize = 0;
};
TlasSlot gTlas[kTlasSlotCount];
int gTlasIndex = 0;
int gTlasActive = -1;   // 已构建且可采样的槽位；-1 = 尚未构建

ComPtr<ID3D12Resource> gInstanceBuffer;  // UPLOAD
UINT gInstanceCapacity = 0;

// ---- 光追管线 ----
struct RtPipeline {
    bool ready = false;
    bool failed = false;
    std::string failReason;

    ComPtr<ID3D12RootSignature> rootSig;
    ComPtr<ID3D12StateObject> stateObject;
    ComPtr<ID3D12StateObjectProperties> props;
    ComPtr<ID3D12DescriptorHeap> heap;   // 3 槽：t0 深度 / t1 TLAS / u0 输出
    ComPtr<ID3D12Resource> sbt;          // 3 条记录，UPLOAD
    D3D12_GPU_VIRTUAL_ADDRESS sbtAddr = 0;
};

RtPipeline gPipeline;

// ---------------------------------------------------------------------------
// 管线
// ---------------------------------------------------------------------------

bool initPipeline(std::string& err)
{
    DeviceContext* dev = nullptr;
    if (!rtAvailable(dev)) {
        err = "DXR 不可用（无 ID3D12Device5）";
        return false;
    }

    // ---- 根签名：b0 CBV + t0/t1 SRV 表 + u0 UAV 表 ----
    D3D12_DESCRIPTOR_RANGE rangeDepth{};
    rangeDepth.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    rangeDepth.NumDescriptors = 1;
    rangeDepth.BaseShaderRegister = 0;
    rangeDepth.RegisterSpace = 0;
    rangeDepth.OffsetInDescriptorsFromTableStart = 0;

    D3D12_DESCRIPTOR_RANGE rangeTlas{};
    rangeTlas.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    rangeTlas.NumDescriptors = 1;
    rangeTlas.BaseShaderRegister = 1;
    rangeTlas.RegisterSpace = 0;
    rangeTlas.OffsetInDescriptorsFromTableStart = 0;

    D3D12_DESCRIPTOR_RANGE rangeUav{};
    rangeUav.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_UAV;
    rangeUav.NumDescriptors = 1;
    rangeUav.BaseShaderRegister = 0;
    rangeUav.RegisterSpace = 0;
    rangeUav.OffsetInDescriptorsFromTableStart = 0;

    D3D12_ROOT_PARAMETER params[4]{};
    params[0].ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
    params[0].Descriptor.ShaderRegister = 0;
    params[0].Descriptor.RegisterSpace = 0;
    params[0].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    params[1].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    params[1].DescriptorTable.NumDescriptorRanges = 1;
    params[1].DescriptorTable.pDescriptorRanges = &rangeDepth;
    params[1].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    params[2].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    params[2].DescriptorTable.NumDescriptorRanges = 1;
    params[2].DescriptorTable.pDescriptorRanges = &rangeTlas;
    params[2].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    params[3].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    params[3].DescriptorTable.NumDescriptorRanges = 1;
    params[3].DescriptorTable.pDescriptorRanges = &rangeUav;
    params[3].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    D3D12_ROOT_SIGNATURE_DESC rsDesc{};
    rsDesc.NumParameters = 4;
    rsDesc.pParameters = params;
    rsDesc.NumStaticSamplers = 0;
    rsDesc.pStaticSamplers = nullptr;
    rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_NONE;

    ComPtr<ID3DBlob> blob;
    ComPtr<ID3DBlob> errBlob;
    HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1_0,
        &blob, &errBlob);
    if (FAILED(hr)) {
        err = "RT D3D12SerializeRootSignature hr=0x" + hrText(hr);
        if (errBlob) {
            err += " ";
            err.append(static_cast<const char*>(errBlob->GetBufferPointer()),
                errBlob->GetBufferSize());
        }
        return false;
    }
    hr = dev->device->CreateRootSignature(0, blob->GetBufferPointer(),
        blob->GetBufferSize(), IID_PPV_ARGS(&gPipeline.rootSig));
    if (FAILED(hr)) {
        err = "RT CreateRootSignature hr=0x" + hrText(hr);
        return false;
    }

    // ---- RTPSO ----
    D3D12_DXIL_LIBRARY_DESC libDesc{};
    libDesc.DXILLibrary.pShaderBytecode = g_rtShadowLib;
    libDesc.DXILLibrary.BytecodeLength = sizeof(g_rtShadowLib);

    D3D12_EXPORT_DESC exports[3]{};
    exports[0].Name = L"RayGen";
    exports[1].Name = L"ClosestHit";
    exports[2].Name = L"Miss";
    libDesc.NumExports = 3;
    libDesc.pExports = exports;

    D3D12_HIT_GROUP_DESC hitGroup{};
    hitGroup.HitGroupExport = L"HitGroup";
    hitGroup.Type = D3D12_HIT_GROUP_TYPE_TRIANGLES;
    hitGroup.ClosestHitShaderImport = L"ClosestHit";
    hitGroup.AnyHitShaderImport = nullptr;
    hitGroup.IntersectionShaderImport = nullptr;

    D3D12_RAYTRACING_SHADER_CONFIG shaderCfg{};
    shaderCfg.MaxPayloadSizeInBytes = sizeof(float);
    shaderCfg.MaxAttributeSizeInBytes = sizeof(float) * 2;

    D3D12_RAYTRACING_PIPELINE_CONFIG pipelineCfg{};
    pipelineCfg.MaxTraceRecursionDepth = 1;  // 只有主 shadow ray，无递归

    D3D12_GLOBAL_ROOT_SIGNATURE globalRoot{};
    globalRoot.pGlobalRootSignature = gPipeline.rootSig.Get();

    LPCWSTR assocExports[] = { L"RayGen", L"ClosestHit", L"Miss", L"HitGroup" };
    D3D12_SUBOBJECT_TO_EXPORTS_ASSOCIATION assoc{};
    assoc.NumExports = 4;
    assoc.pExports = assocExports;

    D3D12_STATE_SUBOBJECT subobjects[6]{};
    subobjects[0].Type = D3D12_STATE_SUBOBJECT_TYPE_DXIL_LIBRARY;
    subobjects[0].pDesc = &libDesc;
    subobjects[1].Type = D3D12_STATE_SUBOBJECT_TYPE_HIT_GROUP;
    subobjects[1].pDesc = &hitGroup;
    subobjects[2].Type = D3D12_STATE_SUBOBJECT_TYPE_RAYTRACING_SHADER_CONFIG;
    subobjects[2].pDesc = &shaderCfg;
    subobjects[3].Type = D3D12_STATE_SUBOBJECT_TYPE_RAYTRACING_PIPELINE_CONFIG;
    subobjects[3].pDesc = &pipelineCfg;
    subobjects[4].Type = D3D12_STATE_SUBOBJECT_TYPE_GLOBAL_ROOT_SIGNATURE;
    subobjects[4].pDesc = &globalRoot;
    subobjects[5].Type = D3D12_STATE_SUBOBJECT_TYPE_SUBOBJECT_TO_EXPORTS_ASSOCIATION;
    subobjects[5].pDesc = &assoc;
    assoc.pSubobjectToAssociate = &subobjects[2];

    D3D12_STATE_OBJECT_DESC soDesc{};
    soDesc.Type = D3D12_STATE_OBJECT_TYPE_RAYTRACING_PIPELINE;
    soDesc.NumSubobjects = 6;
    soDesc.pSubobjects = subobjects;

    hr = dev->device5->CreateStateObject(&soDesc, IID_PPV_ARGS(&gPipeline.stateObject));
    if (FAILED(hr)) {
        err = "RT CreateStateObject hr=0x" + hrText(hr);
        return false;
    }
    hr = gPipeline.stateObject.As(&gPipeline.props);
    if (FAILED(hr)) {
        err = "RT QueryInterface(ID3D12StateObjectProperties) hr=0x" + hrText(hr);
        return false;
    }

    // ---- 描述符堆 ----
    D3D12_DESCRIPTOR_HEAP_DESC hd{};
    hd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    hd.NumDescriptors = 3;
    hd.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
    hr = dev->device->CreateDescriptorHeap(&hd, IID_PPV_ARGS(&gPipeline.heap));
    if (FAILED(hr)) {
        err = "RT CreateDescriptorHeap hr=0x" + hrText(hr);
        return false;
    }

    // ---- SBT：raygen / miss / hitgroup ----
    const UINT record = D3D12_SHADER_IDENTIFIER_SIZE_IN_BYTES;
    const UINT64 sbtSize = (UINT64)record * 3;
    if (!createRtBuffer(dev->device.Get(), sbtSize, D3D12_HEAP_TYPE_UPLOAD,
            D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
            &gPipeline.sbt, err, "RT sbt")) {
        return false;
    }
    void* mapped = nullptr;
    if (FAILED(gPipeline.sbt->Map(0, nullptr, &mapped))) {
        err = "RT sbt Map 失败";
        return false;
    }
    const void* rg = gPipeline.props->GetShaderIdentifier(L"RayGen");
    const void* ms = gPipeline.props->GetShaderIdentifier(L"Miss");
    const void* hg = gPipeline.props->GetShaderIdentifier(L"HitGroup");
    if (!rg || !ms || !hg) {
        gPipeline.sbt->Unmap(0, nullptr);
        err = "RT GetShaderIdentifier 返回空";
        return false;
    }
    unsigned char* p = static_cast<unsigned char*>(mapped);
    memcpy(p, rg, record);
    memcpy(p + record, ms, record);
    memcpy(p + record * 2, hg, record);
    gPipeline.sbt->Unmap(0, nullptr);
    gPipeline.sbtAddr = gPipeline.sbt->GetGPUVirtualAddress();

    gPipeline.ready = true;
    dbgLog("RT scene: pipeline ready (payload=%u B, DXIL=%u B)",
        (unsigned)sizeof(float), (unsigned)sizeof(g_rtShadowLib));
    return true;
}

}  // namespace

// ---------------------------------------------------------------------------
// 场景登记（可在 worker 线程调用）
// ---------------------------------------------------------------------------

bool rtUpdateSection(long long key, int layer, int cx, int cy, int cz,
    const void* vertices, int vertexBytes, int vertexStride, int vertexCount,
    const void* indices, int indexBytes, int indexCount, bool index32,
    std::string& err)
{
    DeviceContext* dev = nullptr;
    if (!rtAvailable(dev)) {
        err = "DXR 不可用";
        return false;
    }
    if (!vertices || vertexBytes <= 0 || vertexCount < 3 || !indices || indexCount < 3) {
        err = "RT section 几何为空";
        return false;
    }
    if (vertexStride < 12) {
        err = "RT section 顶点 stride 过小: " + std::to_string(vertexStride);
        return false;
    }

    ComPtr<ID3D12Resource> vb;
    ComPtr<ID3D12Resource> ib;
    if (!createRtBuffer(dev->device.Get(), (UINT64)vertexBytes, D3D12_HEAP_TYPE_UPLOAD,
            D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
            &vb, err, "RT section vb")) {
        return false;
    }
    if (!createRtBuffer(dev->device.Get(), (UINT64)indexBytes, D3D12_HEAP_TYPE_UPLOAD,
            D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
            &ib, err, "RT section ib")) {
        return false;
    }

    void* dst = nullptr;
    if (FAILED(vb->Map(0, nullptr, &dst))) {
        err = "RT section vb Map 失败";
        return false;
    }
    memcpy(dst, vertices, (size_t)vertexBytes);
    vb->Unmap(0, nullptr);

    dst = nullptr;
    if (FAILED(ib->Map(0, nullptr, &dst))) {
        err = "RT section ib Map 失败";
        return false;
    }
    memcpy(dst, indices, (size_t)indexBytes);
    ib->Unmap(0, nullptr);

    auto section = std::make_unique<RtSection>();
    section->key = key;
    section->layer = layer;
    section->cx = cx;
    section->cy = cy;
    section->cz = cz;
    section->vertexCount = vertexCount;
    section->vertexStride = vertexStride;
    section->indexCount = indexCount;
    section->index32 = index32;
    section->vb = vb;
    section->ib = ib;

    const RtKey mapKey{key, layer};
    {
        std::lock_guard<std::mutex> lock(gSceneMutex);
        auto it = gSections.find(mapKey);
        if (it != gSections.end() && it->second->blasBuilt) {
            // 旧几何已被 BLAS 读过，延迟释放。
            PendingFree pending;
            pending.vb = it->second->vb;
            pending.ib = it->second->ib;
            pending.framesLeft = kGeomFreeDelayFrames;
            gPendingFree.push_back(std::move(pending));
        }
        gSections[mapKey] = std::move(section);
        gTlasDirty = true;
    }
    return true;
}

bool rtRemoveSection(long long key)
{
    std::lock_guard<std::mutex> lock(gSceneMutex);
    bool removed = false;
    for (auto it = gSections.begin(); it != gSections.end();) {
        if (it->first.node != key) {
            ++it;
            continue;
        }
        if (it->second->blasBuilt) {
            PendingFree pending;
            pending.vb = it->second->vb;
            pending.ib = it->second->ib;
            pending.framesLeft = kGeomFreeDelayFrames;
            gPendingFree.push_back(std::move(pending));
        }
        it = gSections.erase(it);
        removed = true;
    }
    if (removed) {
        gTlasDirty = true;
    }
    return removed;
}

void rtResetScene()
{
    std::lock_guard<std::mutex> lock(gSceneMutex);
    gSections.clear();
    gPendingFree.clear();
    gTlasDirty = true;
    gTlasActive = -1;
}

RtStats rtGetStats()
{
    std::lock_guard<std::mutex> lock(gSceneMutex);
    RtStats stats;
    std::unordered_set<long long> nodes;
    for (const auto& entry : gSections) {
        nodes.insert(entry.first.node);
        if (!entry.second->blasBuilt) {
            continue;
        }
        ++stats.builtSections;
        stats.triangles += entry.second->indexCount / 3;
    }
    stats.sections = (int)nodes.size();
    stats.tlasReady = gTlasActive >= 0;
    return stats;
}

// ---------------------------------------------------------------------------
// 加速结构构建（渲染线程）
// ---------------------------------------------------------------------------

namespace {

bool buildOneBlas(ID3D12GraphicsCommandList4* list, RtSection& section, std::string& err)
{
    DeviceContext& dev = deviceContextForJni();

    D3D12_RAYTRACING_GEOMETRY_DESC geom{};
    geom.Type = D3D12_RAYTRACING_GEOMETRY_TYPE_TRIANGLES;
    geom.Flags = D3D12_RAYTRACING_GEOMETRY_FLAG_OPAQUE;
    geom.Triangles.VertexBuffer.StartAddress = section.vb->GetGPUVirtualAddress();
    geom.Triangles.VertexBuffer.StrideInBytes = (UINT)section.vertexStride;
    geom.Triangles.VertexCount = (UINT)section.vertexCount;
    geom.Triangles.VertexFormat = DXGI_FORMAT_R32G32B32_FLOAT;
    geom.Triangles.IndexBuffer = section.ib->GetGPUVirtualAddress();
    geom.Triangles.IndexCount = (UINT)section.indexCount;
    geom.Triangles.IndexFormat = section.index32 ? DXGI_FORMAT_R32_UINT : DXGI_FORMAT_R16_UINT;
    geom.Triangles.Transform3x4 = 0;

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS inputs{};
    inputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL;
    inputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
    inputs.NumDescs = 1;
    inputs.pGeometryDescs = &geom;
    inputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

    D3D12_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO info{};
    dev.device5->GetRaytracingAccelerationStructurePrebuildInfo(&inputs, &info);
    if (info.ResultDataMaxSizeInBytes == 0 || info.ScratchDataSizeInBytes == 0) {
        err = "BLAS prebuild info 为空";
        return false;
    }

    ComPtr<ID3D12Resource> blas;
    if (!createRtBuffer(dev.device.Get(), info.ResultDataMaxSizeInBytes,
            D3D12_HEAP_TYPE_DEFAULT, D3D12_RESOURCE_STATE_RAYTRACING_ACCELERATION_STRUCTURE,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &blas, err, "BLAS")) {
        return false;
    }
    ComPtr<ID3D12Resource> scratch;
    if (!createRtBuffer(dev.device.Get(), info.ScratchDataSizeInBytes,
            D3D12_HEAP_TYPE_DEFAULT, D3D12_RESOURCE_STATE_UNORDERED_ACCESS,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &scratch, err, "BLAS scratch")) {
        return false;
    }

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_DESC desc{};
    desc.Inputs = inputs;
    desc.DestAccelerationStructureData = blas->GetGPUVirtualAddress();
    desc.ScratchAccelerationStructureData = scratch->GetGPUVirtualAddress();
    desc.SourceAccelerationStructureData = 0;
    list->BuildRaytracingAccelerationStructure(&desc, 0, nullptr);

    D3D12_RESOURCE_BARRIER barrier{};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_UAV;
    barrier.UAV.pResource = blas.Get();
    list->ResourceBarrier(1, &barrier);

    section.blas = blas;
    section.blasBuilt = true;
    return true;
}

struct InstanceEntry {
    long long key;
    int layer;
    float tx, ty, tz;
};

bool rebuildTlas(ID3D12GraphicsCommandList4* list4, std::string& err)
{
    DeviceContext& dev = deviceContextForJni();

    std::vector<InstanceEntry> entries;
    for (const auto& entry : gSections) {
        const RtSection& s = *entry.second;
        if (!s.blasBuilt) {
            continue;
        }
        InstanceEntry e;
        e.key = s.key;
        e.layer = s.layer;
        // section 局部坐标 0..16 -> 世界坐标：section 原点 = 坐标 * 16
        e.tx = (float)(s.cx * 16);
        e.ty = (float)(s.cy * 16);
        e.tz = (float)(s.cz * 16);
        entries.push_back(e);
    }
    if (entries.empty()) {
        gTlasActive = -1;
        gTlasDirty = false;
        return true;
    }

    const UINT count = (UINT)entries.size();
    const UINT64 instanceBytes = (UINT64)count * sizeof(D3D12_RAYTRACING_INSTANCE_DESC);

    if (!gInstanceBuffer || gInstanceCapacity < count) {
        UINT capacity = gInstanceCapacity;
        while (capacity < count) {
            capacity = capacity == 0 ? 256 : capacity * 2;
        }
        ComPtr<ID3D12Resource> buffer;
        if (!createRtBuffer(dev.device.Get(), (UINT64)capacity * sizeof(D3D12_RAYTRACING_INSTANCE_DESC),
                D3D12_HEAP_TYPE_UPLOAD, D3D12_RESOURCE_STATE_GENERIC_READ,
                D3D12_RESOURCE_FLAG_NONE, &buffer, err, "TLAS instances")) {
            return false;
        }
        gInstanceBuffer = buffer;
        gInstanceCapacity = capacity;
    }

    void* mapped = nullptr;
    if (FAILED(gInstanceBuffer->Map(0, nullptr, &mapped))) {
        err = "TLAS instances Map 失败";
        return false;
    }
    auto* descs = static_cast<D3D12_RAYTRACING_INSTANCE_DESC*>(mapped);
    memset(descs, 0, (size_t)instanceBytes);
    for (UINT i = 0; i < count; ++i) {
        auto it = gSections.find(RtKey{entries[i].key, entries[i].layer});
        if (it == gSections.end() || !it->second->blasBuilt) {
            continue;
        }
        D3D12_RAYTRACING_INSTANCE_DESC& d = descs[i];
        // 仅平移：3x4 row-major 仿射矩阵
        d.Transform[0][0] = 1.0f;
        d.Transform[1][1] = 1.0f;
        d.Transform[2][2] = 1.0f;
        d.Transform[0][3] = entries[i].tx;
        d.Transform[1][3] = entries[i].ty;
        d.Transform[2][3] = entries[i].tz;
        d.InstanceID = i;
        d.InstanceContributionToHitGroupIndex = 0;
        d.InstanceMask = 0xFF;
        // MC 的三角形绕序与 DXR 默认的顺时针前向不一定一致，关闭裁剪保证不漏遮挡。
        d.Flags = D3D12_RAYTRACING_INSTANCE_FLAG_TRIANGLE_CULL_DISABLE;
        d.AccelerationStructure = it->second->blas->GetGPUVirtualAddress();
    }
    gInstanceBuffer->Unmap(0, nullptr);

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS inputs{};
    inputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL;
    inputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
    inputs.NumDescs = count;
    inputs.InstanceDescs = gInstanceBuffer->GetGPUVirtualAddress();
    inputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

    D3D12_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO info{};
    dev.device5->GetRaytracingAccelerationStructurePrebuildInfo(&inputs, &info);
    if (info.ResultDataMaxSizeInBytes == 0) {
        err = "TLAS prebuild info 为空";
        return false;
    }

    // 双缓冲：写另一个槽位，避免与上一帧仍在执行的 DispatchRays 冲突。
    const int slot = (gTlasActive >= 0) ? (1 - gTlasActive) : 0;
    TlasSlot& target = gTlas[slot];
    if (!target.tlas || target.tlasSize < info.ResultDataMaxSizeInBytes) {
        ComPtr<ID3D12Resource> tlas;
        if (!createRtBuffer(dev.device.Get(), info.ResultDataMaxSizeInBytes,
                D3D12_HEAP_TYPE_DEFAULT, D3D12_RESOURCE_STATE_RAYTRACING_ACCELERATION_STRUCTURE,
                D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &tlas, err, "TLAS")) {
            return false;
        }
        target.tlas = tlas;
        target.tlasSize = info.ResultDataMaxSizeInBytes;
    }
    if (!target.scratch || target.scratch->GetDesc().Width < info.ScratchDataSizeInBytes) {
        ComPtr<ID3D12Resource> scratch;
        if (!createRtBuffer(dev.device.Get(), info.ScratchDataSizeInBytes,
                D3D12_HEAP_TYPE_DEFAULT, D3D12_RESOURCE_STATE_UNORDERED_ACCESS,
                D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &scratch, err, "TLAS scratch")) {
            return false;
        }
        target.scratch = scratch;
    }

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_DESC desc{};
    desc.Inputs = inputs;
    desc.DestAccelerationStructureData = target.tlas->GetGPUVirtualAddress();
    desc.ScratchAccelerationStructureData = target.scratch->GetGPUVirtualAddress();
    desc.SourceAccelerationStructureData = 0;
    list4->BuildRaytracingAccelerationStructure(&desc, 0, nullptr);

    D3D12_RESOURCE_BARRIER barrier{};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_UAV;
    barrier.UAV.pResource = target.tlas.Get();
    list4->ResourceBarrier(1, &barrier);

    gTlasIndex = slot;
    gTlasActive = slot;
    gTlasDirty = false;
    return true;
}

}  // namespace

bool rtBuildAccelerationStructures(CommandContext* ctx, std::string& err)
{
    DeviceContext* dev = nullptr;
    if (!rtAvailable(dev)) {
        err = "DXR 不可用";
        return false;
    }
    if (!ctx || !ctx->commandList) {
        err = "RT: 命令列表未打开";
        return false;
    }

    std::lock_guard<std::mutex> lock(gSceneMutex);

    ComPtr<ID3D12GraphicsCommandList4> list4;
    if (FAILED(ctx->commandList->QueryInterface(IID_PPV_ARGS(&list4)))) {
        err = "RT: 命令列表没有 GraphicsCommandList4";
        return false;
    }

    // 1) 分帧构建 BLAS
    int built = 0;
    for (auto& entry : gSections) {
        RtSection& s = *entry.second;
        if (s.blasBuilt) {
            continue;
        }
        if (built >= kBlasBuildsPerFrame) {
            break;
        }
        if (buildOneBlas(list4.Get(), s, err)) {
            gTlasDirty = true;
            ++built;
        }
        // 单个 section 失败不致命（可能是退化三角形），继续下一个。
    }

    // 2) 延迟释放 BLAS 已消费的几何缓冲
    for (size_t i = 0; i < gPendingFree.size();) {
        PendingFree& p = gPendingFree[i];
        if (--p.framesLeft <= 0) {
            gPendingFree.erase(gPendingFree.begin() + (long long)i);
        } else {
            ++i;
        }
    }

    // 3) TLAS
    if (gTlasDirty) {
        rebuildTlas(list4.Get(), err);
    }

    // 诊断：每 120 帧打印一次场景规模，便于确认几何是否真的送进来了。
    if (++gStatsFrame >= 120) {
        gStatsFrame = 0;
        int built = 0;
        long long tris = 0;
        for (const auto& entry : gSections) {
            if (!entry.second->blasBuilt) {
                continue;
            }
            ++built;
            tris += entry.second->indexCount / 3;
        }
        dbgLog("RT scene: sections=%zu built=%d tris=%lld tlas=%d",
            gSections.size(), built, tris, gTlasActive);
    }
    return true;
}

bool rtEnsurePipeline(std::string& err)
{
    if (gPipeline.ready) {
        return true;
    }
    if (gPipeline.failed) {
        err = gPipeline.failReason;
        return false;
    }
    std::string e;
    if (!initPipeline(e)) {
        gPipeline.failed = true;
        gPipeline.failReason = e;
        dbgLog("RT scene: 管线初始化失败（%s），光追 pass 将被跳过", e.c_str());
        err = e;
        return false;
    }
    return true;
}

// ---------------------------------------------------------------------------
// DispatchRays
// ---------------------------------------------------------------------------

bool rtDispatch(CommandContext* ctx, Dx12Object* depthView,
    Dx12Object* outTex, Dx12Object* uniformBuffer, long long uniformOffset,
    int width, int height, std::string& err)
{
    std::string e;
    if (!rtEnsurePipeline(e)) {
        err = e;
        return false;
    }
    if (!ctx || !ctx->commandList || !depthView || !outTex || !uniformBuffer) {
        err = "RT dispatch 参数不完整";
        return false;
    }
    if (gTlasActive < 0) {
        err = "RT: TLAS 尚未构建";
        return false;
    }
    if (!depthView->sourceTexture) {
        err = "RT: 深度视图没有底层纹理";
        return false;
    }

    DeviceContext& dev = deviceContextForJni();
    ComPtr<ID3D12GraphicsCommandList4> list4;
    if (FAILED(ctx->commandList->QueryInterface(IID_PPV_ARGS(&list4)))) {
        err = "RT: 命令列表没有 GraphicsCommandList4";
        return false;
    }

    // ---- 写描述符：t0 深度 / t1 TLAS / u0 输出 ----
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gPipeline.heap->GetCPUDescriptorHandleForHeapStart();
    const UINT inc = dev.device->GetDescriptorHandleIncrementSize(
        D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);

    D3D12_CPU_DESCRIPTOR_HANDLE hDepth = cpu;
    D3D12_CPU_DESCRIPTOR_HANDLE hTlas{ cpu.ptr + inc };
    D3D12_CPU_DESCRIPTOR_HANDLE hUav{ cpu.ptr + (SIZE_T)inc * 2 };

    dev.device->CopyDescriptorsSimple(1, hDepth, depthView->cpuHandle,
        D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);

    D3D12_SHADER_RESOURCE_VIEW_DESC tlasSrv{};
    tlasSrv.ViewDimension = D3D12_SRV_DIMENSION_RAYTRACING_ACCELERATION_STRUCTURE;
    tlasSrv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    tlasSrv.RaytracingAccelerationStructure.Location =
        gTlas[gTlasActive].tlas->GetGPUVirtualAddress();
    dev.device->CreateShaderResourceView(nullptr, &tlasSrv, hTlas);

    D3D12_UNORDERED_ACCESS_VIEW_DESC uav{};
    uav.Format = outTex->dxgiFormat;
    uav.ViewDimension = D3D12_UAV_DIMENSION_TEXTURE2D;
    uav.Texture2D.MipSlice = 0;
    uav.Texture2D.PlaneSlice = 0;
    dev.device->CreateUnorderedAccessView(outTex->resource.Get(), nullptr, &uav, hUav);

    // ---- 资源状态 ----
    transitionTextureTo(ctx, depthView->sourceTexture,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTextureTo(ctx, outTex, D3D12_RESOURCE_STATE_UNORDERED_ACCESS);

    // ---- 绑定 & dispatch ----
    ID3D12DescriptorHeap* heaps[] = { gPipeline.heap.Get() };
    list4->SetDescriptorHeaps(1, heaps);
    list4->SetComputeRootSignature(gPipeline.rootSig.Get());
    list4->SetComputeRootConstantBufferView(0,
        uniformBuffer->resource->GetGPUVirtualAddress() + (UINT64)uniformOffset);

    D3D12_GPU_DESCRIPTOR_HANDLE gpu = gPipeline.heap->GetGPUDescriptorHandleForHeapStart();
    D3D12_GPU_DESCRIPTOR_HANDLE gpuDepth{ gpu.ptr };
    D3D12_GPU_DESCRIPTOR_HANDLE gpuTlas{ gpu.ptr + inc };
    D3D12_GPU_DESCRIPTOR_HANDLE gpuUav{ gpu.ptr + (SIZE_T)inc * 2 };
    list4->SetComputeRootDescriptorTable(1, gpuDepth);
    list4->SetComputeRootDescriptorTable(2, gpuTlas);
    list4->SetComputeRootDescriptorTable(3, gpuUav);

    list4->SetPipelineState1(gPipeline.stateObject.Get());

    const UINT record = D3D12_SHADER_IDENTIFIER_SIZE_IN_BYTES;
    D3D12_DISPATCH_RAYS_DESC dispatch{};
    dispatch.RayGenerationShaderRecord.StartAddress = gPipeline.sbtAddr;
    dispatch.RayGenerationShaderRecord.SizeInBytes = record;
    dispatch.MissShaderTable.StartAddress = gPipeline.sbtAddr + record;
    dispatch.MissShaderTable.SizeInBytes = record;
    dispatch.MissShaderTable.StrideInBytes = record;
    dispatch.HitGroupTable.StartAddress = gPipeline.sbtAddr + record * 2;
    dispatch.HitGroupTable.SizeInBytes = record;
    dispatch.HitGroupTable.StrideInBytes = record;
    dispatch.Width = (UINT)std::max(width, 1);
    dispatch.Height = (UINT)std::max(height, 1);
    dispatch.Depth = 1;
    list4->DispatchRays(&dispatch);

    // 输出纹理随后会被普通 pass 采样。
    transitionTextureTo(ctx, outTex, D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);
    return true;
}

void rtResetAll()
{
    std::lock_guard<std::mutex> lock(gSceneMutex);
    gSections.clear();
    gPendingFree.clear();
    gInstanceBuffer.Reset();
    gInstanceCapacity = 0;
    for (int i = 0; i < kTlasSlotCount; ++i) {
        gTlas[i].tlas.Reset();
        gTlas[i].scratch.Reset();
        gTlas[i].tlasSize = 0;
    }
    gTlasIndex = 0;
    gTlasActive = -1;
    gTlasDirty = true;

    gPipeline = RtPipeline{};
}

}  // namespace dx12mc
