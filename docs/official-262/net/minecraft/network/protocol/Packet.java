/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.network.protocol.PacketType;

public interface Packet<T extends PacketListener> {
    public PacketType<? extends Packet<T>> type();

    public void handle(T var1);

    default public boolean isSkippable() {
        return false;
    }

    default public boolean isTerminal() {
        return false;
    }

    public static <B extends ByteBuf, T extends Packet<?>> StreamCodec<B, T> codec(StreamMemberEncoder<B, T> writer, StreamDecoder<B, T> reader) {
        return StreamCodec.ofMember(writer, reader);
    }
}

