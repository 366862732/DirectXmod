package com.xgdt.dx11.mixin;

import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private per-layer buffer map of a compiled section mesh.
 */
@Mixin(CompiledSectionMesh.class)
public interface CompiledSectionMeshAccessor {
    @Accessor("buffers")
    Map<ChunkSectionLayer, SectionBuffers> dx11$getBuffers();
}
