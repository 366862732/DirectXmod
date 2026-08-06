/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SculkPatchConfiguration(int chargeCount, int amountPerCharge, int spreadAttempts, int growthRounds, int spreadRounds, IntProvider extraRareGrowths, float catalystChance) implements FeatureConfiguration
{
    public static final Codec<SculkPatchConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.intRange((int)1, (int)32).fieldOf("charge_count").forGetter(SculkPatchConfiguration::chargeCount), (App)Codec.intRange((int)1, (int)500).fieldOf("amount_per_charge").forGetter(SculkPatchConfiguration::amountPerCharge), (App)Codec.intRange((int)1, (int)64).fieldOf("spread_attempts").forGetter(SculkPatchConfiguration::spreadAttempts), (App)Codec.intRange((int)0, (int)8).fieldOf("growth_rounds").forGetter(SculkPatchConfiguration::growthRounds), (App)Codec.intRange((int)0, (int)8).fieldOf("spread_rounds").forGetter(SculkPatchConfiguration::spreadRounds), (App)IntProviders.CODEC.fieldOf("extra_rare_growths").forGetter(SculkPatchConfiguration::extraRareGrowths), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("catalyst_chance").forGetter(SculkPatchConfiguration::catalystChance)).apply((Applicative)i, SculkPatchConfiguration::new));
}

