/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record ValueCheckCondition(NumberProvider value, IntRange range) implements LootItemCondition
{
    public static final MapCodec<ValueCheckCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)NumberProviders.CODEC.fieldOf("value").forGetter(ValueCheckCondition::value), (App)IntRange.CODEC.fieldOf("range").forGetter(ValueCheckCondition::range)).apply((Applicative)i, ValueCheckCondition::new));

    public MapCodec<ValueCheckCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        LootItemCondition.super.validate(context);
        Validatable.validate(context, "value", this.value);
        Validatable.validate(context, "range", this.range);
    }

    @Override
    public boolean test(LootContext context) {
        return this.range.test(context, this.value.getInt(context));
    }

    public static LootItemCondition.Builder hasValue(NumberProvider value, IntRange range) {
        return () -> new ValueCheckCondition(value, range);
    }
}

