/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;

public record WorldDataConfiguration(DataPackConfig dataPacks, FeatureFlagSet enabledFeatures) {
    public static final String ENABLED_FEATURES_ID = "enabled_features";
    public static final MapCodec<WorldDataConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DataPackConfig.CODEC.lenientOptionalFieldOf("DataPacks", (Object)DataPackConfig.DEFAULT).forGetter(WorldDataConfiguration::dataPacks), (App)FeatureFlags.CODEC.lenientOptionalFieldOf(ENABLED_FEATURES_ID, (Object)FeatureFlags.DEFAULT_FLAGS).forGetter(WorldDataConfiguration::enabledFeatures)).apply((Applicative)i, WorldDataConfiguration::new));
    public static final Codec<WorldDataConfiguration> CODEC = MAP_CODEC.codec();
    public static final WorldDataConfiguration DEFAULT = new WorldDataConfiguration(DataPackConfig.DEFAULT, FeatureFlags.DEFAULT_FLAGS);

    public WorldDataConfiguration expandFeatures(FeatureFlagSet newEnabledFeatures) {
        return new WorldDataConfiguration(this.dataPacks, this.enabledFeatures.join(newEnabledFeatures));
    }
}

