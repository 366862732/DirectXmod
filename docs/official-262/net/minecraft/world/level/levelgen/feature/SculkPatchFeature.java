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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SculkPatchConfiguration;

public class SculkPatchFeature
extends Feature<SculkPatchConfiguration> {
    public SculkPatchFeature(Codec<SculkPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SculkPatchConfiguration> context) {
        int i;
        BlockPos origin;
        WorldGenLevel level = context.level();
        if (!this.canSpreadFrom(level, origin = context.origin())) {
            return false;
        }
        SculkPatchConfiguration config = context.config();
        RandomSource random = context.random();
        SculkSpreader spreader = SculkSpreader.createWorldGenSpreader();
        int totalRounds = config.spreadRounds() + config.growthRounds();
        for (int round = 0; round < totalRounds; ++round) {
            for (int i2 = 0; i2 < config.chargeCount(); ++i2) {
                spreader.addCursors(origin, config.amountPerCharge());
            }
            boolean spreadVeins = round < config.spreadRounds();
            for (i = 0; i < config.spreadAttempts(); ++i) {
                spreader.updateCursors(level, origin, random, spreadVeins);
            }
            spreader.clear();
        }
        BlockPos below = origin.below();
        if (random.nextFloat() <= config.catalystChance() && level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
            level.setBlock(origin, Blocks.SCULK_CATALYST.defaultBlockState(), 3);
        }
        int extraGrowths = config.extraRareGrowths().sample(random);
        for (i = 0; i < extraGrowths; ++i) {
            BlockPos candidate = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (!level.getBlockState(candidate).isAir() || !level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) continue;
            level.setBlock(candidate, (BlockState)Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
        }
        return true;
    }

    private boolean canSpreadFrom(LevelAccessor level, BlockPos origin) {
        block5: {
            block4: {
                BlockState start = level.getBlockState(origin);
                if (start.getBlock() instanceof SculkBehaviour) {
                    return true;
                }
                if (start.isAir()) break block4;
                if (!start.is(Blocks.WATER) || !start.getFluidState().isSource()) break block5;
            }
            return Direction.stream().map(origin::relative).anyMatch(pos -> level.getBlockState((BlockPos)pos).isCollisionShapeFullBlock(level, (BlockPos)pos));
        }
        return false;
    }
}

