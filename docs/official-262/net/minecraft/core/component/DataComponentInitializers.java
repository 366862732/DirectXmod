/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 */
package net.minecraft.core.component;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;

public class DataComponentInitializers {
    private final List<InitializerEntry<?>> initializers = new ArrayList();

    public <T> void add(ResourceKey<T> key, Initializer<T> initializer) {
        this.initializers.add(new InitializerEntry<T>(key, initializer));
    }

    private Map<ResourceKey<?>, DataComponentMap.Builder> runInitializers(HolderLookup.Provider context) {
        HashMap results = new HashMap();
        for (InitializerEntry<?> initializer : this.initializers) {
            DataComponentMap.Builder builder = results.computeIfAbsent(initializer.key, k -> DataComponentMap.builder());
            initializer.run(builder, context);
        }
        return results;
    }

    private static <T> void registryEmpty(Map<ResourceKey<? extends Registry<?>>, PendingComponentBuilders<?>> buildersByRegistry, ResourceKey<? extends Registry<? extends T>> registryKey) {
        ResourceKey<? extends Registry<? extends T>> registryKeyButSane = registryKey;
        buildersByRegistry.put(registryKey, new PendingComponentBuilders(registryKeyButSane, new HashMap()));
    }

    private static <T> void addBuilder(Map<ResourceKey<? extends Registry<?>>, PendingComponentBuilders<?>> buildersByRegistry, ResourceKey<T> key, DataComponentMap.Builder builder) {
        PendingComponentBuilders<?> buildersForRegistry = buildersByRegistry.get(key.registryKey());
        buildersForRegistry.builders.put(key, builder);
    }

    public List<PendingComponents<?>> build(HolderLookup.Provider context) {
        HashMap buildersByRegistry = new HashMap();
        context.listRegistryKeys().forEach(registryKey -> DataComponentInitializers.registryEmpty(buildersByRegistry, registryKey));
        this.runInitializers(context).forEach((key, builder) -> DataComponentInitializers.addBuilder(buildersByRegistry, key, builder));
        return buildersByRegistry.values().stream().map(elementBuilders -> DataComponentInitializers.createInitializerForRegistry(context, elementBuilders)).collect(Collectors.toUnmodifiableList());
    }

    private static <T> PendingComponents<T> createInitializerForRegistry(HolderLookup.Provider context, PendingComponentBuilders<T> elementBuilders) {
        final ArrayList entries = new ArrayList();
        final ResourceKey registryKey = elementBuilders.registryKey;
        HolderGetter registry = context.lookupOrThrow(registryKey);
        Set elementsWithComponents = Sets.newIdentityHashSet();
        elementBuilders.builders.forEach((arg_0, arg_1) -> DataComponentInitializers.lambda$createInitializerForRegistry$0((HolderLookup.RegistryLookup)registry, entries, elementsWithComponents, arg_0, arg_1));
        registry.listElements().filter(e -> !elementsWithComponents.contains(e)).forEach(elementWithoutComponents -> entries.add(new BakedEntry(elementWithoutComponents, DataComponentMap.EMPTY)));
        return new PendingComponents<T>(){

            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return registryKey;
            }

            @Override
            public void forEach(BiConsumer<Holder.Reference<T>, DataComponentMap> output) {
                entries.forEach((? super T e) -> output.accept(e.element, e.components));
            }

            @Override
            public void apply() {
                entries.forEach(BakedEntry::apply);
            }
        };
    }

    private static /* synthetic */ void lambda$createInitializerForRegistry$0(HolderLookup.RegistryLookup registry, List entries, Set elementsWithComponents, ResourceKey elementKey, DataComponentMap.Builder elementBuilder) {
        Holder.Reference element = registry.getOrThrow(elementKey);
        DataComponentMap components = elementBuilder.build();
        entries.add(new BakedEntry(element, components));
        elementsWithComponents.add(element);
    }

    private record InitializerEntry<T>(ResourceKey<T> key, Initializer<T> initializer) {
        public void run(DataComponentMap.Builder components, HolderLookup.Provider context) {
            this.initializer.run(components, context, this.key);
        }
    }

    @FunctionalInterface
    public static interface Initializer<T> {
        public void run(DataComponentMap.Builder var1, HolderLookup.Provider var2, ResourceKey<T> var3);

        default public Initializer<T> andThen(Initializer<T> other) {
            return (components, context, key) -> {
                this.run(components, context, key);
                other.run(components, context, key);
            };
        }

        default public <C> Initializer<T> add(DataComponentType<C> type, C value) {
            return this.andThen((components, context, key) -> components.set(type, value));
        }
    }

    private record PendingComponentBuilders<T>(ResourceKey<? extends Registry<T>> registryKey, Map<ResourceKey<T>, DataComponentMap.Builder> builders) {
    }

    private record BakedEntry<T>(Holder.Reference<T> element, DataComponentMap components) {
        public void apply() {
            this.element.bindComponents(this.components);
        }
    }

    public static interface PendingComponents<T> {
        public ResourceKey<? extends Registry<? extends T>> key();

        public void forEach(BiConsumer<Holder.Reference<T>, DataComponentMap> var1);

        public void apply();
    }

    @FunctionalInterface
    public static interface SingleComponentInitializer<C> {
        public C create(HolderLookup.Provider var1);

        default public <T> Initializer<T> asInitializer(DataComponentType<C> type) {
            return (components, context, key) -> components.set(type, this.create(context));
        }
    }
}

