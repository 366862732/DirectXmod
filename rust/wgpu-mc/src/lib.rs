//! wgpu-mc: wgpu renderer with depth buffer, geometry pipeline, and readback.

use bytemuck::{Pod, Zeroable};

#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct Vertex {
    position: [f32; 3],
    color: [f32; 3],
}

impl Vertex {
    const ATTRIBS: [wgpu::VertexAttribute; 2] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x3,
    ];
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

const SHADER_SRC: &str = r#"
struct CameraUniform {
    mvp: mat4x4<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

var<push_constant> model: mat4x4<f32>;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec3<f32>, @location(1) color: vec3<f32>) -> VertexOutput {
    var out: VertexOutput;
    out.position = camera.mvp * model * vec4<f32>(pos, 1.0);
    out.color = color;
    return out;
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
}
"#;

/// Create a translation + scale model matrix (column-major for WGSL).
fn model_matrix(tx: f32, ty: f32, tz: f32, scale: f32) -> [[f32; 4]; 4] {
    [
        [scale, 0.0, 0.0, 0.0],
        [0.0, scale, 0.0, 0.0],
        [0.0, 0.0, scale, 0.0],
        [tx, ty, tz, 1.0],
    ]
}

fn make_depth_texture(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Texture {
    device.create_texture(&wgpu::TextureDescriptor {
        label: Some("Depth Texture"),
        size: wgpu::Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Depth32Float,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
        view_formats: &[],
    })
}

/// Generate a horizontal plane mesh at y=0.
/// Returns (vertex_buffer, index_buffer, index_count).
fn create_plane_mesh(device: &wgpu::Device, size: f32, color: [f32; 3])
    -> (wgpu::Buffer, wgpu::Buffer, u32)
{
    let h = size * 0.5;
    let vertices: [Vertex; 4] = [
        Vertex { position: [-h, 0.0, -h], color },
        Vertex { position: [ h, 0.0, -h], color },
        Vertex { position: [-h, 0.0,  h], color },
        Vertex { position: [ h, 0.0,  h], color },
    ];
    let indices: [u16; 6] = [0, 1, 2, 2, 1, 3];

    let vbuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Plane VB"),
        size: std::mem::size_of_val(&vertices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::VERTEX,
        mapped_at_creation: true,
    });
    vbuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
    vbuf.unmap();

    let ibuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Plane IB"),
        size: std::mem::size_of_val(&indices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::INDEX,
        mapped_at_creation: true,
    });
    ibuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&indices));
    ibuf.unmap();

    (vbuf, ibuf, indices.len() as u32)
}

/// Generate a unit cube mesh. Each face has a distinct color for 3D depth verification.
/// Returns (vertex_buffer, index_buffer, index_count).
fn create_cube_mesh(device: &wgpu::Device, color: [f32; 3])
    -> (wgpu::Buffer, wgpu::Buffer, u32)
{
    // 6 faces, 4 vertices each = 24 vertices. Each face gets a shade of the base color.
    let c = color;
    let d = [c[0] * 0.6, c[1] * 0.6, c[2] * 0.6]; // darker shade

    let vertices: [Vertex; 24] = [
        // +Y (top) — brighter
        Vertex { position: [-0.5,  0.5, -0.5], color: c },
        Vertex { position: [ 0.5,  0.5, -0.5], color: c },
        Vertex { position: [-0.5,  0.5,  0.5], color: c },
        Vertex { position: [ 0.5,  0.5,  0.5], color: c },
        // -Y (bottom) — darker
        Vertex { position: [-0.5, -0.5, -0.5], color: d },
        Vertex { position: [ 0.5, -0.5, -0.5], color: d },
        Vertex { position: [-0.5, -0.5,  0.5], color: d },
        Vertex { position: [ 0.5, -0.5,  0.5], color: d },
        // +Z (front) — brighter
        Vertex { position: [-0.5, -0.5,  0.5], color: c },
        Vertex { position: [ 0.5, -0.5,  0.5], color: c },
        Vertex { position: [-0.5,  0.5,  0.5], color: c },
        Vertex { position: [ 0.5,  0.5,  0.5], color: c },
        // -Z (back) — darker
        Vertex { position: [-0.5, -0.5, -0.5], color: d },
        Vertex { position: [ 0.5, -0.5, -0.5], color: d },
        Vertex { position: [-0.5,  0.5, -0.5], color: d },
        Vertex { position: [ 0.5,  0.5, -0.5], color: d },
        // +X (right) — brighter
        Vertex { position: [ 0.5, -0.5, -0.5], color: c },
        Vertex { position: [ 0.5,  0.5, -0.5], color: c },
        Vertex { position: [ 0.5, -0.5,  0.5], color: c },
        Vertex { position: [ 0.5,  0.5,  0.5], color: c },
        // -X (left) — darker
        Vertex { position: [-0.5, -0.5, -0.5], color: d },
        Vertex { position: [-0.5,  0.5, -0.5], color: d },
        Vertex { position: [-0.5, -0.5,  0.5], color: d },
        Vertex { position: [-0.5,  0.5,  0.5], color: d },
    ];

    let indices: [u16; 36] = [
         0,  1,  2,  2,  1,  3,  // +Y
         4,  6,  5,  5,  6,  7,  // -Y
         8,  9, 10, 10,  9, 11,  // +Z
        12, 14, 13, 13, 14, 15,  // -Z
        16, 17, 18, 18, 17, 19,  // +X
        20, 22, 21, 21, 22, 23,  // -X
    ];

    let vbuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Cube VB"),
        size: std::mem::size_of_val(&vertices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::VERTEX,
        mapped_at_creation: true,
    });
    vbuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
    vbuf.unmap();

    let ibuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Cube IB"),
        size: std::mem::size_of_val(&indices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::INDEX,
        mapped_at_creation: true,
    });
    ibuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&indices));
    ibuf.unmap();

    (vbuf, ibuf, indices.len() as u32)
}

pub struct WmRenderer {
    pub device: wgpu::Device,
    pub queue: wgpu::Queue,
    pub width: u32,
    pub height: u32,
    pub camera_mvp: [[f32; 4]; 4],

    pipeline: wgpu::RenderPipeline,
    bind_group: wgpu::BindGroup,
    #[allow(dead_code)]
    bind_group_layout: wgpu::BindGroupLayout,
    uniform_buffer: wgpu::Buffer,
    texture: wgpu::Texture,
    depth_texture: wgpu::Texture,
    depth_view: wgpu::TextureView,
    staging_buffer: wgpu::Buffer,

    // Geometry
    plane_vb: wgpu::Buffer,
    plane_ib: wgpu::Buffer,
    plane_count: u32,
    cube_vb: wgpu::Buffer,
    cube_ib: wgpu::Buffer,
    cube_count: u32,
}

impl WmRenderer {
    pub fn create(width: u32, height: u32) -> Result<Self, &'static str> {
        log::info!("Creating WmRenderer (offscreen) {}x{}", width, height);

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::DX12,
            ..Default::default()
        });

        let adapter = futures::executor::block_on(instance.request_adapter(
            &wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: None,
                ..Default::default()
            },
        ))
        .ok_or("No adapter")?;

        log::info!("DX12 adapter: {:?}", adapter.get_info().name);

        let (device, queue) = futures::executor::block_on(adapter.request_device(
            &wgpu::DeviceDescriptor {
                label: Some("wgpu-mc"),
                required_features: wgpu::Features::PUSH_CONSTANTS,
                required_limits: wgpu::Limits {
                    max_push_constant_size: 64,
                    ..Default::default()
                },
                memory_hints: Default::default(),
            },
            None,
        ))
        .map_err(|_| "Device failed")?;

        // Shader module
        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Main Shader"),
            source: wgpu::ShaderSource::Wgsl(std::borrow::Cow::Borrowed(SHADER_SRC)),
        });

        // Uniform buffer (64 bytes for mat4x4 MVP)
        let uniform_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Camera Uniform"),
            size: 64,
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        // Bind group layout
        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Camera Bind Group Layout"),
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::VERTEX,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            }],
        });

        // Bind group
        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Camera Bind Group"),
            layout: &bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: uniform_buffer.as_entire_binding(),
            }],
        });

        // Pipeline layout with push constant for model matrix
        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Pipeline Layout"),
            bind_group_layouts: &[&bind_group_layout],
            push_constant_ranges: &[wgpu::PushConstantRange {
                stages: wgpu::ShaderStages::VERTEX,
                range: 0..64, // mat4x4<f32>
            }],
        });

        // Render pipeline with depth testing
        let pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Main Pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                compilation_options: Default::default(),
                buffers: &[Vertex::desc()],
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                compilation_options: Default::default(),
                targets: &[Some(wgpu::ColorTargetState {
                    format: wgpu::TextureFormat::Rgba8UnormSrgb,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: Some(wgpu::Face::Back),
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: wgpu::TextureFormat::Depth32Float,
                depth_write_enabled: true,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        // Offscreen color texture
        let texture = device.create_texture(&wgpu::TextureDescriptor {
            label: Some("Offscreen Texture"),
            size: wgpu::Extent3d {
                width,
                height,
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8UnormSrgb,
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT
                | wgpu::TextureUsages::COPY_SRC
                | wgpu::TextureUsages::TEXTURE_BINDING,
            view_formats: &[],
        });

        // Depth texture
        let depth_texture = make_depth_texture(&device, width, height);
        let depth_view = depth_texture.create_view(&wgpu::TextureViewDescriptor::default());

        // Staging buffer for readback
        let size = (width as u64) * (height as u64) * 4;
        let staging_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Staging"),
            size,
            usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });

        // Generate geometry
        let (plane_vb, plane_ib, plane_count) =
            create_plane_mesh(&device, 200.0, [0.2, 0.65, 0.2]);
        let (cube_vb, cube_ib, cube_count) =
            create_cube_mesh(&device, [0.8, 0.4, 0.1]);

        Ok(Self {
            device,
            queue,
            width,
            height,
            camera_mvp: [
                [1.0, 0.0, 0.0, 0.0],
                [0.0, 1.0, 0.0, 0.0],
                [0.0, 0.0, 1.0, 0.0],
                [0.0, 0.0, 0.0, 1.0],
            ],
            pipeline,
            bind_group,
            bind_group_layout,
            uniform_buffer,
            texture,
            depth_texture,
            depth_view,
            staging_buffer,
            plane_vb,
            plane_ib,
            plane_count,
            cube_vb,
            cube_ib,
            cube_count,
        })
    }

    pub fn set_camera(&mut self, mvp: [[f32; 4]; 4]) {
        self.camera_mvp = mvp;
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        self.width = width;
        self.height = height;

        // Recreate color texture
        self.texture = self.device.create_texture(&wgpu::TextureDescriptor {
            label: Some("Offscreen Texture"),
            size: wgpu::Extent3d {
                width,
                height,
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8UnormSrgb,
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT
                | wgpu::TextureUsages::COPY_SRC
                | wgpu::TextureUsages::TEXTURE_BINDING,
            view_formats: &[],
        });

        // Recreate depth texture
        self.depth_texture = make_depth_texture(&self.device, width, height);
        self.depth_view = self.depth_texture.create_view(&wgpu::TextureViewDescriptor::default());

        // Recreate staging buffer
        let size = (width as u64) * (height as u64) * 4;
        self.staging_buffer = self.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Staging"),
            size,
            usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });
    }

    /// Render a frame and return pixel data (RGBA).
    pub fn render_frame(&mut self) -> Vec<u8> {
        let w = self.width as usize;
        let h = self.height as usize;
        let size = (w * h * 4) as u64;

        // 1. Update uniform buffer with camera MVP
        let mvp_bytes: &[u8] = bytemuck::cast_slice(&self.camera_mvp);
        self.queue.write_buffer(&self.uniform_buffer, 0, mvp_bytes);

        // 2. Create command encoder
        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("Render"),
            });

        // 3. Render pass
        {
            let color_view = self
                .texture
                .create_view(&wgpu::TextureViewDescriptor::default());
            let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Main Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &color_view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.53,
                            g: 0.81,
                            b: 0.92,
                            a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                    view: &self.depth_view,
                    depth_ops: Some(wgpu::Operations {
                        load: wgpu::LoadOp::Clear(1.0),
                        store: wgpu::StoreOp::Store,
                    }),
                    stencil_ops: None,
                }),
                timestamp_writes: None,
                occlusion_query_set: None,
            });

            rp.set_pipeline(&self.pipeline);
            rp.set_bind_group(0, &self.bind_group, &[]);

            // Draw ground plane at y=0
            let plane_model = model_matrix(0.0, 0.0, 0.0, 1.0);
            rp.set_push_constants(wgpu::ShaderStages::VERTEX, 0,
                bytemuck::cast_slice(&plane_model));
            rp.set_vertex_buffer(0, self.plane_vb.slice(..));
            rp.set_index_buffer(self.plane_ib.slice(..), wgpu::IndexFormat::Uint16);
            rp.draw_indexed(0..self.plane_count, 0, 0..1);

            // Draw 5 colored cubes at different positions
            let cube_positions: [(f32, f32, f32); 5] = [
                (0.0, 1.0, 0.0),
                (4.0, 1.0, 2.0),
                (-4.0, 1.0, -1.0),
                (2.0, 1.0, -4.0),
                (-3.0, 1.0, 3.0),
            ];

            // Render each cube
            // We'll re-use the same cube geometry with different push constants.
            // To vary colors, we could create separate cube meshes, but for now
            // all cubes share the same orange base color.
            for &(cx, cy, cz) in &cube_positions {
                let cube_model = model_matrix(cx, cy, cz, 1.0);
                rp.set_push_constants(wgpu::ShaderStages::VERTEX, 0,
                    bytemuck::cast_slice(&cube_model));
                rp.set_vertex_buffer(0, self.cube_vb.slice(..));
                rp.set_index_buffer(self.cube_ib.slice(..), wgpu::IndexFormat::Uint16);
                rp.draw_indexed(0..self.cube_count, 0, 0..1);
            }
        }

        // 4. Copy texture to staging buffer
        if self.staging_buffer.size() < size {
            self.staging_buffer = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Staging"),
                size,
                usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
                mapped_at_creation: false,
            });
        }
        encoder.copy_texture_to_buffer(
            wgpu::ImageCopyTexture {
                texture: &self.texture,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            wgpu::ImageCopyBuffer {
                buffer: &self.staging_buffer,
                layout: wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(4 * self.width),
                    rows_per_image: Some(self.height),
                },
            },
            wgpu::Extent3d {
                width: self.width,
                height: self.height,
                depth_or_array_layers: 1,
            },
        );

        // 5. Submit and wait
        self.queue.submit(Some(encoder.finish()));
        self.device.poll(wgpu::Maintain::Wait);

        // 6. Read back pixels
        let sb_slice = self.staging_buffer.slice(..);
        sb_slice.map_async(wgpu::MapMode::Read, |_| {});
        self.device.poll(wgpu::Maintain::Wait);

        let data = sb_slice.get_mapped_range();
        let pixels = data.to_vec();
        drop(data);
        self.staging_buffer.unmap();

        pixels
    }
}
