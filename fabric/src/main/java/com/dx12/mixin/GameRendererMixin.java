package com.dx12.mixin;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;
import com.dx12.Dx12Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Plan C (HUD Overlay via GL → D3D12 composition):
 *
 * - renderLevel HEAD (cancellable): when D3D12 has surface + chunks,
 *   cancel GL world rendering so the color buffer retains MC's initial
 *   clear color. HUD/GUI elements render on top of that background.
 * - render TAIL: capture the GL framebuffer.
 *   * If chunks active → capture is HUD-only (world was cancelled) → setHudPixels
 *   * If no chunks → capture is full GL frame → setFramePixels
 * - D3D12 composites: world (chunks) → HUD overlay (alpha-blended)
 *
 * Note: In MC 26.1.2, renderLevel() renders both the world AND potentially
 * the HUD within the same method. Cancelling at HEAD prevents MC's world
 * rendering while preserving the existing framebuffer state. The HUD
 * may render at a different point in render(). We capture whatever ends
 * up on the default framebuffer at render TAIL time.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /** Pre-allocated buffer for GL framebuffer capture. */
    private static ByteBuffer frameCaptureBuffer;

    /** Pre-allocated buffer for GL HUD capture (PBO readback destination). */
    private static ByteBuffer hudCaptureBuffer;

    // ── PBO async HUD readback ─────────────────────────────────────────
    // Triple-buffered Pixel Buffer Objects for non-blocking framebuffer readback.
    // Frame N: glReadPixels → PBO[writeIdx]  (async DMA, returns immediately)
    // Frame N: glMapBuffer(PBO[readIdx])      → get previous frame's data
    // glFlush() replaces glFinish() — submits commands without blocking CPU.
    private static final int PBO_COUNT = 3;
    private static int[] pboIds;         // null ≡ not initialized
    private static int pboWriteIndex = 0;
    private static int pboReadIndex = 0; // the PBO whose DMA finished last frame
    private static int pboWarmup = 0;    // frames elapsed since PBO init
    private static int lastHudWidth = 0;
    private static int lastHudHeight = 0;
    private static boolean pboSupported = true;
    private static boolean pboInitAttempted = false;

    /**
     * HEAD injection: cancel renderLevel when D3D12 has chunks + surface.
     * This prevents the GL world from being rendered, but the GL HUD/GUI
     * still renders normally. The framebuffer retains MC's initial clear
     * color (sky blue) + HUD elements.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(CallbackInfo ci) {
        // Pass through — let GL render everything.
    }

    /**
     * Cancel renderLevel at HEAD when D3D12 has chunks + surface.
     * This prevents GL world rendering. The framebuffer retains MC's
     * initial clear color + any subsequent HUD/GUI rendering.
     */
    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderLevelHead(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface() && D3D12Bridge.hasChunkGeometry()) {
            if (!renderLevelCancelledLogged) {
                renderLevelCancelledLogged = true;
                com.dx12.Dx12Mod.LOGGER.info("[dx12-wm] renderLevel HEAD cancelled");
            }
            ci.cancel();
        }
    }

    /**
     * TAIL injection: after MC finishes rendering the frame.
     * - Surface + chunks: capture HUD-only framebuffer → D3D12 overlay
     * - Surface (no chunks): capture full GL framebuffer → D3D12 texture
     * - Offscreen mode: upload D3D12-rendered pixels as GL quad overlay
     *
     * Phase 11h (Frame Pacing): D3D12 Present now runs HERE, once per rendered
     * frame (vsync-driven). Previously it ran at Minecraft.runTick() TAIL which
     * fires only on game ticks (~20 Hz), capping D3D12 output at 20 FPS.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            if (D3D12Bridge.hasChunkGeometry()) {
                captureHudForD3D12();
            } else {
                captureFramebufferForD3D12();
            }
            presentD3D12Frame();
        } else {
            Dx12Mod.onPostRender();
        }
    }

    /**
     * Present one D3D12 frame from the render-thread tail (per frame, vsync).
     * The GL context is temporarily detached before Present() and reattached
     * afterwards so DXGI and WGL never touch the HWND simultaneously (the same
     * TDR-avoidance pattern the runTick hook used).
     */
    private void presentD3D12Frame() {
        if (!presentLogOnce) {
            presentLogOnce = true;
            Dx12Mod.LOGGER.info("[dx12-wm] D3D12 present moved to render TAIL (per-frame pacing)");
        }
        // Phase 11i: update camera per rendered frame (was ~20 Hz on the tick
        // path). Runs before detaching GL so the JNI call stays off the HWND path.
        Dx12Mod.updateCameraFromRender();
        long glfwWindow = GLFW.glfwGetCurrentContext();
        if (glfwWindow == 0) return;
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);

        GLFW.glfwMakeContextCurrent(0);
        try {
            if (hwnd != 0) {
                D3D12Bridge.setWindow(hwnd);
            }
            if (D3D12Bridge.hasSurface()) {
                D3D12Bridge.renderFrame();
            }
        } finally {
            GLFW.glfwMakeContextCurrent(glfwWindow);
        }
    }

    private void captureFramebufferForD3D12() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        int size = w * h * 4; // RGBA8
        if (frameCaptureBuffer == null || frameCaptureBuffer.capacity() < size) {
            frameCaptureBuffer = BufferUtils.createByteBuffer(size);
        }

        frameCaptureBuffer.position(0);
        if (frameCaptureBuffer.limit() < size) {
            frameCaptureBuffer.limit(size);
        }

        // Save current GL read FBO state
        int oldReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        if (oldDrawFbo != 0 && !firstCaptureLogged) {
            com.dx12.Dx12Mod.LOGGER.info(
                "[dx12-wm] Reading from draw FBO {} (not GL_BACK)", oldDrawFbo);
        }

        if (oldDrawFbo != 0) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldDrawFbo);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GL11.glReadBuffer(GL11.GL_BACK);
        }

        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameCaptureBuffer);

        // Restore previous GL state
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldReadFbo);

        frameCaptureBuffer.position(0);
        frameCaptureBuffer.limit(size);

        // One-time diagnostic
        if (!firstCaptureLogged) {
            firstCaptureLogged = true;
            int r = frameCaptureBuffer.get(0) & 0xFF;
            int g = frameCaptureBuffer.get(1) & 0xFF;
            int b = frameCaptureBuffer.get(2) & 0xFF;
            int a = frameCaptureBuffer.get(3) & 0xFF;
            com.dx12.Dx12Mod.LOGGER.info(
                "[dx12-wm] First GL capture: {}x{}, first pixel RGBA=({},{},{},{})",
                w, h, r, g, b, a);
        }

        D3D12Bridge.setFramePixels(frameCaptureBuffer, w, h);
    }

    // ── PBO helpers ────────────────────────────────────────────────────

    /** (Re)initialize PBOs for the given size. Keeps existing data if resizing. */
    private static void initPBOs(int size) {
        if (pboIds == null) {
            pboIds = new int[PBO_COUNT];
        }
        for (int i = 0; i < PBO_COUNT; i++) {
            int oldId = pboIds[i];
            if (oldId != 0) {
                GL15.glDeleteBuffers(oldId);
            }
            int id = GL15.glGenBuffers();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, id);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, size, GL15.GL_STREAM_READ);
            pboIds[i] = id;
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        pboWarmup = 0;
        pboWriteIndex = 0;
        pboReadIndex = 0;
    }

    /** Capture HUD via PBO async DMA, falling back to synchronous readback. */
    private void captureHudForD3D12() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        int size = w * h * 4; // RGBA8

        // ── (A) Save GL state ──────────────────────────────────────
        int oldReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldPackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);

        // Log FBO info on first capture
        if (!firstHudCaptureLogged) {
            Dx12Mod.LOGGER.info(
                "[dx12-wm] HUD capture FBO state: readFbo={} drawFbo={}",
                oldReadFbo, oldDrawFbo);
        }

        // ── (B) Setup read framebuffer ─────────────────────────────
        if (oldDrawFbo != 0) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldDrawFbo);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GL11.glReadBuffer(GL11.GL_BACK);
        }

        // ── (C) Try PBO async path ─────────────────────────────────
        boolean usedPbo = false;
        if (pboSupported) {
            usedPbo = captureHudViaPbo(size, w, h);
        }

        // ── (D) Fallback: synchronous readback ─────────────────────
        if (!usedPbo) {
            captureHudSync(size, w, h);
        }

        // ── (E) Restore GL state ───────────────────────────────────
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldReadFbo);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, oldPackBuffer);
    }

    /**
     * PBO-based async HUD readback.
     * Returns true if successful, false to trigger synchronous fallback.
     */
    private boolean captureHudViaPbo(int size, int w, int h) {
        // Init PBOs on first use or on resize
        boolean needsInit = (pboIds == null)
            || (w != lastHudWidth || h != lastHudHeight);
        if (needsInit) {
            try {
                initPBOs(size);
                lastHudWidth = w;
                lastHudHeight = h;
            } catch (Exception e) {
                Dx12Mod.LOGGER.warn("[dx12-wm] PBO init failed, falling back: {}",
                    e.getMessage());
                pboSupported = false;
                return false;
            }
        }

        // Ensure the HUD capture buffer is ready
        if (hudCaptureBuffer == null || hudCaptureBuffer.capacity() < size) {
            hudCaptureBuffer = BufferUtils.createByteBuffer(size);
        }

        // ═══ Step 1: Start async DMA read into current write PBO ═══
        int writeId = pboIds[pboWriteIndex];
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, writeId);

        // glFlush submits pending HUD rendering commands to GPU.
        // Without this, MC's render graph may not flush before glReadPixels.
        GL11.glFlush();

        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE, 0L); // offset 0 = write into PBO

        // Unbind PBO so subsequent GL operations are not affected.
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);

        // ═══ Step 2: Map previous frame's PBO (read completed DMA) ═══
        // Triple buffering: PBO[readIndex] was written 2 frames ago.
        // glMapBuffer blocks briefly until DMA is complete (~microseconds).
        int readId = pboIds[pboReadIndex];
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, readId);

        ByteBuffer mapped = null;
        try {
            mapped = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL30.GL_READ_ONLY);
        } catch (Exception e) {
            // glMapBuffer can fail if driver doesn't support GL_READ_ONLY
            // in core profile; fall back to synchronous path.
        }

        if (mapped == null) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            // Still advance PBO indices so warmup counter can proceed
            advancePboIndices();
            pboWarmup++;
            return false;
        }

        // ── Copy mapped PBO data into hudCaptureBuffer ──────────────
        hudCaptureBuffer.clear();
        int bytesToCopy = Math.min(mapped.remaining(), hudCaptureBuffer.remaining());
        hudCaptureBuffer.limit(bytesToCopy);
        mapped.limit(mapped.position() + bytesToCopy);
        hudCaptureBuffer.put(mapped);
        hudCaptureBuffer.flip();

        GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);

        // ── Diagnostic on first successful PBO readback ─────────────
        if (!firstHudCaptureLogged) {
            firstHudCaptureLogged = true;
            int r = hudCaptureBuffer.get(0) & 0xFF;
            int g = hudCaptureBuffer.get(1) & 0xFF;
            int b = hudCaptureBuffer.get(2) & 0xFF;
            int a = hudCaptureBuffer.get(3) & 0xFF;
            Dx12Mod.LOGGER.info(
                "[dx12-wm] First HUD capture (PBO): {}x{}, first pixel RGBA=({},{},{},{})",
                w, h, r, g, b, a);
            logHudEdgeSamples(hudCaptureBuffer, w, h, size);
        }

        hudCaptureBuffer.position(0);
        hudCaptureBuffer.limit(bytesToCopy);
        D3D12Bridge.setHudPixels(hudCaptureBuffer, w, h);

        // ═══ Step 3: Advance PBO ring indices ═══
        advancePboIndices();
        pboWarmup++;
        return true;
    }

    /** Advance the PBO write/read ring indices. */
    private static void advancePboIndices() {
        pboReadIndex = pboWriteIndex;
        pboWriteIndex = (pboWriteIndex + 1) % PBO_COUNT;
    }

    /** Synchronous fallback: traditional glFinish + glReadPixels. */
    private void captureHudSync(int size, int w, int h) {
        if (hudCaptureBuffer == null || hudCaptureBuffer.capacity() < size) {
            hudCaptureBuffer = BufferUtils.createByteBuffer(size);
        }

        hudCaptureBuffer.position(0);
        hudCaptureBuffer.limit(size);

        // Force all pending GL commands to complete before reading pixels.
        // MC 26.1.2 may batch HUD render commands through the render graph
        // system (SubmitNodes), which might not flush to the default FBO
        // until glFinish or an explicit flush.
        GL11.glFinish();

        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE, hudCaptureBuffer);

        hudCaptureBuffer.position(0);
        hudCaptureBuffer.limit(size);

        if (!firstHudCaptureLogged) {
            firstHudCaptureLogged = true;
            int r = hudCaptureBuffer.get(0) & 0xFF;
            int g = hudCaptureBuffer.get(1) & 0xFF;
            int b = hudCaptureBuffer.get(2) & 0xFF;
            int a = hudCaptureBuffer.get(3) & 0xFF;
            Dx12Mod.LOGGER.info(
                "[dx12-wm] First HUD capture (sync): {}x{}, first pixel RGBA=({},{},{},{})",
                w, h, r, g, b, a);
            logHudEdgeSamples(hudCaptureBuffer, w, h, size);
        }

        D3D12Bridge.setHudPixels(hudCaptureBuffer, w, h);
    }

    /** Log edge + center pixel samples for diagnostics. */
    private static void logHudEdgeSamples(ByteBuffer buf, int w, int h, int size) {
        StringBuilder sb = new StringBuilder("HUD edge samples:");
        for (int i = 0; i < 5; i++) {
            int px = buf.get(i * 4) & 0xFF;
            int py = buf.get(i * 4 + 1) & 0xFF;
            int pz = buf.get(i * 4 + 2) & 0xFF;
            int pa = buf.get(i * 4 + 3) & 0xFF;
            sb.append(String.format(" [%d]=(%d,%d,%d,%d)", i, px, py, pz, pa));
        }
        int centerOff = (h / 2 * w + w / 2) * 4;
        if (centerOff + 4 <= size) {
            sb.append(String.format(" center=(%d,%d,%d,%d)",
                buf.get(centerOff) & 0xFF,
                buf.get(centerOff + 1) & 0xFF,
                buf.get(centerOff + 2) & 0xFF,
                buf.get(centerOff + 3) & 0xFF));
        }
        Dx12Mod.LOGGER.info("[dx12-wm] HUD pixel samples: {}", sb.toString());
    }

    private static boolean firstCaptureLogged = false;
    private static boolean firstHudCaptureLogged = false;
    private static boolean renderLevelCancelledLogged = false;
    private static boolean presentLogOnce = false;
}
