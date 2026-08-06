/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.trading;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class TradeSets {
    public static final ResourceKey<TradeSet> WANDERING_TRADER_BUYING = TradeSets.resourceKey("wandering_trader/buying");
    public static final ResourceKey<TradeSet> WANDERING_TRADER_COMMON = TradeSets.resourceKey("wandering_trader/common");
    public static final ResourceKey<TradeSet> WANDERING_TRADER_UNCOMMON = TradeSets.resourceKey("wandering_trader/uncommon");
    public static final ResourceKey<TradeSet> ARMORER_LEVEL_1 = TradeSets.resourceKey("armorer/level_1");
    public static final ResourceKey<TradeSet> ARMORER_LEVEL_2 = TradeSets.resourceKey("armorer/level_2");
    public static final ResourceKey<TradeSet> ARMORER_LEVEL_3 = TradeSets.resourceKey("armorer/level_3");
    public static final ResourceKey<TradeSet> ARMORER_LEVEL_4 = TradeSets.resourceKey("armorer/level_4");
    public static final ResourceKey<TradeSet> ARMORER_LEVEL_5 = TradeSets.resourceKey("armorer/level_5");
    public static final ResourceKey<TradeSet> BUTCHER_LEVEL_1 = TradeSets.resourceKey("butcher/level_1");
    public static final ResourceKey<TradeSet> BUTCHER_LEVEL_2 = TradeSets.resourceKey("butcher/level_2");
    public static final ResourceKey<TradeSet> BUTCHER_LEVEL_3 = TradeSets.resourceKey("butcher/level_3");
    public static final ResourceKey<TradeSet> BUTCHER_LEVEL_4 = TradeSets.resourceKey("butcher/level_4");
    public static final ResourceKey<TradeSet> BUTCHER_LEVEL_5 = TradeSets.resourceKey("butcher/level_5");
    public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_1 = TradeSets.resourceKey("cartographer/level_1");
    public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_2 = TradeSets.resourceKey("cartographer/level_2");
    public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_3 = TradeSets.resourceKey("cartographer/level_3");
    public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_4 = TradeSets.resourceKey("cartographer/level_4");
    public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_5 = TradeSets.resourceKey("cartographer/level_5");
    public static final ResourceKey<TradeSet> CLERIC_LEVEL_1 = TradeSets.resourceKey("cleric/level_1");
    public static final ResourceKey<TradeSet> CLERIC_LEVEL_2 = TradeSets.resourceKey("cleric/level_2");
    public static final ResourceKey<TradeSet> CLERIC_LEVEL_3 = TradeSets.resourceKey("cleric/level_3");
    public static final ResourceKey<TradeSet> CLERIC_LEVEL_4 = TradeSets.resourceKey("cleric/level_4");
    public static final ResourceKey<TradeSet> CLERIC_LEVEL_5 = TradeSets.resourceKey("cleric/level_5");
    public static final ResourceKey<TradeSet> FARMER_LEVEL_1 = TradeSets.resourceKey("farmer/level_1");
    public static final ResourceKey<TradeSet> FARMER_LEVEL_2 = TradeSets.resourceKey("farmer/level_2");
    public static final ResourceKey<TradeSet> FARMER_LEVEL_3 = TradeSets.resourceKey("farmer/level_3");
    public static final ResourceKey<TradeSet> FARMER_LEVEL_4 = TradeSets.resourceKey("farmer/level_4");
    public static final ResourceKey<TradeSet> FARMER_LEVEL_5 = TradeSets.resourceKey("farmer/level_5");
    public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_1 = TradeSets.resourceKey("fisherman/level_1");
    public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_2 = TradeSets.resourceKey("fisherman/level_2");
    public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_3 = TradeSets.resourceKey("fisherman/level_3");
    public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_4 = TradeSets.resourceKey("fisherman/level_4");
    public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_5 = TradeSets.resourceKey("fisherman/level_5");
    public static final ResourceKey<TradeSet> FLETCHER_LEVEL_1 = TradeSets.resourceKey("fletcher/level_1");
    public static final ResourceKey<TradeSet> FLETCHER_LEVEL_2 = TradeSets.resourceKey("fletcher/level_2");
    public static final ResourceKey<TradeSet> FLETCHER_LEVEL_3 = TradeSets.resourceKey("fletcher/level_3");
    public static final ResourceKey<TradeSet> FLETCHER_LEVEL_4 = TradeSets.resourceKey("fletcher/level_4");
    public static final ResourceKey<TradeSet> FLETCHER_LEVEL_5 = TradeSets.resourceKey("fletcher/level_5");
    public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_1 = TradeSets.resourceKey("leatherworker/level_1");
    public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_2 = TradeSets.resourceKey("leatherworker/level_2");
    public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_3 = TradeSets.resourceKey("leatherworker/level_3");
    public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_4 = TradeSets.resourceKey("leatherworker/level_4");
    public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_5 = TradeSets.resourceKey("leatherworker/level_5");
    public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_1 = TradeSets.resourceKey("librarian/level_1");
    public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_2 = TradeSets.resourceKey("librarian/level_2");
    public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_3 = TradeSets.resourceKey("librarian/level_3");
    public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_4 = TradeSets.resourceKey("librarian/level_4");
    public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_5 = TradeSets.resourceKey("librarian/level_5");
    public static final ResourceKey<TradeSet> MASON_LEVEL_1 = TradeSets.resourceKey("mason/level_1");
    public static final ResourceKey<TradeSet> MASON_LEVEL_2 = TradeSets.resourceKey("mason/level_2");
    public static final ResourceKey<TradeSet> MASON_LEVEL_3 = TradeSets.resourceKey("mason/level_3");
    public static final ResourceKey<TradeSet> MASON_LEVEL_4 = TradeSets.resourceKey("mason/level_4");
    public static final ResourceKey<TradeSet> MASON_LEVEL_5 = TradeSets.resourceKey("mason/level_5");
    public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_1 = TradeSets.resourceKey("shepherd/level_1");
    public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_2 = TradeSets.resourceKey("shepherd/level_2");
    public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_3 = TradeSets.resourceKey("shepherd/level_3");
    public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_4 = TradeSets.resourceKey("shepherd/level_4");
    public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_5 = TradeSets.resourceKey("shepherd/level_5");
    public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_1 = TradeSets.resourceKey("toolsmith/level_1");
    public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_2 = TradeSets.resourceKey("toolsmith/level_2");
    public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_3 = TradeSets.resourceKey("toolsmith/level_3");
    public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_4 = TradeSets.resourceKey("toolsmith/level_4");
    public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_5 = TradeSets.resourceKey("toolsmith/level_5");
    public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_1 = TradeSets.resourceKey("weaponsmith/level_1");
    public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_2 = TradeSets.resourceKey("weaponsmith/level_2");
    public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_3 = TradeSets.resourceKey("weaponsmith/level_3");
    public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_4 = TradeSets.resourceKey("weaponsmith/level_4");
    public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_5 = TradeSets.resourceKey("weaponsmith/level_5");

    public static Holder<TradeSet> bootstrap(BootstrapContext<TradeSet> context) {
        TradeSets.register(context, WANDERING_TRADER_BUYING, VillagerTradeTags.WANDERING_TRADER_BUYING);
        TradeSets.register(context, WANDERING_TRADER_COMMON, VillagerTradeTags.WANDERING_TRADER_COMMON, ConstantValue.exactly(5.0f));
        TradeSets.register(context, WANDERING_TRADER_UNCOMMON, VillagerTradeTags.WANDERING_TRADER_UNCOMMON);
        TradeSets.register(context, ARMORER_LEVEL_1, VillagerTradeTags.ARMORER_LEVEL_1);
        TradeSets.register(context, ARMORER_LEVEL_2, VillagerTradeTags.ARMORER_LEVEL_2);
        TradeSets.register(context, ARMORER_LEVEL_3, VillagerTradeTags.ARMORER_LEVEL_3);
        TradeSets.register(context, ARMORER_LEVEL_4, VillagerTradeTags.ARMORER_LEVEL_4);
        TradeSets.register(context, ARMORER_LEVEL_5, VillagerTradeTags.ARMORER_LEVEL_5);
        TradeSets.register(context, BUTCHER_LEVEL_1, VillagerTradeTags.BUTCHER_LEVEL_1);
        TradeSets.register(context, BUTCHER_LEVEL_2, VillagerTradeTags.BUTCHER_LEVEL_2);
        TradeSets.register(context, BUTCHER_LEVEL_3, VillagerTradeTags.BUTCHER_LEVEL_3);
        TradeSets.register(context, BUTCHER_LEVEL_4, VillagerTradeTags.BUTCHER_LEVEL_4);
        TradeSets.register(context, BUTCHER_LEVEL_5, VillagerTradeTags.BUTCHER_LEVEL_5);
        TradeSets.register(context, CARTOGRAPHER_LEVEL_1, VillagerTradeTags.CARTOGRAPHER_LEVEL_1);
        TradeSets.register(context, CARTOGRAPHER_LEVEL_2, VillagerTradeTags.CARTOGRAPHER_LEVEL_2);
        TradeSets.register(context, CARTOGRAPHER_LEVEL_3, VillagerTradeTags.CARTOGRAPHER_LEVEL_3);
        TradeSets.register(context, CARTOGRAPHER_LEVEL_4, VillagerTradeTags.CARTOGRAPHER_LEVEL_4);
        TradeSets.register(context, CARTOGRAPHER_LEVEL_5, VillagerTradeTags.CARTOGRAPHER_LEVEL_5);
        TradeSets.register(context, CLERIC_LEVEL_1, VillagerTradeTags.CLERIC_LEVEL_1);
        TradeSets.register(context, CLERIC_LEVEL_2, VillagerTradeTags.CLERIC_LEVEL_2);
        TradeSets.register(context, CLERIC_LEVEL_3, VillagerTradeTags.CLERIC_LEVEL_3);
        TradeSets.register(context, CLERIC_LEVEL_4, VillagerTradeTags.CLERIC_LEVEL_4);
        TradeSets.register(context, CLERIC_LEVEL_5, VillagerTradeTags.CLERIC_LEVEL_5);
        TradeSets.register(context, FARMER_LEVEL_1, VillagerTradeTags.FARMER_LEVEL_1);
        TradeSets.register(context, FARMER_LEVEL_2, VillagerTradeTags.FARMER_LEVEL_2);
        TradeSets.register(context, FARMER_LEVEL_3, VillagerTradeTags.FARMER_LEVEL_3);
        TradeSets.register(context, FARMER_LEVEL_4, VillagerTradeTags.FARMER_LEVEL_4);
        TradeSets.register(context, FARMER_LEVEL_5, VillagerTradeTags.FARMER_LEVEL_5);
        TradeSets.register(context, FISHERMAN_LEVEL_1, VillagerTradeTags.FISHERMAN_LEVEL_1);
        TradeSets.register(context, FISHERMAN_LEVEL_2, VillagerTradeTags.FISHERMAN_LEVEL_2);
        TradeSets.register(context, FISHERMAN_LEVEL_3, VillagerTradeTags.FISHERMAN_LEVEL_3);
        TradeSets.register(context, FISHERMAN_LEVEL_4, VillagerTradeTags.FISHERMAN_LEVEL_4);
        TradeSets.register(context, FISHERMAN_LEVEL_5, VillagerTradeTags.FISHERMAN_LEVEL_5);
        TradeSets.register(context, FLETCHER_LEVEL_1, VillagerTradeTags.FLETCHER_LEVEL_1);
        TradeSets.register(context, FLETCHER_LEVEL_2, VillagerTradeTags.FLETCHER_LEVEL_2);
        TradeSets.register(context, FLETCHER_LEVEL_3, VillagerTradeTags.FLETCHER_LEVEL_3);
        TradeSets.register(context, FLETCHER_LEVEL_4, VillagerTradeTags.FLETCHER_LEVEL_4);
        TradeSets.register(context, FLETCHER_LEVEL_5, VillagerTradeTags.FLETCHER_LEVEL_5);
        TradeSets.register(context, LEATHERWORKER_LEVEL_1, VillagerTradeTags.LEATHERWORKER_LEVEL_1);
        TradeSets.register(context, LEATHERWORKER_LEVEL_2, VillagerTradeTags.LEATHERWORKER_LEVEL_2);
        TradeSets.register(context, LEATHERWORKER_LEVEL_3, VillagerTradeTags.LEATHERWORKER_LEVEL_3);
        TradeSets.register(context, LEATHERWORKER_LEVEL_4, VillagerTradeTags.LEATHERWORKER_LEVEL_4);
        TradeSets.register(context, LEATHERWORKER_LEVEL_5, VillagerTradeTags.LEATHERWORKER_LEVEL_5);
        TradeSets.register(context, LIBRARIAN_LEVEL_1, VillagerTradeTags.LIBRARIAN_LEVEL_1);
        TradeSets.register(context, LIBRARIAN_LEVEL_2, VillagerTradeTags.LIBRARIAN_LEVEL_2);
        TradeSets.register(context, LIBRARIAN_LEVEL_3, VillagerTradeTags.LIBRARIAN_LEVEL_3);
        TradeSets.register(context, LIBRARIAN_LEVEL_4, VillagerTradeTags.LIBRARIAN_LEVEL_4);
        TradeSets.register(context, LIBRARIAN_LEVEL_5, VillagerTradeTags.LIBRARIAN_LEVEL_5, ConstantValue.exactly(3.0f));
        TradeSets.register(context, MASON_LEVEL_1, VillagerTradeTags.MASON_LEVEL_1);
        TradeSets.register(context, MASON_LEVEL_2, VillagerTradeTags.MASON_LEVEL_2);
        TradeSets.register(context, MASON_LEVEL_3, VillagerTradeTags.MASON_LEVEL_3);
        TradeSets.register(context, MASON_LEVEL_4, VillagerTradeTags.MASON_LEVEL_4);
        TradeSets.register(context, MASON_LEVEL_5, VillagerTradeTags.MASON_LEVEL_5);
        TradeSets.register(context, SHEPHERD_LEVEL_1, VillagerTradeTags.SHEPHERD_LEVEL_1);
        TradeSets.register(context, SHEPHERD_LEVEL_2, VillagerTradeTags.SHEPHERD_LEVEL_2);
        TradeSets.register(context, SHEPHERD_LEVEL_3, VillagerTradeTags.SHEPHERD_LEVEL_3);
        TradeSets.register(context, SHEPHERD_LEVEL_4, VillagerTradeTags.SHEPHERD_LEVEL_4);
        TradeSets.register(context, SHEPHERD_LEVEL_5, VillagerTradeTags.SHEPHERD_LEVEL_5);
        TradeSets.register(context, TOOLSMITH_LEVEL_1, VillagerTradeTags.TOOLSMITH_LEVEL_1);
        TradeSets.register(context, TOOLSMITH_LEVEL_2, VillagerTradeTags.TOOLSMITH_LEVEL_2);
        TradeSets.register(context, TOOLSMITH_LEVEL_3, VillagerTradeTags.TOOLSMITH_LEVEL_3);
        TradeSets.register(context, TOOLSMITH_LEVEL_4, VillagerTradeTags.TOOLSMITH_LEVEL_4);
        TradeSets.register(context, TOOLSMITH_LEVEL_5, VillagerTradeTags.TOOLSMITH_LEVEL_5);
        TradeSets.register(context, WEAPONSMITH_LEVEL_1, VillagerTradeTags.WEAPONSMITH_LEVEL_1);
        TradeSets.register(context, WEAPONSMITH_LEVEL_2, VillagerTradeTags.WEAPONSMITH_LEVEL_2);
        TradeSets.register(context, WEAPONSMITH_LEVEL_3, VillagerTradeTags.WEAPONSMITH_LEVEL_3);
        TradeSets.register(context, WEAPONSMITH_LEVEL_4, VillagerTradeTags.WEAPONSMITH_LEVEL_4);
        return TradeSets.register(context, WEAPONSMITH_LEVEL_5, VillagerTradeTags.WEAPONSMITH_LEVEL_5);
    }

    public static Holder.Reference<TradeSet> register(BootstrapContext<TradeSet> context, ResourceKey<TradeSet> resourceKey, TagKey<VillagerTrade> tradeTag) {
        return TradeSets.register(context, resourceKey, tradeTag, ConstantValue.exactly(2.0f));
    }

    public static Holder.Reference<TradeSet> register(BootstrapContext<TradeSet> context, ResourceKey<TradeSet> resourceKey, TagKey<VillagerTrade> tradeTag, NumberProvider numberProvider) {
        return context.register(resourceKey, new TradeSet(context.lookup(Registries.VILLAGER_TRADE).getOrThrow(tradeTag), numberProvider, false, Optional.of(resourceKey.identifier().withPrefix("trade_set/"))));
    }

    public static ResourceKey<TradeSet> resourceKey(String path) {
        return ResourceKey.create(Registries.TRADE_SET, Identifier.withDefaultNamespace(path));
    }
}

