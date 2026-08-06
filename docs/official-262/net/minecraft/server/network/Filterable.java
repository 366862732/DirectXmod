/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.server.network;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.network.FilteredText;

public record Filterable<T>(T raw, Optional<T> filtered) {
    public static <T> Codec<Filterable<T>> codec(Codec<T> valueCodec) {
        Codec fullCodec = RecordCodecBuilder.create(i -> i.group((App)valueCodec.fieldOf("raw").forGetter(Filterable::raw), (App)valueCodec.optionalFieldOf("filtered").forGetter(Filterable::filtered)).apply((Applicative)i, Filterable::new));
        Codec simpleCodec = valueCodec.xmap(Filterable::passThrough, Filterable::raw);
        return Codec.withAlternative((Codec)fullCodec, (Codec)simpleCodec);
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Filterable<T>> streamCodec(StreamCodec<B, T> valueCodec) {
        return StreamCodec.composite(valueCodec, Filterable::raw, valueCodec.apply(ByteBufCodecs::optional), Filterable::filtered, Filterable::new);
    }

    public static <T> Filterable<T> passThrough(T value) {
        return new Filterable<T>(value, Optional.empty());
    }

    public static Filterable<String> from(FilteredText text) {
        return new Filterable<String>(text.raw(), text.isFiltered() ? Optional.of(text.filteredOrEmpty()) : Optional.empty());
    }

    public T get(boolean filterEnabled) {
        if (filterEnabled) {
            return this.filtered.orElse(this.raw);
        }
        return this.raw;
    }

    public <U> Filterable<U> map(Function<T, U> function) {
        return new Filterable<U>(function.apply(this.raw), this.filtered.map(function));
    }

    public <U> Optional<Filterable<U>> resolve(Function<T, Optional<U>> function) {
        Optional<U> newRaw = function.apply(this.raw);
        if (newRaw.isEmpty()) {
            return Optional.empty();
        }
        if (this.filtered.isPresent()) {
            Optional<U> newFiltered = function.apply(this.filtered.get());
            if (newFiltered.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Filterable<U>(newRaw.get(), newFiltered));
        }
        return Optional.of(new Filterable<U>(newRaw.get(), Optional.empty()));
    }
}

