/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.equipment;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.EquipmentAsset;

public interface EquipmentAssets {
    public static final ResourceKey<? extends Registry<EquipmentAsset>> ROOT_ID = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));
    public static final ResourceKey<EquipmentAsset> LEATHER = EquipmentAssets.createId("leather");
    public static final ResourceKey<EquipmentAsset> COPPER = EquipmentAssets.createId("copper");
    public static final ResourceKey<EquipmentAsset> CHAINMAIL = EquipmentAssets.createId("chainmail");
    public static final ResourceKey<EquipmentAsset> IRON = EquipmentAssets.createId("iron");
    public static final ResourceKey<EquipmentAsset> GOLD = EquipmentAssets.createId("gold");
    public static final ResourceKey<EquipmentAsset> DIAMOND = EquipmentAssets.createId("diamond");
    public static final ResourceKey<EquipmentAsset> TURTLE_SCUTE = EquipmentAssets.createId("turtle_scute");
    public static final ResourceKey<EquipmentAsset> NETHERITE = EquipmentAssets.createId("netherite");
    public static final ResourceKey<EquipmentAsset> ARMADILLO_SCUTE = EquipmentAssets.createId("armadillo_scute");
    public static final ResourceKey<EquipmentAsset> ELYTRA = EquipmentAssets.createId("elytra");
    public static final ResourceKey<EquipmentAsset> SADDLE = EquipmentAssets.createId("saddle");
    public static final Map<DyeColor, ResourceKey<EquipmentAsset>> CARPETS = Util.makeEnumMap(DyeColor.class, color -> EquipmentAssets.createId(color.getSerializedName() + "_carpet"));
    public static final ResourceKey<EquipmentAsset> TRADER_LLAMA = EquipmentAssets.createId("trader_llama");
    public static final ResourceKey<EquipmentAsset> TRADER_LLAMA_BABY = EquipmentAssets.createId("trader_llama_baby");
    public static final Map<DyeColor, ResourceKey<EquipmentAsset>> HARNESSES = Util.makeEnumMap(DyeColor.class, color -> EquipmentAssets.createId(color.getSerializedName() + "_harness"));

    public static ResourceKey<EquipmentAsset> createId(String name) {
        return ResourceKey.create(ROOT_ID, Identifier.withDefaultNamespace(name));
    }
}

