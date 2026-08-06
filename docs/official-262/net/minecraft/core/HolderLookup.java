/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;

public interface HolderLookup<T>
extends HolderGetter<T> {
    public Stream<Holder.Reference<T>> listElements();

    default public Stream<ResourceKey<T>> listElementIds() {
        return this.listElements().map(Holder.Reference::key);
    }

    public Stream<HolderSet.Named<T>> listTags();

    default public Stream<TagKey<T>> listTagIds() {
        return this.listTags().map(HolderSet.Named::key);
    }

    public static interface Provider
    extends HolderGetter.Provider {
        public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys();

        default public Stream<RegistryLookup<?>> listRegistries() {
            return this.listRegistryKeys().map(resourceKey -> this.lookupOrThrow((ResourceKey)resourceKey));
        }

        public <T> Optional<? extends RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

        default public <T> RegistryLookup<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> key) {
            return this.lookup(key).orElseThrow(() -> new IllegalStateException("Registry " + String.valueOf(key.identifier()) + " not found"));
        }

        default public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> parent) {
            return RegistryOps.create(parent, this);
        }

        public static Provider create(Stream<RegistryLookup<?>> lookups) {
            final Map<ResourceKey, RegistryLookup> map = lookups.collect(Collectors.toUnmodifiableMap(RegistryLookup::key, e -> e));
            return new Provider(){

                @Override
                public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                    return map.keySet().stream();
                }

                public <T> Optional<RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                    return Optional.ofNullable((RegistryLookup)map.get(key));
                }
            };
        }

        default public Lifecycle allRegistriesLifecycle() {
            return this.listRegistries().map(RegistryLookup::registryLifecycle).reduce(Lifecycle.stable(), Lifecycle::add);
        }
    }

    public static interface RegistryLookup<T>
    extends HolderLookup<T>,
    HolderOwner<T> {
        public ResourceKey<? extends Registry<? extends T>> key();

        public Lifecycle registryLifecycle();

        default public RegistryLookup<T> filterFeatures(FeatureFlagSet enabledFeatures) {
            if (FeatureElement.FILTERED_REGISTRIES.contains(this.key())) {
                return this.filterElements(t -> ((FeatureElement)t).isEnabled(enabledFeatures));
            }
            return this;
        }

        default public RegistryLookup<T> filterElements(final Predicate<T> filter) {
            return new Delegate<T>(this){
                final /* synthetic */ RegistryLookup this$0;
                {
                    RegistryLookup registryLookup = this$0;
                    Objects.requireNonNull(registryLookup);
                    this.this$0 = registryLookup;
                }

                @Override
                public RegistryLookup<T> parent() {
                    return this.this$0;
                }

                @Override
                public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                    return this.parent().get(id).filter(holder -> filter.test(holder.value()));
                }

                @Override
                public Stream<Holder.Reference<T>> listElements() {
                    return this.parent().listElements().filter(e -> filter.test(e.value()));
                }
            };
        }

        public static interface Delegate<T>
        extends RegistryLookup<T> {
            public RegistryLookup<T> parent();

            @Override
            default public ResourceKey<? extends Registry<? extends T>> key() {
                return this.parent().key();
            }

            @Override
            default public Lifecycle registryLifecycle() {
                return this.parent().registryLifecycle();
            }

            @Override
            default public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                return this.parent().get(id);
            }

            @Override
            default public Stream<Holder.Reference<T>> listElements() {
                return this.parent().listElements();
            }

            @Override
            default public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                return this.parent().get(id);
            }

            @Override
            default public Stream<HolderSet.Named<T>> listTags() {
                return this.parent().listTags();
            }
        }
    }
}

