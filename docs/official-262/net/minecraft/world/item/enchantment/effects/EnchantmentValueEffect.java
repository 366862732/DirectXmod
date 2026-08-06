/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;
import net.minecraft.world.item.enchantment.effects.RemoveBinomial;
import net.minecraft.world.item.enchantment.effects.ScaleExponentially;
import net.minecraft.world.item.enchantment.effects.SetValue;

public interface EnchantmentValueEffect {
    public static final Codec<EnchantmentValueEffect> CODEC = BuiltInRegistries.ENCHANTMENT_VALUE_EFFECT_TYPE.byNameCodec().dispatch(EnchantmentValueEffect::codec, Function.identity());

    public static MapCodec<? extends EnchantmentValueEffect> bootstrap(Registry<MapCodec<? extends EnchantmentValueEffect>> registry) {
        Registry.register(registry, "add", AddValue.CODEC);
        Registry.register(registry, "all_of", AllOf.ValueEffects.CODEC);
        Registry.register(registry, "multiply", MultiplyValue.CODEC);
        Registry.register(registry, "remove_binomial", RemoveBinomial.CODEC);
        Registry.register(registry, "exponential", ScaleExponentially.CODEC);
        return Registry.register(registry, "set", SetValue.CODEC);
    }

    public float process(int var1, RandomSource var2, float var3);

    public MapCodec<? extends EnchantmentValueEffect> codec();
}

