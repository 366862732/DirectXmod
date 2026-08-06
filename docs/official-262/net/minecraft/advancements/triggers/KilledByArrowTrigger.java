/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.triggers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import org.jspecify.annotations.Nullable;

public class KilledByArrowTrigger
extends SimpleCriterionTrigger<TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Collection<Entity> victims, @Nullable ItemStack firedByWeapon) {
        ArrayList victimContexts = Lists.newArrayList();
        HashSet entityTypes = Sets.newHashSet();
        for (Entity victim : victims) {
            entityTypes.add(victim.getType());
            victimContexts.add(EntityPredicate.createContext(player, victim));
        }
        this.trigger(player, t -> t.matches(victimContexts, entityTypes.size(), firedByWeapon));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes, Optional<ItemPredicate> firedFromWeapon) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group((App)EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player), (App)EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(TriggerInstance::victims), (App)MinMaxBounds.Ints.CODEC.optionalFieldOf("unique_entity_types", (Object)MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::uniqueEntityTypes), (App)ItemPredicate.CODEC.optionalFieldOf("fired_from_weapon").forGetter(TriggerInstance::firedFromWeapon)).apply((Applicative)i, TriggerInstance::new));

        public static Criterion<TriggerInstance> crossbowKilled(HolderGetter<Item> items, EntityPredicate.Builder ... victims) {
            return CriteriaTriggers.KILLED_BY_ARROW.createCriterion(new TriggerInstance(Optional.empty(), EntityPredicate.wrap(victims), MinMaxBounds.Ints.ANY, Optional.of(ItemPredicate.Builder.item().of(items, Items.CROSSBOW).build())));
        }

        public static Criterion<TriggerInstance> crossbowKilled(HolderGetter<Item> items, MinMaxBounds.Ints uniqueEntityTypes) {
            return CriteriaTriggers.KILLED_BY_ARROW.createCriterion(new TriggerInstance(Optional.empty(), List.of(), uniqueEntityTypes, Optional.of(ItemPredicate.Builder.item().of(items, Items.CROSSBOW).build())));
        }

        public boolean matches(Collection<LootContext> victims, int uniqueEntityTypes, @Nullable ItemStack firedFromWeapon) {
            if (this.firedFromWeapon.isPresent() && (firedFromWeapon == null || !this.firedFromWeapon.get().test(firedFromWeapon))) {
                return false;
            }
            if (!this.victims.isEmpty()) {
                ArrayList victimsCopy = Lists.newArrayList(victims);
                for (ContextAwarePredicate predicate : this.victims) {
                    boolean found = false;
                    Iterator iterator = victimsCopy.iterator();
                    while (iterator.hasNext()) {
                        LootContext entity = (LootContext)iterator.next();
                        if (!predicate.matches(entity)) continue;
                        iterator.remove();
                        found = true;
                        break;
                    }
                    if (found) continue;
                    return false;
                }
            }
            return this.uniqueEntityTypes.matches(uniqueEntityTypes);
        }

        @Override
        public void validate(ValidationContextSource validator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
            Validatable.validate(validator.entityContext(), "victims", this.victims);
        }
    }
}

