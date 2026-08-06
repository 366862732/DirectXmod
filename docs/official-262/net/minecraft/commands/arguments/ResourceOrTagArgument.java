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
 *  com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.datafixers.util.Either
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ResourceOrTagArgument<T>
implements ArgumentType<Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private static final Dynamic2CommandExceptionType ERROR_UNKNOWN_TAG = new Dynamic2CommandExceptionType((id, registry) -> Component.translatableEscape("argument.resource_tag.not_found", id, registry));
    private static final Dynamic3CommandExceptionType ERROR_INVALID_TAG_TYPE = new Dynamic3CommandExceptionType((id, actualRegistry, expectedRegistry) -> Component.translatableEscape("argument.resource_tag.invalid_type", id, actualRegistry, expectedRegistry));
    private final HolderLookup<T> registryLookup;
    private final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceOrTagArgument(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.registryLookup = context.lookupOrThrow(registryKey);
    }

    public static <T> ResourceOrTagArgument<T> resourceOrTag(CommandBuildContext context, ResourceKey<? extends Registry<T>> key) {
        return new ResourceOrTagArgument<T>(context, key);
    }

    public static <T> Result<T> getResourceOrTag(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryKey) throws CommandSyntaxException {
        Result argument = (Result)context.getArgument(name, Result.class);
        Optional<Result<T>> value = argument.cast(registryKey);
        return value.orElseThrow(() -> (CommandSyntaxException)((Object)((Object)argument.unwrap().map(element -> {
            ResourceKey elementKey = element.key();
            return ResourceArgument.ERROR_INVALID_RESOURCE_TYPE.create((Object)elementKey.identifier(), (Object)elementKey.registry(), (Object)registryKey.identifier());
        }, tag -> {
            TagKey tagKey = tag.key();
            return ERROR_INVALID_TAG_TYPE.create((Object)tagKey.location(), tagKey.registry(), (Object)registryKey.identifier());
        }))));
    }

    public Result<T> parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '#') {
            int cursor = reader.getCursor();
            try {
                reader.skip();
                Identifier tagId = Identifier.read(reader);
                TagKey tagKey = TagKey.create(this.registryKey, tagId);
                HolderSet.Named holderSet = this.registryLookup.get(tagKey).orElseThrow(() -> ERROR_UNKNOWN_TAG.createWithContext((ImmutableStringReader)reader, (Object)tagId, (Object)this.registryKey.identifier()));
                return new TagResult(holderSet);
            }
            catch (CommandSyntaxException e) {
                reader.setCursor(cursor);
                throw e;
            }
        }
        Identifier resourceId = Identifier.read(reader);
        ResourceKey resourceKey = ResourceKey.create(this.registryKey, resourceId);
        Holder.Reference holder = this.registryLookup.get(resourceKey).orElseThrow(() -> ResourceArgument.ERROR_UNKNOWN_RESOURCE.createWithContext((ImmutableStringReader)reader, (Object)resourceId, (Object)this.registryKey.identifier()));
        return new ResourceResult(holder);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static interface Result<T>
    extends Predicate<Holder<T>> {
        public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap();

        public <E> Optional<Result<E>> cast(ResourceKey<? extends Registry<E>> var1);

        public String asPrintable();
    }

    private record TagResult<T>(HolderSet.Named<T> tag) implements Result<T>
    {
        @Override
        public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
            return Either.right(this.tag);
        }

        @Override
        public <E> Optional<Result<E>> cast(ResourceKey<? extends Registry<E>> registryKey) {
            return this.tag.key().isFor(registryKey) ? Optional.of(this) : Optional.empty();
        }

        @Override
        public boolean test(Holder<T> holder) {
            return this.tag.contains(holder);
        }

        @Override
        public String asPrintable() {
            return "#" + String.valueOf(this.tag.key().location());
        }
    }

    private record ResourceResult<T>(Holder.Reference<T> value) implements Result<T>
    {
        @Override
        public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
            return Either.left(this.value);
        }

        @Override
        public <E> Optional<Result<E>> cast(ResourceKey<? extends Registry<E>> registryKey) {
            return this.value.key().isFor(registryKey) ? Optional.of(this) : Optional.empty();
        }

        @Override
        public boolean test(Holder<T> holder) {
            return holder.equals(this.value);
        }

        @Override
        public String asPrintable() {
            return this.value.key().identifier().toString();
        }
    }

    public static class Info<T>
    implements ArgumentTypeInfo<ResourceOrTagArgument<T>, Template> {
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
        public Template unpack(ResourceOrTagArgument<T> argument) {
            return new Template(this, argument.registryKey);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<ResourceOrTagArgument<T>> {
            private final ResourceKey<? extends Registry<T>> registryKey;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, ResourceKey<? extends Registry<T>> registryKey) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.registryKey = registryKey;
            }

            @Override
            public ResourceOrTagArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceOrTagArgument(context, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceOrTagArgument<T>, ?> type() {
                return this.this$0;
            }
        }
    }
}

