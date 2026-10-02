package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RenderProf;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** P51：可见性与遮挡图每帧更新耗时。 */
@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphProfMixin {

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void dx12_profOcclusionHead(CameraRenderState camera, int fov,
                                        ChunkLoadingRenderState chunkLoadingRenderState, CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.OCCLUSION);
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void dx12_profOcclusionReturn(CameraRenderState camera, int fov,
                                          ChunkLoadingRenderState chunkLoadingRenderState, CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.OCCLUSION);
    }
}
