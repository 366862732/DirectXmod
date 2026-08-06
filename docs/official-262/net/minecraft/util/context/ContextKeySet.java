/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.google.common.collect.Sets
 */
package net.minecraft.util.context;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.util.context.ContextKey;

public class ContextKeySet {
    private final Set<ContextKey<?>> required;
    private final Set<ContextKey<?>> allowed;

    private ContextKeySet(Set<ContextKey<?>> required, Set<ContextKey<?>> optional) {
        this.required = Set.copyOf(required);
        this.allowed = Set.copyOf(Sets.union(required, optional));
    }

    public Set<ContextKey<?>> required() {
        return this.required;
    }

    public Set<ContextKey<?>> allowed() {
        return this.allowed;
    }

    public String toString() {
        return "[" + Joiner.on((String)", ").join(this.allowed.stream().map(k -> (this.required.contains(k) ? "!" : "") + String.valueOf(k.name())).iterator()) + "]";
    }

    public static class Builder {
        private final Set<ContextKey<?>> required = Sets.newIdentityHashSet();
        private final Set<ContextKey<?>> optional = Sets.newIdentityHashSet();

        public Builder required(ContextKey<?> param) {
            if (this.optional.contains(param)) {
                throw new IllegalArgumentException("Parameter " + String.valueOf(param.name()) + " is already optional");
            }
            this.required.add(param);
            return this;
        }

        public Builder optional(ContextKey<?> param) {
            if (this.required.contains(param)) {
                throw new IllegalArgumentException("Parameter " + String.valueOf(param.name()) + " is already required");
            }
            this.optional.add(param);
            return this;
        }

        public ContextKeySet build() {
            return new ContextKeySet(this.required, this.optional);
        }
    }
}

