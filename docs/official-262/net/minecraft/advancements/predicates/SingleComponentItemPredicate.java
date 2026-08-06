/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.advancements.predicates;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;

public interface SingleComponentItemPredicate<T>
extends DataComponentPredicate {
    @Override
    default public boolean matches(DataComponentGetter components) {
        T value = components.get(this.componentType());
        return value != null && this.matches(value);
    }

    public DataComponentType<T> componentType();

    public boolean matches(T var1);
}

