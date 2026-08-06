/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetEnchantmentsFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetEnchantmentsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetEnchantmentsFunction.commonFields(i).and(i.group((App)Codec.unboundedMap(Enchantment.CODEC, NumberProviders.CODEC).optionalFieldOf("enchantments", Map.of()).forGetter(f -> f.enchantments), (App)Codec.BOOL.optionalFieldOf("add", (Object)false).forGetter(f -> f.add))).apply((Applicative)i, SetEnchantmentsFunction::new));
    private final Map<Holder<Enchantment>, NumberProvider> enchantments;
    private final boolean add;

    private SetEnchantmentsFunction(List<LootItemCondition> predicates, Map<Holder<Enchantment>, NumberProvider> enchantments, boolean add) {
        super(predicates);
        this.enchantments = Map.copyOf(enchantments);
        this.add = add;
    }

    public MapCodec<SetEnchantmentsFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        this.enchantments.forEach((enchantment, value) -> value.validate(context.forMapField("enchantments", enchantment.getRegisteredName())));
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        if (itemStack.is(Items.BOOK)) {
            itemStack = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
        }
        EnchantmentHelper.updateEnchantments(itemStack, enchantments -> {
            if (this.add) {
                this.enchantments.forEach((enchantment, levelProvider) -> enchantments.set((Holder<Enchantment>)enchantment, Mth.clamp(enchantments.getLevel((Holder<Enchantment>)enchantment) + levelProvider.getInt(context), 0, 255)));
            } else {
                this.enchantments.forEach((enchantment, levelProvider) -> enchantments.set((Holder<Enchantment>)enchantment, Mth.clamp(levelProvider.getInt(context), 0, 255)));
            }
        });
        return itemStack;
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final ImmutableMap.Builder<Holder<Enchantment>, NumberProvider> enchantments = ImmutableMap.builder();
        private final boolean add;

        public Builder() {
            this(false);
        }

        public Builder(boolean add) {
            this.add = add;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withEnchantment(Holder<Enchantment> enchantment, NumberProvider levelProvider) {
            this.enchantments.put(enchantment, (Object)levelProvider);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetEnchantmentsFunction(this.getConditions(), (Map<Holder<Enchantment>, NumberProvider>)this.enchantments.build(), this.add);
        }
    }
}

