/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.level.levelgen.DensityFunction;

public record NoiseRouter(DensityFunction barrierNoise, DensityFunction fluidLevelFloodednessNoise, DensityFunction fluidLevelSpreadNoise, DensityFunction lavaNoise, DensityFunction temperature, DensityFunction vegetation, DensityFunction continents, DensityFunction erosion, DensityFunction depth, DensityFunction ridges, DensityFunction preliminarySurfaceLevel, DensityFunction finalDensity, DensityFunction veinToggle, DensityFunction veinRidged, DensityFunction veinGap) {
    public static final Codec<NoiseRouter> CODEC = RecordCodecBuilder.create(i -> i.group(NoiseRouter.field("barrier", NoiseRouter::barrierNoise), NoiseRouter.field("fluid_level_floodedness", NoiseRouter::fluidLevelFloodednessNoise), NoiseRouter.field("fluid_level_spread", NoiseRouter::fluidLevelSpreadNoise), NoiseRouter.field("lava", NoiseRouter::lavaNoise), NoiseRouter.field("temperature", NoiseRouter::temperature), NoiseRouter.field("vegetation", NoiseRouter::vegetation), NoiseRouter.field("continents", NoiseRouter::continents), NoiseRouter.field("erosion", NoiseRouter::erosion), NoiseRouter.field("depth", NoiseRouter::depth), NoiseRouter.field("ridges", NoiseRouter::ridges), NoiseRouter.field("preliminary_surface_level", NoiseRouter::preliminarySurfaceLevel), NoiseRouter.field("final_density", NoiseRouter::finalDensity), NoiseRouter.field("vein_toggle", NoiseRouter::veinToggle), NoiseRouter.field("vein_ridged", NoiseRouter::veinRidged), NoiseRouter.field("vein_gap", NoiseRouter::veinGap)).apply((Applicative)i, NoiseRouter::new));

    private static RecordCodecBuilder<NoiseRouter, DensityFunction> field(String name, Function<NoiseRouter, DensityFunction> getter) {
        return DensityFunction.CODEC.fieldOf(name).forGetter(getter);
    }

    public NoiseRouter mapAll(DensityFunction.Visitor visitor) {
        return new NoiseRouter(this.barrierNoise.mapAll(visitor), this.fluidLevelFloodednessNoise.mapAll(visitor), this.fluidLevelSpreadNoise.mapAll(visitor), this.lavaNoise.mapAll(visitor), this.temperature.mapAll(visitor), this.vegetation.mapAll(visitor), this.continents.mapAll(visitor), this.erosion.mapAll(visitor), this.depth.mapAll(visitor), this.ridges.mapAll(visitor), this.preliminarySurfaceLevel.mapAll(visitor), this.finalDensity.mapAll(visitor), this.veinToggle.mapAll(visitor), this.veinRidged.mapAll(visitor), this.veinGap.mapAll(visitor));
    }
}

