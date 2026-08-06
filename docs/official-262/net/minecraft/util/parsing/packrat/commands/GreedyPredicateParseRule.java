/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public abstract class GreedyPredicateParseRule
implements Rule<StringReader, String> {
    private final int minSize;
    private final int maxSize;
    private final DelayedException<CommandSyntaxException> error;

    public GreedyPredicateParseRule(int minSize, DelayedException<CommandSyntaxException> error) {
        this(minSize, Integer.MAX_VALUE, error);
    }

    public GreedyPredicateParseRule(int minSize, int maxSize, DelayedException<CommandSyntaxException> error) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.error = error;
    }

    @Override
    public @Nullable String parse(ParseState<StringReader> state) {
        int start;
        int pos;
        StringReader input = state.input();
        String fullString = input.getString();
        for (pos = start = input.getCursor(); pos < fullString.length() && this.isAccepted(fullString.charAt(pos)) && pos - start < this.maxSize; ++pos) {
        }
        int length = pos - start;
        if (length < this.minSize) {
            state.errorCollector().store(state.mark(), this.error);
            return null;
        }
        input.setCursor(pos);
        return fullString.substring(start, pos);
    }

    protected abstract boolean isAccepted(char var1);
}

