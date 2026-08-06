/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.block.entity.trialspawner;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public record TrialSpawnerConfig(int spawnRange, float totalMobs, float simultaneousMobs, float totalMobsAddedPerPlayer, float simultaneousMobsAddedPerPlayer, int ticksBetweenSpawn, WeightedList<SpawnData> spawnPotentialsDefinition, WeightedList<ResourceKey<LootTable>> lootTablesToEject, ResourceKey<LootTable> itemsToDropWhenOminous) {
    public static final TrialSpawnerConfig DEFAULT = TrialSpawnerConfig.builder().build();
    public static final Codec<TrialSpawnerConfig> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.intRange((int)1, (int)128).optionalFieldOf("spawn_range", (Object)TrialSpawnerConfig.DEFAULT.spawnRange).forGetter(TrialSpawnerConfig::spawnRange), (App)Codec.floatRange((float)0.0f, (float)Float.MAX_VALUE).optionalFieldOf("total_mobs", (Object)Float.valueOf(TrialSpawnerConfig.DEFAULT.totalMobs)).forGetter(TrialSpawnerConfig::totalMobs), (App)Codec.floatRange((float)0.0f, (float)Float.MAX_VALUE).optionalFieldOf("simultaneous_mobs", (Object)Float.valueOf(TrialSpawnerConfig.DEFAULT.simultaneousMobs)).forGetter(TrialSpawnerConfig::simultaneousMobs), (App)Codec.floatRange((float)0.0f, (float)Float.MAX_VALUE).optionalFieldOf("total_mobs_added_per_player", (Object)Float.valueOf(TrialSpawnerConfig.DEFAULT.totalMobsAddedPerPlayer)).forGetter(TrialSpawnerConfig::totalMobsAddedPerPlayer), (App)Codec.floatRange((float)0.0f, (float)Float.MAX_VALUE).optionalFieldOf("simultaneous_mobs_added_per_player", (Object)Float.valueOf(TrialSpawnerConfig.DEFAULT.simultaneousMobsAddedPerPlayer)).forGetter(TrialSpawnerConfig::simultaneousMobsAddedPerPlayer), (App)Codec.intRange((int)0, (int)Integer.MAX_VALUE).optionalFieldOf("ticks_between_spawn", (Object)TrialSpawnerConfig.DEFAULT.ticksBetweenSpawn).forGetter(TrialSpawnerConfig::ticksBetweenSpawn), (App)SpawnData.LIST_CODEC.optionalFieldOf("spawn_potentials", WeightedList.of()).forGetter(TrialSpawnerConfig::spawnPotentialsDefinition), (App)WeightedList.codec(LootTable.KEY_CODEC).optionalFieldOf("loot_tables_to_eject", TrialSpawnerConfig.DEFAULT.lootTablesToEject).forGetter(TrialSpawnerConfig::lootTablesToEject), (App)LootTable.KEY_CODEC.optionalFieldOf("items_to_drop_when_ominous", TrialSpawnerConfig.DEFAULT.itemsToDropWhenOminous).forGetter(TrialSpawnerConfig::itemsToDropWhenOminous)).apply((Applicative)i, TrialSpawnerConfig::new));
    public static final Codec<Holder<TrialSpawnerConfig>> CODEC = RegistryFileCodec.create(Registries.TRIAL_SPAWNER_CONFIG, DIRECT_CODEC);

    public int calculateTargetTotalMobs(int additionalPlayers) {
        return (int)Math.floor(this.totalMobs + this.totalMobsAddedPerPlayer * (float)additionalPlayers);
    }

    public int calculateTargetSimultaneousMobs(int additionalPlayers) {
        return (int)Math.floor(this.simultaneousMobs + this.simultaneousMobsAddedPerPlayer * (float)additionalPlayers);
    }

    public long ticksBetweenItemSpawners() {
        return 160L;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TrialSpawnerConfig withSpawning(EntityType<?> type) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        SpawnData spawnData = new SpawnData(tag, Optional.empty(), Optional.empty());
        return new TrialSpawnerConfig(this.spawnRange, this.totalMobs, this.simultaneousMobs, this.totalMobsAddedPerPlayer, this.simultaneousMobsAddedPerPlayer, this.ticksBetweenSpawn, WeightedList.of(spawnData), this.lootTablesToEject, this.itemsToDropWhenOminous);
    }

    public static class Builder {
        private int spawnRange = 4;
        private float totalMobs = 6.0f;
        private float simultaneousMobs = 2.0f;
        private float totalMobsAddedPerPlayer = 2.0f;
        private float simultaneousMobsAddedPerPlayer = 1.0f;
        private int ticksBetweenSpawn = 40;
        private WeightedList<SpawnData> spawnPotentialsDefinition = WeightedList.of();
        private WeightedList<ResourceKey<LootTable>> lootTablesToEject = WeightedList.builder().add(BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_CONSUMABLES).add(BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_KEY).build();
        private ResourceKey<LootTable> itemsToDropWhenOminous = BuiltInLootTables.SPAWNER_TRIAL_ITEMS_TO_DROP_WHEN_OMINOUS;

        public Builder spawnRange(int spawnRange) {
            this.spawnRange = spawnRange;
            return this;
        }

        public Builder totalMobs(float totalMobs) {
            this.totalMobs = totalMobs;
            return this;
        }

        public Builder simultaneousMobs(float simultaneousMobs) {
            this.simultaneousMobs = simultaneousMobs;
            return this;
        }

        public Builder totalMobsAddedPerPlayer(float totalMobsAddedPerPlayer) {
            this.totalMobsAddedPerPlayer = totalMobsAddedPerPlayer;
            return this;
        }

        public Builder simultaneousMobsAddedPerPlayer(float simultaneousMobsAddedPerPlayer) {
            this.simultaneousMobsAddedPerPlayer = simultaneousMobsAddedPerPlayer;
            return this;
        }

        public Builder ticksBetweenSpawn(int ticksBetweenSpawn) {
            this.ticksBetweenSpawn = ticksBetweenSpawn;
            return this;
        }

        public Builder spawnPotentialsDefinition(WeightedList<SpawnData> spawnPotentialsDefinition) {
            this.spawnPotentialsDefinition = spawnPotentialsDefinition;
            return this;
        }

        public Builder lootTablesToEject(WeightedList<ResourceKey<LootTable>> lootTablesToEject) {
            this.lootTablesToEject = lootTablesToEject;
            return this;
        }

        public Builder itemsToDropWhenOminous(ResourceKey<LootTable> itemsToDropWhenOminous) {
            this.itemsToDropWhenOminous = itemsToDropWhenOminous;
            return this;
        }

        public TrialSpawnerConfig build() {
            return new TrialSpawnerConfig(this.spawnRange, this.totalMobs, this.simultaneousMobs, this.totalMobsAddedPerPlayer, this.simultaneousMobsAddedPerPlayer, this.ticksBetweenSpawn, this.spawnPotentialsDefinition, this.lootTablesToEject, this.itemsToDropWhenOminous);
        }
    }
}

