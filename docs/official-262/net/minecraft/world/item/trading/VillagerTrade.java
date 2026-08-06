/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.trading;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jspecify.annotations.Nullable;

public class VillagerTrade
implements Validatable {
    public static final Codec<VillagerTrade> CODEC = RecordCodecBuilder.create(i -> i.group((App)TradeCost.CODEC.fieldOf("wants").forGetter(villagerTrade -> villagerTrade.wants), (App)TradeCost.CODEC.optionalFieldOf("additional_wants").forGetter(villagerTrade -> villagerTrade.additionalWants), (App)ItemStackTemplate.CODEC.fieldOf("gives").forGetter(villagerTrade -> villagerTrade.gives), (App)NumberProviders.CODEC.lenientOptionalFieldOf("max_uses", (Object)ConstantValue.exactly(4.0f)).forGetter(villagerTrade -> villagerTrade.maxUses), (App)NumberProviders.CODEC.lenientOptionalFieldOf("reputation_discount", (Object)ConstantValue.exactly(0.0f)).forGetter(villagerTrade -> villagerTrade.reputationDiscount), (App)NumberProviders.CODEC.lenientOptionalFieldOf("xp", (Object)ConstantValue.exactly(1.0f)).forGetter(villagerTrade -> villagerTrade.xp), (App)LootItemCondition.DIRECT_CODEC.optionalFieldOf("merchant_predicate").forGetter(villagerTrade -> villagerTrade.merchantPredicate), (App)LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("given_item_modifiers", List.of()).forGetter(villagerTrade -> villagerTrade.givenItemModifiers), (App)RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("double_trade_price_enchantments").forGetter(villagerTrade -> villagerTrade.doubleTradePriceEnchantments)).apply((Applicative)i, VillagerTrade::new)).validate(Validatable.validatorForContext(LootContextParamSets.VILLAGER_TRADE));
    private final TradeCost wants;
    private final Optional<TradeCost> additionalWants;
    private final ItemStackTemplate gives;
    private final Optional<LootItemCondition> merchantPredicate;
    private final List<LootItemFunction> givenItemModifiers;
    private final NumberProvider maxUses;
    private final NumberProvider reputationDiscount;
    private final NumberProvider xp;
    private final Optional<HolderSet<Enchantment>> doubleTradePriceEnchantments;

    private VillagerTrade(TradeCost wants, Optional<TradeCost> additionalWants, ItemStackTemplate gives, NumberProvider maxUses, NumberProvider reputationDiscount, NumberProvider xp, Optional<LootItemCondition> merchantPredicate, List<LootItemFunction> givenItemModifiers, Optional<HolderSet<Enchantment>> doubleTradePriceEnchantments) {
        this.wants = wants;
        this.additionalWants = additionalWants;
        this.gives = gives;
        this.maxUses = maxUses;
        this.reputationDiscount = reputationDiscount;
        this.xp = xp;
        this.merchantPredicate = merchantPredicate;
        this.givenItemModifiers = givenItemModifiers;
        this.doubleTradePriceEnchantments = doubleTradePriceEnchantments;
    }

    public VillagerTrade(TradeCost wants, Optional<TradeCost> additionalWants, ItemStackTemplate gives, int maxUses, int xp, float reputationDiscount, Optional<LootItemCondition> merchantPredicate, List<LootItemFunction> givenItemModifiers, HolderSet<Enchantment> doubleTradePriceEnchantments) {
        this(wants, additionalWants, gives, ConstantValue.exactly(maxUses), ConstantValue.exactly(reputationDiscount), ConstantValue.exactly(xp), merchantPredicate, givenItemModifiers, Optional.of(doubleTradePriceEnchantments));
    }

    public VillagerTrade(TradeCost wants, Optional<TradeCost> additionalWants, ItemStackTemplate gives, int maxUses, int xp, float reputationDiscount, Optional<LootItemCondition> merchantPredicate, List<LootItemFunction> givenItemModifiers) {
        this(wants, additionalWants, gives, ConstantValue.exactly(maxUses), ConstantValue.exactly(reputationDiscount), ConstantValue.exactly(xp), merchantPredicate, givenItemModifiers, Optional.empty());
    }

    public VillagerTrade(TradeCost wants, ItemStackTemplate gives, int maxUses, int xp, float reputationDiscount, Optional<LootItemCondition> merchantPredicate, List<LootItemFunction> givenItemModifiers) {
        this(wants, Optional.empty(), gives, ConstantValue.exactly(maxUses), ConstantValue.exactly(reputationDiscount), ConstantValue.exactly(xp), merchantPredicate, givenItemModifiers, Optional.empty());
    }

    @Override
    public void validate(ValidationContext context) {
        Validatable.validate(context, "wants", this.wants);
        Validatable.validate(context, "additional_wants", this.additionalWants);
        Validatable.validate(context, "max_uses", this.maxUses);
        Validatable.validate(context, "reputation_discount", this.reputationDiscount);
        Validatable.validate(context, "xp", this.xp);
        Validatable.validate(context, "merchant_predicate", this.merchantPredicate);
        Validatable.validate(context, "given_item_modifiers", this.givenItemModifiers);
    }

    public @Nullable MerchantOffer getOffer(LootContext lootContext) {
        ItemCost itemCost;
        if (this.merchantPredicate.isPresent() && !this.merchantPredicate.get().test(lootContext)) {
            return null;
        }
        ItemStack result = this.gives.create();
        int additionalCost = 0;
        for (LootItemFunction outputItemModifier : this.givenItemModifiers) {
            result = (ItemStack)outputItemModifier.apply(result, lootContext);
            if (!result.isEmpty()) continue;
            return null;
        }
        Integer additionalTradeCost = result.remove(DataComponents.ADDITIONAL_TRADE_COST);
        if (additionalTradeCost != null) {
            additionalCost += additionalTradeCost.intValue();
        }
        if (this.doubleTradePriceEnchantments.isPresent()) {
            HolderSet<Enchantment> enchantments = this.doubleTradePriceEnchantments.get();
            ItemEnchantments itemEnchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (itemEnchantments != null) {
                if (itemEnchantments.keySet().stream().anyMatch(enchantments::contains)) {
                    additionalCost *= 2;
                }
            }
        }
        if ((itemCost = this.wants.toItemCost(lootContext, additionalCost)).count() < 1) {
            return null;
        }
        Optional<ItemCost> additionalItemCost = this.additionalWants.map(tradeCost -> tradeCost.toItemCost(lootContext, 0));
        if (additionalItemCost.isPresent() && additionalItemCost.get().count() < 1) {
            return null;
        }
        return new MerchantOffer(itemCost, additionalItemCost, result, Math.max(this.maxUses.getInt(lootContext), 1), Math.max(this.xp.getInt(lootContext), 0), Math.max(this.reputationDiscount.getFloat(lootContext), 0.0f));
    }
}

