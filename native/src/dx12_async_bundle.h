#pragma once

// dx12-mc 异步渲染基础设施（P33）：Bundle 录制器池
//
// 每个 worker 独占一个 D3D12_COMMAND_LIST_TYPE_BUNDLE 的 allocator + command
// list，负责把一段“同一 pass + 同一 PSO + 同一 scissor”的绘制录制进 bundle。
//
// D3D12 bundle 限制（本类严格不调用以下被禁止的 API）：
//   - 任何 Clear（ClearRenderTargetView / ClearDepthStencilView / ClearUnorderedAccessView*）
//   - 任何 Copy（CopyBufferRegion / CopyTextureRegion / CopyResource / CopyDescriptors*）
//   - ResourceBarrier / ResolveSubresource / BeginQuery / EndQuery
//   - OMSetRenderTargets / RSSetViewports / RSSetScissorRects
//   - ExecuteBundle / render pass begin/end / SetDescriptorHeaps（须由父列表负责）
// bundle 内允许：SetPipelineState、SetGraphicsRootSignature、
// SetGraphicsRootDescriptorTable、IASetVertexBuffers/IASetIndexBuffer/
// IASetPrimitiveTopology、DrawInstanced/DrawIndexedInstanced、ExecuteIndirect。
//
// 注意：bundle 内的 PSO 必须与父列表在录制时的 RT 格式 / 深度格式 / 采样数一致，
// 因此调用方需按“同一 pass + 同一 PSO + 同一 scissor”切段后再 begin/end。

#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <d3d12.h>
#include <wrl/client.h>

#include <cstdint>
#include <memory>
#include <vector>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

// 每个 worker 持有的 (allocator + bundle list) 帧槽数。命令 allocator 只能在其
// 对应的 GPU 工作完成后才能 Reset，因此按帧槽（fenceValue % kBundleFrameSlots）
// 轮转，保证复用某槽时其 N-kBundleFrameSlots 帧前的提交已完成。
constexpr UINT kBundleFrameSlots = 4;

// MC PrimitiveTopology ordinal -> 命令列表级拓扑（bundle 内使用）。
D3D12_PRIMITIVE_TOPOLOGY bundlePrimitiveTopology(int ordinal);

class BundleRecorder {
public:
    BundleRecorder() = default;
    ~BundleRecorder() = default;

    BundleRecorder(const BundleRecorder&) = delete;
    BundleRecorder& operator=(const BundleRecorder&) = delete;

    // 创建 kBundleFrameSlots 组 BUNDLE allocator + command list（初始为 closed）。
    // 现有 ExecuteIndirect 用的 command signature 从 DeviceContext 读取。
    bool init(ID3D12Device* device, UINT workerId);

    bool valid() const { return m_bundle != nullptr; }
    UINT workerId() const { return m_workerId; }
    bool isRecording() const { return m_recording; }
    ID3D12GraphicsCommandList* commandList() const { return m_bundle.Get(); }

    // 开始 / 结束录制。frameSlot 选择本帧使用的 allocator/list 槽（须为
    // fenceValue % kBundleFrameSlots，保证该槽的 GPU 工作已完成）。
    // end() 成功返回 bundle 命令列表句柄，失败返回 nullptr。
    bool begin(UINT frameSlot);
    ID3D12GraphicsCommandList* end();

    // ---- 录制命令（仅 bundle 允许的子集）----
    void setRootSignature(ID3D12RootSignature* rootSignature);
    void setPipelineState(ID3D12PipelineState* pso);
    void setDescriptorTable(UINT slot, D3D12_GPU_DESCRIPTOR_HANDLE handle);
    void setVertexBuffers(UINT startSlot, UINT count,
        const D3D12_VERTEX_BUFFER_VIEW* views);
    void setIndexBuffer(const D3D12_INDEX_BUFFER_VIEW* view);
    void setPrimitiveTopology(D3D12_PRIMITIVE_TOPOLOGY topology);
    void drawIndexedInstanced(UINT indexCount, UINT instanceCount,
        UINT startIndexLocation, INT baseVertexLocation, UINT startInstanceLocation);
    void drawInstanced(UINT vertexCount, UINT instanceCount,
        UINT startVertexLocation, UINT startInstanceLocation);
    // ExecuteIndirect：drawIndexedIndirect 使用现有 cmdSigIndexed，
    // 若签名为空直接返回 false（绝不向 ExecuteIndirect 传 nullptr）。
    bool drawIndexedIndirect(ID3D12Resource* commands, UINT64 offset, UINT drawCount);
    bool drawIndirect(ID3D12Resource* commands, UINT64 offset, UINT drawCount);

    struct Stats {
        UINT64 commandsRecorded = 0;
        UINT64 drawCalls = 0;
        UINT64 indexCount = 0;
        UINT64 vertexCount = 0;
        UINT64 descriptorUpdates = 0;
    };
    Stats stats() const { return m_stats; }
    void resetStats() { m_stats = Stats{}; }

private:
    // 按帧槽轮转，避免 Reset 正在被 GPU 读取的 allocator（详见 kBundleFrameSlots）。
    ComPtr<ID3D12CommandAllocator> m_allocators[kBundleFrameSlots];
    ComPtr<ID3D12GraphicsCommandList> m_lists[kBundleFrameSlots];
    // 当前录制使用的槽位与其命令列表（begin 时指向 m_lists[m_frameSlot]）。
    UINT m_frameSlot = 0;
    ComPtr<ID3D12GraphicsCommandList> m_bundle;
    ComPtr<ID3D12CommandSignature> m_cmdSigIndexed;     // = DeviceContext::cmdSigIndexed
    ComPtr<ID3D12CommandSignature> m_cmdSigNonIndexed;  // = DeviceContext::cmdSigNonIndexed

    ID3D12Device* m_device = nullptr;
    UINT m_workerId = 0;
    bool m_recording = false;

    // 状态去重（减少冗余录制命令）。
    ID3D12RootSignature* m_rootSig = nullptr;
    ID3D12PipelineState* m_pso = nullptr;
    D3D12_PRIMITIVE_TOPOLOGY m_topology = D3D_PRIMITIVE_TOPOLOGY_UNDEFINED;

    Stats m_stats;
};

// Bundle 录制器池：每个 worker 一个 recorder。
class BundleRecorderPool {
public:
    BundleRecorderPool() = default;
    ~BundleRecorderPool() = default;

    BundleRecorderPool(const BundleRecorderPool&) = delete;
    BundleRecorderPool& operator=(const BundleRecorderPool&) = delete;

    bool init(ID3D12Device* device, UINT workerCount);
    void shutdown();

    bool valid() const { return !m_recorders.empty(); }
    UINT workerCount() const { return (UINT)m_recorders.size(); }

    // worker 越界返回 nullptr。
    BundleRecorder* recorder(UINT worker);

private:
    std::vector<std::unique_ptr<BundleRecorder>> m_recorders;
};

// 在父（DIRECT）命令列表上执行 bundle；mainList 或 bundle 为空时静默跳过。
void executeBundle(ID3D12GraphicsCommandList* mainList,
    ID3D12GraphicsCommandList* bundle);

}  // namespace dx12mc
