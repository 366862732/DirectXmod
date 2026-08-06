/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.apache.commons.lang3.ArrayUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

public class BedBlock
extends HorizontalDirectionalBlock {
    public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), BedBlock.propertiesCodec()).apply((Applicative)i, BedBlock::new));
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final Map<Direction, VoxelShape> SHAPES = Util.make(() -> {
        VoxelShape northWestLeg = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
        VoxelShape northEastLeg = Shapes.rotate(northWestLeg, OctahedralGroup.BLOCK_ROT_Y_90);
        return Shapes.rotateHorizontal(Shapes.or(Block.column(16.0, 3.0, 9.0), northWestLeg, northEastLeg));
    });
    private final DyeColor color;

    public MapCodec<BedBlock> codec() {
        return CODEC;
    }

    public BedBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PART, BedPart.FOOT)).setValue(OCCUPIED, false));
    }

    public static @Nullable Direction getBedOrientation(BlockGetter level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return blockState.getBlock() instanceof BedBlock ? (Direction)blockState.getValue(FACING) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (state.getValue(PART) != BedPart.HEAD && !(state = level.getBlockState(pos = pos.relative((Direction)state.getValue(FACING)))).is(this)) {
            return InteractionResult.CONSUME;
        }
        BedRule bedRule = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
        if (bedRule.explodes()) {
            bedRule.errorMessage().ifPresent(player::sendOverlayMessage);
            level.removeBlock(pos, false);
            BlockPos blockPos = pos.relative(((Direction)state.getValue(FACING)).getOpposite());
            if (level.getBlockState(blockPos).is(this)) {
                level.removeBlock(blockPos, false);
            }
            Vec3 boomPos = Vec3.atCenterOf(pos);
            level.explode(null, level.damageSources().badRespawnPointExplosion(boomPos), null, boomPos, 5.0f, true, Level.ExplosionInteraction.BLOCK);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (state.getValue(OCCUPIED).booleanValue()) {
            if (!this.kickVillagerOutOfBed(level, pos)) {
                player.sendOverlayMessage(Component.translatable("block.minecraft.bed.occupied"));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        player.startSleepInBed(pos).ifLeft(problem -> {
            if (problem.message() != null) {
                player.sendOverlayMessage(problem.message());
            }
        });
        return InteractionResult.SUCCESS_SERVER;
    }

    private boolean kickVillagerOutOfBed(Level level, BlockPos pos) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, new AABB(pos), LivingEntity::isSleeping);
        if (villagers.isEmpty()) {
            return false;
        }
        villagers.get(0).stopSleeping();
        return true;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == BedBlock.getNeighbourDirection(state.getValue(PART), (Direction)state.getValue(FACING))) {
            if (neighbourState.is(this) && neighbourState.getValue(PART) != state.getValue(PART)) {
                return (BlockState)state.setValue(OCCUPIED, neighbourState.getValue(OCCUPIED));
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static Direction getNeighbourDirection(BedPart part, Direction facing) {
        return part == BedPart.FOOT ? facing : facing.getOpposite();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos headPos;
        BlockState headState;
        BedPart part;
        if (!level.isClientSide() && player.preventsBlockDrops() && (part = state.getValue(PART)) == BedPart.FOOT && (headState = level.getBlockState(headPos = pos.relative(BedBlock.getNeighbourDirection(part, (Direction)state.getValue(FACING))))).is(this) && headState.getValue(PART) == BedPart.HEAD) {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, headPos, Block.getId(headState));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();
        BlockPos relative = pos.relative(facing);
        Level level = context.getLevel();
        if (level.getBlockState(relative).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(relative)) {
            return (BlockState)this.defaultBlockState().setValue(FACING, facing);
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(BedBlock.getConnectedDirection(state).getOpposite());
    }

    public static Direction getConnectedDirection(BlockState state) {
        Direction facing = (Direction)state.getValue(FACING);
        return state.getValue(PART) == BedPart.HEAD ? facing.getOpposite() : facing;
    }

    public static DoubleBlockCombiner.BlockType getBlockType(BlockState state) {
        BedPart part = state.getValue(PART);
        if (part == BedPart.HEAD) {
            return DoubleBlockCombiner.BlockType.FIRST;
        }
        return DoubleBlockCombiner.BlockType.SECOND;
    }

    private static boolean isBunkBed(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof BedBlock;
    }

    public static Optional<Vec3> findStandUpPosition(EntityType<?> type, CollisionGetter level, BlockPos pos, Direction forward, float yaw) {
        Direction side;
        Direction right = forward.getClockWise();
        Direction direction = side = right.isFacingAngle(yaw) ? right.getOpposite() : right;
        if (BedBlock.isBunkBed(level, pos)) {
            return BedBlock.findBunkBedStandUpPosition(type, level, pos, forward, side);
        }
        int[][] offsets = BedBlock.bedStandUpOffsets(forward, side);
        Optional<Vec3> safePosition = BedBlock.findStandUpPositionAtOffset(type, level, pos, offsets, true);
        if (safePosition.isPresent()) {
            return safePosition;
        }
        return BedBlock.findStandUpPositionAtOffset(type, level, pos, offsets, false);
    }

    private static Optional<Vec3> findBunkBedStandUpPosition(EntityType<?> type, CollisionGetter level, BlockPos pos, Direction forward, Direction side) {
        int[][] offsets = BedBlock.bedSurroundStandUpOffsets(forward, side);
        Optional<Vec3> safePosition = BedBlock.findStandUpPositionAtOffset(type, level, pos, offsets, true);
        if (safePosition.isPresent()) {
            return safePosition;
        }
        BlockPos below = pos.below();
        Optional<Vec3> belowSafePosition = BedBlock.findStandUpPositionAtOffset(type, level, below, offsets, true);
        if (belowSafePosition.isPresent()) {
            return belowSafePosition;
        }
        int[][] aboveOffsets = BedBlock.bedAboveStandUpOffsets(forward);
        Optional<Vec3> aboveSafePosition = BedBlock.findStandUpPositionAtOffset(type, level, pos, aboveOffsets, true);
        if (aboveSafePosition.isPresent()) {
            return aboveSafePosition;
        }
        Optional<Vec3> unsafePosition = BedBlock.findStandUpPositionAtOffset(type, level, pos, offsets, false);
        if (unsafePosition.isPresent()) {
            return unsafePosition;
        }
        Optional<Vec3> belowUnsafePosition = BedBlock.findStandUpPositionAtOffset(type, level, below, offsets, false);
        if (belowUnsafePosition.isPresent()) {
            return belowUnsafePosition;
        }
        return BedBlock.findStandUpPositionAtOffset(type, level, pos, aboveOffsets, false);
    }

    private static Optional<Vec3> findStandUpPositionAtOffset(EntityType<?> type, CollisionGetter level, BlockPos pos, int[][] offsets, boolean checkDangerous) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int[] offset : offsets) {
            blockPos.set(pos.getX() + offset[0], pos.getY(), pos.getZ() + offset[1]);
            Vec3 position = DismountHelper.findSafeDismountLocation(type, level, blockPos, checkDangerous);
            if (position == null) continue;
            return Optional.of(position);
        }
        return Optional.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        BlockPos otherPos = pos.relative((Direction)state.getValue(FACING));
        level.setBlockAndUpdate(otherPos, (BlockState)state.setValue(PART, BedPart.HEAD));
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        BlockPos sourcePos = pos.relative((Direction)state.getValue(FACING), state.getValue(PART) == BedPart.HEAD ? 0 : 1);
        return Mth.getSeed(sourcePos.getX(), pos.getY(), sourcePos.getZ());
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    private static int[][] bedStandUpOffsets(Direction forward, Direction side) {
        return (int[][])ArrayUtils.addAll((Object[])BedBlock.bedSurroundStandUpOffsets(forward, side), (Object[])BedBlock.bedAboveStandUpOffsets(forward));
    }

    private static int[][] bedSurroundStandUpOffsets(Direction forward, Direction side) {
        return new int[][]{{side.getStepX(), side.getStepZ()}, {side.getStepX() - forward.getStepX(), side.getStepZ() - forward.getStepZ()}, {side.getStepX() - forward.getStepX() * 2, side.getStepZ() - forward.getStepZ() * 2}, {-forward.getStepX() * 2, -forward.getStepZ() * 2}, {-side.getStepX() - forward.getStepX() * 2, -side.getStepZ() - forward.getStepZ() * 2}, {-side.getStepX() - forward.getStepX(), -side.getStepZ() - forward.getStepZ()}, {-side.getStepX(), -side.getStepZ()}, {-side.getStepX() + forward.getStepX(), -side.getStepZ() + forward.getStepZ()}, {forward.getStepX(), forward.getStepZ()}, {side.getStepX() + forward.getStepX(), side.getStepZ() + forward.getStepZ()}};
    }

    private static int[][] bedAboveStandUpOffsets(Direction forward) {
        return new int[][]{{0, 0}, {-forward.getStepX(), -forward.getStepZ()}};
    }
}

