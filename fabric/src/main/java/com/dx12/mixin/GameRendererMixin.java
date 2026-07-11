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

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /** Pre-allocated buffer for GL framebuffer capture (surface mode). */
    private static ByteBuffer frameCaptureBuffer;

    /**
     * Surface mode: let MC render the world normally via OpenGL.
     * The GL context detachment in MinecraftMixin prevents DXGI-WGL
     * HWND contention during D3D12 Present().
     * The rendered framebuffer is captured at TAIL and uploaded to a
     * D3D12 texture for display through the swapchain.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(CallbackInfo ci) {
        // Offscreen mode: existing behavior (upload overlay at TAIL).
        // Surface mode: pass through (let GL render, capture at TAIL).
        // No cancellation needed — GL + D3D12 coexist via context detach.
    }

    /**
     * TAIL injection: after MC finishes rendering the frame.
     * - Surface mode: capture GL framebuffer → D3D12 texture.
     * - Offscreen mode: upload D3D12-rendered pixels as GL quad overlay.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            captureFramebufferForD3D12();
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
        frameCaptureBuffer.clear();

        // MC renders to custom FBOs; the back buffer may not be the read target.
        // Bind default framebuffer (0) for reading, then read from GL_BACK.
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameCaptureBuffer);
        frameCaptureBuffer.flip();

        // One-time diagnostic
        if (!firstCaptureLogged) {
            firstCaptureLogged = true;
            int r = frameCaptureBuffer.get(0) & 0xFF;
            int g = frameCaptureBuffer.get(1) & 0xFF;
            int b = frameCaptureBuffer.get(2) & 0xFF;
            int a = frameCaptureBuffer.get(3) & 0xFF;
            com.dx12.Dx12Mod.LOGGER.info(
                "First GL capture: {}x{}, first pixel RGBA=({},{},{},{})",
                w, h, r, g, b, a);
        }

        D3D12Bridge.setFramePixels(frameCaptureBuffer, w, h);
    }

    private static boolean firstCaptureLogged = false;
}
