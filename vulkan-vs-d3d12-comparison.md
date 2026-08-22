# 官方 Vulkan 实现 vs 我们 D3D12 实现 差异分析

> 基准：Minecraft 26.2 官方反编译源码 `com.mojang.blaze3d.vulkan.*`
> 对比目标：我们的 D3D12 实现 `com.dx12.dx12.*`
> 分析日期：2026-08-21

---

## 一、总体架构对比

| 维度 | 官方 Vulkan | 我们的 D3D12 |
|------|-------------|--------------|
| **设备抽象** | `VulkanInstance` + `VulkanPhysicalDevice` + `VkDevice` 三层 | `dx12_mc.dll` 内部封装，Java 层只持有 `handle` |
| **内存管理** | VMA（Vulkan Memory Allocator）统一管理 | 无系统级内存分配器，`TransiantMemory` 自实现 |
| **队列模型** | `VulkanQueue`（graphics/compute/transfer 三个独立 Queue） | 单一 queue（D3D12 同 queue family 处理所有操作） |
| **Command Pool** | `VulkanCommandPool[2]`（双缓冲 CommandPool） | 无显式 CommandPool，由 DLL 内部管理 |
| **提交模型** | `VK_KHR_synchronization2`（VkSubmitInfo2 + semaphore stages） | 直接 `ExecuteAndSubmitCommandLists`（无 stage mask 细化） |
| **Fence/Semaphore** | timeline semaphore（`VkSemaphoreTypeCreateInfo{type=1}`） | 二进制 fence + `globalQueueFenceValue` 计数器 |
| **错误处理** | `VulkanUtils.crashIfFailure()` + 详细 message | `BackendCreationException`（自测阶段）/ `IllegalStateException`（运行时） |
| **诊断插桩** | `CheckpointExtension`（AMD/NVIDIA vendor-specific） | 无 vendor checkpoint，只有 Java 侧 `System.err.println` |

---

## 二、逐模块差异详解

### 2.1 GpuBackend / Backend 初始化

#### 官方 Vulkan（VulkanBackend.java）
```java
// 支持多个 GPU，按优先级选择 discrete GPU
private static VulkanPhysicalDevice findPhysicalDevice(VulkanInstance instance) {
    // 遍历所有物理设备
    // 1. 过滤：API >= 1.1, 有 graphics queue, 有 required extensions/features
    // 2. 跳过已知问题设备（deviceUUID 黑名单）
    // 3. 优先选择 discrete GPU（非 integrated）
    // 4. firstDevice 兜底
}

// 设备能力要求非常严格
Set<String> REQUIRED_DEVICE_EXTENSIONS = Set.of(
    "VK_KHR_dynamic_rendering", "VK_KHR_push_descriptor",
    "VK_KHR_synchronization2", "VK_EXT_vertex_attribute_divisor",
    "VK_KHR_swapchain"
);
Set<VulkanFeature> REQUIRED_DEVICE_FEATURES = Set.of(
    multiDrawIndirect, fillModeNonSolid, samplerAnisotropy,
    shaderDrawParameters, timelineSemaphore, hostQueryReset,
    synchronization2, dynamicRendering, vertexAttributeInstanceRateDivisor
);
// 可选扩展：VK_KHR_portability_subset, VK_AMD_buffer_marker, VK_NV_device_diagnostic_checkpoints, VK_EXT_multi_draw
```

#### 我们的 D3D12（Dx12Backend.java / dx12_device.cpp）
```cpp
// D3D12EnumAdapters 遍历所有适配器，选择第一个 D3D_FEATURE_LEVEL >= 11_0 的
// 无 discrete GPU 优先级，无已知问题设备黑名单
// 无 optional extension 检测（D3D12 没有 analogous 概念）
```

**差异点：**
- **无 vendor checkpoint extension**：官方检测 AMD/NVIDIA checkpoint 扩展并在 pipeline 中记录关键 checkpoint，我们的实现完全没有等价物。这意味着 GPU timeout 时无法精确定位是哪一步崩溃。
- **无 device 黑名单**：官方有 `VulkanUtils.KNOWN_PROBLEMATIC_DEVICES` 列表，我们无此机制。
- **无 device 类型优先级**：官方偏好 discrete GPU，我们取第一个可用适配器。

---

### 2.2 GpuDeviceBackend / Device

#### 官方 Vulkan（VulkanDevice.java）
```java
// 三个独立 Queue
private final VulkanQueue graphicsQueue;
private final VulkanQueue computeQueue;
private final VulkanQueue transferQueue;

// VMA 内存分配器
private final long vma;

// 完整的 DeviceInfo
private final DeviceInfo deviceInfo;
//   - maxSamplerAnisotropy
//   - minUniformBufferOffsetAlignment
//   - maxImageDimension2D
//   - maxMemoryAllocationSize
//   - maxMultiDrawCount
//   - maxColorAttachments
//   - timestampPeriod
//   - deviceType

// Shader 编译使用官方 GlslCompiler
private final GlslCompiler glslCompiler = new GlslCompiler();

// 双缓冲 CommandEncoder（2 个 CommandPool）
private final VulkanCommandEncoder commandEncoder;
```

#### 我们的 D3D12（Dx12Device.java）
```java
// 单一 queue（隐含在 dll 内）
// 无 VMA，自实现 TransientMemory
private final Dx12TransientMemory transientMemory;

// DeviceInfo 只有 name + featureLevel
// 缺少 anisotropy, alignment, maxDim, maxAllocSize, multiDrawCount 等

// Shader 编译使用自己的 Dx12ShaderCompiler（SPIR-V→HLSL 而非 SPIR-V→DXBC）
private final Dx12ShaderCompiler shaderCompiler;

// 共享单例 CommandEncoder（不是双缓冲 CommandPool 模型）
private Dx12CommandEncoderBackend sharedCommandEncoder;
```

**差异点：**
- **DeviceInfo 不完整**：官方 `DeviceInfo` 包含 6 个硬件限制参数，我们的实现只传了 name。这导致 Minecraft 无法查询各设备上限（如 `maxSamplerAnisotropy` 用于 AA 配置）。
- **无多 Queue 概念**：D3D12 单 queue 处理所有操作是合理的（对应官方 `graphicsQueue`），但官方还有独立的 `computeQueue` 和 `transferQueue`，可并行执行计算/拷贝与渲染。
- **ShaderCompiler 架构不同**：官方直接在 Java 侧完成 SPIR-V → ShaderModule 转换；我们额外引入 HLSL 中间层（spvc），增加了复杂度但也提供了跨 API 的 shader 源可读性。
- **无 Vendor Checkpoint**：见 2.1。

---

### 2.3 CommandEncoder

#### 官方 Vulkan（VulkanCommandEncoder.java）
```java
public static final int MAX_SUBMITS_IN_FLIGHT = 2;
private long currentSubmitIndex = 2L;       // timeline semaphore 值
private long completedSubmitIndex = 0L;     // 已完成的 submit 索引
private final long submitSemaphore;         // timeline semaphore

// 双缓冲 CommandPool
private final VulkanCommandPool[] commandPools = new VulkanCommandPool[2];

// 销毁队列（延迟清理）
private final DestructionQueue<Destroyable> destroyQueue = new DestructionQueue<>(2, Destroyable::destroy);

// 临时命令 buffer（用于 copy/clear 等非 render pass 操作）
private @Nullable VkCommandBuffer currentCommandBuffer;

public void submit() {
    this.endCommandBuffer();
    this.transientMemory.endSubmit();
    // signal timeline semaphore
    this.signalSemaphore(this.submitSemaphore, this.currentSubmitIndex, PIPELINE_STAGE_GRAPHICS);
    this.submissionBuilder.close();
    this.submissionBuilder = this.device.graphicsQueue().beginSubmit();
    ++this.currentSubmitIndex;
    // 等待 2 帧前的 submit 完成（双缓冲流水线）
    if (!this.awaitSubmitCompletion(this.currentSubmitIndex - 2L, 5s)) { ... }
    this.currentCommandPool().reset();
    this.destroyQueue.rotate();
    this.transientMemory.beginSubmit();
}

// Fence 基于当前 submitIndex 创建
public GpuFence createFence() {
    return new GpuFence(this) {
        private final long submitIndex = this.this$0.currentSubmitIndex;
        public boolean awaitCompletion(long timeoutMs) {
            return this.this$0.awaitSubmitCompletion(this.submitIndex, timeoutMs);
        }
    };
}
```

**关键设计：timeline semaphore 双缓冲**
- `currentSubmitIndex` 单调递增，每帧 +1
- `awaitSubmitCompletion(n)` 通过 `vkWaitSemaphores` 等待指定值的 semaphore
- 始终等待 `currentSubmitIndex - 2`，即留出 2 帧的并行窗口
- `completedSubmitIndex` 记录实际完成值，避免重复等待

#### 我们的 D3D12（Dx12CommandEncoderBackend.java）
```java
// 无 submitSemaphore/timeline semaphore
// 使用全局 D3D12 fence 值
private long queueFenceValue = -1;
private long globalQueueFenceValue = 0;  // 全局计数

public void submit() {
    this.dx12EndCommandList(this.nativeCtx);
    this.dx12SignalFence(this.nativeCtx, ++this.globalQueueFenceValue);
    this.dx12SubmitCommandList(this.nativeCtx);
    // 等待 2 帧前的 fence
    if (this.globalQueueFenceValue >= 2) {
        this.dx12WaitForFence(this.nativeCtx, this.globalQueueFenceValue - 2);
    }
    this.rotate();
}

public GpuFence createFence() {
    // BUG: 使用 globalQueueFenceValue 而非 per-encoder 值
    long fenceValue = this.dx12CreateFence(this.nativeCtx, this.globalQueueFenceValue + 1);
    ...
}
```

**差异点：**
| 维度 | 官方 | 我们 |
|------|------|------|
| **同步原语** | timeline semaphore（per-submit value） | 全局 fence counter |
| **In-flight 管理** | 显式 2 缓冲（commandPool + destroyQueue + transientMemory 各 rotate） | 隐式（DLL 内部管理，但 Java 侧无对应 rotate 概念） |
| **延迟销毁** | `DestructionQueue<Destroyable>` 双缓冲 rotate | 无等价机制 |
| **Fence 粒度** | per-encoder 局部变量（submitIndex） | 全局 `globalQueueFenceValue`（BUG-03 根源） |
| **超时错误** | 带 checkpoint 的详细错误信息 | 简单 `IllegalStateException` |

---

### 2.4 RenderPass

#### 官方 Vulkan（VulkanRenderPass.java）
```java
// VALIDATION 模式：在 IDE 运行时做严格检查
public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE;

protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
protected final HashMap<String, TextureViewAndSampler> textures = new HashMap<>();
private boolean anyDescriptorDirty = false;

// setPipeline 时同时检查有效性
public void setPipeline(RenderPipeline pipeline) {
    this.pipeline = this.device.getOrCompilePipeline(pipeline);
    if (!this.pipeline.isValid()) {
        throw new IllegalStateException("Pipeline is not valid...");
    }
    this.anyDescriptorDirty = true;
    // 有 depth 和没 depth 用不同 pipeline（二元变体）
    VK12.vkCmdBindPipeline(commandBuffer, 0,
        this.hasDepth ? this.pipeline.withDepthPipeline() : this.pipeline.withoutDepthPipeline());
}

// pushDescriptors 延迟到 draw 时执行（脏标记机制）
private void pushDescriptors() {
    if (!this.anyDescriptorDirty) return;
    // VALIDATION 模式：检查所有 uniform 都已设置
    if (VALIDATION) {
        for (BindGroupLayout.UniformDescription uniform : ...) {
            GpuBufferSlice value = this.uniforms.get(uniform.name());
            if (value == null) throw new IllegalStateException("Missing uniform ...");
            // 检查 USAGE flag 正确性
        }
    }
    // 批量 push
    KHRPushDescriptor.vkCmdPushDescriptorSetKHR(commandBuffer, 0, layout, 0, writes);
    this.anyDescriptorDirty = false;
}

// multiDrawIndexed 支持两种形式
public void multiDrawIndexed(IntBuffer drawParameters, ...) { ... }
public void multiDrawIndexed(PointerBuffer firstIndexOffsets, ...) {
    throw new UnsupportedOperationException("Vulkan does not support multiDrawDirectSeparate");
}
```

#### 我们的 D3D12（Dx12RenderPassBackend.java）
```java
private Map<String, Dx12BindGroupEntry> descriptors = new HashMap<>();
private Dx12CompiledRenderPipeline currentPipeline = null;
private long currentPipelineHandle = 0L;

public void setPipeline(CompiledRenderPipeline p) {
    this.currentPipeline = (Dx12CompiledRenderPipeline) p;
    this.currentPipelineHandle = this.currentPipeline.handle();
    // 每次 setPipeline 都重新 setupRootSignature + IASetPrimitiveTopology
}

// 每次 draw 都重新 push 所有 descriptors（无脏标记优化）
public void drawIndexed(int indexCount, int instanceCount, ...) {
    if (this.currentPipelineHandle == 0L) throw ...;
    this.pushDescriptors();
    this.dx12DrawIndexed(...);
}

// 无 VALIDATION 模式（不在 IDE 做额外检查）
// 无 multiDrawIndexed 支持
```

**差异点：**
| 维度 | 官方 | 我们 |
|------|------|------|
| **Descriptor 更新策略** | 脏标记（`anyDescriptorDirty`），仅在变化时 push | 每次 draw 都 push（低效但安全） |
| **Pipeline 变体** | 有/无 depth 两套 PSO | 单套 PSO + `OMSetRenderTargets` 动态切换 |
| **VALIDATION 模式** | IDE 下严格检查 uniform/sampler/usage flag | 无等价机制 |
| **MultiDraw** | 支持 `vkCmdDrawMultiIndexedEXT` | 不支持 |
| **Debug Group** | 通过 `CheckpointExtension` 记录 checkpoint | 通过 `dbgLogDebug` 打印字符串日志 |

---

### 2.5 Shader 编译（核心差异）

#### 官方 Vulkan（GlslCompiler.java）
```java
public GlslCompiler() {
    Shaderc.shaderc_compile_options_set_target_env(shaderOptions, 0, 0x402000); // SPIR-V 1.2
    Shaderc.shaderc_compile_options_set_auto_bind_uniforms(shaderOptions, true);   // 自动绑定
    Shaderc.shaderc_compile_options_set_auto_map_locations(shaderOptions, true);   // 自动 location
    Shaderc.shaderc_compile_options_set_generate_debug_info(shaderOptions);
    Shaderc.shaderc_compile_options_set_optimization_level(shaderOptions, 0);
    this.globalDefines = ShaderDefines.builder()
        .define("gl_VertexID", "gl_VertexIndex")
        .define("gl_InstanceID", "gl_InstanceIndex")
        .build();
}

public CompiledModules compile(device, pipeline, vertex, fragment) {
    // 1. addToBindGroup（收集 UBO/Sampler/TEXEL_BUFFER）
    // 2. vertex.rebind(vertexInputNames, entries)   // spvc: 重写 SPIR-V 装饰
    // 3. fragment.rebind(vertexOutputNames, entries)
    // 4. vertex.createVulkanShaderModule(device)    // SPIR-V → VkShaderModule
    // 5. fragment.createVulkanShaderModule(device)
    // 6. VulkanBindGroupLayout.create(device, entries, name)
    // 返回: vertex shader module + fragment shader module + descriptor set layout
}
```

#### 我们的 D3D12（Dx12ShaderCompiler.java）
```java
public Dx12CompiledShader compile(pipeline, vertex, fragment) {
    // 1. addToBindGroup（收集 UBO/Sampler/TEXEL_BUFFER）— 相同
    // 2. vertex.rebind(vertexInputNames, entries)   — 相同（spvc rebind）
    // 3. fragment.rebind(vertexOutputNames, entries) — 相同
    // 4. vertex.toHlsl(true)                        // SPIR-V → HLSL（spvc HLSL backend）
    // 5. fragment.toHlsl(false)
    // 6. 生成 semanticNames（从 vertexInputNames 推导）
    // 7. 返回: vertexHlsl + fragmentHlsl + entries + vertexInputNames + semanticNames
    // 原生层负责: HLSL → DXBC → RootSignature → PSO
}
```

**差异点：**
| 维度 | 官方 | 我们 |
|------|------|------|
| **编译目标** | SPIR-V 1.2 | HLSL SM5.1 |
| **Shader 后端** | 直接 SPIR-V → VkShaderModule（GPU driver 编译） | SPIR-V → HLSL → D3DCompile → DXBC（中间语言转换） |
| **root signature** | Vulkan push descriptor（无需 root signature） | 必须在 native 侧构建 D3D12 root signature |
| **Shader 缓存** | 无（每次编译为 VkShaderModule） | pipeline cache（IdentityHashMap）+ shader source cache |
| **auto_bind_uniforms** | shaderc 自动绑定 descriptor set/binding | 手动 rebind（spvc） |
| **auto_map_locations** | shaderc 自动映射 location | 手动 rebind（spvc） |
| **Debug info** | `set_generate_debug_info` | 无 |
| **诊断绿色通道** | 无 | `DIAG_GREEN` 选项（用于排除 shader 数据问题） |

---

### 2.6 Pipeline 创建

#### 官方 Vulkan（VulkanRenderPipeline.java）
```java
public static VulkanRenderPipeline compile(device, layout, pipeline, vertexModule, fragmentModule) {
    // 1. vkCreatePipelineLayout（含 descriptor set layout）
    // 2. VkPipelineShaderStageCreateInfo{vertex, fragment}
    // 3. VkVertexInputAttributeDescription + VkVertexInputBindingDescription
    //    - attribLocation 连续递增（0, 1, 2, ...）
    //    - binding.divisor 支持 instanced attributes
    // 4. VkPipelineInputAssemblyStateCreateInfo
    // 5. VkPipelineRasterizationStateCreateInfo（cullMode, frontFace, lineWidth）
    // 6. VkPipelineDepthStencilStateCreateInfo
    // 7. VkPipelineColorBlendAttachmentState（blend function）
    // 8. VkPipelineViewportStateCreateInfo（scissorCount=1, viewportCount=1）
    // 9. VkPipelineMultisampleStateCreateInfo（rasterizationSamples=1）
    // 10. VkPipelineDynamicStateCreateInfo（DYNAMIC_STATE_VIEWPORT, DYNAMIC_STATE_SCISSOR）
    // 11. VkPipelineRenderingCreateInfoKHR（colorAttachmentFormats[], depthAttachmentFormat）
    // 12. vkCreateGraphicsPipelines → withDepthPipeline
    // 13. （无 depth）→ withoutDepthPipeline（depthAttachmentFormat=0 重编译）
}
```

#### 我们的 D3D12（dx12_device.cpp 第 1500-1610 行）
```cpp
// 1. ID3D12RootSignature (从 entries 构建)
// 2. D3D12_INPUT_ELEMENT_DESC[]（semanticName 来自 spvc remap）
// 3. D3D12_GRAPHICS_PIPELINE_STATE_DESCRIPTION
//    - Flags = 0（无 IA Primitive Restart）
//    - RS（RasterizerState）
//    - DS（DepthStencilState）
//    - IA（InputAssembler）
//    - SO（StreamOutput，全部 null）
//    - BS（BlendState）
//    - DSVFormat
// 4. m_pDevice->CreateGraphicsPipelineState → pPipelineState
// 5. 只创建一个 PSO（不分 withDepth/withoutDepth 变体）
```

**差异点：**
| 维度 | 官方 | 我们 |
|------|------|------|
| **PSO 数量** | 2 个（withDepth + withoutDepth） | 1 个（动态切换 OM 目标） |
| **Dynamic State** | DYNAMIC_STATE_VIEWPORT + DYNAMIC_STATE_SCISSOR | 无 dynamic state（viewport/scissor 在 push 时固定） |
| **IA Flags** | 无特殊 flag | 无 `D3D12_PIPELINE_STATE_FLAG_IMMEDIATE_LIST` |
| **Depth/Stencil** | 独立的 `VkPipelineDepthStencilStateCreateInfo` | 内嵌在 `D3D12_GRAPHICS_PIPELINE_STATE_DESCRIPTION` |
| **Blend 配置** | 按 `ColorTargetState` 逐个配置 | 类似，但无 per-attachment blend state 数组 |
| **Vertex Input Divisor** | `VkVertexInputBindingDivisorDescriptionEXT` | 无 divisor 支持（`inputRate` 固定为 0） |
| **Pipeline 命名** | `setObjectName` 调试命名 | 无等价机制 |

---

### 2.7 Surface（交换链）

#### 官方 Vulkan（VulkanGpuSurface.java）
```java
// 独立 acquire semaphore（binary，2 个循环）
private final long[] acquireSemaphores = new long[2];
private int currentAcquireSemaphore = 0;

// 每个 back buffer 独立的 present semaphore
private long[] presentSemethores = new long[swapchainImageCount];

// 3 个 swap chain image（minImageCount=3）
// acquireNextTexture 中:
//   - 旋转 acquire semaphore
//   - vkAcquireNextImageKHR（timeout=5s，等待 acquireSemaphore）
//   - 记录 currentImageIndex

// blitFromTexture:
//   - 创建临时 command buffer（allocateAndBeginTransientCommandBuffer）
//   - ImageBarrier: UNDEFINED → GENERAL（copy source）
//   - vkCmdBlitImage（带 mipLevel 支持）
//   - ImageBarrier: GENERAL → PRESENT_SRC_KHR（需要 memory barrier）
//   - Signal present semaphore（值=0 binary）
//   - waitSemaphore(acquireSemaphore) + execute

// present:
//   - vkQueuePresentKHR（等待 presentSemaphore[currentImageIndex]）
//   - 重置 currentImageIndex = -1
```

#### 我们的 D3D12（Dx12GpuSurface.java + dx12_surface.cpp）
```cpp
// per-backbuffer fence（P18，新增）
struct BackbufferState {
    ComPtr<ID3D12CommandAllocator> allocator;
    ComPtr<ID3D12GraphicsCommandList> commandList;
    uint64_t fenceValue;        // 该 back buffer 上一次 submit 的 fence 值
};
std::vector<BackbufferState> backbuffers;
uint64_t currentFenceValue = 0;

// acquire:
//   - IDXGISwapChain3::GetBuffer(index)
//   - GetCurrentBackBufferIndex() 确保不重复 acquire 未完成的 buffer

// blit:
//   - 无显式 barrier（通过 descriptor state management）
//   - CopyTextureRegion（非 blit，纯 copy）
//   - Release/Reserve Descriptor Handle 管理 SRV

// present:
//   - IDXGISwapChain3::Present(syncInterval, flags)
//   - Flush + Signal fence
```

**差异点：**
| 维度 | 官方 | 我们 |
|------|------|------|
| **同步模型** | 独立 acquire semaphore + per-image present semaphore | per-backbuffer D3D12 fence（P18） |
| **Blit vs Copy** | `vkCmdBlitImage`（支持缩放+filter） | `CopyTextureRegion`（1:1 copy，无缩放） |
| **Image Layout 转换** | 显式 barrier（UNDEFINED→GENERAL→PREVIEW） | D3D12 resource state（RenderTarget → CopySource → Present） |
| **Swapchain 图像数** | 3（minImageCount=3） | 由 DXGI 决定（通常 2 或 3） |
| **Out of Date 处理** | `vkAcquireNextImageKHR` 返回 `ERROR_OUT_OF_DATE` → 标记 outOfDate | `ResizeBuffers` 处理窗口大小变化 |
| **GPU Timeout 检测** | `vkAcquireNextImageKHR` 返回 `WAIT_TIMEOUT` + checkpoint 信息 | 无 GPU timeout 检测（fence wait 无 timeout） |
| **Present Mode** | 通过 `VK_KHR_swapchain` 枚举 | 通过 `DXGI_OUTPUT_DESC` + `DXGI_FORMAT` 枚举 |
| **Surface Format 选择** | 选择 `B8G8R8A8_UNORM` 或 `R8G8B8A8_UNORM` + `SRGB_COLOR_SPACE` | 选择 `DXGI_FORMAT_B8G8R8A8_UNORM_SRGB` |

---

### 2.8 TransientMemory

#### 官方 Vulkan（VulkanTransientMemory.java）
```java
// 使用 VMA 分配器管理瞬时 buffer
private final long vmaAllocator;

public GpuBufferSlice uploadStaging(ByteBuffer data, long alignment, int memoryFlags) {
    VmaAllocationInfo info = ...;
    VmaAllocation allocation = vmaAllocateBuffer(this.vma, ..., &info);
    // 映射到 CPU 可见内存并 copy
    return new GpuBufferSlice(stagingBuffer, offset, data.remaining());
}
// 生命周期由 DestructionQueue 管理
```

#### 我们的 D3D12（Dx12TransientMemory.java）
```java
// 自实现 staging buffer 管理
private final List<Dx12GpuBuffer> stagingBuffers = new ArrayList<>();
private int currentStagingBufferIdx = 0;
private long currentOffset = 0;

public GpuBufferSlice uploadStaging(ByteBuffer data, long alignment, int memoryFlags) {
    // 轮转 staging buffer，超出容量时创建新 buffer
    // 无 GPU-visible 内存分配器
}
```

**差异点：**
| 维度 | 官方 | 我们 |
|------|------|------|
| **内存分配** | VMA（GPU 本地 + CPU 可见统一管理层） | 自实现 ring buffer |
| **Staging 回收** | DestructionQueue 延迟回收 | rotate() 立即切换 |
| **缓存策略** | VMA 管理分配缓存 | 无缓存 |

---

## 三、功能缺失清单

### 3.1 完全缺失的功能

| 功能 | 官方有 | 我们没有 | 影响 |
|------|--------|----------|------|
| **Vendor Checkpoint 扩展** | AMD/NVIDIA checkpoint | 无 | GPU timeout 无法定位崩溃位置 |
| **multiDrawIndexed** | vkCmdDrawMultiIndexedEXT | 不支持 | 大量相似 draw call 性能损失 |
| **Descriptor Set 持久化** | push descriptor 可跨 draw 复用 | 每次 draw 全量 re-push | descriptor 绑定开销高 |
| **Staging Buffer 池化** | VMA 自动池化 | 自实现 ring buffer | 内存碎片可能更多 |
| **Device UUID 黑名单** | KNOWN_PROBLEMATIC_DEVICES | 无 | 已知问题显卡不会跳过 |
| **Discrete GPU 优先** | isDeviceDiscrete() 优先 | 取第一个可用 | 集成显卡可能被选中 |
| **IDE Validation 模式** | `SharedConstants.IS_RUNNING_IN_IDE` | 无 | 开发阶段缺少 descriptor 一致性检查 |
| **Pipeline Debug 命名** | `setObjectName` | 无 | RenderDoc/Nsight 中无法看到有意义的名字 |
| **Timeline Semaphore** | `VkSemaphoreTypeCreateInfo{type=1}` | 全局 fence counter | 语义等价但实现简陋 |
| **Compute/Transfer Queue** | 独立的 compute/transfer queue | 单 queue | 无法并行 compute/copy 与渲染 |

### 3.2 实现差异但功能等价

| 功能 | 官方 | 我们 |
|------|------|------|
| **Fence 同步** | timeline semaphore + `vkWaitSemaphores` | D3D12 fence + `Signal`/`WaitForFence` |
| **In-flight 管理** | 双 CommandPool rotate | DLL 内部管理（黑盒） |
| **PSO 创建** | with/without depth 两套 | 单套 + 动态 OM 切换 |
| **Descriptor 更新** | `vkCmdPushDescriptorSetKHR` | D3D12 root descriptor table + `RSSetShaderResources` |
| **Swapchain** | `vkCreateSwapchainKHR` + acquire/present semaphores | DXGI flip-model swapchain |

### 3.3 已知 Bug（来自 code-review-deep.md）

| Bug | 官方 Vulkan | 我们的 D3D12 |
|-----|-------------|--------------|
| **语义名索引错位** | `attribLocation` 连续递增，spvc rebind 保证匹配 | `buildNativeDesc` 与 `toHlsl()` 基准不一致（BUG-01） |
| **Fence 竞态** | timeline semaphore 串行化，无竞态 | 全局 `queueFenceValue` 被临时 encoder 递增（BUG-03） |
| **Per-backbuffer fence** | present semaphore per image，天然隔离 | P18 新增 per-backbuffer fence |
| **Resource leak（静态）** | 无 | readback/staging 使用函数静态 `ComPtr`（BUG-04） |
| **ensureDevice 幂等** | 无此问题（Vulkan 不支持中途重创建） | 创建失败重试会泄漏（BUG-05） |

---

## 四、关键设计哲学差异

### 4.1 官方 Vulkan：显式、可观测、分层清晰

1. **每层都有独立对象**：Instance → PhysicalDevice → Device → Queue → CommandPool → CommandBuffer → Swapchain
2. **内存管理外包**：VMA 是独立库，与 Vulkan 层完全分离
3. **诊断优先**：Checkpoint extension、Validation mode、ObjectName
4. **延迟销毁**：DestructionQueue 确保 GPU 不再引用后才真正释放
5. **双缓冲流水线**：MAX_SUBMITS_IN_FLIGHT=2，始终领先 GPU 2 帧

### 4.2 我们的 D3D12：封装、紧凑、自给自足

1. **D3D12 层黑盒**：DLL 内部管理 DeviceContext/CommandContext，Java 层只传 handle
2. **自实现内存管理**：`Dx12TransientMemory` 替代 VMA，简单但功能有限
3. **Java 侧诊断**：`System.err.println` 替代 vendor checkpoint
4. **单缓冲为主**：共享 encoder 单例，简化了并发模型但也限制了并行度
5. **HLSL 中间层**：增加了编译链复杂度，但提供了 shader 源码可读性

---

## 五、修复优先级建议

| 优先级 | 项目 | 原因 |
|--------|------|------|
| 🔴 P0 | **BUG-01 语义名索引错位** | 直接导致 4+ 属性管线崩溃 |
| 🔴 P0 | **BUG-03 Fence 竞态** | 可能导致 use-after-free |
| 🟠 P1 | **DeviceInfo 补齐** | 缺失 anisotropy/alignment 等硬件参数 |
| 🟠 P1 | **Descriptor 脏标记优化** | 每次 draw 全量 re-push 性能浪费 |
| 🟠 P1 | **Vendor Checkpoint** | GPU timeout 调试必要 |
| 🟡 P2 | **discrete GPU 优先** | 笔记本双显卡场景 |
| 🟡 P2 | **DestructionQueue 延迟销毁** | 防止临时 buffer 过早释放 |
| 🟡 P2 | **Pipeline debug 命名** | RenderDoc 调试体验 |
| 🔵 P3 | **multiDrawIndexed** | 大量实例化 draw 性能优化 |
| 🔵 P3 | **独立 compute/transfer queue** | 并行度提升 |
