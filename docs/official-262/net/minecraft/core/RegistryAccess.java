/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public interface RegistryAccess
extends HolderLookup.Provider {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Frozen EMPTY = new ImmutableRegistryAccess(Map.of()).freeze();

    public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> var1);

    default public <E> Registry<E> lookupOrThrow(ResourceKey<? extends Registry<? extends E>> name) {
        return this.lookup(name).orElseThrow(() -> new IllegalStateException("Missing registry: " + String.valueOf(name)));
    }

    public Stream<RegistryEntry<?>> registries();

    @Override
    default public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
        return this.registries().map(e -> e.key);
    }

    public static Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> registries) {
        return new Frozen(){

            public <T> Optional<Registry<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                Registry registry = registries;
                return registry.getOptional(registryKey);
            }

            @Override
            public Stream<RegistryEntry<?>> registries() {
                return registries.entrySet().stream().map(RegistryEntry::fromMapEntry);
            }

            @Override
            public Frozen freeze() {
                return this;
            }
        };
    }

    default public Frozen freeze() {
        class FrozenAccess
        extends ImmutableRegistryAccess
        implements Frozen {
            protected FrozenAccess(RegistryAccess this$0, Stream<RegistryEntry<?>> entries) {
                Objects.requireNonNull(this$0);
                super(entries);
            }
        }
        return new FrozenAccess(this, this.registries().map(RegistryEntry::freeze));
    }

    public record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
        private static <T, R extends Registry<? extends T>> RegistryEntry<T> fromMapEntry(Map.Entry<? extends ResourceKey<? extends Registry<?>>, R> e) {
            return RegistryEntry.fromUntyped(e.getKey(), (Registry)e.getValue());
        }

        private static <T> RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> key, Registry<?> value) {
            return new RegistryEntry(key, value);
        }

        private RegistryEntry<T> freeze() {
            return new RegistryEntry<T>(this.key, this.value.freeze());
        }
    }

    public static class ImmutableRegistryAccess
    implements RegistryAccess {
        private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

        public ImmutableRegistryAccess(List<? extends Registry<?>> registries) {
            this.registries = registries.stream().collect(Collectors.toUnmodifiableMap(Registry::key, v -> v));
        }

        public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries) {
            this.registries = Map.copyOf(registries);
        }

        public ImmutableRegistryAccess(Stream<RegistryEntry<?>> entries) {
            this.registries = (Map)entries.collect(ImmutableMap.toImmutableMap(RegistryEntry::key, RegistryEntry::value));
        }

        @Override
        public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
            return Optional.ofNullable(this.registries.get(registryKey)).map(r -> r);
        }

        @Override
        public Stream<RegistryEntry<?>> registries() {
            return this.registries.entrySet().stream().map(RegistryEntry::fromMapEntry);
        }
    }

    public static interface Frozen
    extends RegistryAccess {
    }
}

