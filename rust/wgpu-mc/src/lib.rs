//! wgpu-mc: Minecraft rendering engine backed by wgpu/DX12
//!
//! Provides WmRenderer with HWND-based Surface creation for Minecraft integration.

use wgpu::{
    Instance, Surface, SurfaceConfiguration, Device, Queue, PresentMode,
    PowerPreference, RequestAdapterOptions,
    SurfaceError,
};

pub struct WmRenderer {
    /// The wgpu surface tied to the MC window
    pub surface: Surface<'static>,
    /// The wgpu device for command submission
    pub device: Device,
    /// The queue for submitting commands
    pub queue: Queue,
    /// Current surface configuration (width/height/format)
    pub config: SurfaceConfiguration,
    /// Current width in pixels
    pub width: u32,
    /// Current height in pixels
    pub height: u32,
    /// Whether a compatible DX12 adapter was found
    pub has_dx12_adapter: bool,
}

impl WmRenderer {
    /// Check if a suitable DX12 adapter is available (no surface required).
    pub fn check_gpu_availability() -> bool {
        let instance = Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::PRIMARY, // DX12 + Vulkan
            ..Default::default()
        });

        let adapter = futures::executor::block_on(
            instance.request_adapter(&RequestAdapterOptions {
                power_preference: PowerPreference::HighPerformance,
                force_fallback_adapter: false,
                compatible_surface: None,
            })
        );

        if let Some(adapter) = adapter {
            let info = adapter.get_info();
            log::info!("GPU Adapter: {} ({:?})", info.name, info.backend);
            info.backend == wgpu::Backend::Dx12
        } else {
            log::warn!("No suitable GPU adapter found");
            false
        }
    }

    /// Create a WmRenderer from an HWND pointer.
    /// Uses winit window as a bridge to create wgpu Surface.
    pub fn from_hwnd(_hwnd: u64) -> Result<Self, SurfaceError> {
        log::info!("Creating WmRenderer");

        // Create wgpu instance
        let instance = Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::DX12,
            ..Default::default()
        });

        // Create a hidden winit window to bridge to wgpu Surface
        let event_loop = winit::event_loop::EventLoop::new()
            .map_err(|e| {
                log::error!("Failed to create event loop: {:?}", e);
                SurfaceError::InternalError
            })?;

        let window = event_loop.create_window(
            winit::window::WindowAttributes::default()
                .with_visible(false)  // Hide the window completely
                .with_decorations(false)
                .with_resizable(false)
                .with_inner_size(winit::dpi::PhysicalSize::new(1, 1)),
        ).map_err(|e| {
            log::error!("Failed to create winit window: {:?}", e);
            SurfaceError::InternalError
        })?;

        let surface = instance.create_surface(window)
            .map_err(|e| {
                log::error!("Failed to create surface: {:?}", e);
                SurfaceError::Unsupported
            })?;

        // Request adapter
        let adapter = futures::executor::block_on(
            instance.request_adapter(&RequestAdapterOptions {
                power_preference: PowerPreference::HighPerformance,
                force_fallback_adapter: false,
                compatible_surface: Some(&surface),
            })
        ).ok_or_else(|| {
            log::error!("No suitable adapter found for surface");
            SurfaceError::Lost
        })?;

        log::info!("DX12 adapter: {:?}", adapter.get_info().name);

        let (device, queue) = futures::executor::block_on(
            adapter.request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("wgpu-mc device"),
                    required_features: wgpu::Features::empty(),
                    required_limits: wgpu::Limits::default(),
                    memory_hints: Default::default(),
                },
                None,
            )
        ).map_err(|e| {
            log::error!("Device request failed: {:?}", e);
            SurfaceError::Lost
        })?;

        // Configure surface
        let caps = surface.get_capabilities(&adapter);
        let formats = caps.formats;
        let preferred_format = formats.iter()
            .find(|f| f.is_srgb())
            .copied()
            .unwrap_or(formats[0]);

        let config = SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: preferred_format,
            width: 1280,
            height: 720,
            present_mode: PresentMode::Fifo,
            alpha_mode: wgpu::CompositeAlphaMode::Opaque,
            view_formats: vec![],
            desired_maximum_frame_latency: 2,
        };

        surface.configure(&device, &config);

        log::info!("WmRenderer created successfully");

        Ok(Self {
            surface,
            device,
            queue,
            config,
            width: 1280,
            height: 720,
            has_dx12_adapter: true,
        })
    }

    /// Resize the renderer to new dimensions.
    pub fn resize(&mut self, width: u32, height: u32) {
        self.width = width;
        self.height = height;
        self.config.width = width;
        self.config.height = height;
        self.surface.configure(&self.device, &self.config);
        log::info!("Renderer resized to {}x{}", width, height);
    }

    /// Render a single frame: clear to blue color.
    pub fn render_frame(&self) -> Result<(), SurfaceError> {
        let output = self.surface.get_current_texture()?;
        let view = output.texture.create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self.device.create_command_encoder(
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
                            r: 0.0,   // Blue background
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

        self.queue.submit(std::iter::once(encoder.finish()));
        output.present();

        Ok(())
    }
}
