/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.enchantment.providers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;

public record SingleEnchantment(Holder<Enchantment> enchantment, IntProvider level) implements EnchantmentProvider
{
    public static final MapCodec<SingleEnchantment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Enchantment.CODEC.fieldOf("enchantment").forGetter(SingleEnchantment::enchantment), (App)IntProviders.CODEC.fieldOf("level").forGetter(SingleEnchantment::level)).apply((Applicative)i, SingleEnchantment::new));

    @Override
    public void enchant(ItemStack item, ItemEnchantments.Mutable itemEnchantments, RandomSource random, DifficultyInstance difficulty) {
        itemEnchantments.upgrade(this.enchantment, Mth.clamp(this.level.sample(random), this.enchantment.value().getMinLevel(), this.enchantment.value().getMaxLevel()));
    }

    public MapCodec<SingleEnchantment> codec() {
        return CODEC;
    }
}

