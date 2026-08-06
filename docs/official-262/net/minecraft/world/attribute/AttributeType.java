/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableBiMap
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.attribute;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public record AttributeType<Value>(Codec<Value> valueCodec, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary, Codec<AttributeModifier<Value, ?>> modifierCodec, LerpFunction<Value> keyframeLerp, LerpFunction<Value> stateChangeLerp, LerpFunction<Value> spatialLerp, LerpFunction<Value> partialTickLerp, @Nullable ToFloatFunction<Value> toFloat) {
    public static <Value> AttributeType<Value> ofInterpolated(Codec<Value> valueCodec, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary, LerpFunction<Value> lerp) {
        return AttributeType.ofInterpolated(valueCodec, modifierLibrary, lerp, lerp, null);
    }

    public static <Value> AttributeType<Value> ofInterpolated(Codec<Value> valueCodec, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary, LerpFunction<Value> lerp, LerpFunction<Value> partialTickLerp, @Nullable ToFloatFunction<Value> toFloat) {
        return new AttributeType<Value>(valueCodec, modifierLibrary, AttributeType.createModifierCodec(modifierLibrary), lerp, lerp, lerp, partialTickLerp, toFloat);
    }

    public static <Value> AttributeType<Value> ofNotInterpolated(Codec<Value> valueCodec, Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifierLibrary) {
        return new AttributeType<Value>(valueCodec, modifierLibrary, AttributeType.createModifierCodec(modifierLibrary), LerpFunction.ofStep(1.0f), LerpFunction.ofStep(0.0f), LerpFunction.ofStep(0.5f), LerpFunction.ofStep(0.0f), null);
    }

    public static <Value> AttributeType<Value> ofNotInterpolated(Codec<Value> valueCodec) {
        return AttributeType.ofNotInterpolated(valueCodec, Map.of());
    }

    private static <Value> Codec<AttributeModifier<Value, ?>> createModifierCodec(Map<AttributeModifier.OperationId, AttributeModifier<Value, ?>> modifiers) {
        ImmutableBiMap modifierLookup = ImmutableBiMap.builder().put((Object)AttributeModifier.OperationId.OVERRIDE, AttributeModifier.override()).putAll(modifiers).buildOrThrow();
        return ExtraCodecs.idResolverCodec(AttributeModifier.OperationId.CODEC, arg_0 -> ((ImmutableBiMap)modifierLookup).get(arg_0), arg_0 -> ((ImmutableBiMap)modifierLookup.inverse()).get(arg_0));
    }

    public void checkAllowedModifier(AttributeModifier<Value, ?> modifier) {
        if (modifier != AttributeModifier.override() && !this.modifierLibrary.containsValue(modifier)) {
            throw new IllegalArgumentException("Modifier " + String.valueOf(modifier) + " is not valid for " + String.valueOf(this));
        }
    }

    public float toFloat(Value value) {
        if (this.toFloat == null) {
            throw new IllegalStateException(String.valueOf(value) + " cannot be represented as a float");
        }
        return this.toFloat.applyAsFloat(value);
    }

    @Override
    public String toString() {
        return Util.getRegisteredName(BuiltInRegistries.ATTRIBUTE_TYPE, this);
    }
}

