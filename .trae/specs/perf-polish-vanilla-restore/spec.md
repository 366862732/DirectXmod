# 性能抛光与原版画面还原待办清单 Spec

## Why

1. **轻微卡顿**：860dc10（墓碑删除）已消除跑图卡爆（FPS 213–473 稳定），但墓碑**压缩重建**仍每 ~10 秒触发一次（`Chunk batch rebuilt: 4834→5891 meshes, ~17.5M verts ≈ 700MB` 全量重建 + 上传），压缩帧 FPS 明显回落（270→225），产生周期性轻微卡顿。
2. **原版画面还原差距大**：当前渲染器仅覆盖地形 + 天空渐变 + 雾 + 粒子（软圆）+ 实体（彩色包围盒）+ HUD 叠加，太阳/月亮/星星、云、水下效果、实体模型、粒子贴图、天气、阴影、AA 等原版元素均未实现。
3. **缺少统一待办清单**：开发任务分散在 步骤.md 各阶段"已知边界/待用户验证"与多个旧 spec 中，无全局优先级视图。

## What Changes

- **P0 性能优化（本 spec 实施）**：
  - 墓碑压缩触发阈值降频：`deleted_vb_bytes` 64MB→128MB、占比 1/3→1/2、硬上限 256MB→512MB（压缩间隔 ~10s → ~20s+）
  - 压缩重建异步化（实施裁定：**渐进压缩**，非后台线程——`wgpu::Queue` 非 Sync、`WmRenderer` `&mut self` 调用约定下后台线程+Mutex 有死锁/竞争风险）：压缩启动时按 base_vertex 快照 mesh 槽位，每帧按 64MB budget 从前向后把存活 mesh 数据重排到 `merged_verts`/`merged_indices` 前部并逐段 `queue.write_buffer`，offsets 与数据同帧落位；压缩期间继续用旧 buffer 渲染（墓碑无害），完成后截断缓存并清零墓碑统计
- **P1 原版画面还原（本 spec 形成差距清单与验收标准，实现列入待办）**：太阳/月亮盘 + 星星、云层、半透明分层渲染（按 RenderLayer 拆分 draw list，透明 pass 不再重复提交不透明 mesh）、水下效果、实体模型（模型加载 + 动画）、粒子贴图、天气（雨雪）
- **P2 视觉增强（待办）**：阴影、后处理/AA（MSAA/FXAA/SMAA/TAA）
- **P3 架构与文档（待办）**：分段合并缓存（根治压缩 + 突破 1 GiB 上限）、README/旧 spec checklist 同步
- **待办清单**：在 spec 中给出 P0–P3 完整任务、优先级、依赖与验收标准

## Impact

- Affected specs: fix-surface-tdr（TDR 相关项）、s3-2-wgpu-integration、s4-camera-wgpu-pipeline（遗留未勾选项并入待办）
- Affected code: `rust/wgpu-mc/src/lib.rs`（压缩逻辑、draw_chunks）、`rust/wgpu-mc-jni/src/lib.rs`、`fabric/src/main/java/com/dx12/`（数据提取）、`步骤.md`、`verify-deploy.ps1`
- 不改动已有稳定成果：1 GiB 上限、增量合并、墓碑删除、HUD 叠加

## ADDED Requirements

### Requirement: 压缩卡顿消除
系统 SHALL 将墓碑压缩重建对单帧帧时间的影响控制在可感知阈值以下，跑图时不再出现周期性掉帧。

#### Scenario: 跑图压缩
- **WHEN** 玩家持续移动导致墓碑删除量达到压缩阈值
- **THEN** 压缩在后台/帧间完成，`D3D12 present FPS` 保持稳定（不出现明显单帧停顿），且渲染画面不出现缺块或撕裂

#### Scenario: 压缩触发频率
- **WHEN** 连续跑图 60 秒（大渲染距离）
- **THEN** `Chunk batch rebuilt` 日志出现次数 ≤ 3 次（当前 ~10s 一次，目标 ≤ 20s 一次）

### Requirement: 原版画面还原差距清单
系统 SHALL 提供完整的原版画面功能差距清单，逐项标注状态（已实现/部分/未实现）、缺口描述、验收标准与优先级。

#### Scenario: 差距排查
- **WHEN** 对照原版 MC 渲染功能清单（天空/云/水/实体/粒子/天气/阴影/AA 等）
- **THEN** 清单覆盖全部功能点，每项给出证据（文件:行号或函数名）与验收标准

### Requirement: 待办清单
系统 SHALL 维护 P0–P3 优先级待办清单，每项包含：任务描述、验收标准、依赖关系。

#### Scenario: 清单维护
- **WHEN** 后续开发启动新任务
- **THEN** 按清单优先级实施，完成后在 步骤.md 与 checklist 中勾选

## MODIFIED Requirements

### Requirement: 墓碑删除与延迟压缩（13.9）
压缩触发阈值由"≥64MB 且 ≥1/3 存活数据，或 256MB 硬上限"调整为"≥128MB 且 ≥1/2 存活数据，或 512MB 硬上限"；压缩执行由"同步全量重建"调整为"渐进式帧间压缩"（每帧 64MB 切片重排 + 逐帧上传，压缩期间旧 buffer 继续绘制）。

## REMOVED Requirements

（无删除项）
