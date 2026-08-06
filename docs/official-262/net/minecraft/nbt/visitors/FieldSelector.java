/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt.visitors;

import java.util.List;
import net.minecraft.nbt.TagType;

public record FieldSelector(List<String> path, TagType<?> type, String name) {
    public FieldSelector(TagType<?> type, String name) {
        this(List.of(), type, name);
    }

    public FieldSelector(String parent, TagType<?> type, String name) {
        this(List.of(parent), type, name);
    }

    public FieldSelector(String grandparent, String parent, TagType<?> type, String name) {
        this(List.of(grandparent, parent), type, name);
    }
}

