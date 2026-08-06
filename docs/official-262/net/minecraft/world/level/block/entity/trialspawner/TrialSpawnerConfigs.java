/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity.trialspawner;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerConfigs {
    private static final Keys TRIAL_CHAMBER_BREEZE = Keys.of("trial_chamber/breeze");
    private static final Keys TRIAL_CHAMBER_MELEE_HUSK = Keys.of("trial_chamber/melee/husk");
    private static final Keys TRIAL_CHAMBER_MELEE_SPIDER = Keys.of("trial_chamber/melee/spider");
    private static final Keys TRIAL_CHAMBER_MELEE_ZOMBIE = Keys.of("trial_chamber/melee/zombie");
    private static final Keys TRIAL_CHAMBER_RANGED_POISON_SKELETON = Keys.of("trial_chamber/ranged/poison_skeleton");
    private static final Keys TRIAL_CHAMBER_RANGED_SKELETON = Keys.of("trial_chamber/ranged/skeleton");
    private static final Keys TRIAL_CHAMBER_RANGED_STRAY = Keys.of("trial_chamber/ranged/stray");
    private static final Keys TRIAL_CHAMBER_SLOW_RANGED_POISON_SKELETON = Keys.of("trial_chamber/slow_ranged/poison_skeleton");
    private static final Keys TRIAL_CHAMBER_SLOW_RANGED_SKELETON = Keys.of("trial_chamber/slow_ranged/skeleton");
    private static final Keys TRIAL_CHAMBER_SLOW_RANGED_STRAY = Keys.of("trial_chamber/slow_ranged/stray");
    private static final Keys TRIAL_CHAMBER_SMALL_MELEE_BABY_ZOMBIE = Keys.of("trial_chamber/small_melee/baby_zombie");
    private static final Keys TRIAL_CHAMBER_SMALL_MELEE_CAVE_SPIDER = Keys.of("trial_chamber/small_melee/cave_spider");
    private static final Keys TRIAL_CHAMBER_SMALL_MELEE_SILVERFISH = Keys.of("trial_chamber/small_melee/silverfish");
    private static final Keys TRIAL_CHAMBER_SMALL_MELEE_SLIME = Keys.of("trial_chamber/small_melee/slime");

    public static void bootstrap(BootstrapContext<TrialSpawnerConfig> context) {
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_BREEZE, TrialSpawnerConfig.builder().simultaneousMobs(1.0f).simultaneousMobsAddedPerPlayer(0.5f).ticksBetweenSpawn(20).totalMobs(2.0f).totalMobsAddedPerPlayer(1.0f).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.BREEZE))).build(), TrialSpawnerConfig.builder().simultaneousMobsAddedPerPlayer(0.5f).ticksBetweenSpawn(20).totalMobs(4.0f).totalMobsAddedPerPlayer(1.0f).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.BREEZE))).lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_MELEE_HUSK, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.HUSK))).build(), TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.HUSK, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE))).lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_MELEE_SPIDER, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.SPIDER))).build(), TrialSpawnerConfigs.trialChamberMeleeOminous().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.SPIDER))).lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_MELEE_ZOMBIE, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.ZOMBIE))).build(), TrialSpawnerConfigs.trialChamberBase().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.ZOMBIE, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_RANGED_POISON_SKELETON, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.BOGGED))).build(), TrialSpawnerConfigs.trialChamberBase().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.BOGGED, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_RANGED_SKELETON, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.SKELETON))).build(), TrialSpawnerConfigs.trialChamberBase().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.SKELETON, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_RANGED_STRAY, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.STRAY))).build(), TrialSpawnerConfigs.trialChamberBase().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.STRAY, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SLOW_RANGED_POISON_SKELETON, TrialSpawnerConfigs.trialChamberSlowRanged().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.BOGGED))).build(), TrialSpawnerConfigs.trialChamberSlowRanged().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.BOGGED, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SLOW_RANGED_SKELETON, TrialSpawnerConfigs.trialChamberSlowRanged().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.SKELETON))).build(), TrialSpawnerConfigs.trialChamberSlowRanged().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.SKELETON, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SLOW_RANGED_STRAY, TrialSpawnerConfigs.trialChamberSlowRanged().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.STRAY))).build(), TrialSpawnerConfigs.trialChamberSlowRanged().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnDataWithEquipment(EntityTypes.STRAY, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SMALL_MELEE_BABY_ZOMBIE, TrialSpawnerConfig.builder().simultaneousMobsAddedPerPlayer(0.5f).ticksBetweenSpawn(20).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.customSpawnDataWithEquipment(EntityTypes.ZOMBIE, tag -> tag.putBoolean("IsBaby", true), null))).build(), TrialSpawnerConfig.builder().simultaneousMobsAddedPerPlayer(0.5f).ticksBetweenSpawn(20).lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.customSpawnDataWithEquipment(EntityTypes.ZOMBIE, tag -> tag.putBoolean("IsBaby", true), BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SMALL_MELEE_CAVE_SPIDER, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.CAVE_SPIDER))).build(), TrialSpawnerConfigs.trialChamberMeleeOminous().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.CAVE_SPIDER))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SMALL_MELEE_SILVERFISH, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.SILVERFISH))).build(), TrialSpawnerConfigs.trialChamberMeleeOminous().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.of(TrialSpawnerConfigs.spawnData(EntityTypes.SILVERFISH))).build());
        TrialSpawnerConfigs.register(context, TRIAL_CHAMBER_SMALL_MELEE_SLIME, TrialSpawnerConfigs.trialChamberBase().spawnPotentialsDefinition(WeightedList.builder().add(TrialSpawnerConfigs.customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)1)), 3).add(TrialSpawnerConfigs.customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)2)), 1).build()).build(), TrialSpawnerConfigs.trialChamberMeleeOminous().lootTablesToEject(WeightedList.builder().add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3).add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7).build()).spawnPotentialsDefinition(WeightedList.builder().add(TrialSpawnerConfigs.customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)1)), 3).add(TrialSpawnerConfigs.customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)2)), 1).build()).build());
    }

    private static <T extends Entity> SpawnData spawnData(EntityType<T> type) {
        return TrialSpawnerConfigs.customSpawnDataWithEquipment(type, tag -> {}, null);
    }

    private static <T extends Entity> SpawnData customSpawnData(EntityType<T> type, Consumer<CompoundTag> tagModifier) {
        return TrialSpawnerConfigs.customSpawnDataWithEquipment(type, tagModifier, null);
    }

    private static <T extends Entity> SpawnData spawnDataWithEquipment(EntityType<T> type, ResourceKey<LootTable> equipmentLootTable) {
        return TrialSpawnerConfigs.customSpawnDataWithEquipment(type, tag -> {}, equipmentLootTable);
    }

    private static <T extends Entity> SpawnData customSpawnDataWithEquipment(EntityType<T> type, Consumer<CompoundTag> tagModifier, @Nullable ResourceKey<LootTable> equipmentLootTable) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        tagModifier.accept(tag);
        Optional<EquipmentTable> table = Optional.ofNullable(equipmentLootTable).map(lootTable -> new EquipmentTable((ResourceKey<LootTable>)lootTable, 0.0f));
        return new SpawnData(tag, Optional.empty(), table);
    }

    private static void register(BootstrapContext<TrialSpawnerConfig> context, Keys keys, TrialSpawnerConfig normalConfig, TrialSpawnerConfig ominousConfig) {
        context.register(keys.normal, normalConfig);
        context.register(keys.ominous, ominousConfig);
    }

    private static ResourceKey<TrialSpawnerConfig> registryKey(String id) {
        return ResourceKey.create(Registries.TRIAL_SPAWNER_CONFIG, Identifier.withDefaultNamespace(id));
    }

    private static TrialSpawnerConfig.Builder trialChamberMeleeOminous() {
        return TrialSpawnerConfig.builder().simultaneousMobs(4.0f).simultaneousMobsAddedPerPlayer(0.5f).ticksBetweenSpawn(20).totalMobs(12.0f);
    }

    private static TrialSpawnerConfig.Builder trialChamberSlowRanged() {
        return TrialSpawnerConfig.builder().simultaneousMobs(4.0f).simultaneousMobsAddedPerPlayer(2.0f).ticksBetweenSpawn(160);
    }

    private static TrialSpawnerConfig.Builder trialChamberBase() {
        return TrialSpawnerConfig.builder().simultaneousMobs(3.0f).simultaneousMobsAddedPerPlayer(0.5f).ticksBetweenSpawn(20);
    }

    private record Keys(ResourceKey<TrialSpawnerConfig> normal, ResourceKey<TrialSpawnerConfig> ominous) {
        public static Keys of(String id) {
            return new Keys(TrialSpawnerConfigs.registryKey(id + "/normal"), TrialSpawnerConfigs.registryKey(id + "/ominous"));
        }
    }
}

