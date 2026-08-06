/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Function;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public interface ConditionUserBuilder<T extends ConditionUserBuilder<T>> {
    public T when(LootItemCondition.Builder var1);

    default public <E> T when(Iterable<E> collection, Function<E, LootItemCondition.Builder> conditionProvider) {
        T result = this.unwrap();
        for (E value : collection) {
            result = result.when(conditionProvider.apply(value));
        }
        return result;
    }

    public T unwrap();
}

