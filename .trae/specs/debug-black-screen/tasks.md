# Tasks

## 第一阶段：验证 Backbuffer 通路

- [x] Task 1.1: 修改 `blitSurface` 清空 Backbuffer 为纯红色
  - [x] 在 `blitSurface` 函数开头调用 `ClearRenderTargetView`，颜色 (1.0, 0.0, 0.0, 1.0)
  - [x] 重新编译 native 库并运行游戏
  - [ ] 确认屏幕是否为纯红色（需用户测试）
  - [ ] 若变红，记录结果并继续 Task 1.2；若仍黑屏，跳到第三阶段

- [ ] Task 1.2: 用已知测试纹理复制到 Backbuffer
  - [ ] 在 `blitSurface` 中创建或加载一个青色三角形测试纹理
  - [ ] 使用 `CopyTextureRegion` 将该纹理复制到 Backbuffer
  - [ ] 确认屏幕是否显示青色三角形
  - [ ] 若成功，Backbuffer 通路正常，跳到第二阶段；若失败，跳到第三阶段

## 第二阶段：验证 src 纹理内容

- [ ] Task 2.1: 读回 src 纹理像素
  - [ ] 在 `blitSurface` 中添加读回逻辑，读取 src 纹理前 16 个像素
  - [ ] 将像素值 (RGBA) 打印到日志文件
  - [ ] 确认像素值是否非零
  - [ ] 若全零，渲染结果为空，检查 beginRenderPass 绑定的 RenderTarget
  - [ ] 若有颜色，继续 Task 2.2

- [ ] Task 2.2: 检查 src 纹理格式
  - [x] 在 `blitSurface` 中获取 src 纹理的 DXGI_FORMAT（已有 fmt=%d 日志）
  - [ ] 若格式不是 `DXGI_FORMAT_R8G8B8A8_UNORM` 等彩色格式，打印警告
  - [ ] 若格式不对，定位调用方并确保传入正确的彩色输出纹理

## 第三阶段：验证交换链与 Present

- [x] Task 3.1: 确认 SwapChain 格式
  - [x] 在 `configureSurface` 中打印 `swapChainDesc.BufferDesc.Format`
  - [ ] 确认是否为 `DXGI_FORMAT_R8G8B8A8_UNORM`

- [x] Task 3.2: 检查 Present 时 Backbuffer 状态
  - [x] 在 `presentSurface` 中调用 `GetDesc` 打印 Backbuffer 格式和尺寸
  - [ ] 确认与窗口尺寸一致

- [ ] Task 3.3: 检查 ResizeBuffers 调用
  - [x] 已在 configureSurface 中添加日志（Retry/ok 日志）
  - [ ] 确认窗口尺寸变化时正确调用且无错误

## 清理与回归

- [ ] 移除所有诊断代码，恢复原有实现
- [ ] 根据排查结果修复根本问题
- [ ] 验证修复后画面正常显示
