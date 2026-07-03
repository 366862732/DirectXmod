//! Standalone test program: creates a window and renders a colored triangle

use wgpu_mc::WmRenderer;
use winit::application::ApplicationHandler;
use winit::event::WindowEvent;
use winit::event_loop::{ControlFlow, EventLoop};
use winit::window::{Window, WindowId};

struct App {
    renderer: Option<WmRenderer>,
    window: Option<Window>,
}

impl ApplicationHandler for App {
    fn resumed(&mut self, event_loop: &winit::event_loop::ActiveEventLoop) {
        let window = event_loop
            .create_window(Window::default_attributes().with_inner_size(winit::dpi::PhysicalSize {
                width: 800,
                height: 600,
            }))
            .unwrap();
        self.window = Some(window);
    }

    fn window_event(
        &mut self,
        _event_loop: &winit::event_loop::ActiveEventLoop,
        _window_id: WindowId,
        event: WindowEvent,
    ) {
        match event {
            WindowEvent::Resized(physical_size) => {
                if let Some(ref mut renderer) = self.renderer {
                    renderer.resize(physical_size.width, physical_size.height);
                }
            }
            WindowEvent::CloseRequested => {
                _event_loop.exit();
            }
            _ => {}
        }
    }

    fn about_to_wait(&mut self, _event_loop: &winit::event_loop::ActiveEventLoop) {
        if let Some(ref mut renderer) = self.renderer {
            match renderer.render_frame() {
                Ok(_) => {}
                Err(wgpu::SurfaceError::Lost) => {
                    if let Some(window) = &self.window {
                        let size = window.inner_size();
                        renderer.resize(size.width, size.height);
                    }
                }
                Err(wgpu::SurfaceError::OutOfMemory) => {
                    _event_loop.exit();
                }
                Err(reason) => {
                    log::warn!("render_frame error: {:?}", reason);
                }
            }
        }
    }
}

fn main() {
    env_logger::init();
    log::info!("Starting wgpu-mc standalone test...");

    let event_loop = EventLoop::new().unwrap();
    let mut app = App {
        renderer: None,
        window: None,
    };

    // Create renderer after window is created
    let window = app.window.as_ref().unwrap();
    app.renderer = Some(WmRenderer::new(window.inner_size().width, window.inner_size().height));

    event_loop.run_app(&mut app).unwrap();
}
