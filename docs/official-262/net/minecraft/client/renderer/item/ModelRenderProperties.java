/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(value=EnvType.CLIENT)
public record ModelRenderProperties(boolean usesBlockLight, Material.Baked particleMaterial, ItemTransforms transforms) {
    public static ModelRenderProperties fromResolvedModel(ModelBaker baker, ResolvedModel resolvedModel, TextureSlots textureSlots) {
        Material.Baked particleSprite = resolvedModel.resolveParticleMaterial(textureSlots, baker);
        return new ModelRenderProperties(resolvedModel.getTopGuiLight().lightLikeBlock(), particleSprite, resolvedModel.getTopTransforms());
    }

    public void applyToLayer(ItemStackRenderState.LayerRenderState layer, ItemDisplayContext displayContext) {
        layer.setUsesBlockLight(this.usesBlockLight);
        layer.setParticleMaterial(this.particleMaterial);
        layer.setItemTransform(this.transforms.getTransform(displayContext));
    }
}

