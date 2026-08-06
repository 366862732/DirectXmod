/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.level.DryFoliageColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class Biome {
    public static final Codec<Biome> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)ClimateSettings.CODEC.forGetter(b -> b.climateSettings), (App)EnvironmentAttributeMap.CODEC_ONLY_POSITIONAL.optionalFieldOf("attributes", (Object)EnvironmentAttributeMap.EMPTY).forGetter(b -> b.attributes), (App)BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(b -> b.specialEffects), (App)BiomeGenerationSettings.CODEC.forGetter(b -> b.generationSettings), (App)MobSpawnSettings.CODEC.forGetter(b -> b.mobSettings)).apply((Applicative)i, Biome::new));
    public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create(i -> i.group((App)ClimateSettings.CODEC.forGetter(b -> b.climateSettings), (App)EnvironmentAttributeMap.NETWORK_CODEC.optionalFieldOf("attributes", (Object)EnvironmentAttributeMap.EMPTY).forGetter(b -> b.attributes), (App)BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(b -> b.specialEffects)).apply((Applicative)i, (climateSettings, attributes, specialEffects) -> new Biome((ClimateSettings)climateSettings, (EnvironmentAttributeMap)attributes, (BiomeSpecialEffects)specialEffects, BiomeGenerationSettings.EMPTY, MobSpawnSettings.EMPTY)));
    public static final Codec<Holder<Biome>> CODEC = RegistryFileCodec.create(Registries.BIOME, DIRECT_CODEC);
    public static final Codec<HolderSet<Biome>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.BIOME, DIRECT_CODEC);
    private static final PerlinSimplexNoise TEMPERATURE_NOISE = new PerlinSimplexNoise((RandomSource)new WorldgenRandom(new LegacyRandomSource(1234L)), (List<Integer>)ImmutableList.of((Object)0));
    private static final PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE = new PerlinSimplexNoise((RandomSource)new WorldgenRandom(new LegacyRandomSource(3456L)), (List<Integer>)ImmutableList.of((Object)-2, (Object)-1, (Object)0));
    @Deprecated(forRemoval=true)
    public static final PerlinSimplexNoise BIOME_INFO_NOISE = new PerlinSimplexNoise((RandomSource)new WorldgenRandom(new LegacyRandomSource(2345L)), (List<Integer>)ImmutableList.of((Object)0));
    private static final int TEMPERATURE_CACHE_SIZE = 1024;
    private final ClimateSettings climateSettings;
    private final BiomeGenerationSettings generationSettings;
    private final MobSpawnSettings mobSettings;
    private final EnvironmentAttributeMap attributes;
    private final BiomeSpecialEffects specialEffects;
    private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
        Long2FloatLinkedOpenHashMap map = new Long2FloatLinkedOpenHashMap(this, 1024, 0.25f){
            final /* synthetic */ Biome this$0;
            {
                Biome biome = this$0;
                Objects.requireNonNull(biome);
                this.this$0 = biome;
                super(expected, f);
            }

            protected void rehash(int newN) {
            }
        };
        map.defaultReturnValue(Float.NaN);
        return map;
    });

    private Biome(ClimateSettings climateSettings, EnvironmentAttributeMap attributes, BiomeSpecialEffects specialEffects, BiomeGenerationSettings generationSettings, MobSpawnSettings mobSettings) {
        this.climateSettings = climateSettings;
        this.generationSettings = generationSettings;
        this.mobSettings = mobSettings;
        this.attributes = attributes;
        this.specialEffects = specialEffects;
    }

    public MobSpawnSettings getMobSettings() {
        return this.mobSettings;
    }

    public boolean hasPrecipitation() {
        return this.climateSettings.hasPrecipitation();
    }

    public Precipitation getPrecipitationAt(BlockPos pos, int seaLevel) {
        if (!this.hasPrecipitation()) {
            return Precipitation.NONE;
        }
        return this.coldEnoughToSnow(pos, seaLevel) ? Precipitation.SNOW : Precipitation.RAIN;
    }

    private float getHeightAdjustedTemperature(BlockPos pos, int seaLevel) {
        float adjustedTemperature = this.climateSettings.temperatureModifier.modifyTemperature(pos, this.getBaseTemperature());
        int snowLevel = seaLevel + 17;
        if (pos.getY() > snowLevel) {
            float v = (float)(TEMPERATURE_NOISE.getValue((float)pos.getX() / 8.0f, (float)pos.getZ() / 8.0f, false) * 8.0);
            return adjustedTemperature - (v + (float)pos.getY() - (float)snowLevel) * 0.05f / 40.0f;
        }
        return adjustedTemperature;
    }

    @Deprecated
    private float getTemperature(BlockPos pos, int seaLevel) {
        long key = pos.asLong();
        Long2FloatLinkedOpenHashMap cache = this.temperatureCache.get();
        float cached = cache.get(key);
        if (!Float.isNaN(cached)) {
            return cached;
        }
        float temp = this.getHeightAdjustedTemperature(pos, seaLevel);
        if (cache.size() == 1024) {
            cache.removeFirstFloat();
        }
        cache.put(key, temp);
        return temp;
    }

    public boolean shouldFreeze(LevelReader level, BlockPos pos) {
        return this.shouldFreeze(level, pos, true);
    }

    public boolean shouldFreeze(LevelReader level, BlockPos pos, boolean checkNeighbors) {
        if (this.warmEnoughToRain(pos, level.getSeaLevel())) {
            return false;
        }
        if (level.isInsideBuildHeight(pos.getY()) && level.getBrightness(LightLayer.BLOCK, pos) < 10) {
            BlockState blockState = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            if (fluidState.is(Fluids.WATER) && blockState.getBlock() instanceof LiquidBlock) {
                boolean surroundedByWater;
                if (!checkNeighbors) {
                    return true;
                }
                boolean bl = surroundedByWater = level.isWaterAt(pos.west()) && level.isWaterAt(pos.east()) && level.isWaterAt(pos.north()) && level.isWaterAt(pos.south());
                if (!surroundedByWater) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean coldEnoughToSnow(BlockPos pos, int seaLevel) {
        return !this.warmEnoughToRain(pos, seaLevel);
    }

    public boolean warmEnoughToRain(BlockPos pos, int seaLevel) {
        return this.getTemperature(pos, seaLevel) >= 0.15f;
    }

    public boolean shouldMeltFrozenOceanIcebergSlightly(BlockPos pos, int seaLevel) {
        return this.getTemperature(pos, seaLevel) > 0.1f;
    }

    public boolean shouldSnow(LevelReader level, BlockPos pos) {
        BlockState state;
        if (this.getPrecipitationAt(pos, level.getSeaLevel()) != Precipitation.SNOW) {
            return false;
        }
        return level.isInsideBuildHeight(pos.getY()) && level.getBrightness(LightLayer.BLOCK, pos) < 10 && ((state = level.getBlockState(pos)).isAir() || state.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos);
    }

    public BiomeGenerationSettings getGenerationSettings() {
        return this.generationSettings;
    }

    public int getGrassColor(double x, double z) {
        int baseGrassColor = this.getBaseGrassColor();
        return this.specialEffects.grassColorModifier().modifyColor(x, z, baseGrassColor);
    }

    private int getBaseGrassColor() {
        Optional<Integer> colorOverride = this.specialEffects.grassColorOverride();
        if (colorOverride.isPresent()) {
            return colorOverride.get();
        }
        return this.getGrassColorFromTexture();
    }

    private int getGrassColorFromTexture() {
        double temp = Mth.clamp(this.climateSettings.temperature, 0.0f, 1.0f);
        double rain = Mth.clamp(this.climateSettings.downfall, 0.0f, 1.0f);
        return GrassColor.get(temp, rain);
    }

    public int getFoliageColor() {
        return this.specialEffects.foliageColorOverride().orElseGet(this::getFoliageColorFromTexture);
    }

    private int getFoliageColorFromTexture() {
        double temp = Mth.clamp(this.climateSettings.temperature, 0.0f, 1.0f);
        double rain = Mth.clamp(this.climateSettings.downfall, 0.0f, 1.0f);
        return FoliageColor.get(temp, rain);
    }

    public int getDryFoliageColor() {
        return this.specialEffects.dryFoliageColorOverride().orElseGet(this::getDryFoliageColorFromTexture);
    }

    private int getDryFoliageColorFromTexture() {
        double temp = Mth.clamp(this.climateSettings.temperature, 0.0f, 1.0f);
        double rain = Mth.clamp(this.climateSettings.downfall, 0.0f, 1.0f);
        return DryFoliageColor.get(temp, rain);
    }

    public float getBaseTemperature() {
        return this.climateSettings.temperature;
    }

    public EnvironmentAttributeMap getAttributes() {
        return this.attributes;
    }

    public BiomeSpecialEffects getSpecialEffects() {
        return this.specialEffects;
    }

    public int getWaterColor() {
        return this.specialEffects.waterColor();
    }

    private record ClimateSettings(boolean hasPrecipitation, float temperature, TemperatureModifier temperatureModifier, float downfall) {
        public static final MapCodec<ClimateSettings> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.fieldOf("has_precipitation").forGetter(b -> b.hasPrecipitation), (App)Codec.FLOAT.fieldOf("temperature").forGetter(b -> Float.valueOf(b.temperature)), (App)TemperatureModifier.CODEC.optionalFieldOf("temperature_modifier", (Object)TemperatureModifier.NONE).forGetter(b -> b.temperatureModifier), (App)Codec.FLOAT.fieldOf("downfall").forGetter(b -> Float.valueOf(b.downfall))).apply((Applicative)i, ClimateSettings::new));
    }

    public static enum Precipitation implements StringRepresentable
    {
        NONE("none"),
        RAIN("rain"),
        SNOW("snow");

        public static final Codec<Precipitation> CODEC;
        private final String name;

        private Precipitation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Precipitation::values);
        }
    }

    public static enum TemperatureModifier implements StringRepresentable
    {
        NONE("none"){

            @Override
            public float modifyTemperature(BlockPos pos, float baseTemperature) {
                return baseTemperature;
            }
        }
        ,
        FROZEN("frozen"){

            @Override
            public float modifyTemperature(BlockPos pos, float baseTemperature) {
                double groundValueSmallVariation;
                double groundValueEdgeVariation;
                double groundValueLargeVariation = FROZEN_TEMPERATURE_NOISE.getValue((double)pos.getX() * 0.05, (double)pos.getZ() * 0.05, false) * 7.0;
                double icePatches = groundValueLargeVariation + (groundValueEdgeVariation = BIOME_INFO_NOISE.getValue((double)pos.getX() * 0.2, (double)pos.getZ() * 0.2, false));
                if (icePatches < 0.3 && (groundValueSmallVariation = BIOME_INFO_NOISE.getValue((double)pos.getX() * 0.09, (double)pos.getZ() * 0.09, false)) < 0.8) {
                    return 0.2f;
                }
                return baseTemperature;
            }
        };

        private final String name;
        public static final Codec<TemperatureModifier> CODEC;

        public abstract float modifyTemperature(BlockPos var1, float var2);

        private TemperatureModifier(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(TemperatureModifier::values);
        }
    }

    public static class BiomeBuilder {
        private boolean hasPrecipitation = true;
        private @Nullable Float temperature;
        private TemperatureModifier temperatureModifier = TemperatureModifier.NONE;
        private @Nullable Float downfall;
        private final EnvironmentAttributeMap.Builder attributes = EnvironmentAttributeMap.builder();
        private @Nullable BiomeSpecialEffects specialEffects;
        private @Nullable MobSpawnSettings mobSpawnSettings;
        private @Nullable BiomeGenerationSettings generationSettings;

        public BiomeBuilder hasPrecipitation(boolean hasPrecipitation) {
            this.hasPrecipitation = hasPrecipitation;
            return this;
        }

        public BiomeBuilder temperature(float temperature) {
            this.temperature = Float.valueOf(temperature);
            return this;
        }

        public BiomeBuilder downfall(float downfall) {
            this.downfall = Float.valueOf(downfall);
            return this;
        }

        public BiomeBuilder putAttributes(EnvironmentAttributeMap attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        public BiomeBuilder putAttributes(EnvironmentAttributeMap.Builder attributes) {
            return this.putAttributes(attributes.build());
        }

        public <Value> BiomeBuilder setAttribute(EnvironmentAttribute<Value> attribute, Value value) {
            this.attributes.set(attribute, value);
            return this;
        }

        public <Value, Parameter> BiomeBuilder modifyAttribute(EnvironmentAttribute<Value> attribute, AttributeModifier<Value, Parameter> modifier, Parameter value) {
            this.attributes.modify(attribute, modifier, value);
            return this;
        }

        public BiomeBuilder specialEffects(BiomeSpecialEffects specialEffects) {
            this.specialEffects = specialEffects;
            return this;
        }

        public BiomeBuilder mobSpawnSettings(MobSpawnSettings mobSpawnSettings) {
            this.mobSpawnSettings = mobSpawnSettings;
            return this;
        }

        public BiomeBuilder generationSettings(BiomeGenerationSettings generationSettings) {
            this.generationSettings = generationSettings;
            return this;
        }

        public BiomeBuilder temperatureAdjustment(TemperatureModifier temperatureModifier) {
            this.temperatureModifier = temperatureModifier;
            return this;
        }

        public Biome build() {
            if (this.temperature == null || this.downfall == null || this.specialEffects == null || this.mobSpawnSettings == null || this.generationSettings == null) {
                throw new IllegalStateException("You are missing parameters to build a proper biome\n" + String.valueOf(this));
            }
            return new Biome(new ClimateSettings(this.hasPrecipitation, this.temperature.floatValue(), this.temperatureModifier, this.downfall.floatValue()), this.attributes.build(), this.specialEffects, this.generationSettings, this.mobSpawnSettings);
        }

        public String toString() {
            return "BiomeBuilder{\nhasPrecipitation=" + this.hasPrecipitation + ",\ntemperature=" + this.temperature + ",\ntemperatureModifier=" + String.valueOf(this.temperatureModifier) + ",\ndownfall=" + this.downfall + ",\nspecialEffects=" + String.valueOf(this.specialEffects) + ",\nmobSpawnSettings=" + String.valueOf(this.mobSpawnSettings) + ",\ngenerationSettings=" + String.valueOf(this.generationSettings) + ",\n}";
        }
    }
}

