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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SpikeConfiguration(BlockState state, BlockPredicate canPlaceOn, BlockPredicate canReplace) implements FeatureConfiguration
{
    public static final Codec<SpikeConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockState.CODEC.fieldOf("state").forGetter(SpikeConfiguration::state), (App)BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(SpikeConfiguration::canPlaceOn), (App)BlockPredicate.CODEC.fieldOf("can_replace").forGetter(SpikeConfiguration::canReplace)).apply((Applicative)i, SpikeConfiguration::new));
}

