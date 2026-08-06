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
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WeepingVinesFeature
extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public WeepingVinesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.isEmptyBlock(origin)) {
            return false;
        }
        BlockState stateAbove = level.getBlockState(origin.above());
        if (!stateAbove.is(Blocks.NETHERRACK) && !stateAbove.is(Blocks.NETHER_WART_BLOCK)) {
            return false;
        }
        this.placeRoofNetherWart(level, random, origin);
        this.placeRoofWeepingVines(level, random, origin);
        return true;
    }

    private void placeRoofNetherWart(LevelAccessor level, RandomSource random, BlockPos origin) {
        level.setBlock(origin, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
        BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 200; ++i) {
            placePos.setWithOffset(origin, random.nextInt(6) - random.nextInt(6), random.nextInt(2) - random.nextInt(5), random.nextInt(6) - random.nextInt(6));
            if (!level.isEmptyBlock(placePos)) continue;
            int neighbours = 0;
            for (Direction direction : DIRECTIONS) {
                BlockState neighbourBlockState = level.getBlockState(neighbourPos.setWithOffset((Vec3i)placePos, direction));
                if (neighbourBlockState.is(Blocks.NETHERRACK) || neighbourBlockState.is(Blocks.NETHER_WART_BLOCK)) {
                    ++neighbours;
                }
                if (neighbours > 1) break;
            }
            if (neighbours != true) continue;
            level.setBlock(placePos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
        }
    }

    private void placeRoofWeepingVines(LevelAccessor level, RandomSource random, BlockPos origin) {
        BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 100; ++i) {
            BlockState stateAbove;
            placePos.setWithOffset(origin, random.nextInt(8) - random.nextInt(8), random.nextInt(2) - random.nextInt(7), random.nextInt(8) - random.nextInt(8));
            if (!level.isEmptyBlock(placePos) || !(stateAbove = level.getBlockState((BlockPos)placePos.above())).is(Blocks.NETHERRACK) && !stateAbove.is(Blocks.NETHER_WART_BLOCK)) continue;
            int vineHeight = Mth.nextInt(random, 1, 8);
            if (random.nextInt(6) == 0) {
                vineHeight *= 2;
            }
            if (random.nextInt(5) == 0) {
                vineHeight = 1;
            }
            int minVineAge = 17;
            int maxVineAge = 25;
            WeepingVinesFeature.placeWeepingVinesColumn(level, random, placePos, vineHeight, 17, 25);
        }
    }

    public static void placeWeepingVinesColumn(LevelAccessor level, RandomSource random, BlockPos.MutableBlockPos placePos, int totalHeight, int minAge, int naxAge) {
        for (int height = 0; height <= totalHeight; ++height) {
            if (level.isEmptyBlock(placePos)) {
                if (height == totalHeight || !level.isEmptyBlock((BlockPos)placePos.below())) {
                    level.setBlock(placePos, (BlockState)Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(random, minAge, naxAge)), 2);
                    break;
                }
                level.setBlock(placePos, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
            }
            placePos.move(Direction.DOWN);
        }
    }
}

