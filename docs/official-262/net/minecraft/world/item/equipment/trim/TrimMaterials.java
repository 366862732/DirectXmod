/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.equipment.trim;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public class TrimMaterials {
    public static final ResourceKey<TrimMaterial> QUARTZ = TrimMaterials.registryKey("quartz");
    public static final ResourceKey<TrimMaterial> IRON = TrimMaterials.registryKey("iron");
    public static final ResourceKey<TrimMaterial> NETHERITE = TrimMaterials.registryKey("netherite");
    public static final ResourceKey<TrimMaterial> REDSTONE = TrimMaterials.registryKey("redstone");
    public static final ResourceKey<TrimMaterial> COPPER = TrimMaterials.registryKey("copper");
    public static final ResourceKey<TrimMaterial> GOLD = TrimMaterials.registryKey("gold");
    public static final ResourceKey<TrimMaterial> EMERALD = TrimMaterials.registryKey("emerald");
    public static final ResourceKey<TrimMaterial> DIAMOND = TrimMaterials.registryKey("diamond");
    public static final ResourceKey<TrimMaterial> LAPIS = TrimMaterials.registryKey("lapis");
    public static final ResourceKey<TrimMaterial> AMETHYST = TrimMaterials.registryKey("amethyst");
    public static final ResourceKey<TrimMaterial> RESIN = TrimMaterials.registryKey("resin");

    public static void bootstrap(BootstrapContext<TrimMaterial> context) {
        TrimMaterials.register(context, QUARTZ, Style.EMPTY.withColor(14931140), MaterialAssetGroup.QUARTZ);
        TrimMaterials.register(context, IRON, Style.EMPTY.withColor(0xECECEC), MaterialAssetGroup.IRON);
        TrimMaterials.register(context, NETHERITE, Style.EMPTY.withColor(6445145), MaterialAssetGroup.NETHERITE);
        TrimMaterials.register(context, REDSTONE, Style.EMPTY.withColor(9901575), MaterialAssetGroup.REDSTONE);
        TrimMaterials.register(context, COPPER, Style.EMPTY.withColor(11823181), MaterialAssetGroup.COPPER);
        TrimMaterials.register(context, GOLD, Style.EMPTY.withColor(14594349), MaterialAssetGroup.GOLD);
        TrimMaterials.register(context, EMERALD, Style.EMPTY.withColor(1155126), MaterialAssetGroup.EMERALD);
        TrimMaterials.register(context, DIAMOND, Style.EMPTY.withColor(7269586), MaterialAssetGroup.DIAMOND);
        TrimMaterials.register(context, LAPIS, Style.EMPTY.withColor(4288151), MaterialAssetGroup.LAPIS);
        TrimMaterials.register(context, AMETHYST, Style.EMPTY.withColor(10116294), MaterialAssetGroup.AMETHYST);
        TrimMaterials.register(context, RESIN, Style.EMPTY.withColor(16545810), MaterialAssetGroup.RESIN);
    }

    private static void register(BootstrapContext<TrimMaterial> context, ResourceKey<TrimMaterial> registryKey, Style hoverTextStyle, MaterialAssetGroup assets) {
        MutableComponent description = Component.translatable(Util.makeDescriptionId("trim_material", registryKey.identifier())).withStyle(hoverTextStyle);
        context.register(registryKey, new TrimMaterial(assets, description));
    }

    private static ResourceKey<TrimMaterial> registryKey(String id) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.withDefaultNamespace(id));
    }
}

