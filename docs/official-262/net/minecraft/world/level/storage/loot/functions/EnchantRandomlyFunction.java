/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction
extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<EnchantRandomlyFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> EnchantRandomlyFunction.commonFields(i).and(i.group((App)RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter(f -> f.options), (App)Codec.BOOL.optionalFieldOf("only_compatible", (Object)true).forGetter(f -> f.onlyCompatible), (App)Codec.BOOL.optionalFieldOf("include_additional_cost_component", (Object)false).forGetter(f -> f.includeAdditionalCostComponent))).apply((Applicative)i, EnchantRandomlyFunction::new));
    private final Optional<HolderSet<Enchantment>> options;
    private final boolean onlyCompatible;
    private final boolean includeAdditionalCostComponent;

    private EnchantRandomlyFunction(List<LootItemCondition> predicates, Optional<HolderSet<Enchantment>> options, boolean onlyCompatible, boolean includeAdditionalCostComponent) {
        super(predicates);
        this.options = options;
        this.onlyCompatible = onlyCompatible;
        this.includeAdditionalCostComponent = includeAdditionalCostComponent;
    }

    public MapCodec<EnchantRandomlyFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        if (this.includeAdditionalCostComponent) {
            return Set.of(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED);
        }
        return Set.of();
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        RandomSource random = context.getRandom();
        boolean targetIsBook = itemStack.is(Items.BOOK);
        boolean shouldCheckCompatibility = !targetIsBook && this.onlyCompatible;
        Stream<Holder> compatibleEnchantmentsStream = this.options.map(HolderSet::stream).orElseGet(() -> context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(Function.identity())).filter(candidate -> !shouldCheckCompatibility || ((Enchantment)candidate.value()).canEnchant(itemStack));
        List<Holder> compatibleEnchantments = compatibleEnchantmentsStream.toList();
        Optional<Holder> enchantment = Util.getRandomSafe(compatibleEnchantments, random);
        if (enchantment.isEmpty()) {
            LOGGER.warn("Couldn't find a compatible enchantment for {}", (Object)itemStack);
            return itemStack;
        }
        return this.enchantItem(itemStack, enchantment.get(), context);
    }

    private ItemStack enchantItem(ItemStack itemStack, Holder<Enchantment> enchantment, LootContext context) {
        RandomSource random = context.getRandom();
        int level = Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
        if (itemStack.is(Items.BOOK)) {
            itemStack = new ItemStack(Items.ENCHANTED_BOOK);
        }
        itemStack.enchant(enchantment, level);
        if (this.includeAdditionalCostComponent && context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)) {
            itemStack.set(DataComponents.ADDITIONAL_TRADE_COST, 2 + random.nextInt(5 + level * 10) + 3 * level);
        }
        return itemStack;
    }

    public static Builder randomEnchantment() {
        return new Builder();
    }

    public static Builder randomApplicableEnchantment(HolderLookup.Provider registries) {
        return EnchantRandomlyFunction.randomEnchantment().withOneOf(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private Optional<HolderSet<Enchantment>> options = Optional.empty();
        private boolean onlyCompatible = true;
        private boolean includeAdditionalCostComponent = false;

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withEnchantment(Holder<Enchantment> enchantment) {
            this.options = Optional.of(HolderSet.direct(enchantment));
            return this;
        }

        public Builder withOneOf(HolderSet<Enchantment> enchantments) {
            this.options = Optional.of(enchantments);
            return this;
        }

        public Builder withOptions(HolderSet<Enchantment> enchantments) {
            this.options = Optional.of(enchantments);
            return this;
        }

        public Builder allowingIncompatibleEnchantments() {
            this.onlyCompatible = false;
            return this;
        }

        public Builder includeAdditionalCostComponent() {
            this.includeAdditionalCostComponent = true;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new EnchantRandomlyFunction(this.getConditions(), this.options, this.onlyCompatible, this.includeAdditionalCostComponent);
        }
    }
}

