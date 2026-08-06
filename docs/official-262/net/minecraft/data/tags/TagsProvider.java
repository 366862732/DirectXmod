/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 */
package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;

public abstract class TagsProvider<T>
implements DataProvider {
    protected final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;
    private final CompletableFuture<Void> contentsDone = new CompletableFuture();
    private final CompletableFuture<TagLookup<T>> parentProvider;
    protected final ResourceKey<? extends Registry<T>> registryKey;
    private final Map<Identifier, TagBuilder> builders = Maps.newLinkedHashMap();

    protected TagsProvider(PackOutput output, ResourceKey<? extends Registry<T>> registryKey, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this(output, registryKey, lookupProvider, CompletableFuture.completedFuture(TagLookup.empty()));
    }

    protected TagsProvider(PackOutput output, ResourceKey<? extends Registry<T>> registryKey, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<T>> parentProvider) {
        this.pathProvider = output.createRegistryTagsPathProvider(registryKey);
        this.registryKey = registryKey;
        this.parentProvider = parentProvider;
        this.lookupProvider = lookupProvider;
    }

    @Override
    public final String getName() {
        return "Tags for " + String.valueOf(this.registryKey.identifier());
    }

    protected abstract void addTags(HolderLookup.Provider var1);

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        record CombinedData<T>(HolderLookup.Provider contents, TagLookup<T> parent) {
        }
        return ((CompletableFuture)((CompletableFuture)this.createContentsProvider().thenApply(provider -> {
            this.contentsDone.complete(null);
            return provider;
        })).thenCombineAsync(this.parentProvider, (x$0, x$1) -> new CombinedData((HolderLookup.Provider)x$0, x$1), (Executor)Util.backgroundExecutor())).thenCompose(c -> {
            HolderGetter lookup = c.contents.lookupOrThrow(this.registryKey);
            Predicate<Identifier> elementCheck = arg_0 -> this.lambda$run$3((HolderLookup.RegistryLookup)lookup, arg_0);
            Predicate<Identifier> tagCheck = id -> this.builders.containsKey(id) || c.parent.contains(TagKey.create(this.registryKey, id));
            return CompletableFuture.allOf((CompletableFuture[])this.builders.entrySet().stream().map(entry -> {
                Identifier id = (Identifier)entry.getKey();
                TagBuilder builder = (TagBuilder)entry.getValue();
                List<TagEntry> entries = builder.build();
                List<TagEntry> unresolvedEntries = entries.stream().filter(e -> !e.verifyIfPresent(elementCheck, tagCheck)).toList();
                if (!unresolvedEntries.isEmpty()) {
                    throw new IllegalArgumentException(String.format(Locale.ROOT, "Couldn't define tag %s as it is missing following references: %s", id, unresolvedEntries.stream().map(Objects::toString).collect(Collectors.joining(","))));
                }
                Path path = this.pathProvider.json(id);
                return DataProvider.saveStable(cache, c.contents, TagFile.CODEC, new TagFile(entries, builder.shouldReplace()), path);
            }).toArray(CompletableFuture[]::new));
        });
    }

    protected TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
        return this.builders.computeIfAbsent(tag.location(), k -> TagBuilder.create());
    }

    public CompletableFuture<TagLookup<T>> contentsGetter() {
        return this.contentsDone.thenApply(ignore -> id -> Optional.ofNullable(this.builders.get(id.location())));
    }

    protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
        return this.lookupProvider.thenApply(registries -> {
            this.builders.clear();
            this.addTags((HolderLookup.Provider)registries);
            return registries;
        });
    }

    protected TagAppender<T> tag(TagKey<T> tag) {
        TagBuilder builder = this.getOrCreateRawBuilder(tag);
        return TagAppender.forBuilder(builder);
    }

    protected TagAppender<T> tag(TagKey<T> tag, boolean replace) {
        TagBuilder builder = this.getOrCreateRawBuilder(tag);
        builder.setReplace(replace);
        return TagAppender.forBuilder(builder);
    }

    private /* synthetic */ boolean lambda$run$3(HolderLookup.RegistryLookup lookup, Identifier id) {
        return lookup.get(ResourceKey.create(this.registryKey, id)).isPresent();
    }

    @FunctionalInterface
    public static interface TagLookup<T>
    extends Function<TagKey<T>, Optional<TagBuilder>> {
        public static <T> TagLookup<T> empty() {
            return id -> Optional.empty();
        }

        default public boolean contains(TagKey<T> key) {
            return ((Optional)this.apply(key)).isPresent();
        }
    }
}

