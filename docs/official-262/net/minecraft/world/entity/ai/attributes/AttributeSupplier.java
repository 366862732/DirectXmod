/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jspecify.annotations.Nullable;

public class AttributeSupplier {
    private final Map<Holder<Attribute>, AttributeInstance> instances;

    private AttributeSupplier(Map<Holder<Attribute>, AttributeInstance> instances) {
        this.instances = instances;
    }

    private AttributeInstance getAttributeInstance(Holder<Attribute> attribute) {
        AttributeInstance instance = this.instances.get(attribute);
        if (instance == null) {
            throw new IllegalArgumentException("Can't find attribute " + attribute.getRegisteredName());
        }
        return instance;
    }

    public double getValue(Holder<Attribute> attribute) {
        return this.getAttributeInstance(attribute).getValue();
    }

    public double getBaseValue(Holder<Attribute> attribute) {
        return this.getAttributeInstance(attribute).getBaseValue();
    }

    public double getModifierValue(Holder<Attribute> attribute, Identifier id) {
        AttributeModifier modifier = this.getAttributeInstance(attribute).getModifier(id);
        if (modifier == null) {
            throw new IllegalArgumentException("Can't find modifier " + String.valueOf(id) + " on attribute " + attribute.getRegisteredName());
        }
        return modifier.amount();
    }

    public @Nullable AttributeInstance createInstance(Consumer<AttributeInstance> onDirty, Holder<Attribute> attribute) {
        AttributeInstance template = this.instances.get(attribute);
        if (template == null) {
            return null;
        }
        AttributeInstance result = new AttributeInstance(attribute, onDirty);
        result.replaceFrom(template);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasAttribute(Holder<Attribute> attribute) {
        return this.instances.containsKey(attribute);
    }

    public boolean hasModifier(Holder<Attribute> attribute, Identifier modifier) {
        AttributeInstance attributeInstance = this.instances.get(attribute);
        return attributeInstance != null && attributeInstance.getModifier(modifier) != null;
    }

    public static class Builder {
        private final ImmutableMap.Builder<Holder<Attribute>, AttributeInstance> builder = ImmutableMap.builder();
        private boolean instanceFrozen;

        private AttributeInstance create(Holder<Attribute> attribute) {
            AttributeInstance result = new AttributeInstance(attribute, attributeInstance -> {
                if (this.instanceFrozen) {
                    throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + attribute.getRegisteredName());
                }
            });
            this.builder.put(attribute, (Object)result);
            return result;
        }

        public Builder add(Holder<Attribute> attribute) {
            this.create(attribute);
            return this;
        }

        public Builder add(Holder<Attribute> attribute, double baseValue) {
            AttributeInstance result = this.create(attribute);
            result.setBaseValue(baseValue);
            return this;
        }

        public AttributeSupplier build() {
            this.instanceFrozen = true;
            return new AttributeSupplier((Map<Holder<Attribute>, AttributeInstance>)this.builder.buildKeepingLast());
        }
    }
}

