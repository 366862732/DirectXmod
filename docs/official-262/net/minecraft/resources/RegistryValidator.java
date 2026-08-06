/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.resources;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@FunctionalInterface
public interface RegistryValidator<T> {
    public static final RegistryValidator<?> NONE = (registry, map) -> {};
    public static final RegistryValidator<?> NON_EMPTY = (registry, loadingErrors) -> {
        if (registry.size() == 0) {
            loadingErrors.put(registry.key(), new IllegalStateException("Registry must be non-empty: " + String.valueOf(registry.key().identifier())));
        }
    };

    public static <T> RegistryValidator<T> none() {
        return NONE;
    }

    public static <T> RegistryValidator<T> nonEmpty() {
        return NON_EMPTY;
    }

    public void validate(Registry<T> var1, Map<ResourceKey<?>, Exception> var2);
}

