/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record TimeCheck(Holder<WorldClock> clock, Optional<Long> period, IntRange value) implements LootItemCondition
{
    public static final MapCodec<TimeCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)WorldClock.CODEC.fieldOf("clock").forGetter(TimeCheck::clock), (App)Codec.LONG.optionalFieldOf("period").forGetter(TimeCheck::period), (App)IntRange.CODEC.fieldOf("value").forGetter(TimeCheck::value)).apply((Applicative)i, TimeCheck::new));

    public MapCodec<TimeCheck> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        LootItemCondition.super.validate(context);
        Validatable.validate(context, "value", this.value);
    }

    @Override
    public boolean test(LootContext context) {
        ServerLevel level = context.getLevel();
        long time = level.clockManager().getTotalTicks(this.clock);
        if (this.period.isPresent()) {
            time %= this.period.get().longValue();
        }
        return this.value.test(context, (int)time);
    }

    public static Builder time(Holder<WorldClock> clock, IntRange value) {
        return new Builder(clock, value);
    }

    public static class Builder
    implements LootItemCondition.Builder {
        private final Holder<WorldClock> clock;
        private Optional<Long> period = Optional.empty();
        private final IntRange value;

        public Builder(Holder<WorldClock> clock, IntRange value) {
            this.clock = clock;
            this.value = value;
        }

        public Builder setPeriod(long period) {
            this.period = Optional.of(period);
            return this;
        }

        @Override
        public TimeCheck build() {
            return new TimeCheck(this.clock, this.period, this.value);
        }
    }
}

