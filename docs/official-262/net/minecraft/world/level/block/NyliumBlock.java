/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.lighting.LightEngine;

public class NyliumBlock
extends Block
implements BonemealableBlock {
    public static final MapCodec<NyliumBlock> CODEC = NyliumBlock.simpleCodec(NyliumBlock::new);

    public MapCodec<NyliumBlock> codec() {
        return CODEC;
    }

    protected NyliumBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static boolean canBeNylium(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        int lightDampeningTopFace = LightEngine.getLightDampeningInto(state, aboveState, Direction.UP, aboveState.getLightDampening());
        return lightDampeningTopFace < 15;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!NyliumBlock.canBeNylium(state, level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.NETHERRACK.defaultBlockState());
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir() && level.isInsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState blockState = level.getBlockState(pos);
        BlockPos abovePos = pos.above();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        HolderLookup.RegistryLookup configuredFeatures = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
            this.place((Registry<ConfiguredFeature<?, ?>>)configuredFeatures, NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL, level, generator, random, abovePos);
        } else if (blockState.is(Blocks.WARPED_NYLIUM)) {
            this.place((Registry<ConfiguredFeature<?, ?>>)configuredFeatures, NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL, level, generator, random, abovePos);
            this.place((Registry<ConfiguredFeature<?, ?>>)configuredFeatures, NetherFeatures.NETHER_SPROUTS_BONEMEAL, level, generator, random, abovePos);
            if (random.nextInt(8) == 0) {
                this.place((Registry<ConfiguredFeature<?, ?>>)configuredFeatures, NetherFeatures.TWISTING_VINES_BONEMEAL, level, generator, random, abovePos);
            }
        }
    }

    private void place(Registry<ConfiguredFeature<?, ?>> configuredFeatures, ResourceKey<ConfiguredFeature<?, ?>> id, ServerLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos) {
        if (level.isInsideBuildHeight(pos)) {
            configuredFeatures.get(id).ifPresent(h -> ((ConfiguredFeature)h.value()).place(level, generator, random, pos));
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}

