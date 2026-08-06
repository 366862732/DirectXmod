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
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record UniformGenerator(NumberProvider min, NumberProvider max) implements NumberProvider
{
    public static final MapCodec<UniformGenerator> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)NumberProviders.CODEC.fieldOf("min").forGetter(UniformGenerator::min), (App)NumberProviders.CODEC.fieldOf("max").forGetter(UniformGenerator::max)).apply((Applicative)i, UniformGenerator::new));

    public MapCodec<UniformGenerator> codec() {
        return MAP_CODEC;
    }

    public static UniformGenerator between(float min, float max) {
        return new UniformGenerator(ConstantValue.exactly(min), ConstantValue.exactly(max));
    }

    @Override
    public int getInt(LootContext context) {
        return Mth.nextInt(context.getRandom(), this.min.getInt(context), this.max.getInt(context));
    }

    @Override
    public float getFloat(LootContext context) {
        return Mth.nextFloat(context.getRandom(), this.min.getFloat(context), this.max.getFloat(context));
    }

    @Override
    public void validate(ValidationContext context) {
        NumberProvider.super.validate(context);
        Validatable.validate(context, "min", this.min);
        Validatable.validate(context, "max", this.max);
    }
}

