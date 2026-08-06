/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagEntry;

public class TagBuilder {
    private final List<TagEntry> entries = new ArrayList<TagEntry>();
    private boolean replace = false;

    public static TagBuilder create() {
        return new TagBuilder();
    }

    public List<TagEntry> build() {
        return List.copyOf(this.entries);
    }

    public boolean shouldReplace() {
        return this.replace;
    }

    public TagBuilder setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }

    public TagBuilder add(TagEntry entry) {
        this.entries.add(entry);
        return this;
    }

    public TagBuilder addElement(Identifier id) {
        return this.add(TagEntry.element(id));
    }

    public TagBuilder addOptionalElement(Identifier id) {
        return this.add(TagEntry.optionalElement(id));
    }

    public TagBuilder addTag(Identifier id) {
        return this.add(TagEntry.tag(id));
    }

    public TagBuilder addOptionalTag(Identifier id) {
        return this.add(TagEntry.optionalTag(id));
    }
}

