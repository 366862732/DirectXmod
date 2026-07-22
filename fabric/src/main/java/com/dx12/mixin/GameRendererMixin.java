package com.dx12.mixin;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
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

    /** Pre-allocated buffer for GL HUD capture. */
    private static ByteBuffer hudCaptureBuffer;

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
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            if (D3D12Bridge.hasChunkGeometry()) {
                captureHudForD3D12();
            } else {
                captureFramebufferForD3D12();
            }
        } else {
            Dx12Mod.onPostRender();
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

    /** Capture the HUD-only framebuffer for D3D12 overlay compositing.
     *  The world was cleared to transparent in renderLevel TAIL,
     *  so only HUD elements remain in the framebuffer. */
    private void captureHudForD3D12() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        int size = w * h * 4; // RGBA8
        if (hudCaptureBuffer == null || hudCaptureBuffer.capacity() < size) {
            hudCaptureBuffer = BufferUtils.createByteBuffer(size);
        }

        hudCaptureBuffer.position(0);
        if (hudCaptureBuffer.limit() < size) {
            hudCaptureBuffer.limit(size);
        }

        // Query current GL FBO state for diagnostic
        int oldReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        // Log FBO info on first capture
        if (!firstHudCaptureLogged) {
            com.dx12.Dx12Mod.LOGGER.info(
                "[dx12-wm] HUD capture FBO state: readFbo={} drawFbo={}",
                oldReadFbo, oldDrawFbo);
        }

        if (oldDrawFbo != 0) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldDrawFbo);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GL11.glReadBuffer(GL11.GL_BACK);
        }

        // Force all pending GL commands to complete before reading pixels.
        // MC 26.1.2 may batch HUD render commands through the render graph
        // system (SubmitNodes), which might not flush to the default FBO
        // until glFinish or an explicit flush. glReadPixels without finish
        // may read stale/empty pixel data.
        GL11.glFinish();

        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, hudCaptureBuffer);

        // Restore previous GL state
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldReadFbo);

        hudCaptureBuffer.position(0);
        hudCaptureBuffer.limit(size);

        // One-time diagnostic
        if (!firstHudCaptureLogged) {
            firstHudCaptureLogged = true;
            int r = hudCaptureBuffer.get(0) & 0xFF;
            int g = hudCaptureBuffer.get(1) & 0xFF;
            int b = hudCaptureBuffer.get(2) & 0xFF;
            int a = hudCaptureBuffer.get(3) & 0xFF;
            com.dx12.Dx12Mod.LOGGER.info(
                "[dx12-wm] First HUD capture: {}x{}, first pixel RGBA=({},{},{},{})",
                w, h, r, g, b, a);
            // Log edge pixel samples to verify HUD content vs transparent areas
            StringBuilder sb = new StringBuilder("HUD edge samples:");
            for (int i = 0; i < 5; i++) {
                int px = hudCaptureBuffer.get(i * 4) & 0xFF;
                int py = hudCaptureBuffer.get(i * 4 + 1) & 0xFF;
                int pz = hudCaptureBuffer.get(i * 4 + 2) & 0xFF;
                int pa = hudCaptureBuffer.get(i * 4 + 3) & 0xFF;
                sb.append(String.format(" [%d]=(%d,%d,%d,%d)", i, px, py, pz, pa));
            }
            // Also sample center pixels
            int centerOff = (h / 2 * w + w / 2) * 4;
            if (centerOff + 4 <= size) {
                sb.append(String.format(" center=(%d,%d,%d,%d)",
                    hudCaptureBuffer.get(centerOff) & 0xFF,
                    hudCaptureBuffer.get(centerOff + 1) & 0xFF,
                    hudCaptureBuffer.get(centerOff + 2) & 0xFF,
                    hudCaptureBuffer.get(centerOff + 3) & 0xFF));
            }
            com.dx12.Dx12Mod.LOGGER.info("[dx12-wm] HUD pixel samples: {}", sb.toString());
        }

        D3D12Bridge.setHudPixels(hudCaptureBuffer, w, h);
    }

    private static boolean firstCaptureLogged = false;
    private static boolean firstHudCaptureLogged = false;
    private static boolean renderLevelCancelledLogged = false;
}
