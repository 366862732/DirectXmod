/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Comparators
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.doubles.DoubleArrayList
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen;

import com.google.common.collect.Comparators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.slf4j.Logger;

public final class DensityFunctions {
    private static final Codec<DensityFunction> CODEC = BuiltInRegistries.DENSITY_FUNCTION_TYPE.byNameCodec().dispatch(function -> function.codec().codec(), Function.identity());
    static final double MAX_REASONABLE_NOISE_VALUE = 1000000.0;
    private static final Codec<Double> NOISE_VALUE_CODEC = Codec.doubleRange((double)-1000000.0, (double)1000000.0);
    public static final Codec<DensityFunction> DIRECT_CODEC = Codec.either(NOISE_VALUE_CODEC, CODEC).xmap(either -> (DensityFunction)either.map(DensityFunctions::constant, Function.identity()), function -> {
        if (function instanceof Constant) {
            Constant constant = (Constant)function;
            return Either.left((Object)constant.value());
        }
        return Either.right((Object)function);
    });

    public static MapCodec<? extends DensityFunction> bootstrap(Registry<MapCodec<? extends DensityFunction>> registry) {
        DensityFunctions.register(registry, "blend_alpha", BlendAlpha.CODEC);
        DensityFunctions.register(registry, "blend_offset", BlendOffset.CODEC);
        DensityFunctions.register(registry, "beardifier", BeardifierMarker.CODEC);
        DensityFunctions.register(registry, "old_blended_noise", BlendedNoise.CODEC);
        for (Marker.Type type : Marker.Type.values()) {
            DensityFunctions.register(registry, type.getSerializedName(), type.codec);
        }
        DensityFunctions.register(registry, "noise", Noise.CODEC);
        DensityFunctions.register(registry, "end_islands", EndIslandDensityFunction.CODEC);
        DensityFunctions.register(registry, "shifted_noise", ShiftedNoise.CODEC);
        DensityFunctions.register(registry, "range_choice", RangeChoice.CODEC);
        DensityFunctions.register(registry, "interval_select", IntervalSelect.CODEC);
        DensityFunctions.register(registry, "shift_a", ShiftA.CODEC);
        DensityFunctions.register(registry, "shift_b", ShiftB.CODEC);
        DensityFunctions.register(registry, "shift", Shift.CODEC);
        DensityFunctions.register(registry, "clamp", Clamp.CODEC);
        for (Enum enum_ : Mapped.Type.values()) {
            DensityFunctions.register(registry, ((Mapped.Type)enum_).getSerializedName(), ((Mapped.Type)enum_).codec);
        }
        for (Enum enum_ : TwoArgumentSimpleFunction.Type.values()) {
            DensityFunctions.register(registry, ((TwoArgumentSimpleFunction.Type)enum_).getSerializedName(), ((TwoArgumentSimpleFunction.Type)enum_).codec);
        }
        DensityFunctions.register(registry, "spline", Spline.CODEC);
        DensityFunctions.register(registry, "constant", Constant.CODEC);
        DensityFunctions.register(registry, "y_clamped_gradient", YClampedGradient.CODEC);
        return DensityFunctions.register(registry, "find_top_surface", FindTopSurface.CODEC);
    }

    private static MapCodec<? extends DensityFunction> register(Registry<MapCodec<? extends DensityFunction>> registry, String name, KeyDispatchDataCodec<? extends DensityFunction> codec) {
        return Registry.register(registry, name, codec.codec());
    }

    private static <A, O> KeyDispatchDataCodec<O> singleArgumentCodec(Codec<A> argumentCodec, Function<A, O> constructor, Function<O, A> getter) {
        return KeyDispatchDataCodec.of(argumentCodec.fieldOf("argument").xmap(constructor, getter));
    }

    private static <O> KeyDispatchDataCodec<O> singleFunctionArgumentCodec(Function<DensityFunction, O> constructor, Function<O, DensityFunction> getter) {
        return DensityFunctions.singleArgumentCodec(DensityFunction.CODEC, constructor, getter);
    }

    private static <O> KeyDispatchDataCodec<O> doubleFunctionArgumentCodec(BiFunction<DensityFunction, DensityFunction, O> constructor, Function<O, DensityFunction> firstArgumentGetter, Function<O, DensityFunction> secondArgumentGetter) {
        return KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.CODEC.fieldOf("argument1").forGetter(firstArgumentGetter), (App)DensityFunction.CODEC.fieldOf("argument2").forGetter(secondArgumentGetter)).apply((Applicative)i, constructor)));
    }

    private static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> dataCodec) {
        return KeyDispatchDataCodec.of(dataCodec);
    }

    private DensityFunctions() {
    }

    public static DensityFunction interpolated(DensityFunction function) {
        return new Marker(Marker.Type.Interpolated, function);
    }

    public static DensityFunction flatCache(DensityFunction function) {
        return new Marker(Marker.Type.FlatCache, function);
    }

    public static DensityFunction cache2d(DensityFunction function) {
        return new Marker(Marker.Type.Cache2D, function);
    }

    public static DensityFunction cacheOnce(DensityFunction function) {
        return new Marker(Marker.Type.CacheOnce, function);
    }

    public static DensityFunction cacheAllInCell(DensityFunction function) {
        return new Marker(Marker.Type.CacheAllInCell, function);
    }

    public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> noiseData, @Deprecated double xzScale, double yScale, double minTarget, double maxTarget) {
        return DensityFunctions.mapFromUnitTo(new Noise(new DensityFunction.NoiseHolder(noiseData), xzScale, yScale), minTarget, maxTarget);
    }

    public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> noiseData, double yScale, double minTarget, double maxTarget) {
        return DensityFunctions.mappedNoise(noiseData, 1.0, yScale, minTarget, maxTarget);
    }

    public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> noiseData, double minTarget, double maxTarget) {
        return DensityFunctions.mappedNoise(noiseData, 1.0, 1.0, minTarget, maxTarget);
    }

    public static DensityFunction shiftedNoise2d(DensityFunction shiftX, DensityFunction shiftZ, double xzScale, Holder<NormalNoise.NoiseParameters> noiseData) {
        return new ShiftedNoise(shiftX, DensityFunctions.zero(), shiftZ, xzScale, 0.0, new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> noiseData) {
        return DensityFunctions.noise(noiseData, 1.0, 1.0);
    }

    public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> noiseData, double xzScale, double yScale) {
        return new Noise(new DensityFunction.NoiseHolder(noiseData), xzScale, yScale);
    }

    public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> noiseData, double yScale) {
        return DensityFunctions.noise(noiseData, 1.0, yScale);
    }

    public static DensityFunction rangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange) {
        return new RangeChoice(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange);
    }

    public static DensityFunction intervalSelect(DensityFunction input, DoubleList thresholds, List<DensityFunction> functions) {
        return new IntervalSelect(input, thresholds, functions);
    }

    public static DensityFunction shiftA(Holder<NormalNoise.NoiseParameters> noiseData) {
        return new ShiftA(new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction shiftB(Holder<NormalNoise.NoiseParameters> noiseData) {
        return new ShiftB(new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction shift(Holder<NormalNoise.NoiseParameters> noiseData) {
        return new Shift(new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction blendDensity(DensityFunction input) {
        return new Marker(Marker.Type.BlendDensity, input);
    }

    public static DensityFunction endIslands(long seed) {
        return new EndIslandDensityFunction(seed);
    }

    public static DensityFunction add(DensityFunction f1, DensityFunction f2) {
        return TwoArgumentSimpleFunction.create(TwoArgumentSimpleFunction.Type.ADD, f1, f2);
    }

    public static DensityFunction mul(DensityFunction f1, DensityFunction f2) {
        return TwoArgumentSimpleFunction.create(TwoArgumentSimpleFunction.Type.MUL, f1, f2);
    }

    public static DensityFunction min(DensityFunction f1, DensityFunction f2) {
        return TwoArgumentSimpleFunction.create(TwoArgumentSimpleFunction.Type.MIN, f1, f2);
    }

    public static DensityFunction max(DensityFunction f1, DensityFunction f2) {
        return TwoArgumentSimpleFunction.create(TwoArgumentSimpleFunction.Type.MAX, f1, f2);
    }

    public static DensityFunction spline(CubicSpline<Spline.Coordinate> spline) {
        return new Spline(spline);
    }

    public static DensityFunction zero() {
        return Constant.ZERO;
    }

    public static DensityFunction constant(double value) {
        return new Constant(value);
    }

    public static DensityFunction yClampedGradient(int fromY, int toY, double fromValue, double toValue) {
        return new YClampedGradient(fromY, toY, fromValue, toValue);
    }

    public static DensityFunction map(DensityFunction function, Mapped.Type type) {
        return Mapped.create(type, function);
    }

    private static DensityFunction mapFromUnitTo(DensityFunction function, double min, double max) {
        double middle = (min + max) * 0.5;
        double factor = (max - min) * 0.5;
        return DensityFunctions.add(DensityFunctions.constant(middle), DensityFunctions.mul(DensityFunctions.constant(factor), function));
    }

    public static DensityFunction blendAlpha() {
        return BlendAlpha.INSTANCE;
    }

    public static DensityFunction blendOffset() {
        return BlendOffset.INSTANCE;
    }

    public static DensityFunction lerp(DensityFunction alpha, DensityFunction first, DensityFunction second) {
        if (first instanceof Constant) {
            Constant constant = (Constant)first;
            return DensityFunctions.lerp(alpha, constant.value, second);
        }
        DensityFunction alphaCached = DensityFunctions.cacheOnce(alpha);
        DensityFunction oneMinusAlpha = DensityFunctions.add(DensityFunctions.mul(alphaCached, DensityFunctions.constant(-1.0)), DensityFunctions.constant(1.0));
        return DensityFunctions.add(DensityFunctions.mul(first, oneMinusAlpha), DensityFunctions.mul(second, alphaCached));
    }

    public static DensityFunction lerp(DensityFunction factor, double first, DensityFunction second) {
        return DensityFunctions.add(DensityFunctions.mul(factor, DensityFunctions.add(second, DensityFunctions.constant(-first))), DensityFunctions.constant(first));
    }

    public static DensityFunction findTopSurface(DensityFunction density, DensityFunction upperBound, int lowerBound, int stepSize) {
        return new FindTopSurface(density, upperBound, lowerBound, stepSize);
    }

    static enum BlendAlpha implements DensityFunction.SimpleFunction
    {
        INSTANCE;

        public static final KeyDispatchDataCodec<DensityFunction> CODEC;

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 1.0;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, 1.0);
        }

        @Override
        public double minValue() {
            return 1.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        static {
            CODEC = KeyDispatchDataCodec.of(MapCodec.unit((Object)INSTANCE));
        }
    }

    static enum BlendOffset implements DensityFunction.SimpleFunction
    {
        INSTANCE;

        public static final KeyDispatchDataCodec<DensityFunction> CODEC;

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, 0.0);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        static {
            CODEC = KeyDispatchDataCodec.of(MapCodec.unit((Object)INSTANCE));
        }
    }

    static enum BeardifierMarker implements BeardifierOrMarker
    {
        INSTANCE;


        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, 0.0);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }
    }

    record Marker(Type type, DensityFunction wrapped) implements MarkerOrMarked
    {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.wrapped.compute(context);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.wrapped.fillArray(output, contextProvider);
        }

        @Override
        public double minValue() {
            if (this.type == Type.BlendDensity) {
                return Double.NEGATIVE_INFINITY;
            }
            return this.wrapped.minValue();
        }

        @Override
        public double maxValue() {
            if (this.type == Type.BlendDensity) {
                return Double.POSITIVE_INFINITY;
            }
            return this.wrapped.maxValue();
        }

        public static enum Type implements StringRepresentable
        {
            Interpolated("interpolated"),
            FlatCache("flat_cache"),
            Cache2D("cache_2d"),
            CacheOnce("cache_once"),
            CacheAllInCell("cache_all_in_cell"),
            BlendDensity("blend_density");

            private final String name;
            private final KeyDispatchDataCodec<MarkerOrMarked> codec = DensityFunctions.singleFunctionArgumentCodec(input -> new Marker(this, (DensityFunction)input), MarkerOrMarked::wrapped);

            private Type(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    protected record Noise(DensityFunction.NoiseHolder noise, @Deprecated double xzScale, double yScale) implements DensityFunction
    {
        public static final MapCodec<Noise> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(Noise::noise), (App)Codec.DOUBLE.fieldOf("xz_scale").forGetter(Noise::xzScale), (App)Codec.DOUBLE.fieldOf("y_scale").forGetter(Noise::yScale)).apply((Applicative)i, Noise::new));
        public static final KeyDispatchDataCodec<Noise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.noise.getValue((double)context.blockX() * this.xzScale, (double)context.blockY() * this.yScale, (double)context.blockZ() * this.xzScale);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new Noise(visitor.visitNoise(this.noise), this.xzScale, this.yScale);
        }

        @Override
        public double minValue() {
            return -this.maxValue();
        }

        @Override
        public double maxValue() {
            return this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static final class EndIslandDensityFunction
    implements DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<EndIslandDensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit((Object)new EndIslandDensityFunction(0L)));
        private static final float ISLAND_THRESHOLD = -0.9f;
        private final SimplexNoise islandNoise;

        public EndIslandDensityFunction(long seed) {
            LegacyRandomSource islandRandom = new LegacyRandomSource(seed);
            islandRandom.consumeCount(17292);
            this.islandNoise = new SimplexNoise(islandRandom);
        }

        private static float getHeightValue(SimplexNoise islandNoise, int sectionX, int sectionZ) {
            int chunkX = sectionX / 2;
            int chunkZ = sectionZ / 2;
            int subSectionX = sectionX % 2;
            int subSectionZ = sectionZ % 2;
            float doffs = 100.0f - Mth.sqrt(sectionX * sectionX + sectionZ * sectionZ) * 8.0f;
            doffs = Mth.clamp(doffs, -100.0f, 80.0f);
            for (int xo = -12; xo <= 12; ++xo) {
                for (int zo = -12; zo <= 12; ++zo) {
                    long totalChunkX = chunkX + xo;
                    long totalChunkZ = chunkZ + zo;
                    if (totalChunkX * totalChunkX + totalChunkZ * totalChunkZ <= 4096L || !(islandNoise.getValue(totalChunkX, totalChunkZ) < (double)-0.9f)) continue;
                    float islandSize = (Mth.abs(totalChunkX) * 3439.0f + Mth.abs(totalChunkZ) * 147.0f) % 13.0f + 9.0f;
                    float xd = subSectionX - xo * 2;
                    float zd = subSectionZ - zo * 2;
                    float newDoffs = 100.0f - Mth.sqrt(xd * xd + zd * zd) * islandSize;
                    newDoffs = Mth.clamp(newDoffs, -100.0f, 80.0f);
                    doffs = Math.max(doffs, newDoffs);
                }
            }
            return doffs;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return ((double)EndIslandDensityFunction.getHeightValue(this.islandNoise, context.blockX() / 8, context.blockZ() / 8) - 8.0) / 128.0;
        }

        @Override
        public double minValue() {
            return -0.84375;
        }

        @Override
        public double maxValue() {
            return 0.5625;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record ShiftedNoise(DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise) implements DensityFunction
    {
        private static final MapCodec<ShiftedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.CODEC.fieldOf("shift_x").forGetter(ShiftedNoise::shiftX), (App)DensityFunction.CODEC.fieldOf("shift_y").forGetter(ShiftedNoise::shiftY), (App)DensityFunction.CODEC.fieldOf("shift_z").forGetter(ShiftedNoise::shiftZ), (App)Codec.DOUBLE.fieldOf("xz_scale").forGetter(ShiftedNoise::xzScale), (App)Codec.DOUBLE.fieldOf("y_scale").forGetter(ShiftedNoise::yScale), (App)DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(ShiftedNoise::noise)).apply((Applicative)i, ShiftedNoise::new));
        public static final KeyDispatchDataCodec<ShiftedNoise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double x = (double)context.blockX() * this.xzScale + this.shiftX.compute(context);
            double y = (double)context.blockY() * this.yScale + this.shiftY.compute(context);
            double z = (double)context.blockZ() * this.xzScale + this.shiftZ.compute(context);
            return this.noise.getValue(x, y, z);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new ShiftedNoise(visitor.apply(this.shiftX), visitor.apply(this.shiftY), visitor.apply(this.shiftZ), this.xzScale, this.yScale, visitor.visitNoise(this.noise));
        }

        @Override
        public double minValue() {
            return -this.maxValue();
        }

        @Override
        public double maxValue() {
            return this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record RangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange) implements DensityFunction
    {
        public static final MapCodec<RangeChoice> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.CODEC.fieldOf("input").forGetter(RangeChoice::input), (App)NOISE_VALUE_CODEC.fieldOf("min_inclusive").forGetter(RangeChoice::minInclusive), (App)NOISE_VALUE_CODEC.fieldOf("max_exclusive").forGetter(RangeChoice::maxExclusive), (App)DensityFunction.CODEC.fieldOf("when_in_range").forGetter(RangeChoice::whenInRange), (App)DensityFunction.CODEC.fieldOf("when_out_of_range").forGetter(RangeChoice::whenOutOfRange)).apply((Applicative)i, RangeChoice::new));
        public static final KeyDispatchDataCodec<RangeChoice> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double inputValue = this.input.compute(context);
            if (inputValue >= this.minInclusive && inputValue < this.maxExclusive) {
                return this.whenInRange.compute(context);
            }
            return this.whenOutOfRange.compute(context);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.input.fillArray(output, contextProvider);
            for (int i = 0; i < output.length; ++i) {
                double v = output[i];
                output[i] = v >= this.minInclusive && v < this.maxExclusive ? this.whenInRange.compute(contextProvider.forIndex(i)) : this.whenOutOfRange.compute(contextProvider.forIndex(i));
            }
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new RangeChoice(visitor.apply(this.input), this.minInclusive, this.maxExclusive, visitor.apply(this.whenInRange), visitor.apply(this.whenOutOfRange));
        }

        @Override
        public double minValue() {
            return Math.min(this.whenInRange.minValue(), this.whenOutOfRange.minValue());
        }

        @Override
        public double maxValue() {
            return Math.max(this.whenInRange.maxValue(), this.whenOutOfRange.maxValue());
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record IntervalSelect(DensityFunction input, DoubleList thresholds, List<DensityFunction> functions) implements DensityFunction
    {
        private static final Codec<DoubleList> THRESHOLDS_CODEC = NOISE_VALUE_CODEC.listOf().xmap(DoubleArrayList::new, Function.identity());
        public static final MapCodec<IntervalSelect> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.CODEC.fieldOf("input").forGetter(IntervalSelect::input), (App)THRESHOLDS_CODEC.fieldOf("thresholds").forGetter(IntervalSelect::thresholds), (App)DensityFunction.CODEC.listOf(2, Integer.MAX_VALUE).fieldOf("functions").forGetter(IntervalSelect::functions)).apply((Applicative)i, IntervalSelect::new)).validate(IntervalSelect::validate);
        public static final KeyDispatchDataCodec<IntervalSelect> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        private DataResult<IntervalSelect> validate() {
            if (this.thresholds.size() != this.functions.size() - 1) {
                return DataResult.error(() -> "Expected " + (this.functions.size() - 1) + " thresholds for " + this.functions.size() + " functions, but got " + this.thresholds.size());
            }
            if (!Comparators.isInOrder((Iterable)this.thresholds, Double::compare)) {
                return DataResult.error(() -> "Threshold values must be ordered from smallest to largest");
            }
            return DataResult.success((Object)this);
        }

        private double compute(DensityFunction.FunctionContext context, double input) {
            for (int i = 0; i < this.thresholds.size(); ++i) {
                if (!(input < this.thresholds.getDouble(i))) continue;
                return this.functions.get(i).compute(context);
            }
            return this.functions.getLast().compute(context);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.compute(context, this.input.compute(context));
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.input.fillArray(output, contextProvider);
            for (int i = 0; i < output.length; ++i) {
                output[i] = this.compute(contextProvider.forIndex(i), output[i]);
            }
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new IntervalSelect(visitor.apply(this.input), this.thresholds, List.copyOf(Lists.transform(this.functions, visitor::apply)));
        }

        @Override
        public double minValue() {
            double minValue = Double.MAX_VALUE;
            for (DensityFunction function : this.functions) {
                minValue = Math.min(function.minValue(), minValue);
            }
            return minValue;
        }

        @Override
        public double maxValue() {
            double maxValue = -1.7976931348623157E308;
            for (DensityFunction function : this.functions) {
                maxValue = Math.max(function.maxValue(), maxValue);
            }
            return maxValue;
        }

        public KeyDispatchDataCodec<IntervalSelect> codec() {
            return CODEC;
        }
    }

    protected record ShiftA(DensityFunction.NoiseHolder offsetNoise) implements ShiftNoise
    {
        private static final KeyDispatchDataCodec<ShiftA> CODEC = DensityFunctions.singleArgumentCodec(DensityFunction.NoiseHolder.CODEC, ShiftA::new, ShiftA::offsetNoise);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.compute(context.blockX(), 0.0, context.blockZ());
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new ShiftA(visitor.visitNoise(this.offsetNoise));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record ShiftB(DensityFunction.NoiseHolder offsetNoise) implements ShiftNoise
    {
        private static final KeyDispatchDataCodec<ShiftB> CODEC = DensityFunctions.singleArgumentCodec(DensityFunction.NoiseHolder.CODEC, ShiftB::new, ShiftB::offsetNoise);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.compute(context.blockZ(), context.blockX(), 0.0);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new ShiftB(visitor.visitNoise(this.offsetNoise));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record Shift(DensityFunction.NoiseHolder offsetNoise) implements ShiftNoise
    {
        private static final KeyDispatchDataCodec<Shift> CODEC = DensityFunctions.singleArgumentCodec(DensityFunction.NoiseHolder.CODEC, Shift::new, Shift::offsetNoise);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.compute(context.blockX(), context.blockY(), context.blockZ());
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new Shift(visitor.visitNoise(this.offsetNoise));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record Clamp(DensityFunction input, double minValue, double maxValue) implements PureTransformer
    {
        private static final MapCodec<Clamp> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.CODEC.fieldOf("input").forGetter(Clamp::input), (App)NOISE_VALUE_CODEC.fieldOf("min").forGetter(Clamp::minValue), (App)NOISE_VALUE_CODEC.fieldOf("max").forGetter(Clamp::maxValue)).apply((Applicative)i, Clamp::new));
        public static final KeyDispatchDataCodec<Clamp> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double transform(double input) {
            return Mth.clamp(input, this.minValue, this.maxValue);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new Clamp(visitor.apply(this.input), this.minValue, this.maxValue);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record Mapped(Type type, DensityFunction input, double minValue, double maxValue) implements PureTransformer
    {
        public static Mapped create(Type type, DensityFunction input) {
            double minValue = input.minValue();
            double maxValue = input.maxValue();
            double minImage = Mapped.transform(type, minValue);
            double maxImage = Mapped.transform(type, maxValue);
            if (type == Type.INVERT) {
                if (minValue < 0.0 && maxValue > 0.0) {
                    return new Mapped(type, input, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                }
                return new Mapped(type, input, maxImage, minImage);
            }
            if (type == Type.ABS || type == Type.SQUARE) {
                return new Mapped(type, input, Math.max(0.0, minValue), Math.max(minImage, maxImage));
            }
            return new Mapped(type, input, minImage, maxImage);
        }

        private static double transform(Type type, double input) {
            return switch (type.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> Math.abs(input);
                case 1 -> input * input;
                case 2 -> input * input * input;
                case 3 -> {
                    if (input > 0.0) {
                        yield input;
                    }
                    yield input * 0.5;
                }
                case 4 -> {
                    if (input > 0.0) {
                        yield input;
                    }
                    yield input * 0.25;
                }
                case 5 -> 1.0 / input;
                case 6 -> {
                    double c = Mth.clamp(input, -1.0, 1.0);
                    yield c / 2.0 - c * c * c / 24.0;
                }
            };
        }

        @Override
        public double transform(double input) {
            return Mapped.transform(this.type, input);
        }

        @Override
        public Mapped mapChildren(DensityFunction.Visitor visitor) {
            return Mapped.create(this.type, visitor.apply(this.input));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type.codec;
        }

        public static enum Type implements StringRepresentable
        {
            ABS("abs"),
            SQUARE("square"),
            CUBE("cube"),
            HALF_NEGATIVE("half_negative"),
            QUARTER_NEGATIVE("quarter_negative"),
            INVERT("invert"),
            SQUEEZE("squeeze");

            private final String name;
            private final KeyDispatchDataCodec<Mapped> codec = DensityFunctions.singleFunctionArgumentCodec(input -> Mapped.create(this, input), Mapped::input);

            private Type(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    public static interface TwoArgumentSimpleFunction
    extends DensityFunction {
        public static final Logger LOGGER = LogUtils.getLogger();

        public static TwoArgumentSimpleFunction create(Type type, DensityFunction argument1, DensityFunction argument2) {
            double maxValue;
            double min1 = argument1.minValue();
            double min2 = argument2.minValue();
            double max1 = argument1.maxValue();
            double max2 = argument2.maxValue();
            if (type == Type.MIN || type == Type.MAX) {
                boolean secondAlwaysBiggerThanFirst;
                boolean firstAlwaysBiggerThanSecond = min1 >= max2;
                boolean bl = secondAlwaysBiggerThanFirst = min2 >= max1;
                if (firstAlwaysBiggerThanSecond || secondAlwaysBiggerThanFirst) {
                    LOGGER.warn("Creating a {} function between two non-overlapping inputs: {} and {}", new Object[]{type, argument1, argument2});
                }
            }
            double minValue = switch (type.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> min1 + min2;
                case 3 -> Math.max(min1, min2);
                case 2 -> Math.min(min1, min2);
                case 1 -> min1 > 0.0 && min2 > 0.0 ? min1 * min2 : (max1 < 0.0 && max2 < 0.0 ? max1 * max2 : Math.min(min1 * max2, max1 * min2));
            };
            switch (type.ordinal()) {
                default: {
                    throw new MatchException(null, null);
                }
                case 0: {
                    double d = max1 + max2;
                    break;
                }
                case 3: {
                    double d = Math.max(max1, max2);
                    break;
                }
                case 2: {
                    double d = Math.min(max1, max2);
                    break;
                }
                case 1: {
                    double d = min1 > 0.0 && min2 > 0.0 ? max1 * max2 : (maxValue = max1 < 0.0 && max2 < 0.0 ? min1 * min2 : Math.max(min1 * min2, max1 * max2));
                }
            }
            if (type == Type.MUL || type == Type.ADD) {
                if (argument1 instanceof Constant) {
                    Constant constant = (Constant)argument1;
                    return new MulOrAdd(type == Type.ADD ? MulOrAdd.Type.ADD : MulOrAdd.Type.MUL, argument2, minValue, maxValue, constant.value);
                }
                if (argument2 instanceof Constant) {
                    Constant constant = (Constant)argument2;
                    return new MulOrAdd(type == Type.ADD ? MulOrAdd.Type.ADD : MulOrAdd.Type.MUL, argument1, minValue, maxValue, constant.value);
                }
            }
            return new Ap2(type, argument1, argument2, minValue, maxValue);
        }

        public Type type();

        public DensityFunction argument1();

        public DensityFunction argument2();

        @Override
        default public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type().codec;
        }

        public static enum Type implements StringRepresentable
        {
            ADD("add"),
            MUL("mul"),
            MIN("min"),
            MAX("max");

            private final KeyDispatchDataCodec<TwoArgumentSimpleFunction> codec = DensityFunctions.doubleFunctionArgumentCodec((argument1, argument2) -> TwoArgumentSimpleFunction.create(this, argument1, argument2), TwoArgumentSimpleFunction::argument1, TwoArgumentSimpleFunction::argument2);
            private final String name;

            private Type(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    public static final class Spline
    implements DensityFunction {
        private static final Codec<CubicSpline<Coordinate>> SPLINE_CODEC = CubicSpline.codec(Coordinate.CODEC);
        private static final MapCodec<Spline> DATA_CODEC = SPLINE_CODEC.fieldOf("spline").xmap(Spline::new, Spline::spline);
        public static final KeyDispatchDataCodec<Spline> CODEC = DensityFunctions.makeCodec(DATA_CODEC);
        private final CubicSpline<Coordinate> spline;
        private final BoundedFloatFunction<Point> sampler;

        public Spline(CubicSpline<Coordinate> spline) {
            this.spline = spline;
            this.sampler = CubicSpline.asSampler(spline);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.sampler.apply(new Point(context));
        }

        @Override
        public double minValue() {
            return this.spline.minValue();
        }

        @Override
        public double maxValue() {
            return this.spline.maxValue();
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new Spline(this.spline.mapCoordinates(c -> c.mapChildren(visitor)));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        public CubicSpline<Coordinate> spline() {
            return this.spline;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Spline)) return false;
            Spline splineFunction = (Spline)obj;
            if (!this.spline.equals(splineFunction.spline)) return false;
            return true;
        }

        public int hashCode() {
            return this.spline.hashCode();
        }

        public String toString() {
            return this.spline.toString();
        }

        public record Point(DensityFunction.FunctionContext context) {
        }

        public record Coordinate(DensityFunction function) implements BoundedFloatFunction<Point>
        {
            public static final Codec<Coordinate> CODEC = DensityFunction.CODEC.xmap(Coordinate::new, Coordinate::function);

            @Override
            public float apply(Point point) {
                return (float)this.function.compute(point.context());
            }

            @Override
            public float minValue() {
                return (float)this.function.minValue();
            }

            @Override
            public float maxValue() {
                return (float)this.function.maxValue();
            }

            public Coordinate mapChildren(DensityFunction.Visitor visitor) {
                return new Coordinate(visitor.apply(this.function));
            }
        }
    }

    private record Constant(double value) implements DensityFunction.SimpleFunction
    {
        private static final KeyDispatchDataCodec<Constant> CODEC = DensityFunctions.singleArgumentCodec(NOISE_VALUE_CODEC, Constant::new, Constant::value);
        private static final Constant ZERO = new Constant(0.0);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.value;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, this.value);
        }

        @Override
        public double minValue() {
            return this.value;
        }

        @Override
        public double maxValue() {
            return this.value;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record YClampedGradient(int fromY, int toY, double fromValue, double toValue) implements DensityFunction.SimpleFunction
    {
        private static final MapCodec<YClampedGradient> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.intRange((int)(DimensionType.MIN_Y * 2), (int)(DimensionType.MAX_Y * 2)).fieldOf("from_y").forGetter(YClampedGradient::fromY), (App)Codec.intRange((int)(DimensionType.MIN_Y * 2), (int)(DimensionType.MAX_Y * 2)).fieldOf("to_y").forGetter(YClampedGradient::toY), (App)NOISE_VALUE_CODEC.fieldOf("from_value").forGetter(YClampedGradient::fromValue), (App)NOISE_VALUE_CODEC.fieldOf("to_value").forGetter(YClampedGradient::toValue)).apply((Applicative)i, YClampedGradient::new));
        public static final KeyDispatchDataCodec<YClampedGradient> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return Mth.clampedMap((double)context.blockY(), (double)this.fromY, (double)this.toY, this.fromValue, this.toValue);
        }

        @Override
        public double minValue() {
            return Math.min(this.fromValue, this.toValue);
        }

        @Override
        public double maxValue() {
            return Math.max(this.fromValue, this.toValue);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record FindTopSurface(DensityFunction density, DensityFunction upperBound, int lowerBound, int cellHeight) implements DensityFunction
    {
        private static final MapCodec<FindTopSurface> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DensityFunction.CODEC.fieldOf("density").forGetter(FindTopSurface::density), (App)DensityFunction.CODEC.fieldOf("upper_bound").forGetter(FindTopSurface::upperBound), (App)Codec.intRange((int)(DimensionType.MIN_Y * 2), (int)(DimensionType.MAX_Y * 2)).fieldOf("lower_bound").forGetter(FindTopSurface::lowerBound), (App)ExtraCodecs.POSITIVE_INT.fieldOf("cell_height").forGetter(FindTopSurface::cellHeight)).apply((Applicative)i, FindTopSurface::new));
        public static final KeyDispatchDataCodec<FindTopSurface> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int topY = Mth.floor(this.upperBound.compute(context) / (double)this.cellHeight) * this.cellHeight;
            if (topY <= this.lowerBound) {
                return this.lowerBound;
            }
            for (int blockY = topY; blockY >= this.lowerBound; blockY -= this.cellHeight) {
                DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(context.blockX(), blockY, context.blockZ());
                if (!(this.density.compute(singlePointContext) > 0.0)) continue;
                return blockY;
            }
            return this.lowerBound;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new FindTopSurface(visitor.apply(this.density), visitor.apply(this.upperBound), this.lowerBound, this.cellHeight);
        }

        @Override
        public double minValue() {
            return this.lowerBound;
        }

        @Override
        public double maxValue() {
            return Math.max((double)this.lowerBound, this.upperBound.maxValue());
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record Ap2(TwoArgumentSimpleFunction.Type type, DensityFunction argument1, DensityFunction argument2, double minValue, double maxValue) implements TwoArgumentSimpleFunction
    {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double v1 = this.argument1.compute(context);
            return switch (this.type.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> v1 + this.argument2.compute(context);
                case 1 -> {
                    if (v1 == 0.0) {
                        yield 0.0;
                    }
                    yield v1 * this.argument2.compute(context);
                }
                case 2 -> {
                    if (v1 < this.argument2.minValue()) {
                        yield v1;
                    }
                    yield Math.min(v1, this.argument2.compute(context));
                }
                case 3 -> v1 > this.argument2.maxValue() ? v1 : Math.max(v1, this.argument2.compute(context));
            };
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.argument1.fillArray(output, contextProvider);
            switch (this.type.ordinal()) {
                case 0: {
                    double[] v2 = new double[output.length];
                    this.argument2.fillArray(v2, contextProvider);
                    for (int i = 0; i < output.length; ++i) {
                        output[i] = output[i] + v2[i];
                    }
                    break;
                }
                case 1: {
                    for (int i = 0; i < output.length; ++i) {
                        double v = output[i];
                        output[i] = v == 0.0 ? 0.0 : v * this.argument2.compute(contextProvider.forIndex(i));
                    }
                    break;
                }
                case 2: {
                    double min = this.argument2.minValue();
                    for (int i = 0; i < output.length; ++i) {
                        double v = output[i];
                        output[i] = v < min ? v : Math.min(v, this.argument2.compute(contextProvider.forIndex(i)));
                    }
                    break;
                }
                case 3: {
                    double max = this.argument2.maxValue();
                    for (int i = 0; i < output.length; ++i) {
                        double v = output[i];
                        output[i] = v > max ? v : Math.max(v, this.argument2.compute(contextProvider.forIndex(i)));
                    }
                    break;
                }
            }
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return TwoArgumentSimpleFunction.create(this.type, visitor.apply(this.argument1), visitor.apply(this.argument2));
        }
    }

    private record MulOrAdd(Type specificType, DensityFunction input, double minValue, double maxValue, double argument) implements TwoArgumentSimpleFunction,
    PureTransformer
    {
        @Override
        public TwoArgumentSimpleFunction.Type type() {
            return this.specificType == Type.MUL ? TwoArgumentSimpleFunction.Type.MUL : TwoArgumentSimpleFunction.Type.ADD;
        }

        @Override
        public DensityFunction argument1() {
            return DensityFunctions.constant(this.argument);
        }

        @Override
        public DensityFunction argument2() {
            return this.input;
        }

        @Override
        public double transform(double input) {
            return switch (this.specificType.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> input * this.argument;
                case 1 -> input + this.argument;
            };
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            double maxValue;
            double minValue;
            DensityFunction function = visitor.apply(this.input);
            double min = function.minValue();
            double max = function.maxValue();
            if (this.specificType == Type.ADD) {
                minValue = min + this.argument;
                maxValue = max + this.argument;
            } else if (this.argument >= 0.0) {
                minValue = min * this.argument;
                maxValue = max * this.argument;
            } else {
                minValue = max * this.argument;
                maxValue = min * this.argument;
            }
            return new MulOrAdd(this.specificType, function, minValue, maxValue, this.argument);
        }

        public static enum Type {
            MUL,
            ADD;

        }
    }

    protected static interface ShiftNoise
    extends DensityFunction {
        public DensityFunction.NoiseHolder offsetNoise();

        @Override
        default public double minValue() {
            return -this.maxValue();
        }

        @Override
        default public double maxValue() {
            return this.offsetNoise().maxValue() * 4.0;
        }

        default public double compute(double localX, double localY, double localZ) {
            return this.offsetNoise().getValue(localX * 0.25, localY * 0.25, localZ * 0.25) * 4.0;
        }

        @Override
        default public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }
    }

    public static interface MarkerOrMarked
    extends DensityFunction {
        public Marker.Type type();

        public DensityFunction wrapped();

        @Override
        default public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type().codec;
        }

        @Override
        default public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new Marker(this.type(), visitor.apply(this.wrapped()));
        }
    }

    @VisibleForDebug
    public record HolderHolder(Holder<DensityFunction> function) implements DensityFunction
    {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.function.value().compute(context);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.function.value().fillArray(output, contextProvider);
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return new HolderHolder(Holder.direct(visitor.apply(this.function.value())));
        }

        @Override
        public double minValue() {
            return this.function.isBound() ? this.function.value().minValue() : Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return this.function.isBound() ? this.function.value().maxValue() : Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
        }
    }

    public static interface BeardifierOrMarker
    extends DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit((Object)BeardifierMarker.INSTANCE));

        @Override
        default public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private static interface PureTransformer
    extends DensityFunction {
        public DensityFunction input();

        @Override
        default public double compute(DensityFunction.FunctionContext context) {
            return this.transform(this.input().compute(context));
        }

        @Override
        default public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.input().fillArray(output, contextProvider);
            for (int i = 0; i < output.length; ++i) {
                output[i] = this.transform(output[i]);
            }
        }

        public double transform(double var1);
    }

    private static interface TransformerWithContext
    extends DensityFunction {
        public DensityFunction input();

        @Override
        default public double compute(DensityFunction.FunctionContext context) {
            return this.transform(context, this.input().compute(context));
        }

        @Override
        default public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.input().fillArray(output, contextProvider);
            for (int i = 0; i < output.length; ++i) {
                output[i] = this.transform(contextProvider.forIndex(i), output[i]);
            }
        }

        public double transform(DensityFunction.FunctionContext var1, double var2);
    }
}

