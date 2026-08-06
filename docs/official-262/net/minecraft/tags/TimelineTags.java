/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.timeline.Timeline;

public interface TimelineTags {
    public static final TagKey<Timeline> UNIVERSAL = TimelineTags.create("universal");
    public static final TagKey<Timeline> IN_OVERWORLD = TimelineTags.create("in_overworld");
    public static final TagKey<Timeline> IN_NETHER = TimelineTags.create("in_nether");
    public static final TagKey<Timeline> IN_END = TimelineTags.create("in_end");

    private static TagKey<Timeline> create(String name) {
        return TagKey.create(Registries.TIMELINE, Identifier.withDefaultNamespace(name));
    }
}

