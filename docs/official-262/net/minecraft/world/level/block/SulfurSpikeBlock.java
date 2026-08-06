/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.block;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SulfurSpikeBlock
extends SpeleothemBlock {
    private static int MAX_GROWING_LENGTH = 2;
    public static final MapCodec<SulfurSpikeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BlockState.CODEC.fieldOf("block_to_grow_on").forGetter(b -> b.blockToGrowOn), SulfurSpikeBlock.propertiesCodec()).apply((Applicative)i, SulfurSpikeBlock::new));

    public MapCodec<SulfurSpikeBlock> codec() {
        return CODEC;
    }

    public SulfurSpikeBlock(BlockState blockToGrowOn, BlockBehaviour.Properties properties) {
        super(blockToGrowOn, properties);
    }

    @Override
    protected int getStalactiteLandingSound() {
        return 1052;
    }

    @Override
    protected int getMaxGrowthLength() {
        return MAX_GROWING_LENGTH;
    }
}

