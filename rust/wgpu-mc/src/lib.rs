//! wgpu-mc: wgpu renderer with depth buffer, geometry pipeline, and readback.
//!
//! Architecture: Triple-buffered ring on JNI thread with optional surface mode.
//! - Surface mode:  render directly to window swapchain, present → no readback
//! - Offscreen mode: render to textures, async readback via triple-buffer ring
//!
//! Note: Push constants removed — not supported on all GPUs.
//! Model transforms are baked into vertex buffers at creation time.
//!
//! Surface mode uses a **child overlay window** to avoid HWND sharing conflicts
//! between D3D12 (DXGI) and OpenGL (WGL). A WS_CHILD window is created as a
//! child of Minecraft's main window. D3D12 swapchain targets this child HWND,
//! completely isolating it from the GL context on the parent HWND.
//! This eliminates the GPU driver TDR from D3D12/GL HWND contention.

use bytemuck::{Pod, Zeroable};
use std::sync::mpsc;

// Win32 API for child overlay window creation
use windows_sys::Win32::Foundation::*;
use windows_sys::Win32::UI::WindowsAndMessaging::*;

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

// No push constants — compatible with all GPUs
const SHADER_SRC: &str = r#"
struct CameraUniform {
    view_proj: mat4x4<f32>,
}
@group(0) @binding(0) var<uniform> camera: CameraUniform;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
}

@vertex
fn vs_main(@location(0) pos: vec3<f32>, @location(1) color: vec3<f32>) -> VertexOutput {
    var out: VertexOutput;
    out.position = camera.view_proj * vec4<f32>(pos, 1.0);
    out.color = color;
    return out;
}

@fragment
fn fs_main(@location(0) color: vec3<f32>) -> @location(0) vec4<f32> {
    return vec4<f32>(color, 1.0);
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
            eprintln!("[dx12-wm] create_surface_unsafe failed: {:?}", e);
            None
        }
    }
}

/// Create a WS_CHILD overlay window of `parent_hwnd` for the D3D12 swapchain.
/// This avoids sharing Minecraft's main HWND between OpenGL (WGL) and D3D12 (DXGI),
/// which causes GPU driver TDR timeouts on some configurations.
fn create_child_overlay(parent_hwnd: usize, width: u32, height: u32) -> usize {
    unsafe {
        let parent = parent_hwnd as HWND;
        let instance = GetWindowLongW(parent, GWLP_HINSTANCE);

        // Use built-in STATIC class (pre-registered in every Win32 process).
        // Any window class works for D3D12 swapchain targets.
        let class_name: Vec<u16> = "STATIC\0".encode_utf16().collect();

        let child = CreateWindowExW(
            WS_EX_NOACTIVATE | WS_EX_TRANSPARENT,
            class_name.as_ptr(),
            std::ptr::null(),
            WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS,
            0,
            0,
            width as i32,
            height as i32,
            parent,
            std::ptr::null_mut(),
            instance as HINSTANCE,
            std::ptr::null_mut(),
        );

        if child.is_null() {
            log::error!("[dx12-wm] CreateWindowExW failed (child overlay): error={}", GetLastError());
            return 0;
        }

        let hwnd = child as usize;
        log::info!("[dx12-wm] Created child overlay window: HWND 0x{:x} (parent=0x{:x}, {}x{})",
            hwnd, parent_hwnd, width, height);
        hwnd
    }
}

/// Resize/move the child overlay window to match parent client area.
fn resize_child_overlay(child_hwnd: isize, width: u32, height: u32) {
    unsafe {
        SetWindowPos(
            child_hwnd as HWND,
            std::ptr::null_mut(), // HWND_TOP
            0,
            0,
            width as i32,
            height as i32,
            SWP_NOACTIVATE | SWP_NOZORDER | SWP_SHOWWINDOW,
        );
    }
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
    child_hwnd: Option<isize>,       // Child overlay window HWND
    surface: Option<wgpu::Surface<'static>>,
    surface_config: Option<wgpu::SurfaceConfiguration>,
    surface_format: wgpu::TextureFormat,
    surface_depth: Option<wgpu::Texture>,  // Cached depth texture (reused per-frame)

    // Offscreen mode (triple-buffer readback)
    slots: [Slot; RING_SIZE],
    idx: usize,
    pending_rx: [Option<mpsc::Receiver<Result<(), wgpu::BufferAsyncError>>>; RING_SIZE],
    prev_pixels: Vec<u8>,
}

// SAFETY: WmRenderer is only accessed from the JNI thread (Minecraft render thread).
// The Surface is !Send (tied to window system), but we never send it across threads.
unsafe impl Send for WmRenderer {}

impl Drop for WmRenderer {
    fn drop(&mut self) {
        if let Some(hwnd) = self.child_hwnd.take() {
            unsafe {
                DestroyWindow(hwnd as HWND);
            }
            log::info!("[dx12-wm] Destroyed child overlay window");
        }
    }
}

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
            size: 64,
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
            create_plane_mesh(&device, 200.0, [0.2, 0.65, 0.2]);

        // Create one cube VB per position (model offsets baked into vertices)
        let cube_color = [0.8, 0.4, 0.1];
        let cube_positions: [(f32, f32, f32); 5] = [
            (0.0, 1.0, 0.0), (4.0, 1.0, 2.0), (-4.0, 1.0, -1.0),
            (2.0, 1.0, -4.0), (-3.0, 1.0, 3.0),
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
        for &pos in &cube_positions {
            cube_vbs.push(create_cube_mesh_at(&device, cube_color, pos));
        }

        let slots = [
            Slot::new(&device, width, height),
            Slot::new(&device, width, height),
            Slot::new(&device, width, height),
        ];

        eprintln!("[dx12-wm] Offscreen slots created ({}x{})", width, height);

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
            child_hwnd: None,
            slots,
            idx: 0,
            pending_rx: [None, None, None],
            prev_pixels: Vec::new(),
        })
    }

    /// Initialize a D3D12 swapchain surface on a child overlay window of the given HWND.
    /// Called from JNI when the Minecraft window handle becomes available.
    ///
    /// To avoid GPU TDR (HWND sharing conflict between D3D12 and OpenGL),
    /// we create a WS_CHILD overlay window of Minecraft's main window and
    /// create the D3D12 swapchain on this child HWND instead of on the
    /// OpenGL-bound parent HWND. This completely isolates the two APIs.
    pub fn init_surface(&mut self, parent_hwnd: usize) {
        if self.surface.is_some() {
            return;
        }

        log::info!("[dx12-wm] init_surface: creating child overlay + D3D12 swapchain (parent HWND 0x{:x})",
            parent_hwnd);

        // Create child overlay window (D3D12 swapchain target, NOT the GL-bound parent)
        let child_hwnd = create_child_overlay(parent_hwnd, self.width, self.height);
        if child_hwnd == 0 {
            log::error!("[dx12-wm] Failed to create child overlay window — falling back to offscreen");
            return;
        }
        self.child_hwnd = Some(child_hwnd as isize);

        let surface = match create_surface_from_hwnd(&self.instance, child_hwnd) {
            Some(s) => s,
            None => {
                log::error!("[dx12-wm] Failed to create wgpu surface from child HWND 0x{:x}", child_hwnd);
                return;
            }
        };

        let caps = surface.get_capabilities(&self.adapter);
        let format = caps.formats.iter()
            .find(|f| f.is_srgb())
            .copied()
            .unwrap_or(caps.formats[0]);

        log::info!("[dx12-wm] Surface caps: format={:?}, present_modes={:?}", format, caps.present_modes);

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

        log::info!("[dx12-wm] Surface mode ENABLED on child overlay: {:?} {}x{}",
            format, self.width, self.height);
    }

    /// Returns true if the renderer has an active surface (swapchain mode).
    pub fn has_surface(&self) -> bool {
        self.surface.is_some()
    }

    pub fn set_camera(&mut self, mvp: [[f32; 4]; 4]) {
        self.camera_target = mvp;
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        if width == 0 || height == 0 {
            return;
        }
        self.width = width;
        self.height = height;

        // Surface mode: resize child overlay + update stored config + recreate depth texture.
        // Do NOT call surface.configure() here — DXGI ResizeBuffers can throw
        // a C++ exception when called while GL is active on the same HWND.
        // Instead, the swapchain is resized lazily in render_surface()
        // when get_current_texture() returns SurfaceError::Lost/Outdated.
        // Child overlay window is resized immediately (it's a separate HWND,
        // no GL contention).
        if let (Some(_), Some(ref mut config)) = (&self.surface, &mut self.surface_config) {
            config.width = width;
            config.height = height;
            // Resize child overlay window to match parent client area
            if let Some(child_hwnd) = self.child_hwnd {
                resize_child_overlay(child_hwnd, width, height);
            }
            self.surface_depth = Some(make_depth_texture(&self.device, width, height));
            log::info!("[dx12-wm] Surface size updated to {}x{} (lazy resize in render_surface)", width, height);
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

    // ── Surface mode: render directly to swapchain ────────────────

    fn render_surface(&mut self) {
        // Lerp camera
        self.camera_mvp = mat4_lerp(&self.camera_prev, &self.camera_target, LERP_FACTOR);
        self.camera_prev = self.camera_mvp;

        // Write camera VP uniform
        self.queue.write_buffer(&self.uniform_buffer, 0,
            bytemuck::cast_slice(&self.camera_mvp));

        // Get surface frame
        let surface = self.surface.as_ref().unwrap();
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
                // Do not reconfigure; wait for Lost/Outdated on next frame
                return;
            }
            Err(e) => {
                log::error!("[dx12-wm] Surface error: {:?}, falling back to offscreen", e);
                // Fall back to offscreen mode: destroy surface + child window
                if let Some(hwnd) = self.child_hwnd.take() {
                    unsafe { DestroyWindow(hwnd as HWND); }
                }
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

        {
            // Use cached depth texture (reused every frame, recreated on resize)
            let depth_view = self.surface_depth
                .as_ref()
                .expect("surface_depth must be created in init_surface")
                .create_view(&wgpu::TextureViewDescriptor::default());

            let mut rp = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Surface Pass"),
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

        self.queue.submit(Some(encoder.finish()));
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

        self.queue.write_buffer(&self.uniform_buffer, 0,
            bytemuck::cast_slice(&self.camera_mvp));

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
