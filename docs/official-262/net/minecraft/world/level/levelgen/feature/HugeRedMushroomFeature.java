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
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeRedMushroomFeature
extends AbstractHugeMushroomFeature {
    public HugeRedMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected void makeCap(WorldGenLevel level, RandomSource random, BlockPos origin, int treeHeight, BlockPos.MutableBlockPos blockPos, HugeMushroomFeatureConfiguration config) {
        for (int dy = treeHeight - 3; dy <= treeHeight; ++dy) {
            int radius = dy < treeHeight ? config.foliageRadius() : config.foliageRadius() - 1;
            int center = config.foliageRadius() - 2;
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    boolean zEdge;
                    boolean minX = dx == -radius;
                    boolean maxX = dx == radius;
                    boolean minZ = dz == -radius;
                    boolean maxZ = dz == radius;
                    boolean xEdge = minX || maxX;
                    boolean bl = zEdge = minZ || maxZ;
                    if (dy < treeHeight && xEdge == zEdge) continue;
                    blockPos.setWithOffset(origin, dx, dy, dz);
                    BlockState state = config.capProvider().getState(level, random, origin);
                    if (state.hasProperty(HugeMushroomBlock.WEST) && state.hasProperty(HugeMushroomBlock.EAST) && state.hasProperty(HugeMushroomBlock.NORTH) && state.hasProperty(HugeMushroomBlock.SOUTH) && state.hasProperty(HugeMushroomBlock.UP)) {
                        state = (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.setValue(HugeMushroomBlock.UP, dy >= treeHeight - 1)).setValue(HugeMushroomBlock.WEST, dx < -center)).setValue(HugeMushroomBlock.EAST, dx > center)).setValue(HugeMushroomBlock.NORTH, dz < -center)).setValue(HugeMushroomBlock.SOUTH, dz > center);
                    }
                    this.placeMushroomBlock(level, blockPos, state);
                }
            }
        }
    }

    @Override
    protected int getTreeRadiusForHeight(int trunkHeight, int treeHeight, int leafRadius, int yo) {
        int radius = 0;
        if (yo < treeHeight && yo >= treeHeight - 3) {
            radius = leafRadius;
        } else if (yo == treeHeight) {
            radius = leafRadius;
        }
        return radius;
    }
}

