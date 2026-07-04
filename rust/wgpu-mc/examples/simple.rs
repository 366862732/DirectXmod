//! Simple triangle test program: renders a colored triangle using wgpu
//! Run with: cargo run --example simple

use wgpu::util::DeviceExt;
use winit::{
    application::ApplicationHandler,
    event::WindowEvent,
    event_loop::{ActiveEventLoop, EventLoop},
    window::WindowId,
};

struct AppState {
    window: Option<std::sync::Arc<winit::window::Window>>,
    surface: Option<wgpu::Surface<'static>>,
    device: Option<wgpu::Device>,
    queue: Option<wgpu::Queue>,
    config: Option<wgpu::SurfaceConfiguration>,
    pipeline: Option<wgpu::RenderPipeline>,
    vertex_buffer: Option<wgpu::Buffer>,
}

impl ApplicationHandler for AppState {
    fn resumed(&mut self, event_loop: &ActiveEventLoop) {
        let window = std::sync::Arc::new(
            event_loop
                .create_window(
                    winit::window::WindowAttributes::default()
                        .with_inner_size(winit::dpi::LogicalSize::new(1280.0, 720.0))
                        .with_resizable(true)
                        .with_title("wgpu-mc — Triangle"),
                )
                .unwrap(),
        );

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::PRIMARY,
            ..Default::default()
        });

        // Clone Arc for surface (gives it 'static lifetime)
        let surface = instance.create_surface(window.clone()).unwrap();

        let adapter = futures::executor::block_on(
            instance.request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                ..Default::default()
            })
        ).expect("No suitable adapter found");

        let (device, queue) = futures::executor::block_on(
            adapter.request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("Triangle Device"),
                    required_features: wgpu::Features::empty(),
                    required_limits: wgpu::Limits::default(),
                    memory_hints: Default::default(),
                },
                None,
            )
        ).expect("Device request failed");

        let caps = surface.get_capabilities(&adapter);
        let format = caps.formats[0];
        let size = window.inner_size();

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format,
            width: size.width,
            height: size.height,
            present_mode: wgpu::PresentMode::Fifo,
            alpha_mode: caps.alpha_modes[0],
            view_formats: vec![],
            desired_maximum_frame_latency: 2,
        };

        surface.configure(&device, &config);

        // Create shader
        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Triangle Shader"),
            source: wgpu::ShaderSource::Wgsl(include_str!("../shaders/triangle.wgsl").into()),
        });

        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Triangle Pipeline Layout"),
            bind_group_layouts: &[],
            push_constant_ranges: &[],
        });

        let pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Triangle Pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: Some("vs_main"),
                buffers: &[wgpu::VertexBufferLayout {
                    array_stride: std::mem::size_of::<Vertex>() as wgpu::BufferAddress,
                    step_mode: wgpu::VertexStepMode::Vertex,
                    attributes: &[
                        wgpu::VertexAttribute {
                            offset: 0,
                            shader_location: 0,
                            format: wgpu::VertexFormat::Float32x2,
                        },
                        wgpu::VertexAttribute {
                            offset: 8,
                            shader_location: 1,
                            format: wgpu::VertexFormat::Float32x3,
                        },
                    ],
                }],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: Some("fs_main"),
                targets: &[Some(wgpu::ColorTargetState {
                    format,
                    blend: None,
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                strip_index_format: None,
                front_face: wgpu::FrontFace::Ccw,
                cull_mode: Some(wgpu::Face::Back),
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: None,
            multisample: wgpu::MultisampleState {
                count: 1,
                mask: !0,
                alpha_to_coverage_enabled: false,
            },
            multiview: None,
            cache: None,
        });

        // Vertex data: triangle with RGB colors
        let vertices: [Vertex; 3] = [
            Vertex::new( 0.0,  0.8, 1.0, 0.0, 0.0), // top — red
            Vertex::new(-0.8, -0.8, 0.0, 1.0, 0.0), // bottom-left — green
            Vertex::new( 0.8, -0.8, 0.0, 0.0, 1.0), // bottom-right — blue
        ];

        let vertex_buffer = device.create_buffer_init(
            &wgpu::util::BufferInitDescriptor {
                label: Some("Triangle Vertex Buffer"),
                contents: bytemuck::cast_slice(&vertices),
                usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_SRC,
            }
        );

        self.window = Some(window);
        self.surface = Some(surface);
        self.device = Some(device);
        self.queue = Some(queue);
        self.config = Some(config);
        self.pipeline = Some(pipeline);
        self.vertex_buffer = Some(vertex_buffer);
    }

    fn window_event(&mut self, _event_loop: &ActiveEventLoop, _window_id: WindowId, event: WindowEvent) {
        if let (Some(ref device), Some(ref mut config), Some(ref mut surface)) =
            (&self.device, &mut self.config, &mut self.surface)
        {
            match event {
                WindowEvent::Resized(size) => {
                    if size.width > 0 && size.height > 0 {
                        config.width = size.width;
                        config.height = size.height;
                        surface.configure(device, config);
                    }
                }
                _ => {}
            }
        }
    }

    fn about_to_wait(&mut self, _event_loop: &ActiveEventLoop) {
        if let (Some(ref device), Some(ref queue), Some(ref config), Some(ref surface),
                Some(ref pipeline), Some(ref vertex_buffer)) =
            (&self.device, &self.queue, &self.config, &mut self.surface,
             &self.pipeline, &self.vertex_buffer)
        {
            let output = match surface.get_current_texture() {
                Ok(frame) => frame,
                Err(wgpu::SurfaceError::Timeout | wgpu::SurfaceError::Outdated) => return,
                Err(e) => {
                    log::error!("Render surface error: {:?}", e);
                    return;
                }
            };

            let view = output.texture.create_view(&wgpu::TextureViewDescriptor::default());

            let mut encoder = device.create_command_encoder(
                &wgpu::CommandEncoderDescriptor { label: Some("Render Encoder") }
            );

            {
                let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                    label: Some("Triangle Pass"),
                    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                        view: &view,
                        resolve_target: None,
                        ops: wgpu::Operations {
                            load: wgpu::LoadOp::Clear(wgpu::Color {
                                r: 0.1, g: 0.1, b: 0.2, a: 1.0,
                            }),
                            store: wgpu::StoreOp::Store,
                        },
                    })],
                    depth_stencil_attachment: None,
                    timestamp_writes: None,
                    occlusion_query_set: None,
                });
                render_pass.set_pipeline(pipeline);
                render_pass.set_vertex_buffer(0, vertex_buffer.slice(..));
                render_pass.draw(0..3, 0..1);
            }

            queue.submit(std::iter::once(encoder.finish()));
            output.present();
        }
    }
}

#[repr(C)]
#[derive(Copy, Clone, bytemuck::Pod, bytemuck::Zeroable)]
struct Vertex {
    position: [f32; 2],
    color: [f32; 3],
}

impl Vertex {
    fn new(x: f32, y: f32, r: f32, g: f32, b: f32) -> Self {
        Self {
            position: [x, y],
            color: [r, g, b],
        }
    }
}

fn main() {
    let event_loop = EventLoop::new().unwrap();
    let mut app_state = AppState {
        window: None,
        surface: None,
        device: None,
        queue: None,
        config: None,
        pipeline: None,
        vertex_buffer: None,
    };

    event_loop.run_app(&mut app_state).unwrap();
}
