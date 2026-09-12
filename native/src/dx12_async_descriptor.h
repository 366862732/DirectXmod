#pragma once

// dx12-mc 异步渲染基础设施（P33）：分区描述符分配器
//
// 方案取舍：不使用“每 worker CPU-only 镜像堆 + 主线程 CopyDescriptorsSimple
// 统一回落”的方案，而是把 DeviceContext 现有的 SHADER_VISIBLE drawHeap
// 按 (frameSlot, worker) 划分为互不相交的连续区域：
//
//     regionBaseSlot(frameSlot, worker)
//         = frameSlot * sectionSize + reservedPerSection + worker * slotsPerRegion
//     其中 sectionSize = heapSize / kAsyncFrameSlots，
//     slotsPerRegion = (sectionSize - reservedPerSection) / workerCount。
//
// 每个帧槽 section 的起始 reservedPerSection 个槽位保留给同步路径 ring（见
// kAsyncSyncRingReserve），其余按 worker 继续切分为互不相交的区域。
//
// 每个区域由所属 worker 在录制 bundle 期间通过原子游标独占分配。worker 直接把
// 自己的瞬时 CBV/SRV 描述符写入本区域（CPU 侧写 SHADER_VISIBLE 堆是合法的），
// bundle 内 SetGraphicsRootDescriptorTable 使用 drawHeap 的 GPU 基址 + 区域偏移。
// 区域互不相交 => 不同 (frameSlot, worker) 之间无数据竞争，跨帧并发安全。
//
// 帧槽数固定为 kAsyncFrameSlots（与主循环计划的最大在飞帧数一致），worker
// 数可配（<= kMaxAsyncWorkers）。

#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <d3d12.h>
#include <wrl/client.h>

#include <array>
#include <atomic>
#include <cstdint>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

// 帧槽数：计划的最大在飞帧数（现有 drawHeap ring 亦为 x4）。
constexpr UINT kAsyncFrameSlots = 4;
// 单帧槽内可划分的 worker 分区上限。
constexpr UINT kMaxAsyncWorkers = 8;
// 每个帧槽起始处保留给同步路径 ring 的槽位数（blit / 小批量同步 draw）。
constexpr UINT kAsyncSyncRingReserve = 8192;

class PartitionedDescriptorAllocator {
public:
    PartitionedDescriptorAllocator() = default;
    ~PartitionedDescriptorAllocator() = default;

    PartitionedDescriptorAllocator(const PartitionedDescriptorAllocator&) = delete;
    PartitionedDescriptorAllocator& operator=(const PartitionedDescriptorAllocator&) = delete;

    // heap 为 DeviceContext::drawHeap（CBV_SRV_UAV / SHADER_VISIBLE，非拥有，
    // 仅额外持有一个引用保持存活）。heapSize 为堆槽位数（可由 GetDesc 获取）。
    // workerCount 为每帧槽内的分区数。
    // reservedPerSection 为每个帧槽（堆的 1/kAsyncFrameSlots）起始处保留给同步
    // 路径 ring 的槽位数——异步模式下仍有少量 draw 走同步直连（小批量 chunk、
    // surface blit），必须与 worker 分区互不重叠。失败返回 false。
    bool init(ID3D12Device* device, ID3D12DescriptorHeap* heap,
        UINT heapSize, UINT workerCount, UINT reservedPerSection = 0);

    bool valid() const { return m_heap != nullptr && m_slotsPerRegion > 0; }
    UINT workerCount() const { return m_workerCount; }
    UINT slotsPerRegion() const { return m_slotsPerRegion; }
    UINT heapSize() const { return m_heapSize; }
    UINT incrementSize() const { return m_increment; }

    // (frameSlot, worker) 区域的绝对起始槽位；参数越界返回 UINT_MAX。
    UINT regionBaseSlot(UINT frameSlot, UINT worker) const;

    // 在 (frameSlot, worker) 区域内原子分配 count 个连续槽位。
    // 成功返回绝对起始槽位；区域容量不足返回 UINT_MAX。
    UINT allocate(UINT frameSlot, UINT worker, UINT count);

    // 帧边界：清零指定帧槽下所有 worker 的分配游标。
    // 调用前提：该帧槽对应的 GPU 工作已完成（无并发分配）。
    void resetFrame(UINT frameSlot);

    // 绝对槽位的 CPU / GPU 句柄；slot 越界时返回空句柄（ptr=0）。
    D3D12_CPU_DESCRIPTOR_HANDLE cpuHandle(UINT slot) const;
    D3D12_GPU_DESCRIPTOR_HANDLE gpuHandle(UINT slot) const;

    // 写入辅助：与现有 gCtx.drawInc（CBV_SRV_UAV 增量）对齐。
    // CBV：把 buffer 资源的 [offset, offset+size) 写为常量缓冲视图。
    bool writeCBV(UINT slot, ID3D12Resource* buffer, UINT64 offset, UINT64 size);
    // 复制一个已存在的描述符（如 texture view 的 CPU 句柄）到 slot。
    bool writeDescriptor(UINT slot, D3D12_CPU_DESCRIPTOR_HANDLE src);

    ID3D12DescriptorHeap* heap() const { return m_heap.Get(); }

private:
    UINT regionIndex(UINT frameSlot, UINT worker) const {
        return frameSlot * m_workerCount + worker;
    }

    ComPtr<ID3D12Device> m_device;
    ComPtr<ID3D12DescriptorHeap> m_heap;
    UINT m_heapSize = 0;
    UINT m_workerCount = 0;
    UINT m_slotsPerRegion = 0;
    UINT m_sectionSize = 0;
    UINT m_reservedPerSection = 0;
    UINT m_increment = 0;
    std::array<std::atomic<UINT>, (size_t)kAsyncFrameSlots * kMaxAsyncWorkers> m_cursors;
};

}  // namespace dx12mc
