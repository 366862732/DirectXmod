# S6 — DX12 后端：在官方 GPU 后端架构上实现纯 D3D12 渲染

## 背景

Minecraft **26.2** 官方重构了渲染器，引入**可插拔 GPU 后端架构**：

- `net.minecraft.client.PreferredGraphicsApi`（枚举 `DEFAULT / OPENGL / VULKAN`，视频设置里新增 "Graphics API" 选项）
- `com.mojang.blaze3d.systems.GpuBackend` — 顶层后端接口（`getName / setWindowHints / createDevice`）
- `GpuDeviceBackend` — 设备级工厂接口（创建 surface / commandEncoder / texture / buffer / sampler / pipeline / query）
- `GpuSurfaceBackend` / `CommandEncoderBackend` / `RenderPassBackend` — 命令与呈现层
- 资源接口：`GpuTexture`、`GpuTextureView`、`GpuBuffer`、`GpuSampler`、`GpuFence`、`GpuQueryPool`、`CompiledRenderPipeline`、`TransientMemory`、`DeviceInfo`
- 官方 OpenGL 实现：`com.mojang.blaze3d.opengl.GlBackend`
- 官方 Vulkan 实现：`com.mojang.blaze3d.vulkan`（VulkanInstance / VulkanDevice / VulkanCommandEncoder / VulkanRenderPass / VulkanGpuBuffer 等 30+ 类，**这是我们学习的模板**）

## 目标

**用原生 D3D12 实现官方这套 GpuBackend 接口**，作为 `PreferredGraphicsApi` 的第三个后端（DX12）嵌入，让官方全部渲染逻辑（RenderState 提取、区块网格、实体、天空、粒子）原样运行，只替换 GPU 层。

- 技术栈：Rust 手写 D3D12（不使用 wgpu），Java 侧实现官方接口，JNI 桥接。
- 现有 wgpu-mc 渲染器源码**仅作参考**（数据提取、天空/云/雾/粒子算法可借鉴），不直接复用。
- 官方 Vulkan 实现的**逻辑直接照搬**到 D3D12（语言/API 需重写）。

## 官方接口清单（必须实现的 backend 接口）

### 1. `GpuBackend`（顶层）
```java
String getName();
void setWindowHints();
void handleWindowCreationErrors(GLFWErrorCapture.Error) throws BackendCreationException;
GpuDevice createDevice(long windowHandle, ShaderSource, GpuDebugOptions, Runnable onDeviceLost) throws BackendCreationException;
```

### 2. `GpuDeviceBackend`（工厂）
```java
GpuSurfaceBackend createSurface(long hwnd);
CommandEncoderBackend createCommandEncoder();
GpuSampler createSampler(AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int mipLevels, OptionalDouble anisotropy);
GpuTexture createTexture(Supplier<String> label, int textureFlags, GpuFormat format, int w, int h, int depth, int mipLevels);
GpuTextureView createTextureView(GpuTexture tex);
GpuTextureView createTextureView(GpuTexture tex, int baseLevel, int levelCount);
GpuBuffer createBuffer(Supplier<String> label, int flags, long size);
GpuBuffer createBuffer(Supplier<String> label, int flags, ByteBuffer data);
List<String> getLastDebugMessages();
boolean isDebuggingEnabled();
CompiledRenderPipeline precompilePipeline(RenderPipeline, ShaderSource);
void clearPipelineCache();
void close();
GpuQueryPool createTimestampQueryPool(int count);
long getTimestampNow();
DeviceInfo getDeviceInfo();
```

### 3. `GpuSurfaceBackend`（swapchain）
```java
void configure(GpuSurface.Configuration) throws SurfaceException;
boolean isSuboptimal();
void acquireNextTexture() throws SurfaceException;
void blitFromTexture(CommandEncoderBackend, GpuTextureView);
void present();
Collection<PresentMode> supportedPresentModes();
```

### 4. `CommandEncoderBackend`
```java
void submit();
TransientMemory transientMemory();
RenderPassBackend createRenderPass(RenderPassDescriptor);
void submitRenderPass();
// clearColorTexture / clearColorAndDepthTextures / clearDepthTexture
// writeToBuffer / copyToBuffer / writeToTexture / copyBufferToTexture
// copyTextureToBuffer / copyTextureToTexture / createFence / writeTimestamp
```

### 5. `RenderPassBackend`
```java
void setPipeline(RenderPipeline);
void bindTexture(String name, GpuTextureView, GpuSampler);
void setUniform(String name, GpuBuffer);  // + GpuBufferSlice
void setVertexBuffer(int binding, GpuBufferSlice);
void setIndexBuffer(GpuBuffer, IndexType);
void drawIndexed(...);  // + multiDrawIndexed / drawIndexedIndirect / drawMultipleIndexed / draw / multiDraw / drawIndirect
void writeTimestamp(GpuQueryPool, int);
// pushDebugGroup / popDebugGroup / enableScissor / disableScissor
```

### 6. 资源接口
`GpuTexture`（upload/download/write/view/filter/blit）、`GpuTextureView`、`GpuBuffer`（getMappedSlice/write/read/fence）、`GpuBufferSlice`、`GpuSampler`、`GpuFence`、`GpuQueryPool`（createQuery/freeQuery）、`CompiledRenderPipeline`、`TransientMemory`、`DeviceInfo`、`RenderPassDescriptor`。

> 高层门面类（`GpuDevice` / `GpuSurface` / `CommandEncoder` / `RenderPass` / `GpuTexture` 接口的默认方法）由官方提供，我们只实现 Backend 接口 + 资源对象。

## 嵌入机制（mixin）

1. Mixin `PreferredGraphicsApi`：在枚举中注入 `DX12` 常量，`getBackendsToTry()` 返回包含 `Dx12Backend` 的数组（DEFAULT 时优先尝试 DX12，失败回退 GL/Vulkan）。
2. `Options` 的 Graphics API 选项通过 `PreferredGraphicsApi.CODEC` 序列化，加枚举值即可自动出现在设置里（需同步 `DataFixer` 无关）。
3. `GpuBackend.createDevice(long hwnd, ShaderSource, GpuDebugOptions, Runnable)` 被窗口创建流程调用 → JNI 进入 Rust 创建 D3D12 device。

## 技术难点

1. **Shader 链路（最大风险）**：官方给出 GLSL 源码（`ShaderSource.get(id, type)`）+ `ShaderDefines`。Vulkan 后端内部做 GLSL→SPIR-V。D3D12 需要 **GLSL→SPIR-V→HLSL→DXIL** 或等价链路（glslang/SPIRV-Cross + DXC），或将官方 shader 移植为 HLSL。
2. **描述符管理**：D3D12 descriptor heap（CBV/SRV/UAV/sampler）管理，对应官方 BindGroupLayout。
3. **资源状态转换**：D3D12 手动 barrier，对应官方自动布局转换。
4. **GPU 同步**：fence + 多帧飞行。
5. **UberGpuBuffer**：官方把所有顶点/索引数据塞进超大 GPU 缓冲（`com.mojang.blaze3d.vertex.UberGpuBuffer`），需要 D3D12 heap 管理。
6. **shader 缓存**：对应 `ShaderCompilationKey(id, type, defines)` → DXIL 缓存。

## 阶段规划

- **P1 挂点验证**：Java `Dx12Backend` 骨架（实现 GpuBackend）+ mixin 注册 DX12 + createDevice 调通 JNI → Rust 创建 D3D12 device + 打印设备信息；失败安全 fallback，不崩溃。
- **P2 资源层**：`GpuDeviceBackend` 全套资源创建（texture/buffer/sampler/view）→ D3D12 resource + heap 分配。
- **P3 命令层**：`CommandEncoderBackend` + `RenderPassBackend` → command list 录制、clear/copy/draw、fence 同步。
- **P4 管线与 shader**：`RenderPipeline` → D3D12 PSO；GLSL→HLSL→DXIL 编译链路落地 + 缓存。
- **P5 Surface 呈现**：DXGI swapchain + `GpuSurfaceBackend`，clear 后 present 全屏颜色。
- **P6 首帧画面**：官方渲染流程跑通（清屏→简单 draw），窗口呈现 MC 画面（哪怕只有天空/清屏色）。
- **P7+ 完整渲染**：对照官方 Vulkan 逐 RenderPass 移植：区块、实体、天空、粒子、lightmap、后处理（FXAA）。

## 学习沉淀

- 将官方 Vulkan 后端反编译源码存入 `docs/official-vulkan/` 作为移植参照。
- 每个阶段对照官方 `GlBackend`（GL 实现，逻辑最接近 D3D12）与 `vulkan/*`（架构最现代）两套实现。
