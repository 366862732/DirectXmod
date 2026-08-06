/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class FluidTags {
    public static final TagKey<Fluid> WATER = FluidTags.create("water");
    public static final TagKey<Fluid> LAVA = FluidTags.create("lava");
    public static final TagKey<Fluid> SUPPORTS_SUGAR_CANE_ADJACENTLY = FluidTags.create("supports_sugar_cane_adjacently");
    public static final TagKey<Fluid> SUPPORTS_LILY_PAD = FluidTags.create("supports_lily_pad");
    public static final TagKey<Fluid> SUPPORTS_FROGSPAWN = FluidTags.create("supports_frogspawn");
    public static final TagKey<Fluid> BUBBLE_COLUMN_CAN_OCCUPY = FluidTags.create("bubble_column_can_occupy");

    private FluidTags() {
    }

    private static TagKey<Fluid> create(String name) {
        return TagKey.create(Registries.FLUID, Identifier.withDefaultNamespace(name));
    }
}

