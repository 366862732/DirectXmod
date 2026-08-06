/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum SwingAnimationType implements StringRepresentable
{
    NONE(0, "none"),
    WHACK(1, "whack"),
    STAB(2, "stab");

    private static final IntFunction<SwingAnimationType> BY_ID;
    public static final Codec<SwingAnimationType> CODEC;
    public static final StreamCodec<ByteBuf, SwingAnimationType> STREAM_CODEC;
    private final int id;
    private final String name;

    private SwingAnimationType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        BY_ID = ByIdMap.continuous(SwingAnimationType::getId, SwingAnimationType.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        CODEC = StringRepresentable.fromEnum(SwingAnimationType::values);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, SwingAnimationType::getId);
    }
}

