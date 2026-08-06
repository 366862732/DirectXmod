/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.biome;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.CheckerboardColumnBiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;

public class BiomeSources {
    public static MapCodec<? extends BiomeSource> bootstrap(Registry<MapCodec<? extends BiomeSource>> registry) {
        Registry.register(registry, "fixed", FixedBiomeSource.CODEC);
        Registry.register(registry, "multi_noise", MultiNoiseBiomeSource.CODEC);
        Registry.register(registry, "checkerboard", CheckerboardColumnBiomeSource.CODEC);
        return Registry.register(registry, "the_end", TheEndBiomeSource.CODEC);
    }
}

