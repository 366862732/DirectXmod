/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class LiquidBlock
extends Block
implements BucketPickup {
    private static final Codec<FlowingFluid> FLOWING_FLUID = BuiltInRegistries.FLUID.byNameCodec().comapFlatMap(fluid -> {
        DataResult dataResult;
        if (fluid instanceof FlowingFluid) {
            FlowingFluid flowing = (FlowingFluid)fluid;
            dataResult = DataResult.success((Object)flowing);
        } else {
            dataResult = DataResult.error(() -> "Not a flowing fluid: " + String.valueOf(fluid));
        }
        return dataResult;
    }, fluid -> fluid);
    public static final MapCodec<LiquidBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)FLOWING_FLUID.fieldOf("fluid").forGetter(b -> b.fluid), LiquidBlock.propertiesCodec()).apply((Applicative)i, LiquidBlock::new));
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    protected final FlowingFluid fluid;
    private final List<FluidState> stateCache;
    public static final ImmutableList<Direction> POSSIBLE_FLOW_DIRECTIONS = ImmutableList.of((Object)Direction.DOWN, (Object)Direction.SOUTH, (Object)Direction.NORTH, (Object)Direction.EAST, (Object)Direction.WEST);
    private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

    public MapCodec<LiquidBlock> codec() {
        return CODEC;
    }

    protected LiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(properties);
        this.fluid = fluid;
        this.stateCache = Lists.newArrayList();
        this.stateCache.add(fluid.getSource(false));
        for (int level = 1; level < 8; ++level) {
            this.stateCache.add(fluid.getFlowing(8 - level, false));
        }
        this.stateCache.add(fluid.getFlowing(8, true));
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(LEVEL, 0));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.alwaysCollideWithFluid()) {
            return Shapes.block();
        }
        if (state.getValue(LEVEL) != 0) {
            return Shapes.empty();
        }
        return this.ifMobIsColliding(context).map(LivingEntity::getLiquidCollisionShape).filter(liquidStableShape -> context.isAbove((VoxelShape)liquidStableShape, pos, true) && context.canStandOnFluid(level.getFluidState(pos.above()), state.getFluidState())).orElse(Shapes.empty());
    }

    private Optional<LivingEntity> ifMobIsColliding(CollisionContext context) {
        EntityCollisionContext entityCollisionContext;
        Entity entity;
        if (context instanceof EntityCollisionContext && (entity = (entityCollisionContext = (EntityCollisionContext)context).getEntity()) instanceof LivingEntity) {
            LivingEntity mob = (LivingEntity)entity;
            return Optional.of(mob);
        }
        return Optional.empty();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getFluidState().isRandomlyTicking();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        state.getFluidState().randomTick(level, pos, random);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return !this.fluid.is(FluidTags.LAVA);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        int level = state.getValue(LEVEL);
        return this.stateCache.get(Math.min(level, 8));
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
        return neighborState.getFluidState().getType().isSame(this.fluid);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (this.shouldSpreadLiquid(level, pos, state)) {
            level.scheduleTick(pos, state.getFluidState().getType(), this.fluid.getTickDelay(level));
        }
        if (LiquidBlock.shouldBubbleColumnOccupy(state)) {
            BlockState stateBelow = level.getBlockState(pos.below());
            this.tryScheduleBubbleBlockColumn(level, pos, stateBelow);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (LiquidBlock.shouldBubbleColumnOccupy(state)) {
            BlockState stateBelow = level.getBlockState(pos.below());
            BubbleColumnBlock.updateColumn(Blocks.BUBBLE_COLUMN, level, pos, stateBelow);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getFluidState().isSource() || neighbourState.getFluidState().isSource()) {
            ticks.scheduleTick(pos, state.getFluidState().getType(), this.fluid.getTickDelay(level));
        }
        if (directionToNeighbour == Direction.DOWN && LiquidBlock.shouldBubbleColumnOccupy(state)) {
            this.tryScheduleBubbleBlockColumn(ticks, pos, neighbourState);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static boolean shouldBubbleColumnOccupy(BlockState state) {
        return state.getFluidState().is(FluidTags.BUBBLE_COLUMN_CAN_OCCUPY) && state.getFluidState().isSource() && state.getFluidState().isFull();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (this.shouldSpreadLiquid(level, pos, state)) {
            level.scheduleTick(pos, state.getFluidState().getType(), this.fluid.getTickDelay(level));
        }
        if (LiquidBlock.shouldBubbleColumnOccupy(state)) {
            BlockState stateBelow = level.getBlockState(pos.below());
            this.tryScheduleBubbleBlockColumn(level, pos, stateBelow);
        }
    }

    private void tryScheduleBubbleBlockColumn(ScheduledTickAccess ticks, BlockPos pos, BlockState stateBelow) {
        if (stateBelow.is(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN) || stateBelow.is(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)) {
            ticks.scheduleTick(pos, this, 20);
        }
    }

    private boolean shouldSpreadLiquid(Level level, BlockPos pos, BlockState state) {
        if (this.fluid.is(FluidTags.LAVA)) {
            boolean isOverSoulSoil = level.getBlockState(pos.below()).is(Blocks.SOUL_SOIL);
            for (Direction direction : POSSIBLE_FLOW_DIRECTIONS) {
                BlockPos neighbourPos = pos.relative(direction.getOpposite());
                if (level.getFluidState(neighbourPos).is(FluidTags.WATER)) {
                    Block convertToBlock = level.getFluidState(pos).isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
                    level.setBlockAndUpdate(pos, convertToBlock.defaultBlockState());
                    this.fizz(level, pos);
                    return false;
                }
                if (!isOverSoulSoil || !level.getBlockState(neighbourPos).is(Blocks.BLUE_ICE)) continue;
                level.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
                this.fizz(level, pos);
                return false;
            }
        }
        return true;
    }

    private void fizz(LevelAccessor level, BlockPos pos) {
        level.levelEvent(1501, pos, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            return new ItemStack(this.fluid.getBucket());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return this.fluid.getPickupSound();
    }
}

