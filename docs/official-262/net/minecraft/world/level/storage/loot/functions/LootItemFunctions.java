/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.functions.DiscardItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.FillPlayerHead;
import net.minecraft.world.level.storage.loot.functions.FilteredFunction;
import net.minecraft.world.level.storage.loot.functions.FunctionReference;
import net.minecraft.world.level.storage.loot.functions.LimitCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.ModifyContainerContents;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import net.minecraft.world.level.storage.loot.functions.SetAttributesFunction;
import net.minecraft.world.level.storage.loot.functions.SetBannerPatternFunction;
import net.minecraft.world.level.storage.loot.functions.SetBookCoverFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetContainerContents;
import net.minecraft.world.level.storage.loot.functions.SetContainerLootTable;
import net.minecraft.world.level.storage.loot.functions.SetCustomDataFunction;
import net.minecraft.world.level.storage.loot.functions.SetCustomModelDataFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetFireworkExplosionFunction;
import net.minecraft.world.level.storage.loot.functions.SetFireworksFunction;
import net.minecraft.world.level.storage.loot.functions.SetInstrumentFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetLoreFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.functions.SetOminousBottleAmplifierFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.functions.SetRandomDyesFunction;
import net.minecraft.world.level.storage.loot.functions.SetRandomPotionFunction;
import net.minecraft.world.level.storage.loot.functions.SetStewEffectFunction;
import net.minecraft.world.level.storage.loot.functions.SetWritableBookPagesFunction;
import net.minecraft.world.level.storage.loot.functions.SetWrittenBookPagesFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.functions.ToggleTooltips;

public class LootItemFunctions {
    public static final BiFunction<ItemStack, LootContext, ItemStack> IDENTITY = (stack, context) -> stack;
    public static final Codec<LootItemFunction> TYPED_CODEC = BuiltInRegistries.LOOT_FUNCTION_TYPE.byNameCodec().dispatch("function", LootItemFunction::codec, c -> c);
    public static final Codec<LootItemFunction> ROOT_CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(TYPED_CODEC, SequenceFunction.INLINE_CODEC));
    public static final Codec<Holder<LootItemFunction>> CODEC = RegistryFileCodec.create(Registries.ITEM_MODIFIER, ROOT_CODEC);

    public static MapCodec<? extends LootItemFunction> bootstrap(Registry<MapCodec<? extends LootItemFunction>> registry) {
        Registry.register(registry, "set_count", SetItemCountFunction.MAP_CODEC);
        Registry.register(registry, "set_item", SetItemFunction.MAP_CODEC);
        Registry.register(registry, "enchant_with_levels", EnchantWithLevelsFunction.MAP_CODEC);
        Registry.register(registry, "enchant_randomly", EnchantRandomlyFunction.MAP_CODEC);
        Registry.register(registry, "set_enchantments", SetEnchantmentsFunction.MAP_CODEC);
        Registry.register(registry, "set_custom_data", SetCustomDataFunction.MAP_CODEC);
        Registry.register(registry, "set_components", SetComponentsFunction.MAP_CODEC);
        Registry.register(registry, "furnace_smelt", SmeltItemFunction.MAP_CODEC);
        Registry.register(registry, "enchanted_count_increase", EnchantedCountIncreaseFunction.MAP_CODEC);
        Registry.register(registry, "set_damage", SetItemDamageFunction.MAP_CODEC);
        Registry.register(registry, "set_attributes", SetAttributesFunction.MAP_CODEC);
        Registry.register(registry, "set_name", SetNameFunction.MAP_CODEC);
        Registry.register(registry, "exploration_map", ExplorationMapFunction.MAP_CODEC);
        Registry.register(registry, "set_stew_effect", SetStewEffectFunction.MAP_CODEC);
        Registry.register(registry, "copy_name", CopyNameFunction.MAP_CODEC);
        Registry.register(registry, "set_contents", SetContainerContents.MAP_CODEC);
        Registry.register(registry, "modify_contents", ModifyContainerContents.MAP_CODEC);
        Registry.register(registry, "filtered", FilteredFunction.MAP_CODEC);
        Registry.register(registry, "limit_count", LimitCount.MAP_CODEC);
        Registry.register(registry, "apply_bonus", ApplyBonusCount.MAP_CODEC);
        Registry.register(registry, "set_loot_table", SetContainerLootTable.MAP_CODEC);
        Registry.register(registry, "explosion_decay", ApplyExplosionDecay.MAP_CODEC);
        Registry.register(registry, "set_lore", SetLoreFunction.MAP_CODEC);
        Registry.register(registry, "fill_player_head", FillPlayerHead.MAP_CODEC);
        Registry.register(registry, "copy_custom_data", CopyCustomDataFunction.MAP_CODEC);
        Registry.register(registry, "copy_state", CopyBlockState.MAP_CODEC);
        Registry.register(registry, "set_banner_pattern", SetBannerPatternFunction.MAP_CODEC);
        Registry.register(registry, "set_potion", SetPotionFunction.MAP_CODEC);
        Registry.register(registry, "set_random_dyes", SetRandomDyesFunction.MAP_CODEC);
        Registry.register(registry, "set_random_potion", SetRandomPotionFunction.MAP_CODEC);
        Registry.register(registry, "set_instrument", SetInstrumentFunction.MAP_CODEC);
        Registry.register(registry, "reference", FunctionReference.MAP_CODEC);
        Registry.register(registry, "sequence", SequenceFunction.MAP_CODEC);
        Registry.register(registry, "copy_components", CopyComponentsFunction.MAP_CODEC);
        Registry.register(registry, "set_fireworks", SetFireworksFunction.MAP_CODEC);
        Registry.register(registry, "set_firework_explosion", SetFireworkExplosionFunction.MAP_CODEC);
        Registry.register(registry, "set_book_cover", SetBookCoverFunction.MAP_CODEC);
        Registry.register(registry, "set_written_book_pages", SetWrittenBookPagesFunction.MAP_CODEC);
        Registry.register(registry, "set_writable_book_pages", SetWritableBookPagesFunction.MAP_CODEC);
        Registry.register(registry, "toggle_tooltips", ToggleTooltips.MAP_CODEC);
        Registry.register(registry, "set_ominous_bottle_amplifier", SetOminousBottleAmplifierFunction.MAP_CODEC);
        Registry.register(registry, "set_custom_model_data", SetCustomModelDataFunction.MAP_CODEC);
        return Registry.register(registry, "discard", DiscardItem.MAP_CODEC);
    }

    public static BiFunction<ItemStack, LootContext, ItemStack> compose(List<? extends BiFunction<ItemStack, LootContext, ItemStack>> functions) {
        List<? extends BiFunction<ItemStack, LootContext, ItemStack>> terms = List.copyOf(functions);
        return switch (terms.size()) {
            case 0 -> IDENTITY;
            case 1 -> terms.get(0);
            case 2 -> {
                BiFunction<ItemStack, LootContext, ItemStack> first = terms.get(0);
                BiFunction<ItemStack, LootContext, ItemStack> second = terms.get(1);
                yield (itemStack, context) -> (ItemStack)second.apply((ItemStack)first.apply((ItemStack)itemStack, (LootContext)context), (LootContext)context);
            }
            default -> (itemStack, context) -> {
                for (BiFunction function : terms) {
                    itemStack = (ItemStack)function.apply(itemStack, context);
                }
                return itemStack;
            };
        };
    }
}

