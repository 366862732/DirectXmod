// dx12-mc 原生 D3D12 层（C++）
// P33：线程安全分段描述符分配器实现。
// 设计说明见 dx12_descriptor_allocator.h 顶部注释（ASYNC-01 分区隔离方案）。

#include "dx12_descriptor_allocator.h"

namespace dx12mc {

SegmentedDescriptorAllocator::SegmentedDescriptorAllocator(
    ID3D12Device* device, D3D12_DESCRIPTOR_HEAP_TYPE type, bool shaderVisible,
    UINT sections, UINT descriptorsPerSection, UINT workerCount)
    : m_shaderVisible(shaderVisible) {
    // 参数校验：device 非空、段/worker/每段容量非零、段数能被 worker 数整除
    // （保证 inFlightFrames = sections / workerCount 精确无余，帧维与 worker 维
    // 正交网格才不重不漏地覆盖全部段）。非法参数一律保持 valid()==false。
    if (!device || sections == 0 || descriptorsPerSection == 0 || workerCount == 0 ||
        sections % workerCount != 0) {
        return;
    }

    // 描述符总数可能溢出 UINT（调用方乱传参数时防御），先按 UINT64 校验。
    const UINT64 total64 = static_cast<UINT64>(sections) * descriptorsPerSection;
    if (total64 > UINT_MAX) {
        return;
    }
    const UINT totalDescriptors = static_cast<UINT>(total64);

    m_inc = device->GetDescriptorHandleIncrementSize(type);

    D3D12_DESCRIPTOR_HEAP_DESC desc{};
    desc.Type = type;
    desc.NumDescriptors = totalDescriptors;
    desc.Flags = shaderVisible ? D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE
                               : D3D12_DESCRIPTOR_HEAP_FLAG_NONE;

    HRESULT hr = device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&m_heap));
    if (FAILED(hr)) {
        m_heap.Reset();  // 保持 valid()==false（例如描述符总数超平台堆上限）
        return;
    }

    m_cpuStart = m_heap->GetCPUDescriptorHandleForHeapStart();
    if (shaderVisible) {
        // 非 SHADER_VISIBLE 堆没有有意义的 GPU 句柄，跳过（gpuStartFor 返回空句柄）。
        m_gpuStart = m_heap->GetGPUDescriptorHandleForHeapStart();
    }

    m_sections = sections;
    m_descriptorsPerSection = descriptorsPerSection;
    m_workerCount = workerCount;
    m_sectionsState = std::make_unique<Section[]>(sections);  // 每段默认构造：游标 0、空复用列表
}

// 段号 = 帧带 × workerCount + worker：帧维与 worker 维正交。
//   * 固定 worker、不同帧（差 < inFlightFrames）→ 不同帧带 → 段号不同；
//   * 固定帧带、不同 worker → 段号不同。
// 故任意并发录制的 worker（各自 frame/worker 组合不同）必然落在不同段，
// 段之间无任何共享可变状态 → 并发写 SHADER_VISIBLE 堆零竞争（ASYNC-01 分区隔离）。
UINT SegmentedDescriptorAllocator::sectionFor(UINT frameIndex, UINT workerIndex) const {
    if (m_workerCount == 0) {
        return UINT_MAX;  // 构造失败（valid()==false）时的防御
    }
    if (workerIndex >= m_workerCount) {
        return UINT_MAX;  // worker 越界：调用方把 frame 段数量当 worker 段数量用了
    }
    const UINT inFlight = m_sections / m_workerCount;
    const UINT frameBand = (inFlight != 0) ? (frameIndex % inFlight) : 0;
    return frameBand * m_workerCount + workerIndex;
}

D3D12_CPU_DESCRIPTOR_HANDLE SegmentedDescriptorAllocator::cpuStartFor(UINT section) const {
    D3D12_CPU_DESCRIPTOR_HANDLE h{};
    if (m_heap && section < m_sections) {
        // 段间在堆内物理连续：section 起始 = 堆起始 + section × 每段字节跨度。
        const SIZE_T sectionBytes =
            static_cast<SIZE_T>(section) * m_descriptorsPerSection * m_inc;
        h.ptr = m_cpuStart.ptr + sectionBytes;
    }
    return h;  // 越界返回空句柄（ptr=0），调用方使用即崩溃，便于暴露调用错误
}

D3D12_GPU_DESCRIPTOR_HANDLE SegmentedDescriptorAllocator::gpuStartFor(UINT section) const {
    D3D12_GPU_DESCRIPTOR_HANDLE h{};
    if (m_shaderVisible && m_heap && section < m_sections) {
        const SIZE_T sectionBytes =
            static_cast<SIZE_T>(section) * m_descriptorsPerSection * m_inc;
        h.ptr = m_gpuStart.ptr + sectionBytes;
    }
    return h;
}

UINT SegmentedDescriptorAllocator::allocSlot(UINT section) {
    if (section >= m_sections) {
        return UINT_MAX;
    }
    Section& st = m_sectionsState[section];

    // 快路径（热点）：无锁原子递增。即使同段被多线程并发调用，fetch_add 也保证
    // 每次调用拿到互不相同的游标值 → 索引唯一、无数据竞争。不同段之间则完全不
    // 共享缓存行以上的任何状态（每段一个独立 atomic），恒零竞争。
    const UINT idx = st.next.fetch_add(1, std::memory_order_relaxed);
    if (idx < m_descriptorsPerSection) {
        return idx;
    }

    // 慢路径：段内线性槽位耗尽。锁内优先复用 freeSlot 归还的槽（LIFO），
    // 否则段真满返回 UINT_MAX。此锁仅在同一段内部存在竞争（不同段无交集）。
    std::lock_guard<std::mutex> lock(st.mutex);
    if (!st.freeList.empty()) {
        const UINT reused = st.freeList.back();
        st.freeList.pop_back();
        return reused;
    }
    return UINT_MAX;  // 段满（该 (frame, worker) 组合单帧描述符超过段容量）
}

void SegmentedDescriptorAllocator::freeSlot(UINT section, UINT index) {
    if (section >= m_sections || index >= m_descriptorsPerSection) {
        return;
    }
    Section& st = m_sectionsState[section];
    std::lock_guard<std::mutex> lock(st.mutex);  // 与同段 allocSlot 慢路径/reset 互斥
    st.freeList.push_back(index);
}

void SegmentedDescriptorAllocator::resetSection(UINT section) {
    if (section >= m_sections) {
        return;
    }
    Section& st = m_sectionsState[section];
    // 须由调用方保证该段 GPU 读取已完成且无并发 allocSlot（帧边界）。锁内整段
    // 归零：游标回 0（下次分配重新从段头覆盖），并清空复用列表（旧 free 槽位
    // 一并作废——整段复用后它们本就属于新一轮生命周期）。
    std::lock_guard<std::mutex> lock(st.mutex);
    st.next.store(0, std::memory_order_relaxed);
    st.freeList.clear();
}

void SegmentedDescriptorAllocator::reset() {
    for (UINT s = 0; s < m_sections; ++s) {
        resetSection(s);
    }
}

}  // namespace dx12mc
