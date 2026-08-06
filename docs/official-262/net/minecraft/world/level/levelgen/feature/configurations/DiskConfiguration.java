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
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record DiskConfiguration(BlockStateProvider stateProvider, BlockPredicate target, IntProvider radius, int halfHeight) implements FeatureConfiguration
{
    public static final Codec<DiskConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(DiskConfiguration::stateProvider), (App)BlockPredicate.CODEC.fieldOf("target").forGetter(DiskConfiguration::target), (App)IntProviders.codec(0, 8).fieldOf("radius").forGetter(DiskConfiguration::radius), (App)Codec.intRange((int)0, (int)4).fieldOf("half_height").forGetter(DiskConfiguration::halfHeight)).apply((Applicative)i, DiskConfiguration::new));
}

