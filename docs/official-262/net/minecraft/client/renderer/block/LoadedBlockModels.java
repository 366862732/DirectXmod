/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.block;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class LoadedBlockModels {
    private static final Matrix4fc IDENTITY = new Matrix4f();
    private final Map<BlockState, BlockModel.Unbaked> unbakedModels;
    private final EntityModelSet entityModelSet;
    private final SpriteGetter sprites;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public LoadedBlockModels(Map<BlockState, BlockModel.Unbaked> unbakedModels, EntityModelSet entityModelSet, SpriteGetter sprites, PlayerSkinRenderCache playerSkinRenderCache) {
        this.unbakedModels = unbakedModels;
        this.entityModelSet = entityModelSet;
        this.sprites = sprites;
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    public CompletableFuture<Map<BlockState, BlockModel>> bake(Function<BlockState, BlockStateModel> bakedBlockStateModels, BlockStateModel missingModel, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            BlockStateModelWrapper wrappedMissingModel = new BlockStateModelWrapper(missingModel, List.of(), IDENTITY);
            BlockModel.BakingContext context = new BlockModel.BakingContext(this.entityModelSet, this.sprites, this.playerSkinRenderCache, bakedBlockStateModels, wrappedMissingModel);
            ImmutableMap.Builder result = ImmutableMap.builder();
            this.unbakedModels.forEach((blockState, unbakedModel) -> result.put(blockState, (Object)unbakedModel.bake(context, IDENTITY)));
            return result.build();
        }, executor);
    }
}

