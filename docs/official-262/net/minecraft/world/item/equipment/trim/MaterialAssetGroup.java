/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 */
package net.minecraft.world.item.equipment.trim;

import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public record MaterialAssetGroup(AssetInfo base, Map<ResourceKey<EquipmentAsset>, AssetInfo> overrides) {
    public static final String SEPARATOR = "_";
    public static final MapCodec<MaterialAssetGroup> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)AssetInfo.CODEC.fieldOf("asset_name").forGetter(MaterialAssetGroup::base), (App)Codec.unboundedMap(ResourceKey.codec(EquipmentAssets.ROOT_ID), AssetInfo.CODEC).optionalFieldOf("override_armor_assets", Map.of()).forGetter(MaterialAssetGroup::overrides)).apply((Applicative)i, MaterialAssetGroup::new));
    public static final StreamCodec<ByteBuf, MaterialAssetGroup> STREAM_CODEC = StreamCodec.composite(AssetInfo.STREAM_CODEC, MaterialAssetGroup::base, ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ResourceKey.streamCodec(EquipmentAssets.ROOT_ID), AssetInfo.STREAM_CODEC), MaterialAssetGroup::overrides, MaterialAssetGroup::new);
    public static final MaterialAssetGroup QUARTZ = MaterialAssetGroup.create("quartz");
    public static final MaterialAssetGroup IRON = MaterialAssetGroup.create("iron", Map.of(EquipmentAssets.IRON, "iron_darker"));
    public static final MaterialAssetGroup NETHERITE = MaterialAssetGroup.create("netherite", Map.of(EquipmentAssets.NETHERITE, "netherite_darker"));
    public static final MaterialAssetGroup REDSTONE = MaterialAssetGroup.create("redstone");
    public static final MaterialAssetGroup COPPER = MaterialAssetGroup.create("copper", Map.of(EquipmentAssets.COPPER, "copper_darker"));
    public static final MaterialAssetGroup GOLD = MaterialAssetGroup.create("gold", Map.of(EquipmentAssets.GOLD, "gold_darker"));
    public static final MaterialAssetGroup EMERALD = MaterialAssetGroup.create("emerald");
    public static final MaterialAssetGroup DIAMOND = MaterialAssetGroup.create("diamond", Map.of(EquipmentAssets.DIAMOND, "diamond_darker"));
    public static final MaterialAssetGroup LAPIS = MaterialAssetGroup.create("lapis");
    public static final MaterialAssetGroup AMETHYST = MaterialAssetGroup.create("amethyst");
    public static final MaterialAssetGroup RESIN = MaterialAssetGroup.create("resin");

    public static MaterialAssetGroup create(String base) {
        return new MaterialAssetGroup(new AssetInfo(base), Map.of());
    }

    public static MaterialAssetGroup create(String base, Map<ResourceKey<EquipmentAsset>, String> overrides) {
        return new MaterialAssetGroup(new AssetInfo(base), Map.copyOf(Maps.transformValues(overrides, AssetInfo::new)));
    }

    public AssetInfo assetId(ResourceKey<EquipmentAsset> equipmentAssetId) {
        return this.overrides.getOrDefault(equipmentAssetId, this.base);
    }

    public record AssetInfo(String suffix) {
        public static final Codec<AssetInfo> CODEC = ExtraCodecs.RESOURCE_PATH_CODEC.xmap(AssetInfo::new, AssetInfo::suffix);
        public static final StreamCodec<ByteBuf, AssetInfo> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(AssetInfo::new, AssetInfo::suffix);

        public AssetInfo {
            if (!Identifier.isValidPath(suffix)) {
                throw new IllegalArgumentException("Invalid string to use as a resource path element: " + suffix);
            }
        }
    }
}

