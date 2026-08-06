/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SideChainPart;

public interface SideChainPartBlock {
    public SideChainPart getSideChainPart(BlockState var1);

    public BlockState setSideChainPart(BlockState var1, SideChainPart var2);

    public Direction getFacing(BlockState var1);

    public boolean isConnectable(BlockState var1);

    public int getMaxChainLength();

    default public List<BlockPos> getAllBlocksConnectedTo(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!this.isConnectable(state)) {
            return List.of();
        }
        Neighbors neighbors = this.getNeighbors(level, pos, this.getFacing(state));
        LinkedList<BlockPos> results = new LinkedList<BlockPos>();
        results.add(pos);
        this.addBlocksConnectingTowards(neighbors::left, SideChainPart.LEFT, results::addFirst);
        this.addBlocksConnectingTowards(neighbors::right, SideChainPart.RIGHT, results::addLast);
        return results;
    }

    private void addBlocksConnectingTowards(IntFunction<Neighbor> getNeighbor, SideChainPart endPart, Consumer<BlockPos> accumulator) {
        for (int steps = 1; steps < this.getMaxChainLength(); ++steps) {
            Neighbor neighbor = getNeighbor.apply(steps);
            if (neighbor.connectsTowards(endPart)) {
                accumulator.accept(neighbor.pos());
            }
            if (neighbor.isUnconnectableOrChainEnd()) break;
        }
    }

    default public void updateNeighborsAfterPoweringDown(LevelAccessor level, BlockPos pos, BlockState state) {
        Neighbors neighbors = this.getNeighbors(level, pos, this.getFacing(state));
        neighbors.left().disconnectFromRight();
        neighbors.right().disconnectFromLeft();
    }

    default public void updateSelfAndNeighborsOnPoweringUp(LevelAccessor level, BlockPos pos, BlockState state, BlockState oldState) {
        if (!this.isConnectable(state)) {
            return;
        }
        if (this.isBeingUpdatedByNeighbor(state, oldState)) {
            return;
        }
        Neighbors neighbors = this.getNeighbors(level, pos, this.getFacing(state));
        SideChainPart newPartForSelf = SideChainPart.UNCONNECTED;
        int existingChainOnTheLeft = neighbors.left().isConnectable() ? this.getAllBlocksConnectedTo(level, neighbors.left().pos()).size() : 0;
        int existingChainOnTheRight = neighbors.right().isConnectable() ? this.getAllBlocksConnectedTo(level, neighbors.right().pos()).size() : 0;
        int currentChainLength = 1;
        if (this.canConnect(existingChainOnTheLeft, currentChainLength)) {
            newPartForSelf = newPartForSelf.whenConnectedToTheLeft();
            neighbors.left().connectToTheRight();
            currentChainLength += existingChainOnTheLeft;
        }
        if (this.canConnect(existingChainOnTheRight, currentChainLength)) {
            newPartForSelf = newPartForSelf.whenConnectedToTheRight();
            neighbors.right().connectToTheLeft();
        }
        this.setPart(level, pos, newPartForSelf);
    }

    private boolean canConnect(int newBlocksToConnectTo, int currentChainLength) {
        return newBlocksToConnectTo > 0 && currentChainLength + newBlocksToConnectTo <= this.getMaxChainLength();
    }

    private boolean isBeingUpdatedByNeighbor(BlockState state, BlockState oldState) {
        boolean isGettingConnected = this.getSideChainPart(state).isConnected();
        boolean hasBeenConnectedBefore = this.isConnectable(oldState) && this.getSideChainPart(oldState).isConnected();
        return isGettingConnected || hasBeenConnectedBefore;
    }

    private Neighbors getNeighbors(LevelAccessor level, BlockPos center, Direction facing) {
        return new Neighbors(this, level, facing, center, new HashMap<BlockPos, Neighbor>());
    }

    private void setPart(LevelAccessor level, BlockPos pos, SideChainPart newPart) {
        BlockState state = level.getBlockState(pos);
        if (this.getSideChainPart(state) != newPart) {
            level.setBlock(pos, this.setSideChainPart(state, newPart), 3);
        }
    }

    public record Neighbors(SideChainPartBlock block, LevelAccessor level, Direction facing, BlockPos center, Map<BlockPos, Neighbor> cache) {
        private boolean isConnectableToThisBlock(BlockState neighbor) {
            return this.block.isConnectable(neighbor) && this.block.getFacing(neighbor) == this.facing;
        }

        private Neighbor createNewNeighbor(BlockPos pos) {
            BlockState neighbor = this.level.getBlockState(pos);
            SideChainPart part = this.isConnectableToThisBlock(neighbor) ? this.block.getSideChainPart(neighbor) : null;
            return part == null ? new EmptyNeighbor(pos) : new SideChainNeighbor(this.level, this.block, pos, part);
        }

        private Neighbor getOrCreateNeighbor(Direction dir, Integer steps) {
            return this.cache.computeIfAbsent(this.center.relative(dir, (int)steps), this::createNewNeighbor);
        }

        public Neighbor left(int steps) {
            return this.getOrCreateNeighbor(this.facing.getClockWise(), steps);
        }

        public Neighbor right(int steps) {
            return this.getOrCreateNeighbor(this.facing.getCounterClockWise(), steps);
        }

        public Neighbor left() {
            return this.left(1);
        }

        public Neighbor right() {
            return this.right(1);
        }
    }

    public static sealed interface Neighbor
    permits EmptyNeighbor, SideChainNeighbor {
        public BlockPos pos();

        public boolean isConnectable();

        public boolean isUnconnectableOrChainEnd();

        public boolean connectsTowards(SideChainPart var1);

        default public void connectToTheRight() {
        }

        default public void connectToTheLeft() {
        }

        default public void disconnectFromRight() {
        }

        default public void disconnectFromLeft() {
        }
    }

    public record SideChainNeighbor(LevelAccessor level, SideChainPartBlock block, BlockPos pos, SideChainPart part) implements Neighbor
    {
        @Override
        public boolean isConnectable() {
            return true;
        }

        @Override
        public boolean isUnconnectableOrChainEnd() {
            return this.part.isChainEnd();
        }

        @Override
        public boolean connectsTowards(SideChainPart endPart) {
            return this.part.isConnectionTowards(endPart);
        }

        @Override
        public void connectToTheRight() {
            this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheRight());
        }

        @Override
        public void connectToTheLeft() {
            this.block.setPart(this.level, this.pos, this.part.whenConnectedToTheLeft());
        }

        @Override
        public void disconnectFromRight() {
            this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheRight());
        }

        @Override
        public void disconnectFromLeft() {
            this.block.setPart(this.level, this.pos, this.part.whenDisconnectedFromTheLeft());
        }
    }

    public record EmptyNeighbor(BlockPos pos) implements Neighbor
    {
        @Override
        public boolean isConnectable() {
            return false;
        }

        @Override
        public boolean isUnconnectableOrChainEnd() {
            return true;
        }

        @Override
        public boolean connectsTowards(SideChainPart endPart) {
            return false;
        }
    }
}

