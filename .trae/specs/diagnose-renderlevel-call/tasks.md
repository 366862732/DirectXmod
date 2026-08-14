# Tasks

- [x] Task 1: 增强 GameRendererRenderDebugMixin 诊断信息
  - [x] 1.1 新增 `advanceGameTime` 参数打印（`render(DeltaTracker, boolean advanceGameTime)` 第二个参数）
  - [x] 1.2 新增渲染帧计数器，每 30 帧打印一次关键状态摘要
  - [x] 1.3 新增 `isPaused` 状态检测（`minecraft.options.isPauseShortcut` 或 pause 标志）
  - [x] 1.4 编译验证：`gradlew compileJava` 通过

- [x] Task 2: 添加 render pass 生命周期诊断（native 侧）
  - [x] 2.1 在 `beginRenderPass` / `endRenderPass` 入口加结构化日志（`[dx12] beginRenderPass: colorCount=X w=Y h=Z`）
  - [x] 2.2 在 `submitCommandList` 前打印当前 fence 值和 queueFence
  - [x] 2.3 在 `presentSurface` 后打印 swapchain state（`currentImageIndex`, `suboptimal` 标志）
  - [x] 2.4 编译验证：`cmake --build build --config Release` 通过

- [x] Task 3: 添加 command encoder 提交诊断（Java 侧）
  - [x] 3.1 `Dx12CommandEncoderBackend.submit()` 增加日志：frame counter + encoder fence value
  - [x] 3.2 `Dx12GpuSurface.present()` 增加日志：是否成功 present
  - [x] 3.3 编译验证：`gradlew compileJava` 通过

- [x] Task 4: 构建并部署
  - [x] 4.1 重新构建 native DLL
  - [x] 4.2 复制 DLL 到 fabric/src/main/resources/
  - [x] 4.3 `gradlew jar` 构建 JAR
  - [x] 4.4 更新 test-deploy.ps1 适配 C++/D3D12 路径
  - [ ] 4.5 手动部署：运行 `powershell -File test-deploy.ps1` 将新 JAR+DLL 推到测试实例
  - [ ] 4.6 游戏启动并进入世界，收集日志，确认 `advanceGameTime=true` 和 `renderLevel()` 调用
