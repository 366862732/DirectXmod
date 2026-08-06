/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

public class CaveWorldCarver
extends WorldCarver<CaveCarverConfiguration> {
    public CaveWorldCarver(Codec<CaveCarverConfiguration> configurationFactory) {
        super(configurationFactory);
    }

    @Override
    public boolean isStartChunk(CaveCarverConfiguration configuration, RandomSource random) {
        return random.nextFloat() <= configuration.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, RandomSource random, Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask mask) {
        int maxDistance = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
        int caveCount = random.nextInt(random.nextInt(random.nextInt(this.getCaveBound()) + 1) + 1);
        for (int cave = 0; cave < caveCount; ++cave) {
            double x = sourceChunkPos.getBlockX(random.nextInt(16));
            double y = configuration.y.sample(random, context);
            double z = sourceChunkPos.getBlockZ(random.nextInt(16));
            double horizontalRadiusMultiplier = configuration.horizontalRadiusMultiplier.sample(random);
            double verticalRadiusMultiplier = configuration.verticalRadiusMultiplier.sample(random);
            double floorLevel = configuration.floorLevel.sample(random);
            WorldCarver.CarveSkipChecker skipChecker = (c, xd, yd, zd, worldY) -> CaveWorldCarver.shouldSkip(xd, yd, zd, floorLevel);
            int tunnels = 1;
            if (random.nextInt(4) == 0) {
                double yScale = configuration.yScale.sample(random);
                float thickness = 1.0f + random.nextFloat() * 6.0f;
                this.createRoom(context, configuration, chunk, biomeGetter, aquifer, x, y, z, thickness, yScale, mask, skipChecker);
                tunnels += random.nextInt(4);
            }
            for (int i = 0; i < tunnels; ++i) {
                float horizontalRotation = random.nextFloat() * ((float)Math.PI * 2);
                float verticalRotation = (random.nextFloat() - 0.5f) / 4.0f;
                float thickness = this.getThickness(random);
                int distance = maxDistance - random.nextInt(maxDistance / 4);
                boolean initialStep = false;
                this.createTunnel(context, configuration, chunk, biomeGetter, random.nextLong(), aquifer, x, y, z, horizontalRadiusMultiplier, verticalRadiusMultiplier, thickness, horizontalRotation, verticalRotation, 0, distance, this.getYScale(), mask, skipChecker);
            }
        }
        return true;
    }

    protected int getCaveBound() {
        return 15;
    }

    protected float getThickness(RandomSource random) {
        float thickness = random.nextFloat() * 2.0f + random.nextFloat();
        if (random.nextInt(10) == 0) {
            thickness *= random.nextFloat() * random.nextFloat() * 3.0f + 1.0f;
        }
        return thickness;
    }

    protected double getYScale() {
        return 1.0;
    }

    protected void createRoom(CarvingContext context, CaveCarverConfiguration configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, Aquifer aquifer, double x, double y, double z, float thickness, double yScale, CarvingMask mask, WorldCarver.CarveSkipChecker skipChecker) {
        double horizontalRadius = 1.5 + (double)(Mth.sin(1.5707963705062866) * thickness);
        double verticalRadius = horizontalRadius * yScale;
        this.carveEllipsoid(context, configuration, chunk, biomeGetter, aquifer, x + 1.0, y, z, horizontalRadius, verticalRadius, mask, skipChecker);
    }

    protected void createTunnel(CarvingContext context, CaveCarverConfiguration configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, long tunnelSeed, Aquifer aquifer, double x, double y, double z, double horizontalRadiusMultiplier, double verticalRadiusMultiplier, float thickness, float horizontalRotation, float verticalRotation, int step, int dist, double yScale, CarvingMask mask, WorldCarver.CarveSkipChecker skipChecker) {
        RandomSource random = RandomSource.createThreadLocalInstance(tunnelSeed);
        int splitPoint = random.nextInt(dist / 2) + dist / 4;
        boolean steep = random.nextInt(6) == 0;
        float yRota = 0.0f;
        float xRota = 0.0f;
        for (int currentStep = step; currentStep < dist; ++currentStep) {
            double horizontalRadius = 1.5 + (double)(Mth.sin((float)Math.PI * (float)currentStep / (float)dist) * thickness);
            double verticalRadius = horizontalRadius * yScale;
            float cosX = Mth.cos(verticalRotation);
            x += (double)(Mth.cos(horizontalRotation) * cosX);
            y += (double)Mth.sin(verticalRotation);
            z += (double)(Mth.sin(horizontalRotation) * cosX);
            verticalRotation *= steep ? 0.92f : 0.7f;
            verticalRotation += xRota * 0.1f;
            horizontalRotation += yRota * 0.1f;
            xRota *= 0.9f;
            yRota *= 0.75f;
            xRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0f;
            yRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0f;
            if (currentStep == splitPoint && thickness > 1.0f) {
                this.createTunnel(context, configuration, chunk, biomeGetter, random.nextLong(), aquifer, x, y, z, horizontalRadiusMultiplier, verticalRadiusMultiplier, random.nextFloat() * 0.5f + 0.5f, horizontalRotation - 1.5707964f, verticalRotation / 3.0f, currentStep, dist, 1.0, mask, skipChecker);
                this.createTunnel(context, configuration, chunk, biomeGetter, random.nextLong(), aquifer, x, y, z, horizontalRadiusMultiplier, verticalRadiusMultiplier, random.nextFloat() * 0.5f + 0.5f, horizontalRotation + 1.5707964f, verticalRotation / 3.0f, currentStep, dist, 1.0, mask, skipChecker);
                return;
            }
            if (random.nextInt(4) == 0) continue;
            if (!CaveWorldCarver.canReach(chunk.getPos(), x, z, currentStep, dist, thickness)) {
                return;
            }
            this.carveEllipsoid(context, configuration, chunk, biomeGetter, aquifer, x, y, z, horizontalRadius * horizontalRadiusMultiplier, verticalRadius * verticalRadiusMultiplier, mask, skipChecker);
        }
    }

    private static boolean shouldSkip(double xd, double yd, double zd, double floorLevel) {
        if (yd <= floorLevel) {
            return true;
        }
        return xd * xd + yd * yd + zd * zd >= 1.0;
    }
}

