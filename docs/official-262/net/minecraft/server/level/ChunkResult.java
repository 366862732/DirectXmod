/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface ChunkResult<T> {
    public static <T> ChunkResult<T> of(T value) {
        return new Success<T>(value);
    }

    public static <T> ChunkResult<T> error(String error) {
        return ChunkResult.error(() -> error);
    }

    public static <T> ChunkResult<T> error(Supplier<String> errorSupplier) {
        return new Fail(errorSupplier);
    }

    public boolean isSuccess();

    public @Nullable T orElse(@Nullable T var1);

    public static <R> @Nullable R orElse(ChunkResult<? extends R> chunkResult, @Nullable R orElse) {
        R result = chunkResult.orElse(null);
        return result != null ? result : (R)orElse;
    }

    public @Nullable String getError();

    public ChunkResult<T> ifSuccess(Consumer<T> var1);

    public <R> ChunkResult<R> map(Function<T, R> var1);

    public <E extends Throwable> T orElseThrow(Supplier<E> var1) throws E;

    public record Success<T>(T value) implements ChunkResult<T>
    {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T orElse(@Nullable T orElse) {
            return this.value;
        }

        @Override
        public @Nullable String getError() {
            return null;
        }

        @Override
        public ChunkResult<T> ifSuccess(Consumer<T> consumer) {
            consumer.accept(this.value);
            return this;
        }

        @Override
        public <R> ChunkResult<R> map(Function<T, R> map) {
            return new Success<R>(map.apply(this.value));
        }

        @Override
        public <E extends Throwable> T orElseThrow(Supplier<E> exceptionSupplier) throws E {
            return this.value;
        }
    }

    public record Fail<T>(Supplier<String> error) implements ChunkResult<T>
    {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public @Nullable T orElse(@Nullable T orElse) {
            return orElse;
        }

        @Override
        public String getError() {
            return this.error.get();
        }

        @Override
        public ChunkResult<T> ifSuccess(Consumer<T> consumer) {
            return this;
        }

        @Override
        public <R> ChunkResult<R> map(Function<T, R> map) {
            return new Fail<T>(this.error);
        }

        @Override
        public <E extends Throwable> T orElseThrow(Supplier<E> exceptionSupplier) throws E {
            throw (Throwable)exceptionSupplier.get();
        }
    }
}

