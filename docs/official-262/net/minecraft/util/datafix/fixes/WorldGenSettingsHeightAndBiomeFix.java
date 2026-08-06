/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 *  org.apache.commons.lang3.mutable.MutableBoolean
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.References;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenSettingsHeightAndBiomeFix
extends DataFix {
    private static final String NAME = "WorldGenSettingsHeightAndBiomeFix";
    public static final String WAS_PREVIOUSLY_INCREASED_KEY = "has_increased_height_already";

    public WorldGenSettingsHeightAndBiomeFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type worldGenSettingsType = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
        OpticFinder dimensionsFinder = worldGenSettingsType.findField("dimensions");
        Type worldGenSettingsTypeNew = this.getOutputSchema().getType(References.WORLD_GEN_SETTINGS);
        Type dimensionsType = worldGenSettingsTypeNew.findFieldType("dimensions");
        return this.fixTypeEverywhereTyped(NAME, worldGenSettingsType, worldGenSettingsTypeNew, input -> {
            OptionalDynamic wasIncreasedOpt = ((Dynamic)input.get(DSL.remainderFinder())).get(WAS_PREVIOUSLY_INCREASED_KEY);
            boolean wasExpSnap = wasIncreasedOpt.result().isEmpty();
            boolean wasPreviouslyIncreased = wasIncreasedOpt.asBoolean(true);
            return input.update(DSL.remainderFinder(), tag -> tag.remove(WAS_PREVIOUSLY_INCREASED_KEY)).updateTyped(dimensionsFinder, dimensionsType, dimensions -> Util.writeAndReadTypedOrThrow(dimensions, dimensionsType, dimensionsTag -> dimensionsTag.update("minecraft:overworld", overworldTag -> overworldTag.update("generator", generator -> {
                String generatorType = generator.get("type").asString("");
                if ("minecraft:noise".equals(generatorType)) {
                    MutableBoolean isLargeBiomes = new MutableBoolean();
                    generator = generator.update("biome_source", biomeSource -> {
                        String type = biomeSource.get("type").asString("");
                        if ("minecraft:vanilla_layered".equals(type) || wasExpSnap && "minecraft:multi_noise".equals(type)) {
                            if (biomeSource.get("large_biomes").asBoolean(false)) {
                                isLargeBiomes.setTrue();
                            }
                            return biomeSource.createMap((Map)ImmutableMap.of((Object)biomeSource.createString("preset"), (Object)biomeSource.createString("minecraft:overworld"), (Object)biomeSource.createString("type"), (Object)biomeSource.createString("minecraft:multi_noise")));
                        }
                        return biomeSource;
                    });
                    if (isLargeBiomes.booleanValue()) {
                        return generator.update("settings", settings -> {
                            if ("minecraft:overworld".equals(settings.asString(""))) {
                                return settings.createString("minecraft:large_biomes");
                            }
                            return settings;
                        });
                    }
                    return generator;
                }
                if ("minecraft:flat".equals(generatorType)) {
                    if (wasPreviouslyIncreased) {
                        return generator;
                    }
                    return generator.update("settings", settings -> settings.update("layers", WorldGenSettingsHeightAndBiomeFix::updateLayers));
                }
                return generator;
            }))));
        });
    }

    private static Dynamic<?> updateLayers(Dynamic<?> layers) {
        Dynamic airLayer = layers.createMap((Map)ImmutableMap.of((Object)layers.createString("height"), (Object)layers.createInt(64), (Object)layers.createString("block"), (Object)layers.createString("minecraft:air")));
        return layers.createList(Stream.concat(Stream.of(airLayer), layers.asStream()));
    }
}

