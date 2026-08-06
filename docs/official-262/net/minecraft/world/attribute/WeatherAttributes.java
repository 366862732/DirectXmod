/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 */
package net.minecraft.world.attribute;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.attribute.modifier.FloatWithAlpha;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timelines;

public class WeatherAttributes {
    public static final EnvironmentAttributeMap RAIN = EnvironmentAttributeMap.builder().modify(EnvironmentAttributes.SKY_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.6f, 0.75f)).modify(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, ARGB.colorFromFloat(1.0f, 0.5f, 0.5f, 0.6f)).modify(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.24f, 0.5f)).modify(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(4.0f, 0.3125f)).modify(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.ALPHA_BLEND, ARGB.color(0.3125f, Timelines.NIGHT_SKY_LIGHT_COLOR)).modify(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(0.24f, 0.3125f)).set(EnvironmentAttributes.STAR_BRIGHTNESS, Float.valueOf(0.0f)).modify(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ColorModifier.MULTIPLY_ARGB, ARGB.colorFromFloat(1.0f, 0.5f, 0.5f, 0.6f)).set(EnvironmentAttributes.BEES_STAY_IN_HIVE, true).build();
    public static final EnvironmentAttributeMap THUNDER = EnvironmentAttributeMap.builder().modify(EnvironmentAttributes.SKY_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.24f, 0.94f)).modify(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, ARGB.colorFromFloat(1.0f, 0.25f, 0.25f, 0.3f)).modify(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.BLEND_TO_GRAY, new ColorModifier.BlendToGray(0.095f, 0.94f)).modify(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(4.0f, 0.52734375f)).modify(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.ALPHA_BLEND, ARGB.color(0.52734375f, Timelines.NIGHT_SKY_LIGHT_COLOR)).modify(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.ALPHA_BLEND, new FloatWithAlpha(0.24f, 0.52734375f)).set(EnvironmentAttributes.STAR_BRIGHTNESS, Float.valueOf(0.0f)).modify(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, ColorModifier.MULTIPLY_ARGB, ARGB.colorFromFloat(1.0f, 0.25f, 0.25f, 0.3f)).set(EnvironmentAttributes.BEES_STAY_IN_HIVE, true).build();
    private static final Set<EnvironmentAttribute<?>> WEATHER_ATTRIBUTES = Sets.union(RAIN.keySet(), THUNDER.keySet());

    public static void addBuiltinLayers(EnvironmentAttributeSystem.Builder system, WeatherAccess weatherAccess) {
        for (EnvironmentAttribute<?> attribute : WEATHER_ATTRIBUTES) {
            WeatherAttributes.addLayer(system, weatherAccess, attribute);
        }
    }

    private static <Value> void addLayer(EnvironmentAttributeSystem.Builder system, WeatherAccess weatherAccess, EnvironmentAttribute<Value> attribute) {
        EnvironmentAttributeMap.Entry rainEntry = RAIN.get(attribute);
        EnvironmentAttributeMap.Entry thunderEntry = THUNDER.get(attribute);
        system.addTimeBasedLayer(attribute, (result, cacheTickId) -> {
            float thunderLevel = weatherAccess.thunderLevel();
            float rainLevel = weatherAccess.rainLevel() - thunderLevel;
            if (rainEntry != null && rainLevel > 0.0f) {
                Object rainValue = rainEntry.applyModifier(result);
                result = attribute.type().stateChangeLerp().apply(rainLevel, result, rainValue);
            }
            if (thunderEntry != null && thunderLevel > 0.0f) {
                Object thunderValue = thunderEntry.applyModifier(result);
                result = attribute.type().stateChangeLerp().apply(thunderLevel, result, thunderValue);
            }
            return result;
        });
    }

    public static interface WeatherAccess {
        public static WeatherAccess from(final Level level) {
            return new WeatherAccess(){

                @Override
                public float rainLevel() {
                    return level.getRainLevel(1.0f);
                }

                @Override
                public float thunderLevel() {
                    return level.getThunderLevel(1.0f);
                }
            };
        }

        public float rainLevel();

        public float thunderLevel();
    }
}

