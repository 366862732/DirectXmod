/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.common.custom;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.resources.Identifier;

public interface CustomPacketPayload {
    public Type<? extends CustomPacketPayload> type();

    public static <B extends ByteBuf, T extends CustomPacketPayload> StreamCodec<B, T> codec(StreamMemberEncoder<B, T> writer, StreamDecoder<B, T> reader) {
        return StreamCodec.ofMember(writer, reader);
    }

    public static <T extends CustomPacketPayload> Type<T> createType(String id) {
        return new Type(Identifier.withDefaultNamespace(id));
    }

    public static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> codec(final FallbackProvider<B> fallback, List<TypeAndCodec<? super B, ?>> types) {
        final Map<Identifier, StreamCodec> idToType = types.stream().collect(Collectors.toUnmodifiableMap(t -> t.type().id(), TypeAndCodec::codec));
        return new StreamCodec<B, CustomPacketPayload>(){

            private StreamCodec<? super B, ? extends CustomPacketPayload> findCodec(Identifier typeId) {
                StreamCodec codec = (StreamCodec)idToType.get(typeId);
                if (codec != null) {
                    return codec;
                }
                return fallback.create(typeId);
            }

            private <T extends CustomPacketPayload> void writeCap(B output, Type<T> type, CustomPacketPayload payload) {
                ((FriendlyByteBuf)((Object)output)).writeIdentifier(type.id());
                StreamCodec codec = this.findCodec(type.id);
                codec.encode(output, payload);
            }

            @Override
            public void encode(B output, CustomPacketPayload value) {
                this.writeCap(output, value.type(), value);
            }

            @Override
            public CustomPacketPayload decode(B input) {
                Identifier identifier = ((FriendlyByteBuf)((Object)input)).readIdentifier();
                return (CustomPacketPayload)this.findCodec(identifier).decode(input);
            }
        };
    }

    public record Type<T extends CustomPacketPayload>(Identifier id) {
    }

    public static interface FallbackProvider<B extends FriendlyByteBuf> {
        public StreamCodec<B, ? extends CustomPacketPayload> create(Identifier var1);
    }

    public record TypeAndCodec<B extends FriendlyByteBuf, T extends CustomPacketPayload>(Type<T> type, StreamCodec<B, T> codec) {
    }
}

