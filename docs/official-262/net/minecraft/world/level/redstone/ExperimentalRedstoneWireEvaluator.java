/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.redstone;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.jspecify.annotations.Nullable;

public class ExperimentalRedstoneWireEvaluator
extends RedstoneWireEvaluator {
    private final Deque<BlockPos> wiresToTurnOff = new ArrayDeque<BlockPos>();
    private final Deque<BlockPos> wiresToTurnOn = new ArrayDeque<BlockPos>();
    private final Object2IntMap<BlockPos> updatedWires = new Object2IntLinkedOpenHashMap();

    public ExperimentalRedstoneWireEvaluator(RedStoneWireBlock wireBlock) {
        super(wireBlock);
    }

    @Override
    public void updatePowerStrength(Level level, BlockPos initialPos, BlockState ignored, @Nullable Orientation orientation, boolean shapeUpdateWiresAroundInitialPosition) {
        Orientation initialOrientation = ExperimentalRedstoneWireEvaluator.getInitialOrientation(level, orientation);
        this.calculateCurrentChanges(level, initialPos, initialOrientation);
        ObjectIterator iterator = this.updatedWires.object2IntEntrySet().iterator();
        boolean initialWire = true;
        while (iterator.hasNext()) {
            Object2IntMap.Entry next = (Object2IntMap.Entry)iterator.next();
            BlockPos pos = (BlockPos)next.getKey();
            int packed = next.getIntValue();
            int newLevel = ExperimentalRedstoneWireEvaluator.unpackPower(packed);
            BlockState state = level.getBlockState(pos);
            if (state.is(this.wireBlock) && !state.getValue(RedStoneWireBlock.POWER).equals(newLevel)) {
                int updateFlags = 2;
                if (!shapeUpdateWiresAroundInitialPosition || !initialWire) {
                    updateFlags |= 0x80;
                }
                level.setBlock(pos, (BlockState)state.setValue(RedStoneWireBlock.POWER, newLevel), updateFlags);
            } else {
                iterator.remove();
            }
            initialWire = false;
        }
        this.causeNeighborUpdates(level);
    }

    private void causeNeighborUpdates(Level level) {
        ServerLevel serverLevel;
        this.updatedWires.forEach((wirePos, packed) -> {
            Orientation orientation = ExperimentalRedstoneWireEvaluator.unpackOrientation(packed);
            BlockState state = level.getBlockState((BlockPos)wirePos);
            for (Direction neighborDirection : orientation.getDirections()) {
                if (!ExperimentalRedstoneWireEvaluator.isConnected(state, neighborDirection)) continue;
                BlockPos neighborPos = wirePos.relative(neighborDirection);
                BlockState neighborState = level.getBlockState(neighborPos);
                Orientation neighborOrientation = orientation.withFrontPreserveUp(neighborDirection);
                level.neighborChanged(neighborState, neighborPos, this.wireBlock, neighborOrientation, false);
                if (!neighborState.isRedstoneConductor(level, neighborPos)) continue;
                for (Direction direction : neighborOrientation.getDirections()) {
                    if (direction == neighborDirection.getOpposite()) continue;
                    level.neighborChanged(neighborPos.relative(direction), this.wireBlock, neighborOrientation.withFrontPreserveUp(direction));
                }
            }
        });
        if (level instanceof ServerLevel && (serverLevel = (ServerLevel)level).debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS)) {
            this.updatedWires.forEach((wirePos, packed) -> serverLevel.debugSynchronizers().sendBlockValue((BlockPos)wirePos, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, ExperimentalRedstoneWireEvaluator.unpackOrientation(packed)));
        }
    }

    private static boolean isConnected(BlockState state, Direction direction) {
        EnumProperty<RedstoneSide> property = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
        if (property == null) {
            return direction == Direction.DOWN;
        }
        return state.getValue(property).isConnected();
    }

    private static Orientation getInitialOrientation(Level level, @Nullable Orientation incomingOrigination) {
        Orientation orientation = incomingOrigination != null ? incomingOrigination : Orientation.random(level.getRandom());
        return orientation.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
    }

    private void calculateCurrentChanges(Level level, BlockPos initialPosition, Orientation initialOrientation) {
        int packed;
        BlockPos pos;
        BlockState initialState = level.getBlockState(initialPosition);
        if (initialState.is(this.wireBlock)) {
            this.setPower(initialPosition, initialState.getValue(RedStoneWireBlock.POWER), initialOrientation);
            this.wiresToTurnOff.add(initialPosition);
        } else {
            this.propagateChangeToNeighbors(level, initialPosition, 0, initialOrientation, true);
        }
        while (!this.wiresToTurnOff.isEmpty()) {
            int powerToSet;
            int wirePower;
            pos = this.wiresToTurnOff.removeFirst();
            packed = this.updatedWires.getInt((Object)pos);
            Orientation orientation = ExperimentalRedstoneWireEvaluator.unpackOrientation(packed);
            int oldPower = ExperimentalRedstoneWireEvaluator.unpackPower(packed);
            int blockPower = this.getBlockSignal(level, pos);
            int newPower = Math.max(blockPower, wirePower = this.getIncomingWireSignal(level, pos));
            if (newPower < oldPower) {
                if (blockPower > 0 && !this.wiresToTurnOn.contains(pos)) {
                    this.wiresToTurnOn.add(pos);
                }
                powerToSet = 0;
            } else {
                powerToSet = newPower;
            }
            if (powerToSet != oldPower) {
                this.setPower(pos, powerToSet, orientation);
            }
            this.propagateChangeToNeighbors(level, pos, powerToSet, orientation, oldPower > newPower);
        }
        while (!this.wiresToTurnOn.isEmpty()) {
            pos = this.wiresToTurnOn.removeFirst();
            packed = this.updatedWires.getInt((Object)pos);
            int oldPower = ExperimentalRedstoneWireEvaluator.unpackPower(packed);
            int blockPower = this.getBlockSignal(level, pos);
            int wirePower = this.getIncomingWireSignal(level, pos);
            int newPower = Math.max(blockPower, wirePower);
            Orientation orientation = ExperimentalRedstoneWireEvaluator.unpackOrientation(packed);
            if (newPower > oldPower) {
                this.setPower(pos, newPower, orientation);
            } else if (newPower < oldPower) {
                throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
            }
            this.propagateChangeToNeighbors(level, pos, newPower, orientation, false);
        }
    }

    private static int packOrientationAndPower(Orientation orientation, int power) {
        return orientation.getIndex() << 4 | power;
    }

    private static Orientation unpackOrientation(int packed) {
        return Orientation.fromIndex(packed >> 4);
    }

    private static int unpackPower(int packed) {
        return packed & 0xF;
    }

    private void setPower(BlockPos pos, int newPower, Orientation orientation) {
        this.updatedWires.compute((Object)pos, (key, packed) -> {
            if (packed == null) {
                return ExperimentalRedstoneWireEvaluator.packOrientationAndPower(orientation, newPower);
            }
            return ExperimentalRedstoneWireEvaluator.packOrientationAndPower(ExperimentalRedstoneWireEvaluator.unpackOrientation(packed), newPower);
        });
    }

    private void propagateChangeToNeighbors(Level level, BlockPos pos, int newPower, Orientation orientation, boolean allowTurningOff) {
        BlockPos offsetPos;
        for (Direction directionHorizontal : orientation.getHorizontalDirections()) {
            offsetPos = pos.relative(directionHorizontal);
            this.enqueueNeighborWire(level, offsetPos, newPower, orientation.withFront(directionHorizontal), allowTurningOff);
        }
        for (Direction directionVertical : orientation.getVerticalDirections()) {
            offsetPos = pos.relative(directionVertical);
            boolean solidBlock = level.getBlockState(offsetPos).isRedstoneConductor(level, offsetPos);
            for (Direction directionHorizontal : orientation.getHorizontalDirections()) {
                BlockPos neighborWire;
                BlockPos neighbor = pos.relative(directionHorizontal);
                if (directionVertical == Direction.UP && !solidBlock) {
                    neighborWire = offsetPos.relative(directionHorizontal);
                    this.enqueueNeighborWire(level, neighborWire, newPower, orientation.withFront(directionHorizontal), allowTurningOff);
                    continue;
                }
                if (directionVertical != Direction.DOWN || level.getBlockState(neighbor).isRedstoneConductor(level, neighbor)) continue;
                neighborWire = offsetPos.relative(directionHorizontal);
                this.enqueueNeighborWire(level, neighborWire, newPower, orientation.withFront(directionHorizontal), allowTurningOff);
            }
        }
    }

    private void enqueueNeighborWire(Level level, BlockPos pos, int newFromPower, Orientation orientation, boolean allowTurningOff) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this.wireBlock)) {
            int toPower = this.getWireSignal(pos, state);
            if (toPower < newFromPower - 1 && !this.wiresToTurnOn.contains(pos)) {
                this.wiresToTurnOn.add(pos);
                this.setPower(pos, toPower, orientation);
            }
            if (allowTurningOff && toPower > newFromPower && !this.wiresToTurnOff.contains(pos)) {
                this.wiresToTurnOff.add(pos);
                this.setPower(pos, toPower, orientation);
            }
        }
    }

    @Override
    protected int getWireSignal(BlockPos pos, BlockState state) {
        int packed = this.updatedWires.getOrDefault((Object)pos, -1);
        if (packed != -1) {
            return ExperimentalRedstoneWireEvaluator.unpackPower(packed);
        }
        return super.getWireSignal(pos, state);
    }
}

