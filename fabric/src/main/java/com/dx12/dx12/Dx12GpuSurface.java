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
    /** P6 诊断：缓存最近一次 blit 的 color texture handle，用于直接读回验证。 */
    private long lastColorTextureHandle = 0L;

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
        this.lastColorTextureHandle = tex.handle();
        Dx12Native.dx12BlitSurface(encoder.nativeHandle(), this.handle, tex.handle());
    }

    @Override
    public void present() {
        Dx12Native.dx12PresentSurface(handle);
    }

    /** P6 诊断：供 Dx12Backend.selfTestSurface 在 fence 完成后读取 color texture。 */
    public long getColorTextureHandle() {
        return lastColorTextureHandle;
    }

    /** P6 诊断：供 Dx12Backend.selfTestSurface 在 fence 完成后读取 back buffer。 */
    public long getHandle() {
        return handle;
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
