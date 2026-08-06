/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DiscardedPayload(Identifier id) implements CustomPacketPayload
{
    public static <T extends FriendlyByteBuf> StreamCodec<T, DiscardedPayload> codec(Identifier id, int maxPayloadSize) {
        return CustomPacketPayload.codec((T payload, B buf) -> {}, (B buf) -> {
            int length = buf.readableBytes();
            if (length < 0 || length > maxPayloadSize) {
                throw new IllegalArgumentException("Payload may not be larger than " + maxPayloadSize + " bytes");
            }
            buf.skipBytes(length);
            return new DiscardedPayload(id);
        });
    }

    public CustomPacketPayload.Type<DiscardedPayload> type() {
        return new CustomPacketPayload.Type<DiscardedPayload>(this.id);
    }
}

