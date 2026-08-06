/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;

public class DeltaFeature
extends Feature<DeltaFeatureConfiguration> {
    private static final ImmutableList<Block> CANNOT_REPLACE = ImmutableList.of((Object)Blocks.BEDROCK, (Object)Blocks.NETHER_BRICKS, (Object)Blocks.NETHER_BRICK_FENCE, (Object)Blocks.NETHER_BRICK_STAIRS, (Object)Blocks.NETHER_WART, (Object)Blocks.CHEST, (Object)Blocks.SPAWNER);
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final double RIM_SPAWN_CHANCE = 0.9;

    public DeltaFeature(Codec<DeltaFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DeltaFeatureConfiguration> context) {
        boolean anyPlaced = false;
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        DeltaFeatureConfiguration config = context.config();
        BlockPos origin = context.origin();
        boolean spawnRim = random.nextDouble() < 0.9;
        int rimX = spawnRim ? config.rimSize().sample(random) : 0;
        int rimZ = spawnRim ? config.rimSize().sample(random) : 0;
        boolean hasRim = spawnRim && rimX != 0 && rimZ != 0;
        int radiusX = config.size().sample(random);
        int radiusZ = config.size().sample(random);
        int radiusLimit = Math.max(radiusX, radiusZ);
        for (BlockPos pos : BlockPos.withinManhattan(origin, radiusX, 0, radiusZ)) {
            BlockPos posOffset;
            if (pos.distManhattan(origin) > radiusLimit) break;
            if (!DeltaFeature.isClear(level, pos, config)) continue;
            if (hasRim) {
                anyPlaced = true;
                this.setBlock(level, pos, config.rim());
            }
            if (!DeltaFeature.isClear(level, posOffset = pos.offset(rimX, 0, rimZ), config)) continue;
            anyPlaced = true;
            this.setBlock(level, posOffset, config.contents());
        }
        return anyPlaced;
    }

    private static boolean isClear(LevelAccessor level, BlockPos pos, DeltaFeatureConfiguration config) {
        BlockState state = level.getBlockState(pos);
        if (state.is(config.contents().getBlock())) {
            return false;
        }
        if (CANNOT_REPLACE.contains((Object)state.getBlock())) {
            return false;
        }
        for (Direction d : DIRECTIONS) {
            boolean isAir = level.getBlockState(pos.relative(d)).isAir();
            if ((!isAir || d == Direction.UP) && (isAir || d != Direction.UP)) continue;
            return false;
        }
        return true;
    }
}

