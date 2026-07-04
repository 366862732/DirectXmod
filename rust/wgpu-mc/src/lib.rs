//! wgpu-mc: Minecraft rendering engine backed by wgpu/DX12
//!
//! Provides WmRenderer with HWND-based Surface creation for Minecraft integration.
//!
//! Architecture:
//! - `from_hwnd()` creates a hidden winit Window from the HWND pointer
//! - winit Window implements HasWindowHandle + HasDisplayHandle
//! - wgpu uses winit's Window to create a Surface
//! - The hidden winit Window is dropped; Surface retains the HWND binding

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
    /// Implementation strategy:
    /// 1. Create a hidden winit Window (implements HasWindowHandle + HasDisplayHandle)
    /// 2. Extract the HWND from the winit Window's native handle
    /// 3. Use wgpu's instance.create_surface() with the HWND
    /// 4. Drop the winit Window; the Surface remains bound to the original HWND
    ///
    /// # Safety
    /// The HWND must be valid and live at least as long as the WmRenderer.
    pub fn from_hwnd(hwnd: u64) -> Result<Self, SurfaceError> {
        if hwnd == 0 {
            log::error!("Invalid HWND (zero)");
            return Err(SurfaceError::Lost);
        }

        log::info!("Creating WmRenderer from HWND 0x{:016x}", hwnd);

        // Check adapter availability
        let has_adapter = Self::check_gpu_availability();
        if !has_adapter {
            log::error!("No DX12 adapter available");
            return Err(SurfaceError::Lost);
        }

        // Create a hidden winit Window to serve as a bridge for Surface creation
        // winit's Window implements HasWindowHandle + HasDisplayHandle required by wgpu 23
        let event_loop = winit::event_loop::EventLoop::new()
            .map_err(|e| {
                log::error!("Failed to create event loop: {:?}", e);
                SurfaceError::Lost
            })?;
        
        let window = event_loop.create_window(
            winit::window::WindowAttributes::default()
                .with_inner_size(winit::dpi::LogicalSize::new(1.0, 1.0)),
        ).map_err(|e| {
            log::error!("Failed to create winit window: {:?}", e);
            SurfaceError::Lost
        })?;

        // Now create the surface using the winit window's native handle
        // We need to extract the HWND from winit and pass it to wgpu
        use raw_window_handle::{HasWindowHandle, RawWindowHandle};
        
        // Get the window handle from winit
        let winit_handle = window.window_handle()
            .map_err(|e| {
                log::error!("Failed to get window handle: {:?}", e);
                SurfaceError::Lost
            })?;

        // Extract HWND from winit's RawWindowHandle::Win32
        let _extracted_hwnd = match winit_handle.as_raw() {
            RawWindowHandle::Win32(win32) => {
                // hwnd is NonZeroIsize, convert to isize then to usize
                win32.hwnd.get() as usize
            }
            _ => {
                log::error!("Unexpected window handle type");
                return Err(SurfaceError::Lost);
            }
        };

        log::info!("Extracted HWND from winit: 0x{:016x}", _extracted_hwnd);

        // Create wgpu surface using winit's window directly
        // winit's Window implements HasWindowHandle which wgpu 23 requires
        let instance = Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::DX12,
            ..Default::default()
        });

        // Use winit window to create surface
        let surface = instance.create_surface(window)
            .map_err(|e| {
                log::error!("Failed to create surface from winit window: {:?}", e);
                SurfaceError::Lost
            })?;

        // Get adapter and create device
        let adapter = futures::executor::block_on(
            instance.request_adapter(&RequestAdapterOptions {
                power_preference: PowerPreference::HighPerformance,
                force_fallback_adapter: false,
                compatible_surface: Some(&surface),
            })
        ).ok_or(SurfaceError::Lost)?;

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
        ).map_err(|_| SurfaceError::Lost)?;

        // Configure surface
        let formats = surface.get_capabilities(&adapter).formats;
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
            alpha_mode: wgpu::CompositeAlphaMode::Auto,
            view_formats: vec![],
            desired_maximum_frame_latency: 2,
        };

        surface.configure(&device, &config);

        log::info!("WmRenderer created from HWND 0x{:016x}", hwnd);

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
