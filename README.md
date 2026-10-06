# DirectXMod (gl4dx12)

> **Minecraft 26.2 Fabric 模组的原生 D3D12 渲染后端。**
> 通过实现 Minecraft 官方的 `GpuBackend` / `GpuDeviceBackend` 接口，用 C++ 原生 DLL（`dx12_mc.dll`）替代 OpenGL/Vulkan 渲染路径，使 Minecraft 使用 D3D12 直接渲染游戏画面，同时保留 Mod 扩展点（AA 设置、ModMenu 集成）。

> 鸣谢：爱睡觉的白洲梓 / xiaozi Graphics Development Team

## 概述

本项目采用 **Mixin 注入** 方式修改 Minecraft 的图形 API 选择逻辑，让游戏改用 D3D12 后端渲染，同时通过 Java 层提供渲染状态同步、配置管理和 Mod 兼容层。

### 核心特点

- **Mixin 注入**：12 个 Mixin 覆盖图形 API 选择 + 初始化/渲染/资源加载/世界加载/区块/摄像机/雾效全链路诊断（见 [Mixin 注入点](#mixin-注入点)）
- **官方 GpuBackend 接口**：完整实现 `GpuBackend` / `GpuDeviceBackend` / `CommandEncoderBackend` / `RenderPassBackend` / `GpuSurfaceBackend` 等官方接口
- **零自定义渲染循环**：不复写 `RenderSystem`，完全接管 Minecraft 官方的渲染流程
- **Shaderc + Spvc 编译链**：GLSL → SPIR-V（shaderc）→ HLSL SM5.1（spvc）→ DXBC（D3DCompile）
- **资源安全**：所有 D3D12 资源通过 Java 包装类（`Dx12Gpu*`）管理，随 Minecraft 生命周期自动释放
- **配置界面**：Fx12Config 持久化配置，Dx12SettingsScreen 设置界面，ModMenu 集成
- **DLL 自动加载**：从 JAR 提取到 `{user.dir}/dx12mod/dx12_mc.dll`，支持版本隔离
- **MC 26.2**：支持 Mojang 官方映射（不依赖 Yarn 映射），fabric-api 0.156.0+26.2 / loader 0.19.3 / ModMenu 20.0.1

### 当前阶段：P0-P33 + Async Phase 1（C++ 已完成，Java 层未纳入），LIGHTMAP-VFLIP 已修复，世界渲染正常（2026-09-26）

| 阶段 | 状态 | 说明 |
|------|------|------|
| **P0: 原生层基础** | ✅ 已完成 | dx12_mc.dll：D3D12 初始化 + 资源创建 (texture/buffer/sampler/view) |
| **P1: 设备自检** | ✅ 已完成 | Dx12Native.dx12CreateDevice() + 资源 self-test |
| **P2: 资源层** | ✅ 已完成 | Dx12GpuTexture / Dx12GpuBuffer / Dx12GpuSampler / Dx12GpuTextureView |
| **P3: 命令编码层** | ✅ 已完成 | Dx12CommandEncoderBackend：submit/fence/copy/clear/timestamp |
| **P4: 管线编译层** | ✅ 已完成 | shaderc SPIR-V → spvc 反射/rebind → HLSL → D3DCompile DXBC |
| **P5: 交换链层** | ✅ 已完成 | Dx12GpuSurface：DXGI flip-model swapchain (configure/acquire/blit/present) |
| **P6: Draw 全链路** | ✅ 已完成 | Dx12RenderPassBackend：setPipeline + pushDescriptors + draw/drawIndexed |
| **P15: 日志分级系统** | ✅ 已完成 | GameRenderer 调试插桩 + 分级日志 (dbgLog/dbgLogInfo/dbgLogDebug) |
| **P16: renderLevel 诊断** | ✅ 已完成 | 帧计数/advanceGameTime/pause/native submit+present 日志 |
| **P17: 绘制目标跟踪** | ✅ 已完成 | activeColorTargetsTouched + dbgReadbackSurfacePixels() 3×3 采样 |
| **P18: Surface fence** | ✅ 已完成 | per-backbuffer fence 追踪，acquireSurface 前检查 GPU 占用 |
| **P20: 描述符表偏移修复** | ✅ 已完成 | pushDescriptors 根描述符表 GPU 地址偏移修正，避免读前帧残留 |
| **P21: 深度测试修复** | ✅ 已完成 | removeDepthClearAutoFix + reverse-Z 比较函数 + depth PSO 双轨创建 |
| **P22: 描述符堆扩容** | ✅ 已完成 | drawHeap x4 半区，支持三帧并行飞行（fenceValue%4 交替写入） |
| **P24: 帧级瞬态 RTV/DSV** | ✅ 已完成 | 新增 frameRtvHeap/dsvHeap（每帧从 0 复用），P24 fix：SRV 必须写入命令列表当前设置的堆（黑屏根因之一） |
| **P25: SampleMask + 纹理格式** | ✅ 已完成 | PSO 补设 SampleMask=UINT_MAX；RGB32_FLOAT 不再加宽成 RGBA32_FLOAT |
| **P26: upload readback 修复** | ✅ 已完成 | Map 读回必须带 offset 的 D3D12_RANGE，否则读到旧数据 |
| **P27: 高频日志降级** | ✅ 已完成 | beginRenderPass/setPipeline/drawIndexed 逐条诊断日志降级为 DBG_LOG_DEBUG（数千次/帧不拖死主线程） |
| **P28: 图集渲染与坐标系适配** | ✅ 已完成 | animate_sprite 顶点着色器注入 Y-flip（GL bottom-up → D3D12 top-down）；静态采样器改 POINT（与官方图集上传 NEAREST 语义一致）；图集 dump 验证内容正确、GUI 按钮渲染正常 |
| **BUG-01: semanticNames 修复** | ✅ 已完成 | `semanticNames` 基于 `vertex.inputs().size()` 补齐，与 spvc 基准对齐 |
| **LIGHTMAP-VFLIP（已修复）** | ✅ 已完成 | 光贴图更新 pass（pipeline/lightmap）被 P31 flipY 判定排除（usage=13 且需带深度附件）→ GL bottom-up 写入方向与 terrain 采样相反，白天方块采到暗行（lightmapviz≈26-46、whitelight 探针恢复明亮、readback BMP 证实内容渐变正确但 Y 镜像）。修复：Dx12ShaderCompiler 对 location 含 "lightmap" 且 !flipY 的管线在 frag_main 入口注入 `texCoord.y = 1.0f - texCoord.y;`；诊断：whitelight/uv2viz/lightmapviz 探针 + dbgReadbackTexturePixels 读回 16×16 光贴图 |
| **P29: NDC 翻转修复** | ✅ 已完成 | 修复 GuiItemAtlas / 人物渲染倒置：NDC Y 轴方向适配 D3D12 top-down 坐标 |
| **P30: 世界渲染黑屏修复** | ✅ 已完成 | level=null 根因定位并修复，世界地形/区块/实体正常渲染；新增区块/摄像机/雾效/切片诊断 mixin |
| **P31: flipY 判定逻辑优化** | ✅ 已完成 | 仅对需要垂直翻转的 shader 路径注入 Y-flip，避免光贴图误处理 |
| **P32: Async Phase 1（C++）** | ✅ 已完成 | 异步 Fence/Bundle/Descriptor/Executor C++ 基础设施完成（487 行头文件 + 1253 行 cpp）；per-worker 镜像堆 + SetEventOnCompletion + COM 主线程释放；Java 侧异步包未纳入本次提交 |
| **P33: JNI 异步描述符绑定** | ✅ 已完成 | jni_bridge_p33.cpp（605 行）新增异步描述符管理的 native 接口 |
| **CS_OWNDC 窗口修复** | ✅ 已完成 | 修复 Windows CS_OWNDC 类窗口在 DX12 下 swapchain 创建失败的兼容性问题 |

## 项目结构

```
dx12-lib-template-26.1.2/
├── fabric/                          # Fabric 模组（Java）
│   ├── src/main/java/com/xgdt/dx12/
│   │   ├── Dx12Mod.java            # 模组入口，设置图形 API 偏好 + 日志自检信息
│   │   ├── ModMenuIntegration.java # ModMenuApi 实现，配置界面集成
│   │   ├── config/
│   │   │   └── Dx12Config.java     # 配置持久化（aa_mode 等，Properties 格式）
│   │   ├── gui/
│   │   │   └── Dx12SettingsScreen.java # AA 模式设置 GUI（None/FXAA/SMAA/TAA）
│   │   ├── mixin/
│   │   │   ├── PreferredGraphicsApiMixin.java          # 将 D3D12 设为首选图形 API
│   │   │   ├── GameRendererRenderDebugMixin.java       # P16: renderLevel 诊断（frame/resourcesLoaded等）
│   │   │   ├── MinecraftRunDebugMixin.java             # P19: Minecraft.run() 入口诊断
│   │   │   ├── MinecraftRunTickDebugMixin.java         # P19: runTick 每帧 level/gameLoadFinished/pause
│   │   │   ├── MinecraftSetLevelDebugMixin.java        # P19: setLevel() 调用诊断
│   │   │   ├── MinecraftResourceLoadDebugMixin.java    # P19: onResourceLoadFinished/onGameLoadFinished
│   │   │   ├── MinecraftDoWorldLoadDebugMixin.java     # P0: doWorldLoad() 入口诊断
│   │   │   ├── ClientPacketListenerLoginDebugMixin.java  # P0: handleLogin() 登录包诊断
│   │   │   ├── LevelRendererPrepareChunksDebugMixin.java # P30: 区块预加载诊断
│   │   │   ├── CameraUpdateDebugMixin.java             # P30: 摄像机更新诊断
│   │   │   ├── FogRendererUpdateDebugMixin.java        # P30: 雾效更新诊断
│   │   │   └── SectionRenderDispatcherGetSliceDebugMixin.java # P30: 区块切片调度诊断
│   │   └── dx12/                    # D3D12 后端核心实现（镜像官方 Vulkan 后端）
│   │       ├── Dx12Native.java               # JNI 桥接层（474 行）
│   │       ├── Dx12Backend.java              # GpuBackend 实现：窗口/设备创建 + 4 轮自测（416 行）
│   │       ├── Dx12Device.java               # GpuDeviceBackend 实现：资源创建 + Shader 编译缓存（679 行）
│   │       ├── Dx12ShaderCompiler.java       # GLSL→SPIR-V→HLSL 编译链（shaderc+spvc，498 行）
│   │       ├── Dx12IntermediaryShaderModule.java  # SPIR-V 反射绑定信息（spvc 语义注入，401 行）
│   │       ├── Dx12CompiledShader.java       # 编译产物（HLSL 源码 + 绑定列表）
│   │       ├── Dx12CompiledRenderPipeline.java  # 编译后的渲染管线
│   │       ├── Dx12BindGroupEntry.java       # 管线绑定条目（UBO/SRV/TexelBuffer）
│   │       ├── Dx12GpuSurface.java           # DXGI swapchain（P5+P17，169 行）
│   │       ├── Dx12CommandEncoderBackend.java    # 命令编码层（P3+P22，584 行）
│   │       ├── Dx12RenderPassBackend.java        # 渲染通道层（P6+P17+P22，538 行）
│   │       ├── Dx12TransientMemory.java          # 瞬时内存管理（per-frame 缓冲回收，274 行）
│   │       ├── Dx12GpuTexture.java             # D3D12 纹理资源包装（view 引用计数，68 行）
│   │       ├── Dx12GpuTextureView.java         # D3D12 纹理视图（SRV，延迟销毁，43 行）
│   │       ├── Dx12GpuBuffer.java              # D3D12 缓冲区资源包装（61 行）
│   │       ├── Dx12GpuSampler.java             # D3D12 采样器包装（86 行）
│   │       └── Dx12GpuQueryPool.java           # D3D12 GPU 时间戳查询池（72 行）
│   ├── src/test/java/com/xgdt/dx12/d3d12/
│   │   └── Dx12DeviceTest.java           # 单元测试（渲染循环验证）
│   ├── src/main/resources/
│   │   ├── fabric.mod.json           # Fabric 模组描述（client + modmenu entrypoints）
│   │   ├── gl4dx12.mixins.json       # Mixin 配置（9 个 mixin）
│   │   ├── dx12_mc.dll               # 原生 DLL（从 JAR 提取，运行时替换到 {user.dir}）
│   │   └── assets/dx12mod/icon.png   # 模组图标（16x16）
│   ├── libs/
│   │   └── modmenu-20.0.1.jar        # ModMenu 本地依赖（MC 26.2）
│   ├── build.gradle                  # Gradle 构建配置 (loom 1.15.5)
│   └── gradle.properties             # 版本参数
├── native/                          # D3D12 原生层（C++17，MSVC + CMake）
│   ├── src/
│   │   ├── dx12_device.cpp           # D3D12 设备/资源/命令/管线/Draw（4073 行）
│   │   ├── dx12_device.h             # 公共头文件（635 行，定义 DeviceContext/CommandContext 等）
│   │   ├── dx12_surface.cpp          # DXGI swapchain + blit + present + 读回诊断（868 行）
│   │   ├── jni_bridge.cpp            # JNI 入口（141 行）
│   │   ├── jni_bridge_p3.cpp         # P3 命令层 native（409 行）
│   │   ├── jni_bridge_p4.cpp         # P4 管线编译 native（172 行）
│   │   ├── jni_bridge_p5.cpp         # P5 交换链 + 读回 native（417 行）
│   │   ├── jni_bridge_p6.cpp         # P6 绘制 native（159 行）
│   │   ├── jni_bridge_p33.cpp        # P33 异步描述符管理 native（605 行）
│   │   ├── dx12_async_bundle.h/.cpp  # Bundle 录制与执行（142/181 行）
│   │   ├── dx12_async_descriptor.h/.cpp  # Per-worker 镜像描述符堆（109/104 行）
│   │   ├── dx12_async_executor.h/.cpp    # 异步命令分发器（100/179 行）
│   │   ├── dx12_async_fence.h/.cpp       # Fence 同步（96/251 行）
│   └── CMakeLists.txt                # CMake 构建配置
│   └── build/bin/Release/dx12_mc.dll # 预编译 DLL（开发用，JAR 内打包版本另行构建）
├── docs/
│   ├── official-262/                 # Minecraft 26.2 官方源码参考
│   ├── code-review-deep.md           # 深度代码审查报告（BUG-01 ~ BUG-17）
│   └── 步骤.md                       # 详细开发步骤文档
├── debug.log                         # 运行时诊断日志
└── 问题.md                           # 黑屏问题审查报告（P0 BUG-00 ~ P3 BUG-17）
```

## 架构设计

### 渲染管线：完全接管 Minecraft 官方渲染流程

本项目不创建自定义渲染循环，而是实现 Minecraft 26.1+ 引入的 `GpuBackend` 接口族，让游戏引擎自己驱动整个渲染：

```
Minecraft RenderSystem
  │
  ▼
GpuBackend.createDevice(window, shaderSource, ...)   ← Dx12Backend
  │
  ├─ GpuDeviceBackend.createTexture()                ← Dx12Device → Dx12Native.dx12CreateTexture()
  ├─ GpuDeviceBackend.createBuffer()                 ← Dx12Device → Dx12Native.dx12CreateBuffer()
  ├─ GpuDeviceBackend.createSampler()                ← Dx12Device → Dx12Native.dx12CreateSampler()
  ├─ GpuDeviceBackend.createSurface()                ← Dx12Device → Dx12Native.dx12CreateSurface()
  ├─ GpuDeviceBackend.createCommandEncoder()         ← Dx12Device → Dx12CommandEncoderBackend
  │   │
  │   └─ CommandEncoderBackend.beginRenderPass()     ← Dx12CommandEncoderBackend
  │       │
  │       └─ RenderPassBackend                       ← Dx12RenderPassBackend
  │           ├─ setPipeline()      → dx12SetPipeline()
  │           ├─ setVertexBuffer()  → dx12SetVertexBuffer()
  │           ├─ setIndexBuffer()   → dx12SetIndexBuffer()
  │           ├─ pushDescriptors()  → dx12PushDescriptors()
  │           ├─ drawIndexed()      → dx12DrawIndexed()
  │           └─ draw()             → dx12Draw()
  │
  └─ GpuSurfaceBackend                             ← Dx12GpuSurface
      ├─ configure()      → dx12ConfigureSurface()
      ├─ acquireNextTexture() → dx12AcquireSurface()
      ├─ blitFromTexture()  → dx12BlitSurface()
      └─ present()          → dx12PresentSurface()
```

### Shader 编译链（P4）

```
Minecraft ShaderSource (assets/minecraft/shaders/core/*.glsl)
  │
  ▼
Dx12ShaderCompiler.createIntermediary()
  │  shaderc: GLSL → SPIR-V
  │  GlslPreprocessor: 注入 gl_VertexID/gl_InstanceID 宏
  │
  ▼
Dx12IntermediaryShaderModule
  │  spvc 反射: 提取 UBO / Sampler / Output / Input 绑定
  │  spvc rebind: 将 SPIR-V binding decoration 重写为 0..n-1
  │
  ▼
Dx12CompiledShader
  │  spvc HLSL 后端: SPIR-V → HLSL SM5.1
  │
  ▼
Dx12Device.precompilePipeline()
  │  Dx12ShaderCompiler.compilePipeline()
  │  传入 packed descriptor buffer 到原生层
  │
  ▼
dx12_mc.dll: D3DCompile → DXBC + Root Signature + PSO
  │
  ▼
Dx12CompiledRenderPipeline (handle ≠ 0 表示成功)
```

### Mixin 注入点

12 个 Mixin 覆盖从 API 选择到世界加载/渲染全链路诊断：

```java
// PreferredGraphicsApiMixin.java — 强制 D3D12 为首选
@Mixin(GdxApplicationParameters.class)
public abstract class PreferredGraphicsApiMixin {
    @Overwrite
    public String getPreferredGraphicsApi() {
        return "D3D12";
    }
}

// GameRendererRenderDebugMixin.java — P16: renderLevel 诊断
//   inject: GameRenderer.render() HEAD → 打印 frame/resourcesLoaded/advanceGameTime/levelNotNull

// MinecraftRunDebugMixin.java — P19: Minecraft.run() 入口
//   inject: Minecraft.run() HEAD → 确认渲染循环是否启动

// MinecraftRunTickDebugMixin.java — P19: 每帧 tick 状态
//   inject: Minecraft.runTick() HEAD → 打印 tick/level/gameLoadFinished/pause

// MinecraftSetLevelDebugMixin.java — P19: 世界加载
//   inject: Minecraft.setLevel() HEAD → 打印 level 是否为 null（黑屏根因排查）

// MinecraftResourceLoadDebugMixin.java — P19: 资源加载完成
//   inject: onResourceLoadFinished/onGameLoadFinished → 确认游戏加载流程

// MinecraftDoWorldLoadDebugMixin.java — P0: 世界加载入口
//   inject: Minecraft.doWorldLoad() HEAD → 确认世界加载路径是否进入（黑屏根因排查，已修复）

// ClientPacketListenerLoginDebugMixin.java — P0: 登录包接收
//   inject: ClientPacketListener.handleLogin() HEAD → 确认服务端登录包是否到达（黑屏根因排查，已修复）

// LevelRendererPrepareChunksDebugMixin.java — P30: 区块预加载
//   inject: LevelRenderer.prepareChunks() → 打印区块预加载状态

// CameraUpdateDebugMixin.java — P30: 摄像机更新
//   inject: Camera.update() → 打印摄像机位置与视锥参数

// FogRendererUpdateDebugMixin.java — P30: 雾效更新
//   inject: FogRenderer.update() → 打印雾效配置与状态

// SectionRenderDispatcherGetSliceDebugMixin.java — P30: 区块切片调度
//   inject: SectionRenderDispatcher.getSlice() → 打印区块切片分配状态
```

Minecraft 选择后端时优先使用 `D3D12`，进而实例化我们的 `Dx12Backend`。

## 已完成功能

| 模块 | 说明 |
|------|------|
| **P0 原生层** | dx12_mc.dll: D3D12 设备创建、texture/buffer/sampler/view 资源管理、命令列表、fence、swapchain |
| **P1 设备自检** | Dx12Native.dx12CreateDevice() 返回适配器名称 + 特性级别 + 自测结果 |
| **P2 资源层** | Dx12GpuTexture/Dx12GpuBuffer/Dx12GpuSampler/Dx12GpuTextureView — 全部通过 JNI 创建真实 D3D12 资源 |
| **P3 命令层** | Dx12CommandEncoderBackend: submit/fence/copyBuffer/copyTexture/writeToTexture/clear/startEndRenderPass/timestamp |
| **P4 管线层** | GLSL→SPIR-V→HLSL→DXBC 编译，Pipeline cache (IdentityHashMap)，Shader source cache (HashMap) |
| **P5 交换链** | Dx12GpuSurface: DXGI flip-model swapchain, 支持 IMMEDIATE/MERCHANTABILITY/RALAXED present mode |
| **P6 Draw 全链路** | Dx12RenderPassBackend: setPipeline + pushDescriptors(UBO/SRV/TexelBuffer) + draw/drawIndexed/drawIndirect |
| **P15 日志分级** | dbgLog(ERROR/WARN)/dbgLogInfo/INFO)/dbgLogDebug(DEBUG) 三级输出，DX12_LOG_VERBOSE=1 开启详细日志 |
| **P16 renderLevel 诊断** | 帧计数器（每 30 帧）+ advanceGameTime/pause/native submit+present 日志 |
| **P17 绘制目标跟踪** | activeColorTargetsTouched 向量 + dbgReadbackSurfacePixels() 每 30 帧采样，dx12ReadbackTexturePixels 读指定纹理 |
| **P18 Surface Fence** | per-backbuffer fence 追踪，acquireSurface 前检查 back buffer 是否仍被 GPU 使用 |
| **P20 描述符偏移修复** | pushDescriptors 根描述符表 GPU 地址加偏移，避免读取前帧残留描述符导致黑屏 |
| **P21 深度测试修复** | 移除 depthClearFlag=0 的无条件 clear=1.0 回退（pre-clear 已由 MC 完成）；修复 reverse-Z 比较函数 |
| **P22 描述符堆扩容** | drawHeap 从 2 半区扩展到 4 半区，支持三帧并行飞行（fenceValue%4 交替写入） |
| **BUG-01 semanticNames 修复** | `semanticNames` 补齐改用 `vertex.inputs().size()` 与 spvc remap 基准对齐 |
| **LIGHTMAP-VFLIP 修复** | 光贴图更新 pass（pipeline/lightmap）被 P31 flipY 判定排除 → GL bottom-up 内容写入方向与 terrain 采样相反，白天采到暗行（lightmapviz≈26-46、whitelight 探针恢复明亮、readback BMP 证实内容渐变正确但镜像）。修复：frag_main 注入 `texCoord.y = 1.0f - texCoord.y;`（location 含 lightmap 且 !flipY 时自动生效） |
| **Shader 编译自检** | 内嵌 core/gui + core/position_tex_color 的 GLSL 源码，createDevice 时资源包未加载也可编译 |
| **4 轮自测** | Java 资源(self-test)→命令层→管线→Surface 逐轮验证，任何失败立即终止并回退 OpenGL |
| **D3D12 资源管理** | AutoCloseable 模式 + TextureView 引用计数 + gPendingDeletes 延迟销毁 |
| **ModMenu 集成** | Dx12Config + Dx12SettingsScreen + ModMenuApi entrypoint |
| **AA 模式持久化** | Properties 格式存储到 `config/gl4dx12.properties`，4 种模式 (None/FXAA/SMAA/TAA) |
| **异步渲染 Bug 修复** | ✅ 已完成 | ASYNC-01（per-worker CPU-only 镜像堆）+ ASYNC-03（非阻塞 tryBeginFrame）+ ASYNC-04（SetEventOnCompletion 事件驱动）+ ASYNC-05（COM 资源主线程释放）+ ASYNC-09（CommandSignature 参数修复）+ ASYNC-10（PSO 边界切分 Bundle） |
| **CS_OWNDC 兼容性** | ✅ 已完成 | Windows CS_OWNDC 类窗口的 DX12 swapchain 创建失败修复 |
| **shader 诊断探针** | ✅ 已完成 | whitelight/uv2viz/lightmapviz 探针 + DIAG_GRAY/UV2_VIZ/LIGHTMAP_VIZ/DUMP_SHADER 等环境变量驱动 |

## 关键设计原则

- **Mixin 仅修改 API 选择**：不复写任何渲染逻辑，完全依赖 Minecraft 官方 `RenderSystem` 调用我们的后端
- **镜像官方 Vulkan 后端**：每个 Java 类都对应官方 `com.mojang.blaze3d.vulkan.Vulkan*` 的等价实现
- **资源安全释放**：所有 `AutoCloseable`，close() 时销毁所有 D3D12 句柄；TextureView 引用计数 + 延迟销毁队列防 use-after-free
- **自测驱动开发**：4 轮自测（资源/命令/管线/Surface）在 `createDevice()` 中逐轮验证，失败即终止
- **Lazy shared encoder**：`createCommandEncoder()` 返回同一实例（镜像官方语义），避免每帧新建 CommandContext 导致的命令丢失
- **Shader cache**：Pipeline cache (IdentityHashMap) + Shader source cache (HashMap)，同一 shader 只编译一次
- **DLL 版本隔离**：每次启动从 JAR 重新提取，确保 DLL 与 JAR 版本一致
- **三帧并行命令**：3 allocator + value-2 完成等待；drawHeap x4 半区防三帧并行冲突
- **SRV 写当前堆**（P24）：pushDescriptors 将 SRV 写入命令列表当前激活的 drawHeap，避免跨帧描述符错乱（黑屏根因之一）
- **Async Phase 1 设计**（P32）：Bundle 录制按 PSO 边界切分（ASYNC-10）；Per-worker CPU-only 镜像描述符堆（ASYNC-01）；非阻塞 `tryBeginFrame()`（ASYNC-03）；`SetEventOnCompletion` 事件驱动 fence 等待（ASYNC-04）；COM 资源在主线程释放（ASYNC-05）
- **诊断开关**：`DX12_LOG_VERBOSE=1` 开启详细日志；`DX12_DIAG_GREEN=1` 开启绿色着色器诊断；`DX12_DBG_DISABLE_DEPTH=1` 强制关闭深度测试（排查深度配置问题）

## 调试与验证

### 启动日志关键输出

模组加载时依次打印：

```
[dx12] Loading D3D12 mod...
[dx12] Preferred graphics API: D3D12
[dx12] dx12CreateDevice() result: <适配器名称> (D3D_FEATURE_LEVEL <级别>); SELF-TEST OK (...)
[dx12] 创建 GpuBackend: dx12.Dx12Backend
[dx12] Java resource self-test OK (texture/buffer/sampler/view via JNI)
[dx12] Command layer self-test OK (submit/fence/copy/readback via JNI)
[dx12] Pipeline self-test OK (GLSL->SPIR-V->HLSL->DXBC->PSO for core/gui + core/position_tex_color)
[dx12] Surface self-test OK (DXGI swapchain configure/acquire/blit/present via JNI)
[dx12] D3D12 backend initialized successfully.
[dx12] Device name: <适配器名称>
[dx12] [P16] Frame #N submit (queueFence=X) / present idx=Y suboptimal=false
[dx12] [P17] activeColorTargetsTouched=[true] / dbgReadback center=[r,g,b,a]
[dx12-debug] Minecraft.setLevel() called: level=<non-null>   ← P19 诊断（level=null 已修复）
[dx12-debug] runTick tick=<N> gameLoadFinished=<true> level=<non-null> pause=<false>  ← P19 诊断
[dx12-debug] LevelRenderer.prepareChunks() ...   ← P30 区块诊断
[dx12-debug] Camera.update() pos=... frustum=...  ← P30 摄像机诊断
```

### 自测失败时的行为

任何一轮自测失败都会：
1. 关闭已创建的 D3D12 设备
2. 抛出 `BackendCreationException`
3. Minecraft 自动回退到 OpenGL 渲染
4. 日志中打印完整错误堆栈

### 额外诊断日志

除上述标准输出外，还支持以下诊断手段：
- **额外诊断日志**
  - **分级日志**：`DX12_LOG_VERBOSE=1` 开启详细日志，同时写入 `%TEMP%\dx12-native.log`
  - **3×3 采样读回**：`dbgReadbackSurfacePixels()` 每 30 帧采样中心 3×3 像素的 RGBA，用于验证 backbuffer 内容
  - **纹理读回**：`dbgReadbackTexturePixels(tag)` 读取任意纹理并输出 3×3 采样 + 保存 BMP 文件（`dx12_dump_<tag>.bmp`）
  - **帧计数器**：每 30 帧打印 submit/present 状态，方便判断渲染循环是否正常工作
  - **pushDescriptors 诊断**：每帧首次 pushDescriptors 打印 binding 数量和 UBO/SRV 指针
  - **深/读回诊断**：`dx12ReadbackBufferBytes()` 读回 UBO/CBV 原始字节用于调试；`DX12_DBG_DISABLE_DEPTH=1` 强制关闭深度测试

### 4 轮自测详解

| 轮次 | 测试内容 | 失败原因示例 |
|------|----------|-------------|
| **1. Java 资源** | texture/buffer/sampler/view 创建 + map/unmap 读写 | DLL 加载失败、D3D12 初始化失败 |
| **2. 命令层** | submit + fence wait + buffer copy readback + texture write/readback | 命令列表提交失败、copy 数据不匹配 |
| **3. 管线** | core/gui + core/position_tex_color 全链路编译（GLSL→SPIR-V→HLSL→DXBC→PSO） | shaderc/spvc/D3DCompile 编译失败 |
| **4. Surface** | swapchain 创建 → configure → acquire → blit → submit → present | HWND 无效、DXGI 创建 swapchain 失败 |

## 已知问题

| 问题 | 严重度 | 说明 | 状态 |
|------|--------|------|------|
| **`minecraft.level` 始终为 null** | ✅ 已修复 | level=null 根因已定位并修复，世界地形/区块/实体正常渲染（P30） |
| **`beginRenderPass` activeColorTargets 重复 push_back** | 🟠 中 | L1701 和 L1767 两处均 push_back，导致 endRenderPass 回切多执行一次 barrier（幂等，当前无害） | 🔧 待修 |
| **`toPrimitiveTopology` case 4 (TRIANGLES) 缺失** | 🟡 低 | switch 无 case 4，落入 default 返回 TRIANGLELIST（结果正确），但防御性不足 | 🔧 待修 |
| **CBV offset 256 对齐未验证** | 🟡 低 | `pushDescriptors` 要求 offset 256 对齐，但未在 native 层断言；若 Java 侧传入未对齐值可能导致 shader 读到错误数据 | 🔧 待修 |
| **`createFence` 使用全局队列 fence** | 🟠 中 | 多 encoder 场景下 fence 可能由其他 encoder 的 submit 提前完成 | 🔧 待修 |
| **临时 encoder 与全局 fence 竞争** | 🟠 中 | `createBuffer(data)` 的临时 submit 会递增全局 `queueFenceValue`，可能与 `StagedVertexBuffer` fence 竞态 | 🔧 待修 |
| **transientMemory queuedFrames 泄漏** | 🟡 低 | `transientMemory.close: queuedFrames=1` 每帧出现，表明 1 路 ubo ring buffer 未被回收 | � 待修 |
| **`d3d12` 实验包（已迁移）** | 🔵 低 | `com.xgdt.dx12.d3d12.*` 是 JNA 探索的半成品，仍通过 JNI 调用，`close()` 是空操作 | ⚠️ 待清理 |
| **大量调试日志残留** | 🔵 低 | `getStackTrace()`/高频打印散落在渲染路径，影响性能 | ⚠️ 待清理 |
| **`ensureDevice` 无幂等保护** | 🟡 低 | 中途创建失败后重试会泄漏首次创建的所有 D3D12 资源 | ⚠️ 已知 |
| **`dx12_mc_new.dll` 残留** | 🔵 低 | 旧版重命名 DLL 仍存在，需清理 | ⚠️ 待清理 |

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 游戏 | Minecraft | 26.2 |
| 加载器 | Fabric Loader | 0.19.3 |
| API | Fabric API | 0.156.0+26.2 |
| Mixin | SpongePowered Mixin | 0.8.7 (via Fabric 0.17.3) |
| 模组浏览器 | ModMenu | 20.0.1 |
| 语言 | Java | 26 |
| 原生层 | C++ D3D12 (JNI) | dx12_mc.dll（预编译） |
| Shader 编译 | shaderc (GLSL→SPIR-V) | LWJGL shaderc |
| Shader 反射 | spvc (SPIR-V→HLSL) | LWJGL spvc |
| 依赖 | JNA | 5.17.0（预留，暂未用于生产） |

## 路线图

### P0-P33 + Async Phase 1（C++）: D3D12 后端核心层 ✅ 全部完成

| 阶段 | 状态 | 说明 |
|------|------|------|
| **P0-P6: 原生层到 Draw 全链路** | ✅ | 完整 D3D12 设备/资源/命令/管线/表面/draw |
| **P15-P28: 诊断增强** | ✅ | 日志分级/帧计数/绘制跟踪/Surface fence/描述符偏移/深度测试/堆扩容/帧级瞬态RTV/高频日志降级/图集渲染与坐标系适配 |
| **P29-P33: NDC修复/世界渲染/Async C++** | ✅ | NDC Y-flip/level=null 根因修复/world terrain 正常渲染/Async Phase 1 C++ 基础设施 |
| **BUG-01: semanticNames** | ✅ | 补齐逻辑与 spvc 基准对齐 |
| **LIGHTMAP-VFLIP** | ✅ | 光贴图 Y-flip 注入修复，白天地形亮度正常 |
| **自测通过** | ✅ | GUI + GUI_TEXTURED 管线 + surface blit + buffer copy + texture readback |
| **Async 7 Bug 修复** | ✅ | ASYNC-01/03/04/05/09/10 全部修复 |

### 🔜 后续优化

| 任务 | 优先级 | 说明 |
|------|--------|------|
| **Async Phase 2: 2 线程并行** | 🔴 P0 | Worker 只录命令，主线程写描述符；需实现 Java 侧 async/* 包（BundleRecorder、AsyncDispatcher 等） |
| **Async Phase 3: N 线程全量异步** | 🔜 待评估 | Per-worker 镜像堆 + 批量 CopyDescriptorsSimple + 完整 fence 管理 |
| **BUG-01 重复 push_back 修复** | P2 | 删除 beginRenderPass 诊断循环中的重复 activeColorTargets.push_back |
| **BUG-02 case 4 补齐** | P3 | toPrimitiveTopology 补全 TRIANGLES case |
| **BUG-06 CBV 对齐验证** | P2 | native 层添加 offset%256 断言 |
| **诊断日志降级** | P1 | 将高频 `getStackTrace()`/readback 改为条件日志（环境变量控制） |
| **性能基准测试** | P1 | 对比 GL/Vulkan/D3D12 的 FPS、内存占用、GPU 利用率 |

### 🎯 最终目标

| 阶段 | 目标 | 状态 |
|------|------|------|
| **过渡方案** | 通过 GpuBackend 接口完全接管 Minecraft 渲染（当前阶段） | ✅ 已完成 |
| **功能完整** | terrain/entity/particle 全量 shader 渲染正常 + Async Phase 1 Java 接入 | 🔜 进行中 |
| **正式发行** | 通过官方渠道发布为 Minecraft 26.2+ 的 D3D12 后端模组 | 🔜 待实现 |

---
### 如何编译

#### 在编译之前你需要确保你的电脑上安装了MSVC编译器和cmake和Gradle编译器

先安装cmake，然后执行以下命令编译项目：
```bash
//在此之前先删除native目录下的 build 目录,确保编译的dll是最新的
cd native
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release
```
编译的产物在这个路径下：
```bash
\native\build\bin\Release\dx12_mc.dll
``` 
把它复制到\fabric\src\main\resources下如有直接覆盖即可
安装Gradle，然后执行以下命令编译：
```bash
cd fabric
gradlew clean build
```
在这里找到你编译的模组文件
```bash
\fabric\build\libs\gl4dx12-0.1.0.jar
```

## 贡献指南

> 暂时不接受任何贡献

## 许可证

本项目采用 **Apache License 2.0** 协议。

- 你可以自由使用、修改、分发本项目，包括商业用途。
- 分发时必须保留本项目的版权声明、许可证文本和 NOTICE 文件中的归属信息。
- 不得使用本项目的名称或作者名义为衍生品做背书或推广。
- 详见 [LICENSE](LICENSE) 和 [NOTICE](NOTICE)。