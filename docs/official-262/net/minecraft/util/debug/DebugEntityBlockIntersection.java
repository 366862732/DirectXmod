/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.util.debug;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public enum DebugEntityBlockIntersection {
    IN_BLOCK(0, 0x6000FF00),
    IN_FLUID(1, 0x600000FF),
    IN_AIR(2, 0x60333333);

    private static final IntFunction<DebugEntityBlockIntersection> BY_ID;
    public static final StreamCodec<ByteBuf, DebugEntityBlockIntersection> STREAM_CODEC;
    private final int id;
    private final int color;

    private DebugEntityBlockIntersection(int id, int color) {
        this.id = id;
        this.color = color;
    }

    public int color() {
        return this.color;
    }

    static {
        BY_ID = ByIdMap.continuous(i -> i.id, DebugEntityBlockIntersection.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, i -> i.id);
    }
}

