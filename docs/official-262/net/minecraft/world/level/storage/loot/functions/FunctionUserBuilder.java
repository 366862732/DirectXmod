/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage.loot.functions;

import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

public interface FunctionUserBuilder<T extends FunctionUserBuilder<T>> {
    public T apply(LootItemFunction.Builder var1);

    default public <E> T apply(Iterable<E> collection, Function<E, LootItemFunction.Builder> functionProvider) {
        T result = this.unwrap();
        for (E value : collection) {
            result = result.apply(functionProvider.apply(value));
        }
        return result;
    }

    default public <E> T apply(E[] collection, Function<E, LootItemFunction.Builder> functionProvider) {
        return this.apply(Arrays.asList(collection), functionProvider);
    }

    public T unwrap();
}

