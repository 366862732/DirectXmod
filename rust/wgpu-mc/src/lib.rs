//! wgpu-mc: Minecraft rendering engine backed by wgpu/DX12
//!
//! NOTE: This is a skeleton. Full rendering integration with MC's HWND
//! will be implemented in Phase 3 (Surface binding).
//! For now, this crate provides only the JNI bridge and device detection.

use wgpu::Instance;

pub struct WmRenderer {
    pub width: u32,
    pub height: u32,
    /// Whether a compatible DX12 adapter was found
    pub has_dx12_adapter: bool,
}

impl WmRenderer {
    /// Check if a suitable DX12 adapter is available.
    /// Does NOT create a surface or device yet — that requires a real HWND from MC.
    pub fn check_gpu_availability() -> bool {
        let instance = Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::PRIMARY, // DX12 + Vulkan
            ..Default::default()
        });

        let adapter = futures::executor::block_on(
            instance.request_adapter(
                &wgpu::RequestAdapterOptions {
                    power_preference: wgpu::PowerPreference::HighPerformance,
                    force_fallback_adapter: false,
                    compatible_surface: None,
                }
            )
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

    pub fn new(width: u32, height: u32) -> Self {
        let has_dx12 = Self::check_gpu_availability();
        Self {
            width,
            height,
            has_dx12_adapter: has_dx12,
        }
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        self.width = width;
        self.height = height;
    }

    /// Placeholder: actual rendering will be implemented when we have a real HWND
    pub fn render_frame(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        // Not yet implemented — surface creation requires MC's HWND
        log::debug!("render_frame called (placeholder, no surface yet)");
        Ok(())
    }

    pub fn get_device(&self) -> Option<String> {
        if self.has_dx12_adapter {
            Some("DX12 adapter available".to_string())
        } else {
            Some("No compatible GPU adapter".to_string())
        }
    }
}
