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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CanyonCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

public class CanyonWorldCarver
extends WorldCarver<CanyonCarverConfiguration> {
    public CanyonWorldCarver(Codec<CanyonCarverConfiguration> configurationFactory) {
        super(configurationFactory);
    }

    @Override
    public boolean isStartChunk(CanyonCarverConfiguration configuration, RandomSource random) {
        return random.nextFloat() <= configuration.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CanyonCarverConfiguration configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, RandomSource random, Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask mask) {
        int maxDistance = (this.getRange() * 2 - 1) * 16;
        double x = sourceChunkPos.getBlockX(random.nextInt(16));
        int y = configuration.y.sample(random, context);
        double z = sourceChunkPos.getBlockZ(random.nextInt(16));
        float horizontalRotation = random.nextFloat() * ((float)Math.PI * 2);
        float verticalRotation = configuration.verticalRotation.sample(random);
        double yScale = configuration.yScale.sample(random);
        float thickness = configuration.shape.thickness.sample(random);
        int distance = (int)((float)maxDistance * configuration.shape.distanceFactor.sample(random));
        boolean initialStep = false;
        this.doCarve(context, configuration, chunk, biomeGetter, random.nextLong(), aquifer, x, y, z, thickness, horizontalRotation, verticalRotation, 0, distance, yScale, mask);
        return true;
    }

    private void doCarve(CarvingContext context, CanyonCarverConfiguration configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, long tunnelSeed, Aquifer aquifer, double x, double y, double z, float thickness, float horizontalRotation, float verticalRotation, int step, int distance, double yScale, CarvingMask mask) {
        RandomSource random = RandomSource.createThreadLocalInstance(tunnelSeed);
        float[] widthFactorPerHeight = this.initWidthFactors(context, configuration, random);
        float yRota = 0.0f;
        float xRota = 0.0f;
        for (int currentStep = step; currentStep < distance; ++currentStep) {
            double horizontalRadius = 1.5 + (double)(Mth.sin((float)currentStep * (float)Math.PI / (float)distance) * thickness);
            double verticalRadius = horizontalRadius * yScale;
            horizontalRadius *= (double)configuration.shape.horizontalRadiusFactor.sample(random);
            verticalRadius = this.updateVerticalRadius(configuration, random, verticalRadius, distance, currentStep);
            float xc = Mth.cos(verticalRotation);
            float xs = Mth.sin(verticalRotation);
            x += (double)(Mth.cos(horizontalRotation) * xc);
            y += (double)xs;
            z += (double)(Mth.sin(horizontalRotation) * xc);
            verticalRotation *= 0.7f;
            verticalRotation += xRota * 0.05f;
            horizontalRotation += yRota * 0.05f;
            xRota *= 0.8f;
            yRota *= 0.5f;
            xRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0f;
            yRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0f;
            if (random.nextInt(4) == 0) continue;
            if (!CanyonWorldCarver.canReach(chunk.getPos(), x, z, currentStep, distance, thickness)) {
                return;
            }
            this.carveEllipsoid(context, configuration, chunk, biomeGetter, aquifer, x, y, z, horizontalRadius, verticalRadius, mask, (context1, xd, yd, zd, y1) -> this.shouldSkip(context1, widthFactorPerHeight, xd, yd, zd, y1));
        }
    }

    private float[] initWidthFactors(CarvingContext context, CanyonCarverConfiguration configuration, RandomSource random) {
        int depth = context.getGenDepth();
        float[] widthFactorPerHeight = new float[depth];
        float widthFactor = 1.0f;
        for (int yIndex = 0; yIndex < depth; ++yIndex) {
            if (yIndex == 0 || random.nextInt(configuration.shape.widthSmoothness) == 0) {
                widthFactor = 1.0f + random.nextFloat() * random.nextFloat();
            }
            widthFactorPerHeight[yIndex] = widthFactor * widthFactor;
        }
        return widthFactorPerHeight;
    }

    private double updateVerticalRadius(CanyonCarverConfiguration configuration, RandomSource random, double verticalRadius, float distance, float currentStep) {
        float verticalMultiplier = 1.0f - Mth.abs(0.5f - currentStep / distance) * 2.0f;
        float factor = configuration.shape.verticalRadiusDefaultFactor + configuration.shape.verticalRadiusCenterFactor * verticalMultiplier;
        return (double)factor * verticalRadius * (double)Mth.randomBetween(random, 0.75f, 1.0f);
    }

    private boolean shouldSkip(CarvingContext context, float[] widthFactorPerHeight, double xd, double yd, double zd, int y) {
        int yIndex = y - context.getMinGenY();
        return (xd * xd + zd * zd) * (double)widthFactorPerHeight[yIndex - 1] + yd * yd / 6.0 >= 1.0;
    }
}

