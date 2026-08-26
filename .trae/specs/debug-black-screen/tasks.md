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

## 第四阶段：修复 Blit 根签名与管线分配（2026-08-26 根因定位）

根因：`D3D12SerializeRootSignature` 每帧返回 E_INVALIDARG（0x80070057），`initBlitPipeline` 设置 err → `blitSurface` 提前 return false → backbuffer 永不被绘制 → 黑屏。
E_INVALIDARG 原因：根描述符表同时包含 SRV(t0) 与 SAMPLER(s0) 两种类型 range（D3D12 禁止混用），且 table 的 sampler range 与 static sampler(s0) 寄存器冲突。

- [x] Task 4.1: 移除 `initBlitPipeline` 根签名 descriptor table 中的 SAMPLER range，仅保留 SRV(t0)；static sampler(s0) 继续提供采样器
  - [x] 修改 `dx12_device.cpp` 第 3247-3252 行：删除 `samRange`
  - [x] `table.NumDescriptorRanges = 1`，`ranges[] = { srvRange }`
- [x] Task 4.2: 分配 `gCtx.blitPipeline`（dx12_device.h:111 只声明未分配，写 `->vertBuf` 会 null 解引用）
  - [x] `initBlitPipeline` 内改用本地 `std::unique_ptr<BlitPipeline> bp` 构建全部成员
  - [x] 成功后 `gCtx.blitPipeline = std::move(bp);`
- [x] Task 4.3: `blitSurface` 失败路径补 dbgLog（initBlitPipeline / blitBindSourceTexture 失败时输出错误字符串）
- [x] Task 4.4: 重新编译 native DLL（VS 18 2026），同步 resources/ 与 dx12mod/
- [x] Task 4.5: 重建 JAR 并部署到 deploy/
- [ ] Task 4.6: 用户运行游戏，确认日志出现 `blitSurface: set blit rootSig` / `drawIndexed done`，画面不再黑屏

## 清理与回归

- [ ] 移除所有诊断代码，恢复原有实现
- [ ] 根据排查结果修复根本问题
- [ ] 验证修复后画面正常显示
