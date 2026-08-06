/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.datafixers.util.Either
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
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
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ResourceOrTagKeyArgument<T>
implements ArgumentType<Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceOrTagKeyArgument(ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
    }

    public static <T> ResourceOrTagKeyArgument<T> resourceOrTagKey(ResourceKey<? extends Registry<T>> key) {
        return new ResourceOrTagKeyArgument<T>(key);
    }

    public static <T> Result<T> getResourceOrTagKey(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryKey, DynamicCommandExceptionType exceptionType) throws CommandSyntaxException {
        Result argument = (Result)context.getArgument(name, Result.class);
        Optional<Result<T>> value = argument.cast(registryKey);
        return value.orElseThrow(() -> exceptionType.create((Object)argument));
    }

    public Result<T> parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '#') {
            int cursor = reader.getCursor();
            try {
                reader.skip();
                Identifier tagId = Identifier.read(reader);
                return new TagResult(TagKey.create(this.registryKey, tagId));
            }
            catch (CommandSyntaxException e) {
                reader.setCursor(cursor);
                throw e;
            }
        }
        Identifier resourceId = Identifier.read(reader);
        return new ResourceResult(ResourceKey.create(this.registryKey, resourceId));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static interface Result<T>
    extends Predicate<Holder<T>> {
        public Either<ResourceKey<T>, TagKey<T>> unwrap();

        public <E> Optional<Result<E>> cast(ResourceKey<? extends Registry<E>> var1);

        public String asPrintable();
    }

    private record TagResult<T>(TagKey<T> key) implements Result<T>
    {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> unwrap() {
            return Either.right(this.key);
        }

        @Override
        public <E> Optional<Result<E>> cast(ResourceKey<? extends Registry<E>> registryKey) {
            return this.key.cast(registryKey).map(TagResult::new);
        }

        @Override
        public boolean test(Holder<T> holder) {
            return holder.is(this.key);
        }

        @Override
        public String asPrintable() {
            return "#" + String.valueOf(this.key.location());
        }
    }

    private record ResourceResult<T>(ResourceKey<T> key) implements Result<T>
    {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> unwrap() {
            return Either.left(this.key);
        }

        @Override
        public <E> Optional<Result<E>> cast(ResourceKey<? extends Registry<E>> registryKey) {
            return this.key.cast(registryKey).map(ResourceResult::new);
        }

        @Override
        public boolean test(Holder<T> holder) {
            return holder.is(this.key);
        }

        @Override
        public String asPrintable() {
            return this.key.identifier().toString();
        }
    }

    public static class Info<T>
    implements ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, Template> {
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
        public Template unpack(ResourceOrTagKeyArgument<T> argument) {
            return new Template(this, argument.registryKey);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<ResourceOrTagKeyArgument<T>> {
            private final ResourceKey<? extends Registry<T>> registryKey;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, ResourceKey<? extends Registry<T>> registryKey) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.registryKey = registryKey;
            }

            @Override
            public ResourceOrTagKeyArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceOrTagKeyArgument(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ?> type() {
                return this.this$0;
            }
        }
    }
}

