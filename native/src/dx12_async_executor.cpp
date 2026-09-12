#include "dx12_async_executor.h"

#include "dx12_device.h"  // deviceContextForJni（获取现有 drawHeap 以设置描述符堆）

namespace dx12mc {

bool MainCommandExecutor::init(ID3D12Device* device, ID3D12CommandQueue* queue) {
    if (!device || !queue) return false;

    m_device = device;
    m_queue = queue;

    if (FAILED(device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&m_fence)))) {
        return false;
    }

    for (UINT i = 0; i < kFramesInFlight; ++i) {
        FrameSlot& slot = m_slots[i];
        if (FAILED(device->CreateCommandAllocator(
                D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&slot.allocator)))) {
            return false;
        }
        if (FAILED(device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
                slot.allocator.Get(), nullptr, IID_PPV_ARGS(&slot.commandList)))) {
            return false;
        }
        slot.commandList->Close();
        slot.submittedFenceValue = 0;
    }
    m_slotIndex = 0;
    m_fenceValue = 0;
    m_lastFenceValue = 0;
    m_frameInProgress = false;
    return true;
}

void MainCommandExecutor::clearFrameState() {
    std::lock_guard<std::mutex> lk(m_mutex);
    m_bundles.clear();
    m_barriers.clear();
}

bool MainCommandExecutor::tryBeginFrame() {
    if (!valid() || m_frameInProgress) return false;

    FrameSlot& slot = m_slots[m_slotIndex];
    // 非阻塞：该槽上一帧尚未完成则本帧跳过（绝不等待 GPU）。
    if (slot.submittedFenceValue > 0
        && m_fence->GetCompletedValue() < slot.submittedFenceValue) {
        return false;
    }

    HRESULT hr = slot.allocator->Reset();
    if (FAILED(hr)) return false;
    hr = slot.commandList->Reset(slot.allocator.Get(), nullptr);
    if (FAILED(hr)) return false;

    // bundle 内禁止 SetDescriptorHeaps，须由父列表设置（bundle 复用其堆）。
    ID3D12DescriptorHeap* heap = deviceContextForJni().drawHeap.Get();
    if (heap) {
        ID3D12DescriptorHeap* heaps[] = { heap };
        slot.commandList->SetDescriptorHeaps(1, heaps);
    }

    clearFrameState();
    m_frameInProgress = true;
    return true;
}

bool MainCommandExecutor::addBundle(ID3D12GraphicsCommandList* bundle) {
    if (!bundle || !m_frameInProgress) return false;
    std::lock_guard<std::mutex> lk(m_mutex);
    m_bundles.push_back(bundle);
    return true;
}

bool MainCommandExecutor::addBundles(ID3D12GraphicsCommandList* const* bundles,
    UINT count) {
    if (!bundles || count == 0 || !m_frameInProgress) return false;
    std::lock_guard<std::mutex> lk(m_mutex);
    for (UINT i = 0; i < count; ++i) {
        if (bundles[i]) m_bundles.push_back(bundles[i]);
    }
    return true;
}

bool MainCommandExecutor::addResourceBarrier(const D3D12_RESOURCE_BARRIER& barrier) {
    if (!m_frameInProgress) return false;
    std::lock_guard<std::mutex> lk(m_mutex);
    m_barriers.push_back(barrier);
    return true;
}

bool MainCommandExecutor::addTransition(ID3D12Resource* resource,
    D3D12_RESOURCE_STATES before, D3D12_RESOURCE_STATES after) {
    if (!resource) return false;
    D3D12_RESOURCE_BARRIER barrier{};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrier.Flags = D3D12_RESOURCE_BARRIER_FLAG_NONE;
    barrier.Transition.pResource = resource;
    barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    barrier.Transition.StateBefore = before;
    barrier.Transition.StateAfter = after;
    return addResourceBarrier(barrier);
}

bool MainCommandExecutor::submitFrame(FrameSlot& slot) {
    ID3D12CommandList* lists[] = { slot.commandList.Get() };
    m_queue->ExecuteCommandLists(1, lists);

    const UINT64 value = ++m_fenceValue;
    if (FAILED(m_queue->Signal(m_fence.Get(), value))) return false;

    slot.submittedFenceValue = value;
    m_lastFenceValue = value;
    {
        std::lock_guard<std::mutex> lk(m_mutex);
        ++m_stats.framesSubmitted;
    }
    return true;
}

UINT64 MainCommandExecutor::endFrame() {
    if (!m_frameInProgress || !valid()) return 0;

    FrameSlot& slot = m_slots[m_slotIndex];

    std::vector<D3D12_RESOURCE_BARRIER> barriers;
    {
        std::lock_guard<std::mutex> lk(m_mutex);
        barriers.swap(m_barriers);
    }

    // barrier 统一在主列表执行（bundle 内禁止 barrier）。
    // 注意：bundle 由 replayPendingBundles() 在 ctx 上 Execute，此处不重复执行。
    if (!barriers.empty()) {
        slot.commandList->ResourceBarrier((UINT)barriers.size(), barriers.data());
        std::lock_guard<std::mutex> lk(m_mutex);
        m_stats.barriersExecuted += barriers.size();
    }

    if (FAILED(slot.commandList->Close())) {
        m_frameInProgress = false;
        clearFrameState();
        return 0;
    }
    if (!submitFrame(slot)) {
        m_frameInProgress = false;
        return 0;
    }

    m_frameInProgress = false;
    m_slotIndex = (m_slotIndex + 1) % kFramesInFlight;
    return m_lastFenceValue;
}

bool MainCommandExecutor::waitForFrame(UINT64 fenceValue, UINT64 timeoutMs) {
    if (!m_fence) return false;
    if (fenceValue == 0 || m_fence->GetCompletedValue() >= fenceValue) return true;

    HANDLE ev = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!ev) return false;
    if (FAILED(m_fence->SetEventOnCompletion(fenceValue, ev))) {
        CloseHandle(ev);
        return false;
    }
    DWORD ms = timeoutMs == 0 ? INFINITE
        : (timeoutMs > 0xFFFFFFF0ULL ? 0xFFFFFFF0ULL : (DWORD)timeoutMs);
    DWORD r = WaitForSingleObject(ev, ms);
    CloseHandle(ev);
    return r == WAIT_OBJECT_0;
}

MainCommandExecutor::Stats MainCommandExecutor::getStats() const {
    std::lock_guard<std::mutex> lk(m_mutex);
    return m_stats;
}

}  // namespace dx12mc
