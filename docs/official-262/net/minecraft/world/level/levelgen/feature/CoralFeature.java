/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature
extends Feature<NoneFeatureConfiguration> {
    public CoralFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Optional<Block> coral = BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORAL_BLOCKS, random).map(Holder::value);
        if (coral.isEmpty()) {
            return false;
        }
        return this.placeFeature(level, random, origin, coral.get().defaultBlockState());
    }

    protected abstract boolean placeFeature(LevelAccessor var1, RandomSource var2, BlockPos var3, BlockState var4);

    protected boolean placeCoralBlock(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        BlockState targetBlockState = level.getBlockState(pos);
        if (!targetBlockState.is(Blocks.WATER) && !targetBlockState.is(BlockTags.CORALS) || !level.getBlockState(above).is(Blocks.WATER)) {
            return false;
        }
        level.setBlock(pos, state, 3);
        if (random.nextFloat() < 0.25f) {
            BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORALS, random).map(Holder::value).ifPresent(block -> level.setBlock(above, block.defaultBlockState(), 2));
        } else if (random.nextFloat() < 0.05f) {
            level.setBlock(above, (BlockState)Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, random.nextInt(4) + 1), 2);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos relativePos;
            if (!(random.nextFloat() < 0.2f) || !level.getBlockState(relativePos = pos.relative(direction)).is(Blocks.WATER)) continue;
            BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.WALL_CORALS, random).map(Holder::value).ifPresent(coral -> {
                BlockState coralFanState = coral.defaultBlockState();
                if (coralFanState.hasProperty(BaseCoralWallFanBlock.FACING)) {
                    coralFanState = (BlockState)coralFanState.setValue(BaseCoralWallFanBlock.FACING, direction);
                }
                level.setBlock(relativePos, coralFanState, 2);
            });
        }
        return true;
    }
}

