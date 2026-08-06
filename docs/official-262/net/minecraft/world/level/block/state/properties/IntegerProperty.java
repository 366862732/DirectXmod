/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntImmutableList
 */
package net.minecraft.world.level.block.state.properties;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.world.level.block.state.properties.Property;

public final class IntegerProperty
extends Property<Integer> {
    private final IntImmutableList values;
    private final int min;
    private final int max;

    private IntegerProperty(String name, int min, int max) {
        super(name, Integer.class);
        if (min < 0) {
            throw new IllegalArgumentException("Min value of " + name + " must be 0 or greater");
        }
        if (max <= min) {
            throw new IllegalArgumentException("Max value of " + name + " must be greater than min (" + min + ")");
        }
        this.min = min;
        this.max = max;
        this.values = IntImmutableList.toList((IntStream)IntStream.range(min, max + 1));
    }

    @Override
    public List<Integer> getPossibleValues() {
        return this.values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IntegerProperty) {
            IntegerProperty that = (IntegerProperty)o;
            if (super.equals(o)) {
                return this.values.equals(that.values);
            }
        }
        return false;
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }

    public static IntegerProperty create(String name, int min, int max) {
        return new IntegerProperty(name, min, max);
    }

    @Override
    public Optional<Integer> getValue(String name) {
        try {
            int value = Integer.parseInt(name);
            return value >= this.min && value <= this.max ? Optional.of(value) : Optional.empty();
        }
        catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public String getName(Integer value) {
        return value.toString();
    }

    @Override
    public int getInternalIndex(Integer value) {
        if (value <= this.max) {
            return value - this.min;
        }
        return -1;
    }
}

