/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.ErrorEntry;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ResourceSuggestion;
import net.minecraft.util.parsing.packrat.commands.StringReaderParserState;

public record Grammar<T>(Dictionary<StringReader> rules, NamedRule<StringReader, T> top) implements CommandArgumentParser<T>
{
    public Grammar {
        rules.checkAllBound();
    }

    public Optional<T> parse(ParseState<StringReader> state) {
        return state.parseTopRule(this.top);
    }

    @Override
    public T parseForCommands(StringReader reader) throws CommandSyntaxException {
        Object r;
        ErrorCollector.LongestOnly<StringReader> errorCollector = new ErrorCollector.LongestOnly<StringReader>();
        StringReaderParserState state = new StringReaderParserState(errorCollector, reader);
        Optional<T> result = this.parse(state);
        if (result.isPresent()) {
            return result.get();
        }
        List<ErrorEntry<StringReader>> errorEntries = errorCollector.entries();
        List exceptions = errorEntries.stream().mapMulti((entry, output) -> {
            Object patt0$temp = entry.reason();
            if (patt0$temp instanceof DelayedException) {
                DelayedException delayedException = (DelayedException)patt0$temp;
                output.accept(delayedException.create(reader.getString(), entry.cursor()));
            } else {
                Object patt1$temp = entry.reason();
                if (patt1$temp instanceof Exception) {
                    Exception exception = (Exception)patt1$temp;
                    output.accept(exception);
                }
            }
        }).toList();
        for (Exception exception : exceptions) {
            if (!(exception instanceof CommandSyntaxException)) continue;
            CommandSyntaxException cse = (CommandSyntaxException)((Object)exception);
            throw cse;
        }
        if (exceptions.size() == 1 && (r = exceptions.get(0)) instanceof RuntimeException) {
            RuntimeException re = (RuntimeException)r;
            throw re;
        }
        throw new IllegalStateException("Failed to parse: " + errorEntries.stream().map(ErrorEntry::toString).collect(Collectors.joining(", ")));
    }

    @Override
    public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder suggestionsBuilder) {
        StringReader reader = new StringReader(suggestionsBuilder.getInput());
        reader.setCursor(suggestionsBuilder.getStart());
        ErrorCollector.LongestOnly<StringReader> errorCollector = new ErrorCollector.LongestOnly<StringReader>();
        StringReaderParserState state = new StringReaderParserState(errorCollector, reader);
        this.parse(state);
        List<ErrorEntry<StringReader>> errorEntries = errorCollector.entries();
        if (errorEntries.isEmpty()) {
            return suggestionsBuilder.buildFuture();
        }
        SuggestionsBuilder offsetBuilder = suggestionsBuilder.createOffset(errorCollector.cursor());
        for (ErrorEntry<StringReader> entry : errorEntries) {
            SuggestionSupplier<StringReader> suggestionSupplier = entry.suggestions();
            if (suggestionSupplier instanceof ResourceSuggestion) {
                ResourceSuggestion resourceSuggestionTerm = (ResourceSuggestion)suggestionSupplier;
                SharedSuggestionProvider.suggestResource(resourceSuggestionTerm.possibleResources(), offsetBuilder);
                continue;
            }
            SharedSuggestionProvider.suggest(entry.suggestions().possibleValues(state), offsetBuilder);
        }
        return offsetBuilder.buildFuture();
    }
}

