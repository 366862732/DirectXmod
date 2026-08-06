/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record EnvironmentAttributeCheck<Value>(EnvironmentAttribute<Value> attribute, Value value) implements LootItemCondition
{
    public static final MapCodec<EnvironmentAttributeCheck<?>> MAP_CODEC = EnvironmentAttributes.CODEC.dispatchMap("attribute", EnvironmentAttributeCheck::attribute, EnvironmentAttributeCheck::createCodec);

    private static <Value> MapCodec<EnvironmentAttributeCheck<Value>> createCodec(EnvironmentAttribute<Value> attribute) {
        return attribute.valueCodec().fieldOf("value").xmap(value -> new EnvironmentAttributeCheck<Object>(attribute, value), EnvironmentAttributeCheck::value);
    }

    public MapCodec<EnvironmentAttributeCheck<Value>> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.attribute.isPositional() ? Set.of(LootContextParams.ORIGIN) : Set.of();
    }

    @Override
    public boolean test(LootContext context) {
        Value actualValue = context.getLevel().environmentAttributes().getValue(context, this.attribute);
        return this.value.equals(actualValue);
    }

    public static <Value> LootItemCondition.Builder environmentAttribute(EnvironmentAttribute<Value> attribute, Value value) {
        return () -> new EnvironmentAttributeCheck<Object>(attribute, value);
    }
}

