# Checklist

## 读回时机修复
- [x] 1.1: `DIAG_READBACK_COLOR_TEX` 块已移到 `CopyTextureRegion` 之前
- [x] 1.2: 读回发生在 `transitionTextureTo(COPY_SOURCE)` 完成之后
- [x] 1.3: native 库重新编译无报错
- [x] 1.4: JAR 包已包含更新后的 DLL（202752 bytes）

## 诊断日志验证
- [x] 2.1: `blitSurface` 打印 srcTex 指针、尺寸、格式、`colorTargetsWritten`
- [x] 2.2: readback 日志有 `before-copy` 标签
- [x] 2.3: native 库重新编译并部署完成
- [x] 2.4: 修复 `DIAG_READBACK_COLOR_TEX` 块 null 守卫（srcTex=null 时跳过读回）
- [x] 2.5: 修复 `Dx12Backend.java` 编译错误（`getUsage()` → `usage()`）
- [x] 2.6: JAR 构建成功（1273973 bytes），已部署到 deploy/

## 功能验证
- [ ] 3.1: self-test 红色画面正常（DIAG_CLEAR 路径未受影响）
- [ ] 3.2: 无新增 D3D12 validation errors
- [ ] 3.3: splash 阶段渲染画面可见（绿色）

## 清理
- [ ] 4.1: 临时诊断代码已移除或宏开关可控
- [ ] 4.2: 无诊断代码时游戏正常运行

## Bug 修复（2026-08-26）
- [x] 5.1: 修复 `selfTestCommandLayer` 中 src buffer 缺少 `USAGE_COPY_SRC`——原仅有 `USAGE_COPY_DST`，导致 D3D12 copyBufferToBuffer 读取无效，readback 前 252 字节全零
- [x] 5.2: 重新构建 JAR（1273973 bytes），已部署到 deploy/
- [x] 6.1: 修复 native `copyBufferToBuffer` P23 NaN 清洗——原把 UPLOAD 数据中按 float 读为 NaN 的字节（0xFC,0xFD,0xFE,0xFF）改写为 0.0f，静默损坏数据导致 readback mismatch at 252；改为只检测不修改
- [x] 6.2: Java `checkForNanInfinity` 改为进程内仅提示一次、移除 `Thread.dumpStack()`，避免合法字节模式反复误报
- [x] 6.3: native 重新编译（VS 18 2026），DLL 203264 bytes，已同步 resources/、dx12mod/
- [x] 6.4: JAR 重建（1273763 bytes）已部署到 deploy/，内嵌 DLL = 203264 bytes 一致

## 功能验证（待用户运行游戏）
- [ ] 7.1: self-test 全部通过（resource + command layer + pipeline + surface），不再回退 GL
- [ ] 7.2: splash 阶段渲染画面正常
- [ ] 7.3: 无新增 D3D12 validation errors
