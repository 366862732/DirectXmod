/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.advancements.triggers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger
extends SimpleCriterionTrigger<TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Inventory inventory, ItemStack changedItem) {
        int slotsFull = 0;
        int slotsEmpty = 0;
        int slotsOccupied = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            ItemStack itemStack = inventory.getItem(slot);
            if (itemStack.isEmpty()) {
                ++slotsEmpty;
                continue;
            }
            ++slotsOccupied;
            if (itemStack.getCount() < itemStack.getMaxStackSize()) continue;
            ++slotsFull;
        }
        this.trigger(player, inventory, changedItem, slotsFull, slotsEmpty, slotsOccupied);
    }

    private void trigger(ServerPlayer player, Inventory inventory, ItemStack changedItem, int slotsFull, int slotsEmpty, int slotsOccupied) {
        this.trigger(player, t -> t.matches(inventory, changedItem, slotsFull, slotsEmpty, slotsOccupied));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Slots slots, List<ItemPredicate> items) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group((App)EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player), (App)Slots.CODEC.optionalFieldOf("slots", (Object)Slots.ANY).forGetter(TriggerInstance::slots), (App)ItemPredicate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(TriggerInstance::items)).apply((Applicative)i, TriggerInstance::new));

        public static Criterion<TriggerInstance> hasItems(ItemPredicate.Builder ... items) {
            return TriggerInstance.hasItems((ItemPredicate[])Stream.of(items).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
        }

        public static Criterion<TriggerInstance> hasItems(ItemPredicate ... items) {
            return CriteriaTriggers.INVENTORY_CHANGED.createCriterion(new TriggerInstance(Optional.empty(), Slots.ANY, List.of(items)));
        }

        public static Criterion<TriggerInstance> hasItems(ItemLike ... items) {
            ItemPredicate[] predicates = new ItemPredicate[items.length];
            for (int i = 0; i < items.length; ++i) {
                predicates[i] = new ItemPredicate(Optional.of(HolderSet.direct(items[i].asItem().builtInRegistryHolder())), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY);
            }
            return TriggerInstance.hasItems(predicates);
        }

        public boolean matches(Inventory inventory, ItemStack changedItem, int slotsFull, int slotsEmpty, int slotsOccupied) {
            if (!this.slots.matches(slotsFull, slotsEmpty, slotsOccupied)) {
                return false;
            }
            if (this.items.isEmpty()) {
                return true;
            }
            if (this.items.size() == 1) {
                return !changedItem.isEmpty() && this.items.get(0).test(changedItem);
            }
            ObjectArrayList predicates = new ObjectArrayList(this.items);
            int count = inventory.getContainerSize();
            for (int slot = 0; slot < count; ++slot) {
                if (predicates.isEmpty()) {
                    return true;
                }
                ItemStack itemStack = inventory.getItem(slot);
                if (itemStack.isEmpty()) continue;
                predicates.removeIf(predicate -> predicate.test(itemStack));
            }
            return predicates.isEmpty();
        }

        public record Slots(MinMaxBounds.Ints occupied, MinMaxBounds.Ints full, MinMaxBounds.Ints empty) {
            public static final Codec<Slots> CODEC = RecordCodecBuilder.create(i -> i.group((App)MinMaxBounds.Ints.CODEC.optionalFieldOf("occupied", (Object)MinMaxBounds.Ints.ANY).forGetter(Slots::occupied), (App)MinMaxBounds.Ints.CODEC.optionalFieldOf("full", (Object)MinMaxBounds.Ints.ANY).forGetter(Slots::full), (App)MinMaxBounds.Ints.CODEC.optionalFieldOf("empty", (Object)MinMaxBounds.Ints.ANY).forGetter(Slots::empty)).apply((Applicative)i, Slots::new));
            public static final Slots ANY = new Slots(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY);

            public boolean matches(int slotsFull, int slotsEmpty, int slotsOccupied) {
                if (!this.full.matches(slotsFull)) {
                    return false;
                }
                if (!this.empty.matches(slotsEmpty)) {
                    return false;
                }
                return this.occupied.matches(slotsOccupied);
            }
        }
    }
}

