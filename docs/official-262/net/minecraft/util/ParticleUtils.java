/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
    public static void spawnParticlesOnBlockFaces(Level level, BlockPos pos, ParticleOptions particle, IntProvider particlesPerFaceRange) {
        RandomSource random = level.getRandom();
        for (Direction direction : Direction.values()) {
            ParticleUtils.spawnParticlesOnBlockFace(level, pos, particle, particlesPerFaceRange, direction, () -> ParticleUtils.getRandomSpeedRanges(random), 0.55);
        }
    }

    public static void spawnParticlesOnBlockFace(Level level, BlockPos pos, ParticleOptions particle, IntProvider particlesPerFaceRange, Direction face, Supplier<Vec3> speedSupplier, double stepFactor) {
        int particleCount = particlesPerFaceRange.sample(level.getRandom());
        for (int i = 0; i < particleCount; ++i) {
            ParticleUtils.spawnParticleOnFace(level, pos, face, particle, speedSupplier.get(), stepFactor);
        }
    }

    private static Vec3 getRandomSpeedRanges(RandomSource random) {
        return new Vec3(Mth.nextDouble(random, -0.5, 0.5), Mth.nextDouble(random, -0.5, 0.5), Mth.nextDouble(random, -0.5, 0.5));
    }

    public static void spawnParticlesAlongAxis(Direction.Axis attachedAxis, Level level, BlockPos pos, double radius, ParticleOptions particle, UniformInt sparkCount) {
        Vec3 centerOfBlock = Vec3.atCenterOf(pos);
        boolean stepX = attachedAxis == Direction.Axis.X;
        boolean stepY = attachedAxis == Direction.Axis.Y;
        boolean stepZ = attachedAxis == Direction.Axis.Z;
        RandomSource random = level.getRandom();
        int particleCount = sparkCount.sample(random);
        for (int i = 0; i < particleCount; ++i) {
            double x = centerOfBlock.x + Mth.nextDouble(random, -1.0, 1.0) * (stepX ? 0.5 : radius);
            double y = centerOfBlock.y + Mth.nextDouble(random, -1.0, 1.0) * (stepY ? 0.5 : radius);
            double z = centerOfBlock.z + Mth.nextDouble(random, -1.0, 1.0) * (stepZ ? 0.5 : radius);
            double xBaseSpeed = stepX ? Mth.nextDouble(random, -1.0, 1.0) : 0.0;
            double yBaseSpeed = stepY ? Mth.nextDouble(random, -1.0, 1.0) : 0.0;
            double zBaseSpeed = stepZ ? Mth.nextDouble(random, -1.0, 1.0) : 0.0;
            level.addParticle(particle, x, y, z, xBaseSpeed, yBaseSpeed, zBaseSpeed);
        }
    }

    public static void spawnParticleOnFace(Level level, BlockPos pos, Direction face, ParticleOptions particle, Vec3 speed, double stepFactor) {
        Vec3 centerOfBlock = Vec3.atCenterOf(pos);
        int stepX = face.getStepX();
        int stepY = face.getStepY();
        int stepZ = face.getStepZ();
        RandomSource random = level.getRandom();
        double x = centerOfBlock.x + (stepX == 0 ? Mth.nextDouble(random, -0.5, 0.5) : (double)stepX * stepFactor);
        double y = centerOfBlock.y + (stepY == 0 ? Mth.nextDouble(random, -0.5, 0.5) : (double)stepY * stepFactor);
        double z = centerOfBlock.z + (stepZ == 0 ? Mth.nextDouble(random, -0.5, 0.5) : (double)stepZ * stepFactor);
        double xBaseSpeed = stepX == 0 ? speed.x() : 0.0;
        double yBaseSpeed = stepY == 0 ? speed.y() : 0.0;
        double zBaseSpeed = stepZ == 0 ? speed.z() : 0.0;
        level.addParticle(particle, x, y, z, xBaseSpeed, yBaseSpeed, zBaseSpeed);
    }

    public static void spawnParticleBelow(Level level, BlockPos pos, RandomSource random, ParticleOptions particle) {
        double x = (double)pos.getX() + random.nextDouble();
        double y = (double)pos.getY() - 0.05;
        double z = (double)pos.getZ() + random.nextDouble();
        level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
    }

    public static void spawnParticleInBlock(LevelAccessor level, BlockPos pos, int count, ParticleOptions particle) {
        double spreadWidth = 0.5;
        BlockState blockState = level.getBlockState(pos);
        double spreadHeight = blockState.isAir() ? 1.0 : blockState.getShape(level, pos).max(Direction.Axis.Y);
        ParticleUtils.spawnParticles(level, pos, count, 0.5, spreadHeight, true, particle);
    }

    public static void spawnParticles(LevelAccessor level, BlockPos pos, int count, double spreadWidth, double spreadHeight, boolean allowFloatingParticles, ParticleOptions particle) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; ++i) {
            double xVelocity = random.nextGaussian() * 0.02;
            double yVelocity = random.nextGaussian() * 0.02;
            double zVelocity = random.nextGaussian() * 0.02;
            double spreadStartOffset = 0.5 - spreadWidth;
            double x = (double)pos.getX() + spreadStartOffset + random.nextDouble() * spreadWidth * 2.0;
            double y = (double)pos.getY() + random.nextDouble() * spreadHeight;
            double z = (double)pos.getZ() + spreadStartOffset + random.nextDouble() * spreadWidth * 2.0;
            if (!allowFloatingParticles && level.getBlockState(BlockPos.containing(x, y, z).below()).isAir()) continue;
            level.addParticle(particle, x, y, z, xVelocity, yVelocity, zVelocity);
        }
    }

    public static void spawnSmashAttackParticles(LevelAccessor level, BlockPos pos, int count) {
        double zd;
        double yd;
        double xd;
        double z;
        double y;
        double x;
        Vec3 center = Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0);
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.DUST_PILLAR, level.getBlockState(pos));
        int i = 0;
        while ((float)i < (float)count / 3.0f) {
            x = center.x + level.getRandom().nextGaussian() / 2.0;
            y = center.y;
            z = center.z + level.getRandom().nextGaussian() / 2.0;
            xd = level.getRandom().nextGaussian() * (double)0.2f;
            yd = level.getRandom().nextGaussian() * (double)0.2f;
            zd = level.getRandom().nextGaussian() * (double)0.2f;
            level.addParticle(particle, x, y, z, xd, yd, zd);
            ++i;
        }
        i = 0;
        while ((float)i < (float)count / 1.5f) {
            x = center.x + 3.5 * Math.cos(i) + level.getRandom().nextGaussian() / 2.0;
            y = center.y;
            z = center.z + 3.5 * Math.sin(i) + level.getRandom().nextGaussian() / 2.0;
            xd = level.getRandom().nextGaussian() * (double)0.05f;
            yd = level.getRandom().nextGaussian() * (double)0.05f;
            zd = level.getRandom().nextGaussian() * (double)0.05f;
            level.addParticle(particle, x, y, z, xd, yd, zd);
            ++i;
        }
    }
}

