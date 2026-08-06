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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class EndRodBlock
extends RodBlock {
    public static final MapCodec<EndRodBlock> CODEC = EndRodBlock.simpleCodec(EndRodBlock::new);

    public MapCodec<EndRodBlock> codec() {
        return CODEC;
    }

    protected EndRodBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos().relative(clickedFace.getOpposite()));
        if (blockState.is(this) && blockState.getValue(FACING) == clickedFace) {
            return (BlockState)this.defaultBlockState().setValue(FACING, clickedFace.getOpposite());
        }
        return (BlockState)this.defaultBlockState().setValue(FACING, clickedFace);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction direction = (Direction)state.getValue(FACING);
        double x = (double)pos.getX() + 0.55 - (double)(random.nextFloat() * 0.1f);
        double y = (double)pos.getY() + 0.55 - (double)(random.nextFloat() * 0.1f);
        double z = (double)pos.getZ() + 0.55 - (double)(random.nextFloat() * 0.1f);
        double r = 0.4f - (random.nextFloat() + random.nextFloat()) * 0.4f;
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x + (double)direction.getStepX() * r, y + (double)direction.getStepY() * r, z + (double)direction.getStepZ() * r, random.nextGaussian() * 0.005, random.nextGaussian() * 0.005, random.nextGaussian() * 0.005);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}

