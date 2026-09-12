#include "dx12_async_fence.h"

#include <chrono>
#include <vector>

namespace dx12mc {

namespace {

UINT64 nowMsSteady() {
    using namespace std::chrono;
    return (UINT64)duration_cast<milliseconds>(
        steady_clock::now().time_since_epoch()).count();
}

}  // namespace

AsyncFenceManager::~AsyncFenceManager() {
    shutdown();
}

bool AsyncFenceManager::init(ID3D12Device* device, ID3D12CommandQueue* queue) {
    if (!device || !queue) return false;
    if (m_running.load()) return true;  // 已初始化

    m_queue = queue;
    if (FAILED(device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&m_fence)))) {
        return false;
    }
    // manual-reset：登记新回调时唤醒监控线程。
    m_wakeEvent = CreateEventW(nullptr, TRUE, FALSE, nullptr);
    if (!m_wakeEvent) {
        m_fence.Reset();
        m_queue.Reset();
        return false;
    }
    m_running.store(true);
    m_monitorThread = std::thread(&AsyncFenceManager::monitorThread, this);
    return true;
}

bool AsyncFenceManager::signal(UINT64 fenceValue) {
    if (!m_fence || !m_queue) return false;
    return SUCCEEDED(m_queue->Signal(m_fence.Get(), fenceValue));
}

UINT64 AsyncFenceManager::completedValue() const {
    return m_fence ? m_fence->GetCompletedValue() : 0;
}

bool AsyncFenceManager::isFenceComplete(UINT64 fenceValue) const {
    return m_fence && m_fence->GetCompletedValue() >= fenceValue;
}

void AsyncFenceManager::wakeMonitor() {
    if (m_wakeEvent) SetEvent(m_wakeEvent);
}

void AsyncFenceManager::registerCallback(UINT64 fenceValue, FenceCallback callback) {
    registerCallback(fenceValue, std::move(callback), 0);
}

void AsyncFenceManager::registerCallback(UINT64 fenceValue, FenceCallback callback,
    UINT64 timeoutMs) {
    if (!callback) return;

    {
        std::lock_guard<std::mutex> lk(m_statsMutex);
        ++m_stats.totalCallbacks;
    }

    // 已完成：立即回调（不进入监控线程）。
    if (isFenceComplete(fenceValue)) {
        {
            std::lock_guard<std::mutex> lk(m_statsMutex);
            ++m_stats.completedCallbacks;
        }
        callback(fenceValue, true);
        return;
    }
    // 未运行（未 init 或已 shutdown）：以失败收尾。
    if (!m_running.load()) {
        callback(fenceValue, false);
        return;
    }

    {
        std::lock_guard<std::mutex> lk(m_mutex);
        PendingCallback p;
        p.fenceValue = fenceValue;
        p.callback = std::move(callback);
        p.timeoutMs = timeoutMs;
        p.startTimeMs = nowMsSteady();
        m_pending[fenceValue] = std::move(p);
    }
    wakeMonitor();
}

void AsyncFenceManager::unregisterCallback(UINT64 fenceValue) {
    std::lock_guard<std::mutex> lk(m_mutex);
    m_pending.erase(fenceValue);
}

bool AsyncFenceManager::waitFence(UINT64 fenceValue, UINT64 timeoutMs) {
    if (!m_fence) return false;
    if (isFenceComplete(fenceValue)) return true;

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

void AsyncFenceManager::processCompletedFences() {
    struct Completed {
        UINT64 value = 0;
        FenceCallback callback;
        bool success = false;
        UINT64 waitedUs = 0;
    };
    std::vector<Completed> toInvoke;

    {
        std::lock_guard<std::mutex> lk(m_mutex);
        const UINT64 completed = m_fence ? m_fence->GetCompletedValue() : 0;
        const UINT64 now = nowMsSteady();
        for (auto it = m_pending.begin(); it != m_pending.end();) {
            const bool done = completed >= it->first;
            const bool timedOut = it->second.timeoutMs > 0
                && (now - it->second.startTimeMs) >= it->second.timeoutMs;
            if (done || timedOut) {
                Completed c;
                c.value = it->first;
                c.callback = std::move(it->second.callback);
                c.success = done;
                c.waitedUs = (now - it->second.startTimeMs) * 1000ULL;
                toInvoke.push_back(std::move(c));
                it = m_pending.erase(it);
            } else {
                ++it;
            }
        }
    }

    if (!toInvoke.empty()) {
        std::lock_guard<std::mutex> lk(m_statsMutex);
        for (const Completed& c : toInvoke) {
            ++m_stats.completedCallbacks;
            if (!c.success) ++m_stats.timedOutCallbacks;
            m_stats.avgWaitTimeUs = (m_stats.avgWaitTimeUs + c.waitedUs) / 2;
        }
    }
    // 回调在锁外调用：允许回调内再次 register/unregister，避免自锁死。
    for (Completed& c : toInvoke) {
        if (c.callback) c.callback(c.value, c.success);
    }
}

void AsyncFenceManager::monitorThread() {
    SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);

    while (m_running.load()) {
        // 找最早的待完成 fence value。
        UINT64 minValue = UINT64_MAX;
        {
            std::lock_guard<std::mutex> lk(m_mutex);
            for (const auto& p : m_pending) {
                if (p.first < minValue) minValue = p.first;
            }
        }

        if (minValue == UINT64_MAX) {
            // 无待处理回调：睡到有新登记（wakeEvent）或超时再检查。
            WaitForSingleObject(m_wakeEvent, 100);
            ResetEvent(m_wakeEvent);
            continue;
        }

        HANDLE ev = CreateEventW(nullptr, FALSE, FALSE, nullptr);
        if (!ev) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            continue;
        }
        if (FAILED(m_fence->SetEventOnCompletion(minValue, ev))) {
            CloseHandle(ev);
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            continue;
        }

        // 精准等待：fence 完成 或 有新登记 或 超时（用于重算最早值/处理超时）。
        HANDLE handles[2] = { ev, m_wakeEvent };
        DWORD r = WaitForMultipleObjects(2, handles, FALSE, 100);
        CloseHandle(ev);

        if (r == WAIT_OBJECT_0) {
            processCompletedFences();
        } else if (r == WAIT_OBJECT_0 + 1) {
            ResetEvent(m_wakeEvent);
        }
        // WAIT_TIMEOUT：直接下一轮重算（可能处理超时回调）。
    }
}

AsyncFenceManager::Stats AsyncFenceManager::getStats() const {
    Stats s;
    {
        std::lock_guard<std::mutex> lk(m_statsMutex);
        s = m_stats;
    }
    {
        std::lock_guard<std::mutex> lk(m_mutex);
        s.pendingCallbacks = m_pending.size();
    }
    return s;
}

void AsyncFenceManager::shutdown() {
    if (!m_running.exchange(false)) {
        if (m_monitorThread.joinable()) m_monitorThread.join();
        return;
    }
    wakeMonitor();
    if (m_monitorThread.joinable()) m_monitorThread.join();

    std::vector<PendingCallback> remaining;
    {
        std::lock_guard<std::mutex> lk(m_mutex);
        remaining.reserve(m_pending.size());
        for (auto& p : m_pending) remaining.push_back(std::move(p.second));
        m_pending.clear();
    }
    for (PendingCallback& p : remaining) {
        if (p.callback) p.callback(p.fenceValue, false);
    }

    if (m_wakeEvent) {
        CloseHandle(m_wakeEvent);
        m_wakeEvent = nullptr;
    }
    m_fence.Reset();
    m_queue.Reset();
}

}  // namespace dx12mc
