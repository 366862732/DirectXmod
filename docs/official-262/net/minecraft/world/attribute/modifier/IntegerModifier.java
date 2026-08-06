/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public interface IntegerModifier<Argument>
extends AttributeModifier<Integer, Argument> {
    public static final IntegerModifier<Integer> ADD = Integer::sum;
    public static final IntegerModifier<Integer> SUBTRACT = (a, b) -> a - b;
    public static final IntegerModifier<Integer> MULTIPLY = (a, b) -> a * b;
    public static final IntegerModifier<Integer> MINIMUM = Math::min;
    public static final IntegerModifier<Integer> MAXIMUM = Math::max;

    @Override
    public Integer apply(Integer var1, Argument var2);

    @FunctionalInterface
    public static interface Simple
    extends IntegerModifier<Integer> {
        @Override
        default public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) {
            return Codec.INT;
        }

        @Override
        default public LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> type) {
            return LerpFunction.ofInteger();
        }
    }
}

