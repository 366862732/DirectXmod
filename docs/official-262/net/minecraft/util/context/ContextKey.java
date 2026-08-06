/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.context;

import net.minecraft.resources.Identifier;

public class ContextKey<T> {
    private final Identifier name;

    public ContextKey(Identifier name) {
        this.name = name;
    }

    public static <T> ContextKey<T> vanilla(String name) {
        return new ContextKey<T>(Identifier.withDefaultNamespace(name));
    }

    public Identifier name() {
        return this.name;
    }

    public String toString() {
        return "<parameter " + String.valueOf(this.name) + ">";
    }
}

