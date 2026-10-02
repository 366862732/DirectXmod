package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RenderProf;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P50：给 GuiRenderer.render 计时（GameRenderer.render 的子集）。
 */
@Mixin(GuiRenderer.class)
public class GuiRendererProfMixin {

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void dx12_profGuiHead(CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.GUI);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void dx12_profGuiReturn(CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.GUI);
    }
}
