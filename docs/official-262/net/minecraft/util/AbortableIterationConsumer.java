/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface AbortableIterationConsumer<T> {
    public Continuation accept(T var1);

    public static <T> AbortableIterationConsumer<T> forConsumer(Consumer<T> consumer) {
        return e -> {
            consumer.accept(e);
            return Continuation.CONTINUE;
        };
    }

    public static enum Continuation {
        CONTINUE,
        ABORT;


        public boolean shouldAbort() {
            return this == ABORT;
        }
    }
}

