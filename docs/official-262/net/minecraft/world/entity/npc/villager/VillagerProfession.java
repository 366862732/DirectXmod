/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public record VillagerProfession(Component name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite, ImmutableSet<Item> requestedItems, ImmutableSet<Block> secondaryPoi, @Nullable SoundEvent workSound, Int2ObjectMap<ResourceKey<TradeSet>> tradeSetsByLevel) {
    public static final Predicate<Holder<PoiType>> ALL_ACQUIRABLE_JOBS = holder -> holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
    public static final ResourceKey<VillagerProfession> NONE = VillagerProfession.createKey("none");
    public static final ResourceKey<VillagerProfession> ARMORER = VillagerProfession.createKey("armorer");
    public static final ResourceKey<VillagerProfession> BUTCHER = VillagerProfession.createKey("butcher");
    public static final ResourceKey<VillagerProfession> CARTOGRAPHER = VillagerProfession.createKey("cartographer");
    public static final ResourceKey<VillagerProfession> CLERIC = VillagerProfession.createKey("cleric");
    public static final ResourceKey<VillagerProfession> FARMER = VillagerProfession.createKey("farmer");
    public static final ResourceKey<VillagerProfession> FISHERMAN = VillagerProfession.createKey("fisherman");
    public static final ResourceKey<VillagerProfession> FLETCHER = VillagerProfession.createKey("fletcher");
    public static final ResourceKey<VillagerProfession> LEATHERWORKER = VillagerProfession.createKey("leatherworker");
    public static final ResourceKey<VillagerProfession> LIBRARIAN = VillagerProfession.createKey("librarian");
    public static final ResourceKey<VillagerProfession> MASON = VillagerProfession.createKey("mason");
    public static final ResourceKey<VillagerProfession> NITWIT = VillagerProfession.createKey("nitwit");
    public static final ResourceKey<VillagerProfession> SHEPHERD = VillagerProfession.createKey("shepherd");
    public static final ResourceKey<VillagerProfession> TOOLSMITH = VillagerProfession.createKey("toolsmith");
    public static final ResourceKey<VillagerProfession> WEAPONSMITH = VillagerProfession.createKey("weaponsmith");

    private static ResourceKey<VillagerProfession> createKey(String name) {
        return ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.withDefaultNamespace(name));
    }

    private static VillagerProfession register(Registry<VillagerProfession> registry, ResourceKey<VillagerProfession> name, ResourceKey<PoiType> jobSite, @Nullable SoundEvent workSound, Int2ObjectMap<ResourceKey<TradeSet>> trades) {
        return VillagerProfession.register(registry, name, poiType -> poiType.is(jobSite), poiType -> poiType.is(jobSite), workSound, trades);
    }

    private static VillagerProfession register(Registry<VillagerProfession> registry, ResourceKey<VillagerProfession> name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite, @Nullable SoundEvent workSound) {
        return VillagerProfession.register(registry, name, heldJobSite, acquirableJobSite, (ImmutableSet<Item>)ImmutableSet.of(), (ImmutableSet<Block>)ImmutableSet.of(), workSound, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[0]));
    }

    private static VillagerProfession register(Registry<VillagerProfession> registry, ResourceKey<VillagerProfession> name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite, @Nullable SoundEvent workSound, Int2ObjectMap<ResourceKey<TradeSet>> trades) {
        return VillagerProfession.register(registry, name, heldJobSite, acquirableJobSite, (ImmutableSet<Item>)ImmutableSet.of(), (ImmutableSet<Block>)ImmutableSet.of(), workSound, trades);
    }

    private static VillagerProfession register(Registry<VillagerProfession> registry, ResourceKey<VillagerProfession> name, ResourceKey<PoiType> jobSite, ImmutableSet<Item> requestedItems, ImmutableSet<Block> secondaryPoi, @Nullable SoundEvent workSound, Int2ObjectMap<ResourceKey<TradeSet>> trades) {
        return VillagerProfession.register(registry, name, poiType -> poiType.is(jobSite), poiType -> poiType.is(jobSite), requestedItems, secondaryPoi, workSound, trades);
    }

    private static VillagerProfession register(Registry<VillagerProfession> registry, ResourceKey<VillagerProfession> name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite, ImmutableSet<Item> requestedItems, ImmutableSet<Block> secondaryPoi, @Nullable SoundEvent workSound, Int2ObjectMap<ResourceKey<TradeSet>> trades) {
        return Registry.register(registry, name, new VillagerProfession(Component.translatable("entity." + name.identifier().getNamespace() + ".villager." + name.identifier().getPath()), heldJobSite, acquirableJobSite, requestedItems, secondaryPoi, workSound, trades));
    }

    public static VillagerProfession bootstrap(Registry<VillagerProfession> registry) {
        VillagerProfession.register(registry, NONE, PoiType.NONE, ALL_ACQUIRABLE_JOBS, null);
        VillagerProfession.register(registry, ARMORER, PoiTypes.ARMORER, SoundEvents.VILLAGER_WORK_ARMORER, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.ARMORER_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.ARMORER_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.ARMORER_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.ARMORER_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.ARMORER_LEVEL_5)}));
        VillagerProfession.register(registry, BUTCHER, PoiTypes.BUTCHER, SoundEvents.VILLAGER_WORK_BUTCHER, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.BUTCHER_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.BUTCHER_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.BUTCHER_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.BUTCHER_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.BUTCHER_LEVEL_5)}));
        VillagerProfession.register(registry, CARTOGRAPHER, PoiTypes.CARTOGRAPHER, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.CARTOGRAPHER_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.CARTOGRAPHER_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.CARTOGRAPHER_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.CARTOGRAPHER_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.CARTOGRAPHER_LEVEL_5)}));
        VillagerProfession.register(registry, CLERIC, PoiTypes.CLERIC, SoundEvents.VILLAGER_WORK_CLERIC, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.CLERIC_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.CLERIC_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.CLERIC_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.CLERIC_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.CLERIC_LEVEL_5)}));
        VillagerProfession.register(registry, FARMER, PoiTypes.FARMER, (ImmutableSet<Item>)ImmutableSet.of((Object)Items.WHEAT, (Object)Items.WHEAT_SEEDS, (Object)Items.BEETROOT_SEEDS, (Object)Items.BONE_MEAL), (ImmutableSet<Block>)ImmutableSet.of((Object)Blocks.FARMLAND), SoundEvents.VILLAGER_WORK_FARMER, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.FARMER_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.FARMER_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.FARMER_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.FARMER_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.FARMER_LEVEL_5)}));
        VillagerProfession.register(registry, FISHERMAN, PoiTypes.FISHERMAN, SoundEvents.VILLAGER_WORK_FISHERMAN, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.FISHERMAN_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.FISHERMAN_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.FISHERMAN_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.FISHERMAN_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.FISHERMAN_LEVEL_5)}));
        VillagerProfession.register(registry, FLETCHER, PoiTypes.FLETCHER, SoundEvents.VILLAGER_WORK_FLETCHER, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.FLETCHER_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.FLETCHER_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.FLETCHER_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.FLETCHER_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.FLETCHER_LEVEL_5)}));
        VillagerProfession.register(registry, LEATHERWORKER, PoiTypes.LEATHERWORKER, SoundEvents.VILLAGER_WORK_LEATHERWORKER, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.LEATHERWORKER_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.LEATHERWORKER_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.LEATHERWORKER_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.LEATHERWORKER_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.LEATHERWORKER_LEVEL_5)}));
        VillagerProfession.register(registry, LIBRARIAN, PoiTypes.LIBRARIAN, SoundEvents.VILLAGER_WORK_LIBRARIAN, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.LIBRARIAN_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.LIBRARIAN_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.LIBRARIAN_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.LIBRARIAN_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.LIBRARIAN_LEVEL_5)}));
        VillagerProfession.register(registry, MASON, PoiTypes.MASON, SoundEvents.VILLAGER_WORK_MASON, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.MASON_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.MASON_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.MASON_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.MASON_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.MASON_LEVEL_5)}));
        VillagerProfession.register(registry, NITWIT, PoiType.NONE, PoiType.NONE, null);
        VillagerProfession.register(registry, SHEPHERD, PoiTypes.SHEPHERD, SoundEvents.VILLAGER_WORK_SHEPHERD, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.SHEPHERD_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.SHEPHERD_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.SHEPHERD_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.SHEPHERD_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.SHEPHERD_LEVEL_5)}));
        VillagerProfession.register(registry, TOOLSMITH, PoiTypes.TOOLSMITH, SoundEvents.VILLAGER_WORK_TOOLSMITH, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.TOOLSMITH_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.TOOLSMITH_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.TOOLSMITH_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.TOOLSMITH_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.TOOLSMITH_LEVEL_5)}));
        return VillagerProfession.register(registry, WEAPONSMITH, PoiTypes.WEAPONSMITH, SoundEvents.VILLAGER_WORK_WEAPONSMITH, (Int2ObjectMap<ResourceKey<TradeSet>>)Int2ObjectMap.ofEntries((Int2ObjectMap.Entry[])new Int2ObjectMap.Entry[]{Int2ObjectMap.entry((int)1, TradeSets.WEAPONSMITH_LEVEL_1), Int2ObjectMap.entry((int)2, TradeSets.WEAPONSMITH_LEVEL_2), Int2ObjectMap.entry((int)3, TradeSets.WEAPONSMITH_LEVEL_3), Int2ObjectMap.entry((int)4, TradeSets.WEAPONSMITH_LEVEL_4), Int2ObjectMap.entry((int)5, TradeSets.WEAPONSMITH_LEVEL_5)}));
    }

    public @Nullable ResourceKey<TradeSet> getTrades(int level) {
        return (ResourceKey)this.tradeSetsByLevel.get(level);
    }
}

