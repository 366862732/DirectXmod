/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.parsing.packrat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import org.jspecify.annotations.Nullable;

public class Dictionary<S> {
    private final Map<Atom<?>, Entry<S, ?>> terms = new IdentityHashMap();

    public <T> NamedRule<S, T> put(Atom<T> name, Rule<S, T> entry) {
        Entry holder = this.terms.computeIfAbsent(name, Entry::new);
        if (holder.value != null) {
            throw new IllegalArgumentException("Trying to override rule: " + String.valueOf(name));
        }
        holder.value = entry;
        return holder;
    }

    public <T> NamedRule<S, T> putComplex(Atom<T> name, Term<S> term, Rule.RuleAction<S, T> action) {
        return this.put(name, Rule.fromTerm(term, action));
    }

    public <T> NamedRule<S, T> put(Atom<T> name, Term<S> term, Rule.SimpleRuleAction<S, T> action) {
        return this.put(name, Rule.fromTerm(term, action));
    }

    public void checkAllBound() {
        List<Atom> unboundNames = this.terms.entrySet().stream().filter(e -> ((Entry)e.getValue()).value == null).map(Map.Entry::getKey).toList();
        if (!unboundNames.isEmpty()) {
            throw new IllegalStateException("Unbound names: " + String.valueOf(unboundNames));
        }
    }

    public <T> NamedRule<S, T> getOrThrow(Atom<T> name) {
        return Objects.requireNonNull(this.terms.get(name), () -> "No rule called " + String.valueOf(name));
    }

    public <T> NamedRule<S, T> forward(Atom<T> name) {
        return this.getOrCreateEntry(name);
    }

    private <T> Entry<S, T> getOrCreateEntry(Atom<T> name) {
        return this.terms.computeIfAbsent(name, Entry::new);
    }

    public <T> Term<S> named(Atom<T> name) {
        return new Reference<S, T>(this.getOrCreateEntry(name), name);
    }

    public <T> Term<S> namedWithAlias(Atom<T> nameToParse, Atom<T> nameToStore) {
        return new Reference<S, T>(this.getOrCreateEntry(nameToParse), nameToStore);
    }

    private static class Entry<S, T>
    implements NamedRule<S, T>,
    Supplier<String> {
        private final Atom<T> name;
        private @Nullable Rule<S, T> value;

        private Entry(Atom<T> name) {
            this.name = name;
        }

        @Override
        public Atom<T> name() {
            return this.name;
        }

        @Override
        public Rule<S, T> value() {
            return Objects.requireNonNull(this.value, this);
        }

        @Override
        public String get() {
            return "Unbound rule " + String.valueOf(this.name);
        }
    }

    private record Reference<S, T>(Entry<S, T> ruleToParse, Atom<T> nameToStore) implements Term<S>
    {
        @Override
        public boolean parse(ParseState<S> state, Scope scope, Control control) {
            T result = state.parse(this.ruleToParse);
            if (result == null) {
                return false;
            }
            scope.put(this.nameToStore, result);
            return true;
        }
    }
}

