/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.objects.Object2IntArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.LongStream;
import net.minecraft.util.datafix.fixes.References;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructuresBecomeConfiguredFix
extends DataFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Conversion> CONVERSION_MAP = ImmutableMap.builder().put((Object)"mineshaft", (Object)Conversion.biomeMapped(Map.of(List.of("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"), "minecraft:mineshaft_mesa"), "minecraft:mineshaft")).put((Object)"shipwreck", (Object)Conversion.biomeMapped(Map.of(List.of("minecraft:beach", "minecraft:snowy_beach"), "minecraft:shipwreck_beached"), "minecraft:shipwreck")).put((Object)"ocean_ruin", (Object)Conversion.biomeMapped(Map.of(List.of("minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean"), "minecraft:ocean_ruin_warm"), "minecraft:ocean_ruin_cold")).put((Object)"village", (Object)Conversion.biomeMapped(Map.of(List.of("minecraft:desert"), "minecraft:village_desert", List.of("minecraft:savanna"), "minecraft:village_savanna", List.of("minecraft:snowy_plains"), "minecraft:village_snowy", List.of("minecraft:taiga"), "minecraft:village_taiga"), "minecraft:village_plains")).put((Object)"ruined_portal", (Object)Conversion.biomeMapped(Map.of(List.of("minecraft:desert"), "minecraft:ruined_portal_desert", List.of("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:stony_shore", "minecraft:meadow", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:snowy_slopes"), "minecraft:ruined_portal_mountain", List.of("minecraft:bamboo_jungle", "minecraft:jungle", "minecraft:sparse_jungle"), "minecraft:ruined_portal_jungle", List.of("minecraft:deep_frozen_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:frozen_ocean", "minecraft:ocean", "minecraft:cold_ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean"), "minecraft:ruined_portal_ocean"), "minecraft:ruined_portal")).put((Object)"pillager_outpost", (Object)Conversion.trivial("minecraft:pillager_outpost")).put((Object)"mansion", (Object)Conversion.trivial("minecraft:mansion")).put((Object)"jungle_pyramid", (Object)Conversion.trivial("minecraft:jungle_pyramid")).put((Object)"desert_pyramid", (Object)Conversion.trivial("minecraft:desert_pyramid")).put((Object)"igloo", (Object)Conversion.trivial("minecraft:igloo")).put((Object)"swamp_hut", (Object)Conversion.trivial("minecraft:swamp_hut")).put((Object)"stronghold", (Object)Conversion.trivial("minecraft:stronghold")).put((Object)"monument", (Object)Conversion.trivial("minecraft:monument")).put((Object)"fortress", (Object)Conversion.trivial("minecraft:fortress")).put((Object)"endcity", (Object)Conversion.trivial("minecraft:end_city")).put((Object)"buried_treasure", (Object)Conversion.trivial("minecraft:buried_treasure")).put((Object)"nether_fossil", (Object)Conversion.trivial("minecraft:nether_fossil")).put((Object)"bastion_remnant", (Object)Conversion.trivial("minecraft:bastion_remnant")).build();

    public StructuresBecomeConfiguredFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        Type newChunkType = this.getInputSchema().getType(References.CHUNK);
        return this.writeFixAndRead("StucturesToConfiguredStructures", chunkType, newChunkType, this::fix);
    }

    private Dynamic<?> fix(Dynamic<?> chunk) {
        return chunk.update("structures", structures -> structures.update("starts", s -> this.updateStarts((Dynamic<?>)s, chunk)).update("References", r -> this.updateReferences((Dynamic<?>)r, chunk)));
    }

    private Dynamic<?> updateStarts(Dynamic<?> starts, Dynamic<?> chunk) {
        Map<Dynamic, Dynamic> values = starts.getMapValues().result().orElse(Map.of());
        HashMap newMap = Maps.newHashMap();
        values.forEach((key, start) -> {
            if (start.get("id").asString("INVALID").equals("INVALID")) {
                return;
            }
            Dynamic<?> newKey = this.findUpdatedStructureType((Dynamic<?>)key, chunk);
            if (newKey == null) {
                LOGGER.warn("Encountered unknown structure in datafixer: {}", (Object)key.asString("<missing key>"));
                return;
            }
            newMap.computeIfAbsent(newKey, k -> start.set("id", newKey));
        });
        return chunk.createMap((Map)newMap);
    }

    private Dynamic<?> updateReferences(Dynamic<?> references, Dynamic<?> chunk) {
        Map<Dynamic, Dynamic> values = references.getMapValues().result().orElse(Map.of());
        HashMap newMap = Maps.newHashMap();
        values.forEach((key, refList) -> {
            if (refList.asLongStream().count() == 0L) {
                return;
            }
            Dynamic<?> newKey = this.findUpdatedStructureType((Dynamic<?>)key, chunk);
            if (newKey == null) {
                LOGGER.warn("Encountered unknown structure in datafixer: {}", (Object)key.asString("<missing key>"));
                return;
            }
            newMap.compute(newKey, (k, oldRefList) -> {
                if (oldRefList == null) {
                    return refList;
                }
                return refList.createLongList(LongStream.concat(oldRefList.asLongStream(), refList.asLongStream()));
            });
        });
        return chunk.createMap((Map)newMap);
    }

    private @Nullable Dynamic<?> findUpdatedStructureType(Dynamic<?> dynamicKey, Dynamic<?> chunk) {
        Optional<String> result;
        String key = dynamicKey.asString("UNKNOWN").toLowerCase(Locale.ROOT);
        Conversion conversion = CONVERSION_MAP.get(key);
        if (conversion == null) {
            return null;
        }
        String resultingId = conversion.fallback;
        if (!conversion.biomeMapping().isEmpty() && (result = this.guessConfiguration(chunk, conversion)).isPresent()) {
            resultingId = result.get();
        }
        return chunk.createString(resultingId);
    }

    private Optional<String> guessConfiguration(Dynamic<?> chunk, Conversion conversion) {
        Object2IntArrayMap matches = new Object2IntArrayMap();
        chunk.get("sections").asList(Function.identity()).forEach(s -> s.get("biomes").get("palette").asList(Function.identity()).forEach(biome -> {
            String mapping = conversion.biomeMapping().get(biome.asString(""));
            if (mapping != null) {
                matches.mergeInt((Object)mapping, 1, Integer::sum);
            }
        }));
        return matches.object2IntEntrySet().stream().max(Comparator.comparingInt(Object2IntMap.Entry::getIntValue)).map(Map.Entry::getKey);
    }

    private record Conversion(Map<String, String> biomeMapping, String fallback) {
        public static Conversion trivial(String result) {
            return new Conversion(Map.of(), result);
        }

        public static Conversion biomeMapped(Map<List<String>, String> mapping, String fallback) {
            return new Conversion(Conversion.unpack(mapping), fallback);
        }

        private static Map<String, String> unpack(Map<List<String>, String> packed) {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            for (Map.Entry<List<String>, String> entry : packed.entrySet()) {
                entry.getKey().forEach(k -> builder.put(k, (Object)((String)entry.getValue())));
            }
            return builder.build();
        }
    }
}

