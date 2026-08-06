/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.Products$P3
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Instance
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Mu
 */
package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public abstract class NoiseBasedStateProvider
extends BlockStateProvider {
    protected final long seed;
    protected final NormalNoise.NoiseParameters parameters;
    protected final float scale;
    protected final NormalNoise noise;

    protected static <P extends NoiseBasedStateProvider> Products.P3<RecordCodecBuilder.Mu<P>, Long, NormalNoise.NoiseParameters, Float> noiseCodec(RecordCodecBuilder.Instance<P> instance) {
        return instance.group((App)Codec.LONG.fieldOf("seed").forGetter(p -> p.seed), (App)NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("noise").forGetter(p -> p.parameters), (App)ExtraCodecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(p -> Float.valueOf(p.scale)));
    }

    protected NoiseBasedStateProvider(long seed, NormalNoise.NoiseParameters parameters, float scale) {
        this.seed = seed;
        this.parameters = parameters;
        this.scale = scale;
        this.noise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(seed)), parameters);
    }

    protected double getNoiseValue(BlockPos pos, double scale) {
        return this.noise.getValue((double)pos.getX() * scale, (double)pos.getY() * scale, (double)pos.getZ() * scale);
    }
}

