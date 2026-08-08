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
#include <d3d12sdklayers.h>
#include <d3dcompiler.h>
#include <dxgi1_4.h>
#include <wrl/client.h>

#include <cstdint>
#include <string>
#include <unordered_map>
#include <vector>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

// 毫秒时间戳（QPC），供诊断插桩打印精确阻塞点（渲染线程卡死排查用）。
double nowMs();

// 诊断插桩：带毫秒时间戳打印到 stderr（PCL 启动器写入游戏日志）。
void dbgLog(const char* fmt, ...);

// ---------------------------------------------------------------------------
// 设备上下文（进程内单例，Java 侧首次调用 dx12CreateDevice 时创建）
// ---------------------------------------------------------------------------
struct DeviceContext {
    ComPtr<ID3D12Device> device;

    ComPtr<ID3D12DescriptorHeap> srvHeap;      // SRV（SHADER_VISIBLE）
    // P6：SRV 的 CPU-only 镜像堆（非 SHADER_VISIBLE）。CopyDescriptorsSimple
    // 的源必须是 CPU-only 堆——texture view 的 cpuHandle 指向这里（供
    // pushDescriptors 复制到 drawHeap），srvHeap 的 GPU 句柄仍供直接绑定用。
    ComPtr<ID3D12DescriptorHeap> srvCpuHeap;
    ComPtr<ID3D12DescriptorHeap> rtvHeap;      // RTV
    ComPtr<ID3D12DescriptorHeap> dsvHeap;      // DSV
    ComPtr<ID3D12DescriptorHeap> samplerHeap;  // Sampler（SHADER_VISIBLE）
    ComPtr<ID3D12DescriptorHeap> drawHeap;     // P6：每帧瞬时 CBV/SRV 描述符（SHADER_VISIBLE，ring x2）

    // P6：ExecuteIndirect 用 command signature（DrawIndexedInstanced / DrawInstanced）
    ComPtr<ID3D12CommandSignature> cmdSigIndexed;
    ComPtr<ID3D12CommandSignature> cmdSigNonIndexed;

    UINT srvInc = 0, rtvInc = 0, dsvInc = 0, samplerInc = 0;
    UINT drawInc = 0;  // == srvInc（同属 CBV_SRV_UAV 堆类型）

    std::string adapterName;
    D3D_FEATURE_LEVEL featureLevel = D3D_FEATURE_LEVEL_11_0;
    UINT64 timestampFrequency = 0;  // 从 GetTimestampFrequency 获取（DeviceInfo.timestampPeriod 用）
    ComPtr<ID3D12CommandQueue> queue;   // 图形队列（提交命令用）

    // 全局队列 fence（P6 fence token）：官方 createCommandEncoder() 返回共享
    // encoder，createFence() 的语义是"下一次提交完成后完成"。vanilla 在一次性
    // encoder 上创建 fence token（queueFencedTask / StagedVertexBuffer endFrame /
    // MappableRingBuffer rotate），该 encoder 从不 submit——若用 per-ctx fence 则
    // 永不完成 → 渲染线程永久等待（waitFence: value=1 completed=0 黑屏冻结）。
    // 故用设备级队列 fence 复现官方语义：每次 ExecuteCommandLists 后 Signal
    // (queueFence, ++queueFenceValue)，createFence 目标 = 当前值 + 1。
    ComPtr<ID3D12Fence> queueFence;
    HANDLE queueFenceEvent = nullptr;   // waitForQueueFenceValue 用（每调用新建 event，不共享）
    UINT64 queueFenceValue = 0;         // 每次 ExecuteCommandLists 后递增

    // 诊断（调试层）：设备移除时 GetDeviceRemovedReason + InfoQueue 消息定位根因
    ComPtr<ID3D12InfoQueue> infoQueue;
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
    // P6：描述符堆槽位索引（TextureView → SRV 槽位；Sampler → sampler 槽位；
    // -1 = 未占用）。销毁时归还 free-list 复用，防止长会话 SRV 堆耗尽。
    int descSlot = -1;
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
// D3D12 用 ID3D12Fence（timeline 语义）+ 三 command allocator 一一对应：
//   signalSemaphore(idx)      -> queue->Signal(fence, idx)
//   awaitSubmitCompletion(idx)-> fence->GetCompletedValue()/SetEventOnCompletion
//   currentCommandPool().reset-> allocators[fenceValue % 3]->Reset()
// 三 ring：帧 N 用 index (N-1)%3，帧 N+3 复用同一 index 时 submit 已等 N 完成。
// （双 ring + 等 value-2 会差一帧：帧 N+2 复用帧 N 的 allocator 但只等了 N-1。）
// ---------------------------------------------------------------------------
struct CommandContext {
    ComPtr<ID3D12CommandAllocator> allocators[3];   // 三帧飞行，等待 value-2 后复用
    ComPtr<ID3D12GraphicsCommandList> commandList;
    ComPtr<ID3D12Fence> fence;
    UINT64 fenceValue = 0;              // 最近一次 Signal 的值（从 1 开始递增）
    HANDLE fenceEvent = nullptr;        // SetEventOnCompletion 用
    int listOpen = 0;                   // command list 是否已 begin
    int inRenderPass = 0;               // 渲染 pass 是否打开

    // P11：当前渲染 pass 的活跃附件。endRenderPass 必须把它们从
    // RENDER_TARGET/DEPTH_WRITE 显式回切 COMMON——这两种状态属"非可提升状态"，
    // 命令列表执行完成时【不会】隐式 decay 回 COMMON（只有 COPY_SOURCE/
    // COPY_DEST/UAV 等可提升状态才 decay）。若省略回切，下一 command list 的
    // barrier 按 COMMON 写 Before 状态会与资源实际状态错配（每帧验证 ERROR →
    // GPU 状态错乱 → TDR 冻结：游戏挂死，启动器同 GPU 渲染也卡死）。
    std::vector<Dx12Object*> activeColorTargets;
    Dx12Object* activeDepthTarget = nullptr;

    // P6：本帧 drawHeap 瞬时描述符分配（ring：fenceValue%2 交替两个半区；
    // 帧 N+2 重写帧 N 半区时 submit 已等 N 完成，故 x2 足够）
    UINT drawHeapSlotBase = 0;
    UINT nextDrawSlot = 0;

    // 本 command list 内已过渡的资源状态（资源指针 -> 当前 D3D12 状态）。
    // 初始态 = 资源创建时的状态（texture=COMMON，buffer=initialStateFor）。
    // beginCommandList 清空：因为 submit 同步等待完成，上一 command list
    // 执行结束后所有提升状态已隐式 decay 回 COMMON，故每个新 list 一切资源
    // 都从初始态开始。绝不在 list 内显式回退 COMMON（D3D12 禁止）。
    std::unordered_map<ID3D12Resource*, D3D12_RESOURCE_STATES> resourceState;

    ComPtr<ID3D12CommandAllocator>& currentAllocator() {
        return allocators[fenceValue % 3];
    }
};

// 创建命令上下文：3 个 allocator + command list + fence + event。
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

// ---------------------------------------------------------------------------
// 全局队列 fence（P6 fence token；对应官方共享 encoder 的 submit index）
// ---------------------------------------------------------------------------
// 当前全局队列 fence 值（Java 侧 createFence 记录用：目标 = 当前值 + 1，
// 下一次任意 ctx 的提交完成后达成——官方语义"共享 encoder 的下一次 submit"）。
UINT64 currentQueueFenceValue();
// 等待全局队列 fence 达到 value（createFence token 的 awaitCompletion 用；
// 等待对象是设备级 queueFence，而非 per-ctx fence）。
bool waitForQueueFenceValue(UINT64 value, UINT64 timeoutNs, std::string& err);

// 立即读 GPU 时间戳（等价官方 getTimestampNow：临时 query + 提交 + 读回）。
long long getTimestampNow(CommandContext* ctx, std::string& err);

// ---------------------------------------------------------------------------
// 命令录制（P3）：copy / clear / render pass / timestamp
// ---------------------------------------------------------------------------
// buffer -> buffer 拷贝（CopyBufferRegion）。
bool copyBufferToBuffer(CommandContext* ctx, Dx12Object* src, long long srcOffset,
    Dx12Object* dst, long long dstOffset, long long size, std::string& err);

// 状态追踪的纹理过渡：把 tex 过渡到 to（以本 command list 已跟踪状态为准，
// 绝不回退 COMMON——decay 由命令列表完成时隐式处理）。blitSurface 也要用它
// 把渲染后的源纹理过渡到 COPY_SOURCE。
void transitionTextureTo(CommandContext* ctx, Dx12Object* tex,
    D3D12_RESOURCE_STATES to);

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
// 顶点输入布局专用：RGB32_* 用精确的三分量格式（R32G32B32_*，DXGI 合法
// 输入格式；纹理用的 toDxgiFormat 会把 RGB32 加宽成 RGBA32，输入布局不能加宽）。
DXGI_FORMAT toDxgiVertexFormat(int gpuFormat);

// ---------------------------------------------------------------------------
// 图形管线（P4）：D3DCompile(vs_5_1/ps_5_1) + root signature + 双 PSO
// ---------------------------------------------------------------------------
struct Dx12Pipeline {
    ComPtr<ID3D12RootSignature> rootSignature;
    ComPtr<ID3D12PipelineState> withDepth;     // 总是创建；DSV=D32_FLOAT（镜像官方 depthAttachmentFormat=126）
    ComPtr<ID3D12PipelineState> withoutDepth;  // 仅 depthState==null 时创建；DSV=UNKNOWN
    int topology = 4;  // MC PrimitiveTopology ordinal（setPipeline 时 IASetPrimitiveTopology 用）
};

// Java 侧 dx12CreateGraphicsPipeline 的 desc 解析产物（字段语义见 Dx12Native Javadoc）。
struct PipelineDesc {
    struct ColorTarget {
        int format = 0;            // GpuFormat ordinal；-1 = 未使用槽位
        uint8_t writeMask = 0;
        bool blendEnabled = false;
        uint8_t srcColor = 0, dstColor = 0, colorOp = 0;
        uint8_t srcAlpha = 0, dstAlpha = 0, alphaOp = 0;
    };
    struct InputElement {
        int location = 0, binding = 0, format = 0;
        int offset = 0, stride = 0, stepRate = 0;
        std::string semanticName;  // HLSL 声明的语义名（如 "POSITION"、"TEXCOORD0"）；空串则回退到 "TEXCOORD"
    };
    struct Binding {
        uint8_t type = 0;  // 0=CBV，1=SRV+static sampler，2=SRV
        uint8_t reg = 0;
    };

    std::vector<uint8_t> vsBytes;  // HLSL 源码（vs_5_1 编译）
    std::vector<uint8_t> psBytes;  // HLSL 源码（ps_5_1 编译）
    int colorCount = 0;
    std::vector<ColorTarget> colorTargets;
    bool hasDepth = false;
    int depthFormat = 0;
    bool depthWrite = false;
    int depthCompareOp = 0;
    int topology = 0;       // PrimitiveTopology ordinal
    bool cullEnabled = false;
    int polygonMode = 0;    // PolygonMode ordinal（0=FILL, 1=WIREFRAME）
    std::vector<InputElement> inputElements;
    std::vector<Binding> bindings;  // 按 shader 声明顺序（register = 序号）
};

// 创建管线（D3DCompile + root signature + 双 PSO）；失败返回 nullptr + err。
Dx12Pipeline* createGraphicsPipeline(const PipelineDesc& desc, std::string& err);
void destroyPipeline(Dx12Pipeline* pipeline);

// ---------------------------------------------------------------------------
// Draw 命令录制（P6）：渲染 pass 内的 draw 全链路。
// 对应官方 VulkanRenderPass：setPipeline / bindTexture / setUniform /
// enableScissor / setVertexBuffer / setIndexBuffer / drawIndexed / draw /
// multiDraw* / drawIndexedIndirect / drawIndirect。
// ---------------------------------------------------------------------------
// 绑定图形管线（hasDepth 决定用 withDepth / withoutDepth PSO）。
bool setPipeline(CommandContext* ctx, Dx12Pipeline* pipeline, bool hasDepth,
    std::string& err);
// RSSetScissorRects(1)（x,y,w,h）。
bool setScissor(CommandContext* ctx, int x, int y, int w, int h, std::string& err);
// IASetVertexBuffers：slot 0..15；stride 来自管线的 vertex format。
bool setVertexBuffer(CommandContext* ctx, int slot, Dx12Object* buffer,
    long long offset, int stride, std::string& err);
// IASetIndexBuffer（indexType：0=SHORT(R16_UINT)，1=INT(R32_UINT)）。
bool setIndexBuffer(CommandContext* ctx, Dx12Object* buffer, int indexType,
    std::string& err);
// 瞬时描述符绑定（对应官方 pushDescriptors）：
//   type 0 = CBV（buffer + offset + length，offset 须 256 对齐）
//   type 1 = SRV（复制 texture view 的现有描述符）
//   type 2 = SRV（texel buffer，按 texelFormat 建 Buffer SRV）
struct DrawBinding {
    uint8_t type = 0;
    Dx12Object* buffer = nullptr;   // CBV / TEXEL 的 buffer
    long long offset = 0;           // CBV offset
    long long length = 0;           // CBV length（内部向上取整 256）
    int texelFormat = 0;            // TEXEL SRV 的 GpuFormat ordinal
    Dx12Object* view = nullptr;     // SAMPLED_IMAGE 的 texture view
};
// 把 bindings 写入本帧 drawHeap 瞬时槽位并 SetGraphicsRootDescriptorTable(0)。
bool pushDescriptors(CommandContext* ctx, const std::vector<DrawBinding>& bindings,
    std::string& err);

bool drawIndexedInstanced(CommandContext* ctx, UINT indexCount, UINT instanceCount,
    INT startIndexLocation, INT baseVertexLocation, UINT startInstanceLocation,
    std::string& err);
bool drawInstanced(CommandContext* ctx, UINT vertexCount, UINT instanceCount,
    UINT firstVertex, UINT startInstanceLocation, std::string& err);
// ExecuteIndirect（DrawIndexedInstanced / DrawInstanced command signature）。
bool drawIndexedIndirect(CommandContext* ctx, Dx12Object* commands, long long offset,
    UINT drawCount, std::string& err);
bool drawIndirect(CommandContext* ctx, Dx12Object* commands, long long offset,
    UINT drawCount, std::string& err);

// ---------------------------------------------------------------------------
// Surface（P5）：DXGI swapchain（镜像官方 VulkanGpuSurface）。
// PresentMode 序数 = 官方枚举 ordinal：IMMEDIATE=0, MAILBOX=1, FIFO=2,
// FIFO_RELAXED=3。DXGI FLIP 模型支持 {0, 2, 3}（MAILBOX 无直接对应）。
// ---------------------------------------------------------------------------
constexpr UINT kSurfaceBufferCount = 3;  // 镜像官方 minImageCount max(3, min)

struct Dx12Surface {
    ComPtr<IDXGISwapChain3> swapChain;
    UINT width = 1;
    UINT height = 1;
    DXGI_FORMAT format = DXGI_FORMAT_R8G8B8A8_UNORM;
    int presentMode = 2;               // 默认 FIFO
    int currentImageIndex = -1;        // 最近 acquire 的 back buffer
    // P6 诊断：最近一次 blit 写入的 back buffer 下标。present 后 currentImageIndex
    // 被重置为 -1（防 ResizeBuffers 误判），而 readback 需要读"本帧已 blit 的
    // buffer"才能看到真实画面——用 GetCurrentBackBufferIndex() 兜底会读到下一帧
    // 尚未写入的 buffer（全 0 假黑屏）。blit 成功时记录此处。
    int lastBlitIndex = -1;
    bool suboptimal = false;
    std::vector<ComPtr<ID3D12Resource>> backBuffers;
    std::vector<D3D12_CPU_DESCRIPTOR_HANDLE> rtvHandles;  // 由 rtvHeap 分配
};

// 从 rtvHeap 分配一个 RTV CPU 句柄（surface 的 back buffer 用）。
D3D12_CPU_DESCRIPTOR_HANDLE allocRtvHandle(std::string& err);

// 创建 swapchain（FLIP_DISCARD + ALLOW_TEARING，1x1 占位，configure 时 ResizeBuffers）。
Dx12Surface* createSurface(uintptr_t hwnd, std::string& err);
// 取该 surface 支持的 present modes（官方枚举序数数组，MAILBOX 不支持）。
std::vector<int> surfacePresentModes();
// ResizeBuffers + 重新取 back buffers + 重建 RTV。
bool configureSurface(Dx12Surface* s, int width, int height, int presentMode,
    std::string& err);
// 获取当前 back buffer index（DXGI FLIP 模型下 Present 内部同步）。
bool acquireSurface(Dx12Surface* s, std::string& err);
// 录制 blit（源纹理 -> back buffer）：在 command list 录制状态下调用；
// 源与目标格式须一致（RGBA8/BGRA8 族可直接拷贝）。返回 false 表示未录制。
bool blitSurface(CommandContext* ctx, Dx12Surface* s, Dx12Object* srcTex,
    std::string& err);
// Present(interval, flags)：FIFO=Present(1,0)；IMMEDIATE=Present(0,0)；
// FIFO_RELAXED=Present(1,ALLOW_TEARING)。
void presentSurface(Dx12Surface* s);
void destroySurface(Dx12Surface* s);
// P6 诊断：把当前 back buffer 读回 CPU 并打印 3x3 采样点 RGBA（每 ~60 帧调用
// 一次，内部先等 GPU 空闲；用于确认画面实际颜色/内容——纯色=渲染未生效）。
bool readbackSurfacePixels(Dx12Surface* s, std::string& err);
// P6 诊断：读回任意纹理 3x3 采样点 RGBA（tag 标注来源，如 blit 源=渲染目标，
// 用于区分 draw 未写入 vs blit 丢失）。
void dbgReadbackTexturePixels(Dx12Object* tex, const char* tag);
// P6 诊断：把 RGBA8 像素数据保存为 BMP 文件（32bpp，覆盖写当前工作目录
// dx12_dump_<tag>.bmp）并在日志打印 ASCII 缩略图（亮度字符 + 色相字母）。
// 用于把画面完整可视化，定位 GUI 元素绘制目标是否与 blit 源一致。
void dbgDumpPixelsToFile(const uint8_t* rgba, UINT w, UINT h, UINT64 pitch,
    const char* tag);
// P6 诊断：读回 buffer 内容（前若干 float + hex），UPLOAD 直接 Map、DEFAULT 经
// readback staging；用于确认顶点/UBO 数据是否真正写入 GPU 资源。
void dbgReadbackBufferBytes(Dx12Object* buf, long long offset, int len, const char* tag);

// 阻塞等待 GPU 队列上所有已提交命令执行完成（销毁 swapchain/设备前调用，
// 避免 backbuffer 等资源在被 GPU 使用时释放导致 DXGI_ERROR_DEVICE_REMOVED）。
bool deviceWaitIdle(std::string& err);

// 自检：创建 texture/buffer/sampler/view 各一，验证资源层可用后销毁。
// 返回描述字符串（成功/失败明细）。
std::string runResourceSelfTest();

}  // namespace dx12mc
