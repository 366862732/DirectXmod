/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarLong {
    private static final int MAX_VARLONG_SIZE = 10;
    private static final int DATA_BITS_MASK = 127;
    private static final int CONTINUATION_BIT_MASK = 128;
    private static final int DATA_BITS_PER_BYTE = 7;

    public static int getByteSize(long value) {
        for (int i = 1; i < 10; ++i) {
            if ((value & -1L << i * 7) != 0L) continue;
            return i;
        }
        return 10;
    }

    public static boolean hasContinuationBit(byte in) {
        return (in & 0x80) == 128;
    }

    public static long read(ByteBuf input) {
        byte in;
        long out = 0L;
        int bytes = 0;
        do {
            in = input.readByte();
            out |= (long)(in & 0x7F) << bytes++ * 7;
            if (bytes <= 10) continue;
            throw new RuntimeException("VarLong too big");
        } while (VarLong.hasContinuationBit(in));
        return out;
    }

    public static ByteBuf write(ByteBuf output, long value) {
        while (true) {
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0L) {
                output.writeByte((int)value);
                return output;
            }
            output.writeByte((int)(value & 0x7FL) | 0x80);
            value >>>= 7;
        }
    }
}

