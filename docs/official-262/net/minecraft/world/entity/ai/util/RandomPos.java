/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomPos {
    private static final int RANDOM_POS_ATTEMPTS = 10;

    public static BlockPos generateRandomDirection(RandomSource random, int horizontalDist, int verticalDist) {
        int xt = random.nextInt(2 * horizontalDist + 1) - horizontalDist;
        int yt = random.nextInt(2 * verticalDist + 1) - verticalDist;
        int zt = random.nextInt(2 * horizontalDist + 1) - horizontalDist;
        return new BlockPos(xt, yt, zt);
    }

    public static @Nullable BlockPos generateRandomDirectionWithinRadians(RandomSource random, double minHorizontalDist, double maxHorizontalDist, int verticalDist, int flyingHeight, double xDir, double zDir, double maxXzRadiansFromDir) {
        double yRadiansCenter = Mth.atan2(zDir, xDir) - 1.5707963705062866;
        double yRadians = yRadiansCenter + (double)(2.0f * random.nextFloat() - 1.0f) * maxXzRadiansFromDir;
        double dist = Mth.lerp(Math.sqrt(random.nextDouble()), minHorizontalDist, maxHorizontalDist) * (double)Mth.SQRT_OF_TWO;
        double xt = -dist * Math.sin(yRadians);
        double zt = dist * Math.cos(yRadians);
        if (Math.abs(xt) > maxHorizontalDist || Math.abs(zt) > maxHorizontalDist) {
            return null;
        }
        int yt = random.nextInt(2 * verticalDist + 1) - verticalDist + flyingHeight;
        return BlockPos.containing(xt, yt, zt);
    }

    @VisibleForTesting
    public static BlockPos moveUpOutOfSolid(BlockPos pos, int maxY, Predicate<BlockPos> solidityTester) {
        if (solidityTester.test(pos)) {
            BlockPos.MutableBlockPos onGroundPos = pos.mutable().move(Direction.UP);
            while (onGroundPos.getY() <= maxY && solidityTester.test(onGroundPos)) {
                onGroundPos.move(Direction.UP);
            }
            return onGroundPos.immutable();
        }
        return pos;
    }

    @VisibleForTesting
    public static BlockPos moveUpToAboveSolid(BlockPos pos, int aboveSolidAmount, int maxY, Predicate<BlockPos> solidityTester) {
        if (aboveSolidAmount < 0) {
            throw new IllegalArgumentException("aboveSolidAmount was " + aboveSolidAmount + ", expected >= 0");
        }
        if (solidityTester.test(pos)) {
            BlockPos.MutableBlockPos mutablePos = pos.mutable().move(Direction.UP);
            while (mutablePos.getY() <= maxY && solidityTester.test(mutablePos)) {
                mutablePos.move(Direction.UP);
            }
            int firstNonSolidY = mutablePos.getY();
            while (mutablePos.getY() <= maxY && mutablePos.getY() - firstNonSolidY < aboveSolidAmount) {
                mutablePos.move(Direction.UP);
                if (!solidityTester.test(mutablePos)) continue;
                mutablePos.move(Direction.DOWN);
                break;
            }
            return mutablePos.immutable();
        }
        return pos;
    }

    public static @Nullable Vec3 generateRandomPos(PathfinderMob mob, Supplier<@Nullable BlockPos> posSupplier) {
        return RandomPos.generateRandomPos(posSupplier, mob::getWalkTargetValue);
    }

    public static @Nullable Vec3 generateRandomPos(Supplier<@Nullable BlockPos> posSupplier, ToDoubleFunction<BlockPos> positionWeightFunction) {
        double bestWeight = Double.NEGATIVE_INFINITY;
        BlockPos bestPos = null;
        for (int i = 0; i < 10; ++i) {
            double value;
            BlockPos pos = posSupplier.get();
            if (pos == null || !((value = positionWeightFunction.applyAsDouble(pos)) > bestWeight)) continue;
            bestWeight = value;
            bestPos = pos;
        }
        return bestPos != null ? Vec3.atBottomCenterOf(bestPos) : null;
    }

    public static BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double xzDist, RandomSource random, BlockPos direction) {
        double xt = direction.getX();
        double zt = direction.getZ();
        if (mob.hasHome() && xzDist > 1.0) {
            BlockPos center = mob.getHomePosition();
            xt = mob.getX() > (double)center.getX() ? (xt -= random.nextDouble() * xzDist / 2.0) : (xt += random.nextDouble() * xzDist / 2.0);
            zt = mob.getZ() > (double)center.getZ() ? (zt -= random.nextDouble() * xzDist / 2.0) : (zt += random.nextDouble() * xzDist / 2.0);
        }
        return BlockPos.containing(xt + mob.getX(), (double)direction.getY() + mob.getY(), zt + mob.getZ());
    }
}

