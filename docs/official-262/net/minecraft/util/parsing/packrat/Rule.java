/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.parsing.packrat;

import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import org.jspecify.annotations.Nullable;

public interface Rule<S, T> {
    public @Nullable T parse(ParseState<S> var1);

    public static <S, T> Rule<S, T> fromTerm(Term<S> child, RuleAction<S, T> action) {
        return new WrappedTerm<S, T>(action, child);
    }

    public static <S, T> Rule<S, T> fromTerm(Term<S> child, SimpleRuleAction<S, T> action) {
        return new WrappedTerm<S, T>(action, child);
    }

    public record WrappedTerm<S, T>(RuleAction<S, T> action, Term<S> child) implements Rule<S, T>
    {
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public @Nullable T parse(ParseState<S> state) {
            Scope scope = state.scope();
            scope.pushFrame();
            try {
                if (this.child.parse(state, scope, Control.UNBOUND)) {
                    T t = this.action.run(state);
                    return t;
                }
                T t = null;
                return t;
            }
            finally {
                scope.popFrame();
            }
        }
    }

    @FunctionalInterface
    public static interface RuleAction<S, T> {
        public @Nullable T run(ParseState<S> var1);
    }

    @FunctionalInterface
    public static interface SimpleRuleAction<S, T>
    extends RuleAction<S, T> {
        public T run(Scope var1);

        @Override
        default public T run(ParseState<S> state) {
            return this.run(state.scope());
        }
    }
}

