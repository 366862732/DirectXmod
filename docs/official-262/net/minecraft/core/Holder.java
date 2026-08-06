/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

public sealed interface Holder<T> {
    public T value();

    public boolean isBound();

    public boolean areComponentsBound();

    public boolean is(Identifier var1);

    public boolean is(ResourceKey<T> var1);

    public boolean is(Predicate<ResourceKey<T>> var1);

    public boolean is(TagKey<T> var1);

    @Deprecated
    public boolean is(Holder<T> var1);

    public Stream<TagKey<T>> tags();

    public DataComponentMap components();

    public Either<ResourceKey<T>, T> unwrap();

    public Optional<ResourceKey<T>> unwrapKey();

    public Kind kind();

    public boolean canSerializeIn(HolderOwner<T> var1);

    default public String getRegisteredName() {
        return this.unwrapKey().map(key -> key.identifier().toString()).orElse("[unregistered]");
    }

    public static <T> Holder<T> direct(T value) {
        return new Direct<T>(value, DataComponentMap.EMPTY);
    }

    public static <T> Holder<T> direct(T value, DataComponentMap components) {
        return new Direct<T>(value, components);
    }

    public record Direct<T>(T value, DataComponentMap components) implements Holder<T>
    {
        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean areComponentsBound() {
            return true;
        }

        @Override
        public boolean is(Identifier key) {
            return false;
        }

        @Override
        public boolean is(ResourceKey<T> key) {
            return false;
        }

        @Override
        public boolean is(TagKey<T> tag) {
            return false;
        }

        @Override
        public boolean is(Holder<T> holder) {
            return this.value.equals(holder.value());
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> predicate) {
            return false;
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.right(this.value);
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public Kind kind() {
            return Kind.DIRECT;
        }

        @Override
        public String toString() {
            return "Direct{" + String.valueOf(this.value) + "}";
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> registry) {
            return true;
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return Stream.of(new TagKey[0]);
        }
    }

    public static class Reference<T>
    implements Holder<T> {
        private final HolderOwner<T> owner;
        private @Nullable Set<TagKey<T>> tags;
        private @Nullable DataComponentMap components;
        private final Type type;
        private @Nullable ResourceKey<T> key;
        private @Nullable T value;

        protected Reference(Type type, HolderOwner<T> owner, @Nullable ResourceKey<T> key, @Nullable T value) {
            this.owner = owner;
            this.type = type;
            this.key = key;
            this.value = value;
        }

        public static <T> Reference<T> createStandAlone(HolderOwner<T> owner, ResourceKey<T> key) {
            return new Reference<Object>(Type.STAND_ALONE, owner, key, null);
        }

        @Deprecated
        public static <T> Reference<T> createIntrusive(HolderOwner<T> owner, @Nullable T value) {
            return new Reference<T>(Type.INTRUSIVE, owner, null, value);
        }

        public ResourceKey<T> key() {
            if (this.key == null) {
                throw new IllegalStateException("Trying to access unbound value '" + String.valueOf(this.value) + "' from registry " + String.valueOf(this.owner));
            }
            return this.key;
        }

        @Override
        public T value() {
            if (this.value == null) {
                throw new IllegalStateException("Trying to access unbound value '" + String.valueOf(this.key) + "' from registry " + String.valueOf(this.owner));
            }
            return this.value;
        }

        @Override
        public boolean is(Identifier key) {
            return this.key().identifier().equals(key);
        }

        @Override
        public boolean is(ResourceKey<T> key) {
            return this.key() == key;
        }

        private Set<TagKey<T>> boundTags() {
            if (this.tags == null) {
                throw new IllegalStateException("Tags not bound");
            }
            return this.tags;
        }

        @Override
        public boolean is(TagKey<T> tag) {
            return this.boundTags().contains(tag);
        }

        @Override
        public boolean is(Holder<T> holder) {
            return holder.is(this.key());
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> predicate) {
            return predicate.test(this.key());
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> context) {
            return this.owner.canSerializeIn(context);
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.left(this.key());
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.of(this.key());
        }

        @Override
        public Kind kind() {
            return Kind.REFERENCE;
        }

        @Override
        public boolean isBound() {
            return this.key != null && this.value != null;
        }

        @Override
        public boolean areComponentsBound() {
            return this.components != null;
        }

        void bindKey(ResourceKey<T> key) {
            if (this.key != null && key != this.key) {
                throw new IllegalStateException("Can't change holder key: existing=" + String.valueOf(this.key) + ", new=" + String.valueOf(key));
            }
            this.key = key;
        }

        protected void bindValue(T value) {
            if (this.type == Type.INTRUSIVE && this.value != value) {
                throw new IllegalStateException("Can't change holder " + String.valueOf(this.key) + " value: existing=" + String.valueOf(this.value) + ", new=" + String.valueOf(value));
            }
            this.value = value;
        }

        void bindTags(Collection<TagKey<T>> tags) {
            this.tags = Set.copyOf(tags);
        }

        public void bindComponents(DataComponentMap components) {
            this.components = components;
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return this.boundTags().stream();
        }

        @Override
        public DataComponentMap components() {
            return Objects.requireNonNull(this.components, "Components not bound yet");
        }

        public String toString() {
            return "Reference{" + String.valueOf(this.key) + "=" + String.valueOf(this.value) + "}";
        }

        protected static enum Type {
            STAND_ALONE,
            INTRUSIVE;

        }
    }

    public static enum Kind {
        REFERENCE,
        DIRECT;

    }
}

