#include "dx12_async_descriptor.h"

#include <algorithm>

namespace dx12mc {

bool PartitionedDescriptorAllocator::init(ID3D12Device* device,
    ID3D12DescriptorHeap* heap, UINT heapSize, UINT workerCount,
    UINT reservedPerSection) {
    if (!device || !heap || heapSize == 0) return false;
    if (workerCount == 0 || workerCount > kMaxAsyncWorkers) return false;

    // 帧槽与同步路径的 ring 段一一对应（均为 fenceValue % kAsyncFrameSlots）。
    const UINT sectionSize = heapSize / kAsyncFrameSlots;
    if (sectionSize == 0 || reservedPerSection >= sectionSize) return false;

    const UINT slotsPerRegion = (sectionSize - reservedPerSection) / workerCount;
    if (slotsPerRegion == 0) return false;  // 堆太小，无法给每个 (frameSlot,worker) 划分区域

    m_device = device;
    m_heap = heap;
    m_heapSize = heapSize;
    m_workerCount = workerCount;
    m_slotsPerRegion = slotsPerRegion;
    m_sectionSize = sectionSize;
    m_reservedPerSection = reservedPerSection;
    m_increment = device->GetDescriptorHandleIncrementSize(
        D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);

    for (auto& c : m_cursors) c.store(0, std::memory_order_relaxed);
    return true;
}

UINT PartitionedDescriptorAllocator::regionBaseSlot(UINT frameSlot, UINT worker) const {
    if (!valid()) return UINT_MAX;
    if (frameSlot >= kAsyncFrameSlots || worker >= m_workerCount) return UINT_MAX;
    return frameSlot * m_sectionSize + m_reservedPerSection
        + worker * m_slotsPerRegion;
}

UINT PartitionedDescriptorAllocator::allocate(UINT frameSlot, UINT worker, UINT count) {
    const UINT base = regionBaseSlot(frameSlot, worker);
    if (base == UINT_MAX || count == 0) return UINT_MAX;

    std::atomic<UINT>& cursor = m_cursors[regionIndex(frameSlot, worker)];
    UINT cur = cursor.load(std::memory_order_relaxed);
    for (;;) {
        if (cur > m_slotsPerRegion || count > m_slotsPerRegion - cur) {
            return UINT_MAX;  // 区域耗尽
        }
        if (cursor.compare_exchange_weak(cur, cur + count,
                std::memory_order_acq_rel, std::memory_order_relaxed)) {
            return base + cur;
        }
    }
}

void PartitionedDescriptorAllocator::resetFrame(UINT frameSlot) {
    if (!valid() || frameSlot >= kAsyncFrameSlots) return;
    for (UINT w = 0; w < m_workerCount; ++w) {
        m_cursors[regionIndex(frameSlot, w)].store(0, std::memory_order_release);
    }
}

D3D12_CPU_DESCRIPTOR_HANDLE PartitionedDescriptorAllocator::cpuHandle(UINT slot) const {
    D3D12_CPU_DESCRIPTOR_HANDLE h{};
    if (!m_heap || slot >= m_heapSize) return h;
    h = m_heap->GetCPUDescriptorHandleForHeapStart();
    h.ptr += static_cast<SIZE_T>(slot) * m_increment;
    return h;
}

D3D12_GPU_DESCRIPTOR_HANDLE PartitionedDescriptorAllocator::gpuHandle(UINT slot) const {
    D3D12_GPU_DESCRIPTOR_HANDLE h{};
    if (!m_heap || slot >= m_heapSize) return h;
    h = m_heap->GetGPUDescriptorHandleForHeapStart();
    h.ptr += static_cast<UINT64>(slot) * m_increment;
    return h;
}

bool PartitionedDescriptorAllocator::writeCBV(UINT slot, ID3D12Resource* buffer,
    UINT64 offset, UINT64 size) {
    if (!m_device || !buffer) return false;
    if (slot >= m_heapSize) return false;

    D3D12_CONSTANT_BUFFER_VIEW_DESC cbv{};
    cbv.BufferLocation = buffer->GetGPUVirtualAddress() + offset;
    UINT64 aligned = (size + 255) & ~255ULL;
    if (aligned == 0) aligned = 256;
    cbv.SizeInBytes = (UINT)std::min<UINT64>(aligned, 0xFFFFFFFFULL);
    m_device->CreateConstantBufferView(&cbv, cpuHandle(slot));
    return true;
}

bool PartitionedDescriptorAllocator::writeDescriptor(UINT slot,
    D3D12_CPU_DESCRIPTOR_HANDLE src) {
    if (!m_device || !m_heap || src.ptr == 0) return false;
    if (slot >= m_heapSize) return false;
    m_device->CopyDescriptorsSimple(1, cpuHandle(slot), src,
        D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    return true;
}

}  // namespace dx12mc
