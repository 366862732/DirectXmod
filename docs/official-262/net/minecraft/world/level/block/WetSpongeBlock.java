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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WetSpongeBlock
extends Block {
    public static final MapCodec<WetSpongeBlock> CODEC = WetSpongeBlock.simpleCodec(WetSpongeBlock::new);

    public MapCodec<WetSpongeBlock> codec() {
        return CODEC;
    }

    protected WetSpongeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos).booleanValue()) {
            level.setBlock(pos, Blocks.SPONGE.defaultBlockState(), 3);
            level.levelEvent(2009, pos, 0);
            level.playSound(null, pos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0f, (1.0f + level.getRandom().nextFloat() * 0.2f) * 0.7f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.getRandom(random);
        if (direction == Direction.UP) {
            return;
        }
        BlockPos relativePos = pos.relative(direction);
        BlockState blockState = level.getBlockState(relativePos);
        if (state.canOcclude() && blockState.isFaceSturdy(level, relativePos, direction.getOpposite())) {
            return;
        }
        double xx = pos.getX();
        double yy = pos.getY();
        double zz = pos.getZ();
        if (direction == Direction.DOWN) {
            yy -= 0.05;
            xx += random.nextDouble();
            zz += random.nextDouble();
        } else {
            yy += random.nextDouble() * 0.8;
            if (direction.getAxis() == Direction.Axis.X) {
                zz += random.nextDouble();
                xx = direction == Direction.EAST ? (xx += 1.1) : (xx += 0.05);
            } else {
                xx += random.nextDouble();
                zz = direction == Direction.SOUTH ? (zz += 1.1) : (zz += 0.05);
            }
        }
        level.addParticle(ParticleTypes.DRIPPING_WATER, xx, yy, zz, 0.0, 0.0, 0.0);
    }
}

