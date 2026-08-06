/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.attributes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.Nullable;

public class AttributeInstance {
    private final Holder<Attribute> attribute;
    private final Map<AttributeModifier.Operation, Map<Identifier, AttributeModifier>> modifiersByOperation = Maps.newEnumMap(AttributeModifier.Operation.class);
    private final Map<Identifier, AttributeModifier> modifierById = new Object2ObjectArrayMap();
    private final Map<Identifier, AttributeModifier> permanentModifiers = new Object2ObjectArrayMap();
    private double baseValue;
    private boolean dirty = true;
    private double cachedValue;
    private final Consumer<AttributeInstance> onDirty;

    public AttributeInstance(Holder<Attribute> attribute, Consumer<AttributeInstance> onDirty) {
        this.attribute = attribute;
        this.onDirty = onDirty;
        this.baseValue = attribute.value().getDefaultValue();
    }

    public Holder<Attribute> getAttribute() {
        return this.attribute;
    }

    public double getBaseValue() {
        return this.baseValue;
    }

    public void setBaseValue(double baseValue) {
        if (baseValue == this.baseValue) {
            return;
        }
        this.baseValue = baseValue;
        this.setDirty();
    }

    @VisibleForTesting
    Map<Identifier, AttributeModifier> getModifiers(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.computeIfAbsent(operation, key -> new Object2ObjectOpenHashMap());
    }

    public Set<AttributeModifier> getModifiers() {
        return ImmutableSet.copyOf(this.modifierById.values());
    }

    public Set<AttributeModifier> getPermanentModifiers() {
        return ImmutableSet.copyOf(this.permanentModifiers.values());
    }

    public @Nullable AttributeModifier getModifier(Identifier id) {
        return this.modifierById.get(id);
    }

    public boolean hasModifier(Identifier modifier) {
        return this.modifierById.get(modifier) != null;
    }

    private void addModifier(AttributeModifier modifier) {
        AttributeModifier previous = this.modifierById.putIfAbsent(modifier.id(), modifier);
        if (previous != null) {
            throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        }
        this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
        this.setDirty();
    }

    public void addOrUpdateTransientModifier(AttributeModifier modifier) {
        AttributeModifier oldModifier = this.modifierById.put(modifier.id(), modifier);
        if (modifier == oldModifier) {
            return;
        }
        this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
        this.setDirty();
    }

    public void addTransientModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
    }

    public void addOrReplacePermanentModifier(AttributeModifier modifier) {
        this.removeModifier(modifier.id());
        this.addModifier(modifier);
        this.permanentModifiers.put(modifier.id(), modifier);
    }

    public void addPermanentModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
        this.permanentModifiers.put(modifier.id(), modifier);
    }

    public void addPermanentModifiers(Collection<AttributeModifier> modifiers) {
        for (AttributeModifier modifier : modifiers) {
            this.addPermanentModifier(modifier);
        }
    }

    protected void setDirty() {
        this.dirty = true;
        this.onDirty.accept(this);
    }

    public void removeModifier(AttributeModifier modifier) {
        this.removeModifier(modifier.id());
    }

    public boolean removeModifier(Identifier id) {
        AttributeModifier modifier = this.modifierById.remove(id);
        if (modifier == null) {
            return false;
        }
        this.getModifiers(modifier.operation()).remove(id);
        this.permanentModifiers.remove(id);
        this.setDirty();
        return true;
    }

    public void removeModifiers() {
        for (AttributeModifier modifier : this.getModifiers()) {
            this.removeModifier(modifier);
        }
    }

    public double getValue() {
        if (this.dirty) {
            this.cachedValue = this.calculateValue();
            this.dirty = false;
        }
        return this.cachedValue;
    }

    private double calculateValue() {
        double base = this.getBaseValue();
        for (AttributeModifier modifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
            base += modifier.amount();
        }
        double result = base;
        for (AttributeModifier modifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
            result += base * modifier.amount();
        }
        for (AttributeModifier modifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
            result *= 1.0 + modifier.amount();
        }
        return this.attribute.value().sanitizeValue(result);
    }

    private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.getOrDefault(operation, Map.of()).values();
    }

    public void replaceFrom(AttributeInstance other) {
        this.baseValue = other.baseValue;
        this.modifierById.clear();
        this.modifierById.putAll(other.modifierById);
        this.permanentModifiers.clear();
        this.permanentModifiers.putAll(other.permanentModifiers);
        this.modifiersByOperation.clear();
        other.modifiersByOperation.forEach((operation, attributeModifiers) -> this.getModifiers((AttributeModifier.Operation)operation).putAll((Map<Identifier, AttributeModifier>)attributeModifiers));
        this.setDirty();
    }

    public Packed pack() {
        return new Packed(this.attribute, this.baseValue, List.copyOf(this.permanentModifiers.values()));
    }

    public void apply(Packed packed) {
        this.baseValue = packed.baseValue;
        for (AttributeModifier modifier : packed.modifiers) {
            this.modifierById.put(modifier.id(), modifier);
            this.getModifiers(modifier.operation()).put(modifier.id(), modifier);
            this.permanentModifiers.put(modifier.id(), modifier);
        }
        this.setDirty();
    }

    public record Packed(Holder<Attribute> attribute, double baseValue, List<AttributeModifier> modifiers) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("id").forGetter(Packed::attribute), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.DOUBLE, "base", 0.0).forGetter(Packed::baseValue), (App)AttributeModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(Packed::modifiers)).apply((Applicative)i, Packed::new));
        public static final Codec<List<Packed>> LIST_CODEC = CODEC.listOf();
    }
}

