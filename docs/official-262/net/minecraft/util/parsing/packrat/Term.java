/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;

public interface Term<S> {
    public boolean parse(ParseState<S> var1, Scope var2, Control var3);

    public static <S, T> Term<S> marker(Atom<T> name, T value) {
        return new Marker(name, value);
    }

    @SafeVarargs
    public static <S> Term<S> sequence(Term<S> ... terms) {
        return new Sequence<S>(terms);
    }

    @SafeVarargs
    public static <S> Term<S> alternative(Term<S> ... terms) {
        return new Alternative<S>(terms);
    }

    public static <S> Term<S> optional(Term<S> term) {
        return new Maybe<S>(term);
    }

    public static <S, T> Term<S> repeated(NamedRule<S, T> element, Atom<List<T>> listName) {
        return Term.repeated(element, listName, 0);
    }

    public static <S, T> Term<S> repeated(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) {
        return new Repeated<S, T>(element, listName, minRepetitions);
    }

    public static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator) {
        return Term.repeatedWithTrailingSeparator(element, listName, separator, 0);
    }

    public static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions) {
        return new RepeatedWithSeparator<S, T>(element, listName, separator, minRepetitions, true);
    }

    public static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator) {
        return Term.repeatedWithoutTrailingSeparator(element, listName, separator, 0);
    }

    public static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions) {
        return new RepeatedWithSeparator<S, T>(element, listName, separator, minRepetitions, false);
    }

    public static <S> Term<S> positiveLookahead(Term<S> term) {
        return new LookAhead<S>(term, true);
    }

    public static <S> Term<S> negativeLookahead(Term<S> term) {
        return new LookAhead<S>(term, false);
    }

    public static <S> Term<S> cut() {
        return new Term<S>(){

            @Override
            public boolean parse(ParseState<S> state, Scope scope, Control control) {
                control.cut();
                return true;
            }

            public String toString() {
                return "\u2191";
            }
        };
    }

    public static <S> Term<S> empty() {
        return new Term<S>(){

            @Override
            public boolean parse(ParseState<S> state, Scope scope, Control control) {
                return true;
            }

            public String toString() {
                return "\u03b5";
            }
        };
    }

    public static <S> Term<S> fail(final Object message) {
        return new Term<S>(){

            @Override
            public boolean parse(ParseState<S> state, Scope scope, Control control) {
                state.errorCollector().store(state.mark(), message);
                return false;
            }

            public String toString() {
                return "fail";
            }
        };
    }

    public record Marker<S, T>(Atom<T> name, T value) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            scope.put(this.name, this.value);
            return true;
        }
    }

    public record Sequence<S>(Term<S>[] elements) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            int mark = state.mark();
            for (Term<S> element : this.elements) {
                if (element.parse(state, scope, control)) continue;
                state.restore(mark);
                return false;
            }
            return true;
        }
    }

    public record Alternative<S>(Term<S>[] elements) implements Term<S>
    {
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            Control controlForThis = state.acquireControl();
            try {
                int mark = state.mark();
                scope.splitFrame();
                for (Term<S> element : this.elements) {
                    if (element.parse(state, scope, controlForThis)) {
                        scope.mergeFrame();
                        boolean bl = true;
                        return bl;
                    }
                    scope.clearFrameValues();
                    state.restore(mark);
                    if (controlForThis.hasCut()) break;
                }
                scope.popFrame();
                boolean bl = false;
                return bl;
            }
            finally {
                state.releaseControl();
            }
        }
    }

    public record Maybe<S>(Term<S> term) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            int mark = state.mark();
            if (!this.term.parse(state, scope, control)) {
                state.restore(mark);
            }
            return true;
        }
    }

    public record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            int entryMark;
            int mark = state.mark();
            ArrayList<T> elements = new ArrayList<T>(this.minRepetitions);
            while (true) {
                entryMark = state.mark();
                T parsedElement = state.parse(this.element);
                if (parsedElement == null) break;
                elements.add(parsedElement);
            }
            state.restore(entryMark);
            if (elements.size() < this.minRepetitions) {
                state.restore(mark);
                return false;
            }
            scope.put(this.listName, elements);
            return true;
        }
    }

    public record RepeatedWithSeparator<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            int listMark = state.mark();
            ArrayList<T> elements = new ArrayList<T>(this.minRepetitions);
            boolean first = true;
            while (true) {
                int markBeforeSeparator = state.mark();
                if (!first && !this.separator.parse(state, scope, control)) {
                    state.restore(markBeforeSeparator);
                    break;
                }
                int markAfterSeparator = state.mark();
                T parsedElement = state.parse(this.element);
                if (parsedElement == null) {
                    if (first) {
                        state.restore(markAfterSeparator);
                        break;
                    }
                    if (this.allowTrailingSeparator) {
                        state.restore(markAfterSeparator);
                        break;
                    }
                    state.restore(listMark);
                    return false;
                }
                elements.add(parsedElement);
                first = false;
            }
            if (elements.size() < this.minRepetitions) {
                state.restore(listMark);
                return false;
            }
            scope.put(this.listName, elements);
            return true;
        }
    }

    public record LookAhead<S>(Term<S> term, boolean positive) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            int mark = state.mark();
            boolean result = this.term.parse(state.silent(), scope, control);
            state.restore(mark);
            return this.positive == result;
        }
    }
}

