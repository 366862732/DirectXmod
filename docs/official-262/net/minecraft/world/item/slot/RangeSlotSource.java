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
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;

public class RangeSlotSource
implements SlotSource {
    public static final MapCodec<RangeSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LootContextArg.ENTITY_OR_BLOCK.fieldOf("source").forGetter(t -> t.source), (App)SlotRanges.CODEC.fieldOf("slots").forGetter(t -> t.slotRange)).apply((Applicative)i, RangeSlotSource::new));
    private final LootContextArg<Object> source;
    private final SlotRange slotRange;

    private RangeSlotSource(LootContextArg<Object> source, SlotRange slotRange) {
        this.source = source;
        this.slotRange = slotRange;
    }

    public MapCodec<RangeSlotSource> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.contextParam());
    }

    @Override
    public final SlotCollection provide(LootContext context) {
        Object maybeProvider = this.source.get(context);
        if (maybeProvider instanceof SlotProvider) {
            SlotProvider slotProvider = (SlotProvider)maybeProvider;
            return slotProvider.getSlotsFromRange(this.slotRange.slots());
        }
        return SlotCollection.EMPTY;
    }
}

