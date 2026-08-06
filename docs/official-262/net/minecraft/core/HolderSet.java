/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.util.Either
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface HolderSet<T>
extends Iterable<Holder<T>> {
    public Stream<Holder<T>> stream();

    public int size();

    public boolean isBound();

    public Either<TagKey<T>, List<Holder<T>>> unwrap();

    public Optional<Holder<T>> getRandomElement(RandomSource var1);

    public Holder<T> get(int var1);

    public boolean contains(Holder<T> var1);

    public boolean canSerializeIn(HolderOwner<T> var1);

    public Optional<TagKey<T>> unwrapKey();

    @Deprecated
    @VisibleForTesting
    public static <T> Named<T> emptyNamed(HolderOwner<T> owner, TagKey<T> key) {
        return new Named<T>((HolderOwner)owner, (TagKey)key){

            @Override
            protected List<Holder<T>> contents() {
                throw new UnsupportedOperationException("Tag " + String.valueOf(this.key()) + " can't be dereferenced during construction");
            }
        };
    }

    public static <T> HolderSet<T> empty() {
        return Direct.EMPTY;
    }

    @SafeVarargs
    public static <T> Direct<T> direct(Holder<T> ... values) {
        return new Direct<T>(List.of(values));
    }

    public static <T> Direct<T> direct(List<? extends Holder<T>> values) {
        return new Direct(List.copyOf(values));
    }

    @SafeVarargs
    public static <E, T> Direct<T> direct(Function<E, Holder<T>> holderGetter, E ... elements) {
        return HolderSet.direct(Stream.of(elements).map(holderGetter).toList());
    }

    public static <E, T> Direct<T> direct(Function<E, Holder<T>> holderGetter, Collection<E> elements) {
        return HolderSet.direct(elements.stream().map(holderGetter).toList());
    }

    public static final class Direct<T>
    extends ListBacked<T> {
        private static final Direct<?> EMPTY = new Direct(List.of());
        private final List<Holder<T>> contents;
        private @Nullable Set<Holder<T>> contentsSet;

        private Direct(List<Holder<T>> contents) {
            this.contents = contents;
        }

        @Override
        protected List<Holder<T>> contents() {
            return this.contents;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap() {
            return Either.right(this.contents);
        }

        @Override
        public Optional<TagKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public boolean contains(Holder<T> value) {
            if (this.contentsSet == null) {
                this.contentsSet = Set.copyOf(this.contents);
            }
            return this.contentsSet.contains(value);
        }

        public String toString() {
            return "DirectSet[" + String.valueOf(this.contents) + "]";
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Direct)) return false;
            Direct direct = (Direct)obj;
            if (!this.contents.equals(direct.contents)) return false;
            return true;
        }

        public int hashCode() {
            return this.contents.hashCode();
        }
    }

    public static class Named<T>
    extends ListBacked<T> {
        private final HolderOwner<T> owner;
        private final TagKey<T> key;
        private @Nullable List<Holder<T>> contents;

        Named(HolderOwner<T> owner, TagKey<T> key) {
            this.owner = owner;
            this.key = key;
        }

        void bind(List<Holder<T>> contents) {
            this.contents = List.copyOf(contents);
        }

        public TagKey<T> key() {
            return this.key;
        }

        @Override
        protected List<Holder<T>> contents() {
            if (this.contents == null) {
                throw new IllegalStateException("Trying to access unbound tag '" + String.valueOf(this.key) + "' from registry " + String.valueOf(this.owner));
            }
            return this.contents;
        }

        @Override
        public boolean isBound() {
            return this.contents != null;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap() {
            return Either.left(this.key);
        }

        @Override
        public Optional<TagKey<T>> unwrapKey() {
            return Optional.of(this.key);
        }

        @Override
        public boolean contains(Holder<T> value) {
            return value.is(this.key);
        }

        public String toString() {
            return "NamedSet(" + String.valueOf(this.key) + ")[" + String.valueOf(this.contents) + "]";
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> context) {
            return this.owner.canSerializeIn(context);
        }
    }

    public static abstract class ListBacked<T>
    implements HolderSet<T> {
        protected abstract List<Holder<T>> contents();

        @Override
        public int size() {
            return this.contents().size();
        }

        @Override
        public Spliterator<Holder<T>> spliterator() {
            return this.contents().spliterator();
        }

        @Override
        public Iterator<Holder<T>> iterator() {
            return this.contents().iterator();
        }

        @Override
        public Stream<Holder<T>> stream() {
            return this.contents().stream();
        }

        @Override
        public Optional<Holder<T>> getRandomElement(RandomSource random) {
            return Util.getRandomSafe(this.contents(), random);
        }

        @Override
        public Holder<T> get(int index) {
            return this.contents().get(index);
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> owner) {
            return true;
        }
    }
}

