/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock
extends DropExperienceBlock
implements SculkBehaviour {
    public static final MapCodec<SculkBlock> CODEC = SculkBlock.simpleCodec(SculkBlock::new);

    public MapCodec<SculkBlock> codec() {
        return CODEC;
    }

    public SculkBlock(BlockBehaviour.Properties properties) {
        super(ConstantInt.of(1), properties);
    }

    @Override
    public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, LevelAccessor level, BlockPos originPos, RandomSource random, SculkSpreader spreader, boolean spreadVein) {
        int charge = cursor.getCharge();
        if (charge == 0 || random.nextInt(spreader.chargeDecayRate()) != 0) {
            return charge;
        }
        BlockPos chargePos = cursor.getPos();
        boolean isCloseToCatalyst = chargePos.closerThan(originPos, spreader.noGrowthRadius());
        if (isCloseToCatalyst || !SculkBlock.canPlaceGrowth(level, chargePos)) {
            if (random.nextInt(spreader.additionalDecayRate()) != 0) {
                return charge;
            }
            return charge - (isCloseToCatalyst ? 1 : SculkBlock.getDecayPenalty(spreader, chargePos, originPos, charge));
        }
        int xpPerGrowthSpawn = spreader.growthSpawnCost();
        if (random.nextInt(xpPerGrowthSpawn) < charge) {
            BlockPos growthPlacement = chargePos.above();
            BlockState growthState = this.getRandomGrowthState(level, growthPlacement, random, spreader.isWorldGeneration());
            level.setBlock(growthPlacement, growthState, 3);
            level.playSound(null, chargePos, growthState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return Math.max(0, charge - xpPerGrowthSpawn);
    }

    private static int getDecayPenalty(SculkSpreader spreader, BlockPos pos, BlockPos originPos, int charge) {
        int noGrowthRadius = spreader.noGrowthRadius();
        float outerDistanceSquared = Mth.square((float)Math.sqrt(pos.distSqr(originPos)) - (float)noGrowthRadius);
        int maxReachSquared = Mth.square(24 - noGrowthRadius);
        float distanceFactor = Math.min(1.0f, outerDistanceSquared / (float)maxReachSquared);
        return Math.max(1, (int)((float)charge * distanceFactor * 0.5f));
    }

    private BlockState getRandomGrowthState(LevelAccessor level, BlockPos pos, RandomSource random, boolean isWorldGen) {
        BlockState state = random.nextInt(11) == 0 ? (BlockState)Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, isWorldGen) : Blocks.SCULK_SENSOR.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && !level.getFluidState(pos).isEmpty()) {
            return (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return state;
    }

    private static boolean canPlaceGrowth(LevelAccessor level, BlockPos pos) {
        BlockState stateAbove = level.getBlockState(pos.above());
        if (!(stateAbove.isAir() || stateAbove.is(Blocks.WATER) && stateAbove.getFluidState().is(Fluids.WATER))) {
            return false;
        }
        int growthCount = 0;
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 2, 4))) {
            BlockState state = level.getBlockState(blockPos);
            if (state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER)) {
                ++growthCount;
            }
            if (growthCount <= 2) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean canChangeBlockStateOnSpread() {
        return false;
    }
}

