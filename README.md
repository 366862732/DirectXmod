# DirectXMod (gl4dx12)

> **Minecraft 26.2 Fabric 模组的原生 D3D12 渲染后端。**
> 通过实现 Minecraft 官方的 `GpuBackend` / `GpuDeviceBackend` 接口，用 C++ 原生 DLL（`dx12_mc.dll`）替代 OpenGL/Vulkan 渲染路径，使 Minecraft 使用 D3D12 直接渲染游戏画面，同时保留 Mod 扩展点（AA 设置、ModMenu 集成）。

## 概述

本项目采用 **Mixin 注入** 方式修改 Minecraft 的图形 API 选择逻辑，让游戏改用 D3D12 后端渲染，同时通过 Java 层提供渲染状态同步、配置管理和 Mod 兼容层。

### 核心特点

- **Mixin 注入**：仅需 1 个 Mixin（`PreferredGraphicsApiMixin`）将 D3D12 设为首选 API，Minecraft 原生渲染管线直接驱动我们的后端
- **官方 GpuBackend 接口**：完整实现 `GpuBackend` / `GpuDeviceBackend` / `CommandEncoderBackend` / `RenderPassBackend` / `GpuSurfaceBackend` 等官方接口
- **零自定义渲染循环**：不复写 `RenderSystem`，完全接管 Minecraft 官方的渲染流程
- **Shaderc + Spvc 编译链**：GLSL → SPIR-V（shaderc）→ HLSL SM5.1（spvc）→ DXBC（D3DCompile）
- **资源安全**：所有 D3D12 资源通过 Java 包装类（`Dx12Gpu*`）管理，随 Minecraft 生命周期自动释放
- **配置界面**：Fx12Config 持久化配置，Dx12SettingsScreen 设置界面，ModMenu 集成
- **DLL 自动加载**：从 JAR 提取到 `{user.dir}/dx12mod/dx12_mc.dll`，支持版本隔离
- **MC 26.2**：支持 Mojang 官方映射（不依赖 Yarn 映射），fabric-api 0.156.0+26.2 / loader 0.19.3 / ModMenu 20.0.1

### 当前阶段：P0-P18 全部完成，GUI 标题界面可正常渲染（2026-08-21）

| 阶段 | 状态 | 说明 |
|------|------|------|
| **P0: 原生层基础** | ✅ 已完成 | dx12_mc.dll：D3D12 初始化 + 资源创建 (texture/buffer/sampler/view) |
| **P1: 设备自检** | ✅ 已完成 | Dx12Native.dx12CreateDevice() + 资源 self-test |
| **P2: 资源层** | ✅ 已完成 | Dx12GpuTexture / Dx12GpuBuffer / Dx12GpuSampler / Dx12GpuTextureView |
| **P3: 命令编码层** | ✅ 已完成 | Dx12CommandEncoderBackend：submit/fence/copy/clear/timestamp |
| **P4: 管线编译层** | ✅ 已完成 | shaderc SPIR-V → spvc 反射/rebind → HLSL → D3DCompile DXBC |
| **P5: 交换链层** | ✅ 已完成 | Dx12GpuSurface：DXGI flip-model swapchain (configure/acquire/blit/present) |
| **P6: Draw 全链路** | ✅ 已完成 | Dx12RenderPassBackend：setPipeline + pushDescriptors + draw/drawIndexed |
| **P15: 日志分级系统** | ✅ 已完成 | GameRenderer 调试插桩 + 分级日志 (log4j2) |
| **P16: renderLevel 诊断** | ✅ 已完成 | 帧计数/advanceGameTime/pause/native submit+present 日志 |
| **P17: 绘制目标跟踪** | ✅ 已完成 | Draw target tracking + GPU 同步修复 + 调试日志 |
| **P18: Surface fence** | ✅ 已完成 | per-backbuffer fence 追踪，避免 acquire 重用未完成的 back buffer |
| **自测通过** | ✅ | GUI + GUI_TEXTURED 管线编译 + surface blit + buffer copy + texture readback 全部通过 |

## 项目结构

```
dx12-lib-template-26.1.2/
├── fabric/                          # Fabric 模组（Java）
│   ├── src/main/java/com/dx12/
│   │   ├── Dx12Mod.java            # 模组入口，设置图形 API 偏好 + 日志自检信息
│   │   ├── ModMenuIntegration.java # ModMenuApi 实现，配置界面集成
│   │   ├── config/
│   │   │   └── Dx12Config.java     # 配置持久化（aa_mode 等，Properties 格式）
│   │   ├── gui/
│   │   │   └── Dx12SettingsScreen.java # AA 模式设置 GUI（None/FXAA/SMAA/TAA）
│   │   ├── mixin/
│   │   │   ├── PreferredGraphicsApiMixin.java  # 将 D3D12 设为首选图形 API
│   │   │   ├── GameRendererRenderDebugMixin.java  # P16/P17: renderLevel 诊断
│   │   │   └── BufferBuilderMixin.java          # P17: 绘制目标跟踪插桩
│   │   └── dx12/                    # D3D12 后端核心实现（镜像官方 Vulkan 后端）
│   │       ├── Dx12Native.java        # JNI 桥接层（66 native 方法，335 行）
│   │       ├── Dx12Backend.java       # GpuBackend 实现：窗口/设备创建 + 4 轮自测
│   │       ├── Dx12Device.java        # GpuDeviceBackend 实现：资源创建 + Shader 编译缓存
│   │       ├── Dx12ShaderCompiler.java  # GLSL→SPIR-V→HLSL 编译链（shaderc+spvc，760 行）
│   │       ├── Dx12IntermediaryShaderModule.java  # SPIR-V 反射绑定信息（spvc 语义注入，355 行）
│   │       ├── Dx12CompiledShader.java    # 编译产物（HLSL 源码 + 绑定列表）
│   │       ├── Dx12CompiledRenderPipeline.java  # 编译后的渲染管线
│   │       ├── Dx12BindGroupEntry.java    # 管线绑定条目（UBO/SRV/TexelBuffer）
│   │       ├── Dx12GpuSurface.java      # DXGI swapchain（P5+P17，130 行）
│   │       ├── Dx12CommandEncoderBackend.java   # 命令编码层（P3，342 行）
│   │       ├── Dx12RenderPassBackend.java     # 渲染通道层（P6+P17，393 行）
│   │       ├── Dx12TransientMemory.java     # 瞬时内存管理（per-frame 缓冲回收，209 行）
│   │       ├── Dx12GpuTexture.java        # D3D12 纹理资源包装（47 行）
│   │       ├── Dx12GpuTextureView.java    # D3D12 纹理视图（SRV，43 行）
│   │       ├── Dx12GpuBuffer.java         # D3D12 缓冲区资源包装（61 行）
│   │       ├── Dx12GpuSampler.java        # D3D12 采样器包装（86 行）
│   │       └── Dx12GpuQueryPool.java      # D3D12 GPU 时间戳查询池（72 行）
│   │   └── d3d12/                      # 实验性纯 Java D3D12 封装（暂未接入生产）
│   │       ├── Dx12Device.java            # 适配器枚举 + 设备工厂
│   │       ├── Dx12DeviceContext.java     # 完整上下文（device+queue+surface）
│   │       ├── Dx12AdapterInfo.java       # 适配器信息
│   │       └── Dx12Exception.java         # 异常包装
│   ├── src/test/java/com/dx12/d3d12/
│   │   └── Dx12DeviceTest.java          # 单元测试（渲染循环验证）
│   ├── src/main/resources/
│   │   ├── fabric.mod.json         # Fabric 模组描述（client + modmenu entrypoints）
│   │   ├── gl4dx12.mixins.json     # Mixin 配置（3 个 mixin）
│   │   └── assets/dx12mod/icon.png # 模组图标（16x16）
│   ├── libs/
│   │   └── modmenu-20.0.1.jar     # ModMenu 本地依赖（MC 26.2）
│   ├── build.gradle                # Gradle 构建配置 (loom 1.15.5)
│   └── gradle.properties           # 版本参数
├── native/                          # D3D12 原生层（C++17，MSVC + CMake）
│   ├── src/
│   │   ├── dx12_device.cpp         # D3D12 设备/资源/命令/管线/Draw（~3000 行）
│   │   ├── dx12_device.h           # 公共头文件（367 行，定义 DeviceContext/CommandContext 等）
│   │   ├── dx12_surface.cpp        # DXGI swapchain + blit + present + 读回诊断（~470 行）
│   │   ├── jni_bridge.cpp          # JNI 入口（112 行）
│   │   ├── jni_bridge_p3.cpp       # P3 命令层 native（293 行）
│   │   ├── jni_bridge_p4.cpp       # P4 管线编译 native（168 行）
│   │   ├── jni_bridge_p5.cpp       # P5 交换链 + 读回 native（376 行）
│   │   └── jni_bridge_p6.cpp       # P6 绘制 native（156 行）
│   ├── CMakeLists.txt              # CMake 构建配置
│   └── build/bin/Release/dx12_mc.dll  # 预编译 DLL（从 JAR 提取部署）
├── docs/
│   ├── official-262/               # Minecraft 26.2 官方源码参考
│   ├── code-review-deep.md         # 深度代码审查报告（BUG-01 ~ BUG-17）
│   └── 步骤.md                     # 详细开发步骤文档
└── debug.log                       # 运行时诊断日志
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

三个 Mixin 在 Minecraft 初始化/渲染阶段修改行为：

```java
// PreferredGraphicsApiMixin.java — 强制 D3D12 为首选
@Mixin(GdxApplicationParameters.class)
public abstract class PreferredGraphicsApiMixin {
    @Overwrite
    public String getPreferredGraphicsApi() {
        return "D3D12";
    }
}

// GameRendererRenderDebugMixin.java — P16/P17 渲染诊断
// BufferBuilderMixin.java — P17 绘制目标跟踪
```

Minecraft 选择后端时优先使用 `D3D12`，进而实例化我们的 `Dx12Backend`。

## 已完成功能

| 模块 | 说明 |
|------|------|
| **P0 原生层** | dx12_mc.dll: D3D12 设备创建、texture/buffer/sampler/view 资源管理、命令列表、fence、swapchain |
| **P1 设备自检** | Dx12Native.dx12CreateDevice() 返回适配器名称 + 特性级别 + 自测结果 |
| **P2 资源层** | Dx12GpuTexture/Dx12GpuBuffer/Dx12GpuSampler/Dx12GpuTextureView — 全部通过 JNI 创建真实 D3D12 资源 |
| **P3 命令层** | Dx12CommandEncoderBackend: submit/fence/copyBuffer/copyTexture/writeToTexture/clear/startEndRenderPass/timestamp |
| **P4 管线层** | GLSL→SPIR-V(HLSL→DXBC 编译，Pipeline cache (IdentityHashMap)，Shader source cache (HashMap) |
| **P5 交换链** | Dx12GpuSurface: DXGI flip-model swapchain, 支持 IMMEDIATE/MERCHANTABILITY/RALAXED present mode |
| **P6 Draw 全链路** | Dx12RenderPassBackend: setPipeline + pushDescriptors(UBO/SRV/TexelBuffer) + draw/drawIndexed/drawIndirect |
| **P15 日志分级** | GameRenderer 调试插桩 + 分级日志 (log4j2) |
| **P16 renderLevel 诊断** | 帧计数器（每 30 帧）+ advanceGameTime/pause/native submit+present 日志 |
| **P17 绘制目标跟踪** | activeColorTargetsTouched 向量 + dbgReadbackSurfacePixels() 3×3 采样诊断读回 |
| **P18 Surface Fence** | per-backbuffer fence 追踪，acquireSurface 前检查 back buffer 是否仍被 GPU 使用 |
| **Shader 编译自检** | 内嵌 core/gui + core/position_tex_color 的 GLSL 源码，createDevice 时资源包未加载也可编译 |
| **4 轮自测** | Java 资源(self-test)→命令层→管线→Surface 逐轮验证，任何失败立即终止并回退 OpenGL |
| **D3D12 资源管理** | AutoCloseable 模式，close() 时释放所有 D3D12 句柄 |
| **ModMenu 集成** | Dx12Config + Dx12SettingsScreen + ModMenuApi entrypoint |
| **AA 模式持久化** | Properties 格式存储到 `config/gl4dx12.properties`，4 种模式 (None/FXAA/SMAA/TAA) |
| **退出看门狗** | Dx12Mod: Render thread 消失后 20s 内强制 System.exit(1)，防止启动器卡死 |

## 关键设计原则

- **Mixin 仅修改 API 选择**：不复写任何渲染逻辑，完全依赖 Minecraft 官方 `RenderSystem` 调用我们的后端
- **镜像官方 Vulkan 后端**：每个 Java 类都对应官方 `com.mojang.blaze3d.vulkan.Vulkan*` 的等价实现
- **资源安全释放**：所有 `AutoCloseable`，close() 时销毁所有 D3D12 句柄，防止资源泄漏
- **自测驱动开发**：4 轮自测（资源/命令/管线/Surface）在 `createDevice()` 中逐轮验证，失败即终止
- **Lazy shared encoder**：`createCommandEncoder()` 返回同一实例（镜像官方语义），避免每帧新建 CommandContext 导致的命令丢失
- **Shader cache**：Pipeline cache (IdentityHashMap) + Shader source cache (HashMap)，同一 shader 只编译一次
- **DLL 版本隔离**：每次启动从 JAR 重新提取，确保 DLL 与 JAR 版本一致
- **诊断开关**：`DX12_LOG_VERBOSE=1` 开启详细日志，`DX12_DIAG_GREEN=1` 开启绿色着色器诊断

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
```

### 自测失败时的行为

任何一轮自测失败都会：
1. 关闭已创建的 D3D12 设备
2. 抛出 `BackendCreationException`
3. Minecraft 自动回退到 OpenGL 渲染
4. 日志中打印完整错误堆栈

### 额外诊断日志

除上述标准输出外，还支持以下诊断手段：
- **分级日志**：`DX12_LOG_VERBOSE=1` 开启详细日志，同时写入 `%TEMP%\dx12-native.log`
- **3×3 采样读回**：`dbgReadbackSurfacePixels()` 返回中心 3×3 像素的 RGBA 数组，用于验证 backbuffer 内容
- **帧计数器**：每 30 帧打印 submit/present 状态，方便判断渲染循环是否正常工作

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
| **语义名索引错位** | 🔴 高 | `buildNativeDesc` 中 `semanticNames` 补齐逻辑与 spvc `toHlsl()` 基准不一致，含 4+ 顶点属性的管线可能 `E_INVALIDARG` | 🔧 待修 |
| **`createFence` 使用全局队列 fence** | 🟠 中 | 多 encoder 场景下 fence 可能由其他 encoder 的 submit 提前完成 | 🔧 待修 |
| **临时 encoder 与全局 fence 竞争** | 🟠 中 | `createBuffer(data)` 的临时 submit 会递增全局 `queueFenceValue`，可能与 `StagedVertexBuffer` fence 竞态 | 🔧 待修 |
| **诊断消息与实际不符** | 🟡 低 | `Dx12Backend.java` self-test 日志说"GREEN"但实际走 no-copy pass-through（srcTex=null），不调用 clear | 🔧 待修 |
| **每 30 帧 GPU 同步读回** | 🟡 低 | `readbackSurfacePixels` 在主线程调用 `deviceWaitIdle()`，造成轻微卡顿 | ⚠️ 已知 |
| **静态 command list/allocator 不释放** | 🟡 低 | readback/staging 路径使用函数静态 `ComPtr`，进程存活期间不释放 | ⚠️ 已知 |
| **`ensureDevice` 无幂等保护** | 🟡 低 | 中途创建失败后重试会泄漏首次创建的所有 D3D12 资源 | ⚠️ 已知 |
| **`d3d12` 包是未使用的实验代码** | 🔵 低 | `com.dx12.d3d12.*` 是 JNA 迁移探索的半成品，仍通过 JNI 调用，`close()` 是空操作 | ⚠️ 待清理 |
| **大量调试日志残留** | 🔵 低 | `getStackTrace()`/高频打印散落在渲染路径，影响性能 | ⚠️ 待清理 |

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 游戏 | Minecraft | 26.2 |
| 加载器 | Fabric Loader | 0.19.3 |
| API | Fabric API | 0.156.0+26.2 |
| Mixin | SpongePowered Mixin | 0.8.7 (via Fabric 0.17.3) |
| 模组浏览器 | ModMenu | 20.0.1 |
| 语言 | Java | 25 |
| 原生层 | C++ D3D12 (JNI) | dx12_mc.dll（预编译） |
| Shader 编译 | shaderc (GLSL→SPIR-V) | LWJGL shaderc |
| Shader 反射 | spvc (SPIR-V→HLSL) | LWJGL spvc |
| 依赖 | JNA | 5.17.0（预留，暂未用于生产） |

## 路线图

### P0-P6: D3D12 后端核心层 ✅ 全部完成

| 阶段 | 状态 | 说明 |
|------|------|------|
| **P0: 原生层基础** | ✅ | dx12_mc.dll: D3D12 初始化 + 资源创建 |
| **P1: 设备自检** | ✅ | Dx12Native.dx12CreateDevice() + 资源 self-test |
| **P2: 资源层** | ✅ | Dx12GpuTexture/Dx12GpuBuffer/Dx12GpuSampler/Dx12GpuTextureView |
| **P3: 命令编码层** | ✅ | Dx12CommandEncoderBackend: submit/fence/copy/clear/timestamp |
| **P4: 管线编译层** | ✅ | shaderc SPIR-V → spvc 反射/rebind → HLSL → D3DCompile DXBC |
| **P5: 交换链层** | ✅ | Dx12GpuSurface: DXGI flip-model swapchain |
| **P6: Draw 全链路** | ✅ | Dx12RenderPassBackend: setPipeline + pushDescriptors + draw |
| **P15-P18: 诊断增强** | ✅ | 日志分级/帧计数/绘制跟踪/Surface per-backbuffer fence |
| **自测通过** | ✅ | GUI + GUI_TEXTURED 管线 + surface blit + buffer copy + texture readback |

### 🔜 后续优化

| 任务 | 优先级 | 说明 |
|------|--------|------|
| **完整 Shader 支持** | 🔴 P0 | 当前自检 core/gui + core/position_tex_color，需支持 terrain/entity/particle 等全量 shader |
| **语义名索引修复** | 🔴 P0 | BUG-01：`buildNativeDesc` 与 spvc 语义基准不一致，导致 4+ 属性管线崩溃 |
| **多帧并行命令队列** | P0 | 当前共享单例 encoder + fence 等待，可升级到三缓冲 |
| **清理 d3d12 实验包** | P1 | 移除未使用的 `com.dx12.d3d12` 包和未完成的 JNA 迁移探索 |
| **诊断日志降级** | P1 | 将高频 `getStackTrace()`/readback 改为条件日志（环境变量控制） |
| **性能基准测试** | P1 | 对比 GL/Vulkan/D3D12 的 FPS、内存占用、GPU 利用率 |
| **天空盒渲染** | 🟠 P1 | 简单着色器即可 |
| **实体渲染** | 🟠 P1 | 模型加载 + 骨骼动画 |
| **粒子系统** | 🟡 P2 | 点精灵 (point sprites) |

### 🎯 最终目标

| 阶段 | 目标 | 状态 |
|------|------|------|
| **过渡方案** | 通过 GpuBackend 接口完全接管 Minecraft 渲染（当前阶段） | ✅ 已完成 |
| **性能优化** | 完整 shader 支持 + descriptor 优化 + 帧并行提交 + 诊断降级 | 🔜 进行中 |
| **正式发行** | 通过官方渠道发布为 Minecraft 26.2+ 的 D3D12 后端模组 | 🔜 待实现 |

---

## 贡献指南

> 暂时不接受任何贡献

## 许可证

MIT License
