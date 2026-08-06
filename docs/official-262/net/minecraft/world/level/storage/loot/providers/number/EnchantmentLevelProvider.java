/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public record EnchantmentLevelProvider(LevelBasedValue amount) implements NumberProvider
{
    public static final MapCodec<EnchantmentLevelProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LevelBasedValue.CODEC.fieldOf("amount").forGetter(EnchantmentLevelProvider::amount)).apply((Applicative)i, EnchantmentLevelProvider::new));

    public MapCodec<EnchantmentLevelProvider> codec() {
        return MAP_CODEC;
    }

    @Override
    public float getFloat(LootContext context) {
        int level = context.getParameter(LootContextParams.ENCHANTMENT_LEVEL);
        return this.amount.calculate(level);
    }

    public static EnchantmentLevelProvider forEnchantmentLevel(LevelBasedValue amount) {
        return new EnchantmentLevelProvider(amount);
    }
}

