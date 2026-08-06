/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface CommandArgumentParser<T> {
    public T parseForCommands(StringReader var1) throws CommandSyntaxException;

    public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder var1);

    default public <S> CommandArgumentParser<S> mapResult(final Function<T, S> mapper) {
        return new CommandArgumentParser<S>(this){
            final /* synthetic */ CommandArgumentParser this$0;
            {
                CommandArgumentParser commandArgumentParser = this$0;
                Objects.requireNonNull(commandArgumentParser);
                this.this$0 = commandArgumentParser;
            }

            @Override
            public S parseForCommands(StringReader reader) throws CommandSyntaxException {
                return mapper.apply(this.this$0.parseForCommands(reader));
            }

            @Override
            public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder suggestionsBuilder) {
                return this.this$0.parseForSuggestions(suggestionsBuilder);
            }
        };
    }

    default public <T, O> CommandArgumentParser<T> withCodec(final DynamicOps<O> ops, final CommandArgumentParser<O> valueParser, final Codec<T> codec, final DynamicCommandExceptionType exceptionType) {
        return new CommandArgumentParser<T>(this){
            final /* synthetic */ CommandArgumentParser this$0;
            {
                CommandArgumentParser commandArgumentParser2 = this$0;
                Objects.requireNonNull(commandArgumentParser2);
                this.this$0 = commandArgumentParser2;
            }

            @Override
            public T parseForCommands(StringReader reader) throws CommandSyntaxException {
                int cursor = reader.getCursor();
                Object tag = valueParser.parseForCommands(reader);
                DataResult result = codec.parse(ops, tag);
                return result.getOrThrow(message -> {
                    reader.setCursor(cursor);
                    return exceptionType.createWithContext((ImmutableStringReader)reader, message);
                });
            }

            @Override
            public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder suggestionsBuilder) {
                return this.this$0.parseForSuggestions(suggestionsBuilder);
            }
        };
    }
}

