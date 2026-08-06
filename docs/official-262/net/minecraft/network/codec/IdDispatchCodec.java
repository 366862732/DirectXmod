/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.handler.codec.DecoderException
 *  io.netty.handler.codec.EncoderException
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 */
package net.minecraft.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;

public class IdDispatchCodec<B extends ByteBuf, V, T>
implements StreamCodec<B, V> {
    private static final int UNKNOWN_TYPE = -1;
    private final Function<V, ? extends T> typeGetter;
    private final List<Entry<B, V, T>> byId;
    private final Object2IntMap<T> toId;

    private IdDispatchCodec(Function<V, ? extends T> typeGetter, List<Entry<B, V, T>> byId, Object2IntMap<T> toId) {
        this.typeGetter = typeGetter;
        this.byId = byId;
        this.toId = toId;
    }

    @Override
    public V decode(B input) {
        int id = VarInt.read(input);
        if (id < 0 || id >= this.byId.size()) {
            throw new DecoderException("Received unknown packet id " + id);
        }
        Entry<B, V, T> entry = this.byId.get(id);
        try {
            return (V)entry.serializer.decode(input);
        }
        catch (Exception e) {
            if (e instanceof DontDecorateException) {
                throw e;
            }
            throw new DecoderException("Failed to decode packet '" + String.valueOf(entry.type) + "'", (Throwable)e);
        }
    }

    @Override
    public void encode(B output, V value) {
        T type = this.typeGetter.apply(value);
        int id = this.toId.getOrDefault(type, -1);
        if (id == -1) {
            throw new EncoderException("Sending unknown packet '" + String.valueOf(type) + "'");
        }
        VarInt.write(output, id);
        Entry<B, V, T> entry = this.byId.get(id);
        try {
            StreamCodec codec = entry.serializer;
            codec.encode(output, value);
        }
        catch (Exception e) {
            if (e instanceof DontDecorateException) {
                throw e;
            }
            throw new EncoderException("Failed to encode packet '" + String.valueOf(type) + "'", (Throwable)e);
        }
    }

    public static <B extends ByteBuf, V, T> Builder<B, V, T> builder(Function<V, ? extends T> typeGetter) {
        return new Builder(typeGetter);
    }

    private record Entry<B, V, T>(StreamCodec<? super B, ? extends V> serializer, T type) {
    }

    public static interface DontDecorateException {
    }

    public static class Builder<B extends ByteBuf, V, T> {
        private final List<Entry<B, V, T>> entries = new ArrayList<Entry<B, V, T>>();
        private final Function<V, ? extends T> typeGetter;

        private Builder(Function<V, ? extends T> typeGetter) {
            this.typeGetter = typeGetter;
        }

        public Builder<B, V, T> add(T type, StreamCodec<? super B, ? extends V> serializer) {
            this.entries.add(new Entry<B, V, T>(serializer, type));
            return this;
        }

        public IdDispatchCodec<B, V, T> build() {
            Object2IntOpenHashMap toId = new Object2IntOpenHashMap();
            toId.defaultReturnValue(-2);
            for (Entry<B, V, T> entry : this.entries) {
                int id = toId.size();
                int previous = toId.putIfAbsent(entry.type, id);
                if (previous == -2) continue;
                throw new IllegalStateException("Duplicate registration for type " + String.valueOf(entry.type));
            }
            return new IdDispatchCodec<B, V, T>(this.typeGetter, List.copyOf(this.entries), toId);
        }
    }
}

