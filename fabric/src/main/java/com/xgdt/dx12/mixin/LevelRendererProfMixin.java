package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RenderProf;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * P51：拆解 LevelRenderer.render（P50 的 WORLD 段）内部构成。
 * 这三段是世界渲染中"非提交"的 CPU 部分，用于判断 2~3.6ms 花在哪。
 */
@Mixin(LevelRenderer.class)
public class LevelRendererProfMixin {

    @Inject(method = "submitFeatures", at = @At("HEAD"), remap = false)
    private void dx12_profSubmitFeaturesHead(LevelRenderState levelRenderState,
                                             SubmitNodeCollector submitNodeCollector,
                                             boolean renderOutline, CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.SUBMIT_FEATURES);
    }

    @Inject(method = "submitFeatures", at = @At("RETURN"), remap = false)
    private void dx12_profSubmitFeaturesReturn(LevelRenderState levelRenderState,
                                               SubmitNodeCollector submitNodeCollector,
                                               boolean renderOutline, CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.SUBMIT_FEATURES);
    }

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"), remap = false)
    private void dx12_profPrepareChunksHead(Matrix4fc modelViewMatrix,
                                            CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        Dx12RenderProf.begin(Dx12RenderProf.PREPARE_CHUNKS);
    }

    @Inject(method = "prepareChunkRenders", at = @At("RETURN"), remap = false)
    private void dx12_profPrepareChunksReturn(Matrix4fc modelViewMatrix,
                                              CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        Dx12RenderProf.end(Dx12RenderProf.PREPARE_CHUNKS);
    }

    @Inject(method = "compileSections", at = @At("HEAD"), remap = false)
    private void dx12_profCompileSectionsHead(CameraRenderState camera, CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.COMPILE_SECTIONS);
    }

    @Inject(method = "compileSections", at = @At("RETURN"), remap = false)
    private void dx12_profCompileSectionsReturn(CameraRenderState camera, CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.COMPILE_SECTIONS);
    }
}
