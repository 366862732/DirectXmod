/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Hashing
 */
package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

public class BiomeManager {
    public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
    private static final int ZOOM_BITS = 2;
    private static final int ZOOM = 4;
    private static final int ZOOM_MASK = 3;
    private final NoiseBiomeSource noiseBiomeSource;
    private final long biomeZoomSeed;

    public BiomeManager(NoiseBiomeSource noiseBiomeSource, long seed) {
        this.noiseBiomeSource = noiseBiomeSource;
        this.biomeZoomSeed = seed;
    }

    public static long obfuscateSeed(long seed) {
        return Hashing.sha256().hashLong(seed).asLong();
    }

    public BiomeManager withDifferentSource(NoiseBiomeSource biomeSource) {
        return new BiomeManager(biomeSource, this.biomeZoomSeed);
    }

    public Holder<Biome> getBiome(BlockPos pos) {
        int absX = pos.getX() - 2;
        int absY = pos.getY() - 2;
        int absZ = pos.getZ() - 2;
        int parentX = absX >> 2;
        int parentY = absY >> 2;
        int parentZ = absZ >> 2;
        double fractX = (double)(absX & 3) / 4.0;
        double fractY = (double)(absY & 3) / 4.0;
        double fractZ = (double)(absZ & 3) / 4.0;
        int minI = 0;
        double minFiddledDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 8; ++i) {
            double distanceZ;
            double distanceY;
            double distanceX;
            boolean zEven;
            int cornerZ;
            boolean yEven;
            int cornerY;
            boolean xEven = (i & 4) == 0;
            int cornerX = xEven ? parentX : parentX + 1;
            double next = BiomeManager.getFiddledDistance(this.biomeZoomSeed, cornerX, cornerY = (yEven = (i & 2) == 0) ? parentY : parentY + 1, cornerZ = (zEven = (i & 1) == 0) ? parentZ : parentZ + 1, distanceX = xEven ? fractX : fractX - 1.0, distanceY = yEven ? fractY : fractY - 1.0, distanceZ = zEven ? fractZ : fractZ - 1.0);
            if (!(minFiddledDistance > next)) continue;
            minI = i;
            minFiddledDistance = next;
        }
        int biomeX = (minI & 4) == 0 ? parentX : parentX + 1;
        int biomeY = (minI & 2) == 0 ? parentY : parentY + 1;
        int biomeZ = (minI & 1) == 0 ? parentZ : parentZ + 1;
        return this.noiseBiomeSource.getNoiseBiome(biomeX, biomeY, biomeZ);
    }

    public Holder<Biome> getNoiseBiomeAtPosition(double x, double y, double z) {
        int quartX = QuartPos.fromBlock(Mth.floor(x));
        int quartY = QuartPos.fromBlock(Mth.floor(y));
        int quartZ = QuartPos.fromBlock(Mth.floor(z));
        return this.getNoiseBiomeAtQuart(quartX, quartY, quartZ);
    }

    public Holder<Biome> getNoiseBiomeAtPosition(BlockPos blockPos) {
        int quartX = QuartPos.fromBlock(blockPos.getX());
        int quartY = QuartPos.fromBlock(blockPos.getY());
        int quartZ = QuartPos.fromBlock(blockPos.getZ());
        return this.getNoiseBiomeAtQuart(quartX, quartY, quartZ);
    }

    public Holder<Biome> getNoiseBiomeAtQuart(int quartX, int quartY, int quartZ) {
        return this.noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
    }

    private static double getFiddledDistance(long seed, int xRandom, int yRandom, int zRandom, double distanceX, double distanceY, double distanceZ) {
        long rval = seed;
        rval = LinearCongruentialGenerator.next(rval, xRandom);
        rval = LinearCongruentialGenerator.next(rval, yRandom);
        rval = LinearCongruentialGenerator.next(rval, zRandom);
        rval = LinearCongruentialGenerator.next(rval, xRandom);
        rval = LinearCongruentialGenerator.next(rval, yRandom);
        rval = LinearCongruentialGenerator.next(rval, zRandom);
        double fiddleX = BiomeManager.getFiddle(rval);
        rval = LinearCongruentialGenerator.next(rval, seed);
        double fiddleY = BiomeManager.getFiddle(rval);
        rval = LinearCongruentialGenerator.next(rval, seed);
        double fiddleZ = BiomeManager.getFiddle(rval);
        return Mth.square(distanceZ + fiddleZ) + Mth.square(distanceY + fiddleY) + Mth.square(distanceX + fiddleX);
    }

    private static double getFiddle(long rval) {
        double uniform = (double)Math.floorMod(rval >> 24, 1024) / 1024.0;
        return (uniform - 0.5) * 0.9;
    }

    public static interface NoiseBiomeSource {
        public Holder<Biome> getNoiseBiome(int var1, int var2, int var3);
    }
}

