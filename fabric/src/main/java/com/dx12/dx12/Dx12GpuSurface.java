package com.dx12.dx12;

import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.SurfaceException;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link GpuSurfaceBackend} (DXGI flip-model swapchain).
 *
 * Mirrors {@code com.mojang.blaze3d.vulkan.VulkanGpuSurface}: the swapchain is
 * created lazily from the window HWND, resized in {@link #configure}, blitted
 * from the intermediate render target via the current command encoder, and
 * presented with vsync according to the configured {@link PresentMode}.
 */
@Environment(EnvType.CLIENT)
public class Dx12GpuSurface implements GpuSurfaceBackend {
    private final long handle;
    private final List<GpuSurface.PresentMode> presentModes = new ArrayList<>();
    private boolean closed;
    /** P6 诊断：每 ~60 帧读回一次 back buffer 采样像素（确认画面实际内容）。 */
    private int debugReadbackCounter;

    public Dx12GpuSurface(long hwnd) {
        this.handle = Dx12Native.dx12CreateSurface(hwnd);
        if (this.handle == 0) {
            throw new IllegalStateException("dx12CreateSurface returned a null handle");
        }
        int[] modes = Dx12Native.dx12SurfacePresentModes();
        GpuSurface.PresentMode[] all = GpuSurface.PresentMode.values();
        for (int mode : modes) {
            if (mode >= 0 && mode < all.length) {
                this.presentModes.add(all[mode]);
            }
        }
    }

    @Override
    public void configure(GpuSurface.Configuration config) throws SurfaceException {
        boolean ok = Dx12Native.dx12ConfigureSurface(this.handle, config.width(), config.height(),
            config.presentMode().ordinal());
        System.err.println("[dx12-java] configureSurface: " + config.width() + "x" + config.height()
                + " mode=" + config.presentMode() + " ok=" + ok);
        if (!ok) {
            throw new SurfaceException("Failed to configure DX12 surface to "
                + config.width() + "x" + config.height());
        }
    }

    @Override
    public boolean isSuboptimal() {
        return Dx12Native.dx12IsSurfaceSuboptimal(this.handle);
    }

    @Override
    public void acquireNextTexture() throws SurfaceException {
        if (!Dx12Native.dx12AcquireSurface(this.handle)) {
            throw new SurfaceException("Failed to acquire DX12 back buffer");
        }
    }

    @Override
    public void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView textureView) {
        Dx12CommandEncoderBackend encoder = (Dx12CommandEncoderBackend) commandEncoder;
        // 传 texture.handle()（底层纹理对象），而非 view.handle()（SRV view 对象）。
        // view 是 texture 的视图包装，CopyTextureRegion 需要的是纹理资源本身。
        Dx12GpuTexture tex = (Dx12GpuTexture) ((Dx12GpuTextureView) textureView).texture();
        Dx12Native.dx12BlitSurface(encoder.nativeHandle(), this.handle, tex.handle());
    }

    @Override
    public void present() {
        Dx12Native.dx12PresentSurface(handle);
        // P6 诊断：约每秒读回一次 back buffer，打印 3x3 采样像素颜色到 Java 日志。
        if (++debugReadbackCounter % 60 == 1) {
            int[] pixels = Dx12Native.dx12ReadbackSurfacePixels(handle);
            if (pixels != null && pixels.length >= 12) {
                System.err.printf("[dx12-java] readback %dx%d center=RGBA(%d,%d,%d,%d) corners=%d black=%d%n",
                    0, 0,
                    pixels[4], pixels[5], pixels[6], pixels[7],
                    (pixels[0]!=0||pixels[1]!=0||pixels[2]!=0) ? 1 : 0,
                    (pixels[0]==0&&pixels[1]==0&&pixels[2]==0&&pixels[3]==0) ? 1 : 0);
            }
        }
    }

    @Override
    public Collection<GpuSurface.PresentMode> supportedPresentModes() {
        return this.presentModes;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Dx12Native.dx12DestroySurface(this.handle);
    }
}
