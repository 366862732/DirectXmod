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

public record BlockBlobConfiguration(BlockState state, BlockPredicate canPlaceOn) implements FeatureConfiguration
{
    public static final Codec<BlockBlobConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockState.CODEC.fieldOf("state").forGetter(BlockBlobConfiguration::state), (App)BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(BlockBlobConfiguration::canPlaceOn)).apply((Applicative)i, BlockBlobConfiguration::new));
}

