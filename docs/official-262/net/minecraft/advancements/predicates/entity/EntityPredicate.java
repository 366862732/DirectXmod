/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Iterables
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.MobEffectsPredicate;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.advancements.predicates.SlotsPredicate;
import net.minecraft.advancements.predicates.entity.CubeMobPredicate;
import net.minecraft.advancements.predicates.entity.DistanceToPlayerPredicate;
import net.minecraft.advancements.predicates.entity.EntityEffectsPredicate;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityExactDataComponentsPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityLocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityNbtPredicate;
import net.minecraft.advancements.predicates.entity.EntityPartialComponentsPredicate;
import net.minecraft.advancements.predicates.entity.EntitySlotsPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.advancements.predicates.entity.FishingHookPredicate;
import net.minecraft.advancements.predicates.entity.LightningBoltPredicate;
import net.minecraft.advancements.predicates.entity.MovementAffectedByPredicate;
import net.minecraft.advancements.predicates.entity.MovementPredicate;
import net.minecraft.advancements.predicates.entity.PassengerPredicate;
import net.minecraft.advancements.predicates.entity.PeriodicEntityTickPredicate;
import net.minecraft.advancements.predicates.entity.PlayerPredicate;
import net.minecraft.advancements.predicates.entity.RaiderPredicate;
import net.minecraft.advancements.predicates.entity.SheepPredicate;
import net.minecraft.advancements.predicates.entity.SteppingOnPredicate;
import net.minecraft.advancements.predicates.entity.TargetedEntityPredicate;
import net.minecraft.advancements.predicates.entity.TeamPredicate;
import net.minecraft.advancements.predicates.entity.VehiclePredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class EntityPredicate {
    private static final Codec<Map<Codec<? extends EntitySubPredicate>, EntitySubPredicate>> MAP_CODEC = Codec.dispatchedMap(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE.byNameCodec(), c -> c);
    public static final Codec<EntityPredicate> CODEC = MAP_CODEC.xmap(EntityPredicate::new, p -> p.parts);
    public static final Codec<ContextAwarePredicate> ADVANCEMENT_CODEC = Codec.withAlternative(ContextAwarePredicate.CODEC, CODEC, EntityPredicate::wrap);
    private static final Comparator<Map.Entry<Codec<? extends EntitySubPredicate>, EntitySubPredicate>> PREDICATE_TYPE_ORDER = Map.Entry.comparingByKey(Comparator.comparing(codec -> {
        if (codec == EntityTypePredicate.CODEC) {
            return -1;
        }
        if (codec == VehiclePredicate.CODEC || codec == PassengerPredicate.CODEC || codec == TargetedEntityPredicate.CODEC || codec == LightningBoltPredicate.CODEC || codec == PlayerPredicate.CODEC) {
            return 1;
        }
        if (codec == EntityNbtPredicate.CODEC) {
            return 2;
        }
        return 0;
    }));
    private final Map<Codec<? extends EntitySubPredicate>, EntitySubPredicate> parts;
    private final EntitySubPredicate combinedPart;

    public EntityPredicate(Map<Codec<? extends EntitySubPredicate>, EntitySubPredicate> parts) {
        this.parts = parts;
        this.combinedPart = EntityPredicate.combine(parts);
    }

    public static ContextAwarePredicate wrap(Builder singlePredicate) {
        return EntityPredicate.wrap(singlePredicate.build());
    }

    public static Optional<ContextAwarePredicate> wrap(Optional<EntityPredicate> singlePredicate) {
        return singlePredicate.map(EntityPredicate::wrap);
    }

    public static List<ContextAwarePredicate> wrap(Builder ... predicates) {
        return Stream.of(predicates).map(EntityPredicate::wrap).toList();
    }

    public static ContextAwarePredicate wrap(EntityPredicate singlePredicate) {
        LootItemCondition asCondition = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, singlePredicate).build();
        return new ContextAwarePredicate(List.of(asCondition));
    }

    public boolean matches(ServerPlayer player, @Nullable Entity entity) {
        return this.matches(player.level(), player.position(), entity);
    }

    public boolean matches(ServerLevel level, @Nullable Vec3 position, @Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return this.combinedPart.matches(entity, level, position);
    }

    public static LootContext createContext(ServerPlayer player, Entity entity) {
        LootParams lootParams = new LootParams.Builder(player.level()).withParameter(LootContextParams.THIS_ENTITY, entity).withParameter(LootContextParams.ORIGIN, player.position()).create(LootContextParamSets.ADVANCEMENT_ENTITY);
        return new LootContext.Builder(lootParams).create(Optional.empty());
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EntityPredicate)) return false;
        EntityPredicate that = (EntityPredicate)o;
        if (!Objects.equals(this.parts, that.parts)) return false;
        return true;
    }

    public int hashCode() {
        return this.parts.hashCode();
    }

    public String toString() {
        return "EntityPredicate[parts=" + String.valueOf(this.parts) + "]";
    }

    private static EntitySubPredicate combine(Map<Codec<? extends EntitySubPredicate>, EntitySubPredicate> predicateMap) {
        if (predicateMap.isEmpty()) {
            return EntitySubPredicate.ALWAYS_TRUE;
        }
        if (predicateMap.size() == 1) {
            return (EntitySubPredicate)Iterables.getOnlyElement(predicateMap.values());
        }
        EntitySubPredicate[] predicates = (EntitySubPredicate[])predicateMap.entrySet().stream().sorted(PREDICATE_TYPE_ORDER).map(Map.Entry::getValue).toArray(EntitySubPredicate[]::new);
        if (predicates.length == 2) {
            return predicates[0].and(predicates[1]);
        }
        return (entity, level, position) -> {
            for (EntitySubPredicate part : predicates) {
                if (part.matches(entity, level, position)) continue;
                return false;
            }
            return true;
        };
    }

    public static class Builder {
        private final ImmutableMap.Builder<Codec<? extends EntitySubPredicate>, EntitySubPredicate> parts = ImmutableMap.builder();

        public static Builder entity() {
            return new Builder();
        }

        public <T extends EntitySubPredicate> Builder put(Codec<T> key, T predicate) {
            this.parts.put(key, predicate);
            return this;
        }

        public Builder of(HolderGetter<EntityType<?>> lookup, EntityType<?> entityType) {
            return this.entityType(EntityTypePredicate.of(lookup, entityType));
        }

        public Builder of(HolderGetter<EntityType<?>> lookup, TagKey<EntityType<?>> entityTypeTag) {
            return this.entityType(EntityTypePredicate.of(lookup, entityTypeTag));
        }

        public Builder entityType(EntityTypePredicate entityType) {
            return this.put(EntityTypePredicate.CODEC, entityType);
        }

        public Builder distance(DistancePredicate distanceToPlayer) {
            return this.put(DistanceToPlayerPredicate.CODEC, new DistanceToPlayerPredicate(distanceToPlayer));
        }

        public Builder moving(MovementPredicate movement) {
            return this.put(MovementPredicate.CODEC, movement);
        }

        public Builder located(LocationPredicate.Builder location) {
            return this.put(EntityLocationPredicate.CODEC, new EntityLocationPredicate(location.build()));
        }

        public Builder steppingOn(LocationPredicate.Builder location) {
            return this.put(SteppingOnPredicate.CODEC, new SteppingOnPredicate(location.build()));
        }

        public Builder movementAffectedBy(LocationPredicate.Builder location) {
            return this.put(MovementAffectedByPredicate.CODEC, new MovementAffectedByPredicate(location.build()));
        }

        public Builder effects(MobEffectsPredicate.Builder effects) {
            return this.put(EntityEffectsPredicate.CODEC, new EntityEffectsPredicate(effects.build()));
        }

        public Builder nbt(NbtPredicate nbt) {
            return this.put(EntityNbtPredicate.CODEC, new EntityNbtPredicate(nbt));
        }

        public Builder flags(EntityFlagsPredicate.Builder flags) {
            return this.put(EntityFlagsPredicate.CODEC, flags.build());
        }

        public Builder equipment(EntityEquipmentPredicate.Builder equipment) {
            return this.put(EntityEquipmentPredicate.CODEC, equipment.build());
        }

        public Builder equipment(EntityEquipmentPredicate equipment) {
            return this.put(EntityEquipmentPredicate.CODEC, equipment);
        }

        public Builder periodicTick(int period) {
            return this.put(PeriodicEntityTickPredicate.CODEC, new PeriodicEntityTickPredicate(period));
        }

        public Builder vehicle(Builder vehicle) {
            return this.put(VehiclePredicate.CODEC, new VehiclePredicate(vehicle.build()));
        }

        public Builder passenger(Builder passenger) {
            return this.put(PassengerPredicate.CODEC, new PassengerPredicate(passenger.build()));
        }

        public Builder targetedEntity(Builder targetedEntity) {
            return this.put(TargetedEntityPredicate.CODEC, new TargetedEntityPredicate(targetedEntity.build()));
        }

        public Builder team(String team) {
            return this.put(TeamPredicate.CODEC, new TeamPredicate(team));
        }

        public Builder slots(SlotsPredicate slots) {
            return this.put(EntitySlotsPredicate.CODEC, new EntitySlotsPredicate(slots));
        }

        public Builder components(DataComponentMatchers components) {
            if (!components.exact().isEmpty()) {
                this.components(components.exact());
            }
            if (!components.partial().isEmpty()) {
                this.components(components.partial());
            }
            return this;
        }

        public Builder components(DataComponentExactPredicate components) {
            return this.put(EntityExactDataComponentsPredicate.CODEC, new EntityExactDataComponentsPredicate(components));
        }

        public Builder components(Map<DataComponentPredicate.Type<?>, DataComponentPredicate> components) {
            return this.put(EntityPartialComponentsPredicate.CODEC, new EntityPartialComponentsPredicate(components));
        }

        public Builder lightingBolt(LightningBoltPredicate lightningBolt) {
            return this.put(LightningBoltPredicate.CODEC, lightningBolt);
        }

        public Builder player(PlayerPredicate player) {
            return this.put(PlayerPredicate.CODEC, player);
        }

        public Builder sheep(SheepPredicate sheep) {
            return this.put(SheepPredicate.CODEC, sheep);
        }

        public Builder cubeMob(CubeMobPredicate cubeMob) {
            return this.put(CubeMobPredicate.CODEC, cubeMob);
        }

        public Builder raider(RaiderPredicate raider) {
            return this.put(RaiderPredicate.CODEC, raider);
        }

        public Builder fishingHook(FishingHookPredicate fishingHook) {
            return this.put(FishingHookPredicate.CODEC, fishingHook);
        }

        public EntityPredicate build() {
            return new EntityPredicate((Map<Codec<? extends EntitySubPredicate>, EntitySubPredicate>)this.parts.buildOrThrow());
        }
    }
}

