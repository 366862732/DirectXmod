/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.slot.ContentsSlotSource;
import net.minecraft.world.item.slot.EmptySlotSource;
import net.minecraft.world.item.slot.FilteredSlotSource;
import net.minecraft.world.item.slot.GroupSlotSource;
import net.minecraft.world.item.slot.LimitSlotSource;
import net.minecraft.world.item.slot.RangeSlotSource;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.level.storage.loot.LootContext;

public interface SlotSources {
    public static final Codec<SlotSource> TYPED_CODEC = BuiltInRegistries.SLOT_SOURCE_TYPE.byNameCodec().dispatch(SlotSource::codec, c -> c);
    public static final Codec<SlotSource> CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(TYPED_CODEC, GroupSlotSource.INLINE_CODEC));

    public static MapCodec<? extends SlotSource> bootstrap(Registry<MapCodec<? extends SlotSource>> registry) {
        Registry.register(registry, "group", GroupSlotSource.MAP_CODEC);
        Registry.register(registry, "filtered", FilteredSlotSource.MAP_CODEC);
        Registry.register(registry, "limit_slots", LimitSlotSource.MAP_CODEC);
        Registry.register(registry, "slot_range", RangeSlotSource.MAP_CODEC);
        Registry.register(registry, "contents", ContentsSlotSource.MAP_CODEC);
        return Registry.register(registry, "empty", EmptySlotSource.MAP_CODEC);
    }

    public static Function<LootContext, SlotCollection> group(Collection<? extends SlotSource> list) {
        List<? extends SlotSource> terms = List.copyOf(list);
        return switch (terms.size()) {
            case 0 -> context -> SlotCollection.EMPTY;
            case 1 -> terms.getFirst()::provide;
            case 2 -> {
                SlotSource first = terms.get(0);
                SlotSource second = terms.get(1);
                yield context -> SlotCollection.concat(first.provide((LootContext)context), second.provide((LootContext)context));
            }
            default -> context -> {
                ArrayList<SlotCollection> collections = new ArrayList<SlotCollection>();
                for (SlotSource term : terms) {
                    collections.add(term.provide((LootContext)context));
                }
                return SlotCollection.concat(collections);
            };
        };
    }
}

