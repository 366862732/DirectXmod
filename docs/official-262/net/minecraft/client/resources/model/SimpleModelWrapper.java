/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Multimap
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, Material.Baked particleMaterial) implements BlockStateModelPart
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static BlockStateModelPart bake(ModelBaker modelBakery, Identifier location, ModelState state) {
        ResolvedModel model = modelBakery.getModel(location);
        TextureSlots textureSlots = model.getTopTextureSlots();
        boolean hasAmbientOcclusion = model.getTopAmbientOcclusion();
        Material.Baked particleMaterial = model.resolveParticleMaterial(textureSlots, modelBakery);
        QuadCollection geometry = model.bakeTopGeometry(textureSlots, modelBakery, state);
        Multimap<Identifier, Identifier> forbiddenSprites = SimpleModelWrapper.findNonBlockSprites(geometry);
        if (forbiddenSprites != null) {
            LOGGER.warn("Rejecting block model {}, since it contains sprites from outside of supported atlas: {}", (Object)location, forbiddenSprites);
            return modelBakery.missingBlockModelPart();
        }
        return new SimpleModelWrapper(geometry, hasAmbientOcclusion, particleMaterial);
    }

    public static @Nullable Multimap<Identifier, Identifier> findNonBlockSprites(QuadCollection geometry) {
        HashMultimap forbiddenSprites = null;
        for (BakedQuad bakedQuad : geometry.getAll()) {
            TextureAtlasSprite sprite = bakedQuad.materialInfo().sprite();
            if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) continue;
            if (forbiddenSprites == null) {
                forbiddenSprites = HashMultimap.create();
            }
            forbiddenSprites.put((Object)sprite.atlasLocation(), (Object)sprite.contents().name());
        }
        return forbiddenSprites;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return this.quads.getQuads(direction);
    }

    @Override
    public int materialFlags() {
        return this.quads.materialFlags();
    }
}

