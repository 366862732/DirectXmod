/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.block.model;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public interface BlockModel {
    public void update(BlockModelRenderState var1, BlockState var2, BlockDisplayContext var3, long var4);

    @Environment(value=EnvType.CLIENT)
    public record BakingContext(EntityModelSet entityModelSet, SpriteGetter sprites, PlayerSkinRenderCache playerSkinRenderCache, Function<BlockState, BlockStateModel> modelGetter, BlockModel missingBlockModel) implements SpecialModelRenderer.BakingContext
    {
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Unbaked {
        public BlockModel bake(BakingContext var1, Matrix4fc var2);
    }
}

