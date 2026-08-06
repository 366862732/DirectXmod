/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.CharMatcher
 *  com.google.common.base.Strings
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;

public interface SharedSuggestionProvider
extends PermissionSetSupplier {
    public static final CharMatcher MATCH_SPLITTER = CharMatcher.anyOf((CharSequence)"._/");

    public Collection<String> getOnlinePlayerNames();

    default public Collection<String> getCustomTabSuggestions() {
        return this.getOnlinePlayerNames();
    }

    default public Collection<String> getSelectedEntities() {
        return Collections.emptyList();
    }

    public Collection<String> getAllTeams();

    public Stream<Identifier> getAvailableSounds();

    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> var1);

    default public Collection<TextCoordinates> getRelevantCoordinates() {
        return Collections.singleton(TextCoordinates.DEFAULT_GLOBAL);
    }

    default public Collection<TextCoordinates> getAbsoluteCoordinates() {
        return Collections.singleton(TextCoordinates.DEFAULT_GLOBAL);
    }

    public Set<ResourceKey<Level>> levels();

    public RegistryAccess registryAccess();

    public FeatureFlagSet enabledFeatures();

    default public void suggestRegistryElements(HolderLookup<?> registry, ElementSuggestionType elements, SuggestionsBuilder builder) {
        if (elements.shouldSuggestTags()) {
            SharedSuggestionProvider.suggestResource(registry.listTagIds().map(TagKey::location), builder, "#");
        }
        if (elements.shouldSuggestElements()) {
            SharedSuggestionProvider.suggestResource(registry.listElementIds().map(ResourceKey::identifier), builder);
        }
    }

    public static <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder, ResourceKey<? extends Registry<?>> registryKey, ElementSuggestionType type) {
        Object object = context.getSource();
        if (object instanceof SharedSuggestionProvider) {
            SharedSuggestionProvider suggestionProvider = (SharedSuggestionProvider)object;
            return suggestionProvider.suggestRegistryElements(registryKey, type, builder, context);
        }
        return builder.buildFuture();
    }

    public CompletableFuture<Suggestions> suggestRegistryElements(ResourceKey<? extends Registry<?>> var1, ElementSuggestionType var2, SuggestionsBuilder var3, CommandContext<?> var4);

    public static <T> void filterResources(Iterable<T> values, String contents, Function<T, Identifier> converter, Consumer<T> consumer) {
        boolean hasNamespace = contents.indexOf(58) > -1;
        for (T value : values) {
            Identifier id = converter.apply(value);
            if (hasNamespace) {
                String name = id.toString();
                if (!SharedSuggestionProvider.matchesSubStr(contents, name)) continue;
                consumer.accept(value);
                continue;
            }
            if (!SharedSuggestionProvider.matchesSubStr(contents, id.getNamespace()) && !SharedSuggestionProvider.matchesSubStr(contents, id.getPath())) continue;
            consumer.accept(value);
        }
    }

    public static <T> void filterResources(Iterable<T> values, String contents, String prefix, Function<T, Identifier> converter, Consumer<T> consumer) {
        if (contents.isEmpty()) {
            values.forEach(consumer);
        } else {
            String commonPrefix = Strings.commonPrefix((CharSequence)contents, (CharSequence)prefix);
            if (!commonPrefix.isEmpty()) {
                String strippedContents = contents.substring(commonPrefix.length());
                SharedSuggestionProvider.filterResources(values, strippedContents, converter, consumer);
            }
        }
    }

    public static CompletableFuture<Suggestions> suggestResource(Iterable<Identifier> values, SuggestionsBuilder builder, String prefix) {
        String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
        SharedSuggestionProvider.filterResources(values, contents, prefix, t -> t, v -> builder.suggest(prefix + String.valueOf(v)));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestResource(Stream<Identifier> values, SuggestionsBuilder builder, String prefix) {
        return SharedSuggestionProvider.suggestResource(values::iterator, builder, prefix);
    }

    public static CompletableFuture<Suggestions> suggestResource(Iterable<Identifier> values, SuggestionsBuilder builder) {
        String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
        SharedSuggestionProvider.filterResources(values, contents, t -> t, v -> builder.suggest(v.toString()));
        return builder.buildFuture();
    }

    public static <T> CompletableFuture<Suggestions> suggestResource(Iterable<T> values, SuggestionsBuilder builder, Function<T, Identifier> id, Function<T, Message> tooltip) {
        String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
        SharedSuggestionProvider.filterResources(values, contents, id, v -> builder.suggest(((Identifier)id.apply(v)).toString(), (Message)tooltip.apply(v)));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestResource(Stream<Identifier> values, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(values::iterator, builder);
    }

    public static <T> CompletableFuture<Suggestions> suggestResource(Stream<T> values, SuggestionsBuilder builder, Function<T, Identifier> id, Function<T, Message> tooltip) {
        return SharedSuggestionProvider.suggestResource(values::iterator, builder, id, tooltip);
    }

    public static CompletableFuture<Suggestions> suggestCoordinates(String currentInput, Collection<TextCoordinates> allSuggestions, SuggestionsBuilder builder, Predicate<String> validator) {
        ArrayList result;
        block4: {
            String[] fields;
            block5: {
                block3: {
                    result = Lists.newArrayList();
                    if (!Strings.isNullOrEmpty((String)currentInput)) break block3;
                    for (TextCoordinates coordinate : allSuggestions) {
                        String fullValue = coordinate.x + " " + coordinate.y + " " + coordinate.z;
                        if (!validator.test(fullValue)) continue;
                        result.add(coordinate.x);
                        result.add(coordinate.x + " " + coordinate.y);
                        result.add(fullValue);
                    }
                    break block4;
                }
                fields = currentInput.split(" ");
                if (fields.length != 1) break block5;
                for (TextCoordinates coordinate : allSuggestions) {
                    String fullValue = fields[0] + " " + coordinate.y + " " + coordinate.z;
                    if (!validator.test(fullValue)) continue;
                    result.add(fields[0] + " " + coordinate.y);
                    result.add(fullValue);
                }
                break block4;
            }
            if (fields.length != 2) break block4;
            for (TextCoordinates coordinate : allSuggestions) {
                String fullValue = fields[0] + " " + fields[1] + " " + coordinate.z;
                if (!validator.test(fullValue)) continue;
                result.add(fullValue);
            }
        }
        return SharedSuggestionProvider.suggest(result, builder);
    }

    public static CompletableFuture<Suggestions> suggest2DCoordinates(String currentInput, Collection<TextCoordinates> allSuggestions, SuggestionsBuilder builder, Predicate<String> validator) {
        ArrayList result;
        block3: {
            block2: {
                result = Lists.newArrayList();
                if (!Strings.isNullOrEmpty((String)currentInput)) break block2;
                for (TextCoordinates coordinate : allSuggestions) {
                    String fullValue = coordinate.x + " " + coordinate.z;
                    if (!validator.test(fullValue)) continue;
                    result.add(coordinate.x);
                    result.add(fullValue);
                }
                break block3;
            }
            String[] fields = currentInput.split(" ");
            if (fields.length != 1) break block3;
            for (TextCoordinates coordinate : allSuggestions) {
                String fullValue = fields[0] + " " + coordinate.z;
                if (!validator.test(fullValue)) continue;
                result.add(fullValue);
            }
        }
        return SharedSuggestionProvider.suggest(result, builder);
    }

    public static CompletableFuture<Suggestions> suggest(Iterable<String> values, SuggestionsBuilder builder) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : values) {
            if (!SharedSuggestionProvider.matchesSubStr(lowerPrefix, name.toLowerCase(Locale.ROOT))) continue;
            builder.suggest(name);
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggest(Stream<String> values, SuggestionsBuilder builder) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        values.filter(v -> SharedSuggestionProvider.matchesSubStr(lowerPrefix, v.toLowerCase(Locale.ROOT))).forEach(arg_0 -> ((SuggestionsBuilder)builder).suggest(arg_0));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggest(String[] values, SuggestionsBuilder builder) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : values) {
            if (!SharedSuggestionProvider.matchesSubStr(lowerPrefix, name.toLowerCase(Locale.ROOT))) continue;
            builder.suggest(name);
        }
        return builder.buildFuture();
    }

    public static <T> CompletableFuture<Suggestions> suggest(Iterable<T> values, SuggestionsBuilder builder, Function<T, String> toString, Function<T, Message> tooltip) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (T value : values) {
            String name = toString.apply(value);
            if (!SharedSuggestionProvider.matchesSubStr(lowerPrefix, name.toLowerCase(Locale.ROOT))) continue;
            builder.suggest(name, tooltip.apply(value));
        }
        return builder.buildFuture();
    }

    public static boolean matchesSubStr(String pattern, String input) {
        int index = 0;
        while (!input.startsWith(pattern, index)) {
            int indexOfSplitter = MATCH_SPLITTER.indexIn((CharSequence)input, index);
            if (indexOfSplitter < 0) {
                return false;
            }
            index = indexOfSplitter + 1;
        }
        return true;
    }

    public static class TextCoordinates {
        public static final TextCoordinates DEFAULT_LOCAL = new TextCoordinates("^", "^", "^");
        public static final TextCoordinates DEFAULT_GLOBAL = new TextCoordinates("~", "~", "~");
        public final String x;
        public final String y;
        public final String z;

        public TextCoordinates(String x, String y, String z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static enum ElementSuggestionType {
        TAGS,
        ELEMENTS,
        ALL;


        public boolean shouldSuggestTags() {
            return this == TAGS || this == ALL;
        }

        public boolean shouldSuggestElements() {
            return this == ELEMENTS || this == ALL;
        }
    }
}

