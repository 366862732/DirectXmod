/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public record PropertyValueList(List<Property.Value<?>> values) {
    public static final PropertyValueList EMPTY = new PropertyValueList(List.of());
    private static final Comparator<Property.Value<?>> COMPARE_BY_NAME = Comparator.comparing(p -> p.property().getName());

    public PropertyValueList extend(Property.Value<?> element) {
        return new PropertyValueList(Util.copyAndAdd(this.values, element));
    }

    public PropertyValueList extend(PropertyValueList other) {
        return new PropertyValueList((List<Property.Value<?>>)ImmutableList.builder().addAll(this.values).addAll(other.values).build());
    }

    public static PropertyValueList of(Property.Value<?> ... values) {
        return new PropertyValueList(List.of(values));
    }

    public String getKey() {
        return this.values.stream().sorted(COMPARE_BY_NAME).map(Property.Value::toString).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return this.getKey();
    }
}

