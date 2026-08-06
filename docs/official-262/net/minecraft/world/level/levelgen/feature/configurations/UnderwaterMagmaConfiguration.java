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
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class UnderwaterMagmaConfiguration
implements FeatureConfiguration {
    public static final Codec<UnderwaterMagmaConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.intRange((int)0, (int)512).fieldOf("floor_search_range").forGetter(c -> c.floorSearchRange), (App)Codec.intRange((int)0, (int)64).fieldOf("placement_radius_around_floor").forGetter(c -> c.placementRadiusAroundFloor), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("placement_probability_per_valid_position").forGetter(c -> Float.valueOf(c.placementProbabilityPerValidPosition))).apply((Applicative)i, UnderwaterMagmaConfiguration::new));
    public final int floorSearchRange;
    public final int placementRadiusAroundFloor;
    public final float placementProbabilityPerValidPosition;

    public UnderwaterMagmaConfiguration(int floorSearchRange, int placementRadiusAroundFloor, float placementProbabilityPerValidPosition) {
        this.floorSearchRange = floorSearchRange;
        this.placementRadiusAroundFloor = placementRadiusAroundFloor;
        this.placementProbabilityPerValidPosition = placementProbabilityPerValidPosition;
    }
}

