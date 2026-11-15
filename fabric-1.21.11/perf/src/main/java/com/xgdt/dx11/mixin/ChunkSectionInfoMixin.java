package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.region.RegionFrame;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports each visible section's origin, fade visibility and atlas size while
 * vanilla builds its per-section uniform list.  The values are aggregated into
 * one region uniform per frame by {@link RegionFrame}.
 */
@Environment(EnvType.CLIENT)
@Mixin(DynamicUniforms.ChunkSectionInfo.class)
public class ChunkSectionInfoMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void dx11$recordRegionVisibility(
        Matrix4fc modelView,
        int x,
        int y,
        int z,
        float visibility,
        int atlasWidth,
        int atlasHeight,
        CallbackInfo ci) {
        RegionFrame.recordSection(x, y, z, visibility, atlasWidth, atlasHeight);
    }
}
