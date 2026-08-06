/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 */
package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.Property;

public final class EnumProperty<T extends Enum<T>>
extends Property<T> {
    private final List<T> values;
    private final Map<String, T> names;
    private final int[] ordinalToIndex;

    private EnumProperty(String name, Class<T> clazz, List<T> values) {
        super(name, clazz);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Trying to make empty EnumProperty '" + name + "'");
        }
        this.values = List.copyOf(values);
        Enum[] allEnumValues = (Enum[])clazz.getEnumConstants();
        this.ordinalToIndex = new int[allEnumValues.length];
        for (Enum value : allEnumValues) {
            this.ordinalToIndex[value.ordinal()] = values.indexOf(value);
        }
        ImmutableMap.Builder names = ImmutableMap.builder();
        for (Enum value : values) {
            String key = ((StringRepresentable)((Object)value)).getSerializedName();
            names.put((Object)key, (Object)value);
        }
        this.names = names.buildOrThrow();
    }

    @Override
    public List<T> getPossibleValues() {
        return this.values;
    }

    @Override
    public Optional<T> getValue(String name) {
        return Optional.ofNullable((Enum)this.names.get(name));
    }

    @Override
    public String getName(T value) {
        return ((StringRepresentable)value).getSerializedName();
    }

    @Override
    public int getInternalIndex(T value) {
        return this.ordinalToIndex[((Enum)value).ordinal()];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof EnumProperty) {
            EnumProperty that = (EnumProperty)o;
            if (super.equals(o)) {
                return this.values.equals(that.values);
            }
        }
        return false;
    }

    @Override
    public int generateHashCode() {
        int result = super.generateHashCode();
        result = 31 * result + this.values.hashCode();
        return result;
    }

    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz) {
        return EnumProperty.create(name, clazz, (T t) -> true);
    }

    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        return EnumProperty.create(name, clazz, Arrays.stream((Enum[])clazz.getEnumConstants()).filter(filter).collect(Collectors.toList()));
    }

    @SafeVarargs
    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz, T ... values) {
        return EnumProperty.create(name, clazz, List.of(values));
    }

    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz, List<T> values) {
        return new EnumProperty<T>(name, clazz, values);
    }
}

