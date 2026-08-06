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
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record GeodeConfiguration(GeodeBlockSettings geodeBlockSettings, GeodeLayerSettings geodeLayerSettings, GeodeCrackSettings geodeCrackSettings, double usePotentialPlacementsChance, double useAlternateLayer0Chance, boolean placementsRequireLayer0Alternate, IntProvider outerWallDistance, IntProvider distributionPoints, IntProvider pointOffset, int minGenOffset, int maxGenOffset, double noiseMultiplier, int invalidBlocksThreshold) implements FeatureConfiguration
{
    public static final Codec<Double> CHANCE_RANGE = Codec.doubleRange((double)0.0, (double)1.0);
    public static final Codec<GeodeConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)GeodeBlockSettings.CODEC.fieldOf("blocks").forGetter(GeodeConfiguration::geodeBlockSettings), (App)GeodeLayerSettings.CODEC.fieldOf("layers").forGetter(GeodeConfiguration::geodeLayerSettings), (App)GeodeCrackSettings.CODEC.fieldOf("crack").forGetter(GeodeConfiguration::geodeCrackSettings), (App)CHANCE_RANGE.optionalFieldOf("use_potential_placements_chance", (Object)0.35).forGetter(GeodeConfiguration::usePotentialPlacementsChance), (App)CHANCE_RANGE.optionalFieldOf("use_alternate_layer0_chance", (Object)0.0).forGetter(GeodeConfiguration::useAlternateLayer0Chance), (App)Codec.BOOL.optionalFieldOf("placements_require_layer0_alternate", (Object)true).forGetter(GeodeConfiguration::placementsRequireLayer0Alternate), (App)IntProviders.codec(1, 20).optionalFieldOf("outer_wall_distance", (Object)UniformInt.of(4, 5)).forGetter(GeodeConfiguration::outerWallDistance), (App)IntProviders.codec(1, 20).optionalFieldOf("distribution_points", (Object)UniformInt.of(3, 4)).forGetter(GeodeConfiguration::distributionPoints), (App)IntProviders.codec(0, 10).optionalFieldOf("point_offset", (Object)UniformInt.of(1, 2)).forGetter(GeodeConfiguration::pointOffset), (App)Codec.INT.optionalFieldOf("min_gen_offset", (Object)-16).forGetter(GeodeConfiguration::minGenOffset), (App)Codec.INT.optionalFieldOf("max_gen_offset", (Object)16).forGetter(GeodeConfiguration::maxGenOffset), (App)CHANCE_RANGE.optionalFieldOf("noise_multiplier", (Object)0.05).forGetter(GeodeConfiguration::noiseMultiplier), (App)Codec.INT.fieldOf("invalid_blocks_threshold").forGetter(GeodeConfiguration::invalidBlocksThreshold)).apply((Applicative)i, GeodeConfiguration::new));
}

