/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt;

import java.util.Optional;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.nbt.ShortTag;

public sealed interface NumericTag
extends PrimitiveTag
permits ByteTag, ShortTag, IntTag, LongTag, FloatTag, DoubleTag {
    public byte byteValue();

    public short shortValue();

    public int intValue();

    public long longValue();

    public float floatValue();

    public double doubleValue();

    public Number box();

    @Override
    default public Optional<Number> asNumber() {
        return Optional.of(this.box());
    }

    @Override
    default public Optional<Byte> asByte() {
        return Optional.of(this.byteValue());
    }

    @Override
    default public Optional<Short> asShort() {
        return Optional.of(this.shortValue());
    }

    @Override
    default public Optional<Integer> asInt() {
        return Optional.of(this.intValue());
    }

    @Override
    default public Optional<Long> asLong() {
        return Optional.of(this.longValue());
    }

    @Override
    default public Optional<Float> asFloat() {
        return Optional.of(Float.valueOf(this.floatValue()));
    }

    @Override
    default public Optional<Double> asDouble() {
        return Optional.of(this.doubleValue());
    }

    @Override
    default public Optional<Boolean> asBoolean() {
        return Optional.of(this.byteValue() != 0);
    }
}

