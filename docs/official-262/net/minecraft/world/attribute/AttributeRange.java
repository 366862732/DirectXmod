/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public interface AttributeRange<Value> {
    public static final AttributeRange<Float> UNIT_FLOAT = AttributeRange.ofFloat(0.0f, 1.0f);
    public static final AttributeRange<Float> NON_NEGATIVE_FLOAT = AttributeRange.ofFloat(0.0f, Float.POSITIVE_INFINITY);

    public static <Value> AttributeRange<Value> any() {
        return new AttributeRange<Value>(){

            @Override
            public DataResult<Value> validate(Value value) {
                return DataResult.success(value);
            }

            @Override
            public Value sanitize(Value value) {
                return value;
            }
        };
    }

    public static AttributeRange<Float> ofFloat(final float minValue, final float maxValue) {
        return new AttributeRange<Float>(){

            @Override
            public DataResult<Float> validate(Float value) {
                if (value.floatValue() >= minValue && value.floatValue() <= maxValue) {
                    return DataResult.success((Object)value);
                }
                return DataResult.error(() -> value + " is not in range [" + minValue + "; " + maxValue + "]");
            }

            @Override
            public Float sanitize(Float value) {
                if (value.floatValue() >= minValue && value.floatValue() <= maxValue) {
                    return value;
                }
                return Float.valueOf(Mth.clamp(value.floatValue(), minValue, maxValue));
            }
        };
    }

    public DataResult<Value> validate(Value var1);

    public Value sanitize(Value var1);
}

