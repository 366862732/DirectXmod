/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

@FunctionalInterface
public interface TimeSource {
    public long get(TimeUnit var1);

    public static NanoTimeSource constant(long value) {
        return () -> value;
    }

    public static interface NanoTimeSource
    extends LongSupplier,
    TimeSource {
        @Override
        default public long get(TimeUnit timeUnit) {
            return timeUnit.convert(this.getAsLong(), TimeUnit.NANOSECONDS);
        }
    }
}

