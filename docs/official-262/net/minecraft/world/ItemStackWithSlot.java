/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public record ItemStackWithSlot(int slot, ItemStack stack) {
    public static final Codec<ItemStackWithSlot> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.UNSIGNED_BYTE, "Slot", 0).forGetter(ItemStackWithSlot::slot), (App)ItemStack.MAP_CODEC.forGetter(ItemStackWithSlot::stack)).apply((Applicative)i, ItemStackWithSlot::new));

    public boolean isValidInContainer(int containerSize) {
        return this.slot >= 0 && this.slot < containerSize;
    }
}

