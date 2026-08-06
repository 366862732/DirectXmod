/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class DualNoiseProvider
extends NoiseProvider {
    public static final MapCodec<DualNoiseProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)InclusiveRange.codec(Codec.INT, 1, 64).fieldOf("variety").forGetter(p -> p.variety), (App)NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("slow_noise").forGetter(p -> p.slowNoiseParameters), (App)ExtraCodecs.POSITIVE_FLOAT.fieldOf("slow_scale").forGetter(p -> Float.valueOf(p.slowScale))).and(DualNoiseProvider.noiseProviderCodec(i)).apply((Applicative)i, DualNoiseProvider::new));
    private final InclusiveRange<Integer> variety;
    private final NormalNoise.NoiseParameters slowNoiseParameters;
    private final float slowScale;
    private final NormalNoise slowNoise;

    public DualNoiseProvider(InclusiveRange<Integer> variety, NormalNoise.NoiseParameters slowNoiseParameters, float slowScale, long seed, NormalNoise.NoiseParameters parameters, float scale, List<BlockState> states) {
        super(seed, parameters, scale, states);
        this.variety = variety;
        this.slowNoiseParameters = slowNoiseParameters;
        this.slowScale = slowScale;
        this.slowNoise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(seed)), slowNoiseParameters);
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.DUAL_NOISE_PROVIDER;
    }

    @Override
    public BlockState getState(WorldGenLevel level, RandomSource random, BlockPos pos) {
        double varietyNoise = this.getSlowNoiseValue(pos);
        int localVariety = (int)Mth.clampedMap(varietyNoise, -1.0, 1.0, (double)this.variety.minInclusive().intValue(), (double)(this.variety.maxInclusive() + 1));
        ArrayList possibleStates = Lists.newArrayListWithCapacity((int)localVariety);
        for (int i = 0; i < localVariety; ++i) {
            possibleStates.add(this.getRandomState(this.states, this.getSlowNoiseValue(pos.offset(i * 54545, 0, i * 34234))));
        }
        return this.getRandomState(possibleStates, pos, this.scale);
    }

    protected double getSlowNoiseValue(BlockPos pos) {
        return this.slowNoise.getValue((float)pos.getX() * this.slowScale, (float)pos.getY() * this.slowScale, (float)pos.getZ() * this.slowScale);
    }
}

