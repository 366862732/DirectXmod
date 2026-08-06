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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SpeleothemConfiguration(BlockState baseBlock, BlockState pointedBlock, HolderSet<Block> replaceableBlocks, float chanceOfTallerGeneration, float chanceOfDirectionalSpread, float chanceOfSpreadRadius2, float chanceOfSpreadRadius3) implements FeatureConfiguration
{
    public static final Codec<SpeleothemConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockState.CODEC.fieldOf("base_block").forGetter(c -> c.baseBlock), (App)BlockState.CODEC.fieldOf("pointed_block").forGetter(c -> c.pointedBlock), (App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("chance_of_taller_generation", (Object)Float.valueOf(0.2f)).forGetter(c -> Float.valueOf(c.chanceOfTallerGeneration)), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("chance_of_directional_spread", (Object)Float.valueOf(0.7f)).forGetter(c -> Float.valueOf(c.chanceOfDirectionalSpread)), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("chance_of_spread_radius2", (Object)Float.valueOf(0.5f)).forGetter(c -> Float.valueOf(c.chanceOfSpreadRadius2)), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("chance_of_spread_radius3", (Object)Float.valueOf(0.5f)).forGetter(c -> Float.valueOf(c.chanceOfSpreadRadius3))).apply((Applicative)i, SpeleothemConfiguration::new));
}

