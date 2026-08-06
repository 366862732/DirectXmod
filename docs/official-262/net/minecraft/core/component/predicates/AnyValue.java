/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core.component.predicates;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;

public record AnyValue(DataComponentType<?> type) implements DataComponentPredicate
{
    @Override
    public boolean matches(DataComponentGetter components) {
        return components.get(this.type) != null;
    }
}

