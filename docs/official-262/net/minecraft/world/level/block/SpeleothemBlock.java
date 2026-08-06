/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class SpeleothemBlock
extends Block
implements SimpleWaterloggedBlock,
Fallable {
    public static final EnumProperty<Direction> TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<SpeleothemThickness> THICKNESS = BlockStateProperties.SPELEOTHEM_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int DELAY_BEFORE_FALLING = 2;
    private static final double MIN_TRIDENT_VELOCITY_TO_BREAK_SPELEOTHEM = 0.6;
    private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0f;
    private static final int STALACTITE_MAX_DAMAGE = 40;
    private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
    private static final VoxelShape SHAPE_TIP_MERGE = Block.column(6.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_TIP_UP = Block.column(6.0, 0.0, 11.0);
    protected static final VoxelShape SHAPE_TIP_DOWN = Block.column(6.0, 5.0, 16.0);
    private static final VoxelShape SHAPE_FRUSTUM = Block.column(8.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_MIDDLE = Block.column(10.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_BASE = Block.column(12.0, 0.0, 16.0);
    private static final float MAX_HORIZONTAL_OFFSET = (float)SHAPE_BASE.min(Direction.Axis.X);
    private static final float AVERAGE_DAYS_PER_GROWTH = 5.0f;
    private static final float GROWTH_PROBABILITY_PER_RANDOM_TICK = 0.011377778f;
    private static final int MAX_GROWTH_LENGTH = 7;
    private static final int MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING = 10;
    protected final BlockState blockToGrowOn;

    public abstract MapCodec<? extends SpeleothemBlock> codec();

    public SpeleothemBlock(BlockState blockToGrowOn, BlockBehaviour.Properties properties) {
        super(properties);
        this.blockToGrowOn = blockToGrowOn;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(TIP_DIRECTION, Direction.UP)).setValue(THICKNESS, SpeleothemThickness.TIP)).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return this.isValidSpeleothemPlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (directionToNeighbour != Direction.UP && directionToNeighbour != Direction.DOWN) {
            return state;
        }
        Direction tipDirection = state.getValue(TIP_DIRECTION);
        if (tipDirection == Direction.DOWN && ticks.getBlockTicks().hasScheduledTick(pos, this)) {
            return state;
        }
        if (directionToNeighbour == tipDirection.getOpposite() && !this.canSurvive(state, level, pos)) {
            if (tipDirection == Direction.DOWN) {
                ticks.scheduleTick(pos, this, 2);
            } else {
                ticks.scheduleTick(pos, this, 1);
            }
            return state;
        }
        boolean mergeOpposingTips = state.getValue(THICKNESS) == SpeleothemThickness.TIP_MERGE;
        SpeleothemThickness newThickness = this.calculateSpeleothemThickness(level, pos, tipDirection, mergeOpposingTips);
        return (BlockState)state.setValue(THICKNESS, newThickness);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction defaultTipDirection;
        BlockPos pos;
        Level level = context.getLevel();
        Direction tipDirection = this.calculateTipDirection(level, pos = context.getClickedPos(), defaultTipDirection = context.getNearestLookingVerticalDirection().getOpposite());
        if (tipDirection == null) {
            return null;
        }
        boolean mergeOpposingTips = !context.isSecondaryUseActive();
        SpeleothemThickness thickness = this.calculateSpeleothemThickness(level, pos, tipDirection, mergeOpposingTips);
        return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(TIP_DIRECTION, tipDirection)).setValue(THICKNESS, thickness)).setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
    }

    private @Nullable Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction defaultTipDirection) {
        Direction tipDirection;
        if (this.isValidSpeleothemPlacement(level, pos, defaultTipDirection)) {
            tipDirection = defaultTipDirection;
        } else if (this.isValidSpeleothemPlacement(level, pos, defaultTipDirection.getOpposite())) {
            tipDirection = defaultTipDirection.getOpposite();
        } else {
            return null;
        }
        return tipDirection;
    }

    private SpeleothemThickness calculateSpeleothemThickness(LevelReader level, BlockPos pos, Direction tipDirection, boolean mergeOpposingTips) {
        Direction baseDirection = tipDirection.getOpposite();
        BlockState inFrontState = level.getBlockState(pos.relative(tipDirection));
        if (SpeleothemBlock.isSpeleothemWithDirection(inFrontState, baseDirection) && inFrontState.is(this)) {
            if (mergeOpposingTips || inFrontState.getValue(THICKNESS) == SpeleothemThickness.TIP_MERGE) {
                return SpeleothemThickness.TIP_MERGE;
            }
            return SpeleothemThickness.TIP;
        }
        if (!SpeleothemBlock.isSpeleothemWithDirection(inFrontState, tipDirection)) {
            return SpeleothemThickness.TIP;
        }
        SpeleothemThickness inFrontThickness = inFrontState.getValue(THICKNESS);
        if (inFrontThickness == SpeleothemThickness.TIP || inFrontThickness == SpeleothemThickness.TIP_MERGE) {
            return SpeleothemThickness.FRUSTUM;
        }
        BlockState behindState = level.getBlockState(pos.relative(baseDirection));
        if (!SpeleothemBlock.isSpeleothemWithDirection(behindState, tipDirection)) {
            return SpeleothemThickness.BASE;
        }
        return SpeleothemThickness.MIDDLE;
    }

    private boolean isValidSpeleothemPlacement(LevelReader level, BlockPos pos, Direction tipDirection) {
        BlockPos behindPos = pos.relative(tipDirection.getOpposite());
        BlockState behindState = level.getBlockState(behindPos);
        return behindState.isFaceSturdy(level, behindPos, tipDirection) || SpeleothemBlock.isSpeleothemWithDirection(behindState, tipDirection) && behindState.is(this);
    }

    private static boolean isSpeleothemWithDirection(BlockState blockState, Direction tipDirection) {
        return blockState.is(BlockTags.SPELEOTHEMS) && blockState.getValue(TIP_DIRECTION) == tipDirection;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile projectile) {
        ServerLevel serverLevel;
        if (level.isClientSide()) {
            return;
        }
        BlockPos blockPos = blockHit.getBlockPos();
        if (level instanceof ServerLevel && projectile.mayInteract(serverLevel = (ServerLevel)level, blockPos) && projectile.mayBreak(serverLevel) && projectile instanceof ThrownTrident && projectile.getDeltaMovement().length() > 0.6) {
            level.destroyBlock(blockPos, true);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (SpeleothemBlock.isStalagmite(state) && !this.canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else {
            SpeleothemBlock.spawnFallingStalactite(state, level, pos);
        }
    }

    private static void spawnFallingStalactite(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos fallPos = pos.mutable();
        BlockState fallState = state;
        while (SpeleothemBlock.isStalactite(fallState)) {
            FallingBlockEntity entity = FallingBlockEntity.fall(level, fallPos, fallState);
            if (SpeleothemBlock.isTip(fallState, true)) {
                int size = Math.max(1 + pos.getY() - fallPos.getY(), 6);
                float damagePerFallDistance = 1.0f * (float)size;
                entity.setHurtsEntities(damagePerFallDistance, 40);
                break;
            }
            fallPos.move(Direction.DOWN);
            fallState = level.getBlockState(fallPos);
        }
    }

    private static boolean isStalagmite(BlockState state) {
        return SpeleothemBlock.isSpeleothemWithDirection(state, Direction.UP);
    }

    protected static boolean isStalactite(BlockState state) {
        return SpeleothemBlock.isSpeleothemWithDirection(state, Direction.DOWN);
    }

    private static boolean isTip(BlockState state, boolean includeMergedTip) {
        if (!state.is(BlockTags.SPELEOTHEMS)) {
            return false;
        }
        SpeleothemThickness thickness = state.getValue(THICKNESS);
        return thickness == SpeleothemThickness.TIP || includeMergedTip && thickness == SpeleothemThickness.TIP_MERGE;
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        if (!entity.isSilent()) {
            level.levelEvent(this.getStalactiteLandingSound(), pos, 0);
        }
    }

    protected abstract int getStalactiteLandingSound();

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(THICKNESS)) {
            default -> throw new MatchException(null, null);
            case SpeleothemThickness.TIP_MERGE -> SHAPE_TIP_MERGE;
            case SpeleothemThickness.TIP -> {
                if (state.getValue(TIP_DIRECTION) == Direction.DOWN) {
                    yield SHAPE_TIP_DOWN;
                }
                yield SHAPE_TIP_UP;
            }
            case SpeleothemThickness.FRUSTUM -> SHAPE_FRUSTUM;
            case SpeleothemThickness.MIDDLE -> SHAPE_MIDDLE;
            case SpeleothemThickness.BASE -> SHAPE_BASE;
        };
        return shape.move(state.getOffset(pos));
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return MAX_HORIZONTAL_OFFSET;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.011377778f && SpeleothemBlock.isStalactiteStartPos(state, level, pos)) {
            this.growStalactiteOrStalagmiteIfPossible(state, level, pos, random);
        }
    }

    protected static boolean isStalactiteStartPos(BlockState state, LevelReader level, BlockPos pos) {
        return SpeleothemBlock.isStalactite(state) && !level.getBlockState(pos.above()).is(state.getBlock());
    }

    @VisibleForTesting
    public void growStalactiteOrStalagmiteIfPossible(BlockState stalactiteStartState, ServerLevel level, BlockPos stalactiteStartPos, RandomSource random) {
        if (!this.canGrow(level, stalactiteStartPos)) {
            return;
        }
        BlockPos stalactiteTipPos = SpeleothemBlock.findTip(stalactiteStartState, level, stalactiteStartPos, this.getMaxGrowthLength(), false);
        if (stalactiteTipPos == null) {
            return;
        }
        BlockState stalactiteTipState = level.getBlockState(stalactiteTipPos);
        if (!SpeleothemBlock.isFreeHangingStalactite(stalactiteTipState) || !this.canTipGrow(stalactiteTipState, level, stalactiteTipPos)) {
            return;
        }
        if (random.nextBoolean()) {
            this.grow(level, stalactiteTipPos, Direction.DOWN);
        } else {
            this.growStalagmiteBelow(level, stalactiteTipPos);
        }
    }

    protected boolean canGrow(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(this.blockToGrowOn.getBlock());
    }

    protected static @Nullable BlockPos findTip(BlockState speleothemState, LevelAccessor level, BlockPos speleothemPos, int maxSearchLength, boolean includeMergedTip) {
        if (SpeleothemBlock.isTip(speleothemState, includeMergedTip)) {
            return speleothemPos;
        }
        Direction searchDirection = speleothemState.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> pathPredicate = (pos, state) -> state.is(speleothemState.getBlock()) && state.getValue(TIP_DIRECTION) == searchDirection;
        return SpeleothemBlock.findBlockVertical(level, speleothemPos, searchDirection.getAxisDirection(), pathPredicate, speleothem -> SpeleothemBlock.isTip(speleothem, includeMergedTip), maxSearchLength).orElse(null);
    }

    protected static Optional<BlockPos> findBlockVertical(LevelAccessor level, BlockPos pos, Direction.AxisDirection axisDirection, BiPredicate<BlockPos, BlockState> pathPredicate, Predicate<BlockState> targetPredicate, int maxSteps) {
        Direction direction = Direction.get(axisDirection, Direction.Axis.Y);
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        for (int i = 1; i < maxSteps; ++i) {
            mutablePos.move(direction);
            BlockState state = level.getBlockState(mutablePos);
            if (targetPredicate.test(state)) {
                return Optional.of(mutablePos.immutable());
            }
            if (!level.isOutsideBuildHeight(mutablePos.getY()) && pathPredicate.test(mutablePos, state)) continue;
            return Optional.empty();
        }
        return Optional.empty();
    }

    private boolean canTipGrow(BlockState tipState, ServerLevel level, BlockPos tipPos) {
        Direction growDirection = tipState.getValue(TIP_DIRECTION);
        BlockPos growPos = tipPos.relative(growDirection);
        BlockState stateAtGrowPos = level.getBlockState(growPos);
        if (!stateAtGrowPos.getFluidState().isEmpty()) {
            return false;
        }
        if (stateAtGrowPos.isAir()) {
            return true;
        }
        return this.isUnmergedTipWithDirection(stateAtGrowPos, growDirection.getOpposite());
    }

    private boolean isUnmergedTipWithDirection(BlockState state, Direction tipDirection) {
        return SpeleothemBlock.isTip(state, false) && state.getValue(TIP_DIRECTION) == tipDirection && state.is(this);
    }

    private void grow(ServerLevel level, BlockPos growFromPos, Direction growToDirection) {
        BlockPos targetPos = growFromPos.relative(growToDirection);
        BlockState existingStateAtTargetPos = level.getBlockState(targetPos);
        if (this.isUnmergedTipWithDirection(existingStateAtTargetPos, growToDirection.getOpposite())) {
            this.createMergedTips(existingStateAtTargetPos, level, targetPos);
        } else if (existingStateAtTargetPos.isAir() || existingStateAtTargetPos.is(Blocks.WATER)) {
            this.createSpeleothem(level, targetPos, growToDirection, SpeleothemThickness.TIP);
        }
    }

    private void createSpeleothem(LevelAccessor level, BlockPos pos, Direction direction, SpeleothemThickness thickness) {
        BlockState state = (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(TIP_DIRECTION, direction)).setValue(THICKNESS, thickness)).setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
        level.setBlock(pos, state, 3);
    }

    private void createMergedTips(BlockState tipState, LevelAccessor level, BlockPos tipPos) {
        BlockPos stalactitePos;
        BlockPos stalagmitePos;
        if (tipState.getValue(TIP_DIRECTION) == Direction.UP) {
            stalagmitePos = tipPos;
            stalactitePos = tipPos.above();
        } else {
            stalactitePos = tipPos;
            stalagmitePos = tipPos.below();
        }
        this.createSpeleothem(level, stalactitePos, Direction.DOWN, SpeleothemThickness.TIP_MERGE);
        this.createSpeleothem(level, stalagmitePos, Direction.UP, SpeleothemThickness.TIP_MERGE);
    }

    private void growStalagmiteBelow(ServerLevel level, BlockPos posAboveStalagmite) {
        BlockPos.MutableBlockPos pos = posAboveStalagmite.mutable();
        for (int i = 0; i < 10; ++i) {
            pos.move(Direction.DOWN);
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) {
                return;
            }
            if (this.isUnmergedTipWithDirection(state, Direction.UP) && this.canTipGrow(state, level, pos)) {
                this.grow(level, pos, Direction.UP);
                return;
            }
            if (this.isValidSpeleothemPlacement(level, pos, Direction.UP) && !level.isWaterAt((BlockPos)pos.below())) {
                this.grow(level, (BlockPos)pos.below(), Direction.UP);
                return;
            }
            if (!this.blocksStalagmiteScan(level, pos, state)) continue;
            return;
        }
    }

    protected boolean blocksStalagmiteScan(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    protected static boolean isFreeHangingStalactite(BlockState state) {
        return SpeleothemBlock.isStalactite(state) && state.getValue(THICKNESS) == SpeleothemThickness.TIP && state.getValue(WATERLOGGED) == false;
    }

    protected int getMaxGrowthLength() {
        return 7;
    }
}

