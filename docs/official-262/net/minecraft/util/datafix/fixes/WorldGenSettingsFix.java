/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicLike
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.OptionalDynamic
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.apache.commons.lang3.math.NumberUtils
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.apache.commons.lang3.mutable.MutableInt
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicLike;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class WorldGenSettingsFix
extends DataFix {
    private static final String VILLAGE = "minecraft:village";
    private static final String DESERT_PYRAMID = "minecraft:desert_pyramid";
    private static final String IGLOO = "minecraft:igloo";
    private static final String JUNGLE_TEMPLE = "minecraft:jungle_pyramid";
    private static final String SWAMP_HUT = "minecraft:swamp_hut";
    private static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";
    private static final String END_CITY = "minecraft:endcity";
    private static final String WOODLAND_MANSION = "minecraft:mansion";
    private static final String OCEAN_MONUMENT = "minecraft:monument";
    private static final ImmutableMap<String, StructureFeatureConfiguration> DEFAULTS = ImmutableMap.builder().put((Object)"minecraft:village", (Object)new StructureFeatureConfiguration(32, 8, 10387312)).put((Object)"minecraft:desert_pyramid", (Object)new StructureFeatureConfiguration(32, 8, 14357617)).put((Object)"minecraft:igloo", (Object)new StructureFeatureConfiguration(32, 8, 14357618)).put((Object)"minecraft:jungle_pyramid", (Object)new StructureFeatureConfiguration(32, 8, 14357619)).put((Object)"minecraft:swamp_hut", (Object)new StructureFeatureConfiguration(32, 8, 14357620)).put((Object)"minecraft:pillager_outpost", (Object)new StructureFeatureConfiguration(32, 8, 165745296)).put((Object)"minecraft:monument", (Object)new StructureFeatureConfiguration(32, 5, 10387313)).put((Object)"minecraft:endcity", (Object)new StructureFeatureConfiguration(20, 11, 10387313)).put((Object)"minecraft:mansion", (Object)new StructureFeatureConfiguration(80, 20, 10387319)).build();

    public WorldGenSettingsFix(Schema parent) {
        super(parent, true);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("WorldGenSettings building", this.getInputSchema().getType(References.WORLD_GEN_SETTINGS), settings -> settings.update(DSL.remainderFinder(), WorldGenSettingsFix::fix));
    }

    private static <T> Dynamic<T> noise(long seed, DynamicLike<T> input, Dynamic<T> noiseGeneratorSettings, Dynamic<T> biomeSource) {
        return input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString("minecraft:noise"), (Object)input.createString("biome_source"), biomeSource, (Object)input.createString("seed"), (Object)input.createLong(seed), (Object)input.createString("settings"), noiseGeneratorSettings));
    }

    private static <T> Dynamic<T> vanillaBiomeSource(Dynamic<T> input, long seed, boolean legacyBiomeInitLayer, boolean largeBiomes) {
        ImmutableMap.Builder builder = ImmutableMap.builder().put((Object)input.createString("type"), (Object)input.createString("minecraft:vanilla_layered")).put((Object)input.createString("seed"), (Object)input.createLong(seed)).put((Object)input.createString("large_biomes"), (Object)input.createBoolean(largeBiomes));
        if (legacyBiomeInitLayer) {
            builder.put((Object)input.createString("legacy_biome_init_layer"), (Object)input.createBoolean(legacyBiomeInitLayer));
        }
        return input.createMap((Map)builder.build());
    }

    private static <T> Dynamic<T> fix(Dynamic<T> input) {
        Dynamic<T> generator;
        DynamicOps ops = input.getOps();
        long seed = input.get("RandomSeed").asLong(0L);
        Optional name = input.get("generatorName").asString().map(n -> n.toLowerCase(Locale.ROOT)).result();
        Optional legacyCustomOptions = input.get("legacy_custom_options").asString().result().map(Optional::of).orElseGet(() -> {
            if (name.equals(Optional.of("customized"))) {
                return input.get("generatorOptions").asString().result();
            }
            return Optional.empty();
        });
        boolean caves = false;
        if (name.equals(Optional.of("customized"))) {
            generator = WorldGenSettingsFix.defaultOverworld(input, seed);
        } else if (name.isEmpty()) {
            generator = WorldGenSettingsFix.defaultOverworld(input, seed);
        } else {
            switch ((String)name.get()) {
                case "flat": {
                    OptionalDynamic flatSettings = input.get("generatorOptions");
                    Map<Dynamic<T>, Dynamic<T>> structureBuilder = WorldGenSettingsFix.fixFlatStructures(ops, flatSettings);
                    generator = input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString("minecraft:flat"), (Object)input.createString("settings"), (Object)input.createMap((Map)ImmutableMap.of((Object)input.createString("structures"), (Object)input.createMap(structureBuilder), (Object)input.createString("layers"), (Object)flatSettings.get("layers").result().orElseGet(() -> input.createList(Stream.of(input.createMap((Map)ImmutableMap.of((Object)input.createString("height"), (Object)input.createInt(1), (Object)input.createString("block"), (Object)input.createString("minecraft:bedrock"))), input.createMap((Map)ImmutableMap.of((Object)input.createString("height"), (Object)input.createInt(2), (Object)input.createString("block"), (Object)input.createString("minecraft:dirt"))), input.createMap((Map)ImmutableMap.of((Object)input.createString("height"), (Object)input.createInt(1), (Object)input.createString("block"), (Object)input.createString("minecraft:grass_block")))))), (Object)input.createString("biome"), (Object)input.createString(flatSettings.get("biome").asString("minecraft:plains"))))));
                    break;
                }
                case "debug_all_block_states": {
                    generator = input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString("minecraft:debug")));
                    break;
                }
                case "buffet": {
                    Dynamic fixedSource;
                    Dynamic noiseGeneratorSettings;
                    OptionalDynamic settings = input.get("generatorOptions");
                    OptionalDynamic chunkGeneratorObject = settings.get("chunk_generator");
                    Optional type = chunkGeneratorObject.get("type").asString().result();
                    if (Objects.equals(type, Optional.of("minecraft:caves"))) {
                        noiseGeneratorSettings = input.createString("minecraft:caves");
                        caves = true;
                    } else {
                        noiseGeneratorSettings = Objects.equals(type, Optional.of("minecraft:floating_islands")) ? input.createString("minecraft:floating_islands") : input.createString("minecraft:overworld");
                    }
                    Dynamic biomeSource = settings.get("biome_source").result().orElseGet(() -> input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString("minecraft:fixed"))));
                    if (biomeSource.get("type").asString().result().equals(Optional.of("minecraft:fixed"))) {
                        String biome = biomeSource.get("options").get("biomes").asStream().findFirst().flatMap(b -> b.asString().result()).orElse("minecraft:ocean");
                        fixedSource = biomeSource.remove("options").set("biome", input.createString(biome));
                    } else {
                        fixedSource = biomeSource;
                    }
                    generator = WorldGenSettingsFix.noise(seed, input, noiseGeneratorSettings, fixedSource);
                    break;
                }
                default: {
                    boolean normal = ((String)name.get()).equals("default");
                    boolean legacyBiomeInitLayer = ((String)name.get()).equals("default_1_1") || normal && input.get("generatorVersion").asInt(0) == 0;
                    boolean isAmplified = ((String)name.get()).equals("amplified");
                    boolean largeBiomes = ((String)name.get()).equals("largebiomes");
                    generator = WorldGenSettingsFix.noise(seed, input, input.createString(isAmplified ? "minecraft:amplified" : "minecraft:overworld"), WorldGenSettingsFix.vanillaBiomeSource(input, seed, legacyBiomeInitLayer, largeBiomes));
                }
            }
        }
        boolean generateMapFeatures = input.get("MapFeatures").asBoolean(true);
        boolean generateBonusChest = input.get("BonusChest").asBoolean(false);
        ImmutableMap.Builder builder = ImmutableMap.builder();
        builder.put(ops.createString("seed"), ops.createLong(seed));
        builder.put(ops.createString("generate_features"), ops.createBoolean(generateMapFeatures));
        builder.put(ops.createString("bonus_chest"), ops.createBoolean(generateBonusChest));
        builder.put(ops.createString("dimensions"), WorldGenSettingsFix.vanillaLevels(input, seed, generator, caves));
        legacyCustomOptions.ifPresent(o -> builder.put(ops.createString("legacy_custom_options"), ops.createString(o)));
        return new Dynamic(ops, ops.createMap((Map)builder.build()));
    }

    protected static <T> Dynamic<T> defaultOverworld(Dynamic<T> input, long seed) {
        return WorldGenSettingsFix.noise(seed, input, input.createString("minecraft:overworld"), WorldGenSettingsFix.vanillaBiomeSource(input, seed, false, false));
    }

    protected static <T> T vanillaLevels(Dynamic<T> input, long seed, Dynamic<T> overworldGenerator, boolean caves) {
        DynamicOps ops = input.getOps();
        return (T)ops.createMap((Map)ImmutableMap.of((Object)ops.createString("minecraft:overworld"), (Object)ops.createMap((Map)ImmutableMap.of((Object)ops.createString("type"), (Object)ops.createString("minecraft:overworld" + (caves ? "_caves" : "")), (Object)ops.createString("generator"), (Object)overworldGenerator.getValue())), (Object)ops.createString("minecraft:the_nether"), (Object)ops.createMap((Map)ImmutableMap.of((Object)ops.createString("type"), (Object)ops.createString("minecraft:the_nether"), (Object)ops.createString("generator"), (Object)WorldGenSettingsFix.noise(seed, input, input.createString("minecraft:nether"), input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString("minecraft:multi_noise"), (Object)input.createString("seed"), (Object)input.createLong(seed), (Object)input.createString("preset"), (Object)input.createString("minecraft:nether")))).getValue())), (Object)ops.createString("minecraft:the_end"), (Object)ops.createMap((Map)ImmutableMap.of((Object)ops.createString("type"), (Object)ops.createString("minecraft:the_end"), (Object)ops.createString("generator"), (Object)WorldGenSettingsFix.noise(seed, input, input.createString("minecraft:end"), input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString("minecraft:the_end"), (Object)input.createString("seed"), (Object)input.createLong(seed)))).getValue()))));
    }

    private static <T> Map<Dynamic<T>, Dynamic<T>> fixFlatStructures(DynamicOps<T> ops, OptionalDynamic<T> settings) {
        MutableInt strongholdDistance = new MutableInt(32);
        MutableInt strongholdSpread = new MutableInt(3);
        MutableInt strongholdCount = new MutableInt(128);
        MutableBoolean hasStronghold = new MutableBoolean(false);
        HashMap structureConfig = Maps.newHashMap();
        if (settings.result().isEmpty()) {
            hasStronghold.setTrue();
            structureConfig.put(VILLAGE, (StructureFeatureConfiguration)DEFAULTS.get((Object)VILLAGE));
        }
        settings.get("structures").flatMap(Dynamic::getMapValues).ifSuccess(map -> map.forEach((structureKey, value1) -> value1.getMapValues().result().ifPresent(m -> m.forEach((optionKey, optionValue) -> {
            String structureName = structureKey.asString("");
            String optionName = optionKey.asString("");
            String value = optionValue.asString("");
            if ("stronghold".equals(structureName)) {
                hasStronghold.setTrue();
                switch (optionName) {
                    case "distance": {
                        strongholdDistance.setValue(WorldGenSettingsFix.getInt(value, strongholdDistance.intValue(), 1));
                        return;
                    }
                    case "spread": {
                        strongholdSpread.setValue(WorldGenSettingsFix.getInt(value, strongholdSpread.intValue(), 1));
                        return;
                    }
                    case "count": {
                        strongholdCount.setValue(WorldGenSettingsFix.getInt(value, strongholdCount.intValue(), 1));
                        return;
                    }
                }
                return;
            }
            switch (optionName) {
                case "distance": {
                    switch (structureName) {
                        case "village": {
                            WorldGenSettingsFix.setSpacing(structureConfig, VILLAGE, value, 9);
                            return;
                        }
                        case "biome_1": {
                            WorldGenSettingsFix.setSpacing(structureConfig, DESERT_PYRAMID, value, 9);
                            WorldGenSettingsFix.setSpacing(structureConfig, IGLOO, value, 9);
                            WorldGenSettingsFix.setSpacing(structureConfig, JUNGLE_TEMPLE, value, 9);
                            WorldGenSettingsFix.setSpacing(structureConfig, SWAMP_HUT, value, 9);
                            WorldGenSettingsFix.setSpacing(structureConfig, PILLAGER_OUTPOST, value, 9);
                            return;
                        }
                        case "endcity": {
                            WorldGenSettingsFix.setSpacing(structureConfig, END_CITY, value, 1);
                            return;
                        }
                        case "mansion": {
                            WorldGenSettingsFix.setSpacing(structureConfig, WOODLAND_MANSION, value, 1);
                            return;
                        }
                    }
                    return;
                }
                case "separation": {
                    if ("oceanmonument".equals(structureName)) {
                        StructureFeatureConfiguration config = structureConfig.getOrDefault(OCEAN_MONUMENT, (StructureFeatureConfiguration)DEFAULTS.get((Object)OCEAN_MONUMENT));
                        int spacing = WorldGenSettingsFix.getInt(value, config.separation, 1);
                        structureConfig.put(OCEAN_MONUMENT, new StructureFeatureConfiguration(spacing, config.separation, config.salt));
                    }
                    return;
                }
                case "spacing": {
                    if ("oceanmonument".equals(structureName)) {
                        WorldGenSettingsFix.setSpacing(structureConfig, OCEAN_MONUMENT, value, 1);
                    }
                    return;
                }
            }
        }))));
        ImmutableMap.Builder structureBuilder = ImmutableMap.builder();
        structureBuilder.put((Object)settings.createString("structures"), (Object)settings.createMap(structureConfig.entrySet().stream().collect(Collectors.toMap(e -> settings.createString((String)e.getKey()), e -> ((StructureFeatureConfiguration)e.getValue()).serialize(ops)))));
        if (hasStronghold.isTrue()) {
            structureBuilder.put((Object)settings.createString("stronghold"), (Object)settings.createMap((Map)ImmutableMap.of((Object)settings.createString("distance"), (Object)settings.createInt(strongholdDistance.intValue()), (Object)settings.createString("spread"), (Object)settings.createInt(strongholdSpread.intValue()), (Object)settings.createString("count"), (Object)settings.createInt(strongholdCount.intValue()))));
        }
        return structureBuilder.build();
    }

    private static int getInt(String input, int def) {
        return NumberUtils.toInt((String)input, (int)def);
    }

    private static int getInt(String input, int def, int min) {
        return Math.max(min, WorldGenSettingsFix.getInt(input, def));
    }

    private static void setSpacing(Map<String, StructureFeatureConfiguration> structureConfig, String structure, String optionValue, int min) {
        StructureFeatureConfiguration config = structureConfig.getOrDefault(structure, (StructureFeatureConfiguration)DEFAULTS.get((Object)structure));
        int spacing = WorldGenSettingsFix.getInt(optionValue, config.spacing, min);
        structureConfig.put(structure, new StructureFeatureConfiguration(spacing, config.separation, config.salt));
    }

    private static final class StructureFeatureConfiguration {
        public static final Codec<StructureFeatureConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.fieldOf("spacing").forGetter(c -> c.spacing), (App)Codec.INT.fieldOf("separation").forGetter(c -> c.separation), (App)Codec.INT.fieldOf("salt").forGetter(c -> c.salt)).apply((Applicative)i, StructureFeatureConfiguration::new));
        private final int spacing;
        private final int separation;
        private final int salt;

        public StructureFeatureConfiguration(int spacing, int separation, int salt) {
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
        }

        public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
            return new Dynamic(ops, CODEC.encodeStart(ops, (Object)this).result().orElse(ops.emptyMap()));
        }
    }
}

