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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider
{
    public static final MapCodec<BinomialDistributionGenerator> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n), (App)NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)).apply((Applicative)i, BinomialDistributionGenerator::new));

    public MapCodec<BinomialDistributionGenerator> codec() {
        return MAP_CODEC;
    }

    @Override
    public int getInt(LootContext context) {
        int n = this.n.getInt(context);
        float p = this.p.getFloat(context);
        RandomSource random = context.getRandom();
        int result = 0;
        for (int i = 0; i < n; ++i) {
            if (!(random.nextFloat() < p)) continue;
            ++result;
        }
        return result;
    }

    @Override
    public float getFloat(LootContext context) {
        return this.getInt(context);
    }

    public static BinomialDistributionGenerator binomial(int n, float p) {
        return new BinomialDistributionGenerator(ConstantValue.exactly(n), ConstantValue.exactly(p));
    }

    @Override
    public void validate(ValidationContext context) {
        NumberProvider.super.validate(context);
        Validatable.validate(context, "n", this.n);
        Validatable.validate(context, "p", this.p);
    }
}

