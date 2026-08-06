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
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class LargeDripstoneConfiguration
implements FeatureConfiguration {
    public static final Codec<LargeDripstoneConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks), (App)Codec.intRange((int)1, (int)512).optionalFieldOf("floor_to_ceiling_search_range", (Object)30).forGetter(c -> c.floorToCeilingSearchRange), (App)IntProviders.codec(1, 16).fieldOf("column_radius").forGetter(c -> c.columnRadius), (App)FloatProviders.codec(0.0f, 20.0f).fieldOf("height_scale").forGetter(c -> c.heightScale), (App)Codec.floatRange((float)0.1f, (float)1.0f).fieldOf("max_column_radius_to_cave_height_ratio").forGetter(c -> Float.valueOf(c.maxColumnRadiusToCaveHeightRatio)), (App)FloatProviders.codec(0.1f, 10.0f).fieldOf("stalactite_bluntness").forGetter(c -> c.stalactiteBluntness), (App)FloatProviders.codec(0.1f, 10.0f).fieldOf("stalagmite_bluntness").forGetter(c -> c.stalagmiteBluntness), (App)FloatProviders.codec(0.0f, 2.0f).fieldOf("wind_speed").forGetter(c -> c.windSpeed), (App)Codec.intRange((int)0, (int)100).fieldOf("min_radius_for_wind").forGetter(c -> c.minRadiusForWind), (App)Codec.floatRange((float)0.0f, (float)5.0f).fieldOf("min_bluntness_for_wind").forGetter(c -> Float.valueOf(c.minBluntnessForWind))).apply((Applicative)i, LargeDripstoneConfiguration::new));
    public final HolderSet<Block> replaceableBlocks;
    public final int floorToCeilingSearchRange;
    public final IntProvider columnRadius;
    public final FloatProvider heightScale;
    public final float maxColumnRadiusToCaveHeightRatio;
    public final FloatProvider stalactiteBluntness;
    public final FloatProvider stalagmiteBluntness;
    public final FloatProvider windSpeed;
    public final int minRadiusForWind;
    public final float minBluntnessForWind;

    public LargeDripstoneConfiguration(HolderSet<Block> replaceableBlocks, int floorToCeilingSearchRange, IntProvider columnRadius, FloatProvider heightScale, float maxColumnRadiusToCaveHeightRatio, FloatProvider stalactiteBluntness, FloatProvider stalagmiteBluntness, FloatProvider windSpeed, int minRadiusForWind, float minBluntnessForWind) {
        this.replaceableBlocks = replaceableBlocks;
        this.floorToCeilingSearchRange = floorToCeilingSearchRange;
        this.columnRadius = columnRadius;
        this.heightScale = heightScale;
        this.maxColumnRadiusToCaveHeightRatio = maxColumnRadiusToCaveHeightRatio;
        this.stalactiteBluntness = stalactiteBluntness;
        this.stalagmiteBluntness = stalagmiteBluntness;
        this.windSpeed = windSpeed;
        this.minRadiusForWind = minRadiusForWind;
        this.minBluntnessForWind = minBluntnessForWind;
    }
}

