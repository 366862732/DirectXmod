/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public record EnvironmentAttributeValue(EnvironmentAttribute<?> attribute) implements NumberProvider
{
    private static final Codec<EnvironmentAttribute<?>> ATTRIBUTE_CODEC = EnvironmentAttributes.CODEC.validate(attribute -> {
        if (attribute.type().toFloat() == null) {
            return DataResult.error(() -> String.valueOf(attribute) + " cannot be converted to a number");
        }
        return DataResult.success((Object)attribute);
    });
    public static final MapCodec<EnvironmentAttributeValue> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ATTRIBUTE_CODEC.fieldOf("attribute").forGetter(EnvironmentAttributeValue::attribute)).apply((Applicative)i, EnvironmentAttributeValue::new));

    public MapCodec<EnvironmentAttributeValue> codec() {
        return MAP_CODEC;
    }

    @Override
    public float getFloat(LootContext context) {
        return EnvironmentAttributeValue.getAsFloat(context, this.attribute);
    }

    private static <Value> float getAsFloat(LootContext context, EnvironmentAttribute<Value> attribute) {
        Value value = context.getLevel().environmentAttributes().getValue(context, attribute);
        return attribute.type().toFloat(value);
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.attribute.isPositional() ? Set.of(LootContextParams.ORIGIN) : Set.of();
    }

    public static EnvironmentAttributeValue forEnvironmentAttribute(EnvironmentAttribute<?> attribute) {
        return new EnvironmentAttributeValue(attribute);
    }
}

