/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.client.renderer.block.dispatch.multipart.KeyValueCondition;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public class ConditionBuilder {
    private final ImmutableMap.Builder<String, KeyValueCondition.Terms> terms = ImmutableMap.builder();

    private <T extends Comparable<T>> void putValue(Property<T> property, KeyValueCondition.Terms term) {
        this.terms.put((Object)property.getName(), (Object)term);
    }

    public final <T extends Comparable<T>> ConditionBuilder term(Property<T> property, T value) {
        this.putValue(property, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(property.getName(value), false))));
        return this;
    }

    @SafeVarargs
    public final <T extends Comparable<T>> ConditionBuilder term(Property<T> property, T value, T ... values) {
        List<KeyValueCondition.Term> terms = Stream.concat(Stream.of(value), Stream.of(values)).map(property::getName).sorted().distinct().map(v -> new KeyValueCondition.Term((String)v, false)).toList();
        this.putValue(property, new KeyValueCondition.Terms(terms));
        return this;
    }

    public final <T extends Comparable<T>> ConditionBuilder negatedTerm(Property<T> property, T value) {
        this.putValue(property, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(property.getName(value), true))));
        return this;
    }

    public Condition build() {
        return new KeyValueCondition((Map<String, KeyValueCondition.Terms>)this.terms.buildOrThrow());
    }
}

