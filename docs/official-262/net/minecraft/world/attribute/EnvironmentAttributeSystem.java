/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.attribute;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.attribute.WeatherAttributes;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.timeline.Timeline;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeSystem
implements EnvironmentAttributeReader {
    private final Map<EnvironmentAttribute<?>, ValueSampler<?>> attributeSamplers = new Reference2ObjectOpenHashMap();

    private EnvironmentAttributeSystem(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> layersByAttribute) {
        layersByAttribute.forEach((attribute, layers) -> this.attributeSamplers.put((EnvironmentAttribute<?>)attribute, this.bakeLayerSampler((EnvironmentAttribute)attribute, (List<? extends EnvironmentAttributeLayer<?>>)layers)));
    }

    private <Value> ValueSampler<Value> bakeLayerSampler(EnvironmentAttribute<Value> attribute, List<? extends EnvironmentAttributeLayer<?>> untypedLayers) {
        Object e;
        ArrayList layers = new ArrayList(untypedLayers);
        Value constantBaseValue = attribute.defaultValue();
        while (!layers.isEmpty() && (e = layers.getFirst()) instanceof EnvironmentAttributeLayer.Constant) {
            EnvironmentAttributeLayer.Constant constantLayer = (EnvironmentAttributeLayer.Constant)e;
            constantBaseValue = constantLayer.applyConstant(constantBaseValue);
            layers.removeFirst();
        }
        boolean isAffectedByPosition = layers.stream().anyMatch(layer -> layer instanceof EnvironmentAttributeLayer.Positional);
        return new ValueSampler<Value>(attribute, constantBaseValue, List.copyOf(layers), isAffectedByPosition);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void addDefaultLayers(Builder builder, Level level) {
        RegistryAccess registries = level.registryAccess();
        BiomeManager biomeManager = level.getBiomeManager();
        ClockManager clockManager = level.clockManager();
        EnvironmentAttributeSystem.addDimensionLayer(builder, level.dimensionType());
        EnvironmentAttributeSystem.addBiomeLayer(builder, registries.lookupOrThrow(Registries.BIOME), biomeManager);
        level.dimensionType().timelines().forEach(timeline -> builder.addTimelineLayer((Holder<Timeline>)timeline, clockManager));
        if (level.canHaveWeather()) {
            WeatherAttributes.addBuiltinLayers(builder, WeatherAttributes.WeatherAccess.from(level));
        }
    }

    private static void addDimensionLayer(Builder builder, DimensionType dimensionType) {
        builder.addConstantLayer(dimensionType.attributes());
    }

    private static void addBiomeLayer(Builder builder, HolderLookup<Biome> biomes, BiomeManager biomeManager) {
        Stream attributesProvidedByBiomes = biomes.listElements().flatMap(biome -> ((Biome)biome.value()).getAttributes().keySet().stream()).distinct();
        attributesProvidedByBiomes.forEach(attribute -> EnvironmentAttributeSystem.addBiomeLayerForAttribute(builder, attribute, biomeManager));
    }

    private static <Value> void addBiomeLayerForAttribute(Builder builder, EnvironmentAttribute<Value> attribute, BiomeManager biomeManager) {
        builder.addPositionalLayer(attribute, (baseValue, pos, biomeWeights) -> {
            if (biomeWeights != null && attribute.isSpatiallyInterpolated()) {
                return biomeWeights.applyAttributeLayer(attribute, baseValue);
            }
            Holder<Biome> biome = biomeManager.getNoiseBiomeAtPosition(pos.x, pos.y, pos.z);
            return biome.value().getAttributes().applyModifier(attribute, baseValue);
        });
    }

    public void invalidateTickCache() {
        this.attributeSamplers.values().forEach(ValueSampler::invalidateTickCache);
    }

    private <Value> @Nullable ValueSampler<Value> getValueSampler(EnvironmentAttribute<Value> attribute) {
        return this.attributeSamplers.get(attribute);
    }

    @Override
    public <Value> Value getDimensionValue(EnvironmentAttribute<Value> attribute) {
        if (SharedConstants.IS_RUNNING_IN_IDE && attribute.isPositional()) {
            throw new IllegalStateException("Position must always be provided for positional attribute " + String.valueOf(attribute));
        }
        ValueSampler<Value> sampler = this.getValueSampler(attribute);
        if (sampler == null) {
            return attribute.defaultValue();
        }
        return sampler.getDimensionValue();
    }

    @Override
    public <Value> Value getValue(EnvironmentAttribute<Value> attribute, Vec3 pos, @Nullable SpatialAttributeInterpolator biomeInterpolator) {
        ValueSampler<Value> sampler = this.getValueSampler(attribute);
        if (sampler == null) {
            return attribute.defaultValue();
        }
        return sampler.getValue(pos, biomeInterpolator);
    }

    @VisibleForTesting
    <Value> Value getConstantBaseValue(EnvironmentAttribute<Value> attribute) {
        ValueSampler<Value> sampler = this.getValueSampler(attribute);
        return sampler != null ? sampler.baseValue : attribute.defaultValue();
    }

    @VisibleForTesting
    boolean isAffectedByPosition(EnvironmentAttribute<?> attribute) {
        ValueSampler<?> sampler = this.getValueSampler(attribute);
        return sampler != null && sampler.isAffectedByPosition;
    }

    private static class ValueSampler<Value> {
        private final EnvironmentAttribute<Value> attribute;
        private final Value baseValue;
        private final List<EnvironmentAttributeLayer<Value>> layers;
        private final boolean isAffectedByPosition;
        private @Nullable Value cachedTickValue;
        private int cacheTickId;

        private ValueSampler(EnvironmentAttribute<Value> attribute, Value baseValue, List<EnvironmentAttributeLayer<Value>> layers, boolean isAffectedByPosition) {
            this.attribute = attribute;
            this.baseValue = baseValue;
            this.layers = layers;
            this.isAffectedByPosition = isAffectedByPosition;
        }

        public void invalidateTickCache() {
            this.cachedTickValue = null;
            ++this.cacheTickId;
        }

        public Value getDimensionValue() {
            if (this.cachedTickValue != null) {
                return this.cachedTickValue;
            }
            Value result = this.computeValueNotPositional();
            this.cachedTickValue = result;
            return result;
        }

        public Value getValue(Vec3 pos, @Nullable SpatialAttributeInterpolator biomeInterpolator) {
            if (!this.isAffectedByPosition) {
                return this.getDimensionValue();
            }
            return this.computeValuePositional(pos, biomeInterpolator);
        }

        private Value computeValuePositional(Vec3 pos, @Nullable SpatialAttributeInterpolator biomeInterpolator) {
            Value result = this.baseValue;
            for (EnvironmentAttributeLayer<Value> layer : this.layers) {
                EnvironmentAttributeLayer<Value> environmentAttributeLayer;
                Objects.requireNonNull(layer);
                int n = 0;
                result = switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{EnvironmentAttributeLayer.Constant.class, EnvironmentAttributeLayer.TimeBased.class, EnvironmentAttributeLayer.Positional.class}, environmentAttributeLayer, n)) {
                    default -> throw new MatchException(null, null);
                    case 0 -> {
                        EnvironmentAttributeLayer.Constant constantLayer = (EnvironmentAttributeLayer.Constant)environmentAttributeLayer;
                        yield constantLayer.applyConstant(result);
                    }
                    case 1 -> {
                        EnvironmentAttributeLayer.TimeBased timeBasedLayer = (EnvironmentAttributeLayer.TimeBased)environmentAttributeLayer;
                        yield timeBasedLayer.applyTimeBased(result, this.cacheTickId);
                    }
                    case 2 -> {
                        EnvironmentAttributeLayer.Positional positionalLayer = (EnvironmentAttributeLayer.Positional)environmentAttributeLayer;
                        yield positionalLayer.applyPositional(result, Objects.requireNonNull(pos), biomeInterpolator);
                    }
                };
            }
            return this.attribute.sanitizeValue(result);
        }

        private Value computeValueNotPositional() {
            Value result = this.baseValue;
            for (EnvironmentAttributeLayer<Value> layer : this.layers) {
                EnvironmentAttributeLayer<Value> environmentAttributeLayer;
                Objects.requireNonNull(layer);
                int n = 0;
                result = switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{EnvironmentAttributeLayer.Constant.class, EnvironmentAttributeLayer.TimeBased.class, EnvironmentAttributeLayer.Positional.class}, environmentAttributeLayer, n)) {
                    default -> throw new MatchException(null, null);
                    case 0 -> {
                        EnvironmentAttributeLayer.Constant constantLayer = (EnvironmentAttributeLayer.Constant)environmentAttributeLayer;
                        yield constantLayer.applyConstant(result);
                    }
                    case 1 -> {
                        EnvironmentAttributeLayer.TimeBased timeBasedLayer = (EnvironmentAttributeLayer.TimeBased)environmentAttributeLayer;
                        yield timeBasedLayer.applyTimeBased(result, this.cacheTickId);
                    }
                    case 2 -> {
                        EnvironmentAttributeLayer.Positional ignored = (EnvironmentAttributeLayer.Positional)environmentAttributeLayer;
                        yield result;
                    }
                };
            }
            return this.attribute.sanitizeValue(result);
        }
    }

    public static class Builder {
        private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> layersByAttribute = new HashMap();

        private Builder() {
        }

        public Builder addDefaultLayers(Level level) {
            EnvironmentAttributeSystem.addDefaultLayers(this, level);
            return this;
        }

        public Builder addConstantLayer(EnvironmentAttributeMap attributeMap) {
            for (EnvironmentAttribute<?> attribute : attributeMap.keySet()) {
                this.addConstantEntry(attribute, attributeMap);
            }
            return this;
        }

        private <Value> Builder addConstantEntry(EnvironmentAttribute<Value> attribute, EnvironmentAttributeMap attributeMap) {
            EnvironmentAttributeMap.Entry<Value, ?> entry = attributeMap.get(attribute);
            if (entry == null) {
                throw new IllegalArgumentException("Missing attribute " + String.valueOf(attribute));
            }
            return this.addConstantLayer(attribute, entry::applyModifier);
        }

        public <Value> Builder addConstantLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.Constant<Value> layer) {
            return this.addLayer(attribute, layer);
        }

        public <Value> Builder addTimeBasedLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.TimeBased<Value> layer) {
            return this.addLayer(attribute, layer);
        }

        public <Value> Builder addPositionalLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.Positional<Value> layer) {
            return this.addLayer(attribute, layer);
        }

        private <Value> Builder addLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer<Value> layer) {
            this.layersByAttribute.computeIfAbsent(attribute, t -> new ArrayList()).add(layer);
            return this;
        }

        public Builder addTimelineLayer(Holder<Timeline> timeline, ClockManager clockManager) {
            for (EnvironmentAttribute<?> attribute : timeline.value().attributes()) {
                this.addTimelineLayerForAttribute(timeline, attribute, clockManager);
            }
            return this;
        }

        private <Value> void addTimelineLayerForAttribute(Holder<Timeline> timeline, EnvironmentAttribute<Value> attribute, ClockManager clockManager) {
            this.addTimeBasedLayer(attribute, timeline.value().createTrackSampler(attribute, clockManager));
        }

        public EnvironmentAttributeSystem build() {
            return new EnvironmentAttributeSystem(this.layersByAttribute);
        }
    }
}

