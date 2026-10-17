package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RtScene;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

/**
 * P68：把区块网格的 CPU 顶点/索引截获下来送给 native 光追场景。
 *
 * <p>拦截点在 {@code RenderSection.addSectionBuffersToUberBuffer} 的 RETURN：
 * 此时几何已经成功写进 Uber 缓冲（DEFAULT 堆，CPU 不可再读），而调用方传入的
 * 原始 ByteBuffer 仍有效。参数是 {@code ChunkSectionLayer} + 顶点/索引 ByteBuffer。
 *
 * <p>{@code reset()} HEAD 时 sectionNode 还是旧值，用来移除被回收 section 的几何。
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class SectionRenderDispatcherRtGeometryMixin {

    @Shadow private volatile long sectionNode;

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private void dx12_rtDropSection(CallbackInfo ci) {
        try {
            Dx12RtScene.removeSection(this.sectionNode);
        } catch (Throwable ignored) {
            // 光追几何捕获绝不能让原版区块回收流程失败。
        }
    }

    @Inject(method = "addSectionBuffersToUberBuffer", at = @At("RETURN"), remap = false)
    private void dx12_rtUploadSection(ChunkSectionLayer layer, CompiledSectionMesh mesh,
            ByteBuffer vertexBuffer, ByteBuffer indexBuffer,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        try {
            Dx12RtScene.uploadSection(this.sectionNode, layer, mesh, vertexBuffer, indexBuffer);
        } catch (Throwable ignored) {
            // 同上：捕获失败只影响光追阴影，不能影响地形本体渲染。
        }
    }
}
