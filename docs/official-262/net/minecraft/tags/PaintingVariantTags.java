/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;

public class PaintingVariantTags {
    public static final TagKey<PaintingVariant> PLACEABLE = PaintingVariantTags.create("placeable");

    private PaintingVariantTags() {
    }

    private static TagKey<PaintingVariant> create(String name) {
        return TagKey.create(Registries.PAINTING_VARIANT, Identifier.withDefaultNamespace(name));
    }
}

