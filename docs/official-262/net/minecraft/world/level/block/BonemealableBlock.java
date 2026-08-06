/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
    public boolean isValidBonemealTarget(LevelReader var1, BlockPos var2, BlockState var3);

    public boolean isBonemealSuccess(Level var1, RandomSource var2, BlockPos var3, BlockState var4);

    public void performBonemeal(ServerLevel var1, RandomSource var2, BlockPos var3, BlockState var4);

    public static boolean hasSpreadableNeighbourPos(LevelReader level, BlockPos pos, BlockState blockToPlace) {
        return BonemealableBlock.getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.stream().toList(), level, pos, blockToPlace).isPresent();
    }

    public static Optional<BlockPos> findSpreadableNeighbourPos(Level level, BlockPos pos, BlockState blockToPlace) {
        return BonemealableBlock.getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.shuffledCopy(level.getRandom()), level, pos, blockToPlace);
    }

    private static Optional<BlockPos> getSpreadableNeighbourPos(List<Direction> directions, LevelReader level, BlockPos pos, BlockState blockToPlace) {
        for (Direction direction : directions) {
            BlockPos neighbourPos = pos.relative(direction);
            if (!level.isEmptyBlock(neighbourPos) || !blockToPlace.canSurvive(level, neighbourPos)) continue;
            return Optional.of(neighbourPos);
        }
        return Optional.empty();
    }

    default public BlockPos getParticlePos(BlockPos blockPos) {
        return switch (this.getType().ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> blockPos.above();
            case 1 -> blockPos;
        };
    }

    default public Type getType() {
        return Type.GROWER;
    }

    public static enum Type {
        NEIGHBOR_SPREADER,
        GROWER;

    }
}

