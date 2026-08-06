/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap
 *  it.unimi.dsi.fastutil.objects.Reference2DoubleMap$Entry
 *  it.unimi.dsi.fastutil.objects.Reference2DoubleMaps
 */
package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import java.util.Objects;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.LerpFunction;

public class SpatialAttributeInterpolator {
    private final Reference2DoubleArrayMap<EnvironmentAttributeMap> weightsBySource = new Reference2DoubleArrayMap();

    public void clear() {
        this.weightsBySource.clear();
    }

    public SpatialAttributeInterpolator accumulate(double weight, EnvironmentAttributeMap attributes) {
        this.weightsBySource.mergeDouble((Object)attributes, weight, Double::sum);
        return this;
    }

    public <Value> Value applyAttributeLayer(EnvironmentAttribute<Value> attribute, Value baseValue) {
        if (this.weightsBySource.isEmpty()) {
            return baseValue;
        }
        if (this.weightsBySource.size() == 1) {
            EnvironmentAttributeMap sourceAttributes = (EnvironmentAttributeMap)this.weightsBySource.keySet().iterator().next();
            return sourceAttributes.applyModifier(attribute, baseValue);
        }
        LerpFunction<Value> lerp = attribute.type().spatialLerp();
        Object resultValue = null;
        double accumulatedWeight = 0.0;
        for (Reference2DoubleMap.Entry entry : Reference2DoubleMaps.fastIterable(this.weightsBySource)) {
            EnvironmentAttributeMap sourceAttributes = (EnvironmentAttributeMap)entry.getKey();
            double sourceWeight = entry.getDoubleValue();
            Value sourceValue = sourceAttributes.applyModifier(attribute, baseValue);
            accumulatedWeight += sourceWeight;
            if (resultValue == null) {
                resultValue = sourceValue;
                continue;
            }
            float relativeFraction = (float)(sourceWeight / accumulatedWeight);
            resultValue = lerp.apply(relativeFraction, resultValue, sourceValue);
        }
        return Objects.requireNonNull(resultValue);
    }
}

