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
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class EnchantedCountIncreaseFunction
extends LootItemConditionalFunction {
    public static final int NO_LIMIT = 0;
    public static final MapCodec<EnchantedCountIncreaseFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> EnchantedCountIncreaseFunction.commonFields(i).and(i.group((App)Enchantment.CODEC.fieldOf("enchantment").forGetter(f -> f.enchantment), (App)NumberProviders.CODEC.fieldOf("count").forGetter(f -> f.count), (App)Codec.INT.optionalFieldOf("limit", (Object)0).forGetter(f -> f.limit))).apply((Applicative)i, EnchantedCountIncreaseFunction::new));
    private final Holder<Enchantment> enchantment;
    private final NumberProvider count;
    private final int limit;

    private EnchantedCountIncreaseFunction(List<LootItemCondition> predicates, Holder<Enchantment> enchantment, NumberProvider count, int limit) {
        super(predicates);
        this.enchantment = enchantment;
        this.count = count;
        this.limit = limit;
    }

    public MapCodec<EnchantedCountIncreaseFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ATTACKING_ENTITY);
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "count", this.count);
    }

    private boolean hasLimit() {
        return this.limit > 0;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        Entity killer = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
        if (killer instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity)killer;
            int level = EnchantmentHelper.getEnchantmentLevel(this.enchantment, entity);
            if (level == 0) {
                return itemStack;
            }
            float addition = (float)level * this.count.getFloat(context);
            itemStack.grow(Math.round(addition));
            if (this.hasLimit()) {
                itemStack.limitSize(this.limit);
            }
        }
        return itemStack;
    }

    public static Builder lootingMultiplier(HolderLookup.Provider registries, NumberProvider count) {
        HolderGetter enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return new Builder(enchantments.getOrThrow(Enchantments.LOOTING), count);
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final Holder<Enchantment> enchantment;
        private final NumberProvider count;
        private int limit = 0;

        public Builder(Holder<Enchantment> enchantment, NumberProvider count) {
            this.enchantment = enchantment;
            this.count = count;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new EnchantedCountIncreaseFunction(this.getConditions(), this.enchantment, this.count, this.limit);
        }
    }
}

