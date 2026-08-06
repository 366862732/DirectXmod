/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CryingObsidianBlock
extends Block {
    public static final MapCodec<CryingObsidianBlock> CODEC = CryingObsidianBlock.simpleCodec(CryingObsidianBlock::new);

    public MapCodec<CryingObsidianBlock> codec() {
        return CODEC;
    }

    public CryingObsidianBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction dir = Direction.getRandom(random);
        if (dir == Direction.UP) {
            return;
        }
        BlockPos relativePos = pos.relative(dir);
        BlockState blockState = level.getBlockState(relativePos);
        if (state.canOcclude() && blockState.isFaceSturdy(level, relativePos, dir.getOpposite())) {
            return;
        }
        double xOffset = dir.getStepX() == 0 ? random.nextDouble() : 0.5 + (double)dir.getStepX() * 0.6;
        double yOffset = dir.getStepY() == 0 ? random.nextDouble() : 0.5 + (double)dir.getStepY() * 0.6;
        double zOffset = dir.getStepZ() == 0 ? random.nextDouble() : 0.5 + (double)dir.getStepZ() * 0.6;
        level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, (double)pos.getX() + xOffset, (double)pos.getY() + yOffset, (double)pos.getZ() + zOffset, 0.0, 0.0, 0.0);
    }
}

