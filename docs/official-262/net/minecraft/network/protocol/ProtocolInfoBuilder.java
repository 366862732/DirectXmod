/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.CodecModifier;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.ProtocolCodecBuilder;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.UnboundProtocol;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf, C> {
    private final ConnectionProtocol protocol;
    private final PacketFlow flow;
    private final List<CodecEntry<T, ?, B, C>> codecs = new ArrayList();
    private @Nullable BundlerInfo bundlerInfo;

    public ProtocolInfoBuilder(ConnectionProtocol protocol, PacketFlow flow) {
        this.protocol = protocol;
        this.flow = flow;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(PacketType<P> type, StreamCodec<? super B, P> serializer) {
        this.codecs.add(new CodecEntry(type, serializer, null));
        return this;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(PacketType<P> type, StreamCodec<? super B, P> serializer, CodecModifier<B, P, C> modifier) {
        this.codecs.add(new CodecEntry(type, serializer, modifier));
        return this;
    }

    public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B, C> withBundlePacket(PacketType<P> bundlerPacket, Function<Iterable<Packet<? super T>>, P> constructor, D delimiterPacket) {
        StreamCodec delimitedCodec = StreamCodec.unit(delimiterPacket);
        PacketType<BundleDelimiterPacket<? super T>> delimiterType = delimiterPacket.type();
        this.codecs.add(new CodecEntry(delimiterType, delimitedCodec, null));
        this.bundlerInfo = BundlerInfo.createForPacket(bundlerPacket, constructor, delimiterPacket);
        return this;
    }

    private StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(Function<ByteBuf, B> contextWrapper, List<CodecEntry<T, ?, B, C>> codecs, C context) {
        ProtocolCodecBuilder codecBuilder = new ProtocolCodecBuilder(this.flow);
        for (CodecEntry codecEntry : codecs) {
            codecEntry.addToBuilder(codecBuilder, contextWrapper, context);
        }
        return codecBuilder.build();
    }

    private static ProtocolInfo.Details buildDetails(final ConnectionProtocol protocol, final PacketFlow flow, final List<? extends CodecEntry<?, ?, ?, ?>> codecs) {
        return new ProtocolInfo.Details(){

            @Override
            public ConnectionProtocol id() {
                return protocol;
            }

            @Override
            public PacketFlow flow() {
                return flow;
            }

            @Override
            public void listPackets(ProtocolInfo.Details.PacketVisitor output) {
                for (int i = 0; i < codecs.size(); ++i) {
                    CodecEntry entry = (CodecEntry)codecs.get(i);
                    output.accept(entry.type, i);
                }
            }
        };
    }

    public SimpleUnboundProtocol<T, B> buildUnbound(final C context) {
        final List<CodecEntry<T, ?, B, C>> codecs = List.copyOf(this.codecs);
        final BundlerInfo bundlerInfo = this.bundlerInfo;
        final ProtocolInfo.Details details = ProtocolInfoBuilder.buildDetails(this.protocol, this.flow, codecs);
        return new SimpleUnboundProtocol<T, B>(this){
            final /* synthetic */ ProtocolInfoBuilder this$0;
            {
                ProtocolInfoBuilder protocolInfoBuilder = this$0;
                Objects.requireNonNull(protocolInfoBuilder);
                this.this$0 = protocolInfoBuilder;
            }

            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> contextWrapper) {
                return new Implementation(this.this$0.protocol, this.this$0.flow, this.this$0.buildPacketCodec(contextWrapper, codecs, context), bundlerInfo);
            }

            @Override
            public ProtocolInfo.Details details() {
                return details;
            }
        };
    }

    public UnboundProtocol<T, B, C> buildUnbound() {
        final List<CodecEntry<T, ?, B, C>> codecs = List.copyOf(this.codecs);
        final BundlerInfo bundlerInfo = this.bundlerInfo;
        final ProtocolInfo.Details details = ProtocolInfoBuilder.buildDetails(this.protocol, this.flow, codecs);
        return new UnboundProtocol<T, B, C>(this){
            final /* synthetic */ ProtocolInfoBuilder this$0;
            {
                ProtocolInfoBuilder protocolInfoBuilder = this$0;
                Objects.requireNonNull(protocolInfoBuilder);
                this.this$0 = protocolInfoBuilder;
            }

            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> contextWrapper, C context) {
                return new Implementation(this.this$0.protocol, this.this$0.flow, this.this$0.buildPacketCodec(contextWrapper, codecs, context), bundlerInfo);
            }

            @Override
            public ProtocolInfo.Details details() {
                return details;
            }
        };
    }

    private static <L extends PacketListener, B extends ByteBuf> SimpleUnboundProtocol<L, B> protocol(ConnectionProtocol id, PacketFlow flow, Consumer<ProtocolInfoBuilder<L, B, Unit>> config) {
        ProtocolInfoBuilder builder = new ProtocolInfoBuilder(id, flow);
        config.accept(builder);
        return builder.buildUnbound(Unit.INSTANCE);
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> serverboundProtocol(ConnectionProtocol id, Consumer<ProtocolInfoBuilder<T, B, Unit>> config) {
        return ProtocolInfoBuilder.protocol(id, PacketFlow.SERVERBOUND, config);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> clientboundProtocol(ConnectionProtocol id, Consumer<ProtocolInfoBuilder<T, B, Unit>> config) {
        return ProtocolInfoBuilder.protocol(id, PacketFlow.CLIENTBOUND, config);
    }

    private static <L extends PacketListener, B extends ByteBuf, C> UnboundProtocol<L, B, C> contextProtocol(ConnectionProtocol id, PacketFlow flow, Consumer<ProtocolInfoBuilder<L, B, C>> config) {
        ProtocolInfoBuilder builder = new ProtocolInfoBuilder(id, flow);
        config.accept(builder);
        return builder.buildUnbound();
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextServerboundProtocol(ConnectionProtocol id, Consumer<ProtocolInfoBuilder<T, B, C>> config) {
        return ProtocolInfoBuilder.contextProtocol(id, PacketFlow.SERVERBOUND, config);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextClientboundProtocol(ConnectionProtocol id, Consumer<ProtocolInfoBuilder<T, B, C>> config) {
        return ProtocolInfoBuilder.contextProtocol(id, PacketFlow.CLIENTBOUND, config);
    }

    private record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(PacketType<P> type, StreamCodec<? super B, P> serializer, @Nullable CodecModifier<B, P, C> modifier) {
        public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> codecBuilder, Function<ByteBuf, B> contextWrapper, C context) {
            StreamCodec<Object, P> finalSerializer = this.modifier != null ? this.modifier.apply(this.serializer, context) : this.serializer;
            StreamCodec<ByteBuf, P> baseCodec = finalSerializer.mapStream(contextWrapper);
            codecBuilder.add(this.type, baseCodec);
        }
    }

    private record Implementation<L extends PacketListener>(ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo) implements ProtocolInfo<L>
    {
    }
}

