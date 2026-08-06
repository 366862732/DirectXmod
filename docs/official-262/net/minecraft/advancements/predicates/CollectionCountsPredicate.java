/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.MinMaxBounds;

public interface CollectionCountsPredicate<T, P extends Predicate<T>>
extends Predicate<Iterable<? extends T>> {
    public List<Entry<T, P>> unpack();

    public static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate<T, P>> codec(Codec<P> elementCodec) {
        return Entry.codec(elementCodec).listOf().xmap(CollectionCountsPredicate::of, CollectionCountsPredicate::unpack);
    }

    @SafeVarargs
    public static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(Entry<T, P> ... predicates) {
        return CollectionCountsPredicate.of(List.of(predicates));
    }

    public static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(List<Entry<T, P>> predicates) {
        return switch (predicates.size()) {
            case 0 -> new Zero();
            case 1 -> new Single<T, P>(predicates.getFirst());
            default -> new Multiple<T, P>(predicates);
        };
    }

    public record Entry<T, P extends Predicate<T>>(P test, MinMaxBounds.Ints count) {
        public static <T, P extends Predicate<T>> Codec<Entry<T, P>> codec(Codec<P> elementCodec) {
            return RecordCodecBuilder.create(i -> i.group((App)elementCodec.fieldOf("test").forGetter(Entry::test), (App)MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(Entry::count)).apply((Applicative)i, Entry::new));
        }

        public boolean test(Iterable<? extends T> values) {
            int count = 0;
            for (T value : values) {
                if (!this.test.test(value)) continue;
                ++count;
            }
            return this.count.matches(count);
        }
    }

    public static class Zero<T, P extends Predicate<T>>
    implements CollectionCountsPredicate<T, P> {
        @Override
        public boolean test(Iterable<? extends T> values) {
            return true;
        }

        @Override
        public List<Entry<T, P>> unpack() {
            return List.of();
        }
    }

    public record Single<T, P extends Predicate<T>>(Entry<T, P> entry) implements CollectionCountsPredicate<T, P>
    {
        @Override
        public boolean test(Iterable<? extends T> values) {
            return this.entry.test(values);
        }

        @Override
        public List<Entry<T, P>> unpack() {
            return List.of(this.entry);
        }
    }

    public record Multiple<T, P extends Predicate<T>>(List<Entry<T, P>> entries) implements CollectionCountsPredicate<T, P>
    {
        @Override
        public boolean test(Iterable<? extends T> values) {
            for (Entry<? extends T, P> entry : this.entries) {
                if (entry.test(values)) continue;
                return false;
            }
            return true;
        }

        @Override
        public List<Entry<T, P>> unpack() {
            return this.entries;
        }
    }
}

