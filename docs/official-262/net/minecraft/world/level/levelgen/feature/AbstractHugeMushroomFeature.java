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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature
extends Feature<HugeMushroomFeatureConfiguration> {
    public static final int MIN_MUSHROOM_HEIGHT = 4;

    public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    protected void placeTrunk(WorldGenLevel level, RandomSource random, BlockPos origin, HugeMushroomFeatureConfiguration config, int treeHeight, BlockPos.MutableBlockPos blockPos) {
        for (int dy = 0; dy < treeHeight; ++dy) {
            blockPos.set(origin).move(Direction.UP, dy);
            this.placeMushroomBlock(level, blockPos, config.stemProvider().getState(level, random, origin));
        }
    }

    protected void placeMushroomBlock(LevelAccessor level, BlockPos.MutableBlockPos blockPos, BlockState newState) {
        BlockState currentState = level.getBlockState(blockPos);
        if (currentState.isAir() || currentState.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
            this.setBlock(level, blockPos, newState);
        }
    }

    protected int getTreeHeight(RandomSource random) {
        int treeHeight = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) {
            treeHeight *= 2;
        }
        return treeHeight;
    }

    protected boolean isValidPosition(WorldGenLevel level, BlockPos origin, int treeHeight, BlockPos.MutableBlockPos blockPos, HugeMushroomFeatureConfiguration config) {
        int y = origin.getY();
        if (y < level.getMinY() + 1 || y + treeHeight + 1 > level.getMaxY()) {
            return false;
        }
        if (!config.canPlaceOn().test(level, origin.below())) {
            return false;
        }
        for (int dy = 0; dy <= treeHeight; ++dy) {
            int radius = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius(), dy);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    BlockState state = level.getBlockState(blockPos.setWithOffset(origin, dx, dy, dz));
                    if (state.isAir() || state.is(BlockTags.LEAVES)) continue;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> context) {
        BlockPos.MutableBlockPos blockPos;
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        HugeMushroomFeatureConfiguration config = context.config();
        int treeHeight = this.getTreeHeight(random);
        if (!this.isValidPosition(level, origin, treeHeight, blockPos = new BlockPos.MutableBlockPos(), config)) {
            return false;
        }
        this.makeCap(level, random, origin, treeHeight, blockPos, config);
        this.placeTrunk(level, random, origin, config, treeHeight, blockPos);
        return true;
    }

    protected abstract int getTreeRadiusForHeight(int var1, int var2, int var3, int var4);

    protected abstract void makeCap(WorldGenLevel var1, RandomSource var2, BlockPos var3, int var4, BlockPos.MutableBlockPos var5, HugeMushroomFeatureConfiguration var6);
}

