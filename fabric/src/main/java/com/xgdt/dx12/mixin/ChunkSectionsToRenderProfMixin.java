package com.xgdt.dx12.mixin;

import com.mojang.blaze3d.textures.GpuSampler;
import com.xgdt.dx12.dx12.Dx12RenderProf;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** P51：区块实际绘制 pass（OPAQUE + TRANSLUCENT 合计），含 multidraw 批量提交。 */
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderProfMixin {

    @Inject(method = "renderGroup", at = @At("HEAD"), remap = false)
    private void dx12_profRenderGroupHead(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.RENDER_GROUP);
    }

    @Inject(method = "renderGroup", at = @At("RETURN"), remap = false)
    private void dx12_profRenderGroupReturn(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.RENDER_GROUP);
    }
}
