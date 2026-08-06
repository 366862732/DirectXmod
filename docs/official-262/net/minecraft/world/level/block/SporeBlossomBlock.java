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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SporeBlossomBlock
extends Block {
    public static final MapCodec<SporeBlossomBlock> CODEC = SporeBlossomBlock.simpleCodec(SporeBlossomBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 13.0, 16.0);
    private static final int ADD_PARTICLE_ATTEMPTS = 14;
    private static final int PARTICLE_XZ_RADIUS = 10;
    private static final int PARTICLE_Y_MAX = 10;

    public MapCodec<SporeBlossomBlock> codec() {
        return CODEC;
    }

    public SporeBlossomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN) && !level.isWaterAt(pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.UP && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int plantX = pos.getX();
        int plantY = pos.getY();
        int plantZ = pos.getZ();
        double xFalling = (double)plantX + random.nextDouble();
        double yFalling = (double)plantY + 0.7;
        double zFalling = (double)plantZ + random.nextDouble();
        level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, xFalling, yFalling, zFalling, 0.0, 0.0, 0.0);
        BlockPos.MutableBlockPos ambientPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 14; ++i) {
            ambientPos.set(plantX + Mth.nextInt(random, -10, 10), plantY - random.nextInt(10), plantZ + Mth.nextInt(random, -10, 10));
            BlockState particlePosState = level.getBlockState(ambientPos);
            if (particlePosState.isCollisionShapeFullBlock(level, ambientPos)) continue;
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, (double)ambientPos.getX() + random.nextDouble(), (double)ambientPos.getY() + random.nextDouble(), (double)ambientPos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

