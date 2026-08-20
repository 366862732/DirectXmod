# Tasks

- [x] Task 1: 修复 `DIAG_READBACK_COLOR_TEX` 读回时机
  - [x] 将 `dx12_surface.cpp` 中 `#ifdef DIAG_READBACK_COLOR_TEX` 块从 `CopyTextureRegion` 之后移到 `CopyTextureRegion` 之前
  - [x] 确保移动后读回发生在 `transitionTextureTo(ctx, srcTex, COPY_SOURCE)` 完成之后
  - [x] 保留 `deviceWaitIdle` 同步逻辑不变
  - [x] 重新编译 native 库

- [x] Task 2: 添加读回前后诊断日志
  - [x] 在 `blitSurface` 中 `CopyTextureRegion` 前后添加日志：打印 `srcTex` 指针、尺寸、格式、`ctx->colorTargetsWritten` 值
  - [x] 在 readback 日志中增加 `before-copy` 标签以区分读回时机
  - [ ] 重新编译并运行游戏，确认 readback 日志显示绿色值（R≈0, G>0, B≈0, A>0）

- [ ] Task 3: 验证修复效果
  - [ ] 确认 splash 阶段 readback 显示绿色（G 分量显著非零）
  - [ ] 确认 self-test 阶段仍显示红色（DIAG_CLEAR 路径不受影响）
  - [ ] 确认无 D3D12 validation errors 新增

- [ ] Task 4: 清理与回归
  - [ ] 移除或条件编译所有临时诊断代码
  - [ ] 验证无诊断代码时游戏正常运行（splash → 进入游戏 → 渲染正常）

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 3
