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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BlueIceFeature
extends Feature<NoneFeatureConfiguration> {
    public BlueIceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (origin.getY() > level.getSeaLevel() - 1) {
            return false;
        }
        if (!level.getBlockState(origin).is(Blocks.WATER) && !level.getBlockState(origin.below()).is(Blocks.WATER)) {
            return false;
        }
        boolean foundPackedIce = false;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN || !level.getBlockState(origin.relative(direction)).is(Blocks.PACKED_ICE)) continue;
            foundPackedIce = true;
            break;
        }
        if (!foundPackedIce) {
            return false;
        }
        level.setBlock(origin, Blocks.BLUE_ICE.defaultBlockState(), 2);
        block1: for (int i = 0; i < 200; ++i) {
            BlockPos placePos;
            BlockState placeState;
            int yOff = random.nextInt(5) - random.nextInt(6);
            int xzDiff = 3;
            if (yOff < 2) {
                xzDiff += yOff / 2;
            }
            if (xzDiff < 1 || !(placeState = level.getBlockState(placePos = origin.offset(random.nextInt(xzDiff) - random.nextInt(xzDiff), yOff, random.nextInt(xzDiff) - random.nextInt(xzDiff)))).isAir() && !placeState.is(Blocks.WATER) && !placeState.is(Blocks.PACKED_ICE) && !placeState.is(Blocks.ICE)) continue;
            for (Direction direction : Direction.values()) {
                BlockState relativeBlockState = level.getBlockState(placePos.relative(direction));
                if (!relativeBlockState.is(Blocks.BLUE_ICE)) continue;
                level.setBlock(placePos, Blocks.BLUE_ICE.defaultBlockState(), 2);
                continue block1;
            }
        }
        return true;
    }
}

