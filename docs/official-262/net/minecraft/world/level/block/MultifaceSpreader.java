/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MultifaceSpreader {
    public static final SpreadType[] DEFAULT_SPREAD_ORDER = new SpreadType[]{SpreadType.SAME_POSITION, SpreadType.SAME_PLANE, SpreadType.WRAP_AROUND};
    private final SpreadConfig config;

    public MultifaceSpreader(MultifaceBlock multifaceBlock) {
        this(new DefaultSpreaderConfig(multifaceBlock));
    }

    public MultifaceSpreader(SpreadConfig config) {
        this.config = config;
    }

    public boolean canSpreadInAnyDirection(BlockState state, BlockGetter level, BlockPos pos, Direction startingFace) {
        return Direction.stream().anyMatch(spreadDirection -> this.getSpreadFromFaceTowardDirection(state, level, pos, startingFace, (Direction)spreadDirection, this.config::canSpreadInto).isPresent());
    }

    public Optional<SpreadPos> spreadFromRandomFaceTowardRandomDirection(BlockState state, LevelAccessor level, BlockPos pos, RandomSource random) {
        return Direction.allShuffled(random).stream().filter(faceDirection -> this.config.canSpreadFrom(state, (Direction)faceDirection)).map(faceDirection -> this.spreadFromFaceTowardRandomDirection(state, level, pos, (Direction)faceDirection, random, false)).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    public long spreadAll(BlockState state, LevelAccessor level, BlockPos pos, boolean postProcess) {
        return Direction.stream().filter(faceDirection -> this.config.canSpreadFrom(state, (Direction)faceDirection)).map(faceDirection -> this.spreadFromFaceTowardAllDirections(state, level, pos, (Direction)faceDirection, postProcess)).reduce(0L, Long::sum);
    }

    public Optional<SpreadPos> spreadFromFaceTowardRandomDirection(BlockState state, LevelAccessor level, BlockPos pos, Direction startingFace, RandomSource random, boolean postProcess) {
        return Direction.allShuffled(random).stream().map(spreadDirection -> this.spreadFromFaceTowardDirection(state, level, pos, startingFace, (Direction)spreadDirection, postProcess)).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    private long spreadFromFaceTowardAllDirections(BlockState state, LevelAccessor level, BlockPos pos, Direction startingFace, boolean postProcess) {
        return Direction.stream().map(spreadDirection -> this.spreadFromFaceTowardDirection(state, level, pos, startingFace, (Direction)spreadDirection, postProcess)).filter(Optional::isPresent).count();
    }

    @VisibleForTesting
    public Optional<SpreadPos> spreadFromFaceTowardDirection(BlockState state, LevelAccessor level, BlockPos pos, Direction fromFace, Direction spreadDirection, boolean postProcess) {
        return this.getSpreadFromFaceTowardDirection(state, level, pos, fromFace, spreadDirection, this.config::canSpreadInto).flatMap(spreadPos -> this.spreadToFace(level, (SpreadPos)spreadPos, postProcess));
    }

    public Optional<SpreadPos> getSpreadFromFaceTowardDirection(BlockState state, BlockGetter level, BlockPos pos, Direction startingFace, Direction spreadDirection, SpreadPredicate canSpreadInto) {
        if (spreadDirection.getAxis() == startingFace.getAxis()) {
            return Optional.empty();
        }
        if (!(this.config.isOtherBlockValidAsSource(state) || this.config.hasFace(state, startingFace) && !this.config.hasFace(state, spreadDirection))) {
            return Optional.empty();
        }
        for (SpreadType type : this.config.getSpreadTypes()) {
            SpreadPos spreadPos = type.getSpreadPos(pos, spreadDirection, startingFace);
            if (!canSpreadInto.test(level, pos, spreadPos)) continue;
            return Optional.of(spreadPos);
        }
        return Optional.empty();
    }

    public Optional<SpreadPos> spreadToFace(LevelAccessor level, SpreadPos spreadPos, boolean postProcess) {
        BlockState oldState = level.getBlockState(spreadPos.pos());
        if (this.config.placeBlock(level, spreadPos, oldState, postProcess)) {
            return Optional.of(spreadPos);
        }
        return Optional.empty();
    }

    public static class DefaultSpreaderConfig
    implements SpreadConfig {
        protected final MultifaceBlock block;

        public DefaultSpreaderConfig(MultifaceBlock block) {
            this.block = block;
        }

        @Override
        public @Nullable BlockState getStateForPlacement(BlockState oldState, BlockGetter level, BlockPos placementPos, Direction placementDirection) {
            return this.block.getStateForPlacement(oldState, level, placementPos, placementDirection);
        }

        protected boolean stateCanBeReplaced(BlockGetter level, BlockPos sourcePos, BlockPos placementPos, Direction placementDirection, BlockState existingState) {
            return existingState.isAir() || existingState.is(this.block) || existingState.is(Blocks.WATER) && existingState.getFluidState().isSource();
        }

        @Override
        public boolean canSpreadInto(BlockGetter level, BlockPos sourcePos, SpreadPos spreadPos) {
            BlockState existingState = level.getBlockState(spreadPos.pos());
            return this.stateCanBeReplaced(level, sourcePos, spreadPos.pos(), spreadPos.face(), existingState) && this.block.isValidStateForPlacement(level, existingState, spreadPos.pos(), spreadPos.face());
        }
    }

    public static interface SpreadConfig {
        public @Nullable BlockState getStateForPlacement(BlockState var1, BlockGetter var2, BlockPos var3, Direction var4);

        public boolean canSpreadInto(BlockGetter var1, BlockPos var2, SpreadPos var3);

        default public SpreadType[] getSpreadTypes() {
            return DEFAULT_SPREAD_ORDER;
        }

        default public boolean hasFace(BlockState state, Direction face) {
            return MultifaceBlock.hasFace(state, face);
        }

        default public boolean isOtherBlockValidAsSource(BlockState state) {
            return false;
        }

        default public boolean canSpreadFrom(BlockState state, Direction face) {
            return this.isOtherBlockValidAsSource(state) || this.hasFace(state, face);
        }

        default public boolean placeBlock(LevelAccessor level, SpreadPos spreadPos, BlockState oldState, boolean postProcess) {
            BlockState spreadState = this.getStateForPlacement(oldState, level, spreadPos.pos(), spreadPos.face());
            if (spreadState != null) {
                if (postProcess) {
                    level.getChunk(spreadPos.pos()).markPosForPostProcessing(spreadPos.pos());
                }
                return level.setBlock(spreadPos.pos(), spreadState, 2);
            }
            return false;
        }
    }

    @FunctionalInterface
    public static interface SpreadPredicate {
        public boolean test(BlockGetter var1, BlockPos var2, SpreadPos var3);
    }

    public static enum SpreadType {
        SAME_POSITION{

            @Override
            public SpreadPos getSpreadPos(BlockPos pos, Direction spreadDirection, Direction fromFace) {
                return new SpreadPos(pos, spreadDirection);
            }
        }
        ,
        SAME_PLANE{

            @Override
            public SpreadPos getSpreadPos(BlockPos pos, Direction spreadDirection, Direction fromFace) {
                return new SpreadPos(pos.relative(spreadDirection), fromFace);
            }
        }
        ,
        WRAP_AROUND{

            @Override
            public SpreadPos getSpreadPos(BlockPos pos, Direction spreadDirection, Direction fromFace) {
                return new SpreadPos(pos.relative(spreadDirection).relative(fromFace), spreadDirection.getOpposite());
            }
        };


        public abstract SpreadPos getSpreadPos(BlockPos var1, Direction var2, Direction var3);
    }

    public record SpreadPos(BlockPos pos, Direction face) {
    }
}

