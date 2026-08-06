/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.ListBuilder
 */
package net.minecraft.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.function.UnaryOperator;

public abstract class AbstractListBuilder<T, B>
implements ListBuilder<T> {
    private final DynamicOps<T> ops;
    protected DataResult<B> builder = DataResult.success(this.initBuilder(), (Lifecycle)Lifecycle.stable());

    protected AbstractListBuilder(DynamicOps<T> ops) {
        this.ops = ops;
    }

    public DynamicOps<T> ops() {
        return this.ops;
    }

    protected abstract B initBuilder();

    protected abstract B append(B var1, T var2);

    protected abstract DataResult<T> build(B var1, T var2);

    public ListBuilder<T> add(T value) {
        this.builder = this.builder.map(b -> this.append(b, value));
        return this;
    }

    public ListBuilder<T> add(DataResult<T> value) {
        this.builder = this.builder.apply2stable(this::append, value);
        return this;
    }

    public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
        this.builder = this.builder.flatMap(r -> result.map(v -> r));
        return this;
    }

    public ListBuilder<T> mapError(UnaryOperator<String> onError) {
        this.builder = this.builder.mapError(onError);
        return this;
    }

    public DataResult<T> build(T prefix) {
        DataResult result = this.builder.flatMap(b -> this.build(b, prefix));
        this.builder = DataResult.success(this.initBuilder(), (Lifecycle)Lifecycle.stable());
        return result;
    }
}

