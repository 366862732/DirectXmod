package com.dx12.mixin;

import java.nio.ByteBuffer;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dx12.D3D12Bridge;
import com.dx12.Dx12Mod;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;

/**
 * Mixin: intercept chunk section mesh compilation.
 * After SectionCompiler.compile() finishes building all render layers,
 * capture the raw vertex data for each layer and upload to D3D12.
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @Inject(method = "compile", at = @At("RETURN"))
    private void onCompileReturn(
        SectionPos sectionPos,
        RenderSectionRegion region,
        VertexSorting vertexSorting,
        SectionBufferBuilderPack builders,
        CallbackInfoReturnable<SectionCompiler.Results> cir
    ) {
        try {
            SectionCompiler.Results results = cir.getReturnValue();
            if (results == null || results.renderedLayers.isEmpty()) return;

            int sx = sectionPos.getX();
            int sy = sectionPos.getY();
            int sz = sectionPos.getZ();

            for (Map.Entry<ChunkSectionLayer, MeshData> entry : results.renderedLayers.entrySet()) {
                MeshData meshData = entry.getValue();
                MeshData.DrawState drawState = meshData.drawState();
                int vertexCount = drawState.vertexCount();
                int vertexStride = drawState.format().getVertexSize();

                if (vertexCount == 0 || vertexStride == 0) continue;

                ByteBuffer vertexBuffer = meshData.vertexBuffer();
                if (vertexBuffer == null) continue;

                D3D12Bridge.uploadChunkMesh(sx, sy, sz, vertexBuffer, vertexCount, vertexStride);
                return; // One upload per section (all layers share same section coords)
            }
        } catch (Exception e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] Chunk mesh capture failed: {}", e.getMessage());
        }
    }
}
