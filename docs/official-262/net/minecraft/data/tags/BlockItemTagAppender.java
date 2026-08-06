/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.tags;

import java.util.Arrays;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public abstract class BlockItemTagAppender<Element>
implements TagAppender<Element> {
    private final TagAppender<Element> original;

    public BlockItemTagAppender(TagAppender<Element> original) {
        this.original = original;
    }

    protected abstract ResourceKey<Element> convertElement(BlockItemId var1);

    @Override
    public BlockItemTagAppender<Element> add(ResourceKey<Element> element) {
        this.original.add(element);
        return this;
    }

    public BlockItemTagAppender<Element> add(BlockItemId ... ids) {
        this.original.addAll(Arrays.stream(ids).map(this::convertElement));
        return this;
    }

    @Override
    public BlockItemTagAppender<Element> addAll(ColorCollection<ResourceKey<Element>> collection) {
        collection.forEach(resourceKey -> this.add((ResourceKey)resourceKey));
        return this;
    }

    @Override
    public BlockItemTagAppender<Element> addAll(WeatheringCopperCollection<ResourceKey<Element>> collection) {
        collection.forEach(resourceKey -> this.add((ResourceKey)resourceKey));
        return this;
    }

    @Override
    @SafeVarargs
    public final BlockItemTagAppender<Element> add(ResourceKey<Element> ... elements) {
        this.original.add(elements);
        return this;
    }

    @Override
    public BlockItemTagAppender<Element> addOptional(ResourceKey<Element> element) {
        this.original.addOptional(element);
        return this;
    }

    @Override
    public BlockItemTagAppender<Element> addTag(TagKey<Element> tag) {
        this.original.addTag(tag);
        return this;
    }

    @Override
    public BlockItemTagAppender<Element> addOptionalTag(TagKey<Element> tag) {
        this.original.addOptionalTag(tag);
        return this;
    }
}

