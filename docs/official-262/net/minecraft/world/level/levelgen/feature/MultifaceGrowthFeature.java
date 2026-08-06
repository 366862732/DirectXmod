/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceSpreadeableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;

public class MultifaceGrowthFeature
extends Feature<MultifaceGrowthConfiguration> {
    public MultifaceGrowthFeature(Codec<MultifaceGrowthConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MultifaceGrowthConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        MultifaceGrowthConfiguration config = context.config();
        if (!MultifaceGrowthFeature.isAirOrWater(level.getBlockState(origin))) {
            return false;
        }
        Block block = config.placeBlock;
        if (!(block instanceof MultifaceSpreadeableBlock)) {
            return false;
        }
        MultifaceSpreadeableBlock placerBlock = (MultifaceSpreadeableBlock)block;
        List<Direction> searchDirections = config.getShuffledDirections(random);
        if (MultifaceGrowthFeature.placeGrowthIfPossible(placerBlock, level, origin, level.getBlockState(origin), config, random, searchDirections)) {
            return true;
        }
        BlockPos.MutableBlockPos pos = origin.mutable();
        block0: for (Direction searchDirection : searchDirections) {
            pos.set(origin);
            List<Direction> placementDirections = config.getShuffledDirectionsExcept(random, searchDirection.getOpposite());
            for (int i = 0; i < config.searchRange; ++i) {
                pos.setWithOffset((Vec3i)origin, searchDirection);
                BlockState state = level.getBlockState(pos);
                if (!MultifaceGrowthFeature.isAirOrWater(state) && !state.is(config.placeBlock)) continue block0;
                if (!MultifaceGrowthFeature.placeGrowthIfPossible(placerBlock, level, pos, state, config, random, placementDirections)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean placeGrowthIfPossible(MultifaceSpreadeableBlock placerBlock, WorldGenLevel level, BlockPos pos, BlockState oldState, MultifaceGrowthConfiguration config, RandomSource random, List<Direction> placementDirections) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (Direction placementDirection : placementDirections) {
            BlockState neighbourState = level.getBlockState(mutable.setWithOffset((Vec3i)pos, placementDirection));
            if (!neighbourState.is(config.canBePlacedOn)) continue;
            BlockState newState = placerBlock.getStateForPlacement(oldState, level, pos, placementDirection);
            if (newState == null) {
                return false;
            }
            level.setBlock(pos, newState, 3);
            level.getChunk(pos).markPosForPostProcessing(pos);
            if (random.nextFloat() < config.chanceOfSpreading) {
                placerBlock.getSpreader().spreadFromFaceTowardRandomDirection(newState, level, pos, placementDirection, random, true);
            }
            return true;
        }
        return false;
    }

    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }
}

