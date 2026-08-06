/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 */
package net.minecraft.network.protocol.game;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.world.phys.Vec3;

public class VecDeltaCodec {
    private static final double TRUNCATION_STEPS = 4096.0;
    private Vec3 base = Vec3.ZERO;

    @VisibleForTesting
    static long encode(double input) {
        return Math.round(input * 4096.0);
    }

    @VisibleForTesting
    static double decode(long v) {
        return (double)v / 4096.0;
    }

    public Vec3 decode(long xa, long ya, long za) {
        if (xa == 0L && ya == 0L && za == 0L) {
            return this.base;
        }
        double x = xa == 0L ? this.base.x : VecDeltaCodec.decode(VecDeltaCodec.encode(this.base.x) + xa);
        double y = ya == 0L ? this.base.y : VecDeltaCodec.decode(VecDeltaCodec.encode(this.base.y) + ya);
        double z = za == 0L ? this.base.z : VecDeltaCodec.decode(VecDeltaCodec.encode(this.base.z) + za);
        return new Vec3(x, y, z);
    }

    public long encodeX(Vec3 pos) {
        return VecDeltaCodec.encode(pos.x) - VecDeltaCodec.encode(this.base.x);
    }

    public long encodeY(Vec3 pos) {
        return VecDeltaCodec.encode(pos.y) - VecDeltaCodec.encode(this.base.y);
    }

    public long encodeZ(Vec3 pos) {
        return VecDeltaCodec.encode(pos.z) - VecDeltaCodec.encode(this.base.z);
    }

    public Vec3 delta(Vec3 pos) {
        return pos.subtract(this.base);
    }

    public void setBase(Vec3 base) {
        this.base = base;
    }

    public Vec3 getBase() {
        return this.base;
    }
}

