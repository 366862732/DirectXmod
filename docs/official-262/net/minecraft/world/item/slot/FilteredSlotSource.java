/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.slot;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.TransformedSlotSource;

public class FilteredSlotSource
extends TransformedSlotSource {
    public static final MapCodec<FilteredSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> FilteredSlotSource.commonFields(i).and((App)ItemPredicate.CODEC.fieldOf("item_filter").forGetter(t -> t.filter)).apply((Applicative)i, FilteredSlotSource::new));
    private final ItemPredicate filter;

    private FilteredSlotSource(SlotSource slotSource, ItemPredicate filter) {
        super(slotSource);
        this.filter = filter;
    }

    public MapCodec<FilteredSlotSource> codec() {
        return MAP_CODEC;
    }

    @Override
    protected SlotCollection transform(SlotCollection slots) {
        return slots.filter(this.filter);
    }
}

