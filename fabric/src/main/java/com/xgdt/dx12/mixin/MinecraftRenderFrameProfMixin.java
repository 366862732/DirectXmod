package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RenderProf;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P50：给 Minecraft.renderFrame 整体计时，并在帧尾触发分段报告。
 *
 * 用 @At("RETURN") 而非 TAIL——renderFrame 在 surface 已 acquire 时会提前 return，
 * RETURN 覆盖所有返回路径，保证 begin/end 严格配对。
 */
@Mixin(Minecraft.class)
public class MinecraftRenderFrameProfMixin {

    @Inject(method = "renderFrame", at = @At("HEAD"), remap = false)
    private void dx12_profFrameHead(boolean advanceGameTime, CallbackInfo ci) {
        Dx12RenderProf.frameStart();
        Dx12RenderProf.begin(Dx12RenderProf.FRAME);
    }

    @Inject(method = "renderFrame", at = @At("RETURN"), remap = false)
    private void dx12_profFrameReturn(boolean advanceGameTime, CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.FRAME);
        Dx12RenderProf.frameEnd();
    }
}
