/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.inventory;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public enum ContainerInput {
    PICKUP(0),
    QUICK_MOVE(1),
    SWAP(2),
    CLONE(3),
    THROW(4),
    QUICK_CRAFT(5),
    PICKUP_ALL(6);

    private static final IntFunction<ContainerInput> BY_ID;
    public static final StreamCodec<ByteBuf, ContainerInput> STREAM_CODEC;
    private final int id;

    private ContainerInput(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    static {
        BY_ID = ByIdMap.continuous(ContainerInput::id, ContainerInput.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ContainerInput::id);
    }
}

