/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.world.food.FoodData;

public record FoodPredicate(MinMaxBounds.Ints level, MinMaxBounds.Doubles saturation) {
    public static final FoodPredicate ANY = new FoodPredicate(MinMaxBounds.Ints.ANY, MinMaxBounds.Doubles.ANY);
    public static final Codec<FoodPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)MinMaxBounds.Ints.CODEC.optionalFieldOf("level", (Object)MinMaxBounds.Ints.ANY).forGetter(FoodPredicate::level), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("saturation", (Object)MinMaxBounds.Doubles.ANY).forGetter(FoodPredicate::saturation)).apply((Applicative)i, FoodPredicate::new));

    public boolean matches(FoodData food) {
        if (!this.level.matches(food.getFoodLevel())) {
            return false;
        }
        return this.saturation.matches(food.getSaturationLevel());
    }

    public static class Builder {
        private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
        private MinMaxBounds.Doubles saturation = MinMaxBounds.Doubles.ANY;

        public Builder withLevel(MinMaxBounds.Ints level) {
            this.level = level;
            return this;
        }

        public Builder withSaturation(MinMaxBounds.Doubles saturation) {
            this.saturation = saturation;
            return this;
        }

        public static Builder food() {
            return new Builder();
        }

        public FoodPredicate build() {
            return new FoodPredicate(this.level, this.saturation);
        }
    }
}

