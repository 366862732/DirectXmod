/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Encoder
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.MapCodec
 *  org.slf4j.Logger
 */
package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.slf4j.Logger;

public class BiomeParametersDumpReport
implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path topPath;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private static final MapCodec<ResourceKey<Biome>> ENTRY_CODEC = ResourceKey.codec(Registries.BIOME).fieldOf("biome");
    private static final Codec<Climate.ParameterList<ResourceKey<Biome>>> CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes").codec();

    public BiomeParametersDumpReport(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.topPath = output.getOutputFolder(PackOutput.Target.REPORTS).resolve("biome_parameters");
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.registries.thenCompose(registryAccess -> {
            RegistryOps registryOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);
            ArrayList result = new ArrayList();
            MultiNoiseBiomeSourceParameterList.knownPresets().forEach((preset, parameterList) -> result.add(BiomeParametersDumpReport.dumpValue(this.createPath(preset.id()), cache, registryOps, CODEC, parameterList)));
            return CompletableFuture.allOf((CompletableFuture[])result.toArray(CompletableFuture[]::new));
        });
    }

    private static <E> CompletableFuture<?> dumpValue(Path path, CachedOutput cache, DynamicOps<JsonElement> ops, Encoder<E> codec, E value) {
        Optional result = codec.encodeStart(ops, value).resultOrPartial(e -> LOGGER.error("Couldn't serialize element {}: {}", (Object)path, e));
        if (result.isPresent()) {
            return DataProvider.saveStable(cache, (JsonElement)result.get(), path);
        }
        return CompletableFuture.completedFuture(null);
    }

    private Path createPath(Identifier element) {
        return element.withSuffix(".json").resolveAgainst(this.topPath);
    }

    @Override
    public final String getName() {
        return "Biome Parameters";
    }
}

