# Tasks

## P0 性能优化（本 spec 实施）

- [x] Task 1: 墓碑压缩触发阈值降频
  - [x] 修改 `prepare_chunk_batch`（rust/wgpu-mc/src/lib.rs:2581）：`deleted_vb_bytes` 阈值 64MB→128MB、占比 1/3→1/2、硬上限 256MB→512MB
  - [x] `cargo test -p wgpu-mc` 通过
- [x] Task 2: 压缩重建异步化（根治压缩帧停顿）
  - [x] 方案裁定：后台线程方案因 `wgpu::Queue` 非 Sync、`WmRenderer` `&mut self` 调用约定、Mutex/AsyncMutex 风险被否；改用**渐进压缩**——每帧把 CPU `merged_verts`/`merged_indices` 按 64MB budget 切片从前向后重排（数据取自 mesh 权威副本 `mesh.vertices`，免重叠风险），逐帧 `queue.write_buffer` 上传被移动区间
  - [x] 压缩期间继续用旧 GPU buffer 渲染（墓碑无害），offsets 与数据同帧落位：indirect args 每帧从 chunk_meshes 重建，`write_buffer` 先于同帧 draw 入队，保证一致性
  - [x] 并发安全：上传仅追加尾部（offset ≥ 捕获时 len，永不与压缩目标区冲突）；压缩期间 clear 强制走墓碑路径（禁止截断）；被 clear/recompile 的已捕获槽位经身份校验（base_vertex/len 匹配）后跳过
  - [x] `cargo test -p wgpu-mc` 通过（17 tests）
- [x] Task 3: 构建、部署与验证
  - [x] 重建 DLL + JAR 并部署（deploy_commit.txt 同步为 3d97625）
  - [x] 更新 verify-deploy.ps1 期望 hash（860dc10 → 3d97625）
  - [ ] 跑图验证：`Chunk compaction started/finished` 频率下降、压缩帧无卡顿、无 REJECTED/崩溃；步骤.md 记录 13.10（文档已记录，跑图确认待用户执行）

## P1–P3 待办清单（spec 交付，实施列入后续任务）

- [x] Task 4: 原版画面还原差距清单落档（spec.md 已含，后续按 P1 实施）
  - [ ] P1a 太阳/月亮盘 + 星星（sky dome 扩展，uniform 驱动位置/相位）
  - [ ] P1b 云层渲染（分形噪声云 + 光照，或 MC 云图 2D 卷）
  - [ ] P1c 半透明分层渲染（按 RenderLayer 拆分 draw list，透明 pass 只提交 TRANSLUCENT mesh，消除不透明 mesh 重复绘制）
  - [ ] P1d 水下效果（水下雾色/深度滤镜）
  - [ ] P1e 实体模型渲染（模型加载 + 骨骼动画；当前仅彩色包围盒）
  - [ ] P1f 粒子贴图（程序软圆 → MC 粒子纹理 billboard）
  - [ ] P1g 天气（雨雪粒子 + 天空色调）
- [x] Task 5: 视觉增强（P2，清单落档，实施后续）
  - [ ] 阴影（shadow map）
  - [ ] 后处理/AA（MSAA 或 FXAA/SMAA/TAA，恢复 nativeSetAaMode 实现）
- [x] Task 6: 架构与文档（P3，清单落档，实施后续）
  - [ ] 分段合并缓存（彻底消除压缩全量重建 + 突破 1 GiB 上限）
  - [ ] README 同步 11a–13.x 最新进度；旧 spec（fix-surface-tdr / s3-2 / s4 / s5）遗留 checklist 项清理或并档

# Task Dependencies

- [Task 2] depends on [Task 1]（阈值降频先行，异步化为根治手段，两者独立可并施）
- [Task 3] depends on [Task 1][Task 2]
- [Task 4/5/6] 无依赖，均为后续规划项（本 spec 仅形成清单与验收标准）
