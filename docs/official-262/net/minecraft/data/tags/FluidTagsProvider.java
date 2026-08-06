/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidIds;

public class FluidTagsProvider
extends TagsProvider<Fluid> {
    public FluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.FLUID, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(FluidTags.WATER).add(FluidIds.WATER, FluidIds.FLOWING_WATER);
        this.tag(FluidTags.LAVA).add(FluidIds.LAVA, FluidIds.FLOWING_LAVA);
        this.tag(FluidTags.SUPPORTS_SUGAR_CANE_ADJACENTLY).addTag(FluidTags.WATER);
        this.tag(FluidTags.SUPPORTS_LILY_PAD).add(FluidIds.WATER);
        this.tag(FluidTags.SUPPORTS_FROGSPAWN).add(FluidIds.WATER);
        this.tag(FluidTags.BUBBLE_COLUMN_CAN_OCCUPY).add(FluidIds.WATER);
    }
}

