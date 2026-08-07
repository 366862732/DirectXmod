#pragma once

// 禁用 Windows min/max 宏，避免与 std::max/std::min 冲突（须在任何
// windows.h/d3d12.h 之前定义）。
#ifndef NOMINMAX
#define NOMINMAX
#endif

// dx12-mc 原生 D3D12 层（C++）
// P2 目标：D3D12 设备上下文 + 资源创建（texture/buffer/sampler/view）+ 自检。
// P3 目标：命令层（CommandEncoder 双缓冲提交 + fence 同步 + copy/clear +
//          render pass 生命周期 + timestamp query）。
// 设计参考：官方 com.mojang.blaze3d.vulkan.VulkanDevice / VulkanGpuTexture /
// VulkanGpuBuffer.Direct / VulkanGpuSampler / VulkanGpuTextureView /
// VulkanCommandEncoder / VulkanRenderPass。

#include <d3d12.h>
#include <dxgi1_4.h>
#include <wrl/client.h>

#include <cstdint>
#include <string>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

// ---------------------------------------------------------------------------
// 设备上下文（进程内单例，Java 侧首次调用 dx12CreateDevice 时创建）
// ---------------------------------------------------------------------------
struct DeviceContext {
    ComPtr<ID3D12Device> device;

    ComPtr<ID3D12DescriptorHeap> srvHeap;      // SRV（SHADER_VISIBLE）
    ComPtr<ID3D12DescriptorHeap> rtvHeap;      // RTV
    ComPtr<ID3D12DescriptorHeap> dsvHeap;      // DSV
    ComPtr<ID3D12DescriptorHeap> samplerHeap;  // Sampler（SHADER_VISIBLE）

    UINT srvInc = 0, rtvInc = 0, dsvInc = 0, samplerInc = 0;

    std::string adapterName;
    D3D_FEATURE_LEVEL featureLevel = D3D_FEATURE_LEVEL_11_0;
    UINT64 timestampFrequency = 0;  // 从 GetTimestampFrequency 获取（DeviceInfo.timestampPeriod 用）
    ComPtr<ID3D12CommandQueue> queue;   // 图形队列（提交命令用）
};

// ---------------------------------------------------------------------------
// 资源对象（Java long 句柄 = Dx12Object*）
// ---------------------------------------------------------------------------
struct Dx12Object {
    enum class Kind { Texture, Buffer, Sampler, TextureView };

    Kind kind = Kind::Buffer;
    ComPtr<ID3D12Resource> resource;  // Texture / Buffer
    D3D12_CPU_DESCRIPTOR_HANDLE cpuHandle{};  // View / Sampler 的 CPU 句柄
    D3D12_GPU_DESCRIPTOR_HANDLE gpuHandle{};  // View / Sampler 的 GPU 句柄
    D3D12_HEAP_TYPE heapType = D3D12_HEAP_TYPE_DEFAULT;
    DXGI_FORMAT dxgiFormat = DXGI_FORMAT_UNKNOWN;  // Texture 的格式（copy 计算行距用）
    void* mappedPtr = nullptr;  // map 后的指针
    int usage = 0;
    long long size = 0;
};

// ---------------------------------------------------------------------------
// 设备生命周期
// ---------------------------------------------------------------------------
// 创建/复用全局设备上下文；失败时填充 errorOut 并返回 false。
bool ensureDevice(std::string& errorOut);
void destroyDevice();

// JNI 层访问设备上下文（读 adapterName/featureLevel）。
DeviceContext& deviceContextForJni();

// ---------------------------------------------------------------------------
// 资源创建（返回 new 的 Dx12Object*，句柄 = 指针；失败返回 nullptr + err）
// ---------------------------------------------------------------------------
Dx12Object* createTexture(int usage, int format, int width, int height,
    int depthOrLayers, int mipLevels, std::string& err);
Dx12Object* createBuffer(int usage, long long size, std::string& err);
Dx12Object* createSampler(int addressU, int addressV, int minFilter,
    int magFilter, int maxAnisotropy, float maxLod, std::string& err);
Dx12Object* createTextureView(Dx12Object* texture, int baseMipLevel,
    int mipLevels, std::string& err);
void destroyObject(Dx12Object* obj);

// ---------------------------------------------------------------------------
// Buffer 映射
// ---------------------------------------------------------------------------
// 对应官方 GpuBuffer.map(offset, length, read, write)：校验 usage 位后
// ID3D12Resource::Map，返回连续指针（供 NewDirectByteBuffer 使用）。
void* mapBuffer(Dx12Object* buffer, long long offset, long long length,
    bool read, bool write, std::string& err);
void unmapBuffer(Dx12Object* buffer);

// ---------------------------------------------------------------------------
// 命令层（P3）：CommandContext 对应官方 VulkanCommandEncoder。
// 官方用 timeline semaphore + 双 command pool（MAX_SUBMITS_IN_FLIGHT=2）；
// D3D12 用 ID3D12Fence（timeline 语义）+ 双 command allocator 一一对应：
//   signalSemaphore(idx)      -> queue->Signal(fence, idx)
//   awaitSubmitCompletion(idx)-> fence->GetCompletedValue()/SetEventOnCompletion
//   currentCommandPool().reset-> allocators[fenceValue % 2]->Reset()
// ---------------------------------------------------------------------------
struct CommandContext {
    ComPtr<ID3D12CommandAllocator> allocators[2];   // 每帧飞行一个
    ComPtr<ID3D12GraphicsCommandList> commandList;
    ComPtr<ID3D12Fence> fence;
    UINT64 fenceValue = 0;              // 最近一次 Signal 的值（从 1 开始递增）
    HANDLE fenceEvent = nullptr;        // SetEventOnCompletion 用
    int listOpen = 0;                   // command list 是否已 begin
    int inRenderPass = 0;               // 渲染 pass 是否打开

    ComPtr<ID3D12CommandAllocator>& currentAllocator() {
        return allocators[fenceValue % 2];
    }
};

// 创建命令上下文：2 个 allocator + command list + fence + event。
CommandContext* createCommandEncoder(std::string& err);
void destroyCommandEncoder(CommandContext* ctx);

// 开始录制：Reset 当前 allocator（其对应帧的 GPU 工作必须已完成）+ list。
bool beginCommandList(CommandContext* ctx, std::string& err);
// 结束录制：Close list（之后可提交）。
bool endCommandList(CommandContext* ctx, std::string& err);
// 提交：ExecuteCommandLists + Signal(fence, ++fenceValue) + 等待 fenceValue-2
// 完成（对应官方 awaitSubmitCompletion(currentSubmitIndex - 2)）。
// 返回本次提交的 fence value。
UINT64 submitCommandList(CommandContext* ctx, std::string& err);
// 等待 fence 值达到 value；timeoutNs 为纳秒。返回是否完成。
bool waitForFenceValue(CommandContext* ctx, UINT64 value, UINT64 timeoutNs,
    std::string& err);
// 当前 fence value（Java 侧 createFence 记录用）。
UINT64 currentFenceValue(CommandContext* ctx);

// 立即读 GPU 时间戳（等价官方 getTimestampNow：临时 query + 提交 + 读回）。
long long getTimestampNow(CommandContext* ctx, std::string& err);

// ---------------------------------------------------------------------------
// 命令录制（P3）：copy / clear / render pass / timestamp
// ---------------------------------------------------------------------------
// buffer -> buffer 拷贝（CopyBufferRegion）。
bool copyBufferToBuffer(CommandContext* ctx, Dx12Object* src, long long srcOffset,
    Dx12Object* dst, long long dstOffset, long long size, std::string& err);

// clear 颜色纹理（整纹理，RENDER_ATTACHMENT）。
bool clearColorTexture(CommandContext* ctx, Dx12Object* tex,
    float r, float g, float b, float a, std::string& err);
// clear 深度纹理（整纹理）。
bool clearDepthTexture(CommandContext* ctx, Dx12Object* tex, double depth,
    std::string& err);

// ---------------------------------------------------------------------------
// 纹理拷贝（P3）：CopyTextureRegion 系列
// ---------------------------------------------------------------------------
// staging/通用 buffer -> texture 子区域（writeToTexture / copyBufferToTexture）。
// srcWidth/srcHeight 为源 buffer 的行宽/行高（texel），用于 footprint 计算；
// w/h 为实际拷贝尺寸。
bool copyBufferToTexture(CommandContext* ctx, Dx12Object* srcBuf, long long srcOffset,
    int srcWidth, int srcHeight, Dx12Object* dstTex, int mip, int layer,
    int dstX, int dstY, int w, int h, std::string& err);

// texture 子区域 -> readback buffer（copyTextureToBuffer）。
bool copyTextureToBuffer(CommandContext* ctx, Dx12Object* srcTex, int mip, int layer,
    int srcX, int srcY, int w, int h, Dx12Object* dstBuf, long long dstOffset,
    std::string& err);

// texture -> texture 子区域（copyTextureToTexture）。
bool copyTextureToTexture(CommandContext* ctx, Dx12Object* srcTex, Dx12Object* dstTex,
    int mip, int layer, int srcX, int srcY, int dstX, int dstY, int w, int h,
    std::string& err);

// 设备 timestamp 频率（GetTimestampFrequency；0 = 未知/未初始化）。
unsigned long long getTimestampFrequency();

// 渲染 pass 生命周期（P3：OMSetRenderTargets + viewport/scissor + clear；
// draw 命令录制依赖 P4 pipeline）。
//   colorViews      ：颜色纹理数组（允许空指针占位，对应 withUnusedColorAttachment）
//   colorClearFlags ：0=保留(Load)，1=clear（对应 clearValue 存在）
//   clearColors     ：每附件 4 个 float（r,g,b,a），长度 colorCount*4
//   depthView       ：深度纹理（可为空）
//   depthClearFlag  ：0=Load，1=clear
//   depthClearValue ：深度清除值
//   x/y/w/h         ：renderArea
bool beginRenderPass(CommandContext* ctx, Dx12Object* const* colorViews,
    int colorCount, const int* colorClearFlags, const float* clearColors,
    Dx12Object* depthView, int depthClearFlag, double depthClearValue,
    int x, int y, int w, int h, std::string& err);
bool endRenderPass(CommandContext* ctx, std::string& err);

// ---------------------------------------------------------------------------
// Timestamp query pool（P3）
// ---------------------------------------------------------------------------
struct QueryPool {
    ComPtr<ID3D12QueryHeap> heap;
    int size = 0;
};

QueryPool* createQueryPool(int size, std::string& err);
void destroyQueryPool(QueryPool* pool);
// 录制 EndQuery(TIMESTAMP) 到 pool[index]。
bool writeTimestampToPool(CommandContext* ctx, QueryPool* pool, int index,
    std::string& err);
// 阻塞读回单个/多个 timestamp（等待 GPU 完成）。
bool readQueryValue(QueryPool* pool, int index, long long& out, std::string& err);
bool readQueryValues(QueryPool* pool, int start, int count, long long* out,
    std::string& err);

// ---------------------------------------------------------------------------
// 格式映射（官方 GpuFormat 枚举值 -> DXGI_FORMAT）
// ---------------------------------------------------------------------------
DXGI_FORMAT toDxgiFormat(int gpuFormat);

// 自检：创建 texture/buffer/sampler/view 各一，验证资源层可用后销毁。
// 返回描述字符串（成功/失败明细）。
std::string runResourceSelfTest();

}  // namespace dx12mc
