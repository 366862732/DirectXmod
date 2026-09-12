#pragma once

// dx12-mc 异步渲染基础设施（P33）：Fence 管理器
//
// 用 SetEventOnCompletion + 专用等待线程替代 1ms 轮询：
//   - registerCallback(value, cb[, timeoutMs])：登记完成回调；
//   - 监控线程用 WaitForMultipleObjects(fenceEvent, wakeEvent) 精准等待“最早的
//     待完成 fence”，完成的 fence 一次性批量回调（不轮询）；
//   - waitFence：阻塞等待（同样基于 SetEventOnCompletion，非轮询）；
//   - signal：queue->Signal 便捷封装。
// 回调在锁外调用，允许回调内部再次注册/注销，避免自锁死。

#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <d3d12.h>
#include <wrl/client.h>

#include <atomic>
#include <cstdint>
#include <functional>
#include <mutex>
#include <thread>
#include <unordered_map>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

class AsyncFenceManager {
public:
    // success=false 表示超时或被 shutdown 取消。
    using FenceCallback = std::function<void(UINT64 fenceValue, bool success)>;

    AsyncFenceManager() = default;
    ~AsyncFenceManager();

    AsyncFenceManager(const AsyncFenceManager&) = delete;
    AsyncFenceManager& operator=(const AsyncFenceManager&) = delete;

    // 创建内部 ID3D12Fence（初值 0）并启动监控线程。
    bool init(ID3D12Device* device, ID3D12CommandQueue* queue);
    bool valid() const { return m_fence != nullptr; }

    // queue->Signal(m_fence, fenceValue)。
    bool signal(UINT64 fenceValue);
    UINT64 completedValue() const;

    void registerCallback(UINT64 fenceValue, FenceCallback callback);
    void registerCallback(UINT64 fenceValue, FenceCallback callback, UINT64 timeoutMs);
    void unregisterCallback(UINT64 fenceValue);
    bool isFenceComplete(UINT64 fenceValue) const;

    // 阻塞等待 fence 达到 fenceValue；timeoutMs==0 表示无限等待。
    bool waitFence(UINT64 fenceValue, UINT64 timeoutMs);

    struct Stats {
        UINT64 totalCallbacks = 0;
        UINT64 pendingCallbacks = 0;
        UINT64 completedCallbacks = 0;
        UINT64 timedOutCallbacks = 0;
        UINT64 avgWaitTimeUs = 0;
    };
    Stats getStats() const;

    // 停止监控线程；未触发的回调以 success=false 收尾。可重复调用。
    void shutdown();

private:
    struct PendingCallback {
        UINT64 fenceValue = 0;
        FenceCallback callback;
        UINT64 timeoutMs = 0;   // 0 = 无超时
        UINT64 startTimeMs = 0;
    };

    void monitorThread();
    void processCompletedFences();
    void wakeMonitor();

    ComPtr<ID3D12Fence> m_fence;
    ComPtr<ID3D12CommandQueue> m_queue;

    std::unordered_map<UINT64, PendingCallback> m_pending;
    mutable std::mutex m_mutex;

    std::thread m_monitorThread;
    std::atomic<bool> m_running{false};
    HANDLE m_wakeEvent = nullptr;  // manual-reset：有新登记时唤醒监控线程

    mutable Stats m_stats;
    mutable std::mutex m_statsMutex;
};

}  // namespace dx12mc
