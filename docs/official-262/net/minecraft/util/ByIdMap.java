/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 */
package net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import net.minecraft.util.Mth;

public class ByIdMap {
    private static <T> IntFunction<T> createMap(ToIntFunction<T> idGetter, T[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Empty value list");
        }
        Int2ObjectOpenHashMap result = new Int2ObjectOpenHashMap();
        for (T value : values) {
            int id = idGetter.applyAsInt(value);
            Object previous = result.put(id, value);
            if (previous == null) continue;
            throw new IllegalArgumentException("Duplicate entry on id " + id + ": current=" + String.valueOf(value) + ", previous=" + String.valueOf(previous));
        }
        return result;
    }

    public static <T> IntFunction<T> sparse(ToIntFunction<T> idGetter, T[] values, T _default) {
        IntFunction idToObject = ByIdMap.createMap(idGetter, values);
        return id -> Objects.requireNonNullElse(idToObject.apply(id), _default);
    }

    private static <T> T[] createSortedArray(ToIntFunction<T> idGetter, T[] values) {
        int length = values.length;
        if (length == 0) {
            throw new IllegalArgumentException("Empty value list");
        }
        Object[] result = (Object[])values.clone();
        Arrays.fill(result, null);
        for (T value : values) {
            int id = idGetter.applyAsInt(value);
            if (id < 0 || id >= length) {
                throw new IllegalArgumentException("Values are not continous, found index " + id + " for value " + String.valueOf(value));
            }
            Object previous = result[id];
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate entry on id " + id + ": current=" + String.valueOf(value) + ", previous=" + String.valueOf(previous));
            }
            result[id] = value;
        }
        for (int i = 0; i < length; ++i) {
            if (result[i] != null) continue;
            throw new IllegalArgumentException("Missing value at index: " + i);
        }
        return result;
    }

    public static <T> IntFunction<T> continuous(ToIntFunction<T> idGetter, T[] values, OutOfBoundsStrategy strategy) {
        Object[] sortedValues = ByIdMap.createSortedArray(idGetter, values);
        int length = sortedValues.length;
        return switch (strategy.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                Object zeroValue = sortedValues[0];
                yield id -> id >= 0 && id < length ? sortedValues[id] : zeroValue;
            }
            case 1 -> id -> sortedValues[Mth.positiveModulo(id, length)];
            case 2 -> id -> sortedValues[Mth.clamp(id, 0, length - 1)];
        };
    }

    public static enum OutOfBoundsStrategy {
        ZERO,
        WRAP,
        CLAMP;

    }
}

