/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.WeepingVinesFeature;

public class HugeFungusFeature
extends Feature<HugeFungusConfiguration> {
    private static final float HUGE_PROBABILITY = 0.06f;

    public HugeFungusFeature(Codec<HugeFungusConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<HugeFungusConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        HugeFungusConfiguration config = context.config();
        Block allowedBaseBlock = config.validBaseState.getBlock();
        BlockPos newOrigin = null;
        BlockState belowState = level.getBlockState(origin.below());
        if (belowState.is(allowedBaseBlock)) {
            newOrigin = origin;
        }
        if (newOrigin == null) {
            return false;
        }
        int totalHeight = Mth.nextInt(random, 4, 13);
        if (random.nextInt(12) == 0) {
            totalHeight *= 2;
        }
        if (!config.planted) {
            int maxHeight = chunkGenerator.getGenDepth();
            if (newOrigin.getY() + totalHeight + 1 >= maxHeight) {
                return false;
            }
        }
        boolean isHuge = !config.planted && random.nextFloat() < 0.06f;
        level.setBlock(origin, Blocks.AIR.defaultBlockState(), 260);
        this.placeStem(level, random, config, newOrigin, totalHeight, isHuge);
        this.placeHat(level, random, config, newOrigin, totalHeight, isHuge);
        return true;
    }

    private static boolean isReplaceable(WorldGenLevel level, BlockPos pos, HugeFungusConfiguration config, boolean checkNonReplaceablePlants) {
        if (level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::canBeReplaced)) {
            return true;
        }
        if (checkNonReplaceablePlants) {
            return config.replaceableBlocks.test(level, pos);
        }
        return false;
    }

    private void placeStem(WorldGenLevel level, RandomSource random, HugeFungusConfiguration config, BlockPos surfaceOrigin, int totalHeight, boolean isHuge) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockState stem = config.stemState;
        int stemRadius = isHuge ? 1 : 0;
        for (int dx = -stemRadius; dx <= stemRadius; ++dx) {
            for (int dz = -stemRadius; dz <= stemRadius; ++dz) {
                boolean cornerOfHugeStem = isHuge && Mth.abs(dx) == stemRadius && Mth.abs(dz) == stemRadius;
                for (int dy = 0; dy < totalHeight; ++dy) {
                    blockPos.setWithOffset(surfaceOrigin, dx, dy, dz);
                    if (!HugeFungusFeature.isReplaceable(level, blockPos, config, true)) continue;
                    if (config.planted) {
                        if (!level.getBlockState((BlockPos)blockPos.below()).isAir()) {
                            level.destroyBlock(blockPos, true);
                        }
                        level.setBlock(blockPos, stem, 3);
                        continue;
                    }
                    if (cornerOfHugeStem) {
                        if (!(random.nextFloat() < 0.1f)) continue;
                        this.setBlock(level, blockPos, stem);
                        continue;
                    }
                    this.setBlock(level, blockPos, stem);
                }
            }
        }
    }

    private void placeHat(WorldGenLevel level, RandomSource random, HugeFungusConfiguration config, BlockPos surfaceOrigin, int totalHeight, boolean isHuge) {
        int hatStartY;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        boolean placeVines = config.hatState.is(Blocks.NETHER_WART_BLOCK);
        int hatHeight = Math.min(random.nextInt(1 + totalHeight / 3) + 5, totalHeight);
        for (int dy = hatStartY = totalHeight - hatHeight; dy <= totalHeight; ++dy) {
            int radius;
            int n = radius = dy < totalHeight - random.nextInt(3) ? 2 : 1;
            if (hatHeight > 8 && dy < hatStartY + 4) {
                radius = 3;
            }
            if (isHuge) {
                ++radius;
            }
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    boolean isEdgeX = dx == -radius || dx == radius;
                    boolean isEdgeZ = dz == -radius || dz == radius;
                    boolean inside = !isEdgeX && !isEdgeZ && dy != totalHeight;
                    boolean corner = isEdgeX && isEdgeZ;
                    boolean isHatBottom = dy < hatStartY + 3;
                    blockPos.setWithOffset(surfaceOrigin, dx, dy, dz);
                    if (!HugeFungusFeature.isReplaceable(level, blockPos, config, false)) continue;
                    if (config.planted && !level.getBlockState((BlockPos)blockPos.below()).isAir()) {
                        level.destroyBlock(blockPos, true);
                    }
                    if (isHatBottom) {
                        if (inside) continue;
                        this.placeHatDropBlock(level, random, blockPos, config.hatState, placeVines);
                        continue;
                    }
                    if (inside) {
                        this.placeHatBlock(level, random, config, blockPos, 0.1f, 0.2f, placeVines ? 0.1f : 0.0f);
                        continue;
                    }
                    if (corner) {
                        this.placeHatBlock(level, random, config, blockPos, 0.01f, 0.7f, placeVines ? 0.083f : 0.0f);
                        continue;
                    }
                    this.placeHatBlock(level, random, config, blockPos, 5.0E-4f, 0.98f, placeVines ? 0.07f : 0.0f);
                }
            }
        }
    }

    private void placeHatBlock(LevelAccessor level, RandomSource random, HugeFungusConfiguration config, BlockPos.MutableBlockPos blockPos, float decorBlockProbability, float hatBlockProbability, float vinesProbability) {
        if (random.nextFloat() < decorBlockProbability) {
            this.setBlock(level, blockPos, config.decorState);
        } else if (random.nextFloat() < hatBlockProbability) {
            this.setBlock(level, blockPos, config.hatState);
            if (random.nextFloat() < vinesProbability) {
                HugeFungusFeature.tryPlaceWeepingVines(blockPos, level, random);
            }
        }
    }

    private void placeHatDropBlock(LevelAccessor level, RandomSource random, BlockPos blockPos, BlockState hatState, boolean placeVines) {
        if (level.getBlockState(blockPos.below()).is(hatState.getBlock())) {
            this.setBlock(level, blockPos, hatState);
        } else if ((double)random.nextFloat() < 0.15) {
            this.setBlock(level, blockPos, hatState);
            if (placeVines && random.nextInt(11) == 0) {
                HugeFungusFeature.tryPlaceWeepingVines(blockPos, level, random);
            }
        }
    }

    private static void tryPlaceWeepingVines(BlockPos hatBlockPos, LevelAccessor level, RandomSource random) {
        BlockPos.MutableBlockPos placePos = hatBlockPos.mutable().move(Direction.DOWN);
        if (!level.isEmptyBlock(placePos)) {
            return;
        }
        int goalVineHeight = Mth.nextInt(random, 1, 5);
        if (random.nextInt(7) == 0) {
            goalVineHeight *= 2;
        }
        int minVineAge = 23;
        int maxVineAge = 25;
        WeepingVinesFeature.placeWeepingVinesColumn(level, random, placePos, goalVineHeight, 23, 25);
    }
}

