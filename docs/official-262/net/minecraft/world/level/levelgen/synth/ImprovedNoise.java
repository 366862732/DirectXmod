/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 */
package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class ImprovedNoise {
    private static final float SHIFT_UP_EPSILON = 1.0E-7f;
    private final byte[] p;
    public final double xo;
    public final double yo;
    public final double zo;

    public ImprovedNoise(RandomSource random) {
        int i;
        this.xo = random.nextDouble() * 256.0;
        this.yo = random.nextDouble() * 256.0;
        this.zo = random.nextDouble() * 256.0;
        this.p = new byte[256];
        for (i = 0; i < 256; ++i) {
            this.p[i] = (byte)i;
        }
        for (i = 0; i < 256; ++i) {
            int offset = random.nextInt(256 - i);
            byte tmp = this.p[i];
            this.p[i] = this.p[i + offset];
            this.p[i + offset] = tmp;
        }
    }

    public double noise(double _x, double _y, double _z) {
        return this.noise(_x, _y, _z, 0.0, 0.0);
    }

    @Deprecated
    public double noise(double _x, double _y, double _z, double yScale, double yFudge) {
        double yrFudge;
        double x = _x + this.xo;
        double y = _y + this.yo;
        double z = _z + this.zo;
        int xf = Mth.floor(x);
        int yf = Mth.floor(y);
        int zf = Mth.floor(z);
        double xr = x - (double)xf;
        double yr = y - (double)yf;
        double zr = z - (double)zf;
        if (yScale != 0.0) {
            double fudgeLimit = yFudge >= 0.0 && yFudge < yr ? yFudge : yr;
            yrFudge = (double)Mth.floor(fudgeLimit / yScale + (double)1.0E-7f) * yScale;
        } else {
            yrFudge = 0.0;
        }
        return this.sampleAndLerp(xf, yf, zf, xr, yr - yrFudge, zr, yr);
    }

    public double noiseWithDerivative(double _x, double _y, double _z, double[] derivativeOut) {
        double x = _x + this.xo;
        double y = _y + this.yo;
        double z = _z + this.zo;
        int xf = Mth.floor(x);
        int yf = Mth.floor(y);
        int zf = Mth.floor(z);
        double xr = x - (double)xf;
        double yr = y - (double)yf;
        double zr = z - (double)zf;
        return this.sampleWithDerivative(xf, yf, zf, xr, yr, zr, derivativeOut);
    }

    private static double gradDot(int hash, double x, double y, double z) {
        return SimplexNoise.dot(SimplexNoise.GRADIENT[hash & 0xF], x, y, z);
    }

    private int p(int x) {
        return this.p[x & 0xFF] & 0xFF;
    }

    private double sampleAndLerp(int x, int y, int z, double xr, double yr, double zr, double yrOriginal) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);
        double d000 = ImprovedNoise.gradDot(this.p(xy00 + z), xr, yr, zr);
        double d100 = ImprovedNoise.gradDot(this.p(xy10 + z), xr - 1.0, yr, zr);
        double d010 = ImprovedNoise.gradDot(this.p(xy01 + z), xr, yr - 1.0, zr);
        double d110 = ImprovedNoise.gradDot(this.p(xy11 + z), xr - 1.0, yr - 1.0, zr);
        double d001 = ImprovedNoise.gradDot(this.p(xy00 + z + 1), xr, yr, zr - 1.0);
        double d101 = ImprovedNoise.gradDot(this.p(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
        double d011 = ImprovedNoise.gradDot(this.p(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
        double d111 = ImprovedNoise.gradDot(this.p(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);
        double xAlpha = Mth.smoothstep(xr);
        double yAlpha = Mth.smoothstep(yrOriginal);
        double zAlpha = Mth.smoothstep(zr);
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    private double sampleWithDerivative(int x, int y, int z, double xr, double yr, double zr, double[] derivativeOut) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);
        int p000 = this.p(xy00 + z);
        int p100 = this.p(xy10 + z);
        int p010 = this.p(xy01 + z);
        int p110 = this.p(xy11 + z);
        int p001 = this.p(xy00 + z + 1);
        int p101 = this.p(xy10 + z + 1);
        int p011 = this.p(xy01 + z + 1);
        int p111 = this.p(xy11 + z + 1);
        int[] g000 = SimplexNoise.GRADIENT[p000 & 0xF];
        int[] g100 = SimplexNoise.GRADIENT[p100 & 0xF];
        int[] g010 = SimplexNoise.GRADIENT[p010 & 0xF];
        int[] g110 = SimplexNoise.GRADIENT[p110 & 0xF];
        int[] g001 = SimplexNoise.GRADIENT[p001 & 0xF];
        int[] g101 = SimplexNoise.GRADIENT[p101 & 0xF];
        int[] g011 = SimplexNoise.GRADIENT[p011 & 0xF];
        int[] g111 = SimplexNoise.GRADIENT[p111 & 0xF];
        double d000 = SimplexNoise.dot(g000, xr, yr, zr);
        double d100 = SimplexNoise.dot(g100, xr - 1.0, yr, zr);
        double d010 = SimplexNoise.dot(g010, xr, yr - 1.0, zr);
        double d110 = SimplexNoise.dot(g110, xr - 1.0, yr - 1.0, zr);
        double d001 = SimplexNoise.dot(g001, xr, yr, zr - 1.0);
        double d101 = SimplexNoise.dot(g101, xr - 1.0, yr, zr - 1.0);
        double d011 = SimplexNoise.dot(g011, xr, yr - 1.0, zr - 1.0);
        double d111 = SimplexNoise.dot(g111, xr - 1.0, yr - 1.0, zr - 1.0);
        double xAlpha = Mth.smoothstep(xr);
        double yAlpha = Mth.smoothstep(yr);
        double zAlpha = Mth.smoothstep(zr);
        double d1x = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[0], g100[0], g010[0], g110[0], g001[0], g101[0], g011[0], g111[0]);
        double d1y = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[1], g100[1], g010[1], g110[1], g001[1], g101[1], g011[1], g111[1]);
        double d1z = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[2], g100[2], g010[2], g110[2], g001[2], g101[2], g011[2], g111[2]);
        double d2x = Mth.lerp2(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011);
        double d2y = Mth.lerp2(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101);
        double d2z = Mth.lerp2(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110);
        double xSD = Mth.smoothstepDerivative(xr);
        double ySD = Mth.smoothstepDerivative(yr);
        double zSD = Mth.smoothstepDerivative(zr);
        double dX = d1x + xSD * d2x;
        double dY = d1y + ySD * d2y;
        double dZ = d1z + zSD * d2z;
        derivativeOut[0] = derivativeOut[0] + dX;
        derivativeOut[1] = derivativeOut[1] + dY;
        derivativeOut[2] = derivativeOut[2] + dZ;
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder sb) {
        NoiseUtils.parityNoiseOctaveConfigString(sb, this.xo, this.yo, this.zo, this.p);
    }
}

