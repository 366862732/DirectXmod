// DXR（DirectX Raytracing）冒烟测试：验证 DX12 Ultimate 光追链路在真实驱动上可用。
//
// 链路：单三角形 BLAS -> 1 实例 TLAS -> RTPSO（CreateStateObject）
//       -> DispatchRays(64x64) -> UAV 写回 -> CopyTextureRegion -> Map readback。
//
// 该路径独立于现有光栅渲染：自带 queue / allocator / fence，跑完即释放，
// 不改变任何既有渲染状态。着色器为 dxc 预编译的 lib_6_3 DXIL（dxr_shader_bytes.h）。

#include "dx12_device.h"
#include "dxr_shader_bytes.h"
#include "dxr_demo_bytes.h"

#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

namespace dx12mc {
namespace {

std::string hrText2(HRESULT hr) {
    char buf[16];
    snprintf(buf, sizeof(buf), "%08X", (unsigned)hr);
    return buf;
}

bool createBuffer(ID3D12Device* dev, UINT64 size, D3D12_HEAP_TYPE heapType,
                  D3D12_RESOURCE_STATES state, D3D12_RESOURCE_FLAGS flags,
                  ID3D12Resource** out, std::string& err, const char* what) {
    D3D12_HEAP_PROPERTIES hp{};
    hp.Type = heapType;
    hp.CreationNodeMask = 1;
    hp.VisibleNodeMask = 1;

    D3D12_RESOURCE_DESC rd{};
    rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rd.Width = size;
    rd.Height = 1;
    rd.DepthOrArraySize = 1;
    rd.MipLevels = 1;
    rd.SampleDesc.Count = 1;
    rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    rd.Flags = flags;

    HRESULT hr = dev->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE, &rd, state,
        nullptr, IID_PPV_ARGS(out));
    if (FAILED(hr)) {
        err = std::string(what) + " hr=0x" + hrText2(hr);
        return false;
    }
    return true;
}

// 上传堆缓冲：Map 后拷贝数据，Unmap。
bool uploadBuffer(ID3D12Device* dev, const void* data, size_t size,
                  ID3D12Resource** out, std::string& err, const char* what) {
    if (!createBuffer(dev, size, D3D12_HEAP_TYPE_UPLOAD,
            D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
            out, err, what)) {
        return false;
    }
    void* dst = nullptr;
    D3D12_RANGE range{0, size};
    HRESULT hr = (*out)->Map(0, &range, &dst);
    if (FAILED(hr)) {
        err = std::string(what) + " Map hr=0x" + hrText2(hr);
        return false;
    }
    memcpy(dst, data, size);
    D3D12_RANGE written{0, size};
    (*out)->Unmap(0, &written);
    return true;
}

}  // namespace

std::string runDxrSmokeTest() {
    DeviceContext& ctx = deviceContextForJni();
    if (!ctx.device || !ctx.device5) {
        return "DXR smoke: skipped (no ID3D12Device5 / raytracing tier 0)";
    }
    ID3D12Device5* dev5 = ctx.device5.Get();
    ID3D12Device* dev = ctx.device.Get();
    const double t0 = nowMs();
    std::string err;

    // ---- 输出纹理 64x64 R8G8B8A8_UNORM（UAV）----
    const UINT kDim = 64;
    ComPtr<ID3D12Resource> output;
    {
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_DEFAULT;
        hp.CreationNodeMask = 1;
        hp.VisibleNodeMask = 1;

        D3D12_RESOURCE_DESC rd{};
        rd.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        rd.Width = kDim;
        rd.Height = kDim;
        rd.DepthOrArraySize = 1;
        rd.MipLevels = 1;
        rd.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        rd.SampleDesc.Count = 1;
        rd.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
        rd.Flags = D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;

        HRESULT hr = dev->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE, &rd,
            D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(&output));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL output texture hr=0x" + hrText2(hr);
        }
    }

    // ---- readback 缓冲 ----
    const UINT kRowPitch = kDim * 4;
    const UINT64 kReadbackSize = (UINT64)kRowPitch * kDim;
    ComPtr<ID3D12Resource> readback;
    if (!createBuffer(dev, kReadbackSize, D3D12_HEAP_TYPE_READBACK,
            D3D12_RESOURCE_STATE_COPY_DEST, D3D12_RESOURCE_FLAG_NONE,
            &readback, err, "readback")) {
        return "DXR smoke: FAIL " + err;
    }

    // ---- 几何：单三角形 ----
    const float kVertices[9] = {
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
         0.0f,  1.0f, 0.0f,
    };
    ComPtr<ID3D12Resource> vertexBuffer;
    if (!uploadBuffer(dev, kVertices, sizeof(kVertices), &vertexBuffer, err, "vb")) {
        return "DXR smoke: FAIL " + err;
    }

    // ---- BLAS ----
    D3D12_RAYTRACING_GEOMETRY_DESC geom{};
    geom.Type = D3D12_RAYTRACING_GEOMETRY_TYPE_TRIANGLES;
    geom.Flags = D3D12_RAYTRACING_GEOMETRY_FLAG_OPAQUE;
    geom.Triangles.VertexBuffer.StartAddress = vertexBuffer->GetGPUVirtualAddress();
    geom.Triangles.VertexBuffer.StrideInBytes = sizeof(float) * 3;
    geom.Triangles.VertexCount = 3;
    geom.Triangles.VertexFormat = DXGI_FORMAT_R32G32B32_FLOAT;
    geom.Triangles.IndexBuffer = 0;
    geom.Triangles.IndexCount = 0;
    geom.Triangles.Transform3x4 = 0;

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS blasInputs{};
    blasInputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL;
    blasInputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
    blasInputs.NumDescs = 1;
    blasInputs.pGeometryDescs = &geom;
    blasInputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

    D3D12_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO blasInfo{};
    dev5->GetRaytracingAccelerationStructurePrebuildInfo(&blasInputs, &blasInfo);
    if (blasInfo.ResultDataMaxSizeInBytes == 0) {
        return "DXR smoke: FAIL BLAS prebuild info is zero";
    }

    ComPtr<ID3D12Resource> blas;
    if (!createBuffer(dev, blasInfo.ResultDataMaxSizeInBytes, D3D12_HEAP_TYPE_DEFAULT,
            D3D12_RESOURCE_STATE_RAYTRACING_ACCELERATION_STRUCTURE,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &blas, err, "blas")) {
        return "DXR smoke: FAIL " + err;
    }

    // ---- TLAS 实例数据 ----
    D3D12_RAYTRACING_INSTANCE_DESC inst{};
    inst.Transform[0][0] = 1.0f;
    inst.Transform[1][1] = 1.0f;
    inst.Transform[2][2] = 1.0f;
    inst.InstanceID = 0;
    inst.InstanceMask = 0xFF;
    inst.InstanceContributionToHitGroupIndex = 0;
    inst.Flags = D3D12_RAYTRACING_INSTANCE_FLAG_NONE;
    inst.AccelerationStructure = blas->GetGPUVirtualAddress();

    ComPtr<ID3D12Resource> instanceBuffer;
    if (!uploadBuffer(dev, &inst, sizeof(inst), &instanceBuffer, err, "instance desc")) {
        return "DXR smoke: FAIL " + err;
    }

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS tlasInputs{};
    tlasInputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL;
    tlasInputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
    tlasInputs.NumDescs = 1;
    tlasInputs.InstanceDescs = instanceBuffer->GetGPUVirtualAddress();
    tlasInputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

    D3D12_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO tlasInfo{};
    dev5->GetRaytracingAccelerationStructurePrebuildInfo(&tlasInputs, &tlasInfo);
    if (tlasInfo.ResultDataMaxSizeInBytes == 0) {
        return "DXR smoke: FAIL TLAS prebuild info is zero";
    }

    ComPtr<ID3D12Resource> tlas;
    if (!createBuffer(dev, tlasInfo.ResultDataMaxSizeInBytes, D3D12_HEAP_TYPE_DEFAULT,
            D3D12_RESOURCE_STATE_RAYTRACING_ACCELERATION_STRUCTURE,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &tlas, err, "tlas")) {
        return "DXR smoke: FAIL " + err;
    }

    const UINT64 scratchSize = (blasInfo.ScratchDataSizeInBytes > tlasInfo.ScratchDataSizeInBytes
        ? blasInfo.ScratchDataSizeInBytes : tlasInfo.ScratchDataSizeInBytes);
    ComPtr<ID3D12Resource> scratch;
    if (!createBuffer(dev, scratchSize, D3D12_HEAP_TYPE_DEFAULT,
            D3D12_RESOURCE_STATE_UNORDERED_ACCESS,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &scratch, err, "scratch")) {
        return "DXR smoke: FAIL " + err;
    }

    // ---- 描述符堆：SRV(t0)=TLAS, UAV(u0)=输出纹理 ----
    ComPtr<ID3D12DescriptorHeap> heap;
    {
        D3D12_DESCRIPTOR_HEAP_DESC hd{};
        hd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
        hd.NumDescriptors = 2;
        hd.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
        HRESULT hr = dev->CreateDescriptorHeap(&hd, IID_PPV_ARGS(&heap));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL descriptor heap hr=0x" + hrText2(hr);
        }
    }
    const UINT descSize = dev->GetDescriptorHandleIncrementSize(
        D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    D3D12_CPU_DESCRIPTOR_HANDLE cpuSrv = heap->GetCPUDescriptorHandleForHeapStart();
    D3D12_CPU_DESCRIPTOR_HANDLE cpuUav = cpuSrv;
    cpuUav.ptr += descSize;
    D3D12_GPU_DESCRIPTOR_HANDLE gpuSrv = heap->GetGPUDescriptorHandleForHeapStart();
    D3D12_GPU_DESCRIPTOR_HANDLE gpuUav = gpuSrv;
    gpuUav.ptr += descSize;

    {
        D3D12_SHADER_RESOURCE_VIEW_DESC srv{};
        srv.Format = DXGI_FORMAT_UNKNOWN;
        srv.ViewDimension = D3D12_SRV_DIMENSION_RAYTRACING_ACCELERATION_STRUCTURE;
        srv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
        srv.RaytracingAccelerationStructure.Location = tlas->GetGPUVirtualAddress();
        dev->CreateShaderResourceView(nullptr, &srv, cpuSrv);

        D3D12_UNORDERED_ACCESS_VIEW_DESC uav{};
        uav.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        uav.ViewDimension = D3D12_UAV_DIMENSION_TEXTURE2D;
        uav.Texture2D.MipSlice = 0;
        dev->CreateUnorderedAccessView(output.Get(), nullptr, &uav, cpuUav);
    }

    // ---- 全局根签名：两个 descriptor table（t0 / u0）----
    ComPtr<ID3D12RootSignature> rootSig;
    {
        D3D12_DESCRIPTOR_RANGE ranges[2]{};
        ranges[0].RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
        ranges[0].NumDescriptors = 1;
        ranges[0].BaseShaderRegister = 0;
        ranges[0].RegisterSpace = 0;
        ranges[0].OffsetInDescriptorsFromTableStart = 0;
        ranges[1].RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_UAV;
        ranges[1].NumDescriptors = 1;
        ranges[1].BaseShaderRegister = 0;
        ranges[1].RegisterSpace = 0;
        ranges[1].OffsetInDescriptorsFromTableStart = 0;

        D3D12_ROOT_PARAMETER params[2]{};
        params[0].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
        params[0].DescriptorTable.NumDescriptorRanges = 1;
        params[0].DescriptorTable.pDescriptorRanges = &ranges[0];
        params[0].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
        params[1].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
        params[1].DescriptorTable.NumDescriptorRanges = 1;
        params[1].DescriptorTable.pDescriptorRanges = &ranges[1];
        params[1].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

        D3D12_ROOT_SIGNATURE_DESC rsDesc{};
        rsDesc.NumParameters = 2;
        rsDesc.pParameters = params;
        rsDesc.NumStaticSamplers = 0;
        rsDesc.pStaticSamplers = nullptr;
        rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_NONE;

        ComPtr<ID3DBlob> sigBlob, sigErr;
        HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1_0,
            &sigBlob, &sigErr);
        if (FAILED(hr)) {
            return "DXR smoke: FAIL SerializeRootSignature hr=0x" + hrText2(hr);
        }
        hr = dev->CreateRootSignature(0, sigBlob->GetBufferPointer(),
            sigBlob->GetBufferSize(), IID_PPV_ARGS(&rootSig));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL CreateRootSignature hr=0x" + hrText2(hr);
        }
    }

    // ---- RTPSO ----
    ComPtr<ID3D12StateObject> stateObject;
    ComPtr<ID3D12StateObjectProperties> stateProps;
    {
        D3D12_DXIL_LIBRARY_DESC libDesc{};
        libDesc.DXILLibrary.pShaderBytecode = g_dxrSmokeLib;
        libDesc.DXILLibrary.BytecodeLength = sizeof(g_dxrSmokeLib);
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

        D3D12_RAYTRACING_SHADER_CONFIG shaderCfg{};
        shaderCfg.MaxPayloadSizeInBytes = sizeof(float) * 3;
        shaderCfg.MaxAttributeSizeInBytes = sizeof(float) * 2;

        D3D12_RAYTRACING_PIPELINE_CONFIG pipelineCfg{};
        pipelineCfg.MaxTraceRecursionDepth = 1;

        D3D12_GLOBAL_ROOT_SIGNATURE globalRoot{};
        globalRoot.pGlobalRootSignature = rootSig.Get();

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
        // association 必须指向 shader config 子对象本身（地址稳定）。
        assoc.pSubobjectToAssociate = &subobjects[2];

        D3D12_STATE_OBJECT_DESC soDesc{};
        soDesc.Type = D3D12_STATE_OBJECT_TYPE_RAYTRACING_PIPELINE;
        soDesc.NumSubobjects = 6;
        soDesc.pSubobjects = subobjects;

        HRESULT hr = dev5->CreateStateObject(&soDesc, IID_PPV_ARGS(&stateObject));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL CreateStateObject hr=0x" + hrText2(hr);
        }
        hr = stateObject.As(&stateProps);
        if (FAILED(hr)) {
            return "DXR smoke: FAIL QueryInterface(ID3D12StateObjectProperties) hr=0x"
                + hrText2(hr);
        }
    }

    // ---- Shader Binding Table（raygen / miss / hitgroup 各 1 条记录）----
    const UINT kRecordSize = D3D12_SHADER_IDENTIFIER_SIZE_IN_BYTES;  // 32
    ComPtr<ID3D12Resource> sbt;
    {
        const UINT64 sbtSize = (UINT64)kRecordSize * 3;
        if (!createBuffer(dev, sbtSize, D3D12_HEAP_TYPE_UPLOAD,
                D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
                &sbt, err, "sbt")) {
            return "DXR smoke: FAIL " + err;
        }
        void* mapped = nullptr;
        D3D12_RANGE range{0, (SIZE_T)sbtSize};
        if (FAILED(sbt->Map(0, &range, &mapped))) {
            return "DXR smoke: FAIL sbt Map";
        }
        struct Record {
            unsigned char id[D3D12_SHADER_IDENTIFIER_SIZE_IN_BYTES];
        };
        auto* records = static_cast<Record*>(mapped);
        const void* rayGenId = stateProps->GetShaderIdentifier(L"RayGen");
        const void* missId = stateProps->GetShaderIdentifier(L"Miss");
        const void* hitGroupId = stateProps->GetShaderIdentifier(L"HitGroup");
        if (!rayGenId || !missId || !hitGroupId) {
            sbt->Unmap(0, nullptr);
            return "DXR smoke: FAIL GetShaderIdentifier returned null";
        }
        memcpy(records[0].id, rayGenId, kRecordSize);
        memcpy(records[1].id, missId, kRecordSize);
        memcpy(records[2].id, hitGroupId, kRecordSize);
        D3D12_RANGE written{0, (SIZE_T)sbtSize};
        sbt->Unmap(0, &written);
    }

    // ---- 命令层：私有 queue/allocator/fence，跑完即销毁 ----
    ComPtr<ID3D12CommandQueue> queue;
    ComPtr<ID3D12CommandAllocator> allocator;
    ComPtr<ID3D12GraphicsCommandList4> cmd;
    ComPtr<ID3D12Fence> fence;
    HANDLE fenceEvent = nullptr;
    {
        D3D12_COMMAND_QUEUE_DESC qd{};
        qd.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
        qd.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
        HRESULT hr = dev->CreateCommandQueue(&qd, IID_PPV_ARGS(&queue));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL CreateCommandQueue hr=0x" + hrText2(hr);
        }
        hr = dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
            IID_PPV_ARGS(&allocator));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL CreateCommandAllocator hr=0x" + hrText2(hr);
        }
        hr = dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, allocator.Get(),
            nullptr, IID_PPV_ARGS(&cmd));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL CreateCommandList hr=0x" + hrText2(hr);
        }
        hr = dev->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence));
        if (FAILED(hr)) {
            return "DXR smoke: FAIL CreateFence hr=0x" + hrText2(hr);
        }
        fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
        if (!fenceEvent) {
            return "DXR smoke: FAIL CreateEvent";
        }
    }

    // ---- 记录：BLAS/TLAS 构建 + DispatchRays + readback ----
    {
        D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_DESC blasBuild{};
        blasBuild.Inputs = blasInputs;
        blasBuild.DestAccelerationStructureData = blas->GetGPUVirtualAddress();
        blasBuild.ScratchAccelerationStructureData = scratch->GetGPUVirtualAddress();
        cmd->BuildRaytracingAccelerationStructure(&blasBuild, 0, nullptr);

        // BLAS 写入后，TLAS 构建需要看到完整结果。
        D3D12_RESOURCE_BARRIER uavBarrier{};
        uavBarrier.Type = D3D12_RESOURCE_BARRIER_TYPE_UAV;
        uavBarrier.UAV.pResource = blas.Get();
        cmd->ResourceBarrier(1, &uavBarrier);

        D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_DESC tlasBuild{};
        tlasBuild.Inputs = tlasInputs;
        tlasBuild.DestAccelerationStructureData = tlas->GetGPUVirtualAddress();
        tlasBuild.ScratchAccelerationStructureData = scratch->GetGPUVirtualAddress();
        cmd->BuildRaytracingAccelerationStructure(&tlasBuild, 0, nullptr);

        D3D12_RESOURCE_BARRIER tlasBarrier{};
        tlasBarrier.Type = D3D12_RESOURCE_BARRIER_TYPE_UAV;
        tlasBarrier.UAV.pResource = tlas.Get();
        cmd->ResourceBarrier(1, &tlasBarrier);

        // 输出纹理 COMMON -> UNORDERED_ACCESS
        D3D12_RESOURCE_BARRIER toUav{};
        toUav.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
        toUav.Transition.pResource = output.Get();
        toUav.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
        toUav.Transition.StateBefore = D3D12_RESOURCE_STATE_COMMON;
        toUav.Transition.StateAfter = D3D12_RESOURCE_STATE_UNORDERED_ACCESS;
        cmd->ResourceBarrier(1, &toUav);

        ID3D12DescriptorHeap* heaps[] = { heap.Get() };
        cmd->SetDescriptorHeaps(1, heaps);
        cmd->SetComputeRootSignature(rootSig.Get());
        cmd->SetComputeRootDescriptorTable(0, gpuSrv);
        cmd->SetComputeRootDescriptorTable(1, gpuUav);
        cmd->SetPipelineState1(stateObject.Get());

        D3D12_GPU_VIRTUAL_ADDRESS sbtAddr = sbt->GetGPUVirtualAddress();
        D3D12_DISPATCH_RAYS_DESC dispatch{};
        dispatch.RayGenerationShaderRecord.StartAddress = sbtAddr;
        dispatch.RayGenerationShaderRecord.SizeInBytes = kRecordSize;
        dispatch.MissShaderTable.StartAddress = sbtAddr + kRecordSize;
        dispatch.MissShaderTable.SizeInBytes = kRecordSize;
        dispatch.MissShaderTable.StrideInBytes = kRecordSize;
        dispatch.HitGroupTable.StartAddress = sbtAddr + kRecordSize * 2;
        dispatch.HitGroupTable.SizeInBytes = kRecordSize;
        dispatch.HitGroupTable.StrideInBytes = kRecordSize;
        dispatch.Width = kDim;
        dispatch.Height = kDim;
        dispatch.Depth = 1;
        cmd->DispatchRays(&dispatch);

        // 输出纹理 UNORDERED_ACCESS -> COPY_SOURCE
        D3D12_RESOURCE_BARRIER toCopy{};
        toCopy.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
        toCopy.Transition.pResource = output.Get();
        toCopy.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
        toCopy.Transition.StateBefore = D3D12_RESOURCE_STATE_UNORDERED_ACCESS;
        toCopy.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_SOURCE;
        cmd->ResourceBarrier(1, &toCopy);

        D3D12_TEXTURE_COPY_LOCATION dst{};
        dst.pResource = readback.Get();
        dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
        dst.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        dst.PlacedFootprint.Footprint.Width = kDim;
        dst.PlacedFootprint.Footprint.Height = kDim;
        dst.PlacedFootprint.Footprint.Depth = 1;
        dst.PlacedFootprint.Footprint.RowPitch = kRowPitch;

        D3D12_TEXTURE_COPY_LOCATION src{};
        src.pResource = output.Get();
        src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
        src.SubresourceIndex = 0;
        cmd->CopyTextureRegion(&dst, 0, 0, 0, &src, nullptr);

        HRESULT hr = cmd->Close();
        if (FAILED(hr)) {
            CloseHandle(fenceEvent);
            return "DXR smoke: FAIL command list Close hr=0x" + hrText2(hr);
        }
    }

    {
        ID3D12CommandList* lists[] = { cmd.Get() };
        queue->ExecuteCommandLists(1, lists);
        HRESULT hr = queue->Signal(fence.Get(), 1);
        if (FAILED(hr)) {
            CloseHandle(fenceEvent);
            return "DXR smoke: FAIL queue Signal hr=0x" + hrText2(hr);
        }
        if (fence->GetCompletedValue() < 1) {
            hr = fence->SetEventOnCompletion(1, fenceEvent);
            if (FAILED(hr)) {
                CloseHandle(fenceEvent);
                return "DXR smoke: FAIL SetEventOnCompletion hr=0x" + hrText2(hr);
            }
            if (WaitForSingleObject(fenceEvent, 5000) != WAIT_OBJECT_0) {
                CloseHandle(fenceEvent);
                return "DXR smoke: FAIL GPU wait timeout (5s)";
            }
        }
    }
    CloseHandle(fenceEvent);

    // ---- 读回像素：中心 = 命中（暖色），左上角 = 未命中（蓝色）----
    unsigned char* pixels = nullptr;
    D3D12_RANGE readRange{0, (SIZE_T)kReadbackSize};
    if (FAILED(readback->Map(0, &readRange, reinterpret_cast<void**>(&pixels)))) {
        return "DXR smoke: FAIL readback Map";
    }
    const size_t centerIdx = ((size_t)(kDim / 2) * kRowPitch) + (kDim / 2) * 4;
    const int cr = pixels[centerIdx + 0];
    const int cg = pixels[centerIdx + 1];
    const int cb = pixels[centerIdx + 2];
    const int mr = pixels[0];
    const int mg = pixels[1];
    const int mb = pixels[2];
    D3D12_RANGE noWrite{0, 0};
    readback->Unmap(0, &noWrite);

    const bool hitOk = cr > 200 && cb < 100;
    const bool missOk = mb > 90 && mr < 60;
    const double elapsed = nowMs() - t0;

    char buf[320];
    snprintf(buf, sizeof(buf),
        "DXR smoke: %s (BLAS+TLAS+RTPSO+DispatchRays 64x64, center=[%d,%d,%d] "
        "corner=[%d,%d,%d], %.1f ms)",
        (hitOk && missOk) ? "PASS" : "MISMATCH",
        cr, cg, cb, mr, mg, mb, elapsed);
    dbgLog("%s", buf);
    return buf;
}

// ===========================================================================
// P67 可见光追演示：场景几何 -> BLAS/TLAS -> DispatchRays -> UAV 纹理，
// 供光栅管线作为小窗叠加到 backbuffer（RT -> 纹理 -> 屏幕）。
// ===========================================================================
namespace {

constexpr UINT kDemoWidth = 512;
constexpr UINT kDemoHeight = 288;
constexpr UINT kDemoMaxTris = 48;

// 与 dxr_demo.hlsl 的 cbuffer Scene 布局严格一致。
struct DemoCB {
    float params[4];                    // x=秒, y=aspect
    float triColor[kDemoMaxTris][4];    // rgb + w>0.5 => 棋盘地面
    float triNormal[kDemoMaxTris][4];   // xyz 法线
};

struct DemoScene {
    std::vector<float> verts;      // 3 floats/vertex
    std::vector<float> triColor;   // 4 floats/tri
    std::vector<float> triNormal;  // 4 floats/tri
    UINT triCount = 0;

    void tri(const float (&a)[3], const float (&b)[3], const float (&c)[3],
             const float (&n)[3], const float (&col)[3], float checker) {
        if (triCount >= kDemoMaxTris) return;
        verts.insert(verts.end(), { a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2] });
        triColor.insert(triColor.end(), { col[0], col[1], col[2], checker });
        triNormal.insert(triNormal.end(), { n[0], n[1], n[2], 0.0f });
        ++triCount;
    }

    void quad(const float (&a)[3], const float (&b)[3], const float (&c)[3],
              const float (&d)[3], const float (&n)[3], const float (&col)[3],
              float checker) {
        tri(a, b, c, n, col, checker);
        tri(a, c, d, n, col, checker);
    }

    void box(float cx, float cy, float cz, float h, const float (&col)[3]) {
        const float x0 = cx - h, x1 = cx + h;
        const float y0 = cy - h, y1 = cy + h;
        const float z0 = cz - h, z1 = cz + h;
        const float up[3] = { 0.0f, 1.0f, 0.0f };
        const float fz[3] = { 0.0f, 0.0f, 1.0f };
        const float bz[3] = { 0.0f, 0.0f, -1.0f };
        const float px[3] = { 1.0f, 0.0f, 0.0f };
        const float nx[3] = { -1.0f, 0.0f, 0.0f };
        quad({x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}, up, col, 0.0f);
        quad({x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}, fz, col, 0.0f);
        quad({x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, bz, col, 0.0f);
        quad({x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, px, col, 0.0f);
        quad({x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}, nx, col, 0.0f);
    }
};

void buildDemoScene(DemoScene& s) {
    const float up[3] = { 0.0f, 1.0f, 0.0f };
    const float floorCol[3] = { 0.8f, 0.8f, 0.8f };
    // 棋盘地面
    s.quad({-8.0f, 0.0f, -8.0f}, {8.0f, 0.0f, -8.0f},
           {8.0f, 0.0f, 8.0f}, {-8.0f, 0.0f, 8.0f}, up, floorCol, 1.0f);

    const float backCol[3] = { 0.62f, 0.66f, 0.74f };
    const float backN[3] = { 0.0f, 0.0f, -1.0f };
    s.quad({-8.0f, 0.0f, 7.0f}, {8.0f, 0.0f, 7.0f},
           {8.0f, 6.0f, 7.0f}, {-8.0f, 6.0f, 7.0f}, backN, backCol, 0.0f);

    const float leftCol[3] = { 0.70f, 0.55f, 0.48f };
    const float leftN[3] = { 1.0f, 0.0f, 0.0f };
    s.quad({-8.0f, 0.0f, -8.0f}, {-8.0f, 0.0f, 7.0f},
           {-8.0f, 6.0f, 7.0f}, {-8.0f, 6.0f, -8.0f}, leftN, leftCol, 0.0f);

    const float red[3] = { 0.86f, 0.24f, 0.20f };
    const float green[3] = { 0.22f, 0.74f, 0.34f };
    const float blue[3] = { 0.22f, 0.44f, 0.90f };
    s.box(-2.2f, 0.6f, 0.0f, 0.6f, red);
    s.box(0.6f, 0.9f, 1.2f, 0.9f, green);
    s.box(2.8f, 0.45f, -1.5f, 0.45f, blue);
}

struct DxrDemoState {
    bool initialized = false;
    bool failed = false;
    bool asBuilt = false;   // BLAS/TLAS 是否已在 GPU 上构建
    std::string failReason;
    UINT triCount = 0;

    ComPtr<ID3D12RootSignature> rootSig;
    ComPtr<ID3D12StateObject> stateObject;
    ComPtr<ID3D12StateObjectProperties> props;
    ComPtr<ID3D12DescriptorHeap> heap;
    ComPtr<ID3D12Resource> output;
    ComPtr<ID3D12Resource> blas;
    ComPtr<ID3D12Resource> tlas;
    ComPtr<ID3D12Resource> scratch;
    ComPtr<ID3D12Resource> vertexBuffer;
    ComPtr<ID3D12Resource> instanceBuffer;
    ComPtr<ID3D12Resource> sbt;
    ComPtr<ID3D12Resource> cb;
    D3D12_GPU_DESCRIPTOR_HANDLE gpuSrv{};
    D3D12_GPU_DESCRIPTOR_HANDLE gpuUav{};
    D3D12_GPU_VIRTUAL_ADDRESS cbAddr = 0;
    D3D12_GPU_VIRTUAL_ADDRESS sbtAddr = 0;
    unsigned char* cbMapped = nullptr;
    D3D12_RESOURCE_STATES outputState = D3D12_RESOURCE_STATE_COMMON;
};

DxrDemoState gDemo;

bool initDxrDemo(std::string& err) {
    DeviceContext& ctx = deviceContextForJni();
    if (!ctx.device || !ctx.device5) {
        err = "DXR not available (no ID3D12Device5)";
        return false;
    }
    ID3D12Device* dev = ctx.device.Get();
    ID3D12Device5* dev5 = ctx.device5.Get();

    DemoScene scene;
    buildDemoScene(scene);
    if (scene.triCount == 0) {
        err = "empty demo scene";
        return false;
    }
    gDemo.triCount = scene.triCount;

    // ---- 输出纹理（UAV，供 DispatchRays 写入）----
    {
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_DEFAULT;
        hp.CreationNodeMask = 1;
        hp.VisibleNodeMask = 1;

        D3D12_RESOURCE_DESC rd{};
        rd.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        rd.Width = kDemoWidth;
        rd.Height = kDemoHeight;
        rd.DepthOrArraySize = 1;
        rd.MipLevels = 1;
        rd.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        rd.SampleDesc.Count = 1;
        rd.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
        rd.Flags = D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;

        HRESULT hr = dev->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE, &rd,
            D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(&gDemo.output));
        if (FAILED(hr)) {
            err = "demo output texture hr=0x" + hrText2(hr);
            return false;
        }
        gDemo.outputState = D3D12_RESOURCE_STATE_COMMON;
    }

    // ---- 顶点缓冲 ----
    if (!uploadBuffer(dev, scene.verts.data(), scene.verts.size() * sizeof(float),
            &gDemo.vertexBuffer, err, "demo vb")) {
        return false;
    }

    // ---- BLAS ----
    D3D12_RAYTRACING_GEOMETRY_DESC geom{};
    geom.Type = D3D12_RAYTRACING_GEOMETRY_TYPE_TRIANGLES;
    // 不能用 OPAQUE：否则 any-hit 不会被调用，阴影射线无法判定命中
    //（SKIP_CLOSEST_HIT_SHADER 下 payload 永远不会被置位）。
    geom.Flags = D3D12_RAYTRACING_GEOMETRY_FLAG_NONE;
    geom.Triangles.VertexBuffer.StartAddress = gDemo.vertexBuffer->GetGPUVirtualAddress();
    geom.Triangles.VertexBuffer.StrideInBytes = sizeof(float) * 3;
    geom.Triangles.VertexCount = scene.triCount * 3;
    geom.Triangles.VertexFormat = DXGI_FORMAT_R32G32B32_FLOAT;
    geom.Triangles.IndexBuffer = 0;
    geom.Triangles.IndexCount = 0;
    geom.Triangles.Transform3x4 = 0;

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS blasInputs{};
    blasInputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL;
    blasInputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
    blasInputs.NumDescs = 1;
    blasInputs.pGeometryDescs = &geom;
    blasInputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

    D3D12_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO blasInfo{};
    dev5->GetRaytracingAccelerationStructurePrebuildInfo(&blasInputs, &blasInfo);
    if (blasInfo.ResultDataMaxSizeInBytes == 0) {
        err = "demo BLAS prebuild info is zero";
        return false;
    }
    if (!createBuffer(dev, blasInfo.ResultDataMaxSizeInBytes, D3D12_HEAP_TYPE_DEFAULT,
            D3D12_RESOURCE_STATE_RAYTRACING_ACCELERATION_STRUCTURE,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &gDemo.blas, err, "demo blas")) {
        return false;
    }

    // ---- TLAS（单实例，单位变换）----
    D3D12_RAYTRACING_INSTANCE_DESC inst{};
    inst.Transform[0][0] = 1.0f;
    inst.Transform[1][1] = 1.0f;
    inst.Transform[2][2] = 1.0f;
    inst.InstanceID = 0;
    inst.InstanceMask = 0xFF;
    inst.InstanceContributionToHitGroupIndex = 0;
    inst.Flags = D3D12_RAYTRACING_INSTANCE_FLAG_NONE;
    inst.AccelerationStructure = gDemo.blas->GetGPUVirtualAddress();
    if (!uploadBuffer(dev, &inst, sizeof(inst), &gDemo.instanceBuffer, err, "demo instance")) {
        return false;
    }

    D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS tlasInputs{};
    tlasInputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL;
    tlasInputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
    tlasInputs.NumDescs = 1;
    tlasInputs.InstanceDescs = gDemo.instanceBuffer->GetGPUVirtualAddress();
    tlasInputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

    D3D12_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO tlasInfo{};
    dev5->GetRaytracingAccelerationStructurePrebuildInfo(&tlasInputs, &tlasInfo);
    if (tlasInfo.ResultDataMaxSizeInBytes == 0) {
        err = "demo TLAS prebuild info is zero";
        return false;
    }
    if (!createBuffer(dev, tlasInfo.ResultDataMaxSizeInBytes, D3D12_HEAP_TYPE_DEFAULT,
            D3D12_RESOURCE_STATE_RAYTRACING_ACCELERATION_STRUCTURE,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &gDemo.tlas, err, "demo tlas")) {
        return false;
    }

    const UINT64 scratchSize = (blasInfo.ScratchDataSizeInBytes > tlasInfo.ScratchDataSizeInBytes
        ? blasInfo.ScratchDataSizeInBytes : tlasInfo.ScratchDataSizeInBytes);
    if (!createBuffer(dev, scratchSize, D3D12_HEAP_TYPE_DEFAULT,
            D3D12_RESOURCE_STATE_UNORDERED_ACCESS,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, &gDemo.scratch, err, "demo scratch")) {
        return false;
    }

    // ---- 描述符堆：SRV(t0)=TLAS, UAV(u0)=输出纹理 ----
    {
        D3D12_DESCRIPTOR_HEAP_DESC hd{};
        hd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
        hd.NumDescriptors = 2;
        hd.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
        HRESULT hr = dev->CreateDescriptorHeap(&hd, IID_PPV_ARGS(&gDemo.heap));
        if (FAILED(hr)) {
            err = "demo descriptor heap hr=0x" + hrText2(hr);
            return false;
        }
    }
    {
        const UINT inc = dev->GetDescriptorHandleIncrementSize(
            D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
        D3D12_CPU_DESCRIPTOR_HANDLE cpuSrv = gDemo.heap->GetCPUDescriptorHandleForHeapStart();
        D3D12_CPU_DESCRIPTOR_HANDLE cpuUav = cpuSrv;
        cpuUav.ptr += inc;
        gDemo.gpuSrv = gDemo.heap->GetGPUDescriptorHandleForHeapStart();
        gDemo.gpuUav = gDemo.gpuSrv;
        gDemo.gpuUav.ptr += inc;

        D3D12_SHADER_RESOURCE_VIEW_DESC srv{};
        srv.Format = DXGI_FORMAT_UNKNOWN;
        srv.ViewDimension = D3D12_SRV_DIMENSION_RAYTRACING_ACCELERATION_STRUCTURE;
        srv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
        srv.RaytracingAccelerationStructure.Location = gDemo.tlas->GetGPUVirtualAddress();
        dev->CreateShaderResourceView(nullptr, &srv, cpuSrv);

        D3D12_UNORDERED_ACCESS_VIEW_DESC uav{};
        uav.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        uav.ViewDimension = D3D12_UAV_DIMENSION_TEXTURE2D;
        uav.Texture2D.MipSlice = 0;
        dev->CreateUnorderedAccessView(gDemo.output.Get(), nullptr, &uav, cpuUav);
    }

    // ---- 全局根签名：CBV(b0) + SRV table(t0) + UAV table(u0) ----
    {
        D3D12_DESCRIPTOR_RANGE ranges[2]{};
        ranges[0].RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
        ranges[0].NumDescriptors = 1;
        ranges[0].BaseShaderRegister = 0;
        ranges[0].RegisterSpace = 0;
        ranges[0].OffsetInDescriptorsFromTableStart = 0;
        ranges[1].RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_UAV;
        ranges[1].NumDescriptors = 1;
        ranges[1].BaseShaderRegister = 0;
        ranges[1].RegisterSpace = 0;
        ranges[1].OffsetInDescriptorsFromTableStart = 0;

        D3D12_ROOT_PARAMETER params[3]{};
        params[0].ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
        params[0].Descriptor.ShaderRegister = 0;
        params[0].Descriptor.RegisterSpace = 0;
        params[0].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
        params[1].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
        params[1].DescriptorTable.NumDescriptorRanges = 1;
        params[1].DescriptorTable.pDescriptorRanges = &ranges[0];
        params[1].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
        params[2].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
        params[2].DescriptorTable.NumDescriptorRanges = 1;
        params[2].DescriptorTable.pDescriptorRanges = &ranges[1];
        params[2].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

        D3D12_ROOT_SIGNATURE_DESC rsDesc{};
        rsDesc.NumParameters = 3;
        rsDesc.pParameters = params;
        rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_NONE;

        ComPtr<ID3DBlob> sigBlob, sigErr;
        HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1_0,
            &sigBlob, &sigErr);
        if (FAILED(hr)) {
            err = "demo SerializeRootSignature hr=0x" + hrText2(hr);
            return false;
        }
        hr = dev->CreateRootSignature(0, sigBlob->GetBufferPointer(),
            sigBlob->GetBufferSize(), IID_PPV_ARGS(&gDemo.rootSig));
        if (FAILED(hr)) {
            err = "demo CreateRootSignature hr=0x" + hrText2(hr);
            return false;
        }
    }

    // ---- RTPSO ----
    {
        D3D12_DXIL_LIBRARY_DESC libDesc{};
        libDesc.DXILLibrary.pShaderBytecode = g_dxrDemoLib;
        libDesc.DXILLibrary.BytecodeLength = sizeof(g_dxrDemoLib);
        D3D12_EXPORT_DESC exports[4]{};
        exports[0].Name = L"RayGen";
        exports[1].Name = L"ClosestHit";
        exports[2].Name = L"AnyHit";
        exports[3].Name = L"Miss";
        libDesc.NumExports = 4;
        libDesc.pExports = exports;

        D3D12_HIT_GROUP_DESC hitGroup{};
        hitGroup.HitGroupExport = L"HitGroup";
        hitGroup.Type = D3D12_HIT_GROUP_TYPE_TRIANGLES;
        hitGroup.ClosestHitShaderImport = L"ClosestHit";
        hitGroup.AnyHitShaderImport = L"AnyHit";

        D3D12_RAYTRACING_SHADER_CONFIG shaderCfg{};
        shaderCfg.MaxPayloadSizeInBytes = 32;   // Payload 实际 20 字节
        shaderCfg.MaxAttributeSizeInBytes = sizeof(float) * 2;

        D3D12_RAYTRACING_PIPELINE_CONFIG pipelineCfg{};
        pipelineCfg.MaxTraceRecursionDepth = 2;  // 主射线 + 阴影射线

        D3D12_GLOBAL_ROOT_SIGNATURE globalRoot{};
        globalRoot.pGlobalRootSignature = gDemo.rootSig.Get();

        LPCWSTR assocExports[] = { L"RayGen", L"ClosestHit", L"AnyHit", L"Miss", L"HitGroup" };
        D3D12_SUBOBJECT_TO_EXPORTS_ASSOCIATION assoc{};
        assoc.NumExports = 5;
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

        HRESULT hr = dev5->CreateStateObject(&soDesc, IID_PPV_ARGS(&gDemo.stateObject));
        if (FAILED(hr)) {
            err = "demo CreateStateObject hr=0x" + hrText2(hr);
            return false;
        }
        hr = gDemo.stateObject.As(&gDemo.props);
        if (FAILED(hr)) {
            err = "demo QueryInterface(ID3D12StateObjectProperties) hr=0x" + hrText2(hr);
            return false;
        }
    }

    // ---- SBT：raygen / miss / hitgroup 各 1 条 ----
    {
        const UINT record = D3D12_SHADER_IDENTIFIER_SIZE_IN_BYTES;
        const UINT64 sbtSize = (UINT64)record * 3;
        if (!createBuffer(dev, sbtSize, D3D12_HEAP_TYPE_UPLOAD,
                D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
                &gDemo.sbt, err, "demo sbt")) {
            return false;
        }
        void* mapped = nullptr;
        if (FAILED(gDemo.sbt->Map(0, nullptr, &mapped))) {
            err = "demo sbt Map failed";
            return false;
        }
        const void* rg = gDemo.props->GetShaderIdentifier(L"RayGen");
        const void* ms = gDemo.props->GetShaderIdentifier(L"Miss");
        const void* hg = gDemo.props->GetShaderIdentifier(L"HitGroup");
        if (!rg || !ms || !hg) {
            gDemo.sbt->Unmap(0, nullptr);
            err = "demo GetShaderIdentifier returned null";
            return false;
        }
        unsigned char* p = static_cast<unsigned char*>(mapped);
        memcpy(p, rg, record);
        memcpy(p + record, ms, record);
        memcpy(p + record * 2, hg, record);
        gDemo.sbt->Unmap(0, nullptr);
        gDemo.sbtAddr = gDemo.sbt->GetGPUVirtualAddress();
    }

    // ---- 场景常量缓冲（持久映射，每帧只更新 params）----
    {
        const UINT64 cbSize = 1792;  // sizeof(DemoCB)=1552，向上 256 对齐
        if (!createBuffer(dev, cbSize, D3D12_HEAP_TYPE_UPLOAD,
                D3D12_RESOURCE_STATE_GENERIC_READ, D3D12_RESOURCE_FLAG_NONE,
                &gDemo.cb, err, "demo cb")) {
            return false;
        }
        void* mapped = nullptr;
        if (FAILED(gDemo.cb->Map(0, nullptr, &mapped))) {
            err = "demo cb Map failed";
            return false;
        }
        gDemo.cbMapped = static_cast<unsigned char*>(mapped);
        DemoCB* cb = reinterpret_cast<DemoCB*>(gDemo.cbMapped);
        memset(cb, 0, sizeof(DemoCB));
        for (UINT i = 0; i < scene.triCount; ++i) {
            memcpy(cb->triColor[i], &scene.triColor[i * 4], sizeof(float) * 4);
            memcpy(cb->triNormal[i], &scene.triNormal[i * 4], sizeof(float) * 4);
        }
        gDemo.cbAddr = gDemo.cb->GetGPUVirtualAddress();
    }

    gDemo.initialized = true;
    return true;
}

}  // namespace

bool ensureDxrDemo(std::string& err) {
    if (gDemo.initialized) return true;
    if (gDemo.failed) {
        err = gDemo.failReason;
        return false;
    }
    std::string e;
    if (!initDxrDemo(e)) {
        gDemo.failed = true;
        gDemo.failReason = e;
        dbgLog("DXR demo: disabled (%s)", e.c_str());
        err = e;
        return false;
    }
    dbgLog("DXR demo: ready (%ux%u, %u tris, RTPSO + SBT built)",
        kDemoWidth, kDemoHeight, gDemo.triCount);
    return true;
}

bool dxrDemoDispatch(ID3D12GraphicsCommandList* list, double timeSec, std::string& err) {
    if (!gDemo.initialized) {
        err = "DXR demo not initialized";
        return false;
    }
    ComPtr<ID3D12GraphicsCommandList4> list4;
    if (FAILED(list->QueryInterface(IID_PPV_ARGS(&list4)))) {
        err = "command list has no ID3D12GraphicsCommandList4";
        return false;
    }

    // 首次 dispatch 时构建 BLAS/TLAS（init 阶段只创建资源，不能录命令）。
    // 顶点缓冲在 UPLOAD 堆，数据已就绪。
    if (!gDemo.asBuilt) {
        D3D12_RAYTRACING_GEOMETRY_DESC geom{};
        geom.Type = D3D12_RAYTRACING_GEOMETRY_TYPE_TRIANGLES;
        geom.Flags = D3D12_RAYTRACING_GEOMETRY_FLAG_NONE;
        geom.Triangles.VertexBuffer.StartAddress = gDemo.vertexBuffer->GetGPUVirtualAddress();
        geom.Triangles.VertexBuffer.StrideInBytes = sizeof(float) * 3;
        geom.Triangles.VertexCount = gDemo.triCount * 3;
        geom.Triangles.VertexFormat = DXGI_FORMAT_R32G32B32_FLOAT;

        D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS blasInputs{};
        blasInputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL;
        blasInputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
        blasInputs.NumDescs = 1;
        blasInputs.pGeometryDescs = &geom;
        blasInputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

        D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_DESC blasBuild{};
        blasBuild.Inputs = blasInputs;
        blasBuild.DestAccelerationStructureData = gDemo.blas->GetGPUVirtualAddress();
        blasBuild.ScratchAccelerationStructureData = gDemo.scratch->GetGPUVirtualAddress();
        list4->BuildRaytracingAccelerationStructure(&blasBuild, 0, nullptr);

        D3D12_RESOURCE_BARRIER uav{};
        uav.Type = D3D12_RESOURCE_BARRIER_TYPE_UAV;
        uav.UAV.pResource = gDemo.blas.Get();
        list->ResourceBarrier(1, &uav);

        D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_INPUTS tlasInputs{};
        tlasInputs.Type = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL;
        tlasInputs.DescsLayout = D3D12_ELEMENTS_LAYOUT_ARRAY;
        tlasInputs.NumDescs = 1;
        tlasInputs.InstanceDescs = gDemo.instanceBuffer->GetGPUVirtualAddress();
        tlasInputs.Flags = D3D12_RAYTRACING_ACCELERATION_STRUCTURE_BUILD_FLAG_PREFER_FAST_TRACE;

        D3D12_BUILD_RAYTRACING_ACCELERATION_STRUCTURE_DESC tlasBuild{};
        tlasBuild.Inputs = tlasInputs;
        tlasBuild.DestAccelerationStructureData = gDemo.tlas->GetGPUVirtualAddress();
        tlasBuild.ScratchAccelerationStructureData = gDemo.scratch->GetGPUVirtualAddress();
        list4->BuildRaytracingAccelerationStructure(&tlasBuild, 0, nullptr);

        uav.UAV.pResource = gDemo.tlas.Get();
        list->ResourceBarrier(1, &uav);
        gDemo.asBuilt = true;
    }

    // 更新时间（相机公转）。
    DemoCB* cb = reinterpret_cast<DemoCB*>(gDemo.cbMapped);
    cb->params[0] = (float)timeSec;
    cb->params[1] = (float)kDemoWidth / (float)kDemoHeight;
    cb->params[2] = 0.0f;
    cb->params[3] = 0.0f;

    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = gDemo.output.Get();
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = gDemo.outputState;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_UNORDERED_ACCESS;
    list->ResourceBarrier(1, &b);
    gDemo.outputState = D3D12_RESOURCE_STATE_UNORDERED_ACCESS;

    ID3D12DescriptorHeap* heaps[] = { gDemo.heap.Get() };
    list->SetDescriptorHeaps(1, heaps);
    list->SetComputeRootSignature(gDemo.rootSig.Get());
    list->SetComputeRootConstantBufferView(0, gDemo.cbAddr);
    list->SetComputeRootDescriptorTable(1, gDemo.gpuSrv);
    list->SetComputeRootDescriptorTable(2, gDemo.gpuUav);
    list4->SetPipelineState1(gDemo.stateObject.Get());

    const UINT record = D3D12_SHADER_IDENTIFIER_SIZE_IN_BYTES;
    D3D12_DISPATCH_RAYS_DESC desc{};
    desc.RayGenerationShaderRecord.StartAddress = gDemo.sbtAddr;
    desc.RayGenerationShaderRecord.SizeInBytes = record;
    desc.MissShaderTable.StartAddress = gDemo.sbtAddr + record;
    desc.MissShaderTable.SizeInBytes = record;
    desc.MissShaderTable.StrideInBytes = record;
    desc.HitGroupTable.StartAddress = gDemo.sbtAddr + record * 2;
    desc.HitGroupTable.SizeInBytes = record;
    desc.HitGroupTable.StrideInBytes = record;
    desc.Width = kDemoWidth;
    desc.Height = kDemoHeight;
    desc.Depth = 1;
    list4->DispatchRays(&desc);

    b.Transition.StateBefore = D3D12_RESOURCE_STATE_UNORDERED_ACCESS;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;
    list->ResourceBarrier(1, &b);
    gDemo.outputState = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;
    return true;
}

ID3D12Resource* dxrDemoOutputTexture() {
    return gDemo.initialized ? gDemo.output.Get() : nullptr;
}

void resetDxrDemo() {
    if (gDemo.cb && gDemo.cbMapped) {
        gDemo.cb->Unmap(0, nullptr);
    }
    gDemo = DxrDemoState{};
}

// 独立验证：跑一次 demo dispatch 并把 512x288 结果读回，报告若干采样点。
// 仅供 dllexport 诊断入口使用，不参与游戏渲染。
std::string runDxrDemoTest() {
    std::string err;
    DeviceContext& ctx = deviceContextForJni();
    if (!ctx.device || !ctx.device5) {
        return "DXR demo: skipped (no ID3D12Device5)";
    }
    const double t0 = nowMs();
    if (!ensureDxrDemo(err)) {
        return "DXR demo: FAIL " + err;
    }

    ID3D12Device* dev = ctx.device.Get();
    ComPtr<ID3D12CommandAllocator> allocator;
    ComPtr<ID3D12GraphicsCommandList> list;
    ComPtr<ID3D12CommandQueue> queue;
    ComPtr<ID3D12Fence> fence;
    HANDLE ev = nullptr;

    HRESULT hr = dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
        IID_PPV_ARGS(&allocator));
    if (FAILED(hr)) return "DXR demo: FAIL CreateCommandAllocator 0x" + hrText2(hr);
    hr = dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, allocator.Get(),
        nullptr, IID_PPV_ARGS(&list));
    if (FAILED(hr)) return "DXR demo: FAIL CreateCommandList 0x" + hrText2(hr);
    D3D12_COMMAND_QUEUE_DESC qd{};
    qd.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    hr = dev->CreateCommandQueue(&qd, IID_PPV_ARGS(&queue));
    if (FAILED(hr)) return "DXR demo: FAIL CreateCommandQueue 0x" + hrText2(hr);
    hr = dev->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence));
    if (FAILED(hr)) return "DXR demo: FAIL CreateFence 0x" + hrText2(hr);
    ev = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    if (!ev) return "DXR demo: FAIL CreateEvent";

    // DispatchRays（t=1.0s，固定相机角度便于比对）。
    if (!dxrDemoDispatch(list.Get(), 1.0, err)) {
        CloseHandle(ev);
        return "DXR demo: FAIL dispatch " + err;
    }

    // 读回缓冲。
    const UINT rowPitch = (kDemoWidth * 4 + 255) & ~255u;
    ComPtr<ID3D12Resource> readback;
    if (!createBuffer(dev, (UINT64)rowPitch * kDemoHeight, D3D12_HEAP_TYPE_READBACK,
            D3D12_RESOURCE_STATE_COPY_DEST, D3D12_RESOURCE_FLAG_NONE,
            &readback, err, "demo readback")) {
        CloseHandle(ev);
        return "DXR demo: FAIL " + err;
    }

    // 输出纹理 PIXEL_SHADER_RESOURCE -> COPY_SOURCE。
    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = gDemo.output.Get();
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = gDemo.outputState;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_SOURCE;
    list->ResourceBarrier(1, &b);
    gDemo.outputState = D3D12_RESOURCE_STATE_COPY_SOURCE;

    D3D12_TEXTURE_COPY_LOCATION dst{};
    dst.pResource = readback.Get();
    dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    dst.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    dst.PlacedFootprint.Footprint.Width = kDemoWidth;
    dst.PlacedFootprint.Footprint.Height = kDemoHeight;
    dst.PlacedFootprint.Footprint.Depth = 1;
    dst.PlacedFootprint.Footprint.RowPitch = rowPitch;

    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = gDemo.output.Get();
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = 0;
    list->CopyTextureRegion(&dst, 0, 0, 0, &src, nullptr);

    // 复位为 PIXEL_SHADER_RESOURCE，保持与游戏路径一致。
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_SOURCE;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;
    list->ResourceBarrier(1, &b);
    gDemo.outputState = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;

    hr = list->Close();
    if (FAILED(hr)) {
        CloseHandle(ev);
        return "DXR demo: FAIL list Close 0x" + hrText2(hr);
    }
    ID3D12CommandList* lists[] = { list.Get() };
    queue->ExecuteCommandLists(1, lists);
    hr = queue->Signal(fence.Get(), 1);
    if (FAILED(hr)) {
        CloseHandle(ev);
        return "DXR demo: FAIL queue Signal 0x" + hrText2(hr);
    }
    if (fence->GetCompletedValue() < 1) {
        fence->SetEventOnCompletion(1, ev);
        if (WaitForSingleObject(ev, 10000) != WAIT_OBJECT_0) {
            CloseHandle(ev);
            return "DXR demo: FAIL GPU wait timeout (10s)";
        }
    }
    CloseHandle(ev);
    hr = dev->GetDeviceRemovedReason();
    if (FAILED(hr)) {
        return "DXR demo: FAIL device removed 0x" + hrText2(hr);
    }

    unsigned char* pixels = nullptr;
    D3D12_RANGE readRange{0, (SIZE_T)rowPitch * kDemoHeight};
    hr = readback->Map(0, &readRange, reinterpret_cast<void**>(&pixels));
    if (FAILED(hr)) return "DXR demo: FAIL readback Map 0x" + hrText2(hr);

    auto sample = [&](UINT x, UINT y, int out[3]) {
        const size_t idx = (size_t)y * rowPitch + (size_t)x * 4;
        out[0] = pixels[idx + 0];
        out[1] = pixels[idx + 1];
        out[2] = pixels[idx + 2];
    };
    int center[3], sky[3], floorPx[3];
    sample(kDemoWidth / 2, kDemoHeight / 2, center);
    sample(kDemoWidth / 8, kDemoHeight / 8, sky);
    sample(kDemoWidth / 2, (UINT)(kDemoHeight * 0.85f), floorPx);
    D3D12_RANGE noWrite{0, 0};
    readback->Unmap(0, &noWrite);

    const bool notBlack = (center[0] + center[1] + center[2]) > 24;
    const double elapsed = nowMs() - t0;
    char buf[400];
    snprintf(buf, sizeof(buf),
        "DXR demo: %s (512x288, center=[%d,%d,%d] sky=[%d,%d,%d] "
        "floor=[%d,%d,%d], %.1f ms)",
        notBlack ? "PASS" : "BLACK",
        center[0], center[1], center[2], sky[0], sky[1], sky[2],
        floorPx[0], floorPx[1], floorPx[2], elapsed);
    dbgLog("%s", buf);
    return buf;
}

}  // namespace dx12mc

// 诊断入口：供独立测试程序（无 JVM）加载本 DLL 后直接验证 DXR 链路。
extern "C" __declspec(dllexport) int dxrSmokeTestStandalone() {
    std::string err;
    if (!dx12mc::ensureDevice(err)) {
        std::fprintf(stderr, "[dxr-standalone] ensureDevice failed: %s\n", err.c_str());
        return 1;
    }
    std::string result = dx12mc::runDxrSmokeTest();
    std::fprintf(stderr, "[dxr-standalone] %s\n", result.c_str());
    return result.find("PASS") != std::string::npos ? 0 : 2;
}

// 诊断入口：可见 RT 演示（512x288 场景）单次执行 + 读回验证。
extern "C" __declspec(dllexport) int dxrDemoStandalone() {
    std::string err;
    if (!dx12mc::ensureDevice(err)) {
        std::fprintf(stderr, "[dxr-demo-standalone] ensureDevice failed: %s\n", err.c_str());
        return 1;
    }
    std::string result = dx12mc::runDxrDemoTest();
    std::fprintf(stderr, "[dxr-demo-standalone] %s\n", result.c_str());
    return result.find("PASS") != std::string::npos ? 0 : 2;
}
