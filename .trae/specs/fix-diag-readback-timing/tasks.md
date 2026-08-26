# Tasks

- [x] Task 1: 修复 `DIAG_READBACK_COLOR_TEX` 读回时机
  - [x] 将 `dx12_surface.cpp` 中 `#ifdef DIAG_READBACK_COLOR_TEX` 块从 `CopyTextureRegion` 之后移到 `CopyTextureRegion` 之前
  - [x] 确保移动后读回发生在 `transitionTextureTo(ctx, srcTex, COPY_SOURCE)` 完成之后
  - [x] 保留 `deviceWaitIdle` 同步逻辑不变
  - [x] 重新编译 native 库

- [x] Task 2: 添加读回前后诊断日志
  - [x] 在 `blitSurface` 中 `CopyTextureRegion` 前后添加日志：打印 `srcTex` 指针、尺寸、格式、`ctx->colorTargetsWritten` 值
  - [x] 在 readback 日志中增加 `before-copy` 标签以区分读回时机
  - [x] 重新编译 native 库并部署到游戏目录
  - [x] 修复 `DIAG_READBACK_COLOR_TEX` 块的 null 守卫（`srcTex=null` 时跳过读回，避免潜在崩溃）
  - [x] 修复 `Dx12Backend.java` 编译错误（`getUsage()` → `usage()`）
  - [x] JAR 构建成功（1273973 bytes），已部署到 deploy/
  - [ ] 运行游戏，确认 splash 阶段 readback 日志显示绿色值（R≈0, G>0, B≈0, A>0）

- [x] Task 3: 修复 src buffer USAGE 缺失
  - [x] 分析日志发现 `buffer copy readback mismatch at 252`：readback 前 252 字节全零
  - [x] 根因：`selfTestCommandLayer` 中 src buffer 创建时仅有 `USAGE_COPY_DST`，但 `copyToBuffer(src, dst)` 需要 src 为 COPY_SOURCE
  - [x] 修复：`GpuBuffer.USAGE_COPY_DST` → `GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC`
  - [x] 重新构建 JAR 并部署

- [x] Task 6: 修复 native NaN 清洗导致的数据损坏（真正的 mismatch 根因）
  - [x] 二次运行日志确认：USAGE 修复后字节 0-251 拷贝正确，但 252 仍 mismatch——定位到 native `copyBufferToBuffer` 的 P23 NaN 清洗块
  - [x] 根因：self-test 数据 `(i & 0xFF)` 在字节 252-255 = 0xFC,0xFD,0xFE,0xFF，按小端 float 读恰为 NaN；P23 块把 NaN 改写为 0.0f → 拷贝后 252-255 变全零 → readback mismatch
  - [x] 修复：P23 块改为「只检测不修改」（`dbgLogDebug` 输出，默认静默），拷贝保持字节精确
  - [x] Java `checkForNanInfinity` 改为进程内仅提示一次、去掉 `Thread.dumpStack()`，避免合法字节模式误报刷屏
  - [x] native 重新编译（VS 18 2026 generator），DLL 203264 bytes 部署到 resources/ 与 dx12mod/
  - [x] JAR 重建（1273763 bytes）并部署到 deploy/，内嵌 DLL 验证 = 203264 bytes

- [ ] Task 4: 验证修复效果
  - [ ] 确认 self-test 全部通过（resource + command layer + pipeline + surface），不再回退 GL
  - [ ] 确认 splash 阶段渲染画面正常
  - [ ] 确认无 D3D12 validation errors

- [ ] Task 5: 清理与回归
  - [ ] 移除或条件编译所有临时诊断代码
  - [ ] 验证无诊断代码时游戏正常运行（splash → 进入游戏 → 渲染正常）

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 独立（可同时完成）
- Task 6 独立（定位到与 Task 3 不同的根因）
- Task 4 depends on Task 2 + Task 3 + Task 6
- Task 5 depends on Task 4
