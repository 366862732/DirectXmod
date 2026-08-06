/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Keyable
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MobSpawnSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1f;
    public static final WeightedList<SpawnerData> EMPTY_MOB_LIST = WeightedList.of();
    public static final MobSpawnSettings EMPTY = new Builder().build();
    public static final MapCodec<MobSpawnSettings> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.floatRange((float)0.0f, (float)0.9999999f).optionalFieldOf("creature_spawn_probability", (Object)Float.valueOf(0.1f)).forGetter(b -> Float.valueOf(b.creatureGenerationProbability)), (App)Codec.simpleMap(MobCategory.CODEC, (Codec)WeightedList.codec(SpawnerData.CODEC).promotePartial(Util.prefix("Spawn data: ", arg_0 -> ((Logger)LOGGER).error(arg_0))), (Keyable)StringRepresentable.keys(MobCategory.values())).fieldOf("spawners").forGetter(b -> b.spawners), (App)Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE).fieldOf("spawn_costs").forGetter(b -> b.mobSpawnCosts)).apply((Applicative)i, MobSpawnSettings::new));
    private final float creatureGenerationProbability;
    private final Map<MobCategory, WeightedList<SpawnerData>> spawners;
    private final Map<EntityType<?>, MobSpawnCost> mobSpawnCosts;

    private MobSpawnSettings(float creatureGenerationProbability, Map<MobCategory, WeightedList<SpawnerData>> spawners, Map<EntityType<?>, MobSpawnCost> mobSpawnCosts) {
        this.creatureGenerationProbability = creatureGenerationProbability;
        this.spawners = ImmutableMap.copyOf(spawners);
        this.mobSpawnCosts = ImmutableMap.copyOf(mobSpawnCosts);
    }

    public WeightedList<SpawnerData> getMobs(MobCategory category) {
        return this.spawners.getOrDefault(category, EMPTY_MOB_LIST);
    }

    public @Nullable MobSpawnCost getMobSpawnCost(EntityType<?> type) {
        return this.mobSpawnCosts.get(type);
    }

    public float getCreatureProbability() {
        return this.creatureGenerationProbability;
    }

    public record MobSpawnCost(double energyBudget, double charge) {
        public static final Codec<MobSpawnCost> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.DOUBLE.fieldOf("energy_budget").forGetter(e -> e.energyBudget), (App)Codec.DOUBLE.fieldOf("charge").forGetter(e -> e.charge)).apply((Applicative)i, MobSpawnCost::new));
    }

    public record SpawnerData(EntityType<?> type, int minCount, int maxCount) {
        public static final MapCodec<SpawnerData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(d -> d.type), (App)ExtraCodecs.POSITIVE_INT.fieldOf("minCount").forGetter(e -> e.minCount), (App)ExtraCodecs.POSITIVE_INT.fieldOf("maxCount").forGetter(e -> e.maxCount)).apply((Applicative)i, SpawnerData::new)).validate(spawnerData -> {
            if (spawnerData.minCount > spawnerData.maxCount) {
                return DataResult.error(() -> "minCount needs to be smaller or equal to maxCount");
            }
            return DataResult.success((Object)spawnerData);
        });

        public SpawnerData {
            type = type.getCategory() == MobCategory.MISC ? EntityTypes.PIG : type;
        }

        @Override
        public String toString() {
            return String.valueOf(EntityType.getKey(this.type)) + "*(" + this.minCount + "-" + this.maxCount + ")";
        }
    }

    public static class Builder {
        private final Map<MobCategory, WeightedList.Builder<SpawnerData>> spawners = Util.makeEnumMap(MobCategory.class, c -> WeightedList.builder());
        private final Map<EntityType<?>, MobSpawnCost> mobSpawnCosts = Maps.newLinkedHashMap();
        private float creatureGenerationProbability = 0.1f;

        public Builder addSpawn(MobCategory category, int weight, SpawnerData spawnerData) {
            this.spawners.get(category).add(spawnerData, weight);
            return this;
        }

        public Builder addMobCharge(EntityType<?> type, double charge, double energyBudget) {
            this.mobSpawnCosts.put(type, new MobSpawnCost(energyBudget, charge));
            return this;
        }

        public Builder creatureGenerationProbability(float creatureGenerationProbability) {
            this.creatureGenerationProbability = creatureGenerationProbability;
            return this;
        }

        public MobSpawnSettings build() {
            return new MobSpawnSettings(this.creatureGenerationProbability, (Map)this.spawners.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> ((WeightedList.Builder)e.getValue()).build())), (Map<EntityType<?>, MobSpawnCost>)ImmutableMap.copyOf(this.mobSpawnCosts));
        }
    }
}

