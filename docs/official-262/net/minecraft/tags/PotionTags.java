/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.alchemy.Potion;

public class PotionTags {
    public static final TagKey<Potion> TRADEABLE = PotionTags.create("tradeable");

    private PotionTags() {
    }

    private static TagKey<Potion> create(String name) {
        return TagKey.create(Registries.POTION, Identifier.withDefaultNamespace(name));
    }
}

