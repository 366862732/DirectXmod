package com.xgdt.dx11.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xgdt.dx11.perf.region.RegionSubmitter;
import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Routes the per-layer visible draw list through {@link RegionSubmitter}.
 *
 * <p>The redirect runs after vanilla created the render pass, bound the layer
 * pipeline and texture, and built the per-section uniform slices.  If no draw
 * belongs to a region arena, the submitter declines and vanilla's
 * multidraw-emulation path runs unchanged.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {
    @Redirect(
        method = "renderGroup",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;drawMultipleIndexed(Ljava/util/Collection;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;Ljava/util/Collection;Ljava/lang/Object;)V"))
    private <T> void dx11$submitRegions(
        RenderPass pass,
        Collection<RenderPass.Draw<T>> draws,
        GpuBuffer defaultIndexBuffer,
        VertexFormat.IndexType defaultIndexType,
        Collection<String> uniformNames,
        T uniformOwner) {
        boolean handled = RegionSubmitter.submit(
            pass, draws, defaultIndexBuffer, defaultIndexType, uniformNames, uniformOwner);
        if (!handled) {
            pass.drawMultipleIndexed(
                draws, defaultIndexBuffer, defaultIndexType, uniformNames, uniformOwner);
        }
    }
}
