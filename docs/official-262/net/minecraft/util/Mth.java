/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.math.Fraction
 *  org.apache.commons.lang3.math.NumberUtils
 *  org.joml.Math
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.util;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.math.NumberUtils;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Mth {
    private static final long UUID_VERSION = 61440L;
    private static final long UUID_VERSION_TYPE_4 = 16384L;
    private static final long UUID_VARIANT = -4611686018427387904L;
    private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
    public static final float PI = (float)java.lang.Math.PI;
    public static final float HALF_PI = 1.5707964f;
    public static final float TWO_PI = (float)java.lang.Math.PI * 2;
    public static final float DEG_TO_RAD = (float)java.lang.Math.PI / 180;
    public static final float RAD_TO_DEG = 57.295776f;
    public static final float EPSILON = 1.0E-5f;
    public static final float SQRT_OF_TWO = Mth.sqrt(2.0f);
    public static final Vector3fc Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3fc X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3fc Z_AXIS = new Vector3f(0.0f, 0.0f, 1.0f);
    private static final int SIN_QUANTIZATION = 65536;
    private static final int SIN_MASK = 65535;
    private static final int COS_OFFSET = 16384;
    private static final double SIN_SCALE = 10430.378350470453;
    private static final float[] SIN = Util.make(new float[65536], sin -> {
        for (int i = 0; i < ((float[])sin).length; ++i) {
            sin[i] = (float)java.lang.Math.sin((double)i / 10430.378350470453);
        }
    });
    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};
    private static final double ONE_SIXTH = 0.16666666666666666;
    private static final int FRAC_EXP = 8;
    private static final int LUT_SIZE = 257;
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];

    public static float sin(double i) {
        return SIN[(int)((long)(i * 10430.378350470453) & 0xFFFFL)];
    }

    public static float cos(double i) {
        return SIN[(int)((long)(i * 10430.378350470453 + 16384.0) & 0xFFFFL)];
    }

    public static float sqrt(float x) {
        return (float)java.lang.Math.sqrt(x);
    }

    public static int floor(float v) {
        return (int)java.lang.Math.floor(v);
    }

    public static int floor(double v) {
        return (int)java.lang.Math.floor(v);
    }

    public static long lfloor(double v) {
        return (long)java.lang.Math.floor(v);
    }

    public static float abs(float v) {
        return java.lang.Math.abs(v);
    }

    public static int abs(int v) {
        return java.lang.Math.abs(v);
    }

    public static int ceil(float v) {
        return (int)java.lang.Math.ceil(v);
    }

    public static int ceil(double v) {
        return (int)java.lang.Math.ceil(v);
    }

    public static long ceilLong(double v) {
        return (long)java.lang.Math.ceil(v);
    }

    public static int clamp(int value, int min, int max) {
        return java.lang.Math.min(java.lang.Math.max(value, min), max);
    }

    public static long clamp(long value, long min, long max) {
        return java.lang.Math.min(java.lang.Math.max(value, min), max);
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return java.lang.Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        return java.lang.Math.min(value, max);
    }

    public static double clampedLerp(double factor, double min, double max) {
        if (factor < 0.0) {
            return min;
        }
        if (factor > 1.0) {
            return max;
        }
        return Mth.lerp(factor, min, max);
    }

    public static float clampedLerp(float factor, float min, float max) {
        if (factor < 0.0f) {
            return min;
        }
        if (factor > 1.0f) {
            return max;
        }
        return Mth.lerp(factor, min, max);
    }

    public static int absMax(int a, int b) {
        return java.lang.Math.max(java.lang.Math.abs(a), java.lang.Math.abs(b));
    }

    public static float absMax(float a, float b) {
        return java.lang.Math.max(java.lang.Math.abs(a), java.lang.Math.abs(b));
    }

    public static double absMax(double a, double b) {
        return java.lang.Math.max(java.lang.Math.abs(a), java.lang.Math.abs(b));
    }

    public static int chessboardDistance(int x0, int z0, int x1, int z1) {
        return Mth.absMax(x1 - x0, z1 - z0);
    }

    public static int floorDiv(int a, int b) {
        return java.lang.Math.floorDiv(a, b);
    }

    public static int nextInt(RandomSource random, int minInclusive, int maxInclusive) {
        if (minInclusive >= maxInclusive) {
            return minInclusive;
        }
        return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }

    public static float nextFloat(RandomSource random, float min, float max) {
        if (min >= max) {
            return min;
        }
        return random.nextFloat() * (max - min) + min;
    }

    public static double nextDouble(RandomSource random, double min, double max) {
        if (min >= max) {
            return min;
        }
        return random.nextDouble() * (max - min) + min;
    }

    public static boolean equal(float a, float b) {
        return java.lang.Math.abs(b - a) < 1.0E-5f;
    }

    public static boolean equal(double a, double b) {
        return java.lang.Math.abs(b - a) < (double)1.0E-5f;
    }

    public static int positiveModulo(int input, int mod) {
        return java.lang.Math.floorMod(input, mod);
    }

    public static float positiveModulo(float input, float mod) {
        return (input % mod + mod) % mod;
    }

    public static double positiveModulo(double input, double mod) {
        return (input % mod + mod) % mod;
    }

    public static boolean isMultipleOf(int dividend, int divisor) {
        return dividend % divisor == 0;
    }

    public static byte packDegrees(float angle) {
        return (byte)Mth.floor(angle * 256.0f / 360.0f);
    }

    public static float unpackDegrees(byte rot) {
        return (float)(rot * 360) / 256.0f;
    }

    public static int wrapDegrees(int angle) {
        int normalizedAngle = angle % 360;
        if (normalizedAngle >= 180) {
            normalizedAngle -= 360;
        }
        if (normalizedAngle < -180) {
            normalizedAngle += 360;
        }
        return normalizedAngle;
    }

    public static float wrapDegrees(long angle) {
        float normalizedAngle = angle % 360L;
        if (normalizedAngle >= 180.0f) {
            normalizedAngle -= 360.0f;
        }
        if (normalizedAngle < -180.0f) {
            normalizedAngle += 360.0f;
        }
        return normalizedAngle;
    }

    public static float wrapDegrees(float angle) {
        float normalizedAngle = angle % 360.0f;
        if (normalizedAngle >= 180.0f) {
            normalizedAngle -= 360.0f;
        }
        if (normalizedAngle < -180.0f) {
            normalizedAngle += 360.0f;
        }
        return normalizedAngle;
    }

    public static double wrapDegrees(double angle) {
        double normalizedAngle = angle % 360.0;
        if (normalizedAngle >= 180.0) {
            normalizedAngle -= 360.0;
        }
        if (normalizedAngle < -180.0) {
            normalizedAngle += 360.0;
        }
        return normalizedAngle;
    }

    public static float wrapDegrees90(float angle) {
        float normalizedAngle = angle % 90.0f;
        if (normalizedAngle >= 45.0f) {
            normalizedAngle -= 90.0f;
        }
        if (normalizedAngle < -45.0f) {
            normalizedAngle += 90.0f;
        }
        return normalizedAngle;
    }

    public static float degreesDifference(float fromAngle, float toAngle) {
        return Mth.wrapDegrees(toAngle - fromAngle);
    }

    public static float degreesDifferenceAbs(float angleA, float angleB) {
        return Mth.abs(Mth.degreesDifference(angleA, angleB));
    }

    public static float rotateIfNecessary(float baseAngle, float targetAngle, float maxAngleDiff) {
        float deltaAngle = Mth.degreesDifference(baseAngle, targetAngle);
        float deltaAngleClamped = Mth.clamp(deltaAngle, -maxAngleDiff, maxAngleDiff);
        return targetAngle - deltaAngleClamped;
    }

    public static float approach(float current, float target, float increment) {
        increment = Mth.abs(increment);
        if (current < target) {
            return Mth.clamp(current + increment, current, target);
        }
        return Mth.clamp(current - increment, target, current);
    }

    public static float approachDegrees(float current, float target, float increment) {
        float difference = Mth.degreesDifference(current, target);
        return Mth.approach(current, current + difference, increment);
    }

    public static int getInt(String input, int def) {
        return NumberUtils.toInt((String)input, (int)def);
    }

    public static int smallestEncompassingPowerOfTwo(int input) {
        int result = input - 1;
        result |= result >> 1;
        result |= result >> 2;
        result |= result >> 4;
        result |= result >> 8;
        result |= result >> 16;
        return result + 1;
    }

    public static int smallestSquareSide(int itemCount) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must be greater than or equal to zero");
        }
        return Mth.ceil(java.lang.Math.sqrt(itemCount));
    }

    public static boolean isPowerOfTwo(int input) {
        return input != 0 && (input & input - 1) == 0;
    }

    public static boolean isPowerOfTwo(long input) {
        return input != 0L && (input & input - 1L) == 0L;
    }

    public static int ceillog2(int input) {
        input = Mth.isPowerOfTwo(input) ? input : Mth.smallestEncompassingPowerOfTwo(input);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)input * 125613361L >> 27) & 0x1F];
    }

    public static int log2(int input) {
        return Mth.ceillog2(input) - (Mth.isPowerOfTwo(input) ? 0 : 1);
    }

    public static float frac(float num) {
        return num - (float)Mth.floor(num);
    }

    public static double frac(double num) {
        return num - (double)Mth.lfloor(num);
    }

    @Deprecated
    public static long getSeed(Vec3i vec) {
        return Mth.getSeed(vec.getX(), vec.getY(), vec.getZ());
    }

    @Deprecated
    public static long getSeed(int x, int y, int z) {
        long seed = (long)(x * 3129871) ^ (long)z * 116129781L ^ (long)y;
        seed = seed * seed * 42317861L + seed * 11L;
        return seed >> 16;
    }

    public static UUID createInsecureUUID(RandomSource random) {
        long most = random.nextLong() & 0xFFFFFFFFFFFF0FFFL | 0x4000L;
        long least = random.nextLong() & 0x3FFFFFFFFFFFFFFFL | Long.MIN_VALUE;
        return new UUID(most, least);
    }

    public static double inverseLerp(double value, double min, double max) {
        return (value - min) / (max - min);
    }

    public static float inverseLerp(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static boolean rayIntersectsAABB(Vec3 rayStart, Vec3 rayDir, AABB aabb) {
        double centerX = (aabb.minX + aabb.maxX) * 0.5;
        double boxExtentX = (aabb.maxX - aabb.minX) * 0.5;
        double diffX = rayStart.x - centerX;
        if (java.lang.Math.abs(diffX) > boxExtentX && diffX * rayDir.x >= 0.0) {
            return false;
        }
        double centerY = (aabb.minY + aabb.maxY) * 0.5;
        double boxExtentY = (aabb.maxY - aabb.minY) * 0.5;
        double diffY = rayStart.y - centerY;
        if (java.lang.Math.abs(diffY) > boxExtentY && diffY * rayDir.y >= 0.0) {
            return false;
        }
        double centerZ = (aabb.minZ + aabb.maxZ) * 0.5;
        double boxExtentZ = (aabb.maxZ - aabb.minZ) * 0.5;
        double diffZ = rayStart.z - centerZ;
        if (java.lang.Math.abs(diffZ) > boxExtentZ && diffZ * rayDir.z >= 0.0) {
            return false;
        }
        double andrewWooDiffX = java.lang.Math.abs(rayDir.x);
        double andrewWooDiffY = java.lang.Math.abs(rayDir.y);
        double andrewWooDiffZ = java.lang.Math.abs(rayDir.z);
        double f = rayDir.y * diffZ - rayDir.z * diffY;
        if (java.lang.Math.abs(f) > boxExtentY * andrewWooDiffZ + boxExtentZ * andrewWooDiffY) {
            return false;
        }
        f = rayDir.z * diffX - rayDir.x * diffZ;
        if (java.lang.Math.abs(f) > boxExtentX * andrewWooDiffZ + boxExtentZ * andrewWooDiffX) {
            return false;
        }
        f = rayDir.x * diffY - rayDir.y * diffX;
        return java.lang.Math.abs(f) < boxExtentX * andrewWooDiffY + boxExtentY * andrewWooDiffX;
    }

    public static double atan2(double y, double x) {
        boolean steep;
        boolean negX;
        boolean negY;
        double d2 = x * x + y * y;
        if (Double.isNaN(d2)) {
            return Double.NaN;
        }
        boolean bl = negY = y < 0.0;
        if (negY) {
            y = -y;
        }
        boolean bl2 = negX = x < 0.0;
        if (negX) {
            x = -x;
        }
        boolean bl3 = steep = y > x;
        if (steep) {
            double t = x;
            x = y;
            y = t;
        }
        double rinv = Mth.fastInvSqrt(d2);
        x *= rinv;
        double yp = FRAC_BIAS + (y *= rinv);
        int index = (int)Double.doubleToRawLongBits(yp);
        double phi = ASIN_TAB[index];
        double cPhi = COS_TAB[index];
        double sPhi = yp - FRAC_BIAS;
        double sd = y * cPhi - x * sPhi;
        double d = (6.0 + sd * sd) * sd * 0.16666666666666666;
        double theta = phi + d;
        if (steep) {
            theta = 1.5707963267948966 - theta;
        }
        if (negX) {
            theta = java.lang.Math.PI - theta;
        }
        if (negY) {
            theta = -theta;
        }
        return theta;
    }

    public static float invSqrt(float x) {
        return Math.invsqrt((float)x);
    }

    public static double invSqrt(double x) {
        return Math.invsqrt((double)x);
    }

    @Deprecated
    public static double fastInvSqrt(double x) {
        double xhalf = 0.5 * x;
        long i = Double.doubleToRawLongBits(x);
        i = 6910469410427058090L - (i >> 1);
        x = Double.longBitsToDouble(i);
        x *= 1.5 - xhalf * x * x;
        return x;
    }

    public static float fastInvCubeRoot(float x) {
        int i = Float.floatToIntBits(x);
        i = 1419967116 - i / 3;
        float y = Float.intBitsToFloat(i);
        y = 0.6666667f * y + 1.0f / (3.0f * y * y * x);
        y = 0.6666667f * y + 1.0f / (3.0f * y * y * x);
        return y;
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        return Mth.hsvToArgb(hue, saturation, value, 0);
    }

    public static int hsvToArgb(float hue, float saturation, float value, int alpha) {
        float green;
        float red;
        int h = (int)(hue * 6.0f) % 6;
        float f = hue * 6.0f - (float)h;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - f * saturation);
        float t = value * (1.0f - (1.0f - f) * saturation);
        return ARGB.color(alpha, Mth.clamp((int)(red * 255.0f), 0, 255), Mth.clamp((int)(green * 255.0f), 0, 255), Mth.clamp((int)((switch (h) {
            case 0 -> {
                red = value;
                green = t;
                yield p;
            }
            case 1 -> {
                red = q;
                green = value;
                yield p;
            }
            case 2 -> {
                red = p;
                green = value;
                yield t;
            }
            case 3 -> {
                red = p;
                green = q;
                yield value;
            }
            case 4 -> {
                red = t;
                green = p;
                yield value;
            }
            case 5 -> {
                red = value;
                green = p;
                yield q;
            }
            default -> throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }) * 255.0f), 0, 255));
    }

    public static int murmurHash3Mixer(int hash) {
        hash ^= hash >>> 16;
        hash *= -2048144789;
        hash ^= hash >>> 13;
        hash *= -1028477387;
        hash ^= hash >>> 16;
        return hash;
    }

    public static int binarySearch(int from, int to, IntPredicate condition) {
        int len = to - from;
        while (len > 0) {
            int half = len / 2;
            int middle = from + half;
            if (condition.test(middle)) {
                len = half;
                continue;
            }
            from = middle + 1;
            len -= half + 1;
        }
        return from;
    }

    public static int lerpInt(float alpha1, int p0, int p1) {
        return p0 + Mth.floor(alpha1 * (float)(p1 - p0));
    }

    public static int lerpDiscrete(float alpha1, int p0, int p1) {
        int delta = p1 - p0;
        return p0 + Mth.floor(alpha1 * (float)(delta - 1)) + (alpha1 > 0.0f ? 1 : 0);
    }

    public static float lerp(float alpha1, float p0, float p1) {
        return p0 + alpha1 * (p1 - p0);
    }

    public static Vec3 lerp(double alpha, Vec3 p1, Vec3 p2) {
        return new Vec3(Mth.lerp(alpha, p1.x, p2.x), Mth.lerp(alpha, p1.y, p2.y), Mth.lerp(alpha, p1.z, p2.z));
    }

    public static double lerp(double alpha1, double p0, double p1) {
        return p0 + alpha1 * (p1 - p0);
    }

    public static double lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) {
        return Mth.lerp(alpha2, Mth.lerp(alpha1, x00, x10), Mth.lerp(alpha1, x01, x11));
    }

    public static double lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111) {
        return Mth.lerp(alpha3, Mth.lerp2(alpha1, alpha2, x000, x100, x010, x110), Mth.lerp2(alpha1, alpha2, x001, x101, x011, x111));
    }

    public static float catmullrom(float alpha, float p0, float p1, float p2, float p3) {
        return 0.5f * (2.0f * p1 + (p2 - p0) * alpha + (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * alpha * alpha + (3.0f * p1 - p0 - 3.0f * p2 + p3) * alpha * alpha * alpha);
    }

    public static double smoothstep(double x) {
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    public static double smoothstepDerivative(double x) {
        return 30.0 * x * x * (x - 1.0) * (x - 1.0);
    }

    public static int sign(double number) {
        if (number == 0.0) {
            return 0;
        }
        return number > 0.0 ? 1 : -1;
    }

    public static float rotLerp(float a, float from, float to) {
        return from + a * Mth.wrapDegrees(to - from);
    }

    public static double rotLerp(double a, double from, double to) {
        return from + a * Mth.wrapDegrees(to - from);
    }

    public static float rotLerpRad(float a, float from, float to) {
        float diff;
        for (diff = to - from; diff < (float)(-java.lang.Math.PI); diff += (float)java.lang.Math.PI * 2) {
        }
        while (diff >= (float)java.lang.Math.PI) {
            diff -= (float)java.lang.Math.PI * 2;
        }
        return from + a * diff;
    }

    public static float triangleWave(float index, float period) {
        return (java.lang.Math.abs(index % period - period * 0.5f) - period * 0.25f) / (period * 0.25f);
    }

    public static float square(float x) {
        return x * x;
    }

    public static float cube(float x) {
        return x * x * x;
    }

    public static double square(double x) {
        return x * x;
    }

    public static int square(int x) {
        return x * x;
    }

    public static long square(long x) {
        return x * x;
    }

    public static double clampedMap(double value, double fromMin, double fromMax, double toMin, double toMax) {
        return Mth.clampedLerp(Mth.inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static float clampedMap(float value, float fromMin, float fromMax, float toMin, float toMax) {
        return Mth.clampedLerp(Mth.inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static double map(double value, double fromMin, double fromMax, double toMin, double toMax) {
        return Mth.lerp(Mth.inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static float map(float value, float fromMin, float fromMax, float toMin, float toMax) {
        return Mth.lerp(Mth.inverseLerp(value, fromMin, fromMax), toMin, toMax);
    }

    public static double wobble(double coord) {
        return coord + (2.0 * RandomSource.createThreadLocalInstance(Mth.floor(coord * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
    }

    public static int roundToward(int input, int multiple) {
        return Mth.positiveCeilDiv(input, multiple) * multiple;
    }

    public static long roundToward(long input, long multiple) {
        return Mth.positiveCeilDiv(input, multiple) * multiple;
    }

    public static int positiveCeilDiv(int input, int divisor) {
        return -java.lang.Math.floorDiv(-input, divisor);
    }

    public static long positiveCeilDiv(long input, long divisor) {
        return -java.lang.Math.floorDiv(-input, divisor);
    }

    public static int randomBetweenInclusive(RandomSource random, int min, int maxInclusive) {
        return random.nextInt(maxInclusive - min + 1) + min;
    }

    public static float randomBetween(RandomSource random, float min, float maxExclusive) {
        return random.nextFloat() * (maxExclusive - min) + min;
    }

    public static float normal(RandomSource random, float mean, float deviation) {
        return mean + (float)random.nextGaussian() * deviation;
    }

    public static double lengthSquared(double x, double y) {
        return x * x + y * y;
    }

    public static double length(double x, double y) {
        return java.lang.Math.sqrt(Mth.lengthSquared(x, y));
    }

    public static float length(float x, float y) {
        return (float)java.lang.Math.sqrt(Mth.lengthSquared(x, y));
    }

    public static double lengthSquared(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    public static double length(double x, double y, double z) {
        return java.lang.Math.sqrt(Mth.lengthSquared(x, y, z));
    }

    public static float lengthSquared(float x, float y, float z) {
        return x * x + y * y + z * z;
    }

    public static int quantize(double value, int quantizeResolution) {
        return Mth.floor(value / (double)quantizeResolution) * quantizeResolution;
    }

    public static IntStream outFromOrigin(int origin, int lowerBound, int upperBound) {
        return Mth.outFromOrigin(origin, lowerBound, upperBound, 1);
    }

    public static IntStream outFromOrigin(int origin, int lowerBound, int upperBound, int stepSize) {
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "upperBound %d expected to be > lowerBound %d", upperBound, lowerBound));
        }
        if (stepSize < 1) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "step size expected to be >= 1, was %d", stepSize));
        }
        int clampedOrigin = Mth.clamp(origin, lowerBound, upperBound);
        return IntStream.iterate(clampedOrigin, cursor -> {
            int currentDistance = java.lang.Math.abs(clampedOrigin - cursor);
            return clampedOrigin - currentDistance >= lowerBound || clampedOrigin + currentDistance <= upperBound;
        }, cursor -> {
            int attemptedStep;
            boolean canMovePositive;
            boolean previousWasNegative = cursor <= clampedOrigin;
            int currentDistance = java.lang.Math.abs(clampedOrigin - cursor);
            boolean bl = canMovePositive = clampedOrigin + currentDistance + stepSize <= upperBound;
            if (!(previousWasNegative && canMovePositive || (attemptedStep = clampedOrigin - currentDistance - (previousWasNegative ? stepSize : 0)) < lowerBound)) {
                return attemptedStep;
            }
            return clampedOrigin + currentDistance + stepSize;
        });
    }

    public static Quaternionf rotationAroundAxis(Vector3fc axis, Quaternionf rotation, Quaternionf result) {
        float projectedLength = axis.dot(rotation.x, rotation.y, rotation.z);
        return result.set(axis.x() * projectedLength, axis.y() * projectedLength, axis.z() * projectedLength, rotation.w).normalize();
    }

    public static int mulAndTruncate(Fraction fraction, int factor) {
        return fraction.getNumerator() * factor / fraction.getDenominator();
    }

    static {
        for (int ind = 0; ind < 257; ++ind) {
            double v = (double)ind / 256.0;
            double asinv = java.lang.Math.asin(v);
            Mth.COS_TAB[ind] = java.lang.Math.cos(asinv);
            Mth.ASIN_TAB[ind] = asinv;
        }
    }
}

