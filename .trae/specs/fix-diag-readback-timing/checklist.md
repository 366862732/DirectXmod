# Checklist

## 读回时机修复
- [x] 1.1: `DIAG_READBACK_COLOR_TEX` 块已移到 `CopyTextureRegion` 之前
- [x] 1.2: 读回发生在 `transitionTextureTo(COPY_SOURCE)` 完成之后
- [x] 1.3: native 库重新编译无报错
- [x] 1.4: JAR 包已包含更新后的 DLL（187392 bytes）

## 诊断日志验证
- [x] 2.1: `blitSurface` 打印 srcTex 指针、尺寸、格式、`colorTargetsWritten`
- [x] 2.2: readback 日志有 `before-copy` 标签
- [ ] 2.3: splash 阶段 readback 显示绿色（G>0）而非全黑（需用户运行游戏验证）

## 功能验证
- [ ] 3.1: self-test 红色画面正常（DIAG_CLEAR 路径未受影响）
- [ ] 3.2: 无新增 D3D12 validation errors
- [ ] 3.3: splash 阶段渲染画面可见（绿色）

## 清理
- [ ] 4.1: 临时诊断代码已移除或宏开关可控
- [ ] 4.2: 无诊断代码时游戏正常运行
