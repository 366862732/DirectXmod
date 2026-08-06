/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<T> {
    public TagAppender<T> add(ResourceKey<T> var1);

    default public TagAppender<T> add(ResourceKey<T> ... elements) {
        return this.addAll(Arrays.stream(elements));
    }

    default public TagAppender<T> addAll(Collection<ResourceKey<T>> elements) {
        elements.forEach(this::add);
        return this;
    }

    default public TagAppender<T> addAll(Stream<ResourceKey<T>> elements) {
        elements.forEach(this::add);
        return this;
    }

    public TagAppender<T> addOptional(ResourceKey<T> var1);

    public TagAppender<T> addTag(TagKey<T> var1);

    public TagAppender<T> addOptionalTag(TagKey<T> var1);

    public static <T> TagAppender<T> forBuilder(final TagBuilder builder) {
        return new TagAppender<T>(){

            @Override
            public TagAppender<T> add(ResourceKey<T> element) {
                builder.addElement(element.identifier());
                return this;
            }

            @Override
            public TagAppender<T> addOptional(ResourceKey<T> element) {
                builder.addOptionalElement(element.identifier());
                return this;
            }

            @Override
            public TagAppender<T> addTag(TagKey<T> tag) {
                builder.addTag(tag.location());
                return this;
            }

            @Override
            public TagAppender<T> addOptionalTag(TagKey<T> tag) {
                builder.addOptionalTag(tag.location());
                return this;
            }
        };
    }
}

