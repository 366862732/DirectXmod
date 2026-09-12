#pragma once

// dx12-mc 异步渲染基础设施（P33）：主 Command List 执行器
//
// 职责：
//   1. tryBeginFrame()：非阻塞开始一帧。目标 allocator 槽对应的上一帧若仍在
//      GPU 执行（fence 未达），返回 false（调用方跳过本帧/下一帧重试），
//      绝不阻塞主线程等待 GPU。
//   2. addBundle()：聚合各 worker 录制好的 bundle。
//   3. addResourceBarrier()/addTransition()：在主列表执行 barrier（bundle
//      内禁止 barrier，故统一在此执行）。
//   4. endFrame()：执行 barrier + ExecuteBundle 聚合，Close，提交到队列并 Signal。
//
// allocator ring：kFramesInFlight 组 (allocator, commandList)，逐帧轮转；
// 复用某槽前要求该槽上一次提交的 fence 已完成（非阻塞判断）。

#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <d3d12.h>
#include <wrl/client.h>

#include <cstdint>
#include <mutex>
#include <vector>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

class MainCommandExecutor {
public:
    static constexpr UINT kFramesInFlight = 4;

    MainCommandExecutor() = default;
    ~MainCommandExecutor() = default;

    MainCommandExecutor(const MainCommandExecutor&) = delete;
    MainCommandExecutor& operator=(const MainCommandExecutor&) = delete;

    bool init(ID3D12Device* device, ID3D12CommandQueue* queue);
    bool valid() const { return m_slots[0].commandList != nullptr; }

    // 非阻塞开始一帧；失败（上一帧未完成 / 已在帧中 / Reset 失败）返回 false。
    bool tryBeginFrame();

    // 聚合待执行的 bundle（当前帧内有效）。
    bool addBundle(ID3D12GraphicsCommandList* bundle);
    bool addBundles(ID3D12GraphicsCommandList* const* bundles, UINT count);

    // 主列表上的资源屏障（bundle 内禁止，故集中管理）。
    bool addResourceBarrier(const D3D12_RESOURCE_BARRIER& barrier);
    bool addTransition(ID3D12Resource* resource, D3D12_RESOURCE_STATES before,
        D3D12_RESOURCE_STATES after);

    // 关闭主列表 -> 执行 barrier + bundle -> 提交 -> Signal。
    // 返回本次提交的 fence 值；失败返回 0。
    UINT64 endFrame();

    bool isFrameInProgress() const { return m_frameInProgress; }
    UINT64 lastFenceValue() const { return m_lastFenceValue; }

    // 阻塞等待 fence 达到 fenceValue；timeoutMs==0 表示无限等待。
    bool waitForFrame(UINT64 fenceValue, UINT64 timeoutMs);

    struct Stats {
        UINT64 framesSubmitted = 0;
        UINT64 bundlesExecuted = 0;
        UINT64 barriersExecuted = 0;
    };
    Stats getStats() const;

private:
    struct FrameSlot {
        ComPtr<ID3D12CommandAllocator> allocator;
        ComPtr<ID3D12GraphicsCommandList> commandList;
        UINT64 submittedFenceValue = 0;
    };

    void clearFrameState();
    bool submitFrame(FrameSlot& slot);

    ComPtr<ID3D12Device> m_device;
    ComPtr<ID3D12CommandQueue> m_queue;
    ComPtr<ID3D12Fence> m_fence;

    FrameSlot m_slots[kFramesInFlight];
    UINT m_slotIndex = 0;
    UINT64 m_fenceValue = 0;
    UINT64 m_lastFenceValue = 0;
    bool m_frameInProgress = false;

    std::vector<ID3D12GraphicsCommandList*> m_bundles;
    std::vector<D3D12_RESOURCE_BARRIER> m_barriers;
    mutable std::mutex m_mutex;
    mutable Stats m_stats;
};

}  // namespace dx12mc
