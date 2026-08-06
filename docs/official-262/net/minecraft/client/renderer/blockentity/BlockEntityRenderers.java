/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityWithBoundingBoxRenderer;
import net.minecraft.client.renderer.blockentity.BrushableBlockRenderer;
import net.minecraft.client.renderer.blockentity.CampfireRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.CopperGolemStatueBlockRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.ShelfRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.client.renderer.blockentity.TestInstanceRenderer;
import net.minecraft.client.renderer.blockentity.TheEndGatewayRenderer;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.TrialSpawnerRenderer;
import net.minecraft.client.renderer.blockentity.VaultRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

@Environment(value=EnvType.CLIENT)
public class BlockEntityRenderers {
    private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> PROVIDERS = Maps.newHashMap();

    private static <T extends BlockEntity, S extends BlockEntityRenderState> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> renderer) {
        PROVIDERS.put(type, renderer);
    }

    public static Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> createEntityRenderers(BlockEntityRendererProvider.Context context) {
        ImmutableMap.Builder result = ImmutableMap.builder();
        PROVIDERS.forEach((type, provider) -> {
            try {
                result.put(type, provider.create(context));
            }
            catch (Exception e) {
                throw new IllegalStateException("Failed to create model for " + String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)type)), e);
            }
        });
        return result.build();
    }

    static {
        BlockEntityRenderers.register(BlockEntityTypes.SIGN, StandingSignRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.HANGING_SIGN, HangingSignRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.MOB_SPAWNER, SpawnerRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.PISTON, context -> new PistonHeadRenderer());
        BlockEntityRenderers.register(BlockEntityTypes.CHEST, ChestRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.ENDER_CHEST, ChestRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.TRAPPED_CHEST, ChestRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.ENCHANTING_TABLE, EnchantTableRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.LECTERN, LecternRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.END_PORTAL, context -> new TheEndPortalRenderer());
        BlockEntityRenderers.register(BlockEntityTypes.END_GATEWAY, context -> new TheEndGatewayRenderer());
        BlockEntityRenderers.register(BlockEntityTypes.BEACON, context -> new BeaconRenderer());
        BlockEntityRenderers.register(BlockEntityTypes.SKULL, SkullBlockRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.BANNER, BannerRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.STRUCTURE_BLOCK, context -> new BlockEntityWithBoundingBoxRenderer());
        BlockEntityRenderers.register(BlockEntityTypes.TEST_INSTANCE_BLOCK, context -> new TestInstanceRenderer());
        BlockEntityRenderers.register(BlockEntityTypes.SHULKER_BOX, ShulkerBoxRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.CONDUIT, ConduitRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.BELL, BellRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.CAMPFIRE, CampfireRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.BRUSHABLE_BLOCK, BrushableBlockRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.DECORATED_POT, DecoratedPotRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.TRIAL_SPAWNER, TrialSpawnerRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.VAULT, VaultRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.COPPER_GOLEM_STATUE, CopperGolemStatueBlockRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.SHELF, ShelfRenderer::new);
    }
}

