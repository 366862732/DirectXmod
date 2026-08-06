/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SequenceFeature
extends Feature<CompositeFeatureConfiguration> {
    public SequenceFeature(Codec<CompositeFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CompositeFeatureConfiguration> context) {
        for (Holder holder : context.config().features()) {
            if (((PlacedFeature)holder.value()).place(context.level(), context.chunkGenerator(), context.random(), context.origin())) continue;
            return false;
        }
        return true;
    }
}

