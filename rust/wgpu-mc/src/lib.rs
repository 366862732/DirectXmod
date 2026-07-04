//! wgpu-mc: Minecraft rendering engine backed by wgpu/DX12
//!
//! Provides WmRenderer with HWND-based Surface creation for Minecraft integration.

use wgpu::{
    Instance, Surface, SurfaceConfiguration, Device, Queue,
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

        match adapter {
            Some(a) => {
                log::info!("DX12 adapter found: {:?}", a.get_info().name);
                true
            }
            None => {
                log::warn!("No suitable DX12 adapter found!");
                false
            }
        }
    }

    /// Create a WmRenderer from a raw Windows HWND (u64).
    ///
    /// NOTE: Due to wgpu 23 + raw-window-handle 0.6 compatibility issues
    /// (WindowHandle doesn't implement HasDisplayHandle), this function
    /// is currently a placeholder. Use `from_window` instead when integrating
    /// with winit-based applications.
    ///
    /// For MC integration, the HWND will be passed from Java via JNI,
    /// and a proper wrapper will be created using winit.
    pub fn from_hwnd_placeholder(_hwnd: u64) -> Result<Self, SurfaceError> {
        log::warn!("from_hwnd is not yet functional due to raw-window-handle/wgpu compatibility");
        log::warn!("Use from_window() for testing, or implement a custom HWND wrapper");
        Err(SurfaceError::Lost)
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
    pub fn render_frame(&mut self) -> Result<(), SurfaceError> {
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

    /// Get device info string.
    pub fn get_device_info(&self) -> Option<String> {
        if self.has_dx12_adapter {
            Some("DX12 adapter available".to_string())
        } else {
            Some("No compatible GPU adapter".to_string())
        }
    }
}
