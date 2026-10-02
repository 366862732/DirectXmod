#include "dx12_async_bundle.h"

#include "dx12_device.h"  // deviceContextForJni（读取现有 cmdSigIndexed / cmdSigNonIndexed）

namespace dx12mc {

D3D12_PRIMITIVE_TOPOLOGY bundlePrimitiveTopology(int ordinal) {
    switch (ordinal) {
        case 0: case 1: return D3D_PRIMITIVE_TOPOLOGY_LINELIST;   // LINES / DEBUG_LINES
        case 2: return D3D_PRIMITIVE_TOPOLOGY_LINESTRIP;          // DEBUG_LINE_STRIP
        case 3: return D3D_PRIMITIVE_TOPOLOGY_POINTLIST;          // POINTS
        case 5: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP;      // TRIANGLE_STRIP
        case 6: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLEFAN;        // TRIANGLE_FAN
        case 7: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;       // QUADS（不支持，回退）
        default: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;      // TRIANGLES 等
    }
}

bool BundleRecorder::init(ID3D12Device* device, ID3D12DescriptorHeap* heap,
    UINT workerId) {
    if (!device) return false;

    m_device = device;
    m_workerId = workerId;
    m_heap = heap;  // 非拥有；bundle 内 SetGraphicsRootDescriptorTable 前须自设该堆

    for (UINT i = 0; i < kBundleSlotCount; ++i) {
        HRESULT hr = device->CreateCommandAllocator(
            D3D12_COMMAND_LIST_TYPE_BUNDLE, IID_PPV_ARGS(&m_allocators[i]));
        if (FAILED(hr)) return false;

        hr = device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_BUNDLE,
            m_allocators[i].Get(), nullptr, IID_PPV_ARGS(&m_lists[i]));
        if (FAILED(hr)) return false;

        // 创建即为 open，先 Close 到初始状态，之后 begin() 再 Reset。
        m_lists[i]->Close();
    }
    m_recording = false;
    for (UINT i = 0; i < kBundleFrameSlots; ++i) {
        m_frameOfSlot[i] = UINT64_MAX;  // 未使用过（fenceValue 从 1 开始递增）
        m_batchOfSlot[i] = 0;
    }

    // 复用现有 ExecuteIndirect 的 command signature（drawIndexed 用 indexed）。
    const DeviceContext& dc = deviceContextForJni();
    m_cmdSigIndexed = dc.cmdSigIndexed;
    m_cmdSigNonIndexed = dc.cmdSigNonIndexed;
    return true;
}

bool BundleRecorder::begin(UINT frameSlot, UINT64 frameValue) {
    if (m_recording) return false;
    const UINT fs = frameSlot % kBundleFrameSlots;
    // 新的一帧：该帧槽内的批次序号归零，重新从这批的第一个 allocator 槽开始。
    if (m_frameOfSlot[fs] != frameValue) {
        m_frameOfSlot[fs] = frameValue;
        m_batchOfSlot[fs] = 0;
    }
    const UINT batch = m_batchOfSlot[fs];
    if (batch >= kBundleBatchesPerFrame) return false;  // 本帧配额用尽，回退串行
    m_batchOfSlot[fs] = batch + 1;

    const UINT slot = fs * kBundleBatchesPerFrame + batch;
    m_activeSlot = slot;
    m_bundle = m_lists[slot];
    if (!m_bundle || !m_allocators[slot]) return false;
    HRESULT hr = m_allocators[slot]->Reset();
    if (FAILED(hr)) return false;
    hr = m_bundle->Reset(m_allocators[slot].Get(), nullptr);
    if (FAILED(hr)) return false;

    // D3D12 bundle 不继承父命令列表的描述符堆。本 bundle 会录制
    // SetGraphicsRootDescriptorTable，必须在此（任何描述符表命令之前、且整段
    // bundle 仅一次）显式绑定 shader-visible 堆；否则运行时解析 GPU 句柄时
    // 读到空堆指针（命令列表成员 +0x28）而访问冲突崩溃。
    // 该堆必须与父命令列表执行本 bundle 时绑定的堆完全一致。
    if (m_heap) {
        ID3D12DescriptorHeap* heaps[] = { m_heap.Get() };
        m_bundle->SetDescriptorHeaps(1, heaps);
    }

    m_recording = true;
    m_rootSig = nullptr;
    m_pso = nullptr;
    m_topology = D3D_PRIMITIVE_TOPOLOGY_UNDEFINED;
    m_pipeline = nullptr;
    resetStats();
    return true;
}

ID3D12GraphicsCommandList* BundleRecorder::end() {
    if (!m_bundle || !m_recording) return nullptr;
    m_recording = false;
    if (FAILED(m_bundle->Close())) return nullptr;
    return m_bundle.Get();
}

void BundleRecorder::setRootSignature(ID3D12RootSignature* rootSignature) {
    if (!m_recording || !rootSignature || m_rootSig == rootSignature) return;
    m_bundle->SetGraphicsRootSignature(rootSignature);
    m_rootSig = rootSignature;
    ++m_stats.commandsRecorded;
}

void BundleRecorder::setPipelineState(ID3D12PipelineState* pso) {
    if (!m_recording || !pso || m_pso == pso) return;
    m_bundle->SetPipelineState(pso);
    m_pso = pso;
    ++m_stats.commandsRecorded;
}

void BundleRecorder::setPipeline(const Dx12Pipeline* pipeline) {
    m_pipeline = pipeline;
}

UINT BundleRecorder::correctedStride(int slot) const {
    if (!m_pipeline) return 0;
    auto it = m_pipeline->vertexStrides.find(slot);
    return it == m_pipeline->vertexStrides.end() ? 0u : it->second;
}

void BundleRecorder::setDescriptorTable(UINT slot, D3D12_GPU_DESCRIPTOR_HANDLE handle) {
    if (!m_recording) return;
    m_bundle->SetGraphicsRootDescriptorTable(slot, handle);
    ++m_stats.commandsRecorded;
    ++m_stats.descriptorUpdates;
}

void BundleRecorder::setRootConstantBufferView(UINT rootIndex,
    D3D12_GPU_VIRTUAL_ADDRESS address) {
    if (!m_recording) return;
    m_bundle->SetGraphicsRootConstantBufferView(rootIndex, address);
    ++m_stats.commandsRecorded;
}

void BundleRecorder::setVertexBuffers(UINT startSlot, UINT count,
    const D3D12_VERTEX_BUFFER_VIEW* views) {
    if (!m_recording || !views || count == 0) return;
    m_bundle->IASetVertexBuffers(startSlot, count, views);
    ++m_stats.commandsRecorded;
}

void BundleRecorder::setIndexBuffer(const D3D12_INDEX_BUFFER_VIEW* view) {
    if (!m_recording || !view) return;
    m_bundle->IASetIndexBuffer(view);
    ++m_stats.commandsRecorded;
}

void BundleRecorder::setPrimitiveTopology(D3D12_PRIMITIVE_TOPOLOGY topology) {
    if (!m_recording || m_topology == topology) return;
    m_bundle->IASetPrimitiveTopology(topology);
    m_topology = topology;
    ++m_stats.commandsRecorded;
}

void BundleRecorder::drawIndexedInstanced(UINT indexCount, UINT instanceCount,
    UINT startIndexLocation, INT baseVertexLocation, UINT startInstanceLocation) {
    if (!m_recording || indexCount == 0) return;
    m_bundle->DrawIndexedInstanced(indexCount, instanceCount,
        startIndexLocation, baseVertexLocation, startInstanceLocation);
    ++m_stats.drawCalls;
    m_stats.indexCount += indexCount;
}

void BundleRecorder::drawInstanced(UINT vertexCount, UINT instanceCount,
    UINT startVertexLocation, UINT startInstanceLocation) {
    if (!m_recording || vertexCount == 0) return;
    m_bundle->DrawInstanced(vertexCount, instanceCount,
        startVertexLocation, startInstanceLocation);
    ++m_stats.drawCalls;
    m_stats.vertexCount += vertexCount;
}

bool BundleRecorder::drawIndexedIndirect(ID3D12Resource* commands, UINT64 offset,
    UINT drawCount) {
    if (!m_recording || !commands || drawCount == 0) return false;
    // D3D12 禁止向 ExecuteIndirect 传 nullptr command signature。
    if (!m_cmdSigIndexed) return false;
    m_bundle->ExecuteIndirect(m_cmdSigIndexed.Get(), drawCount, commands, offset,
        nullptr, 0);
    m_stats.drawCalls += drawCount;
    return true;
}

bool BundleRecorder::drawIndirect(ID3D12Resource* commands, UINT64 offset,
    UINT drawCount) {
    if (!m_recording || !commands || drawCount == 0) return false;
    if (!m_cmdSigNonIndexed) return false;
    m_bundle->ExecuteIndirect(m_cmdSigNonIndexed.Get(), drawCount, commands, offset,
        nullptr, 0);
    m_stats.drawCalls += drawCount;
    return true;
}

bool BundleRecorderPool::init(ID3D12Device* device, ID3D12DescriptorHeap* heap,
    UINT workerCount) {
    if (!device || workerCount == 0) return false;
    shutdown();
    m_recorders.reserve(workerCount);
    for (UINT i = 0; i < workerCount; ++i) {
        std::unique_ptr<BundleRecorder> rec(new BundleRecorder());
        if (!rec->init(device, heap, i)) {
            shutdown();
            return false;
        }
        m_recorders.push_back(std::move(rec));
    }
    return true;
}

void BundleRecorderPool::shutdown() {
    m_recorders.clear();
}

BundleRecorder* BundleRecorderPool::recorder(UINT worker) {
    if (worker >= m_recorders.size()) return nullptr;
    return m_recorders[worker].get();
}

void executeBundle(ID3D12GraphicsCommandList* mainList,
    ID3D12GraphicsCommandList* bundle) {
    if (!mainList || !bundle) return;
    mainList->ExecuteBundle(bundle);
}

}  // namespace dx12mc
