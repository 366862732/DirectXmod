/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.triggers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PlayerTrigger
extends SimpleCriterionTrigger<TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, t -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group((App)EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)).apply((Applicative)i, TriggerInstance::new));

        public static Criterion<TriggerInstance> located(LocationPredicate.Builder location) {
            return CriteriaTriggers.LOCATION.createCriterion(new TriggerInstance(Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().located(location)))));
        }

        public static Criterion<TriggerInstance> located(EntityPredicate.Builder player) {
            return CriteriaTriggers.LOCATION.createCriterion(new TriggerInstance(Optional.of(EntityPredicate.wrap(player.build()))));
        }

        public static Criterion<TriggerInstance> located(Optional<EntityPredicate> player) {
            return CriteriaTriggers.LOCATION.createCriterion(new TriggerInstance(EntityPredicate.wrap(player)));
        }

        public static Criterion<TriggerInstance> sleptInBed() {
            return CriteriaTriggers.SLEPT_IN_BED.createCriterion(new TriggerInstance(Optional.empty()));
        }

        public static Criterion<TriggerInstance> raidWon() {
            return CriteriaTriggers.RAID_WIN.createCriterion(new TriggerInstance(Optional.empty()));
        }

        public static Criterion<TriggerInstance> avoidVibration() {
            return CriteriaTriggers.AVOID_VIBRATION.createCriterion(new TriggerInstance(Optional.empty()));
        }

        public static Criterion<TriggerInstance> tick() {
            return CriteriaTriggers.TICK.createCriterion(new TriggerInstance(Optional.empty()));
        }

        public static Criterion<TriggerInstance> walkOnBlockWithEquipment(HolderGetter<Block> blocks, HolderGetter<Item> items, Block stepOnBlock, Item requiredEquipment) {
            return TriggerInstance.located(EntityPredicate.Builder.entity().equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(items, requiredEquipment))).steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, stepOnBlock))));
        }
    }
}

