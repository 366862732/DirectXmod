/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarInt {
    public static final int MAX_VARINT_SIZE = 5;
    private static final int DATA_BITS_MASK = 127;
    private static final int CONTINUATION_BIT_MASK = 128;
    private static final int DATA_BITS_PER_BYTE = 7;

    public static int getByteSize(int value) {
        for (int i = 1; i < 5; ++i) {
            if ((value & -1 << i * 7) != 0) continue;
            return i;
        }
        return 5;
    }

    public static boolean hasContinuationBit(byte in) {
        return (in & 0x80) == 128;
    }

    public static int read(ByteBuf input) {
        byte in;
        int out = 0;
        int bytes = 0;
        do {
            in = input.readByte();
            out |= (in & 0x7F) << bytes++ * 7;
            if (bytes <= 5) continue;
            throw new RuntimeException("VarInt too big");
        } while (VarInt.hasContinuationBit(in));
        return out;
    }

    public static ByteBuf write(ByteBuf output, int value) {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                output.writeByte(value);
                return output;
            }
            output.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
    }
}

