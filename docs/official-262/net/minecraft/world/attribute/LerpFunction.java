/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.attribute;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public interface LerpFunction<T> {
    public static final LerpFunction<?> CONSTANT = LerpFunction.ofStep(1.0f);

    public static LerpFunction<Float> ofFloat() {
        return Mth::lerp;
    }

    public static LerpFunction<Integer> ofInteger() {
        return Mth::lerpInt;
    }

    public static LerpFunction<Float> ofDegrees(float maxDelta) {
        return (alpha, from, to) -> {
            float delta = Mth.wrapDegrees(to.floatValue() - from.floatValue());
            if (Math.abs(delta) >= maxDelta) {
                return to;
            }
            return Float.valueOf(from.floatValue() + alpha * delta);
        };
    }

    public static <T> LerpFunction<T> ofConstant() {
        return CONSTANT;
    }

    public static <T> LerpFunction<T> ofStep(float threshold) {
        return (alpha, from, to) -> alpha >= threshold ? to : from;
    }

    public static LerpFunction<Integer> ofColor() {
        return ARGB::srgbLerp;
    }

    public T apply(float var1, T var2, T var3);
}

