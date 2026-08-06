/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.enchantment;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;

public interface LevelBasedValue {
    public static final Codec<LevelBasedValue> DISPATCH_CODEC = BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.byNameCodec().dispatch(LevelBasedValue::codec, c -> c);
    public static final Codec<LevelBasedValue> CODEC = Codec.either(Constant.CODEC, DISPATCH_CODEC).xmap(either -> (LevelBasedValue)either.map(l -> l, r -> r), levelBasedValue -> {
        Either either;
        if (levelBasedValue instanceof Constant) {
            Constant constant = (Constant)levelBasedValue;
            either = Either.left((Object)constant);
        } else {
            either = Either.right((Object)levelBasedValue);
        }
        return either;
    });

    public static MapCodec<? extends LevelBasedValue> bootstrap(Registry<MapCodec<? extends LevelBasedValue>> registry) {
        Registry.register(registry, "clamped", Clamped.CODEC);
        Registry.register(registry, "fraction", Fraction.CODEC);
        Registry.register(registry, "levels_squared", LevelsSquared.CODEC);
        Registry.register(registry, "linear", Linear.CODEC);
        Registry.register(registry, "exponent", Exponent.CODEC);
        return Registry.register(registry, "lookup", Lookup.CODEC);
    }

    public static Constant constant(float value) {
        return new Constant(value);
    }

    public static Linear perLevel(float base, float perLevelAboveFirst) {
        return new Linear(base, perLevelAboveFirst);
    }

    public static Linear perLevel(float perLevel) {
        return LevelBasedValue.perLevel(perLevel, perLevel);
    }

    public static Lookup lookup(List<Float> values, LevelBasedValue fallback) {
        return new Lookup(values, fallback);
    }

    public float calculate(int var1);

    public MapCodec<? extends LevelBasedValue> codec();

    public record Clamped(LevelBasedValue value, float min, float max) implements LevelBasedValue
    {
        public static final MapCodec<Clamped> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("value").forGetter(Clamped::value), (App)Codec.FLOAT.fieldOf("min").forGetter(Clamped::min), (App)Codec.FLOAT.fieldOf("max").forGetter(Clamped::max)).apply((Applicative)i, Clamped::new)).validate(u -> {
            if (u.max <= u.min) {
                return DataResult.error(() -> "Max must be larger than min, min: " + u.min + ", max: " + u.max);
            }
            return DataResult.success((Object)u);
        });

        @Override
        public float calculate(int level) {
            return Mth.clamp(this.value.calculate(level), this.min, this.max);
        }

        public MapCodec<Clamped> codec() {
            return CODEC;
        }
    }

    public record Fraction(LevelBasedValue numerator, LevelBasedValue denominator) implements LevelBasedValue
    {
        public static final MapCodec<Fraction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("numerator").forGetter(Fraction::numerator), (App)CODEC.fieldOf("denominator").forGetter(Fraction::denominator)).apply((Applicative)i, Fraction::new));

        @Override
        public float calculate(int level) {
            float denominator = this.denominator.calculate(level);
            if (denominator == 0.0f) {
                return 0.0f;
            }
            return this.numerator.calculate(level) / denominator;
        }

        public MapCodec<Fraction> codec() {
            return CODEC;
        }
    }

    public record LevelsSquared(float added) implements LevelBasedValue
    {
        public static final MapCodec<LevelsSquared> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("added").forGetter(LevelsSquared::added)).apply((Applicative)i, LevelsSquared::new));

        @Override
        public float calculate(int level) {
            return (float)Mth.square(level) + this.added;
        }

        public MapCodec<LevelsSquared> codec() {
            return CODEC;
        }
    }

    public record Linear(float base, float perLevelAboveFirst) implements LevelBasedValue
    {
        public static final MapCodec<Linear> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("base").forGetter(Linear::base), (App)Codec.FLOAT.fieldOf("per_level_above_first").forGetter(Linear::perLevelAboveFirst)).apply((Applicative)i, Linear::new));

        @Override
        public float calculate(int level) {
            return this.base + this.perLevelAboveFirst * (float)(level - 1);
        }

        public MapCodec<Linear> codec() {
            return CODEC;
        }
    }

    public record Exponent(LevelBasedValue base, LevelBasedValue power) implements LevelBasedValue
    {
        public static final MapCodec<Exponent> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("base").forGetter(Exponent::base), (App)CODEC.fieldOf("power").forGetter(Exponent::power)).apply((Applicative)i, Exponent::new));

        @Override
        public float calculate(int level) {
            return (float)Math.pow(this.base.calculate(level), this.power.calculate(level));
        }

        public MapCodec<Exponent> codec() {
            return CODEC;
        }
    }

    public record Lookup(List<Float> values, LevelBasedValue fallback) implements LevelBasedValue
    {
        public static final MapCodec<Lookup> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.listOf().fieldOf("values").forGetter(Lookup::values), (App)CODEC.fieldOf("fallback").forGetter(Lookup::fallback)).apply((Applicative)i, Lookup::new));

        @Override
        public float calculate(int level) {
            return level <= this.values.size() ? this.values.get(level - 1).floatValue() : this.fallback.calculate(level);
        }

        public MapCodec<Lookup> codec() {
            return CODEC;
        }
    }

    public record Constant(float value) implements LevelBasedValue
    {
        public static final Codec<Constant> CODEC = Codec.FLOAT.xmap(Constant::new, Constant::value);
        public static final MapCodec<Constant> TYPED_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("value").forGetter(Constant::value)).apply((Applicative)i, Constant::new));

        @Override
        public float calculate(int level) {
            return this.value;
        }

        public MapCodec<Constant> codec() {
            return TYPED_CODEC;
        }
    }
}

