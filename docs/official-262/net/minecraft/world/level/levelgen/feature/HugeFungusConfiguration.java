/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class HugeFungusConfiguration
implements FeatureConfiguration {
    public static final Codec<HugeFungusConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockState.CODEC.fieldOf("valid_base_block").forGetter(c -> c.validBaseState), (App)BlockState.CODEC.fieldOf("stem_state").forGetter(c -> c.stemState), (App)BlockState.CODEC.fieldOf("hat_state").forGetter(c -> c.hatState), (App)BlockState.CODEC.fieldOf("decor_state").forGetter(c -> c.decorState), (App)BlockPredicate.CODEC.fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks), (App)Codec.BOOL.optionalFieldOf("planted", (Object)false).forGetter(c -> c.planted)).apply((Applicative)i, HugeFungusConfiguration::new));
    public final BlockState validBaseState;
    public final BlockState stemState;
    public final BlockState hatState;
    public final BlockState decorState;
    public final BlockPredicate replaceableBlocks;
    public final boolean planted;

    public HugeFungusConfiguration(BlockState validBaseState, BlockState stemState, BlockState hatState, BlockState decorState, BlockPredicate replaceableBlocks, boolean planted) {
        this.validBaseState = validBaseState;
        this.stemState = stemState;
        this.hatState = hatState;
        this.decorState = decorState;
        this.replaceableBlocks = replaceableBlocks;
        this.planted = planted;
    }
}

