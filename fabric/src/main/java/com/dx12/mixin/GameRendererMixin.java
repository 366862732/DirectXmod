package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import com.dx12.Dx12Mod;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /**
     * HEAD injection: when D3D12 surface mode is active, skip Minecraft's
     * entire OpenGL rendering pipeline. D3D12 handles all rendering and
     * presents directly to the window swapchain.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            Dx12Mod.onPreRender();
            ci.cancel();
        }
    }

    /**
     * TAIL injection: offscreen mode fallback — upload D3D12-rendered
     * pixels as an OpenGL full-screen quad overlay.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(CallbackInfo ci) {
        if (!D3D12Bridge.hasSurface()) {
            Dx12Mod.onPostRender();
        }
    }
}
