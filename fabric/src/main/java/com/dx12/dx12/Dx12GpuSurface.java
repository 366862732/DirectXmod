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
        // P6 诊断：blit 后从颜色纹理读回 3x3 像素（waitIdle 保证 GPU 已完成）。
        // 直接读 lastColorTextureHandle（shader 输出目标），而非 surface back buffer（被 blit 覆盖前可能含旧数据）。
        if (this.lastColorTextureHandle != 0L && ++debugReadbackCounter % 30 == 0) {
            int[] rb = Dx12Native.dx12ReadbackTexturePixels(this.lastColorTextureHandle);
            if (rb == null) {
                System.err.println("[dx12-java] [DIAG] colorTex rb NULL handle=0x"
                    + Long.toHexString(this.lastColorTextureHandle & 0xFFFFFFFFL));
            } else {
                int len = rb.length;
                System.err.printf("[dx12-java] [DIAG] colorTex rb ARRAY_LEN=%d handle=0x%08X%n",
                    len, (int)(this.lastColorTextureHandle & 0xFFFFFFFFL));
                if (len >= 4)
                    System.err.printf("  TL(%d,%d,%d,%d) TM(%d,%d,%d,%d) TR(%d,%d,%d,%d)%n",
                        rb[0], rb[1], rb[2], rb[3],
                        rb[4], rb[5], rb[6], rb[7],
                        rb[8], rb[9], rb[10], rb[11]);
                if (len >= 12)
                    System.err.printf("  ML(%d,%d,%d,%d) MC(%d,%d,%d,%d) MR(%d,%d,%d,%d)%n",
                        rb[12], rb[13], rb[14], rb[15],
                        rb[16], rb[17], rb[18], rb[19],
                        rb[20], rb[21], rb[22], rb[23]);
                if (len >= 24)
                    System.err.printf("  BL(%d,%d,%d,%d) BM(%d,%d,%d,%d) BR(%d,%d,%d,%d)%n",
                        rb[24], rb[25], rb[26], rb[27],
                        rb[28], rb[29], rb[30], rb[31],
                        rb[32], rb[33], rb[34], rb[35]);
            }
            // P6 诊断：额外读回 back buffer，确认 blit 是否将绿色写入 swapchain。
            // 若 backbuffer 绿色但纹理黑色 → 问题在渲染 pass；若两者都黑 → 问题在 blit/present。
            int[] bb = Dx12Native.dx12ReadbackSurfacePixels(this.handle);
            if (bb != null && bb.length >= 12) {
                System.err.printf("[dx12-java] [DIAG] backbuf rb ARRAY_LEN=%d%n", bb.length);
                System.err.printf("  TL(%d,%d,%d,%d) TM(%d,%d,%d,%d) TR(%d,%d,%d,%d)%n",
                    bb[0], bb[1], bb[2], bb[3],
                    bb[4], bb[5], bb[6], bb[7],
                    bb[8], bb[9], bb[10], bb[11]);
                System.err.printf("  ML(%d,%d,%d,%d) MC(%d,%d,%d,%d) MR(%d,%d,%d,%d)%n",
                    bb[12], bb[13], bb[14], bb[15],
                    bb[16], bb[17], bb[18], bb[19],
                    bb[20], bb[21], bb[22], bb[23]);
                System.err.printf("  BL(%d,%d,%d,%d) BM(%d,%d,%d,%d) BR(%d,%d,%d,%d)%n",
                    bb[24], bb[25], bb[26], bb[27],
                    bb[28], bb[29], bb[30], bb[31],
                    bb[32], bb[33], bb[34], bb[35]);
            }
        }
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
