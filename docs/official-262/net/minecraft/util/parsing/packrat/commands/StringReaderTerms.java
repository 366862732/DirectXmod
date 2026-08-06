/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  it.unimi.dsi.fastutil.chars.CharList
 */
package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.Term;

public interface StringReaderTerms {
    public static Term<StringReader> word(String value) {
        return new TerminalWord(value);
    }

    public static Term<StringReader> character(final char value) {
        return new TerminalCharacters(CharList.of((char)value)){

            @Override
            protected boolean isAccepted(char v) {
                return value == v;
            }
        };
    }

    public static Term<StringReader> characters(final char v1, final char v2) {
        return new TerminalCharacters(CharList.of((char)v1, (char)v2)){

            @Override
            protected boolean isAccepted(char v) {
                return v == v1 || v == v2;
            }
        };
    }

    public static StringReader createReader(String contents, int cursor) {
        StringReader reader = new StringReader(contents);
        reader.setCursor(cursor);
        return reader;
    }

    public static final class TerminalWord
    implements Term<StringReader> {
        private final String value;
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalWord(String value) {
            this.value = value;
            this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), value);
            this.suggestions = s -> Stream.of(value);
        }

        @Override
        public boolean parse(ParseState<StringReader> state, Scope scope, Control control) {
            state.input().skipWhitespace();
            int cursor = state.mark();
            String value = state.input().readUnquotedString();
            if (!value.equals(this.value)) {
                state.errorCollector().store(cursor, this.suggestions, this.error);
                return false;
            }
            return true;
        }

        public String toString() {
            return "terminal[" + this.value + "]";
        }
    }

    public static abstract class TerminalCharacters
    implements Term<StringReader> {
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalCharacters(CharList values) {
            String joinedValues = values.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
            this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), joinedValues);
            this.suggestions = s -> values.intStream().mapToObj(Character::toString);
        }

        @Override
        public boolean parse(ParseState<StringReader> state, Scope scope, Control control) {
            state.input().skipWhitespace();
            int cursor = state.mark();
            if (!state.input().canRead() || !this.isAccepted(state.input().read())) {
                state.errorCollector().store(cursor, this.suggestions, this.error);
                return false;
            }
            return true;
        }

        protected abstract boolean isAccepted(char var1);
    }
}

