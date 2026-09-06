#pragma once

// 禁用 Windows min/max 宏，避免与 std::max/std::min 冲突（须在任何
// windows.h/d3d12.h 之前定义）。
#ifndef NOMINMAX
#define NOMINMAX
#endif

// dx12-mc 原生 D3D12 层（C++）
// P33：线程安全分段描述符分配器（SegmentedDescriptorAllocator）。
//
// 背景：现有 dx12_device.cpp 用匿名 namespace 的 gNextSrv/gFreeSrvSlots
// （单线程 bump + free-list）与 drawHeap 4 段 ring（kDrawHeapPerFrame=32768 /
// kDrawHeapSections=4，见 dx12_device.cpp L80-87）管理描述符槽位。P33 需要
// 多 worker 线程并发分配描述符槽位且互相不冲突——上述单线程路径无法直接复用。
//
// 设计意图（见 多线程方案.md 第 3 章 ASYNC-01 修复要求）：
// 「并发写 SHADER_VISIBLE 堆必须互斥或分区隔离」。本类采用【分区隔离】：
//   1. 拥有一个 ID3D12DescriptorHeap，逻辑上切为 sections 段，每段
//      descriptorsPerSection 个描述符（段与段之间在堆内物理连续）。
//   2. sections 段 = inFlightFrames × workerCount 正交网格。某 worker 在
//      第 frame 帧独占段 sectionFor(frame, worker) =
//      (frame % inFlightFrames) * workerCount + worker —— 帧维 + worker 维
//      双正交，故任意两个并发录制的 worker 永远命中不同段 → 并发写零竞争。
//   3. 即使某段被误用（多线程碰同一段），段内 bump 分配用 std::atomic
//      fetch_add（索引唯一）、槽位复用区用 std::mutex 保护 → 同段内连续
//      分配依然互斥安全。
//
// 语义约定：
//   * 复制描述符不在这里做：录制线程调用 allocSlot + cpuStartFor 拿到本段
//     起始 CPU 句柄后，只写自己的段（CPU 侧写 SHADER_VISIBLE 堆合法）。
//   * ring 帧带复用须遵守 GPU fence 语义：帧 N+inFlightFrames 重用段 N 前，
//     必须确保 GPU 已读完段 N（与 drawHeap ring「帧 N+2 重写帧 N 半区前
//     submit 已等 N 完成」的约束同理，见 dx12_device.cpp L80-87 注释）。
//     到期的段调用 resetSection() 把游标归零即可整段复用，无需逐槽 free。
//
// 线程安全声明（本文件对外契约）：
//   * 不同 worker、不同段：零竞争、无共享可变状态。
//   * 同段内并发 allocSlot：std::atomic fetch_add 保证返回索引互不相同；
//     同段内 freeSlot / allocSlot 慢路径 / resetSection 以 std::mutex 互斥。
//   * 对象的构造、reset()、析构须在"所有段已无并发分配"的帧边界执行。

#include <d3d12.h>
#include <wrl/client.h>

#include <atomic>
#include <climits>
#include <cstdint>
#include <memory>
#include <mutex>
#include <vector>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

class SegmentedDescriptorAllocator {
public:
    // 构造并立即创建底层描述符堆。参数非法或 CreateDescriptorHeap 失败时
    // valid()==false（其余 getter 返回 0/空句柄），调用方须先检查 valid()。
    //   device               ：ID3D12Device（非空）
    //   type                 ：堆类型（CBV_SRV_UAV / SAMPLER / RTV / DSV）
    //   shaderVisible        ：true = SHADER_VISIBLE（GPU 可绑定，gpuStartFor 有效）；
    //                          false = CPU-only 堆（只能作 CopyDescriptorsSimple 源/目标）
    //   sections             ：段总数，须 = inFlightFrames × workerCount（>=1 且能被 workerCount 整除）
    //   descriptorsPerSection：每段描述符数（>=1）。注意 SAMPLER 的 SHADER_VISIBLE
    //                          堆硬上限 2048、CBV_SRV_UAV 的 SHADER_VISIBLE 堆上限
    //                          1,000,000（Tier1），总数超限时 CreateDescriptorHeap 失败
    //   workerCount          ：并发 worker 数（默认 1）。段在帧维与 worker 维的正交
    //                          布局由它决定；越界 worker 的 sectionFor 返回 UINT_MAX
    SegmentedDescriptorAllocator(ID3D12Device* device,
        D3D12_DESCRIPTOR_HEAP_TYPE type, bool shaderVisible,
        UINT sections, UINT descriptorsPerSection, UINT workerCount = 1);

    // 堆创建是否成功（失败原因多为参数非法/描述符总数超平台上限）。
    bool valid() const { return m_heap != nullptr; }

    // ---- 只读配置 ----
    UINT sectionCount() const { return m_sections; }
    UINT descriptorsPerSection() const { return m_descriptorsPerSection; }
    UINT workerCount() const { return m_workerCount; }
    // 帧维段数 = sections / workerCount（sectionFor 内部按 frame % inFlightFrames 取帧带）。
    UINT inFlightFrames() const {
        return m_workerCount ? m_sections / m_workerCount : 0;
    }
    // 该堆类型单个描述符的字节步长（CPU/GPU 句柄共用同一步长）。
    UINT increment() const { return m_inc; }
    bool shaderVisible() const { return m_shaderVisible; }
    ID3D12DescriptorHeap* heap() const { return m_heap.Get(); }

    // ---- 段寻址（录制线程只读，无锁） ----
    // frameIndex 与 worker 正交：返回该 (frame, worker) 独占的段号 [0, sections)，
    // 保证并发录制的不同 worker 永不命中同一段。workerIndex 越界返回 UINT_MAX。
    UINT sectionFor(UINT frameIndex, UINT workerIndex) const;

    // 段内第 0 号描述符的 CPU 句柄（录制线程以此为基址、按 increment() 步长写
    // 自己的段）。section 越界返回空句柄（ptr=0）。
    D3D12_CPU_DESCRIPTOR_HANDLE cpuStartFor(UINT section) const;
    // 段内第 0 号描述符的 GPU 句柄（绑定 descriptor table 用）；仅 shaderVisible
    // 堆有效，非 SHADER_VISIBLE 或 section 越界返回空句柄（ptr=0）。
    D3D12_GPU_DESCRIPTOR_HANDLE gpuStartFor(UINT section) const;

    // ---- 槽位分配 / 释放（线程安全） ----
    // 在 section 内分配一个槽位，返回【段内索引】；配合
    //   cpuHandle = cpuStartFor(section); cpuHandle.ptr += (SIZE_T)index * increment();
    // 使用。段满（线性游标耗尽且无复用槽）返回 UINT_MAX。
    // 线程安全：快路径 = 段内 std::atomic fetch_add（不同段零竞争；同段并发也
    // 由原子保证索引唯一）；线性耗尽后回退 freeList 复用区，以段内 std::mutex 互斥。
    UINT allocSlot(UINT section);

    // 归还 section 内的槽位供复用。调用方须保证该槽位不再被 GPU/CPU 读取
    // （与 allocSlot 返回的索引一一配对，重复 free 是调用方错误）。槽位内容
    // 不擦除，下次 allocSlot 复用时由录制线程覆写。
    void freeSlot(UINT section, UINT index);

    // ring 帧带语义：整段游标归零 + 清空复用列表。调用前提是该段的 GPU 读取
    // 已由 fence 保证完成、且无并发 allocSlot（帧边界单线程调用）。
    void resetSection(UINT section);
    // 全部段 resetSection（须在无任何并发分配的帧边界调用）。
    void reset();

private:
    // 每段的分配状态：快路径线性游标（原子） + 慢路径复用区（互斥）。
    struct Section {
        std::atomic<UINT> next{0};   // 段内线性分配游标（fetch_add 无锁快路径）
        std::mutex mutex;            // 保护 freeList：同段慢路径/free/reset 互斥
        std::vector<UINT> freeList;  // 释放槽位 LIFO（供线性耗尽后复用）
    };

    ComPtr<ID3D12DescriptorHeap> m_heap;
    D3D12_CPU_DESCRIPTOR_HANDLE m_cpuStart{};  // 堆起始 CPU 句柄
    D3D12_GPU_DESCRIPTOR_HANDLE m_gpuStart{};  // 堆起始 GPU 句柄（仅 SHADER_VISIBLE）
    UINT m_inc = 0;                   // 该堆类型句柄步长
    UINT m_sections = 0;              // 段总数（= inFlightFrames × workerCount）
    UINT m_descriptorsPerSection = 0; // 每段描述符数
    UINT m_workerCount = 1;           // worker 维段数
    bool m_shaderVisible = false;
    // 每段分配状态（索引 = 段号）。用 unique_ptr<Section[]> 而非 vector：Section
    // 含 std::atomic/std::mutex（不可拷贝/移动），数组按默认构造原地建段、不搬迁。
    std::unique_ptr<Section[]> m_sectionsState;
};

}  // namespace dx12mc
