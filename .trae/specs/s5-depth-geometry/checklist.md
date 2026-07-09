# Checklist: Depth Buffer + Geometry Rendering Pipeline

- [x] `WmRenderer` 有 `depth_texture` 字段
- [x] `depth_texture` 格式为 `Depth32Float`
- [x] render pass 有 `depth_stencil_attachment`，clear depth = 1.0
- [x] pipeline 的 `depth_stencil` 开启 compare=Less、write=true
- [x] `resize()` 同步重建 depth texture
- [x] `create_plane_mesh()` 生成 y=0 平面 (vertex + index buffer)
- [x] `create_cube_mesh()` 生成立方体（24 顶点 + 36 索引，6 面双色）
- [x] Pipeline layout 有 push constant range: `ShaderStages::VERTEX`, 0..64
- [x] WGSL shader 声明 `var<push_constant> model: mat4x4<f32>`
- [x] `vs_main` 使用 `camera.mvp * model * pos` 做变换
- [x] Clear color 为浅蓝色 (0.53, 0.81, 0.92)
- [x] `render_frame()` 绘制地平面 + 5 个立方体
- [x] 每个 draw call 前 `set_push_constants` 设置正确的 model 矩阵
- [x] `cargo build --release` 编译无错误
- [x] `gradle build` 编译无错误
