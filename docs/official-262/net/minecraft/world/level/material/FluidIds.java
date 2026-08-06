/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;

public class FluidIds {
    public static final ResourceKey<Fluid> EMPTY = FluidIds.create("empty");
    public static final ResourceKey<Fluid> FLOWING_WATER = FluidIds.create("flowing_water");
    public static final ResourceKey<Fluid> WATER = FluidIds.create("water");
    public static final ResourceKey<Fluid> FLOWING_LAVA = FluidIds.create("flowing_lava");
    public static final ResourceKey<Fluid> LAVA = FluidIds.create("lava");

    private static ResourceKey<Fluid> create(String name) {
        return ResourceKey.create(Registries.FLUID, Identifier.withDefaultNamespace(name));
    }
}

