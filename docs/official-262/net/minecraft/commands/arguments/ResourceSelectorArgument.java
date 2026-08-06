/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  org.apache.commons.io.FilenameUtils
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.io.FilenameUtils;

public class ResourceSelectorArgument<T>
implements ArgumentType<Collection<Holder.Reference<T>>> {
    private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
    public static final Dynamic2CommandExceptionType ERROR_NO_MATCHES = new Dynamic2CommandExceptionType((selector, registry) -> Component.translatableEscape("argument.resource_selector.not_found", selector, registry));
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    private ResourceSelectorArgument(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.registryLookup = context.lookupOrThrow(registryKey);
    }

    public Collection<Holder.Reference<T>> parse(StringReader reader) throws CommandSyntaxException {
        String pattern = ResourceSelectorArgument.ensureNamespaced(ResourceSelectorArgument.readPattern(reader));
        List<Holder.Reference<T>> results = this.registryLookup.listElements().filter(element -> ResourceSelectorArgument.matches(pattern, element.key().identifier())).toList();
        if (results.isEmpty()) {
            throw ERROR_NO_MATCHES.createWithContext((ImmutableStringReader)reader, (Object)pattern, (Object)this.registryKey.identifier());
        }
        return results;
    }

    public static <T> Collection<Holder.Reference<T>> parse(StringReader reader, HolderLookup<T> registry) {
        String pattern = ResourceSelectorArgument.ensureNamespaced(ResourceSelectorArgument.readPattern(reader));
        return registry.listElements().filter(element -> ResourceSelectorArgument.matches(pattern, element.key().identifier())).toList();
    }

    private static String readPattern(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && ResourceSelectorArgument.isAllowedPatternCharacter(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean isAllowedPatternCharacter(char character) {
        return Identifier.isAllowedInIdentifier(character) || character == '*' || character == '?';
    }

    private static String ensureNamespaced(String input) {
        if (!input.contains(":")) {
            return "minecraft:" + input;
        }
        return input;
    }

    private static boolean matches(String pattern, Identifier key) {
        return FilenameUtils.wildcardMatch((String)key.toString(), (String)pattern);
    }

    public static <T> ResourceSelectorArgument<T> resourceSelector(CommandBuildContext context, ResourceKey<? extends Registry<T>> registry) {
        return new ResourceSelectorArgument<T>(context, registry);
    }

    public static <T> Collection<Holder.Reference<T>> getSelectedResources(CommandContext<CommandSourceStack> context, String name) {
        return (Collection)context.getArgument(name, Collection.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T>
    implements ArgumentTypeInfo<ResourceSelectorArgument<T>, Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf out) {
            out.writeResourceKey(template.registryKey);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf in) {
            return new Template(this, in.readRegistryKey());
        }

        @Override
        public void serializeToJson(Template template, JsonObject out) {
            out.addProperty("registry", template.registryKey.identifier().toString());
        }

        @Override
        public Template unpack(ResourceSelectorArgument<T> argument) {
            return new Template(this, argument.registryKey);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<ResourceSelectorArgument<T>> {
            private final ResourceKey<? extends Registry<T>> registryKey;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, ResourceKey<? extends Registry<T>> registryKey) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.registryKey = registryKey;
            }

            @Override
            public ResourceSelectorArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceSelectorArgument(context, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceSelectorArgument<T>, ?> type() {
                return this.this$0;
            }
        }
    }
}

