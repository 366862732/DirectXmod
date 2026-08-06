/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionContentsPredicate<T, P extends Predicate<T>>
extends Predicate<Iterable<? extends T>> {
    public List<P> unpack();

    public static <T, P extends Predicate<T>> Codec<CollectionContentsPredicate<T, P>> codec(Codec<P> elementCodec) {
        return elementCodec.listOf().xmap(CollectionContentsPredicate::of, CollectionContentsPredicate::unpack);
    }

    @SafeVarargs
    public static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(P ... predicates) {
        return CollectionContentsPredicate.of(List.of(predicates));
    }

    public static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(List<P> predicates) {
        return switch (predicates.size()) {
            case 0 -> new Zero();
            case 1 -> new Single((Predicate)predicates.getFirst());
            default -> new Multiple(predicates);
        };
    }

    public static class Zero<T, P extends Predicate<T>>
    implements CollectionContentsPredicate<T, P> {
        @Override
        public boolean test(Iterable<? extends T> values) {
            return true;
        }

        @Override
        public List<P> unpack() {
            return List.of();
        }
    }

    public record Single<T, P extends Predicate<T>>(P test) implements CollectionContentsPredicate<T, P>
    {
        @Override
        public boolean test(Iterable<? extends T> values) {
            for (T value : values) {
                if (!this.test.test(value)) continue;
                return true;
            }
            return false;
        }

        @Override
        public List<P> unpack() {
            return List.of(this.test);
        }
    }

    public record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContentsPredicate<T, P>
    {
        @Override
        public boolean test(Iterable<? extends T> values) {
            ArrayList<P> testsToMatch = new ArrayList<P>(this.tests);
            for (Object value : values) {
                testsToMatch.removeIf(p -> p.test(value));
                if (!testsToMatch.isEmpty()) continue;
                return true;
            }
            return false;
        }

        @Override
        public List<P> unpack() {
            return this.tests;
        }
    }
}

