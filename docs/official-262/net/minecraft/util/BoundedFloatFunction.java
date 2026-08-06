/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.Objects;
import java.util.function.Function;

public interface BoundedFloatFunction<C> {
    public static final BoundedFloatFunction<Float> IDENTITY = new BoundedFloatFunction<Float>(){

        @Override
        public float apply(Float value) {
            return value.floatValue();
        }

        @Override
        public float minValue() {
            return Float.NEGATIVE_INFINITY;
        }

        @Override
        public float maxValue() {
            return Float.POSITIVE_INFINITY;
        }
    };

    public float apply(C var1);

    public float minValue();

    public float maxValue();

    public static <C> BoundedFloatFunction<C> constant(final float value) {
        return new BoundedFloatFunction<C>(){

            @Override
            public float apply(C c) {
                return value;
            }

            @Override
            public float minValue() {
                return value;
            }

            @Override
            public float maxValue() {
                return value;
            }
        };
    }

    default public <C2> BoundedFloatFunction<C2> comap(final Function<C2, C> function) {
        final BoundedFloatFunction outer = this;
        return new BoundedFloatFunction<C2>(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public float apply(C2 c2) {
                return outer.apply(function.apply(c2));
            }

            @Override
            public float minValue() {
                return outer.minValue();
            }

            @Override
            public float maxValue() {
                return outer.maxValue();
            }
        };
    }
}

