/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.attribute.modifier.FloatWithAlpha;

public interface FloatModifier<Argument>
extends AttributeModifier<Float, Argument> {
    public static final FloatModifier<FloatWithAlpha> ALPHA_BLEND = new FloatModifier<FloatWithAlpha>(){

        @Override
        public Float apply(Float subject, FloatWithAlpha argument) {
            return Float.valueOf(Mth.lerp(argument.alpha(), subject.floatValue(), argument.value()));
        }

        @Override
        public Codec<FloatWithAlpha> argumentCodec(EnvironmentAttribute<Float> type) {
            return FloatWithAlpha.CODEC;
        }

        @Override
        public LerpFunction<FloatWithAlpha> argumentKeyframeLerp(EnvironmentAttribute<Float> type) {
            return (alpha, from, to) -> new FloatWithAlpha(Mth.lerp(alpha, from.value(), to.value()), Mth.lerp(alpha, from.alpha(), to.alpha()));
        }
    };
    public static final FloatModifier<Float> ADD = Float::sum;
    public static final FloatModifier<Float> SUBTRACT = (a, b) -> Float.valueOf(a.floatValue() - b.floatValue());
    public static final FloatModifier<Float> MULTIPLY = (a, b) -> Float.valueOf(a.floatValue() * b.floatValue());
    public static final FloatModifier<Float> MINIMUM = Math::min;
    public static final FloatModifier<Float> MAXIMUM = Math::max;

    @FunctionalInterface
    public static interface Simple
    extends FloatModifier<Float> {
        @Override
        default public Codec<Float> argumentCodec(EnvironmentAttribute<Float> type) {
            return Codec.FLOAT;
        }

        @Override
        default public LerpFunction<Float> argumentKeyframeLerp(EnvironmentAttribute<Float> type) {
            return LerpFunction.ofFloat();
        }
    }
}

