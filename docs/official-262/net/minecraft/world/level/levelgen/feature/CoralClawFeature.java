/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralClawFeature
extends CoralFeature {
    public CoralClawFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean placeFeature(LevelAccessor level, RandomSource random, BlockPos origin, BlockState state) {
        if (!this.placeCoralBlock(level, random, origin, state)) {
            return false;
        }
        Direction clawDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int nBranches = random.nextInt(2) + 2;
        List<Direction> possibleDirections = Util.toShuffledList(Stream.of(clawDirection, clawDirection.getClockWise(), clawDirection.getCounterClockWise()), random);
        List<Direction> branchDirections = possibleDirections.subList(0, nBranches);
        block0: for (Direction branchDirection : branchDirections) {
            int i;
            int inwayLenth;
            Direction segmentDirection;
            BlockPos.MutableBlockPos mutPos = origin.mutable();
            int sidewayLength = random.nextInt(2) + 1;
            mutPos.move(branchDirection);
            if (branchDirection == clawDirection) {
                segmentDirection = clawDirection;
                inwayLenth = random.nextInt(3) + 2;
            } else {
                mutPos.move(Direction.UP);
                Direction[] segmentPossibleDirections = new Direction[]{branchDirection, Direction.UP};
                segmentDirection = Util.getRandom(segmentPossibleDirections, random);
                inwayLenth = random.nextInt(3) + 3;
            }
            for (i = 0; i < sidewayLength && this.placeCoralBlock(level, random, mutPos, state); ++i) {
                mutPos.move(segmentDirection);
            }
            mutPos.move(segmentDirection.getOpposite());
            mutPos.move(Direction.UP);
            for (i = 0; i < inwayLenth; ++i) {
                mutPos.move(clawDirection);
                if (!this.placeCoralBlock(level, random, mutPos, state)) continue block0;
                if (!(random.nextFloat() < 0.25f)) continue;
                mutPos.move(Direction.UP);
            }
        }
        return true;
    }
}

