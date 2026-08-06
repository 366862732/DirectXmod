/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Lifecycle
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class DefaultedMappedRegistry<T>
extends MappedRegistry<T>
implements DefaultedRegistry<T> {
    private final Identifier defaultKey;
    private Holder.Reference<T> defaultValue;

    public DefaultedMappedRegistry(String defaultKey, ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, boolean intrusiveHolders) {
        super(key, lifecycle, intrusiveHolders);
        this.defaultKey = Identifier.parse(defaultKey);
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo registrationInfo) {
        Holder.Reference<T> result = super.register(key, value, registrationInfo);
        if (this.defaultKey.equals(key.identifier())) {
            this.defaultValue = result;
        }
        return result;
    }

    @Override
    public int getId(@Nullable T thing) {
        int id = super.getId(thing);
        return id == -1 ? super.getId(this.defaultValue.value()) : id;
    }

    @Override
    public Identifier getKey(T thing) {
        Identifier k = super.getKey(thing);
        return k == null ? this.defaultKey : k;
    }

    @Override
    public T getValue(@Nullable Identifier key) {
        Object t = super.getValue(key);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<T> getOptional(@Nullable Identifier key) {
        return Optional.ofNullable(super.getValue(key));
    }

    @Override
    public Optional<Holder.Reference<T>> getAny() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public T byId(int id) {
        Object t = super.byId(id);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource random) {
        return super.getRandom(random).or(() -> Optional.of(this.defaultValue));
    }

    @Override
    public Identifier getDefaultKey() {
        return this.defaultKey;
    }
}

