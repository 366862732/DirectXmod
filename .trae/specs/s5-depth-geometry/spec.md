# Phase 4 Step 2: Depth Buffer + Geometry Rendering Pipeline Spec

## Why
当前管线只有一个 color attachment，没有深度测试。所有东西都在同一平面上绘制，无法区分前后。要渲染 Minecraft 场景，必须先建立深度缓冲和几何渲染管线。这一步搭好骨架：depth texture + 多物体渲染 + push constant 模型矩阵，渲染地平面和彩色方块验证深度写入。

## What Changes
- **Rust wgpu 层**:
  - 新增 `depth_texture` + `depth_view`，render pass 添加 depth_stencil attachment
  - 清屏色改为天空蓝 (0.53, 0.81, 0.92) 模拟天空
  - 渲染地平面：y=0 处的绿色大 quad
  - 渲染几个彩色立方体在地平面上
  - 新增 push constant（model 矩阵，64 bytes mat4x4），每个物体独立变换
  - 新增 `create_cube_mesh()` 和 `create_plane_mesh()` 工具函数生成几何体
  - Pipeline 开启 depth_test、depth_write
  - WGSL shader 添加 `@builtin(position)` 的 z 分量（深度输出），接收 push constant model 矩阵
- **Java 端**: 无变化（MVP 矩阵已通过）
- **JNI 层**: 无变化

## Impact
- Affected specs: s4-camera-wgpu-pipeline（替换 triangle 渲染为多物体几何渲染）
- Affected code: `rust/wgpu-mc/src/lib.rs`（主要改动）

## ADDED Requirements

### Requirement: 深度缓冲
系统 SHALL 创建 depth texture（格式 Depth32Float），在 render pass 中绑定为 depth_stencil_attachment，每帧 clear 到 1.0。

#### Scenario: 深度测试
- **WHEN** 多个物体在渲染管线中绘制
- **THEN** 离观察者更近的物体遮挡更远的物体
- **AND** 渲染结果有正确的遮挡关系

### Requirement: 地平面渲染
系统 SHALL 渲染一个位于 y=0 的大四边形作为参考地平面，使用纯色（绿色）。

#### Scenario: 地平面可见
- **WHEN** 玩家在地面上方
- **THEN** 覆盖层显示绿色地平面延伸到远处
- **AND** 天空色在水平线以上可见

### Requirement: 彩色立方体
系统 SHALL 渲染 3~5 个不同颜色的立方体分布在地平面上不同位置，用于验证 3D 深度和遮挡。

#### Scenario: 立方体遮挡
- **WHEN** 玩家视角移动
- **THEN** 不同立方体之间有正确的遮挡关系
- **AND** 立方体遮挡地平面，地平面遮挡其他物体

### Requirement: Push Constant 模型矩阵
系统 SHALL 使用 push constant 为每个物体传递 model 矩阵（4x4），支持独立平移/旋转/缩放。

#### Scenario: 物体位置独立
- **WHEN** 多个立方体有不同的 model 矩阵
- **THEN** 它们在世界空间中出现在不同位置

### Requirement: 天空色清屏
系统 SHALL 将 render pass 的 clear color 改为浅蓝色 (0.53, 0.81, 0.92)，模拟白天的天空。

#### Scenario: 天空可见
- **WHEN** 没有几何体覆盖某个像素
- **THEN** 该像素显示浅蓝色天空
