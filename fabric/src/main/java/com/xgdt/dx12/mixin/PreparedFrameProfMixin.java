package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RenderProf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** P52：实体/方块实体等 feature 的实际绘制耗时（executeSolid）。 */
@Mixin(targets = "net.minecraft.client.renderer.feature.FeatureRenderDispatcher$PreparedFrame")
public class PreparedFrameProfMixin {

    @Inject(method = "executeSolid", at = @At("HEAD"), remap = false)
    private void dx12_profExecFeaturesHead(CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.EXEC_FEATURES);
    }

    @Inject(method = "executeSolid", at = @At("RETURN"), remap = false)
    private void dx12_profExecFeaturesReturn(CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.EXEC_FEATURES);
    }
}
