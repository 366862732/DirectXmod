package com.xgdt.dx12.dx12;

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
    /**
     * P48（多帧飞行）：{@link #acquireNextTexture()} 不再立即 acquire back buffer，而是把
     * 真实 acquire 推迟到 blit 之前（{@link #ensureAcquired()}）。
     * MC 每帧顺序为 acquire → 整帧录制（~4-5ms）→ blit → submit → present，原实现在帧首
     * （acquire）就阻塞等待上一帧 Present 完成（实测 0.13-1.7ms）；推迟后该等待与本帧录制
     * 重叠，通常为 0ms。GpuSurface 包装层的 hasImageAcquired 不受影响（isAcquired 仍为 true）。
     */
    private boolean acquireDeferred = false;
    /** 推迟的 acquire 已失败：下一帧 acquireNextTexture 抛 SurfaceException 触发 MC 重建 surface。 */
    private boolean acquireDeferredFailed = false;

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
        // P48：reconfigure 会 ResizeBuffers，作废任何未完成的推迟 acquire。
        this.acquireDeferred = false;
        this.acquireDeferredFailed = false;
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
        // P48：上一帧推迟的 acquire 失败过 → 现在补报，让 MC 走 surfaceIsInvalid →
        // reconfigure 路径（在 blit 处抛异常会直接崩游戏）。
        if (this.acquireDeferredFailed) {
            this.acquireDeferredFailed = false;
            throw new SurfaceException("Failed to acquire DX12 back buffer (deferred)");
        }
        // P48（多帧飞行）：不在此处立即 acquire，推迟到 blit 之前——见 ensureAcquired()。
        this.acquireDeferred = true;
    }

    /**
     * P48（多帧飞行）：在真正需要 back buffer 之前（blit 时）才执行 acquire。
     * 此时本帧录制已完成，上一帧 Present 通常早已结束 → acquire 内部的等待为 0ms。
     * 返回 false 表示 acquire 失败，本帧跳过 blit（画面停留上一帧，不崩游戏）。
     */
    private boolean ensureAcquired() {
        if (!this.acquireDeferred) {
            return true;
        }
        this.acquireDeferred = false;
        if (Dx12Native.dx12AcquireSurface(this.handle)) {
            return true;
        }
        System.err.println("[dx12-java] [P48] deferred acquire failed — skipping blit this frame");
        System.err.flush();
        this.acquireDeferredFailed = true;
        return false;
    }

    @Override
    public void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView textureView) {
        // P48：真实 acquire 推迟到这里（本帧录制已完成 → 上一帧 Present 早已结束）。
        if (!ensureAcquired()) {
            return;
        }
        Dx12CommandEncoderBackend encoder = (Dx12CommandEncoderBackend) commandEncoder;
        // 传 texture.handle()（底层纹理对象），而非 view.handle()（SRV view 对象）。
        // view 是 texture 的视图包装，CopyTextureRegion 需要的是纹理资源本身。
        Dx12GpuTexture tex = (Dx12GpuTexture) ((Dx12GpuTextureView) textureView).texture();
        this.lastColorTextureHandle = tex.handle();
        Dx12Native.dx12BlitSurface(encoder.nativeHandle(), this.handle, tex.handle());
        // P6 诊断：blit 后从颜色纹理读回 3x3 像素（waitIdle 保证 GPU 已完成）。
        // 直接读 lastColorTextureHandle（shader 输出目标），而非 surface back buffer（被 blit 覆盖前可能含旧数据）。
        // P29：读回内部 deviceWaitIdle（每 30 帧同步气泡），仅 DX12_LOG_VERBOSE=1 时启用。
        if (Dx12Native.LOG_VERBOSE && this.lastColorTextureHandle != 0L && ++debugReadbackCounter % 30 == 0) {
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
            // 用 dx12GetActiveSurfaceHandle() 读游戏实际 surface（而非 this.handle，
            // 后者在 resize 后可能指向已失效的旧 self-test surface）。
            long gameSurface = Dx12Native.dx12GetActiveSurfaceHandle();
            int[] bb = Dx12Native.dx12ReadbackSurfacePixels(gameSurface != 0 ? gameSurface : this.handle);
            if (bb != null && bb.length >= 12) {
                System.err.printf("[dx12-java] [DIAG] backbuf rb ARRAY_LEN=%d surface=0x%08X%n",
                    bb.length, (int)((gameSurface != 0 ? gameSurface : this.handle) & 0xFFFFFFFFL));
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

    /**
     * 纯红色清空当前 back buffer（不依赖源纹理，用于渲染循环自检）。
     * 需要调用方先 acquireNextTexture()，本方法记录命令后返回。
     */
    public void clearToRed(Dx12CommandEncoderBackend encoder) {
        // P48：real acquire 推迟到此处（与 blitFromTexture 一致）。
        if (!ensureAcquired()) {
            return;
        }
        Dx12Native.dx12BlitSurface(encoder.nativeHandle(), this.handle, 0L);
    }

    @Override
    public void present() {
        Dx12Native.dx12PresentSurface(handle);
    }

    /** P6 诊断：供 Dx12Backend.selfTestSurface 在 fence 完成后读取 color texture。
     * 优先使用 dx12GetBackBufferHandle 获取当前 acquire 的 back buffer，
     * 避免使用缓存的 lastColorTextureHandle（可能是旧渲染 pass 的残留）。 */
    public long getColorTextureHandle() {
        // P48：诊断路径也需保证推后的 acquire 已执行，否则读回的是上一帧的 back buffer。
        ensureAcquired();
        long bb = Dx12Native.dx12GetBackBufferHandle(handle);
        return bb != 0 ? bb : lastColorTextureHandle;
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
