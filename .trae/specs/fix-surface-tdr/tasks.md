# Tasks

- [x] T1: 反编译 MC 26.1.2 源码，确认 `Minecraft.runTick()` 中 `glfwSwapBuffers` 的精确位置和调用方式
  - [x] 执行 `gradlew genSources` — 缓存命中，无需重复反编译
  - [x] 搜索 `minecraft-merged.jar` 定位 `glfwSwapBuffers` 调用点
  - [x] **确认结果**：swap 在 `GlDevice.presentFrame()` 方法中 → `GLFW.glfwSwapBuffers()`。`Minecraft.runTick()` 内部调用 `GlDevice.presentFrame()` 完成 swap。`Window.updateDisplay()` 已移除。

- [x] T2: 实现 GL swap 抑制 Mixin
  - [x] 创建 `GlDeviceMixin.java`：`@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")`，Inject at `presentFrame()` HEAD with `cancellable = true`
  - [x] 条件：`D3D12Bridge.hasSurface() && mc.level != null && mc.player != null`
  - [x] 注册到 `gl4dx12.mixins.json`

- [x] T3: Rust `render_surface()` TDR 容错处理
  - [x] 处理 `wgpu::SurfaceError::Timeout` — 记录警告并跳过帧，等待 Lost/Outdated
  - [x] surface configure 失败时降级到 offscreen 模式（`self.surface = None; self.surface_depth = None; self.surface_config = None`）

- [x] T4: 编译部署与测试
  - [x] 编译 Rust DLL (`cargo build --release`) — **成功**
  - [x] 编译 Java JAR (`gradlew build`) — **BUILD SUCCESSFUL**
  - [x] 部署到 `run/mods/`
  - [ ] 启动游戏进入世界测试：连续运行 >5 分钟无 TDR — **需用户执行**

# Task Dependencies
- T2 依赖 T1 ✓
- T3 与 T1/T2 无依赖，可并行 ✓
- T4 依赖 T2 + T3 ✓
