/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LpVec3 {
    private static final int DATA_BITS = 15;
    private static final int DATA_BITS_MASK = Short.MAX_VALUE;
    private static final double MAX_QUANTIZED_VALUE = 32766.0;
    private static final int SCALE_BITS = 2;
    private static final int SCALE_BITS_MASK = 3;
    private static final int CONTINUATION_FLAG = 4;
    private static final int X_OFFSET = 3;
    private static final int Y_OFFSET = 18;
    private static final int Z_OFFSET = 33;
    public static final double ABS_MAX_VALUE = 1.7179869183E10;
    public static final double ABS_MIN_VALUE = 3.051944088384301E-5;

    public static boolean hasContinuationBit(int in) {
        return (in & 4) == 4;
    }

    public static Vec3 read(ByteBuf input) {
        short lowest = input.readUnsignedByte();
        if (lowest == 0) {
            return Vec3.ZERO;
        }
        short middle = input.readUnsignedByte();
        long highest = input.readUnsignedInt();
        long buffer = highest << 16 | (long)(middle << 8) | (long)lowest;
        long scale = lowest & 3;
        if (LpVec3.hasContinuationBit(lowest)) {
            scale |= ((long)VarInt.read(input) & 0xFFFFFFFFL) << 2;
        }
        return new Vec3(LpVec3.unpack(buffer >> 3) * (double)scale, LpVec3.unpack(buffer >> 18) * (double)scale, LpVec3.unpack(buffer >> 33) * (double)scale);
    }

    public static void write(ByteBuf output, Vec3 value) {
        double z;
        double y;
        double x = LpVec3.sanitize(value.x);
        double chessboardLength = Mth.absMax(x, Mth.absMax(y = LpVec3.sanitize(value.y), z = LpVec3.sanitize(value.z)));
        if (chessboardLength < 3.051944088384301E-5) {
            output.writeByte(0);
            return;
        }
        long scale = Mth.ceilLong(chessboardLength);
        boolean isPartial = (scale & 3L) != scale;
        long markers = isPartial ? scale & 3L | 4L : scale;
        long xn = LpVec3.pack(x / (double)scale) << 3;
        long yn = LpVec3.pack(y / (double)scale) << 18;
        long zn = LpVec3.pack(z / (double)scale) << 33;
        long buffer = markers | xn | yn | zn;
        output.writeByte((int)((byte)buffer));
        output.writeByte((int)((byte)(buffer >> 8)));
        output.writeInt((int)(buffer >> 16));
        if (isPartial) {
            VarInt.write(output, (int)(scale >> 2));
        }
    }

    private static double sanitize(double value) {
        return Double.isNaN(value) ? 0.0 : Math.clamp(value, -1.7179869183E10, 1.7179869183E10);
    }

    private static long pack(double value) {
        return Math.round((value * 0.5 + 0.5) * 32766.0);
    }

    private static double unpack(long value) {
        return Math.min((double)(value & 0x7FFFL), 32766.0) * 2.0 / 32766.0 - 1.0;
    }
}

