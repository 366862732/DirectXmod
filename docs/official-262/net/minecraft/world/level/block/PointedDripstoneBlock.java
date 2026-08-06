/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PointedDripstoneBlock
extends SpeleothemBlock {
    public static final MapCodec<PointedDripstoneBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BlockState.CODEC.fieldOf("block_to_grow_on").forGetter(b -> b.blockToGrowOn), PointedDripstoneBlock.propertiesCodec()).apply((Applicative)i, PointedDripstoneBlock::new));
    private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02f;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12f;
    private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
    private static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125f;
    private static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375f;
    private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.5f;
    private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
    private static final double STALACTITE_DRIP_START_PIXEL = SHAPE_TIP_DOWN.min(Direction.Axis.Y);
    private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.column(4.0, 0.0, 16.0);

    public MapCodec<PointedDripstoneBlock> codec() {
        return CODEC;
    }

    public PointedDripstoneBlock(BlockState blockToGrowOn, BlockBehaviour.Properties properties) {
        super(blockToGrowOn, properties);
    }

    @Override
    protected int getStalactiteLandingSound() {
        return 1045;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == SpeleothemThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.5, 2.0f, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!PointedDripstoneBlock.isFreeHangingStalactite(state)) {
            return;
        }
        float randomValue = random.nextFloat();
        if (randomValue > 0.12f) {
            return;
        }
        PointedDripstoneBlock.getFluidAboveStalactite(level, pos, state).filter(fluidAbove -> randomValue < 0.02f || PointedDripstoneBlock.canFillCauldron(fluidAbove.fluid)).ifPresent(fluidAbove -> PointedDripstoneBlock.spawnDripParticle(level, pos, state, fluidAbove.fluid, fluidAbove.pos));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PointedDripstoneBlock.maybeTransferFluid(state, level, pos, random.nextFloat());
        super.randomTick(state, level, pos, random);
    }

    @VisibleForTesting
    public static void maybeTransferFluid(BlockState state, ServerLevel level, BlockPos pos, float randomValue) {
        float transferProbability;
        if (randomValue > 0.17578125f && randomValue > 0.05859375f) {
            return;
        }
        if (!PointedDripstoneBlock.isStalactiteStartPos(state, level, pos)) {
            return;
        }
        Optional<FluidInfo> fluidInfo = PointedDripstoneBlock.getFluidAboveStalactite(level, pos, state);
        if (fluidInfo.isEmpty()) {
            return;
        }
        Fluid fluid = fluidInfo.get().fluid;
        if (fluid == Fluids.WATER) {
            transferProbability = 0.17578125f;
        } else if (fluid == Fluids.LAVA) {
            transferProbability = 0.05859375f;
        } else {
            return;
        }
        if (randomValue >= transferProbability) {
            return;
        }
        BlockPos stalactiteTipPos = PointedDripstoneBlock.findTip(state, level, pos, 11, false);
        if (stalactiteTipPos == null) {
            return;
        }
        if (fluidInfo.get().sourceState.is(Blocks.MUD) && fluid == Fluids.WATER) {
            BlockState newState = Blocks.CLAY.defaultBlockState();
            level.setBlockAndUpdate(fluidInfo.get().pos, newState);
            Block.pushEntitiesUp(fluidInfo.get().sourceState, newState, level, fluidInfo.get().pos);
            level.gameEvent(GameEvent.BLOCK_CHANGE, fluidInfo.get().pos, GameEvent.Context.of(newState));
            level.levelEvent(1504, stalactiteTipPos, 0);
            return;
        }
        BlockPos cauldronPos = PointedDripstoneBlock.findFillableCauldronBelowStalactiteTip(level, stalactiteTipPos, fluid);
        if (cauldronPos == null) {
            return;
        }
        level.levelEvent(1504, stalactiteTipPos, 0);
        int fallDistance = stalactiteTipPos.getY() - cauldronPos.getY();
        int delay = 50 + fallDistance;
        BlockState cauldronState = level.getBlockState(cauldronPos);
        level.scheduleTick(cauldronPos, cauldronState.getBlock(), delay);
    }

    public static void spawnDripParticle(Level level, BlockPos stalactiteTipPos, BlockState stalactiteTipState) {
        PointedDripstoneBlock.getFluidAboveStalactite(level, stalactiteTipPos, stalactiteTipState).ifPresent(fluidAbove -> PointedDripstoneBlock.spawnDripParticle(level, stalactiteTipPos, stalactiteTipState, fluidAbove.fluid, fluidAbove.pos));
    }

    private static void spawnDripParticle(Level level, BlockPos stalactiteTipPos, BlockState stalactiteTipState, Fluid fluidAbove, BlockPos posAbove) {
        Vec3 offset = stalactiteTipState.getOffset(stalactiteTipPos);
        double PIXEL_SIZE = 0.0625;
        double x = (double)stalactiteTipPos.getX() + 0.5 + offset.x;
        double y = (double)stalactiteTipPos.getY() + STALACTITE_DRIP_START_PIXEL - 0.0625;
        double z = (double)stalactiteTipPos.getZ() + 0.5 + offset.z;
        ParticleOptions dripParticle = PointedDripstoneBlock.getDripParticle(level, fluidAbove, posAbove);
        level.addParticle(dripParticle, x, y, z, 0.0, 0.0, 0.0);
    }

    private static Optional<BlockPos> findRootBlock(Level level, BlockPos pos, BlockState dripStoneState, int maxSearchLength) {
        Direction tipDirection = (Direction)dripStoneState.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> pathPredicate = (pathPos, state) -> state.is(dripStoneState.getBlock()) && state.getValue(TIP_DIRECTION) == tipDirection;
        return PointedDripstoneBlock.findBlockVertical(level, pos, tipDirection.getOpposite().getAxisDirection(), pathPredicate, state -> !state.is(dripStoneState.getBlock()), maxSearchLength);
    }

    private static @Nullable BlockPos findFillableCauldronBelowStalactiteTip(Level level, BlockPos stalactiteTipPos, Fluid fluid) {
        Predicate<BlockState> cauldronPredicate = state -> state.getBlock() instanceof AbstractCauldronBlock && ((AbstractCauldronBlock)state.getBlock()).canReceiveStalactiteDrip(fluid);
        BiPredicate<BlockPos, BlockState> pathPredicate = (pos, state) -> PointedDripstoneBlock.canDripThrough(level, pos, state);
        return PointedDripstoneBlock.findBlockVertical(level, stalactiteTipPos, Direction.DOWN.getAxisDirection(), pathPredicate, cauldronPredicate, 11).orElse(null);
    }

    public static @Nullable BlockPos findStalactiteTipAboveCauldron(Level level, BlockPos cauldronPos) {
        BiPredicate<BlockPos, BlockState> pathPredicate = (pos, state) -> PointedDripstoneBlock.canDripThrough(level, pos, state);
        return PointedDripstoneBlock.findBlockVertical(level, cauldronPos, Direction.UP.getAxisDirection(), pathPredicate, SpeleothemBlock::isFreeHangingStalactite, 11).orElse(null);
    }

    public static Fluid getCauldronFillFluidType(ServerLevel level, BlockPos stalactitePos) {
        return PointedDripstoneBlock.getFluidAboveStalactite(level, stalactitePos, level.getBlockState(stalactitePos)).map(fluidSource -> fluidSource.fluid).filter(PointedDripstoneBlock::canFillCauldron).orElse(Fluids.EMPTY);
    }

    private static Optional<FluidInfo> getFluidAboveStalactite(Level level, BlockPos stalactitePos, BlockState stalactiteState) {
        if (!PointedDripstoneBlock.isStalactite(stalactiteState)) {
            return Optional.empty();
        }
        return PointedDripstoneBlock.findRootBlock(level, stalactitePos, stalactiteState, 11).map(rootPos -> {
            BlockPos abovePos = rootPos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            Fluid fluid = aboveState.is(Blocks.MUD) && level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, abovePos) == false ? Fluids.WATER : level.getFluidState(abovePos).getType();
            return new FluidInfo(abovePos, fluid, aboveState);
        });
    }

    private static boolean canFillCauldron(Fluid fluidAbove) {
        return fluidAbove == Fluids.LAVA || fluidAbove == Fluids.WATER;
    }

    @Override
    protected boolean canGrow(LevelReader level, BlockPos pos) {
        FluidState fluidState = level.getBlockState(pos.above(2)).getFluidState();
        return super.canGrow(level, pos) && fluidState.is(Fluids.WATER) && fluidState.isSource();
    }

    private static ParticleOptions getDripParticle(Level level, Fluid fluidAbove, BlockPos posAbove) {
        if (fluidAbove.isSame(Fluids.EMPTY)) {
            return level.environmentAttributes().getValue(EnvironmentAttributes.DEFAULT_DRIPSTONE_PARTICLE, posAbove);
        }
        return fluidAbove.is(FluidTags.LAVA) ? ParticleTypes.DRIPPING_DRIPSTONE_LAVA : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
    }

    @Override
    protected boolean blocksStalagmiteScan(LevelReader level, BlockPos pos, BlockState state) {
        return !PointedDripstoneBlock.canDripThrough(level, pos, state);
    }

    private static boolean canDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.isSolidRender()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, collisionShape, BooleanOp.AND);
    }

    private record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
    }
}

