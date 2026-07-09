# Tasks: Depth Buffer + Geometry Rendering Pipeline

## 1. 深度缓冲
- [x] 在 `WmRenderer::create()` 中创建 `depth_texture`（`TextureFormat::Depth32Float`，`RENDER_ATTACHMENT` usage）
- [x] 存储 `depth_view`（`TextureView`）
- [x] `resize()` 时同步重建 depth texture
- [x] render pass 中添加 `depth_stencil_attachment`，clear depth = 1.0
- [x] pipeline 中设置 `depth_stencil: Some(...)`，开启 depth_test + depth_write

## 2. 几何体生成
- [x] 实现 `create_plane_mesh(device, size, color)` → `(Buffer, Buffer, u32)` 生成 y=0 平面 quad
- [x] 实现 `create_cube_mesh(device, size, color)` → `(Buffer, Buffer, u32)` 生成立方体（24 顶点，36 索引）
- [x] 两个函数返回 (vertex_buffer, index_buffer, index_count)
- [x] 顶点格式统一：`[f32;3]` position + `[f32;3]` color，复用现有 Vertex 结构

## 3. Push Constant
- [x] Pipeline layout 添加 push constant range（64 bytes = mat4x4）
- [x] WGSL shader 添加 `var<push_constant> model: mat4x4<f32>`
- [x] vs_main 中 `out.position = camera.mvp * model * vec4<f32>(pos, 1.0)`
- [x] 每个 draw call 前 `rp.set_push_constants(...)` 设置 model 矩阵

## 4. 天空色清屏
- [x] clear color 从黑色 `(0,0,0,1)` 改为 `(0.53, 0.81, 0.92, 1.0)`

## 5. 多物体渲染
- [x] 渲染地平面：绿色 200x200 quad 在 y=0
- [x] 渲染 5 个橙色立方体分布在不同位置
- [x] 立方体使用双面色（bright/dark）区分面朝向，辅助深度验证

## 6. 编译验证
- [x] `cargo build --release` 零错误
- [x] `gradle build` 零错误
