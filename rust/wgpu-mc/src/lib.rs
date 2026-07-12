//! wgpu-mc: wgpu renderer with depth buffer, geometry pipeline, and readback.
//!
//! Architecture: Triple-buffered ring on JNI thread with optional surface mode.
//! - Surface mode:  render directly to window swapchain, present → no readback
//! - Offscreen mode: render to textures, async readback via triple-buffer ring
//!
//! Note: Push constants removed — not supported on all GPUs.
//! Model transforms are baked into vertex buffers at creation time.
//!
//! TDR Prevention (HWND sharing with OpenGL):
//! The D3D12 swapchain is created on the same HWND as MC's GL context.
//! To prevent GPU driver TDR, the GL context is temporarily detached
//! (GLFW.glfwMakeContextCurrent(0)) from the Java side before calling
//! renderFrame(), and reattached after. This ensures only one API has
//! access to the HWND at any time.

use bytemuck::{Pod, Zeroable};
use std::sync::mpsc;

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

/// Chunk vertex with UV for texture atlas sampling.
/// 32 bytes: position(12) + color(12) + uv(8).
#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct ChunkVertex {
    position: [f32; 3],
    color: [f32; 3],
    uv: [f32; 2],
}

impl ChunkVertex {
    const ATTRIBS: [wgpu::VertexAttribute; 3] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x3,
        2 => Float32x2,
    ];
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

#[repr(C)]
#[derive(Copy, Clone, Debug, bytemuck::Pod, bytemuck::Zeroable)]
struct TexVertex {
    position: [f32; 2],
    uv: [f32; 2],
}

impl TexVertex {
    const ATTRIBS: [wgpu::VertexAttribute; 2] = wgpu::vertex_attr_array![
        0 => Float32x2,
        1 => Float32x2,
    ];

    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

// No push constants — compatible with all GPUs
const SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec3<f32>, @location(1) color: vec3<f32>) -> VertexOutput {
    var out: VertexOutput;
    // Offset local-space geometry by camera world position so it
    // stays visible near the camera regardless of player location.
    let world_pos = pos + camera.camera_pos;
    out.position = camera.view_proj * vec4<f32>(world_pos, 1.0);
    out.color = color;
    return out;
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
}
"#;

// Chunk shader: samples atlas texture, multiplies by vertex color as lighting tint.
const CHUNK_SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;
@group(0) @binding(1) var atlas: texture_2d<f32>;
@group(0) @binding(2) var atlas_sampler: sampler;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) tint: vec3<f32>,
}

@vertex
fn vs_main(
    @location(0) pos: vec3<f32>,
    @location(1) color: vec3<f32>,
    @location(2) uv: vec2<f32>,
) -> VertexOutput {
    var out: VertexOutput;
    // Positions are in world space (section origin + local offset).
    // The MVP (view_proj) already includes the view matrix that handles
    // world→camera transformation, so no camera_pos addition is needed.
    let world_pos = pos;
    out.position = camera.view_proj * vec4<f32>(world_pos, 1.0);
    out.uv = uv;
    out.tint = color;
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    let tex_color = textureSample(atlas, atlas_sampler, in.uv);
    // Alpha test: discard transparent pixels (leaves, glass, etc.)
    if (tex_color.a < 0.1) {
        discard;
    }
    return vec4<f32>(tex_color.rgb * in.tint, 1.0);
}
"#;

// Textured fullscreen quad shader — renders GL framebuffer capture as D3D12 texture.
const TEX_SHADER_SRC: &str = r#"
struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec2<f32>, @location(1) uv: vec2<f32>) -> VertexOutput {
    var out: VertexOutput;
    out.position = vec4<f32>(pos, 0.0, 1.0);
    out.uv = uv;
    return out;
}

@group(0) @binding(0) var tex: texture_2d<f32>;
@group(0) @binding(1) var samp: sampler;

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    // Flip Y: GL framebuffer reads bottom-up, wgpu textures are top-down.
    var uv = in.uv;
    uv.y = 1.0 - uv.y;
    return textureSample(tex, samp, uv);
}
"#;

const IDENTITY: [[f32; 4]; 4] = [
    [1.0, 0.0, 0.0, 0.0],
    [0.0, 1.0, 0.0, 0.0],
    [0.0, 0.0, 1.0, 0.0],
    [0.0, 0.0, 0.0, 1.0],
];

const RING_SIZE: usize = 3;
const LERP_FACTOR: f32 = 0.3;

fn mat4_lerp(a: &[[f32; 4]; 4], b: &[[f32; 4]; 4], t: f32) -> [[f32; 4]; 4] {
    let mut result = [[0.0f32; 4]; 4];
    for r in 0..4 {
        for c in 0..4 {
            result[r][c] = a[r][c] + (b[r][c] - a[r][c]) * t;
        }
    }
    result
}

/// Write camera MVP + camera_pos to the uniform buffer.
/// Layout: mat4x4 (64 bytes) + vec3 padded to vec4 (16 bytes) = 80 bytes.
fn write_camera_uniform(queue: &wgpu::Queue, buffer: &wgpu::Buffer, mvp: &[[f32; 4]; 4], pos: &[f32; 3]) {
    let mut data = [0u8; 80];
    data[0..64].copy_from_slice(bytemuck::cast_slice(mvp));
    data[64..76].copy_from_slice(bytemuck::cast_slice(pos));
    queue.write_buffer(buffer, 0, &data);
}

fn make_depth_texture(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Texture {
    device.create_texture(&wgpu::TextureDescriptor {
        label: Some("Depth Texture"),
        size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Depth32Float,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
        view_formats: &[],
    })
}

fn make_color_texture(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Texture {
    device.create_texture(&wgpu::TextureDescriptor {
        label: Some("Color Texture"),
        size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Rgba8UnormSrgb,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT
            | wgpu::TextureUsages::COPY_SRC
            | wgpu::TextureUsages::TEXTURE_BINDING,
        view_formats: &[],
    })
}

fn make_staging_buffer(device: &wgpu::Device, width: u32, height: u32) -> wgpu::Buffer {
    let row_aligned = ((width * 4 + 255) / 256) * 256;
    device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Staging"),
        size: (row_aligned as u64) * (height as u64),
        usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
        mapped_at_creation: false,
    })
}

fn aligned_row(width: u32) -> u32 {
    ((width * 4 + 255) / 256) * 256
}

fn create_plane_mesh(device: &wgpu::Device, size: f32, y: f32, z_center: f32, color: [f32; 3])
    -> (wgpu::Buffer, wgpu::Buffer, u32)
{
    let h = size * 0.5;
    let z0 = z_center - h;
    let z1 = z_center + h;
    let vertices: [Vertex; 4] = [
        Vertex { position: [-h, y, z0], color },
        Vertex { position: [ h, y, z0], color },
        Vertex { position: [-h, y, z1], color },
        Vertex { position: [ h, y, z1], color },
    ];
    let indices: [u16; 6] = [0, 3, 1, 0, 2, 3];  // CCW → +Y normal

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

/// Create a cube mesh with vertices pre-offsetted by (ox, oy, oz).
/// Shares a single index buffer for all cubes.
fn create_cube_mesh_at(
    device: &wgpu::Device,
    color: [f32; 3],
    offset: (f32, f32, f32),
) -> wgpu::Buffer {
    let c = color;
    let d = [c[0] * 0.6, c[1] * 0.6, c[2] * 0.6];
    let (ox, oy, oz) = offset;
    let vertices: [Vertex; 24] = [
        Vertex { position: [-0.5+ox,  0.5+oy, -0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy, -0.5+oz], color: c },
        Vertex { position: [-0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox, -0.5+oy,  0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy,  0.5+oz], color: d },
        Vertex { position: [-0.5+ox, -0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox, -0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox,  0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox,  0.5+oy, -0.5+oz], color: d },
        Vertex { position: [ 0.5+ox, -0.5+oy, -0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy, -0.5+oz], color: c },
        Vertex { position: [ 0.5+ox, -0.5+oy,  0.5+oz], color: c },
        Vertex { position: [ 0.5+ox,  0.5+oy,  0.5+oz], color: c },
        Vertex { position: [-0.5+ox, -0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox,  0.5+oy, -0.5+oz], color: d },
        Vertex { position: [-0.5+ox, -0.5+oy,  0.5+oz], color: d },
        Vertex { position: [-0.5+ox,  0.5+oy,  0.5+oz], color: d },
    ];

    let vbuf = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Cube VB"),
        size: std::mem::size_of_val(&vertices) as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::VERTEX,
        mapped_at_creation: true,
    });
    vbuf.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
    vbuf.unmap();
    vbuf
}

// ── Create wgpu Surface from Windows HWND ─────────────────────────

fn create_surface_from_hwnd(
    instance: &wgpu::Instance,
    hwnd: usize,
) -> Option<wgpu::Surface<'static>> {
    use raw_window_handle::{
        RawDisplayHandle, RawWindowHandle, WindowsDisplayHandle, Win32WindowHandle,
    };

    let hwnd_isize = hwnd as isize;
    let raw_handle = RawWindowHandle::Win32(
        Win32WindowHandle::new(std::num::NonZeroIsize::new(hwnd_isize)?)
    );
    let display_handle = RawDisplayHandle::Windows(WindowsDisplayHandle::new());

    let surface = unsafe {
        instance.create_surface_unsafe(
            wgpu::SurfaceTargetUnsafe::RawHandle {
                raw_window_handle: raw_handle,
                raw_display_handle: display_handle,
            }
        )
    };
    match surface {
        Ok(s) => Some(s),
        Err(e) => {
            log::error!("[dx12-wm] create_surface_unsafe failed: {:?}", e);
            None
        }
    }
}

// ── Chunk mesh storage (MC section geometry → D3D12) ─────────────

/// One mesh = one RenderLayer of one 16×16×16 chunk section.
/// Keyed by (section_x, section_y, section_z).
struct ChunkMesh {
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    vertex_count: u32,
    index_count: u32,
    index_is_u32: bool,
}

// ── Ring slot ─────────────────────────────────────────────────────

struct Slot {
    #[allow(dead_code)]
    color: wgpu::Texture,
    #[allow(dead_code)]
    depth: wgpu::Texture,
    depth_view: wgpu::TextureView,
    staging: wgpu::Buffer,
}

impl Slot {
    fn new(device: &wgpu::Device, width: u32, height: u32) -> Self {
        let color = make_color_texture(device, width, height);
        let depth = make_depth_texture(device, width, height);
        let depth_view = depth.create_view(&wgpu::TextureViewDescriptor::default());
        let staging = make_staging_buffer(device, width, height);
        Self { color, depth, depth_view, staging }
    }
}

// ████████████████████████████████████████████████████████████████████████
// ██  RENDERER                                                       ██
// ████████████████████████████████████████████████████████████████████████

pub struct WmRenderer {
    #[allow(dead_code)]
    instance: wgpu::Instance,
    adapter: wgpu::Adapter,
    device: wgpu::Device,
    queue: wgpu::Queue,

    width: u32,
    height: u32,

    pub camera_mvp: [[f32; 4]; 4],
    camera_prev: [[f32; 4]; 4],
    camera_target: [[f32; 4]; 4],
    camera_pos: [f32; 3],

    // Immutable GPU resources
    pipeline: wgpu::RenderPipeline,
    bind_group: wgpu::BindGroup,
    uniform_buffer: wgpu::Buffer,
    plane_vb: wgpu::Buffer,
    plane_ib: wgpu::Buffer,
    plane_count: u32,
    cube_vbs: Vec<wgpu::Buffer>,    // One VB per cube position
    cube_ib: wgpu::Buffer,          // Shared index buffer
    cube_count: u32,

    // Surface mode (native swapchain, no readback)
    surface: Option<wgpu::Surface<'static>>,
    surface_config: Option<wgpu::SurfaceConfiguration>,
    surface_format: wgpu::TextureFormat,
    surface_depth: Option<wgpu::Texture>,  // Cached depth texture (reused per-frame)
    /// True when a window resize has been received but the swapchain has not
    /// been reconfigured yet. Forces a surface reconfig in render_surface().
    resize_pending: bool,

    // Textured fullscreen quad (GL framebuffer → D3D12 display)
    tex_pipeline: wgpu::RenderPipeline,
    tex_bind_group: Option<wgpu::BindGroup>,
    tex_sampler: wgpu::Sampler,
    frame_texture: Option<wgpu::Texture>,
    frame_width: u32,
    frame_height: u32,
    fs_quad_vb: wgpu::Buffer,
    fs_quad_ib: wgpu::Buffer,

    // Offscreen mode (triple-buffer readback)
    slots: [Slot; RING_SIZE],
    idx: usize,
    pending_rx: [Option<mpsc::Receiver<Result<(), wgpu::BufferAsyncError>>>; RING_SIZE],
    prev_pixels: Vec<u8>,

    // Chunk geometry (Phase 7: native MC geometry → D3D12)
    chunk_meshes: std::collections::HashMap<(i32, i32, i32), Vec<ChunkMesh>>,
    has_chunk_geometry: bool,

    // Chunk textured pipeline + atlas
    chunk_shader: Option<wgpu::ShaderModule>,  // stored for lazy pipeline creation
    chunk_pipeline: Option<wgpu::RenderPipeline>,
    chunk_bind_group: Option<wgpu::BindGroup>,
    chunk_bind_group_layout: Option<wgpu::BindGroupLayout>,
    atlas_texture: Option<wgpu::Texture>,
    atlas_sampler: wgpu::Sampler,
    atlas_width: u32,
    atlas_height: u32,
    /// Raw atlas pixels stored for diagnostics
    atlas_pixels: Option<Vec<u8>>,
}

// SAFETY: WmRenderer is only accessed from the JNI thread (Minecraft render thread).
// The Surface is !Send (tied to window system), but we never send it across threads.
unsafe impl Send for WmRenderer {}

impl WmRenderer {
    pub fn create(width: u32, height: u32) -> Result<Self, &'static str> {
        eprintln!("[dx12-wm] Creating WmRenderer {}x{} (triple-buffer + surface support)", width, height);
        log::info!("Creating WmRenderer {}x{}", width, height);

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
        eprintln!("[dx12-wm] Adapter: {:?}", adapter.get_info().name);

        let (device, queue) = futures::executor::block_on(adapter.request_device(
            &wgpu::DeviceDescriptor {
                label: Some("wgpu-mc"),
                required_features: wgpu::Features::empty(),
                required_limits: wgpu::Limits::default(),
                memory_hints: Default::default(),
            },
            None,
        ))
        .map_err(|_| "Device failed")?;
        eprintln!("[dx12-wm] Device created OK");

        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Main Shader"),
            source: wgpu::ShaderSource::Wgsl(std::borrow::Cow::Borrowed(SHADER_SRC)),
        });

        let uniform_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Camera Uniform"),
            size: 80, // mat4x4 (64) + vec3 (16 with padding)
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

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

        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Camera Bind Group"),
            layout: &bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: uniform_buffer.as_entire_binding(),
            }],
        });

        // No push constant ranges — compatible with all GPUs
        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Pipeline Layout"),
            bind_group_layouts: &[&bind_group_layout],
            push_constant_ranges: &[],
        });

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

        let (plane_vb, plane_ib, plane_count) =
            create_plane_mesh(&device, 20.0, -4.0, 8.0, [0.2, 0.65, 0.2]);

        // Create one cube VB per position (model offsets baked into vertices).
        // Positions are RELATIVE to camera (camera_pos added in shader).
        // y = -3.5: on the ground plane; spread over xz for visibility.
        let cube_positions: [([f32; 3], [f32; 3]); 5] = [
            ([-5.0, -3.5,  5.0], [0.9, 0.3, 0.15]),  // front-left, orange
            ([ 5.0, -3.5,  5.0], [0.15, 0.5, 0.9]),  // front-right, blue
            ([ 0.0, -1.0,  3.0], [0.9, 0.7, 0.1]),   // center, yellow
            ([-5.0, -3.5, 10.0], [0.4, 0.8, 0.2]),   // far front-left, green
            ([ 5.0, -3.5, 10.0], [0.7, 0.2, 0.5]),   // far front-right, magenta
        ];

        // Shared index buffer — all cubes use identical index data
        let cube_indices: [u16; 36] = [
             0,  1,  2,  2,  1,  3,
             4,  6,  5,  5,  6,  7,
             8,  9, 10, 10,  9, 11,
            12, 14, 13, 13, 14, 15,
            16, 17, 18, 18, 17, 19,
            20, 22, 21, 21, 22, 23,
        ];
        let cube_count = cube_indices.len() as u32;
        let cube_ib = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Cube IB (shared)"),
            size: std::mem::size_of_val(&cube_indices) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::INDEX,
            mapped_at_creation: true,
        });
        cube_ib.slice(..).get_mapped_range_mut()[..]
            .copy_from_slice(bytemuck::cast_slice(&cube_indices));
        cube_ib.unmap();

        let mut cube_vbs = Vec::with_capacity(cube_positions.len());
        for &(pos, color) in &cube_positions {
            cube_vbs.push(create_cube_mesh_at(&device, color, (pos[0], pos[1], pos[2])));
        }

        // ---- Textured fullscreen quad pipeline (GL framebuffer → D3D12) ----
        let tex_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Texture Shader"),
            source: wgpu::ShaderSource::Wgsl(TEX_SHADER_SRC.into()),
        });

        let tex_bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Texture BGL"),
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Texture {
                        sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        view_dimension: wgpu::TextureViewDimension::D2,
                        multisampled: false,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                    count: None,
                },
            ],
        });

        let tex_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Texture Pipeline Layout"),
            bind_group_layouts: &[&tex_bind_group_layout],
            push_constant_ranges: &[],
        });

        let tex_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Texture Pipeline"),
            layout: Some(&tex_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &tex_shader,
                entry_point: Some("vs_main"),
                buffers: &[TexVertex::desc()],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &tex_shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: wgpu::TextureFormat::Rgba8UnormSrgb,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                cull_mode: None,
                ..Default::default()
            },
            depth_stencil: None,
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        let tex_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            label: Some("Texture Sampler"),
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            mag_filter: wgpu::FilterMode::Nearest,
            min_filter: wgpu::FilterMode::Nearest,
            ..Default::default()
        });

        // Fullscreen quad (two triangles covering NDC [-1,1]²)
        let quad_vertices: [TexVertex; 4] = [
            TexVertex { position: [-1.0, -1.0], uv: [0.0, 1.0] },
            TexVertex { position: [ 1.0, -1.0], uv: [1.0, 1.0] },
            TexVertex { position: [-1.0,  1.0], uv: [0.0, 0.0] },
            TexVertex { position: [ 1.0,  1.0], uv: [1.0, 0.0] },
        ];
        let quad_indices: [u16; 6] = [0, 1, 2, 1, 3, 2];

        let fs_quad_vb = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("FS Quad VB"),
            size: std::mem::size_of_val(&quad_vertices) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::VERTEX,
            mapped_at_creation: true,
        });
        fs_quad_vb.slice(..).get_mapped_range_mut()[..]
            .copy_from_slice(bytemuck::cast_slice(&quad_vertices));
        fs_quad_vb.unmap();

        let fs_quad_ib = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("FS Quad IB"),
            size: std::mem::size_of_val(&quad_indices) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::INDEX,
            mapped_at_creation: true,
        });
        fs_quad_ib.slice(..).get_mapped_range_mut()[..]
            .copy_from_slice(bytemuck::cast_slice(&quad_indices));
        fs_quad_ib.unmap();

        let slots = [
            Slot::new(&device, width, height),
            Slot::new(&device, width, height),
            Slot::new(&device, width, height),
        ];

        eprintln!("[dx12-wm] Offscreen slots created ({}x{})", width, height);

        // Chunk atlas sampler: use Linear filtering for smooth block textures
        let atlas_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            label: Some("Atlas Sampler"),
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            mag_filter: wgpu::FilterMode::Nearest,
            min_filter: wgpu::FilterMode::Nearest,
            ..Default::default()
        });

        // Chunk bind group layout: camera uniform + atlas texture + sampler
        let chunk_bgl = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Chunk BGL"),
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Texture {
                        sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        view_dimension: wgpu::TextureViewDimension::D2,
                        multisampled: false,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 2,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                    count: None,
                },
            ],
        });

        let _chunk_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Chunk Pipeline Layout"),
            bind_group_layouts: &[&chunk_bgl],
            push_constant_ranges: &[],
        });

        eprintln!("[dx12-wm] Chunk pipeline layout created (texture atlas support ready)");

        Ok(Self {
            instance,
            adapter,
            device,
            queue,
            width,
            height,
            camera_mvp: IDENTITY,
            camera_prev: IDENTITY,
            camera_target: IDENTITY,
            camera_pos: [0.0; 3],
            pipeline,
            bind_group,
            uniform_buffer,
            plane_vb,
            plane_ib,
            plane_count,
            cube_vbs,
            cube_ib,
            cube_count,
            surface: None,
            surface_config: None,
            surface_format: wgpu::TextureFormat::Bgra8UnormSrgb,
            surface_depth: None,
            resize_pending: false,
            tex_pipeline,
            tex_bind_group: None,
            tex_sampler,
            frame_texture: None,
            frame_width: 0,
            frame_height: 0,
            fs_quad_vb,
            fs_quad_ib,
            slots,
            idx: 0,
            pending_rx: [None, None, None],
            prev_pixels: Vec::new(),
            chunk_meshes: std::collections::HashMap::new(),
            has_chunk_geometry: false,
            chunk_shader: None,
            chunk_pipeline: None,
            chunk_bind_group: None,
            chunk_bind_group_layout: Some(chunk_bgl),
            atlas_texture: None,
            atlas_sampler,
            atlas_width: 0,
            atlas_height: 0,
            atlas_pixels: None,
        })
    }

    /// Initialize a D3D12 swapchain surface on the given HWND.
    ///
    /// To avoid GPU TDR, the GL context MUST be temporarily detached from the HWND
    /// before calling renderFrame() (via GLFW.glfwMakeContextCurrent(0) on the Java
    /// side) and reattached after. This ensures D3D12's Present() has exclusive
    /// access to the HWND, preventing WDDM driver contention.
    pub fn init_surface(&mut self, hwnd: usize) {
        if self.surface.is_some() {
            return;
        }

        log::info!("[dx12-wm] init_surface: creating D3D12 swapchain on HWND 0x{:x} (parent HWND)",
            hwnd);

        let surface = match create_surface_from_hwnd(&self.instance, hwnd) {
            Some(s) => s,
            None => {
                log::error!("[dx12-wm] Failed to create wgpu surface from HWND 0x{:x}", hwnd);
                return;
            }
        };

        let caps = surface.get_capabilities(&self.adapter);
        // Prefer Rgba8UnormSrgb to match the pipeline format.
        // If not available, fall back to any sRGB format.
        let format = caps.formats.iter()
            .find(|f| **f == wgpu::TextureFormat::Rgba8UnormSrgb)
            .or_else(|| caps.formats.iter().find(|f| f.is_srgb()))
            .copied()
            .unwrap_or(caps.formats[0]);

        log::info!("[dx12-wm] Surface caps: format={:?}, {}x{}, present_modes={:?}",
            format, self.width, self.height, caps.present_modes);

        let present_mode = if caps.present_modes.contains(&wgpu::PresentMode::Immediate) {
            wgpu::PresentMode::Immediate
        } else if caps.present_modes.contains(&wgpu::PresentMode::Fifo) {
            wgpu::PresentMode::Fifo
        } else {
            caps.present_modes[0]
        };

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format,
            width: self.width,
            height: self.height,
            present_mode,
            alpha_mode: wgpu::CompositeAlphaMode::Opaque,
            view_formats: vec![],
            desired_maximum_frame_latency: 1,
        };

        surface.configure(&self.device, &config);

        // Create and cache depth texture (reused every frame)
        self.surface_depth = Some(make_depth_texture(&self.device, self.width, self.height));

        self.surface_format = format;
        self.surface_config = Some(config);
        self.surface = Some(surface);

        // Rebuild chunk pipeline with the actual surface format if atlas already uploaded.
        // Drop the old pipeline first — it may have been created with the wrong format.
        self.chunk_pipeline = None;
        self.ensure_chunk_pipeline();

        log::info!("[dx12-wm] Surface mode ENABLED on parent HWND: {:?} {}x{}",
            format, self.width, self.height);
        eprintln!("[dx12-wm] Surface mode ENABLED: {:?} {}x{}", format, self.width, self.height);
    }

    /// Returns true if the renderer has an active surface (swapchain mode).
    pub fn has_surface(&self) -> bool {
        self.surface.is_some()
    }

    /// Returns true if any MC chunk geometry has been uploaded.
    pub fn has_chunk_geometry(&self) -> bool {
        self.has_chunk_geometry
    }

    /// Upload MC terrain atlas texture for chunk rendering.
    /// `pixels` is RGBA8 data, width and height are atlas dimensions.
    pub fn upload_terrain_atlas(&mut self, pixels: &[u8], width: u32, height: u32) {
        if pixels.len() < (width * height * 4) as usize { return; }

        log::info!("[dx12-wm] Uploading terrain atlas: {}x{} ({} bytes)",
            width, height, pixels.len());

        self.atlas_width = width;
        self.atlas_height = height;
        self.atlas_pixels = Some(pixels.to_vec());  // stored for diagnostics

        let atlas = self.device.create_texture(&wgpu::TextureDescriptor {
            label: Some("Terrain Atlas"),
            size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8UnormSrgb,
            usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
            view_formats: &[],
        });

        self.queue.write_texture(
            wgpu::ImageCopyTexture {
                texture: &atlas,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            pixels,
            wgpu::ImageDataLayout {
                offset: 0,
                bytes_per_row: Some(width * 4),
                rows_per_image: Some(height),
            },
            wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        );

        // Diagnostic: verify first few pixels of the uploaded atlas
        if pixels.len() >= 16 {
            let r = pixels[0]; let g = pixels[1]; let b = pixels[2]; let a = pixels[3];
            eprintln!("[dx12-wm] Atlas first pixel: RGBA=({},{},{},{})", r, g, b, a);
        }
        eprintln!("[dx12-wm] Atlas texture uploaded: {}x{} ({:.1} MB)", width, height, pixels.len() as f64 / 1048576.0);

        // Save atlas as PNG for visual debugging (open in Photoshop/GIMP to inspect texture positions)
        let atlas_path = std::path::Path::new("atlas_debug.png");
        if let Err(e) = image::save_buffer(atlas_path, pixels, width, height, image::ColorType::Rgba8) {
            log::warn!("[dx12-wm] Failed to save atlas PNG: {}", e);
        } else {
            log::info!("[dx12-wm] Atlas saved to atlas_debug.png ({}x{})", width, height);
            eprintln!("[dx12-wm] Atlas saved to atlas_debug.png ({}x{})", width, height);
        }

        // Create the chunk bind group
        let atlas_view = atlas.create_view(&wgpu::TextureViewDescriptor::default());

        let chunk_bgl = self.chunk_bind_group_layout.as_ref().unwrap();
        let chunk_bg = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Chunk Bind Group"),
            layout: chunk_bgl,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: self.uniform_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: wgpu::BindingResource::TextureView(&atlas_view),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: wgpu::BindingResource::Sampler(&self.atlas_sampler),
                },
            ],
        });

        self.atlas_texture = Some(atlas);
        self.chunk_bind_group = Some(chunk_bg);

        // Create and store the chunk shader for lazy pipeline creation.
        // Pipeline is built via ensure_chunk_pipeline() so it uses the
        // correct surface format (not hardcoded Rgba8UnormSrgb).
        self.chunk_shader = Some(self.device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Chunk Shader"),
            source: wgpu::ShaderSource::Wgsl(CHUNK_SHADER_SRC.into()),
        }));
        self.ensure_chunk_pipeline();
    }

    pub fn set_camera(&mut self, mvp: [[f32; 4]; 4]) {
        self.camera_target = mvp;
    }

    /// Set the camera world position (used to offset geometry near the camera).
    pub fn set_camera_pos(&mut self, x: f32, y: f32, z: f32) {
        self.camera_pos = [x, y, z];
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        if width == 0 || height == 0 {
            return;
        }
        self.width = width;
        self.height = height;

        // Surface mode: only update stored config dimensions.
        // Do NOT call surface.configure() or recreate depth here —
        // DXGI ResizeBuffers can throw a C++ exception when called while
        // GL is active on the same HWND.
        // The depth texture is recreated alongside the swapchain reconfig
        // in render_surface() when get_current_texture() returns Lost/Outdated.
        if let (Some(_), Some(ref mut config)) = (&self.surface, &mut self.surface_config) {
            config.width = width;
            config.height = height;
            self.resize_pending = true;
            log::info!("[dx12-wm] Surface resize pending to {}x{} (reconfig in render_surface)", width, height);
            return;
        }

        // Recreate offscreen slots
        for slot in self.slots.iter_mut() {
            *slot = Slot::new(&self.device, width, height);
        }
        self.idx = 0;
        self.pending_rx = [None, None, None];
    }

    /// Render one frame. Returns empty Vec in surface mode (D3D12 presents directly).
    pub fn render_frame(&mut self) -> Vec<u8> {
        if self.surface.is_some() {
            self.render_surface();
            return Vec::new();
        }
        self.render_offscreen()
    }

    /// Upload RGBA8 pixel data from GL framebuffer capture as a D3D12 texture.
    pub fn set_frame_pixels(&mut self, data: &[u8], width: u32, height: u32) {
        if width == 0 || height == 0 { return; }
        let size = (width * height * 4) as usize;

        // D3D12 requires bytes_per_row to be a multiple of 256.
        // glReadPixels returns tightly-packed rows, so we must repack.
        const ROW_ALIGN: u32 = 256;
        let src_row_bytes = width * 4;
        let dst_row_bytes = ((src_row_bytes + ROW_ALIGN - 1) / ROW_ALIGN) * ROW_ALIGN;
        let padded_size = (dst_row_bytes * height) as usize;

        // Recreate texture if dimensions changed
        let need_new = self.frame_texture.as_ref().map_or(true, |_t| {
            self.frame_width != width || self.frame_height != height
        });
        if need_new {
            log::info!("[dx12-wm] Creating new frame texture {}x{} (row {}→{} padded)",
                width, height, src_row_bytes, dst_row_bytes);
            let tex = self.device.create_texture(&wgpu::TextureDescriptor {
                label: Some("Frame Texture"),
                size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                mip_level_count: 1,
                sample_count: 1,
                dimension: wgpu::TextureDimension::D2,
                format: wgpu::TextureFormat::Rgba8UnormSrgb,
                usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
                view_formats: &[],
            });
            let view = tex.create_view(&wgpu::TextureViewDescriptor::default());
            let bind_group = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
                label: Some("Frame Bind Group"),
                layout: &self.tex_pipeline.get_bind_group_layout(0),
                entries: &[
                    wgpu::BindGroupEntry {
                        binding: 0,
                        resource: wgpu::BindingResource::TextureView(&view),
                    },
                    wgpu::BindGroupEntry {
                        binding: 1,
                        resource: wgpu::BindingResource::Sampler(&self.tex_sampler),
                    },
                ],
            });
            self.frame_texture = Some(tex);
            self.tex_bind_group = Some(bind_group);
            self.frame_width = width;
            self.frame_height = height;
            log::info!("[dx12-wm] Frame texture + bind_group created OK");
        }

        // Upload pixel data with D3D12-aligned row pitch (pad if needed)
        if let Some(ref tex) = self.frame_texture {
            let effective_len = data.len().min(size);
            if dst_row_bytes == src_row_bytes {
                // Tightly packed — direct upload
                self.queue.write_texture(
                    wgpu::ImageCopyTexture {
                        texture: tex,
                        mip_level: 0,
                        origin: wgpu::Origin3d::ZERO,
                        aspect: wgpu::TextureAspect::All,
                    },
                    &data[..effective_len],
                    wgpu::ImageDataLayout {
                        offset: 0,
                        bytes_per_row: Some(src_row_bytes),
                        rows_per_image: Some(height),
                    },
                    wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                );
            } else {
                // Repack: copy each row from tightly-packed source to padded destination
                let mut padded = vec![0u8; padded_size];
                let src = &data[..effective_len];
                for row in 0..height as usize {
                    let src_start = row * src_row_bytes as usize;
                    let dst_start = row * dst_row_bytes as usize;
                    let row_len = src_row_bytes as usize;
                    if src_start + row_len <= src.len() && dst_start + row_len <= padded.len() {
                        padded[dst_start..dst_start + row_len]
                            .copy_from_slice(&src[src_start..src_start + row_len]);
                    }
                }
                self.queue.write_texture(
                    wgpu::ImageCopyTexture {
                        texture: tex,
                        mip_level: 0,
                        origin: wgpu::Origin3d::ZERO,
                        aspect: wgpu::TextureAspect::All,
                    },
                    &padded,
                    wgpu::ImageDataLayout {
                        offset: 0,
                        bytes_per_row: Some(dst_row_bytes),
                        rows_per_image: Some(height),
                    },
                    wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
                );
            }
        }
    }

    /// Store a chunk section mesh for D3D12 rendering.
    /// `data` contains MC vertex data (28 bytes/vertex for BLOCK format in MC 26.1.2).
    /// `section_x/y/z` are chunk section coordinates (world coord >> 4).
    pub fn upload_chunk_mesh(
        &mut self,
        section_x: i32,
        section_y: i32,
        section_z: i32,
        data: &[u8],
        vertex_count: u32,
        vertex_stride: u32,
    ) {
        let stride = vertex_stride as usize;
        let expected_size = (vertex_count as usize) * stride;
        if data.len() < expected_size || vertex_count == 0 || stride == 0 {
            log::warn!("[dx12-wm] Chunk mesh REJECTED: data.len={} < expected={}, vcount={}, stride={}",
                data.len(), expected_size, vertex_count, stride);
            return;
        }

        // MC uses GL_QUADS: 4 vertices per quad.
        // D3D12 uses TriangleList: need 6 indices per quad.
        let quad_count = vertex_count / 4;
        let tri_index_count = quad_count * 6;         // 6 indices per quad

        let world_ox = (section_x as f32) * 16.0;
        let world_oy = (section_y as f32) * 16.0;
        let world_oz = (section_z as f32) * 16.0;

        // Convert MC vertex data to ChunkVertex (position + color + uv) and build index buffer.
        // MC 26.1.2 BLOCK format (28 bytes): Pos(12) + Color(4) + UV0(8) + UV2(4)
        let mut vertices: Vec<ChunkVertex> = Vec::with_capacity(vertex_count as usize);
        let max_index = vertex_count;

        // Use u32 indices if vertex count exceeds u16 max
        let use_u32_indices = max_index > 65535;

        for v in 0..vertex_count {
            let base = (v as usize) * stride;
            // Need at least 24 bytes: Pos(12) + Color(4) + UV(8)
            if base + 24 > data.len() { break; }

            // Position: 3 f32 at offset 0 (section-relative, 0..16)
            let px = f32::from_le_bytes([data[base], data[base+1], data[base+2], data[base+3]]);
            let py = f32::from_le_bytes([data[base+4], data[base+5], data[base+6], data[base+7]]);
            let pz = f32::from_le_bytes([data[base+8], data[base+9], data[base+10], data[base+11]]);

            // Color: 4 u8 (RGBA in memory) at offset 12 → float (used as lighting tint)
            let cr = data[base + 12] as f32 / 255.0;
            let cg = data[base + 13] as f32 / 255.0;
            let cb = data[base + 14] as f32 / 255.0;

            // UV: 2 f32 at offset 16 (texture atlas coords)
            let u = f32::from_le_bytes([data[base+16], data[base+17], data[base+18], data[base+19]]);
            let v_uv = f32::from_le_bytes([data[base+20], data[base+21], data[base+22], data[base+23]]);

            // Apply UV offset to correct systematic shift of MC vertex UVs vs atlas.
            // MC chunk vertex UVs are offset by (+16,+16) atlas pixels relative to where
            // sprites actually are in the composited atlas.  Offset = -16/2048 = -0.0078125.
            const UV_OFFSET: f32 = -16.0 / 2048.0;
            let u_corrected = (u + UV_OFFSET).clamp(0.0, 1.0);
            let v_corrected = (v_uv + UV_OFFSET).clamp(0.0, 1.0);

            // World position (section origin + local pos).
            // Store directly in world space so the shader MVP transform is consistent
            // regardless of when the chunk was uploaded.
            let wx = px + world_ox;
            let wy = py + world_oy;
            let wz = pz + world_oz;

            vertices.push(ChunkVertex {
                position: [wx, wy, wz],
                color: [cr, cg, cb],
                uv: [u_corrected, v_corrected],
            });
        }

        // Diagnostic: dump first 4 vertices + atlas area on first chunk upload
        static mut FIRST_UPLOAD: bool = true;
        if unsafe { FIRST_UPLOAD } {
            unsafe { FIRST_UPLOAD = false; }
            eprintln!("[dx12-wm] First chunk upload: section=({},{},{}) stride={} vcount={} len={} camera=({:.1},{:.1},{:.1})",
                section_x, section_y, section_z, stride, vertex_count, data.len(),
                self.camera_pos[0], self.camera_pos[1], self.camera_pos[2]);
            // Dump raw bytes of first vertex to verify format
            if data.len() >= 28 {
                let raw = &data[0..28];
                eprintln!("[dx12-wm]   RAW v0 bytes: {:02X?}", raw);
                // Try reading UV at different offsets
                for off in [16usize, 20, 12, 8] {
                    if off + 8 <= data.len() {
                        let u = f32::from_le_bytes([data[off], data[off+1], data[off+2], data[off+3]]);
                        let v_val = f32::from_le_bytes([data[off+4], data[off+5], data[off+6], data[off+7]]);
                        eprintln!("[dx12-wm]     UV attempt at offset {}: ({:.6}, {:.6})", off, u, v_val);
                    }
                }
                // Check bytes at offset 24-27 (UV2/lightmap)
                if data.len() >= 28 {
                    let uv2_u = u16::from_le_bytes([data[24], data[25]]);
                    let uv2_v = u16::from_le_bytes([data[26], data[27]]);
                    eprintln!("[dx12-wm]     UV2 as u16 at offset 24: ({}, {})", uv2_u, uv2_v);
                }
                // Check if offset 24-27 are normal (bytes)
                eprintln!("[dx12-wm]     Normal at offset 24: ({}, {}, {})", data[24], data[25], data[26]);
            }
            for i in 0..vertices.len().min(4) {
                let v = &vertices[i];
                eprintln!("[dx12-wm]   v[{}]: pos=({:.2},{:.2},{:.2}) color=({:.3},{:.3},{:.3}) uv=({:.4},{:.4})",
                    i, v.position[0], v.position[1], v.position[2],
                    v.color[0], v.color[1], v.color[2],
                    v.uv[0], v.uv[1]);
            }
            // Dump atlas pixel grid for all 4 corners of the first quad
            if let Some(ref pixels) = self.atlas_pixels {
                let aw = self.atlas_width as usize;
                let ah = self.atlas_height as usize;
                for vi in 0..vertices.len().min(4) {
                    let u = vertices[vi].uv[0].clamp(0.0, 1.0);
                    let v_uv = vertices[vi].uv[1].clamp(0.0, 1.0);
                    let px = (u * aw as f32) as usize;
                    let py = (v_uv * ah as f32) as usize;
                    let off = (py * aw + px) * 4;
                    if off + 4 <= pixels.len() {
                        eprintln!("[dx12-wm]   v[{}] atlas ({},{}) RGBA=({},{},{},{})",
                            vi, px, py,
                            pixels[off], pixels[off+1], pixels[off+2], pixels[off+3]);
                    } else {
                        eprintln!("[dx12-wm]   v[{}] atlas ({},{}) OUT OF BOUNDS", vi, px, py);
                    }
                }
                // Dump a 4x4 grid of pixels inside the first quad (16x16 atlas area)
                // Show 5 sample pixels per row: start, 25%, 50%, 75%, end
                let aw_f = self.atlas_width as f32;
                let ah_f = self.atlas_height as f32;
                let u0 = vertices[0].uv[0].clamp(0.0, 1.0);
                let u1 = vertices[1].uv[0].clamp(0.0, 1.0);
                let v0 = vertices[0].uv[1].clamp(0.0, 1.0);
                let v2 = vertices[2].uv[1].clamp(0.0, 1.0);
                eprintln!("[dx12-wm]   16x16 atlas quad uv_x=[{:.4},{:.4}] uv_y=[{:.4},{:.4}]",
                    u0.min(u1), u0.max(u1), v0.min(v2), v0.max(v2));
                for row_pct in [0.0, 0.25, 0.5, 0.75, 1.0] {
                    let row = v0 + (v2 - v0) * row_pct as f32;
                    let py = (row * ah_f) as usize;
                    let mut line = format!("[dx12-wm]   row y={:.1}% (pixel y={}):", row_pct * 100.0, py);
                    for col_pct in [0.0, 0.25, 0.5, 0.75, 1.0] {
                        let col = u0 + (u1 - u0) * col_pct as f32;
                        let px = (col * aw_f) as usize;
                        let off = (py * aw + px) * 4;
                        if off + 4 <= pixels.len() {
                            let r = pixels[off]; let g = pixels[off+1];
                            let b = pixels[off+2]; let a = pixels[off+3];
                            line.push_str(&format!(" ({},{})→({},{},{},{})", px, py, r, g, b, a));
                        }
                    }
                    eprintln!("{}", line);
                }
            }
        }

        if vertices.is_empty() { return; }

        // Build per-quad triangle indices
        if use_u32_indices {
            let mut indices: Vec<u32> = Vec::with_capacity(tri_index_count as usize);
            for q in 0..quad_count {
                let vi = q * 4;
                if vi + 3 >= vertex_count { break; }
                indices.extend_from_slice(&[vi, vi+1, vi+2, vi, vi+2, vi+3]);
            }

            if indices.is_empty() { return; }

            let vb = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk VB"),
                size: (std::mem::size_of::<ChunkVertex>() * vertices.len()) as wgpu::BufferAddress,
                usage: wgpu::BufferUsages::VERTEX,
                mapped_at_creation: true,
            });
            vb.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
            vb.unmap();

            let ib = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk IB"),
                size: (std::mem::size_of::<u32>() * indices.len()) as wgpu::BufferAddress,
                usage: wgpu::BufferUsages::INDEX,
                mapped_at_creation: true,
            });
            ib.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&indices));
            ib.unmap();

            let mesh = ChunkMesh {
                vertex_buffer: vb,
                index_buffer: ib,
                vertex_count: vertices.len() as u32,
                index_count: indices.len() as u32,
                index_is_u32: true,
            };

            let key = (section_x, section_y, section_z);
            self.chunk_meshes.entry(key).or_insert_with(Vec::new).push(mesh);
            self.has_chunk_geometry = true;

            log::info!("[dx12-wm] Chunk mesh uploaded (u32): section=({},{},{}) {} verts, {} indices",
                section_x, section_y, section_z, vertices.len(), indices.len());
        } else {
            let mut indices: Vec<u16> = Vec::with_capacity(tri_index_count as usize);
            for q in 0..quad_count {
                let vi = (q * 4) as u16;
                if (vi as u32) + 3 >= vertex_count { break; }
                indices.extend_from_slice(&[vi, vi+1, vi+2, vi, vi+2, vi+3]);
            }

            if indices.is_empty() { return; }

            let vb = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk VB"),
                size: (std::mem::size_of::<ChunkVertex>() * vertices.len()) as wgpu::BufferAddress,
                usage: wgpu::BufferUsages::VERTEX,
                mapped_at_creation: true,
            });
            vb.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&vertices));
            vb.unmap();

            let ib = self.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Chunk IB"),
                size: (std::mem::size_of::<u16>() * indices.len()) as wgpu::BufferAddress,
                usage: wgpu::BufferUsages::INDEX,
                mapped_at_creation: true,
            });
            ib.slice(..).get_mapped_range_mut()[..].copy_from_slice(bytemuck::cast_slice(&indices));
            ib.unmap();

            let mesh = ChunkMesh {
                vertex_buffer: vb,
                index_buffer: ib,
                vertex_count: vertices.len() as u32,
                index_count: indices.len() as u32,
                index_is_u32: false,
            };

            let key = (section_x, section_y, section_z);
            self.chunk_meshes.entry(key).or_insert_with(Vec::new).push(mesh);
            self.has_chunk_geometry = true;

            log::info!("[dx12-wm] Chunk mesh uploaded: section=({},{},{}) {} verts, {} indices",
                section_x, section_y, section_z, vertices.len(), indices.len());
        }
    }

    /// Remove all chunk meshes for a given section.
    /// Called before recompiling a section to prevent stale mesh accumulation.
    pub fn clear_chunk_section(&mut self, section_x: i32, section_y: i32, section_z: i32) {
        let key = (section_x, section_y, section_z);
        self.chunk_meshes.remove(&key);
    }

    // ── Draw calls shared by surface and offscreen modes ──────────

    fn draw_scene<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        rp.set_pipeline(&self.pipeline);
        rp.set_bind_group(0, &self.bind_group, &[]);

        // Plane (identity model baked into vertices)
        rp.set_vertex_buffer(0, self.plane_vb.slice(..));
        rp.set_index_buffer(self.plane_ib.slice(..), wgpu::IndexFormat::Uint16);
        rp.draw_indexed(0..self.plane_count, 0, 0..1);

        // Cubes (each VB has pre-offsetted vertices)
        for cube_vb in &self.cube_vbs {
            rp.set_vertex_buffer(0, cube_vb.slice(..));
            rp.set_index_buffer(self.cube_ib.slice(..), wgpu::IndexFormat::Uint16);
            rp.draw_indexed(0..self.cube_count, 0, 0..1);
        }
    }

    /// Render all stored MC chunk meshes using the standard 3D pipeline.
    fn draw_chunks<'a>(&'a self, rp: &mut wgpu::RenderPass<'a>) {
        // Require chunk pipeline (with atlas texture) to be ready.
        // Don't fall back to main pipeline — vertex formats differ!
        let Some(pipeline) = &self.chunk_pipeline else { return; };
        let Some(bind_group) = &self.chunk_bind_group else { return; };

        static mut CHUNK_DRAW_FIRST: bool = true;
        if unsafe { CHUNK_DRAW_FIRST } {
            unsafe { CHUNK_DRAW_FIRST = false; }
            let total_meshes: usize = self.chunk_meshes.values().map(|v| v.len()).sum();
            eprintln!("[dx12-wm] draw_chunks: {} sections, {} meshes total", self.chunk_meshes.len(), total_meshes);
        }

        rp.set_pipeline(pipeline);
        rp.set_bind_group(0, bind_group, &[]);

        for (_key, meshes) in &self.chunk_meshes {
            for mesh in meshes {
                rp.set_vertex_buffer(0, mesh.vertex_buffer.slice(..));
                let index_format = if mesh.index_is_u32 {
                    wgpu::IndexFormat::Uint32
                } else {
                    wgpu::IndexFormat::Uint16
                };
                rp.set_index_buffer(mesh.index_buffer.slice(..), index_format);
                rp.draw_indexed(0..mesh.index_count, 0, 0..1);
            }
        }
    }

    /// Create or recreate the chunk render pipeline using the current surface format.
    /// Called after atlas upload and after surface initialization to ensure
    /// the color target format matches the swapchain.
    fn ensure_chunk_pipeline(&mut self) {
        if self.chunk_pipeline.is_some() { return; }

        let Some(shader) = &self.chunk_shader else { return; };
        let Some(bgl) = &self.chunk_bind_group_layout else { return; };

        let pipeline = self.device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Chunk Pipeline"),
            layout: Some(&self.device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("Chunk PL"),
                bind_group_layouts: &[bgl],
                push_constant_ranges: &[],
            })),
            vertex: wgpu::VertexState {
                module: shader,
                entry_point: Some("vs_main"),
                buffers: &[ChunkVertex::desc()],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format: self.surface_format,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
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

        self.chunk_pipeline = Some(pipeline);
        log::info!("[dx12-wm] Chunk pipeline created with format={:?}", self.surface_format);
        eprintln!("[dx12-wm] Chunk pipeline created with format={:?}", self.surface_format);
    }

    // ── Surface mode: render directly to swapchain ────────────────

    fn render_surface(&mut self) {
        let has_frame = self.tex_bind_group.is_some();
        let has_chunks = self.has_chunk_geometry;

        let surface = self.surface.as_ref().unwrap();

        // If a resize is pending, reconfigure the swapchain BEFORE acquiring
        // the next texture so that depth and color attachments stay in sync.
        if self.resize_pending {
            if let Some(config) = &self.surface_config {
                log::info!("[dx12-wm] Applying pending surface reconfig to {}x{}", config.width, config.height);
                surface.configure(&self.device, config);
                self.surface_depth = Some(make_depth_texture(&self.device, self.width, self.height));
            }
            self.resize_pending = false;
        }

        // Get surface frame
        let frame = match surface.get_current_texture() {
            Ok(f) => f,
            Err(wgpu::SurfaceError::Outdated | wgpu::SurfaceError::Lost) => {
                log::info!("[dx12-wm] Surface lost/outdated, reconfiguring");
                if let Some(config) = &self.surface_config {
                    surface.configure(&self.device, config);
                    self.surface_depth = Some(make_depth_texture(&self.device, self.width, self.height));
                }
                return;
            }
            Err(wgpu::SurfaceError::Timeout) => {
                log::warn!("[dx12-wm] Surface timeout — GPU TDR may have occurred; skipping frame");
                return;
            }
            Err(e) => {
                log::error!("[dx12-wm] Surface error: {:?}, falling back to offscreen", e);
                self.surface = None;
                self.surface_depth = None;
                self.surface_config = None;
                log::info!("[dx12-wm] Fallen back to offscreen rendering");
                return;
            }
        };

        let view = frame.texture.create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self.device.create_command_encoder(
            &wgpu::CommandEncoderDescriptor { label: Some("RenderSurface") },
        );

        // Wrap the fallible rendering in catch_unwind so we always present
        // the frame — otherwise the swapchain image stays acquired forever.
        let render_result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            if has_chunks {
                // Phase 7: Render native MC chunk geometry with D3D12
                self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
                self.camera_prev = self.camera_mvp;
                write_camera_uniform(&self.queue, &self.uniform_buffer, &self.camera_mvp, &self.camera_pos);

                let depth_view = self.surface_depth
                    .as_ref()
                    .expect("surface_depth must be created in init_surface")
                    .create_view(&wgpu::TextureViewDescriptor::default());

                {
                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Surface Pass (Chunks)"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Clear(wgpu::Color {
                                    r: 0.53, g: 0.81, b: 0.92, a: 1.0,
                                }),
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                            view: &depth_view,
                            depth_ops: Some(wgpu::Operations {
                                load: wgpu::LoadOp::Clear(1.0),
                                store: wgpu::StoreOp::Store,
                            }),
                            stencil_ops: None,
                        }),
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });

                    self.draw_chunks(&mut rp);
                }
            } else if has_frame {
                // Textured fullscreen quad: display GL framebuffer capture
                {
                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Surface Pass (Textured)"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Clear(wgpu::Color { r: 0.0, g: 0.0, b: 0.0, a: 1.0 }),
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: None,
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });
                    rp.set_pipeline(&self.tex_pipeline);
                    rp.set_bind_group(0, self.tex_bind_group.as_ref().unwrap(), &[]);
                    rp.set_vertex_buffer(0, self.fs_quad_vb.slice(..));
                    rp.set_index_buffer(self.fs_quad_ib.slice(..), wgpu::IndexFormat::Uint16);
                    rp.draw_indexed(0..6, 0, 0..1);
                }
            } else {
                // Fallback: 3D test scene (plane + cubes)
                self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
                self.camera_prev = self.camera_mvp;
                write_camera_uniform(&self.queue, &self.uniform_buffer, &self.camera_mvp, &self.camera_pos);

                {
                    let depth_view = self.surface_depth
                        .as_ref()
                        .expect("surface_depth must be created in init_surface")
                        .create_view(&wgpu::TextureViewDescriptor::default());

                    let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                        label: Some("Surface Pass (3D)"),
                        color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                            view: &view,
                            resolve_target: None,
                            ops: wgpu::Operations {
                                load: wgpu::LoadOp::Clear(wgpu::Color {
                                    r: 0.53, g: 0.81, b: 0.92, a: 1.0,
                                }),
                                store: wgpu::StoreOp::Store,
                            },
                        })],
                        depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                            view: &depth_view,
                            depth_ops: Some(wgpu::Operations {
                                load: wgpu::LoadOp::Clear(1.0),
                                store: wgpu::StoreOp::Store,
                            }),
                            stencil_ops: None,
                        }),
                        timestamp_writes: None,
                        occlusion_query_set: None,
                    });

                    self.draw_scene(&mut rp);
                }
            }

            self.queue.submit(Some(encoder.finish()));
        }));

        // Always present the frame, even if the render block panicked.
        // This releases the swapchain image so subsequent frames can acquire.
        match render_result {
            Ok(()) => {}
            Err(e) => {
                let msg = if let Some(s) = e.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = e.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "unknown panic".to_string()
                };
                log::error!("[dx12-wm] render_surface panicked: {}", msg);
            }
        }
        frame.present();
    }

    // ── Offscreen mode: triple-buffer readback ────────────────────

    fn render_offscreen(&mut self) -> Vec<u8> {
        let w = self.width;
        let h = self.height;
        if w == 0 || h == 0 {
            return Vec::new();
        }

        self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
        self.camera_prev = self.camera_mvp;

        write_camera_uniform(&self.queue, &self.uniform_buffer, &self.camera_mvp, &self.camera_pos);

        let slot = &self.slots[self.idx];
        let row_aligned = aligned_row(w);

        let mut encoder = self.device.create_command_encoder(
            &wgpu::CommandEncoderDescriptor { label: Some("Render") },
        );

        {
            let color_view = slot.color.create_view(&wgpu::TextureViewDescriptor::default());
            let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Main Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &color_view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.53, g: 0.81, b: 0.92, a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                    view: &slot.depth_view,
                    depth_ops: Some(wgpu::Operations {
                        load: wgpu::LoadOp::Clear(1.0),
                        store: wgpu::StoreOp::Store,
                    }),
                    stencil_ops: None,
                }),
                timestamp_writes: None,
                occlusion_query_set: None,
            });

            self.draw_scene(&mut rp);
        }

        encoder.copy_texture_to_buffer(
            wgpu::ImageCopyTexture {
                texture: &slot.color,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            wgpu::ImageCopyBuffer {
                buffer: &slot.staging,
                layout: wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(row_aligned),
                    rows_per_image: Some(h),
                },
            },
            wgpu::Extent3d { width: w, height: h, depth_or_array_layers: 1 },
        );

        self.queue.submit(Some(encoder.finish()));
        self.device.poll(wgpu::Maintain::Poll);

        {
            let slice = slot.staging.slice(..);
            let (tx, rx) = mpsc::channel();
            slice.map_async(wgpu::MapMode::Read, move |result| {
                let _ = tx.send(result);
            });
            self.device.poll(wgpu::Maintain::Poll);
            self.pending_rx[self.idx] = Some(rx);
        }

        let read_idx = (self.idx + RING_SIZE - 1) % RING_SIZE;
        let mut pixels = Vec::new();

        if let Some(rx) = self.pending_rx[read_idx].take() {
            match rx.recv_timeout(std::time::Duration::from_millis(0)) {
                Ok(Ok(())) => {
                    let data = self.slots[read_idx].staging.slice(..).get_mapped_range();
                    let row_aligned_usize = row_aligned as usize;
                    let actual_row = (w * 4) as usize;
                    let mut new_pixels = Vec::with_capacity(actual_row * h as usize);
                    for row in 0..h as usize {
                        let start = row * row_aligned_usize;
                        let end = start + actual_row;
                        new_pixels.extend_from_slice(&data[start..end]);
                    }
                    drop(data);
                    self.slots[read_idx].staging.unmap();
                    pixels = new_pixels;
                }
                _ => {
                    self.pending_rx[read_idx] = Some(rx);
                }
            }
        }

        if pixels.is_empty() && !self.prev_pixels.is_empty() {
            pixels = self.prev_pixels.clone();
        }
        if !pixels.is_empty() {
            self.prev_pixels = pixels.clone();
        }

        self.idx = (self.idx + 1) % RING_SIZE;
        pixels
    }
}
