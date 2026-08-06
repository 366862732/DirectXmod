/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core.component;

import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import org.jspecify.annotations.Nullable;

public interface DataComponentHolder
extends DataComponentGetter {
    public DataComponentMap getComponents();

    @Override
    default public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return this.getComponents().get(type);
    }

    default public <T> Stream<T> getAllOfType(Class<? extends T> valueClass) {
        return this.getComponents().stream().map(TypedDataComponent::value).filter(value -> valueClass.isAssignableFrom(value.getClass())).map(value -> value);
    }

    @Override
    default public <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
        return this.getComponents().getOrDefault(type, defaultValue);
    }

    default public boolean has(DataComponentType<?> type) {
        return this.getComponents().has(type);
    }
}

