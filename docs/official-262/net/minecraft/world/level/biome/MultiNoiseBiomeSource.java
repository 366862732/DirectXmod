/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class MultiNoiseBiomeSource
extends BiomeSource {
    private static final MapCodec<Holder<Biome>> ENTRY_CODEC = Biome.CODEC.fieldOf("biome");
    public static final MapCodec<Climate.ParameterList<Holder<Biome>>> DIRECT_CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes");
    private static final MapCodec<Holder<MultiNoiseBiomeSourceParameterList>> PRESET_CODEC = MultiNoiseBiomeSourceParameterList.CODEC.fieldOf("preset").withLifecycle(Lifecycle.stable());
    public static final MapCodec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(DIRECT_CODEC, PRESET_CODEC).xmap(MultiNoiseBiomeSource::new, o -> o.parameters);
    private final Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

    private MultiNoiseBiomeSource(Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters) {
        this.parameters = parameters;
    }

    public static MultiNoiseBiomeSource createFromList(Climate.ParameterList<Holder<Biome>> parameters) {
        return new MultiNoiseBiomeSource((Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>>)Either.left(parameters));
    }

    public static MultiNoiseBiomeSource createFromPreset(Holder<MultiNoiseBiomeSourceParameterList> preset) {
        return new MultiNoiseBiomeSource((Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>>)Either.right(preset));
    }

    private Climate.ParameterList<Holder<Biome>> parameters() {
        return (Climate.ParameterList)this.parameters.map(direct -> direct, preset -> ((MultiNoiseBiomeSourceParameterList)preset.value()).parameters());
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.parameters().values().stream().map(Pair::getSecond);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    public boolean stable(ResourceKey<MultiNoiseBiomeSourceParameterList> expected) {
        Optional preset = this.parameters.right();
        return preset.isPresent() && ((Holder)preset.get()).is(expected);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        return this.getNoiseBiome(sampler.sample(quartX, quartY, quartZ));
    }

    @VisibleForDebug
    public Holder<Biome> getNoiseBiome(Climate.TargetPoint target) {
        return this.parameters().findValue(target);
    }

    @Override
    public void addDebugInfo(List<String> result, BlockPos feetPos, Climate.Sampler sampler) {
        int quartX = QuartPos.fromBlock(feetPos.getX());
        int quartY = QuartPos.fromBlock(feetPos.getY());
        int quartZ = QuartPos.fromBlock(feetPos.getZ());
        Climate.TargetPoint sampleQuantized = sampler.sample(quartX, quartY, quartZ);
        float continentalness = Climate.unquantizeCoord(sampleQuantized.continentalness());
        float erosion = Climate.unquantizeCoord(sampleQuantized.erosion());
        float temperature = Climate.unquantizeCoord(sampleQuantized.temperature());
        float humidity = Climate.unquantizeCoord(sampleQuantized.humidity());
        float weirdness = Climate.unquantizeCoord(sampleQuantized.weirdness());
        double peaksAndValleys = NoiseRouterData.peaksAndValleys(weirdness);
        OverworldBiomeBuilder biomeBuilder = new OverworldBiomeBuilder();
        result.add("Biome builder PV: " + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys(peaksAndValleys) + " C: " + biomeBuilder.getDebugStringForContinentalness(continentalness) + " E: " + biomeBuilder.getDebugStringForErosion(erosion) + " T: " + biomeBuilder.getDebugStringForTemperature(temperature) + " H: " + biomeBuilder.getDebugStringForHumidity(humidity));
    }
}

