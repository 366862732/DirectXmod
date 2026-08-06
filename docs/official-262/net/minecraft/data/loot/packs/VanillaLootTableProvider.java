/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.loot.packs;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaArchaeologyLoot;
import net.minecraft.data.loot.packs.VanillaBlockInteractLoot;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.data.loot.packs.VanillaChargedCreeperExplosionLoot;
import net.minecraft.data.loot.packs.VanillaChestLoot;
import net.minecraft.data.loot.packs.VanillaEntityInteractLoot;
import net.minecraft.data.loot.packs.VanillaEntityLoot;
import net.minecraft.data.loot.packs.VanillaEquipmentLoot;
import net.minecraft.data.loot.packs.VanillaFishingLoot;
import net.minecraft.data.loot.packs.VanillaGiftLoot;
import net.minecraft.data.loot.packs.VanillaPiglinBarterLoot;
import net.minecraft.data.loot.packs.VanillaShearingLoot;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class VanillaLootTableProvider {
    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(output, BuiltInLootTables.all(), List.of(new LootTableProvider.SubProviderEntry(VanillaFishingLoot::new, LootContextParamSets.FISHING), new LootTableProvider.SubProviderEntry(VanillaChestLoot::new, LootContextParamSets.CHEST), new LootTableProvider.SubProviderEntry(VanillaEntityLoot::new, LootContextParamSets.ENTITY), new LootTableProvider.SubProviderEntry(VanillaEquipmentLoot::new, LootContextParamSets.EQUIPMENT), new LootTableProvider.SubProviderEntry(VanillaBlockLoot::new, LootContextParamSets.BLOCK), new LootTableProvider.SubProviderEntry(VanillaPiglinBarterLoot::new, LootContextParamSets.PIGLIN_BARTER), new LootTableProvider.SubProviderEntry(VanillaGiftLoot::new, LootContextParamSets.GIFT), new LootTableProvider.SubProviderEntry(VanillaArchaeologyLoot::new, LootContextParamSets.ARCHAEOLOGY), new LootTableProvider.SubProviderEntry(VanillaShearingLoot::new, LootContextParamSets.SHEARING), new LootTableProvider.SubProviderEntry(VanillaEntityInteractLoot::new, LootContextParamSets.ENTITY_INTERACT), new LootTableProvider.SubProviderEntry(VanillaBlockInteractLoot::new, LootContextParamSets.BLOCK_INTERACT), new LootTableProvider.SubProviderEntry(VanillaChargedCreeperExplosionLoot::new, LootContextParamSets.ENTITY)), registries);
    }
}

