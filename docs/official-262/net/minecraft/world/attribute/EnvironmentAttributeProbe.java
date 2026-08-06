/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.GaussianSampler;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeProbe {
    private final Map<EnvironmentAttribute<?>, ValueProbe<?>> valueProbes = new Reference2ObjectOpenHashMap();
    private final Function<EnvironmentAttribute<?>, ValueProbe<?>> valueProbeFactory = x$0 -> new ValueProbe(this, x$0);
    private @Nullable Level level;
    private @Nullable Vec3 position;
    private final SpatialAttributeInterpolator biomeInterpolator = new SpatialAttributeInterpolator();

    public void reset() {
        this.level = null;
        this.position = null;
        this.biomeInterpolator.clear();
        this.valueProbes.clear();
    }

    public void tick(Level level, Vec3 position) {
        this.level = level;
        this.position = position;
        this.valueProbes.values().removeIf(ValueProbe::tick);
        this.biomeInterpolator.clear();
        GaussianSampler.sample(position.scale(0.25), level.getBiomeManager()::getNoiseBiomeAtQuart, (weight, biome) -> this.biomeInterpolator.accumulate(weight, ((Biome)biome.value()).getAttributes()));
    }

    public <Value> Value getValue(EnvironmentAttribute<Value> attribute, float partialTicks) {
        ValueProbe<?> valueProbe = this.valueProbes.computeIfAbsent(attribute, this.valueProbeFactory);
        return (Value)valueProbe.get(attribute, partialTicks);
    }

    private class ValueProbe<Value> {
        private Value lastValue;
        private @Nullable Value newValue;
        final /* synthetic */ EnvironmentAttributeProbe this$0;

        public ValueProbe(EnvironmentAttributeProbe environmentAttributeProbe, EnvironmentAttribute<Value> attribute) {
            EnvironmentAttributeProbe environmentAttributeProbe2 = environmentAttributeProbe;
            Objects.requireNonNull(environmentAttributeProbe2);
            this.this$0 = environmentAttributeProbe2;
            Value value = this.getValueFromLevel(attribute);
            this.lastValue = value;
            this.newValue = value;
        }

        private Value getValueFromLevel(EnvironmentAttribute<Value> attribute) {
            if (this.this$0.level == null || this.this$0.position == null) {
                return attribute.defaultValue();
            }
            return this.this$0.level.environmentAttributes().getValue(attribute, this.this$0.position, this.this$0.biomeInterpolator);
        }

        public boolean tick() {
            if (this.newValue == null) {
                return true;
            }
            this.lastValue = this.newValue;
            this.newValue = null;
            return false;
        }

        public Value get(EnvironmentAttribute<Value> attribute, float partialTicks) {
            if (this.newValue == null) {
                this.newValue = this.getValueFromLevel(attribute);
            }
            return attribute.type().partialTickLerp().apply(partialTicks, this.lastValue, this.newValue);
        }
    }
}

