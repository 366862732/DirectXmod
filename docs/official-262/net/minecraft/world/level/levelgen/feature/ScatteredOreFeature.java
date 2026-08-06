/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ScatteredOreFeature
extends Feature<OreConfiguration> {
    private static final int MAX_DIST_FROM_ORIGIN = 7;

    public ScatteredOreFeature(Codec<OreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OreConfiguration config = context.config();
        BlockPos origin = context.origin();
        int numberOfTries = random.nextInt(config.size + 1);
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
        block0: for (int i = 0; i < numberOfTries; ++i) {
            this.offsetTargetPos(targetPos, random, origin, Math.min(i, 7));
            BlockState blockState = level.getBlockState(targetPos);
            for (OreConfiguration.TargetBlockState targetState : config.targetStates) {
                if (!OreFeature.canPlaceOre(blockState, level::getBlockState, random, config, targetState, targetPos)) continue;
                level.setBlock(targetPos, targetState.state, 2);
                continue block0;
            }
        }
        return true;
    }

    private void offsetTargetPos(BlockPos.MutableBlockPos targetPos, RandomSource random, BlockPos origin, int maxDistFromOriginForThisTry) {
        int xd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
        int yd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
        int zd = this.getRandomPlacementInOneAxisRelativeToOrigin(random, maxDistFromOriginForThisTry);
        targetPos.setWithOffset(origin, xd, yd, zd);
    }

    private int getRandomPlacementInOneAxisRelativeToOrigin(RandomSource random, int maxDistanceFromOrigin) {
        return Math.round((random.nextFloat() - random.nextFloat()) * (float)maxDistanceFromOrigin);
    }
}

