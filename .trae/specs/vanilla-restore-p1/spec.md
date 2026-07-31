# 原版画面还原 P1 实施 Spec

## Why

GL4DX12（wgpu/D3D12 渲染器）已完成基础方块渲染（图集 + lightmap + 双 pass + 雾 + 视锥剔除 + 渐进压缩），但与原版画面差距明显：天空只有渐变穹顶（无太阳/月亮/星星/云）、实体是彩色实心包围盒、粒子是程序软圆（且 topology 错误导致仅画点）、半透明 pass 重复提交全部 mesh、水下/天气完全缺失。

## What Changes

- **P1a 天空天体**：启用 `sun_angle` 链路（Java `Level.getSunAngle` → JNI → Rust uniform），天空 shader 程序化绘制太阳盘/月亮盘（含月相）+ 星星（方向 hash 噪声，夜间淡入）；uniform buffer 128B→192B 扩容（后续云/水下复用）
- **P1b 云层**：程序化 fbm 噪声云平面（固定 y=192，x/z 跟随相机），风卷动画，天空色着色的 alpha 混合，方块 pass 之前绘制
- **P1c 半透明分层渲染**：layer 贯穿 Java→JNI→Rust（`ChunkMesh.layer: u8`），draw list 按 pass 过滤——不透明 pass 只提交 SOLID/CUTOUT，透明 pass 只提交 TRANSLUCENT，消除不透明 mesh 重复提交
- **P1f 粒子贴图**：捕获并上传 `textures/atlas/particles.png`，Java 提取粒子 sprite UV/颜色/尺寸（当前 8-float 布局扩展），`ParticleVertex` 加 UV，FS 改纹理采样 + alpha 阈值，修复 topology `PointList→TriangleList`（现 bug：粒子画成 1px 点）
- **P1d 水下效果**：Java `isUnderWater` 检测 + 水下雾色/雾距，天空 pass 在水下时替换为水色（MC 行为），uniform 增 underwater 标志
- **P1g 天气**：雨/雪粒子（复用粒子管线，Java 按天气状态生成落雨/雪花），雷暴时天空压暗（现已有雾密度分支，增强为粒子 + 天空色调联动）
- **P1e 实体模型（基础版）**：新增 Mixin 拦截 `EntityRenderDispatcher.render`，提取模型 cuboid 部件层级与每实体 pose，Rust 侧按部件盒体绘制（替代彩色包围盒）；骨骼动画（腿部摆动等）用实体 tick 相位驱动，不做完整 MC 动画系统

## Impact

- 影响代码：
  - `rust/wgpu-mc/src/lib.rs`：全部 shader（uniform 结构同步）、`write_camera_uniform`、`ChunkMesh`/`upload_chunk_mesh`、`collect_visible_draws`/`draw_chunks`、粒子/实体管线、`ensure_sky_pipeline`/`draw_sky_dome`、bind group 布局
  - `rust/wgpu-mc-jni/src/lib.rs`：`nativeUpdateSky`（消费 horizon/sun_angle）、`nativeUploadChunkMesh`（+layer）、`nativeSetParticles`（+UV）、新增 underwater/天气接口
  - `fabric`：`D3D12Bridge.java`、`Dx12Mod.java`、`SectionCompilerMixin.java`（传 layer）、`TextureAtlasMixin.java`（捕获 particles atlas）、新增实体模型 Mixin
- **BREAKING**：`nativeUploadChunkMesh` 增加 `jint layer` 参数（Java/JNI/Rust 三处同步）；`nativeSetParticles` 布局从 8→12 float/粒子
- 兼容性：offscreen fallback 路径不受影响；shader uniform 扩容需同步全部 6 个 shader 的 `CameraUniform` 定义

## ADDED Requirements

### Requirement: 太阳/月亮/星星天空天体（P1a）
渲染器 SHALL 绘制跟随玩家朝向的太阳/月亮盘与星星，太阳/月亮位置由真实 `sun_angle` 驱动，月亮按 MC 相位显示，星星夜间淡入。

#### Scenario: 白天与夜晚
- **WHEN** `sun_angle` 处于白天区间
- **THEN** 天空 shader 显示太阳盘（位置 = 太阳方向），无星星
- **WHEN** 处于夜晚区间
- **THEN** 显示月亮盘（相位正确）与星星（方向 hash 噪声，亮度随夜色淡入）

### Requirement: 云层渲染（P1b）
渲染器 SHALL 在 y=192 高度绘制随风向滚动的程序化云层，云色与天空色协调，半透明混合。

#### Scenario: 晴天与阴天
- **WHEN** 晴天
- **THEN** 白色云朵在天空色背景上可见，随视角缓慢卷动
- **WHEN** 阴天（雾色/天空色偏灰）
- **THEN** 云色跟随天空色变暗

### Requirement: 半透明分层渲染（P1c）
`ChunkMesh` SHALL 记录 MC RenderLayer（SOLID=0/CUTOUT=1/TRANSLUCENT=2），不透明 pass 只提交非 TRANSLUCENT mesh，透明 pass 只提交 TRANSLUCENT mesh。

#### Scenario: 玻璃与水
- **WHEN** 场景含玻璃/水（TRANSLUCENT）与实体方块
- **THEN** 透明 pass 不再提交 SOLID/CUTOUT draw 项（日志可观测 draw 数下降），玻璃半透明混合正确、深度正确

### Requirement: 粒子贴图（P1f）
粒子 SHALL 使用 MC particles atlas 纹理与 sprite UV，替代程序软圆；topology 修复为 TriangleList。

#### Scenario: 爆炸与火焰粒子
- **WHEN** 玩家触发爆炸/火焰粒子
- **THEN** 粒子显示对应 MC 粒子纹理（非软圆），尺寸/颜色/透明度与原版一致

### Requirement: 水下效果（P1d）
SHALL 在水下时以水色雾覆盖场景，天空 pass 输出水色，雾距缩短（MC 水下线性雾近似）。

#### Scenario: 潜水
- **WHEN** 玩家头部进入水中
- **THEN** 画面整体转为水色雾，远处地形不可见，出水后恢复

### Requirement: 天气（P1g）
SHALL 在雨/雷暴时渲染雨滴粒子，雪天渲染雪花，天空色调随天气变暗。

#### Scenario: 雨天与雪天
- **WHEN** `level.isRaining()`
- **THEN** 相机周围生成雨滴粒子（下落长条），雷暴时密度更大、天空更暗
- **WHEN** 生物群系降雪
- **THEN** 显示白色缓慢飘落雪花粒子

### Requirement: 实体模型（P1e，基础版）
实体 SHALL 以部件盒体模型渲染（替代彩色包围盒），支持每实体 pose 与基础摆动动画；不要求完整 MC 动画系统与纹理。

#### Scenario: 玩家/生物可见
- **WHEN** 玩家/动物/僵尸在视野内
- **THEN** 显示由其模型部件（头/躯干/四肢盒体）构成的模型，随移动腿部/手臂摆动，颜色按实体类型着色

## 实施节奏

每个 P1 项实现后：`cargo test -p wgpu-mc` 通过 + DLL/JAR 重建部署 + 自检完成后，**暂停等待用户跑图测试**，用户确认后再进入下一项（用户明确要求"每一个做完都检查，实现完过后暂停叫我测试"）。
