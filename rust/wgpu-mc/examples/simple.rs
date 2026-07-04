//! Simple test program: renders a blue screen using wgpu directly
//! Run with: cargo run --example simple

use winit::{
    application::ApplicationHandler,
    event::WindowEvent,
    event_loop::{ActiveEventLoop, EventLoop},
    window::WindowId,
};

struct AppState {
    surface: Option<wgpu::Surface<'static>>,
    device: Option<wgpu::Device>,
    queue: Option<wgpu::Queue>,
    config: Option<wgpu::SurfaceConfiguration>,
    size: winit::dpi::PhysicalSize<u32>,
}

impl ApplicationHandler for AppState {
    fn resumed(&mut self, event_loop: &ActiveEventLoop) {
        use wgpu::util::DeviceExt;

        let size = self.size;

        if size.width == 0 || size.height == 0 {
            return;
        }

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::PRIMARY,
            ..Default::default()
        });

        let window = event_loop
            .create_window(
                winit::window::WindowAttributes::default()
                    .with_inner_size(winit::dpi::LogicalSize::new(1280.0, 720.0))
                    .with_resizable(true)
                    .with_title("wgpu-mc Test — Triangle"),
            )
            .unwrap();

        let surface = instance.create_surface(window).unwrap();

        let adapter = futures::executor::block_on(
            instance.request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                ..Default::default()
            })
        ).unwrap();

        let (device, queue) = futures::executor::block_on(
            adapter.request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("Computed Device"),
                    required_features: wgpu::Features::empty(),
                    required_limits: wgpu::Limits::default(),
                    memory_hints: Default::default(),
                },
                None,
            )
        ).unwrap();

        let caps = surface.get_capabilities(&adapter);
        let format = caps.formats[0];

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

        self.surface = Some(surface);
        self.device = Some(device);
        self.queue = Some(queue);
        self.config = Some(config);
    }

    fn window_event(&mut self, _event_loop: &ActiveEventLoop, _window_id: WindowId, event: WindowEvent) {
        if let (Some(ref device), Some(ref queue), Some(ref mut config), Some(ref mut surface)) =
            (&self.device, &self.queue, &mut self.config, &mut self.surface)
        {
            match event {
                WindowEvent::Resized(size) => {
                    if size.width > 0 && size.height > 0 {
                        config.width = size.width;
                        config.height = size.height;
                        surface.configure(device, config);
                    }
                }
                WindowEvent::CloseRequested => {
                    // Exit handled in about_to_wait
                }
                _ => {}
            }
        }
    }

    fn about_to_wait(&mut self, event_loop: &ActiveEventLoop) {
        if let (Some(ref device), Some(ref queue), Some(ref config), Some(ref mut surface)) =
            (&self.device, &self.queue, &self.config, &mut self.surface)
        {
            let output = match surface.get_current_texture() {
                Ok(frame) => frame,
                Err(_) => return,
            };

            let view = output.texture.create_view(&wgpu::TextureViewDescriptor::default());

            let mut encoder = device.create_command_encoder(
                &wgpu::CommandEncoderDescriptor { label: Some("Render Encoder") }
            );

            {
                let _render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                    label: Some("Clear Pass"),
                    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                        view: &view,
                        resolve_target: None,
                        ops: wgpu::Operations {
                            load: wgpu::LoadOp::Clear(wgpu::Color {
                                r: 0.0,
                                g: 0.0,
                                b: 0.5,
                                a: 1.0,
                            }),
                            store: wgpu::StoreOp::Store,
                        },
                    })],
                    depth_stencil_attachment: None,
                    timestamp_writes: None,
                    occlusion_query_set: None,
                });
            }

            queue.submit(std::iter::once(encoder.finish()));
            output.present();
        }
    }
}

fn main() {
    let event_loop = EventLoop::new().unwrap();
    let mut app_state = AppState {
        surface: None,
        device: None,
        queue: None,
        config: None,
        size: winit::dpi::PhysicalSize::default(),
    };

    event_loop.run_app(&mut app_state).unwrap();
}
