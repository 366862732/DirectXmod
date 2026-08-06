/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class DoubleBlockCombiner {
    public static <S extends BlockEntity> NeighborCombineResult<S> combineWithNeigbour(BlockEntityType<S> entityType, Function<BlockState, BlockType> typeResolver, Function<BlockState, Direction> connectionResolver, Property<Direction> facingProperty, BlockState state, LevelAccessor level, BlockPos pos, BiPredicate<LevelAccessor, BlockPos> blockedChecker) {
        BlockType neighbourType;
        boolean isFirst;
        S blockEntity = entityType.getBlockEntity(level, pos);
        if (blockEntity == null) {
            return Combiner::acceptNone;
        }
        if (blockedChecker.test(level, pos)) {
            return Combiner::acceptNone;
        }
        BlockType type = typeResolver.apply(state);
        boolean single = type == BlockType.SINGLE;
        boolean bl = isFirst = type == BlockType.FIRST;
        if (single) {
            return new NeighborCombineResult.Single<S>(blockEntity);
        }
        BlockPos neighborPos = pos.relative(connectionResolver.apply(state));
        BlockState neighbourState = level.getBlockState(neighborPos);
        if (neighbourState.is(state.getBlock()) && (neighbourType = typeResolver.apply(neighbourState)) != BlockType.SINGLE && type != neighbourType && neighbourState.getValue(facingProperty) == state.getValue(facingProperty)) {
            if (blockedChecker.test(level, neighborPos)) {
                return Combiner::acceptNone;
            }
            S neighbour = entityType.getBlockEntity(level, neighborPos);
            if (neighbour != null) {
                S first = isFirst ? blockEntity : neighbour;
                S second = isFirst ? neighbour : blockEntity;
                return new NeighborCombineResult.Double<S>(first, second);
            }
        }
        return new NeighborCombineResult.Single<S>(blockEntity);
    }

    public static interface NeighborCombineResult<S> {
        public <T> T apply(Combiner<? super S, T> var1);

        public static final class Single<S>
        implements NeighborCombineResult<S> {
            private final S single;

            public Single(S single) {
                this.single = single;
            }

            @Override
            public <T> T apply(Combiner<? super S, T> callback) {
                return callback.acceptSingle(this.single);
            }
        }

        public static final class Double<S>
        implements NeighborCombineResult<S> {
            private final S first;
            private final S second;

            public Double(S first, S second) {
                this.first = first;
                this.second = second;
            }

            @Override
            public <T> T apply(Combiner<? super S, T> callback) {
                return callback.acceptDouble(this.first, this.second);
            }
        }
    }

    public static enum BlockType {
        SINGLE,
        FIRST,
        SECOND;

    }

    public static interface Combiner<S, T> {
        public T acceptDouble(S var1, S var2);

        public T acceptSingle(S var1);

        public T acceptNone();
    }
}

