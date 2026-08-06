/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record Sum(List<NumberProvider> summands) implements NumberProvider
{
    public static final MapCodec<Sum> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)NumberProviders.CODEC.listOf().fieldOf("summands").forGetter(Sum::summands)).apply((Applicative)i, Sum::new));

    public static Sum sum(NumberProvider ... summands) {
        return new Sum(List.of(summands));
    }

    public MapCodec<Sum> codec() {
        return MAP_CODEC;
    }

    @Override
    public int getInt(LootContext context) {
        float value = 0.0f;
        for (NumberProvider provider : this.summands) {
            value += provider.getFloat(context);
        }
        return Mth.floor(value);
    }

    @Override
    public float getFloat(LootContext context) {
        float value = 0.0f;
        for (NumberProvider provider : this.summands) {
            value += provider.getFloat(context);
        }
        return value;
    }

    @Override
    public void validate(ValidationContext context) {
        NumberProvider.super.validate(context);
        Validatable.validate(context, "summands", this.summands);
    }
}

