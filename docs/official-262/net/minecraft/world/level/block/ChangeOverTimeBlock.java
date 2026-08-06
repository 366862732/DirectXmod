/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
    public static final int SCAN_DISTANCE = 4;

    public Optional<BlockState> getNext(BlockState var1);

    public float getChanceModifier();

    default public void changeOverTime(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float eachBlockOncePerDayChance = 0.05688889f;
        if (random.nextFloat() < 0.05688889f) {
            this.getNextState(state, level, pos, random).ifPresent(weatheredState -> level.setBlockAndUpdate(pos, (BlockState)weatheredState));
        }
    }

    public T getAge();

    default public Optional<BlockState> getNextState(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos blockPos;
        int manhattanDistance;
        int ownAge = ((Enum)this.getAge()).ordinal();
        int sameAgeCount = 0;
        int olderCount = 0;
        Iterator<BlockPos> iterator = BlockPos.withinManhattan(pos, 4, 4, 4).iterator();
        while (iterator.hasNext() && (manhattanDistance = (blockPos = iterator.next()).distManhattan(pos)) <= 4) {
            Block block;
            if (blockPos.equals(pos) || !((block = level.getBlockState(blockPos).getBlock()) instanceof ChangeOverTimeBlock)) continue;
            ChangeOverTimeBlock neighborBlock = (ChangeOverTimeBlock)((Object)block);
            T neighborAge = neighborBlock.getAge();
            if (this.getAge().getClass() != neighborAge.getClass()) continue;
            int foundAge = ((Enum)neighborAge).ordinal();
            if (foundAge < ownAge) {
                return Optional.empty();
            }
            if (foundAge > ownAge) {
                ++olderCount;
                continue;
            }
            ++sameAgeCount;
        }
        float chance = (float)(olderCount + 1) / (float)(olderCount + sameAgeCount + 1);
        float actualChance = chance * chance * this.getChanceModifier();
        if (random.nextFloat() < actualChance) {
            return this.getNext(state);
        }
        return Optional.empty();
    }
}

