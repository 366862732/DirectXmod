/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
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
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;
import org.jspecify.annotations.Nullable;

public class BasaltColumnsFeature
extends Feature<ColumnFeatureConfiguration> {
    private static final ImmutableList<Block> CANNOT_PLACE_ON = ImmutableList.of((Object)Blocks.LAVA, (Object)Blocks.BEDROCK, (Object)Blocks.MAGMA_BLOCK, (Object)Blocks.SOUL_SAND, (Object)Blocks.NETHER_BRICKS, (Object)Blocks.NETHER_BRICK_FENCE, (Object)Blocks.NETHER_BRICK_STAIRS, (Object)Blocks.NETHER_WART, (Object)Blocks.CHEST, (Object)Blocks.SPAWNER);
    private static final int CLUSTERED_REACH = 5;
    private static final int CLUSTERED_SIZE = 50;
    private static final int UNCLUSTERED_REACH = 8;
    private static final int UNCLUSTERED_SIZE = 15;

    public BasaltColumnsFeature(Codec<ColumnFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ColumnFeatureConfiguration> context) {
        int lavaSeaLevel = context.chunkGenerator().getSeaLevel();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ColumnFeatureConfiguration config = context.config();
        if (!BasaltColumnsFeature.canPlaceAt(level, lavaSeaLevel, origin.mutable())) {
            return false;
        }
        int columnHeight = config.height().sample(random);
        boolean genereteClustered = random.nextFloat() < 0.9f;
        int reach = Math.min(columnHeight, genereteClustered ? 5 : 8);
        int count = genereteClustered ? 50 : 15;
        boolean placed = false;
        for (BlockPos pos : BlockPos.randomBetweenClosed(random, count, origin.getX() - reach, origin.getY(), origin.getZ() - reach, origin.getX() + reach, origin.getY(), origin.getZ() + reach)) {
            int blocksToPlaceY = columnHeight - pos.distManhattan(origin);
            if (blocksToPlaceY < 0) continue;
            placed |= this.placeColumn(level, lavaSeaLevel, pos, blocksToPlaceY, config.reach().sample(random));
        }
        return placed;
    }

    private boolean placeColumn(LevelAccessor level, int lavaSeaLevel, BlockPos origin, int columnHeight, int reach) {
        boolean placedAny = false;
        block0: for (BlockPos pos : BlockPos.betweenClosed(origin.getX() - reach, origin.getY(), origin.getZ() - reach, origin.getX() + reach, origin.getY(), origin.getZ() + reach)) {
            BlockPos columnPos;
            int stepLimit = pos.distManhattan(origin);
            BlockPos blockPos = columnPos = BasaltColumnsFeature.isAirOrLavaOcean(level, lavaSeaLevel, pos) ? BasaltColumnsFeature.findSurface(level, lavaSeaLevel, pos.mutable(), stepLimit) : BasaltColumnsFeature.findAir(level, pos.mutable(), stepLimit);
            if (columnPos == null) continue;
            BlockPos.MutableBlockPos cursor = columnPos.mutable();
            for (int blocksY = columnHeight - stepLimit / 2; blocksY >= 0; --blocksY) {
                if (BasaltColumnsFeature.isAirOrLavaOcean(level, lavaSeaLevel, cursor)) {
                    this.setBlock(level, cursor, Blocks.BASALT.defaultBlockState());
                    cursor.move(Direction.UP);
                    placedAny = true;
                    continue;
                }
                if (!level.getBlockState(cursor).is(Blocks.BASALT)) continue block0;
                cursor.move(Direction.UP);
            }
        }
        return placedAny;
    }

    private static @Nullable BlockPos findSurface(LevelAccessor level, int lavaSeaLevel, BlockPos.MutableBlockPos cursor, int limit) {
        while (cursor.getY() > level.getMinY() + 1 && limit > 0) {
            --limit;
            if (BasaltColumnsFeature.canPlaceAt(level, lavaSeaLevel, cursor)) {
                return cursor;
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean canPlaceAt(LevelAccessor level, int lavaSeaLevel, BlockPos.MutableBlockPos cursor) {
        if (BasaltColumnsFeature.isAirOrLavaOcean(level, lavaSeaLevel, cursor)) {
            BlockState blockState = level.getBlockState(cursor.move(Direction.DOWN));
            cursor.move(Direction.UP);
            return !blockState.isAir() && !CANNOT_PLACE_ON.contains((Object)blockState.getBlock());
        }
        return false;
    }

    private static @Nullable BlockPos findAir(LevelAccessor level, BlockPos.MutableBlockPos cursor, int limit) {
        while (cursor.getY() <= level.getMaxY() && limit > 0) {
            --limit;
            BlockState blockState = level.getBlockState(cursor);
            if (CANNOT_PLACE_ON.contains((Object)blockState.getBlock())) {
                return null;
            }
            if (blockState.isAir()) {
                return cursor;
            }
            cursor.move(Direction.UP);
        }
        return null;
    }

    private static boolean isAirOrLavaOcean(LevelAccessor level, int lavaSeaLevel, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        return blockState.isAir() || blockState.is(Blocks.LAVA) && blockPos.getY() <= lavaSeaLevel;
    }
}

