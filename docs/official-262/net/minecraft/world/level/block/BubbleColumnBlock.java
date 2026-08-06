/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BubbleColumnBlock
extends Block
implements BucketPickup {
    public static final MapCodec<BubbleColumnBlock> CODEC = BubbleColumnBlock.simpleCodec(BubbleColumnBlock::new);
    public static final BooleanProperty DRAG_DOWN = BlockStateProperties.DRAG;
    private static final int CHECK_PERIOD = 5;

    public MapCodec<BubbleColumnBlock> codec() {
        return CODEC;
    }

    public BubbleColumnBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(DRAG_DOWN, true));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (isPrecise) {
            boolean nothingAbove;
            BlockState stateAbove = level.getBlockState(pos.above());
            boolean bl = nothingAbove = stateAbove.getCollisionShape(level, pos).isEmpty() && stateAbove.getFluidState().isEmpty();
            if (nothingAbove) {
                entity.onAboveBubbleColumn(state.getValue(DRAG_DOWN), pos);
            } else {
                entity.onInsideBubbleColumn(state.getValue(DRAG_DOWN));
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BubbleColumnBlock.updateColumn(this, level, pos, state, level.getBlockState(pos.below()));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getSource(false);
    }

    public static void updateColumn(Block bubbleColumn, LevelAccessor level, BlockPos occupyAt, BlockState belowState) {
        BubbleColumnBlock.updateColumn(bubbleColumn, level, occupyAt, level.getBlockState(occupyAt), belowState);
    }

    public static void updateColumn(Block bubbleColumn, LevelAccessor level, BlockPos occupyAt, BlockState occupyState, BlockState belowState) {
        if (!BubbleColumnBlock.canOccupy(bubbleColumn, occupyState)) {
            return;
        }
        BlockState columnState = BubbleColumnBlock.getColumnState(bubbleColumn, belowState, occupyState);
        level.setBlock(occupyAt, columnState, 2);
        BlockPos.MutableBlockPos pos = occupyAt.mutable().move(Direction.UP);
        while (BubbleColumnBlock.canOccupy(bubbleColumn, level.getBlockState(pos))) {
            if (!level.setBlock(pos, columnState, 2)) {
                return;
            }
            pos.move(Direction.UP);
        }
    }

    private static boolean canOccupy(Block bubbleColumn, BlockState occupyState) {
        if (occupyState.is(bubbleColumn)) {
            return true;
        }
        FluidState occupyFluid = occupyState.getFluidState();
        return occupyFluid.is(FluidTags.BUBBLE_COLUMN_CAN_OCCUPY) && occupyState.getBlock() instanceof LiquidBlock && occupyFluid.isSource() && occupyFluid.getAmount() >= 8;
    }

    private static BlockState getColumnState(Block bubbleColumn, BlockState belowState, BlockState occupyState) {
        if (belowState.is(bubbleColumn)) {
            return belowState;
        }
        if (belowState.is(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)) {
            return (BlockState)bubbleColumn.defaultBlockState().setValue(DRAG_DOWN, false);
        }
        if (belowState.is(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN)) {
            return (BlockState)bubbleColumn.defaultBlockState().setValue(DRAG_DOWN, true);
        }
        if (occupyState.is(bubbleColumn)) {
            return Blocks.WATER.defaultBlockState();
        }
        return occupyState;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        if (state.getValue(DRAG_DOWN).booleanValue()) {
            level.addAlwaysVisibleParticle(ParticleTypes.CURRENT_DOWN, x + 0.5, y + 0.8, z, 0.0, 0.0, 0.0);
            if (random.nextInt(200) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.2f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.15f, false);
            }
        } else {
            level.addAlwaysVisibleParticle(ParticleTypes.BUBBLE_COLUMN_UP, x + 0.5, y, z + 0.5, 0.0, 0.04, 0.0);
            level.addAlwaysVisibleParticle(ParticleTypes.BUBBLE_COLUMN_UP, x + (double)random.nextFloat(), y + (double)random.nextFloat(), z + (double)random.nextFloat(), 0.0, 0.04, 0.0);
            if (random.nextInt(200) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.2f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.15f, false);
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (!state.canSurvive(level, pos) || directionToNeighbour == Direction.DOWN || directionToNeighbour == Direction.UP && !neighbourState.is(this) && BubbleColumnBlock.canOccupy(this, neighbourState)) {
            ticks.scheduleTick(pos, this, 5);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        return belowState.is(this) || belowState.is(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP) || belowState.is(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRAG_DOWN);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Fluids.WATER.getPickupSound();
    }
}

