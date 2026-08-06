/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.geom.builders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record UVPair(float u, float v) {
    @Override
    public String toString() {
        return "(" + this.u + "," + this.v + ")";
    }

    public static long pack(float u, float v) {
        long high = (long)Float.floatToIntBits(u) & 0xFFFFFFFFL;
        long low = (long)Float.floatToIntBits(v) & 0xFFFFFFFFL;
        return high << 32 | low;
    }

    public static float unpackU(long packedUV) {
        int bits = (int)(packedUV >> 32);
        return Float.intBitsToFloat(bits);
    }

    public static float unpackV(long packedUV) {
        return Float.intBitsToFloat((int)packedUV);
    }
}

