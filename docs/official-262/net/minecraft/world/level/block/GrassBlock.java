/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.references.BlockItemIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GrassBlock
extends SpreadingSnowyBlock
implements BonemealableBlock {
    public static final MapCodec<GrassBlock> CODEC = GrassBlock.simpleCodec(GrassBlock::new);

    public MapCodec<GrassBlock> codec() {
        return CODEC;
    }

    public GrassBlock(BlockBehaviour.Properties properties) {
        super(properties, BlockItemIds.DIRT.block());
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
        BlockPos above = pos.above();
        BlockState grass = Blocks.SHORT_GRASS.defaultBlockState();
        Optional grassFeature = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(VegetationPlacements.GRASS_BONEMEAL);
        block0: for (int j = 0; j < 128; ++j) {
            BonemealableBlock bonemealableBlock;
            BlockPos testPos = above;
            for (int i = 0; i < j / 16; ++i) {
                if (!level.getBlockState((testPos = testPos.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1)).below()).is(this) || level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos)) continue block0;
            }
            BlockState testState = level.getBlockState(testPos);
            if (testState.is(grass.getBlock()) && random.nextInt(10) == 0 && (bonemealableBlock = (BonemealableBlock)((Object)grass.getBlock())).isValidBonemealTarget(level, testPos, testState)) {
                bonemealableBlock.performBonemeal(level, random, testPos, testState);
            }
            if (!testState.isAir() || level.isOutsideBuildHeight(testPos)) continue;
            if (random.nextInt(8) == 0) {
                List<ConfiguredFeature<?, ?>> features = level.getBiome(testPos).value().getGenerationSettings().getBoneMealFeatures();
                if (features.isEmpty()) continue;
                ConfiguredFeature<?, ?> placementFeature = Util.getRandom(features, random);
                placementFeature.place(level, level.getChunkSource().getGenerator(), random, testPos);
                continue;
            }
            if (!grassFeature.isPresent()) continue;
            ((PlacedFeature)((Holder.Reference)grassFeature.get()).value()).place(level, level.getChunkSource().getGenerator(), random, testPos);
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}

