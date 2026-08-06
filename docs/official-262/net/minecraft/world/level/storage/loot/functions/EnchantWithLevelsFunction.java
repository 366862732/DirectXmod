/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class EnchantWithLevelsFunction
extends LootItemConditionalFunction {
    public static final MapCodec<EnchantWithLevelsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> EnchantWithLevelsFunction.commonFields(i).and(i.group((App)NumberProviders.CODEC.fieldOf("levels").forGetter(f -> f.levels), (App)RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter(f -> f.options), (App)Codec.BOOL.optionalFieldOf("include_additional_cost_component", (Object)false).forGetter(f -> f.includeAdditionalCostComponent))).apply((Applicative)i, EnchantWithLevelsFunction::new));
    private final NumberProvider levels;
    private final Optional<HolderSet<Enchantment>> options;
    private final boolean includeAdditionalCostComponent;

    private EnchantWithLevelsFunction(List<LootItemCondition> predicates, NumberProvider levels, Optional<HolderSet<Enchantment>> options, boolean includeAdditionalCostComponent) {
        super(predicates);
        this.levels = levels;
        this.options = options;
        this.includeAdditionalCostComponent = includeAdditionalCostComponent;
    }

    public MapCodec<EnchantWithLevelsFunction> codec() {
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
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "levels", this.levels);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        RandomSource random = context.getRandom();
        RegistryAccess registryAccess = context.getLevel().registryAccess();
        int enchantmentCost = this.levels.getInt(context);
        ItemStack result = EnchantmentHelper.enchantItem(random, itemStack, enchantmentCost, registryAccess, this.options);
        if (this.includeAdditionalCostComponent && context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED) && !result.isEmpty() && enchantmentCost > 0) {
            result.set(DataComponents.ADDITIONAL_TRADE_COST, enchantmentCost);
        }
        return result;
    }

    public static Builder enchantWithLevels(HolderLookup.Provider registries, NumberProvider levels) {
        return new Builder(levels).withOptions(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final NumberProvider levels;
        private Optional<HolderSet<Enchantment>> options = Optional.empty();
        private boolean includeAdditionalCostComponent = false;

        public Builder(NumberProvider levels) {
            this.levels = levels;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withOptions(HolderSet<Enchantment> tag) {
            this.options = Optional.of(tag);
            return this;
        }

        public Builder withOptions(Optional<HolderSet<Enchantment>> options) {
            this.options = options;
            return this;
        }

        public Builder includeAdditionalCostComponent() {
            this.includeAdditionalCostComponent = true;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new EnchantWithLevelsFunction(this.getConditions(), this.levels, this.options, this.includeAdditionalCostComponent);
        }
    }
}

