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
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record HugeMushroomFeatureConfiguration(BlockStateProvider capProvider, BlockStateProvider stemProvider, int foliageRadius, BlockPredicate canPlaceOn) implements FeatureConfiguration
{
    public static final Codec<HugeMushroomFeatureConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockStateProvider.CODEC.fieldOf("cap_provider").forGetter(c -> c.capProvider), (App)BlockStateProvider.CODEC.fieldOf("stem_provider").forGetter(c -> c.stemProvider), (App)Codec.INT.optionalFieldOf("foliage_radius", (Object)2).forGetter(c -> c.foliageRadius), (App)BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(c -> c.canPlaceOn)).apply((Applicative)i, HugeMushroomFeatureConfiguration::new));
}

