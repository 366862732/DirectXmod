/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft;

import java.util.Objects;

@FunctionalInterface
public interface CharPredicate {
    public boolean test(char var1);

    default public CharPredicate and(CharPredicate other) {
        Objects.requireNonNull(other);
        return value -> this.test(value) && other.test(value);
    }

    default public CharPredicate negate() {
        return value -> !this.test(value);
    }

    default public CharPredicate or(CharPredicate other) {
        Objects.requireNonNull(other);
        return value -> this.test(value) || other.test(value);
    }
}

