/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class SpongeBlock
extends Block {
    public static final MapCodec<SpongeBlock> CODEC = SpongeBlock.simpleCodec(SpongeBlock::new);
    public static final int MAX_DEPTH = 6;
    public static final int MAX_COUNT = 64;
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    public MapCodec<SpongeBlock> codec() {
        return CODEC;
    }

    protected SpongeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        this.tryAbsorbWater(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        this.tryAbsorbWater(level, pos);
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    protected void tryAbsorbWater(Level level, BlockPos pos) {
        if (this.removeWaterBreadthFirstSearch(level, pos)) {
            level.setBlock(pos, Blocks.WET_SPONGE.defaultBlockState(), 2);
            level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private boolean removeWaterBreadthFirstSearch(Level level, BlockPos startPos) {
        return BlockPos.breadthFirstTraversal(startPos, 6, 65, (pos, consumer) -> {
            for (Direction direction : ALL_DIRECTIONS) {
                consumer.accept(pos.relative(direction));
            }
        }, pos -> {
            BucketPickup bucketPickup;
            if (pos.equals(startPos)) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
            BlockState state = level.getBlockState((BlockPos)pos);
            FluidState fluidState = level.getFluidState((BlockPos)pos);
            if (!fluidState.is(FluidTags.WATER)) {
                return BlockPos.TraversalNodeStatus.SKIP;
            }
            Block patt0$temp = state.getBlock();
            if (patt0$temp instanceof BucketPickup && !(bucketPickup = (BucketPickup)((Object)patt0$temp)).pickupBlock(null, level, (BlockPos)pos, state).isEmpty()) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
            if (state.getBlock() instanceof LiquidBlock) {
                level.setBlock((BlockPos)pos, Blocks.AIR.defaultBlockState(), 3);
            } else if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)) {
                BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity((BlockPos)pos) : null;
                SpongeBlock.dropResources(state, level, pos, blockEntity);
                level.setBlock((BlockPos)pos, Blocks.AIR.defaultBlockState(), 3);
            } else {
                return BlockPos.TraversalNodeStatus.SKIP;
            }
            return BlockPos.TraversalNodeStatus.ACCEPT;
        }) > 1;
    }
}

