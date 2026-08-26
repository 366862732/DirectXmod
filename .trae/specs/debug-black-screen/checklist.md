# Checklist

## 第一阶段：验证 Backbuffer 通路
- [ ] 1.1: `blitSurface` 清空 Backbuffer 为纯红色，屏幕显示红色
- [ ] 1.2: `blitSurface` 复制测试纹理到 Backbuffer，屏幕显示青色三角形

## 第二阶段：验证 src 纹理内容
- [ ] 2.1: 读回 src 纹理前 16 像素，日志显示非零 RGBA 值
- [ ] 2.2: src 纹理格式为 `DXGI_FORMAT_R8G8B8A8_UNORM`，非深度/单通道格式

## 第三阶段：验证交换链与 Present
- [ ] 3.1: SwapChain 格式为 `DXGI_FORMAT_R8G8B8A8_UNORM`
- [ ] 3.2: Present 时 Backbuffer 尺寸与窗口一致
- [ ] 3.3: ResizeBuffers 在窗口变化时正确调用且无错误

## 第四阶段：Blit 管线修复（2026-08-26 根因）
- [x] 4.1: 根签名 descriptor table 不再混用 SRV+SAMPLER，`D3D12SerializeRootSignature` 不再返回 E_INVALIDARG
- [x] 4.2: `gCtx.blitPipeline` 已分配，无 null 解引用
- [ ] 4.3: 日志出现 `blitSurface: set blit rootSig=` 与 `blitSurface: drawIndexed done`
- [x] 4.4: DLL（同步 resources/、dx12mod/）与 JAR 已重建部署到 deploy/
- [ ] 4.5: 游戏画面正常显示（用户验证）

## 清理
- [ ] 所有诊断代码已移除，native 库恢复正常状态
- [ ] 根本问题已修复，游戏画面正常显示
