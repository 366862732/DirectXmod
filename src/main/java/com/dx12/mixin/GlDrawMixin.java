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
        // Tracked for future D3D12 viewport sync
    }

    @Inject(method = "glScissor", at = @At("HEAD"))
    private static void onGlScissor(int x, int y, int w, int h, CallbackInfo ci) {
        // Tracked for future D3D12 scissor sync
    }

    // === Blend state ===
    @Inject(method = "glEnable", at = @At("HEAD"))
    private static void onGlEnable(int cap, CallbackInfo ci) {
        // Blend, Cull, DepthTest etc. tracked for future PSO selection
    }

    @Inject(method = "glDisable", at = @At("HEAD"))
    private static void onGlDisable(int cap, CallbackInfo ci) {
    }

    @Inject(method = "glBlendFunc", at = @At("HEAD"))
    private static void onGlBlendFunc(int sfactor, int dfactor, CallbackInfo ci) {
    }

    // === Depth state ===
    @Inject(method = "glDepthFunc", at = @At("HEAD"))
    private static void onGlDepthFunc(int func, CallbackInfo ci) {
    }

    @Inject(method = "glDepthMask", at = @At("HEAD"))
    private static void onGlDepthMask(boolean flag, CallbackInfo ci) {
    }

    // === Cull face ===
    @Inject(method = "glCullFace", at = @At("HEAD"))
    private static void onGlCullFace(int mode, CallbackInfo ci) {
    }
}
