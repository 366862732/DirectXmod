/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.platform.Transparency;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record FluidModel(ChunkSectionLayer layer, Material.Baked stillMaterial, Material.Baked flowingMaterial, @Nullable Material.Baked overlayMaterial, @Nullable BlockTintSource tintSource) {

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Material stillMaterial, Material flowingMaterial, @Nullable Material overlayMaterial, @Nullable BlockTintSource tintSource) {
        public FluidModel bake(MaterialBaker materials, ModelDebugName modelName) {
            Material.Baked stillMaterial = materials.get(this.stillMaterial, modelName);
            Material.Baked flowingMaterial = materials.get(this.flowingMaterial, modelName);
            Material.Baked overlayMaterial = this.overlayMaterial != null ? materials.get(this.overlayMaterial, modelName) : null;
            Transparency transparency = Unbaked.getTransparency(stillMaterial).or(Unbaked.getTransparency(flowingMaterial));
            if (overlayMaterial != null) {
                transparency = transparency.or(Unbaked.getTransparency(overlayMaterial));
            }
            return new FluidModel(ChunkSectionLayer.byTransparency(transparency), stillMaterial, flowingMaterial, overlayMaterial, this.tintSource);
        }

        private static Transparency getTransparency(Material.Baked material) {
            if (material.forceTranslucent()) {
                return Transparency.TRANSLUCENT;
            }
            return material.sprite().transparency();
        }
    }
}

