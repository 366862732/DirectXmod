/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.Object2IntArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BigDripleafBlock
extends HorizontalDirectionalBlock
implements SimpleWaterloggedBlock,
BonemealableBlock {
    public static final MapCodec<BigDripleafBlock> CODEC = BigDripleafBlock.simpleCodec(BigDripleafBlock::new);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final EnumProperty<Tilt> TILT = BlockStateProperties.TILT;
    private static final int NO_TICK = -1;
    private static final Object2IntMap<Tilt> DELAY_UNTIL_NEXT_TILT_STATE = (Object2IntMap)Util.make(new Object2IntArrayMap(), map -> {
        map.defaultReturnValue(-1);
        map.put((Object)Tilt.UNSTABLE, 10);
        map.put((Object)Tilt.PARTIAL, 10);
        map.put((Object)Tilt.FULL, 100);
    });
    private static final int MAX_GEN_HEIGHT = 5;
    private static final int ENTITY_DETECTION_MIN_Y = 11;
    private static final int LOWEST_LEAF_TOP = 13;
    private static final Map<Tilt, VoxelShape> SHAPE_LEAF = Maps.newEnumMap(Map.of(Tilt.NONE, Block.column(16.0, 11.0, 15.0), Tilt.UNSTABLE, Block.column(16.0, 11.0, 15.0), Tilt.PARTIAL, Block.column(16.0, 11.0, 13.0), Tilt.FULL, Shapes.empty()));
    private final Function<BlockState, VoxelShape> shapes;

    public MapCodec<BigDripleafBlock> codec() {
        return CODEC;
    }

    protected BigDripleafBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, false)).setValue(FACING, Direction.NORTH)).setValue(TILT, Tilt.NONE));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> stems = Shapes.rotateHorizontal(Block.column(6.0, 0.0, 13.0).move(0.0, 0.0, 0.25).optimize());
        return this.getShapeForEachState(state -> Shapes.or(SHAPE_LEAF.get(state.getValue(TILT)), (VoxelShape)stems.get(state.getValue(FACING))), WATERLOGGED);
    }

    public static void placeWithRandomHeight(LevelAccessor level, RandomSource random, BlockPos stemBottomPos, Direction facing) {
        int height;
        int desiredHeight = Mth.nextInt(random, 2, 5);
        BlockPos.MutableBlockPos pos = stemBottomPos.mutable();
        for (height = 0; height < desiredHeight && BigDripleafBlock.canPlaceAt(level, pos); ++height) {
            pos.move(Direction.UP);
        }
        int leafY = stemBottomPos.getY() + height - 1;
        pos.setY(stemBottomPos.getY());
        while (pos.getY() < leafY) {
            BigDripleafStemBlock.place(level, pos, level.getFluidState(pos), facing);
            pos.move(Direction.UP);
        }
        BigDripleafBlock.place(level, pos, level.getFluidState(pos), facing);
    }

    private static boolean canReplace(BlockState oldState) {
        return oldState.isAir() || oldState.is(Blocks.WATER) || oldState.is(Blocks.SMALL_DRIPLEAF);
    }

    protected static boolean canPlaceAt(LevelReader level, BlockPos pos) {
        return BigDripleafBlock.canGrowInto(level, pos);
    }

    protected static boolean canGrowInto(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return level.isInsideBuildHeight(pos) && BigDripleafBlock.canReplace(state);
    }

    protected static boolean place(LevelAccessor level, BlockPos pos, FluidState fluidState, Direction facing) {
        BlockState newState = (BlockState)((BlockState)Blocks.BIG_DRIPLEAF.defaultBlockState().setValue(WATERLOGGED, fluidState.isSourceOfType(Fluids.WATER))).setValue(FACING, facing);
        return level.setBlock(pos, newState, 3);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile projectile) {
        this.setTiltAndScheduleTick(state, level, blockHit.getBlockPos(), Tilt.FULL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return belowState.is(this) || belowState.is(Blocks.BIG_DRIPLEAF_STEM) || belowState.is(BlockTags.SUPPORTS_BIG_DRIPLEAF);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (directionToNeighbour == Direction.UP && neighbourState.is(this)) {
            return Blocks.BIG_DRIPLEAF_STEM.withPropertiesOf(state);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return BigDripleafBlock.canGrowInto(level, pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos abovePos = pos.above();
        if (BigDripleafBlock.canPlaceAt(level, abovePos)) {
            Direction facing = (Direction)state.getValue(FACING);
            BigDripleafStemBlock.place(level, pos, state.getFluidState(), facing);
            BigDripleafBlock.place(level, abovePos, level.getBlockState(abovePos).getFluidState(), facing);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) {
            return;
        }
        if (state.getValue(TILT) == Tilt.NONE && BigDripleafBlock.canEntityTilt(pos, entity) && !level.hasNeighborSignal(pos)) {
            this.setTiltAndScheduleTick(state, level, pos, Tilt.UNSTABLE, null);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasNeighborSignal(pos)) {
            BigDripleafBlock.resetTilt(state, level, pos);
            return;
        }
        Tilt tilt = state.getValue(TILT);
        if (tilt == Tilt.UNSTABLE) {
            this.setTiltAndScheduleTick(state, level, pos, Tilt.PARTIAL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
        } else if (tilt == Tilt.PARTIAL) {
            this.setTiltAndScheduleTick(state, level, pos, Tilt.FULL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
        } else if (tilt == Tilt.FULL) {
            BigDripleafBlock.resetTilt(state, level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            BigDripleafBlock.resetTilt(state, level, pos);
        }
    }

    private static void playTiltSound(Level level, BlockPos pos, SoundEvent tiltSound) {
        float pitch = Mth.randomBetween(level.getRandom(), 0.8f, 1.2f);
        level.playSound(null, pos, tiltSound, SoundSource.BLOCKS, 1.0f, pitch);
    }

    private static boolean canEntityTilt(BlockPos pos, Entity entity) {
        return entity.onGround() && entity.position().y > (double)((float)pos.getY() + 0.6875f);
    }

    private void setTiltAndScheduleTick(BlockState state, Level level, BlockPos pos, Tilt tilt, @Nullable SoundEvent sound) {
        int tickDelay;
        BigDripleafBlock.setTilt(state, level, pos, tilt);
        if (sound != null) {
            BigDripleafBlock.playTiltSound(level, pos, sound);
        }
        if ((tickDelay = DELAY_UNTIL_NEXT_TILT_STATE.getInt((Object)tilt)) != -1) {
            level.scheduleTick(pos, this, tickDelay);
        }
    }

    private static void resetTilt(BlockState state, Level level, BlockPos pos) {
        BigDripleafBlock.setTilt(state, level, pos, Tilt.NONE);
        if (state.getValue(TILT) != Tilt.NONE) {
            BigDripleafBlock.playTiltSound(level, pos, SoundEvents.BIG_DRIPLEAF_TILT_UP);
        }
    }

    private static void setTilt(BlockState state, Level level, BlockPos pos, Tilt tilt) {
        Tilt previousTilt = state.getValue(TILT);
        level.setBlock(pos, (BlockState)state.setValue(TILT, tilt), 2);
        if (tilt.causesVibration() && tilt != previousTilt) {
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_LEAF.get(state.getValue(TILT));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState belowState = context.getLevel().getBlockState(context.getClickedPos().below());
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean belowIsDripleafPart = belowState.is(Blocks.BIG_DRIPLEAF) || belowState.is(Blocks.BIG_DRIPLEAF_STEM);
        return (BlockState)((BlockState)this.defaultBlockState().setValue(WATERLOGGED, fluidState.isSourceOfType(Fluids.WATER))).setValue(FACING, belowIsDripleafPart ? (Direction)belowState.getValue(FACING) : context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING, TILT);
    }
}

