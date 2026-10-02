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
//   - ExecuteBundle / render pass begin/end / 二次 SetDescriptorHeaps
//     （bundle 不继承父列表的描述符堆；本类在 begin() 内统一 SetDescriptorHeaps 一次，
//      因为会录制 SetGraphicsRootDescriptorTable；父列表负责在别处的堆设置）
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

// 单个帧槽内允许的并行 bundle 批次数。同一帧里会对同一命令列表多次调用
// drawMultipleIndexed（地形按 layer / draw group 分批），而已经 ExecuteBundle 进
// 父命令列表的 bundle，在其父列表提交并执行完成前不能 Reset 其 allocator。
// 因此同一帧内的第 b 批使用 (frameSlot, b) 对应的独立槽位；b 超过本上限时
// begin() 返回 false，调用方回退串行录制（功能正确，仅少了这段并行）。
constexpr UINT kBundleBatchesPerFrame = 16;
// (allocator, list) 槽位总数。
constexpr UINT kBundleSlotCount = kBundleFrameSlots * kBundleBatchesPerFrame;

// MC PrimitiveTopology ordinal -> 命令列表级拓扑（bundle 内使用）。
D3D12_PRIMITIVE_TOPOLOGY bundlePrimitiveTopology(int ordinal);

// 前向声明：录制时记录所属管线，用于与同步路径同样规则校正顶点步长
// （Dx12Pipeline::vertexStrides 由 inputElements 实际格式推算，覆盖 Java 侧
// getVertexSize() 的错误值）。
struct Dx12Pipeline;

class BundleRecorder {
public:
    BundleRecorder() = default;
    ~BundleRecorder() = default;

    BundleRecorder(const BundleRecorder&) = delete;
    BundleRecorder& operator=(const BundleRecorder&) = delete;

    // 创建 kBundleSlotCount 组 BUNDLE allocator + command list（初始为 closed）。
    // 现有 ExecuteIndirect 用的 command signature 从 DeviceContext 读取。
    // heap 为 DeviceContext::drawHeap（shader-visible CBV_SRV_UAV，非拥有）；
    // bundle 内若要编辑描述符表（SetGraphicsRootDescriptorTable），必须自行
    // SetDescriptorHeaps 一次，否则运行时解析 GPU 句柄时空堆指针解引用崩溃。
    bool init(ID3D12Device* device, ID3D12DescriptorHeap* heap, UINT workerId);

    bool valid() const { return m_bundle != nullptr; }
    UINT workerId() const { return m_workerId; }
    bool isRecording() const { return m_recording; }
    ID3D12GraphicsCommandList* commandList() const { return m_bundle.Get(); }

    // 开始 / 结束录制。frameSlot 须为 fenceValue % kBundleFrameSlots；frameValue 为
    // 原始帧号（fenceValue），用于判定「同一帧内的第几批」，从而为每一批选一个独立的
    // allocator 槽位（已 ExecuteBundle 的 bundle 在父列表执行完成前不能被 Reset）。
    // 同一帧内批次数超过 kBundleBatchesPerFrame 时返回 false（调用方回退串行）。
    // end() 成功返回 bundle 命令列表句柄，失败返回 nullptr。
    bool begin(UINT frameSlot, UINT64 frameValue);
    ID3D12GraphicsCommandList* end();

    // ---- 录制命令（仅 bundle 允许的子集）----
    void setRootSignature(ID3D12RootSignature* rootSignature);
    void setPipelineState(ID3D12PipelineState* pso);
    // 记录本 bundle 所属管线；用于 correctedStride（与同步路径 setVertexBuffer 一致）。
    void setPipeline(const Dx12Pipeline* pipeline);
    // 按所属管线的修正 stride 覆盖 slot 的顶点步长；无记录时返回 0（调用方回退传入值）。
    UINT correctedStride(int slot) const;
    void setDescriptorTable(UINT slot, D3D12_GPU_DESCRIPTOR_HANDLE handle);
    // B：CBV 的 root descriptor（地址烘焙进命令列表，不经过描述符堆）。
    void setRootConstantBufferView(UINT rootIndex, D3D12_GPU_VIRTUAL_ADDRESS address);
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
    // 按 (帧槽, 帧内批次序号) 轮转，避免 Reset 正在被 GPU 读取的 allocator
    // （详见 kBundleFrameSlots / kBundleBatchesPerFrame）。
    ComPtr<ID3D12CommandAllocator> m_allocators[kBundleSlotCount];
    ComPtr<ID3D12GraphicsCommandList> m_lists[kBundleSlotCount];
    // 当前录制使用的槽位与其命令列表（begin 时指向 m_lists[m_activeSlot]）。
    UINT m_activeSlot = 0;
    // 每个帧槽最近一次录制所属的帧号，以及该帧内已用批次数；帧号变化时批次归零。
    UINT64 m_frameOfSlot[kBundleFrameSlots] = {};
    UINT m_batchOfSlot[kBundleFrameSlots] = {};
    ComPtr<ID3D12GraphicsCommandList> m_bundle;
    ComPtr<ID3D12CommandSignature> m_cmdSigIndexed;     // = DeviceContext::cmdSigIndexed
    ComPtr<ID3D12CommandSignature> m_cmdSigNonIndexed;  // = DeviceContext::cmdSigNonIndexed
    // 本 bundle 内 SetGraphicsRootDescriptorTable 引用的 shader-visible 堆。
    // 必须与父命令列表执行 bundle 时绑定的堆一致，否则行为未定义。
    ComPtr<ID3D12DescriptorHeap> m_heap;

    ID3D12Device* m_device = nullptr;
    UINT m_workerId = 0;
    bool m_recording = false;

    // 状态去重（减少冗余录制命令）。
    ID3D12RootSignature* m_rootSig = nullptr;
    ID3D12PipelineState* m_pso = nullptr;
    D3D12_PRIMITIVE_TOPOLOGY m_topology = D3D_PRIMITIVE_TOPOLOGY_UNDEFINED;
    // 本 bundle 所属管线（非拥有，随 DeviceContext 的管线缓存存活）；begin 时清空。
    const Dx12Pipeline* m_pipeline = nullptr;

    Stats m_stats;
};

// Bundle 录制器池：每个 worker 一个 recorder。
class BundleRecorderPool {
public:
    BundleRecorderPool() = default;
    ~BundleRecorderPool() = default;

    BundleRecorderPool(const BundleRecorderPool&) = delete;
    BundleRecorderPool& operator=(const BundleRecorderPool&) = delete;

    bool init(ID3D12Device* device, ID3D12DescriptorHeap* heap, UINT workerCount);
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
