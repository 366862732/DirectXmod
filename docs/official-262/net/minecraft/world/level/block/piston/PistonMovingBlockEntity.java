/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.piston;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMath;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonMovingBlockEntity
extends BlockEntity {
    private static final int TICKS_TO_EXTEND = 2;
    private static final double PUSH_OFFSET = 0.01;
    public static final double TICK_MOVEMENT = 0.51;
    private static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    private static final float DEFAULT_PROGRESS = 0.0f;
    private static final boolean DEFAULT_EXTENDING = false;
    private static final boolean DEFAULT_SOURCE = false;
    private BlockState movedState = DEFAULT_BLOCK_STATE;
    private Direction direction;
    private boolean extending = false;
    private boolean isSourcePiston = false;
    private static final ThreadLocal<Direction> NOCLIP = ThreadLocal.withInitial(() -> null);
    private float progress = 0.0f;
    private float progressO = 0.0f;
    private long lastTicked;
    private int deathTicks;

    public PistonMovingBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.PISTON, worldPosition, blockState);
    }

    public PistonMovingBlockEntity(BlockPos worldPosition, BlockState blockState, BlockState movedState, Direction direction, boolean extending, boolean isSourcePiston) {
        this(worldPosition, blockState);
        this.movedState = movedState;
        this.direction = direction;
        this.extending = extending;
        this.isSourcePiston = isSourcePiston;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public boolean isExtending() {
        return this.extending;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isSourcePiston() {
        return this.isSourcePiston;
    }

    public float getProgress(float a) {
        if (a > 1.0f) {
            a = 1.0f;
        }
        return Mth.lerp(a, this.progressO, this.progress);
    }

    public float getXOff(float a) {
        return (float)this.direction.getStepX() * this.getExtendedProgress(this.getProgress(a));
    }

    public float getYOff(float a) {
        return (float)this.direction.getStepY() * this.getExtendedProgress(this.getProgress(a));
    }

    public float getZOff(float a) {
        return (float)this.direction.getStepZ() * this.getExtendedProgress(this.getProgress(a));
    }

    private float getExtendedProgress(float progress) {
        return this.extending ? progress - 1.0f : 1.0f - progress;
    }

    private BlockState getCollisionRelatedBlockState() {
        if (!this.isExtending() && this.isSourcePiston() && this.movedState.getBlock() instanceof PistonBaseBlock) {
            return (BlockState)((BlockState)((BlockState)Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.SHORT, this.progress > 0.25f)).setValue(PistonHeadBlock.TYPE, this.movedState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT)).setValue(PistonHeadBlock.FACING, (Direction)this.movedState.getValue(PistonBaseBlock.FACING));
        }
        return this.movedState;
    }

    private static void moveCollidedEntities(Level level, BlockPos pos, float newProgress, PistonMovingBlockEntity self) {
        Direction movement = self.getMovementDirection();
        double deltaProgress = newProgress - self.progress;
        VoxelShape shape = self.getCollisionRelatedBlockState().getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return;
        }
        AABB aabb = PistonMovingBlockEntity.moveByPositionAndProgress(pos, shape.bounds(), self);
        List<Entity> entities = level.getEntities(null, PistonMath.getMovementArea(aabb, movement, deltaProgress).minmax(aabb));
        if (entities.isEmpty()) {
            return;
        }
        List<AABB> shapeAabbs = shape.toAabbs();
        boolean causeBounce = self.movedState.is(Blocks.SLIME_BLOCK);
        for (Entity entity : entities) {
            AABB entityAabb;
            AABB shapeAabb;
            AABB movingAABB;
            if (entity.getPistonPushReaction() == PushReaction.IGNORE) continue;
            if (causeBounce) {
                if (entity instanceof ServerPlayer) continue;
                Vec3 deltaMovement = entity.getDeltaMovement();
                double dx = deltaMovement.x;
                double dy = deltaMovement.y;
                double dz = deltaMovement.z;
                switch (movement.getAxis()) {
                    case X: {
                        dx = movement.getStepX();
                        break;
                    }
                    case Y: {
                        dy = movement.getStepY();
                        break;
                    }
                    case Z: {
                        dz = movement.getStepZ();
                    }
                }
                entity.setDeltaMovement(dx, dy, dz);
            }
            double delta = 0.0;
            Iterator<AABB> iterator = shapeAabbs.iterator();
            while (!(!iterator.hasNext() || (movingAABB = PistonMath.getMovementArea(PistonMovingBlockEntity.moveByPositionAndProgress(pos, shapeAabb = iterator.next(), self), movement, deltaProgress)).intersects(entityAabb = entity.getBoundingBox()) && (delta = Math.max(delta, PistonMovingBlockEntity.getMovement(movingAABB, movement, entityAabb))) >= deltaProgress)) {
            }
            if (delta <= 0.0) continue;
            delta = Math.min(delta, deltaProgress) + 0.01;
            PistonMovingBlockEntity.moveEntityByPiston(movement, entity, delta, movement);
            if (self.extending || !self.isSourcePiston) continue;
            PistonMovingBlockEntity.fixEntityWithinPistonBase(pos, entity, movement, deltaProgress);
        }
    }

    private static void moveEntityByPiston(Direction pistonDirection, Entity entity, double delta, Direction movement) {
        NOCLIP.set(pistonDirection);
        Vec3 previousPos = entity.position();
        entity.move(MoverType.PISTON, new Vec3(delta * (double)movement.getStepX(), delta * (double)movement.getStepY(), delta * (double)movement.getStepZ()));
        entity.applyEffectsFromBlocks(previousPos, entity.position());
        entity.removeLatestMovementRecording();
        NOCLIP.set(null);
    }

    private static void moveStuckEntities(Level level, BlockPos pos, float newProgress, PistonMovingBlockEntity self) {
        if (!self.isStickyForEntities()) {
            return;
        }
        Direction movement = self.getMovementDirection();
        if (!movement.getAxis().isHorizontal()) {
            return;
        }
        double stickyTop = self.movedState.getCollisionShape(level, pos).max(Direction.Axis.Y);
        AABB aabb = PistonMovingBlockEntity.moveByPositionAndProgress(pos, new AABB(0.0, stickyTop, 0.0, 1.0, 1.5000010000000001, 1.0), self);
        double deltaProgress = newProgress - self.progress;
        List<Entity> entities = level.getEntities((Entity)null, aabb, entity -> PistonMovingBlockEntity.matchesStickyCritera(aabb, entity, pos));
        for (Entity entity2 : entities) {
            PistonMovingBlockEntity.moveEntityByPiston(movement, entity2, deltaProgress, movement);
        }
    }

    private static boolean matchesStickyCritera(AABB aabb, Entity entity, BlockPos pos) {
        return entity.getPistonPushReaction() == PushReaction.NORMAL && entity.onGround() && (entity.isSupportedBy(pos) || entity.getX() >= aabb.minX && entity.getX() <= aabb.maxX && entity.getZ() >= aabb.minZ && entity.getZ() <= aabb.maxZ);
    }

    private boolean isStickyForEntities() {
        return this.movedState.is(Blocks.HONEY_BLOCK);
    }

    public Direction getMovementDirection() {
        return this.extending ? this.direction : this.direction.getOpposite();
    }

    private static double getMovement(AABB aabbToBeOutsideOf, Direction movement, AABB aabb) {
        switch (movement) {
            case EAST: {
                return aabbToBeOutsideOf.maxX - aabb.minX;
            }
            case WEST: {
                return aabb.maxX - aabbToBeOutsideOf.minX;
            }
            default: {
                return aabbToBeOutsideOf.maxY - aabb.minY;
            }
            case DOWN: {
                return aabb.maxY - aabbToBeOutsideOf.minY;
            }
            case SOUTH: {
                return aabbToBeOutsideOf.maxZ - aabb.minZ;
            }
            case NORTH: 
        }
        return aabb.maxZ - aabbToBeOutsideOf.minZ;
    }

    private static AABB moveByPositionAndProgress(BlockPos pos, AABB aabb, PistonMovingBlockEntity entity) {
        double currentPosition = entity.getExtendedProgress(entity.progress);
        return aabb.move((double)pos.getX() + currentPosition * (double)entity.direction.getStepX(), (double)pos.getY() + currentPosition * (double)entity.direction.getStepY(), (double)pos.getZ() + currentPosition * (double)entity.direction.getStepZ());
    }

    private static void fixEntityWithinPistonBase(BlockPos pos, Entity entity, Direction direction, double deltaProgress) {
        double deltaIntersected;
        Direction opposite;
        double delta;
        AABB box;
        AABB entityAabb = entity.getBoundingBox();
        if (entityAabb.intersects(box = Shapes.block().bounds().move(pos)) && Math.abs((delta = PistonMovingBlockEntity.getMovement(box, opposite = direction.getOpposite(), entityAabb) + 0.01) - (deltaIntersected = PistonMovingBlockEntity.getMovement(box, opposite, entityAabb.intersect(box)) + 0.01)) < 0.01) {
            delta = Math.min(delta, deltaProgress) + 0.01;
            PistonMovingBlockEntity.moveEntityByPiston(direction, entity, delta, opposite);
        }
    }

    public BlockState getMovedState() {
        return this.movedState;
    }

    public void finalTick() {
        if (this.level != null && (this.progressO < 1.0f || this.level.isClientSide())) {
            this.progressO = this.progress = 1.0f;
            this.level.removeBlockEntity(this.worldPosition);
            this.setRemoved();
            if (this.level.getBlockState(this.worldPosition).is(Blocks.MOVING_PISTON)) {
                BlockState newState = this.isSourcePiston ? Blocks.AIR.defaultBlockState() : Block.updateFromNeighbourShapes(this.movedState, this.level, this.worldPosition);
                this.level.setBlock(this.worldPosition, newState, 3);
                this.level.neighborChanged(this.worldPosition, newState.getBlock(), ExperimentalRedstoneUtils.initialOrientation(this.level, this.getPushDirection(), null));
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        this.finalTick();
    }

    public Direction getPushDirection() {
        return this.extending ? this.direction : this.direction.getOpposite();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity entity) {
        entity.lastTicked = level.getGameTime();
        entity.progressO = entity.progress;
        if (entity.progressO >= 1.0f) {
            if (level.isClientSide() && entity.deathTicks < 5) {
                ++entity.deathTicks;
                return;
            }
            level.removeBlockEntity(pos);
            entity.setRemoved();
            if (level.getBlockState(pos).is(Blocks.MOVING_PISTON)) {
                BlockState newState = Block.updateFromNeighbourShapes(entity.movedState, level, pos);
                if (newState.isAir()) {
                    level.setBlock(pos, entity.movedState, 340);
                    Block.updateOrDestroy(entity.movedState, newState, level, pos, 3);
                } else {
                    if (newState.hasProperty(BlockStateProperties.WATERLOGGED) && newState.getValue(BlockStateProperties.WATERLOGGED).booleanValue()) {
                        newState = (BlockState)newState.setValue(BlockStateProperties.WATERLOGGED, false);
                    }
                    level.setBlock(pos, newState, 67);
                    level.neighborChanged(pos, newState.getBlock(), ExperimentalRedstoneUtils.initialOrientation(level, entity.getPushDirection(), null));
                }
            }
            return;
        }
        float newProgress = entity.progress + 0.5f;
        PistonMovingBlockEntity.moveCollidedEntities(level, pos, newProgress, entity);
        PistonMovingBlockEntity.moveStuckEntities(level, pos, newProgress, entity);
        entity.progress = newProgress;
        if (entity.progress >= 1.0f) {
            entity.progress = 1.0f;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.movedState = input.read("blockState", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE);
        this.direction = input.read("facing", Direction.LEGACY_ID_CODEC).orElse(Direction.DOWN);
        this.progressO = this.progress = input.getFloatOr("progress", 0.0f);
        this.extending = input.getBooleanOr("extending", false);
        this.isSourcePiston = input.getBooleanOr("source", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("blockState", BlockState.CODEC, this.movedState);
        output.store("facing", Direction.LEGACY_ID_CODEC, this.direction);
        output.putFloat("progress", this.progressO);
        output.putBoolean("extending", this.extending);
        output.putBoolean("source", this.isSourcePiston);
    }

    public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos) {
        VoxelShape pistonHeadShape = !this.extending && this.isSourcePiston && this.movedState.getBlock() instanceof PistonBaseBlock ? ((BlockState)this.movedState.setValue(PistonBaseBlock.EXTENDED, true)).getCollisionShape(level, pos) : Shapes.empty();
        Direction noClipDirection = NOCLIP.get();
        if ((double)this.progress < 1.0 && noClipDirection == this.getMovementDirection()) {
            return pistonHeadShape;
        }
        BlockState blockState = this.isSourcePiston() ? (BlockState)((BlockState)Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, this.direction)).setValue(PistonHeadBlock.SHORT, this.extending != 1.0f - this.progress < 0.25f) : this.movedState;
        float extendedProgress = this.getExtendedProgress(this.progress);
        double dx = (float)this.direction.getStepX() * extendedProgress;
        double dy = (float)this.direction.getStepY() * extendedProgress;
        double dz = (float)this.direction.getStepZ() * extendedProgress;
        return Shapes.or(pistonHeadShape, blockState.getCollisionShape(level, pos).move(dx, dy, dz));
    }

    public long getLastTicked() {
        return this.lastTicked;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level.holderLookup(Registries.BLOCK).get(this.movedState.getBlock().builtInRegistryHolder().key()).isEmpty()) {
            this.movedState = Blocks.AIR.defaultBlockState();
        }
    }
}

