package com.dx12.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;
import com.dx12.Dx12Mod;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /**
     * HEAD injection: surface mode — cancel MC's OpenGL rendering entirely.
     * This prevents concurrent GL+D3D12 GPU work on the same window,
     * which causes GPU driver TDR timeouts (~2s per frame).
     *
     * Only active in-world (title screen uses offscreen mode to avoid
     * D3D12 swapchain contention with GL at startup).
     *
     * GL still does a GLFW swap (with cancelled/stale buffer — harmless).
     * D3D12 Present() in MinecraftMixin TAIL overwrites the window pixels.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            // Only cancel rendering when in a world (player & level exist).
            // At title screen, offscreen mode handles rendering.
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                ci.cancel();
            }
        }
    }

    /**
     * TAIL injection: offscreen mode — upload D3D12-rendered
     * pixels as an OpenGL full-screen quad overlay.
     * In surface mode, this is skipped (render happens in MinecraftMixin).
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(CallbackInfo ci) {
        if (!D3D12Bridge.hasSurface()) {
            Dx12Mod.onPostRender();
        }
    }
}
