/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Optionull {
    @Deprecated
    public static <T> T orElse(@Nullable T t, T defaultValue) {
        return Objects.requireNonNullElse(t, defaultValue);
    }

    public static <T, R> @Nullable R map(@Nullable T t, Function<T, R> map) {
        return t == null ? null : (R)map.apply(t);
    }

    public static <T, R> R mapOrDefault(@Nullable T t, Function<T, R> map, R defaultValue) {
        return t == null ? defaultValue : map.apply(t);
    }

    public static <T, R> R mapOrElse(@Nullable T t, Function<T, R> map, Supplier<R> elseSupplier) {
        return t == null ? elseSupplier.get() : map.apply(t);
    }

    public static <T> @Nullable T first(Collection<T> collection) {
        Iterator<T> iterator = collection.iterator();
        return iterator.hasNext() ? (T)iterator.next() : null;
    }

    public static <T> T firstOrDefault(Collection<T> collection, T defaultValue) {
        Iterator<T> iterator = collection.iterator();
        return iterator.hasNext() ? iterator.next() : defaultValue;
    }

    public static <T> T firstOrElse(Collection<T> collection, Supplier<T> elseSupplier) {
        Iterator<T> iterator = collection.iterator();
        return iterator.hasNext() ? iterator.next() : elseSupplier.get();
    }

    public static <T> boolean isNullOrEmpty(T @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(boolean @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(byte @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(char @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(short @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(int @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(long @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(float @Nullable [] t) {
        return t == null || t.length == 0;
    }

    public static boolean isNullOrEmpty(double @Nullable [] t) {
        return t == null || t.length == 0;
    }
}

