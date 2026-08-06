/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;

public record InclusiveRange<T extends Comparable<T>>(T minInclusive, T maxInclusive) {
    public static final Codec<InclusiveRange<Integer>> INT = InclusiveRange.codec(Codec.INT);

    public InclusiveRange {
        if (minInclusive.compareTo(maxInclusive) > 0) {
            throw new IllegalArgumentException("min_inclusive must be less than or equal to max_inclusive");
        }
    }

    public InclusiveRange(T value) {
        this(value, value);
    }

    public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> elementCodec) {
        return ExtraCodecs.intervalCodec(elementCodec, "min_inclusive", "max_inclusive", InclusiveRange::create, InclusiveRange::minInclusive, InclusiveRange::maxInclusive);
    }

    public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> elementCodec, T minAllowedInclusive, T maxAllowedInclusive) {
        return InclusiveRange.codec(elementCodec).validate(value -> {
            if (value.minInclusive().compareTo(minAllowedInclusive) < 0) {
                return DataResult.error(() -> "Range limit too low, expected at least " + String.valueOf(minAllowedInclusive) + " [" + String.valueOf(value.minInclusive()) + "-" + String.valueOf(value.maxInclusive()) + "]");
            }
            if (value.maxInclusive().compareTo(maxAllowedInclusive) > 0) {
                return DataResult.error(() -> "Range limit too high, expected at most " + String.valueOf(maxAllowedInclusive) + " [" + String.valueOf(value.minInclusive()) + "-" + String.valueOf(value.maxInclusive()) + "]");
            }
            return DataResult.success((Object)value);
        });
    }

    public static <T extends Comparable<T>> DataResult<InclusiveRange<T>> create(T minInclusive, T maxInclusive) {
        if (minInclusive.compareTo(maxInclusive) <= 0) {
            return DataResult.success(new InclusiveRange<T>(minInclusive, maxInclusive));
        }
        return DataResult.error(() -> "min_inclusive must be less than or equal to max_inclusive");
    }

    public <S extends Comparable<S>> InclusiveRange<S> map(Function<? super T, ? extends S> mapper) {
        return new InclusiveRange<Comparable>((Comparable)mapper.apply(this.minInclusive), (Comparable)mapper.apply(this.maxInclusive));
    }

    public boolean isValueInRange(T value) {
        return value.compareTo(this.minInclusive) >= 0 && value.compareTo(this.maxInclusive) <= 0;
    }

    public boolean contains(InclusiveRange<T> subRange) {
        return subRange.minInclusive().compareTo(this.minInclusive) >= 0 && subRange.maxInclusive.compareTo(this.maxInclusive) <= 0;
    }

    @Override
    public String toString() {
        return "[" + String.valueOf(this.minInclusive) + ", " + String.valueOf(this.maxInclusive) + "]";
    }
}

