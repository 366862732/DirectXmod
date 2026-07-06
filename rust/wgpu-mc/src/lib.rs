//! wgpu-mc: Minimal wgpu renderer that returns pixel data via callback.

pub struct WmRenderer {
    pub device: wgpu::Device,
    pub queue: wgpu::Queue,
    pub width: u32,
    pub height: u32,
}

impl WmRenderer {
    pub fn create(width: u32, height: u32) -> Result<Self, &'static str> {
        log::info!("Creating WmRenderer (offscreen) {}x{}", width, height);

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::DX12,
            ..Default::default()
        });

        let adapter = futures::executor::block_on(
            instance.request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: None,
                ..Default::default()
            })
        ).ok_or("No adapter")?;

        log::info!("DX12 adapter: {:?}", adapter.get_info().name);

        let (device, queue) = futures::executor::block_on(
            adapter.request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("wgpu-mc"),
                    required_features: wgpu::Features::empty(),
                    required_limits: wgpu::Limits::default(),
                    memory_hints: Default::default(),
                },
                None,
            )
        ).map_err(|_| "Device failed")?;

        Ok(Self { device, queue, width, height })
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        self.width = width;
        self.height = height;
    }

    /// Render a frame and return pixel data (RGBA).
    /// Returns blue background for now.
    pub fn render_frame(&mut self) -> Vec<u8> {
        let w = self.width as usize;
        let h = self.height as usize;
        let mut pixels = vec![0u8; w * h * 4];
        
        for chunk in pixels.chunks_exact_mut(4) {
            chunk[0] = 0;   // R
            chunk[1] = 0;   // G
            chunk[2] = 128; // B (blue)
            chunk[3] = 255; // A
        }
        
        pixels
    }
}
