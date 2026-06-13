package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts GL state-setting commands and forwards them to D3D12 backend.
 * NEVER cancels original OpenGL calls — all hooks are passive mirrors.
 */
@Mixin(targets = "org.lwjgl.opengl.GL11", remap = false)
public class GlDrawMixin {

    // === Framebuffer clear ===
    @Inject(method = "glClear", at = @At("HEAD"))
    private static void onGlClear(int mask, CallbackInfo ci) {
        D3D12Bridge.glClear(mask);
    }

    @Inject(method = "glClearColor", at = @At("HEAD"))
    private static void onGlClearColor(float r, float g, float b, float a, CallbackInfo ci) {
        D3D12Bridge.glClearColor(r, g, b, a);
    }

    // === Viewport / Scissor ===
    @Inject(method = "glViewport", at = @At("HEAD"))
    private static void onGlViewport(int x, int y, int w, int h, CallbackInfo ci) {
        D3D12Bridge.onGlViewport(x, y, w, h);
    }

    @Inject(method = "glScissor", at = @At("HEAD"))
    private static void onGlScissor(int x, int y, int w, int h, CallbackInfo ci) {
        // Tracked for future D3D12 scissor sync
    }

    // === Blend state ===
    @Inject(method = "glEnable", at = @At("HEAD"))
    private static void onGlEnable(int cap, CallbackInfo ci) {
        D3D12Bridge.onGlEnable(cap);
    }

    @Inject(method = "glDisable", at = @At("HEAD"))
    private static void onGlDisable(int cap, CallbackInfo ci) {
        D3D12Bridge.onGlDisable(cap);
    }

    @Inject(method = "glBlendFunc", at = @At("HEAD"))
    private static void onGlBlendFunc(int sfactor, int dfactor, CallbackInfo ci) {
        D3D12Bridge.onGlBlendFunc(sfactor, dfactor);
    }

    // === Depth state ===
    @Inject(method = "glDepthFunc", at = @At("HEAD"))
    private static void onGlDepthFunc(int func, CallbackInfo ci) {
    }

    @Inject(method = "glDepthMask", at = @At("HEAD"))
    private static void onGlDepthMask(boolean flag, CallbackInfo ci) {
        D3D12Bridge.onGlDepthMask(flag);
    }

    // === Cull face ===
    @Inject(method = "glCullFace", at = @At("HEAD"))
    private static void onGlCullFace(int mode, CallbackInfo ci) {
        D3D12Bridge.onGlCullFace(mode);
    }

    // === Texture binding ===
    @Inject(method = "glBindTexture", at = @At("HEAD"))
    private static void onGlBindTexture(int target, int texture, CallbackInfo ci) {
        D3D12Bridge.onBindTexture(texture);
    }

    // === Texture upload — capture pixel data for D3D12 SRV ===
    @Inject(method = "glTexImage2D", at = @At("HEAD"))
    private static void onGlTexImage2D(int target, int level, int internalformat,
                                        int width, int height, int border,
                                        int format, int type,
                                        java.nio.ByteBuffer pixels, CallbackInfo ci) {
        D3D12Bridge.onTexImage2D(target, level, internalformat,
            width, height, format, type, pixels);
    }

    // glTexSubImage2D — skip for now: per-region updates don't map cleanly
    // to full D3D12 texture replacement; initial glTexImage2D is sufficient
    @Inject(method = "glTexSubImage2D", at = @At("HEAD"))
    private static void onGlTexSubImage2D(int target, int level,
                                           int xoffset, int yoffset,
                                           int width, int height,
                                           int format, int type,
                                           java.nio.ByteBuffer pixels, CallbackInfo ci) {
        // no-op: avoid corrupting full texture with sub-region data
    }
}
