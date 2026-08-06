/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jspecify.annotations.Nullable;

public class IntRange
implements LootContextUser {
    private static final Codec<IntRange> RECORD_CODEC = RecordCodecBuilder.create(i -> i.group((App)NumberProviders.CODEC.optionalFieldOf("min").forGetter(r -> Optional.ofNullable(r.min)), (App)NumberProviders.CODEC.optionalFieldOf("max").forGetter(r -> Optional.ofNullable(r.max))).apply((Applicative)i, IntRange::new));
    public static final Codec<IntRange> CODEC = Codec.either((Codec)Codec.INT, RECORD_CODEC).xmap(e -> (IntRange)e.map(IntRange::exact, Function.identity()), range -> {
        OptionalInt exact = range.unpackExact();
        if (exact.isPresent()) {
            return Either.left((Object)exact.getAsInt());
        }
        return Either.right((Object)range);
    });
    private final @Nullable NumberProvider min;
    private final @Nullable NumberProvider max;
    private final IntLimiter limiter;
    private final IntChecker predicate;

    @Override
    public void validate(ValidationContext context) {
        LootContextUser.super.validate(context);
        if (this.min != null) {
            Validatable.validate(context, "min", this.min);
        }
        if (this.max != null) {
            Validatable.validate(context, "max", this.max);
        }
    }

    private IntRange(Optional<NumberProvider> min, Optional<NumberProvider> max) {
        this((NumberProvider)min.orElse(null), (NumberProvider)max.orElse(null));
    }

    private IntRange(@Nullable NumberProvider min, @Nullable NumberProvider max) {
        this.min = min;
        this.max = max;
        if (min == null) {
            if (max == null) {
                this.limiter = (context, value) -> value;
                this.predicate = (context, value) -> true;
            } else {
                this.limiter = (context, value) -> Math.min(max.getInt(context), value);
                this.predicate = (context, value) -> value <= max.getInt(context);
            }
        } else if (max == null) {
            this.limiter = (context, value) -> Math.max(min.getInt(context), value);
            this.predicate = (context, value) -> value >= min.getInt(context);
        } else {
            this.limiter = (context, value) -> Mth.clamp(value, min.getInt(context), max.getInt(context));
            this.predicate = (context, value) -> value >= min.getInt(context) && value <= max.getInt(context);
        }
    }

    public static IntRange exact(int value) {
        ConstantValue c = ConstantValue.exactly(value);
        return new IntRange(Optional.of(c), Optional.of(c));
    }

    public static IntRange range(int min, int max) {
        return new IntRange(Optional.of(ConstantValue.exactly(min)), Optional.of(ConstantValue.exactly(max)));
    }

    public static IntRange lowerBound(int value) {
        return new IntRange(Optional.of(ConstantValue.exactly(value)), Optional.empty());
    }

    public static IntRange upperBound(int value) {
        return new IntRange(Optional.empty(), Optional.of(ConstantValue.exactly(value)));
    }

    public int clamp(LootContext context, int value) {
        return this.limiter.apply(context, value);
    }

    public boolean test(LootContext context, int value) {
        return this.predicate.test(context, value);
    }

    private OptionalInt unpackExact() {
        ConstantValue constant;
        NumberProvider numberProvider;
        if (Objects.equals(this.min, this.max) && (numberProvider = this.min) instanceof ConstantValue && Math.floor((constant = (ConstantValue)numberProvider).value()) == (double)constant.value()) {
            return OptionalInt.of((int)constant.value());
        }
        return OptionalInt.empty();
    }

    @FunctionalInterface
    private static interface IntLimiter {
        public int apply(LootContext var1, int var2);
    }

    @FunctionalInterface
    private static interface IntChecker {
        public boolean test(LootContext var1, int var2);
    }
}

