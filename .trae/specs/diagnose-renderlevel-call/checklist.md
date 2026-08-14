# Checklist

## Task 1: GameRendererRenderDebugMixin 增强
- [x] mixin 打印 `advanceGameTime` 参数值
- [x] mixin 打印帧计数和关键状态摘要
- [x] mixin 检测并打印 pause 状态
- [x] `gradlew compileJava` 无编译错误

## Task 2: Native 渲染 pass 日志
- [x] `beginRenderPass` 入口有结构化日志
- [x] `endRenderPass` 入口有结构化日志
- [x] `submitCommandList` 打印 fence/queueFence 状态
- [x] `presentSurface` 打印 swapchain 状态（每 30 帧）
- [x] CMake 构建无错误，DLL 大小合理（<500KB）

## Task 3: Java command encoder 日志
- [x] `Dx12CommandEncoderBackend.submit()` 有 frame counter 日志
- [x] `Dx12GpuSurface.present()` 有 present 结果日志
- [x] `gradlew compileJava` 无编译错误

## Task 4: 构建部署验证
- [x] native DLL 重新编译成功
- [x] JAR 构建成功（gl4dx12-0.1.0.jar, 1236KB）
- [x] DLL 已复制到 fabric/src/main/resources/
- [x] JAR 已复制到 deploy/ 目录供手动部署
- [ ] 部署到测试实例（需手动运行 test-deploy.ps1）
- [ ] 游戏启动后日志包含新的诊断信息
- [ ] 确认 `advanceGameTime` 在游戏世界中的值（true/false）
- [ ] 确认 `renderLevel()` 是否被调用（通过日志）
