/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.floats.Float2FloatFunction
 *  it.unimi.dsi.fastutil.floats.FloatArrayList
 *  it.unimi.dsi.fastutil.floats.FloatList
 */
package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;

public sealed interface CubicSpline<I> {
    public CubicSpline<I> mapCoordinates(UnaryOperator<I> var1);

    public float minValue();

    public float maxValue();

    @VisibleForDebug
    public String parityString();

    public static <C, I extends BoundedFloatFunction<C>> float sample(CubicSpline<I> spline, C coordinate) {
        CubicSpline<I> cubicSpline = spline;
        Objects.requireNonNull(cubicSpline);
        CubicSpline<I> cubicSpline2 = cubicSpline;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Multipoint.class, Constant.class}, cubicSpline2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                Multipoint multipoint = (Multipoint)cubicSpline2;
                yield Multipoint.sample(multipoint, coordinate);
            }
            case 1 -> {
                Constant constant = (Constant)cubicSpline2;
                yield constant.value();
            }
        };
    }

    public static <C, I extends BoundedFloatFunction<C>> BoundedFloatFunction<C> asSampler(CubicSpline<I> spline) {
        CubicSpline<I> cubicSpline = spline;
        Objects.requireNonNull(cubicSpline);
        CubicSpline<I> cubicSpline2 = cubicSpline;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Multipoint.class, Constant.class}, cubicSpline2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                final Multipoint multipoint = (Multipoint)cubicSpline2;
                yield new BoundedFloatFunction<C>(){

                    @Override
                    public float apply(C c) {
                        return Multipoint.sample(multipoint, c);
                    }

                    @Override
                    public float minValue() {
                        return multipoint.minValue();
                    }

                    @Override
                    public float maxValue() {
                        return multipoint.maxValue();
                    }
                };
            }
            case 1 -> {
                Constant constant = (Constant)cubicSpline2;
                yield BoundedFloatFunction.constant(constant.value());
            }
        };
    }

    public static <I extends BoundedFloatFunction<?>> Codec<CubicSpline<I>> codec(Codec<I> coordinateCodec) {
        return Codec.recursive((String)"CubicSpline", subSplineCodec -> Codec.either((Codec)Codec.FLOAT, Multipoint.codec(coordinateCodec, subSplineCodec)).xmap(e -> (CubicSpline)e.map(Constant::new, m -> m), spline -> {
            Either either;
            CubicSpline cubicSpline = spline;
            Objects.requireNonNull(cubicSpline);
            CubicSpline selector1$temp = cubicSpline;
            int index$2 = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Constant.class, Multipoint.class}, (CubicSpline)selector1$temp, index$2)) {
                default: {
                    throw new MatchException(null, null);
                }
                case 0: {
                    float patt3$temp;
                    Constant $b$0 = (Constant)selector1$temp;
                    float tmp0$ = patt3$temp = $b$0.value();
                    float value = patt3$temp;
                    either = Either.left((Object)Float.valueOf(value));
                    return either;
                }
                case 1: 
            }
            Multipoint multipoint = (Multipoint)selector1$temp;
            either = Either.right((Object)multipoint);
            return either;
            catch (Throwable throwable) {
                throw new MatchException(throwable.toString(), throwable);
            }
        }));
    }

    public static <I> CubicSpline<I> constant(float value) {
        return new Constant(value);
    }

    public static <I extends BoundedFloatFunction<?>> Builder<I> builder(I coordinate) {
        return new Builder<I>(coordinate);
    }

    public static <I extends BoundedFloatFunction<?>> Builder<I> builder(I coordinate, Float2FloatFunction valueTransformer) {
        return new Builder<I>(coordinate, valueTransformer);
    }

    @VisibleForDebug
    public static final class Multipoint<I extends BoundedFloatFunction<?>>
    extends Record
    implements CubicSpline<I> {
        private final I coordinate;
        private final float[] locations;
        private final List<CubicSpline<I>> values;
        private final float[] derivatives;
        private final float minValue;
        private final float maxValue;

        public Multipoint(I coordinate, float[] locations, List<CubicSpline<I>> values, float[] derivatives, float minValue, float maxValue) {
            Multipoint.validateSizes(locations, values, derivatives);
            this.coordinate = coordinate;
            this.locations = locations;
            this.values = values;
            this.derivatives = derivatives;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public Multipoint(I coordinate, float[] locations, List<CubicSpline<I>> values, float[] derivatives) {
            float edge2;
            float edge1;
            int lastIndex = locations.length - 1;
            float minValue = Float.POSITIVE_INFINITY;
            float maxValue = Float.NEGATIVE_INFINITY;
            float minInput = coordinate.minValue();
            float maxInput = coordinate.maxValue();
            if (minInput < locations[0]) {
                edge1 = Multipoint.linearExtend(minInput, locations, values.get(0).minValue(), derivatives, 0);
                edge2 = Multipoint.linearExtend(minInput, locations, values.get(0).maxValue(), derivatives, 0);
                minValue = Math.min(minValue, Math.min(edge1, edge2));
                maxValue = Math.max(maxValue, Math.max(edge1, edge2));
            }
            if (maxInput > locations[lastIndex]) {
                edge1 = Multipoint.linearExtend(maxInput, locations, values.get(lastIndex).minValue(), derivatives, lastIndex);
                edge2 = Multipoint.linearExtend(maxInput, locations, values.get(lastIndex).maxValue(), derivatives, lastIndex);
                minValue = Math.min(minValue, Math.min(edge1, edge2));
                maxValue = Math.max(maxValue, Math.max(edge1, edge2));
            }
            for (CubicSpline<I> value : values) {
                minValue = Math.min(minValue, value.minValue());
                maxValue = Math.max(maxValue, value.maxValue());
            }
            for (int i = 0; i < lastIndex; ++i) {
                float x1 = locations[i];
                float x2 = locations[i + 1];
                float xDiff = x2 - x1;
                CubicSpline<I> v1 = values.get(i);
                CubicSpline<I> v2 = values.get(i + 1);
                float min1 = v1.minValue();
                float max1 = v1.maxValue();
                float min2 = v2.minValue();
                float max2 = v2.maxValue();
                float d1 = derivatives[i];
                float d2 = derivatives[i + 1];
                if (d1 == 0.0f && d2 == 0.0f) continue;
                float p1 = d1 * xDiff;
                float p2 = d2 * xDiff;
                float minLerp1 = Math.min(min1, min2);
                float maxLerp1 = Math.max(max1, max2);
                float minA = p1 - max2 + min1;
                float maxA = p1 - min2 + max1;
                float minB = -p2 + min2 - max1;
                float maxB = -p2 + max2 - min1;
                float minLerp2 = Math.min(minA, minB);
                float maxLerp2 = Math.max(maxA, maxB);
                minValue = Math.min(minValue, minLerp1 + 0.25f * minLerp2);
                maxValue = Math.max(maxValue, maxLerp1 + 0.25f * maxLerp2);
            }
            this(coordinate, locations, values, derivatives, minValue, maxValue);
        }

        private static float linearExtend(float input, float[] locations, float value, float[] derivatives, int index) {
            float derivative = derivatives[index];
            if (derivative == 0.0f) {
                return value;
            }
            return value + derivative * (input - locations[index]);
        }

        private static <I> void validateSizes(float[] locations, List<CubicSpline<I>> values, float[] derivatives) {
            if (locations.length != values.size() || locations.length != derivatives.length) {
                throw new IllegalArgumentException("All lengths must be equal, got: " + locations.length + " " + values.size() + " " + derivatives.length);
            }
            if (locations.length == 0) {
                throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
            }
        }

        public static <C, I extends BoundedFloatFunction<C>> float sample(Multipoint<I> sampler, C c) {
            return Multipoint.sample(sampler.coordinate, sampler.derivatives, sampler.locations, sampler.values, c);
        }

        private static <C, I extends BoundedFloatFunction<C>> float sample(I coordinate, float[] derivatives, float[] locations, List<CubicSpline<I>> values, C c) {
            float input = coordinate.apply(c);
            int start = Multipoint.findIntervalStart(locations, input);
            int lastIndex = locations.length - 1;
            if (start < 0) {
                return Multipoint.linearExtend(input, locations, CubicSpline.sample(values.getFirst(), c), derivatives, 0);
            }
            if (start == lastIndex) {
                return Multipoint.linearExtend(input, locations, CubicSpline.sample(values.get(lastIndex), c), derivatives, lastIndex);
            }
            float x1 = locations[start];
            float x2 = locations[start + 1];
            float t = (input - x1) / (x2 - x1);
            CubicSpline<I> f1 = values.get(start);
            CubicSpline<I> f2 = values.get(start + 1);
            float d1 = derivatives[start];
            float d2 = derivatives[start + 1];
            float y1 = CubicSpline.sample(f1, c);
            float y2 = CubicSpline.sample(f2, c);
            float a = d1 * (x2 - x1) - (y2 - y1);
            float b = -d2 * (x2 - x1) + (y2 - y1);
            float offset = Mth.lerp(t, y1, y2) + t * (1.0f - t) * Mth.lerp(t, a, b);
            return offset;
        }

        private static int findIntervalStart(float[] locations, float input) {
            return Mth.binarySearch(0, locations.length, i -> input < locations[i]) - 1;
        }

        @Override
        @VisibleForTesting
        public String parityString() {
            return "Spline{coordinate=" + String.valueOf(this.coordinate) + ", locations=" + Multipoint.toString(this.locations) + ", derivatives=" + Multipoint.toString(this.derivatives) + ", values=" + this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]")) + "}";
        }

        private static String toString(float[] arr) {
            return "[" + IntStream.range(0, arr.length).mapToDouble(i -> arr[i]).mapToObj(f -> String.format(Locale.ROOT, "%.3f", f)).collect(Collectors.joining(", ")) + "]";
        }

        @Override
        public CubicSpline<I> mapCoordinates(UnaryOperator<I> mapper) {
            return new Multipoint<BoundedFloatFunction>((BoundedFloatFunction)mapper.apply(this.coordinate), this.locations, this.values.stream().map(v -> v.mapCoordinates(mapper)).toList(), this.derivatives);
        }

        public static <I extends BoundedFloatFunction<?>> Codec<Multipoint<I>> codec(Codec<I> coordinateCodec, Codec<CubicSpline<I>> subSplineCodec) {
            return RecordCodecBuilder.create(i -> i.group((App)coordinateCodec.fieldOf("coordinate").forGetter(Multipoint::coordinate), (App)ExtraCodecs.nonEmptyList(Point.codec(subSplineCodec).listOf()).fieldOf("points").forGetter(Multipoint::packToPoints)).apply((Applicative)i, Multipoint::createFromPoints));
        }

        private List<Point<I>> packToPoints() {
            int pointCount = this.locations.length;
            ArrayList<Point<I>> list = new ArrayList<Point<I>>(pointCount);
            for (int p = 0; p < pointCount; ++p) {
                list.add(new Point<I>(this.locations[p], this.values.get(p), this.derivatives[p]));
            }
            return list;
        }

        private static <I extends BoundedFloatFunction<?>> Multipoint<I> createFromPoints(I coordinate, List<Point<I>> points) {
            int pointCount = points.size();
            float[] locations = new float[pointCount];
            ImmutableList.Builder values = ImmutableList.builderWithExpectedSize((int)pointCount);
            float[] derivatives = new float[pointCount];
            for (int p = 0; p < pointCount; ++p) {
                Point<I> point = points.get(p);
                locations[p] = point.location();
                values.add(point.value());
                derivatives[p] = point.derivative();
            }
            return new Multipoint<I>(coordinate, locations, values.build(), derivatives);
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{Multipoint.class, "coordinate;locations;values;derivatives;minValue;maxValue", "coordinate", "locations", "values", "derivatives", "minValue", "maxValue"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{Multipoint.class, "coordinate;locations;values;derivatives;minValue;maxValue", "coordinate", "locations", "values", "derivatives", "minValue", "maxValue"}, this);
        }

        @Override
        public final boolean equals(Object o) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{Multipoint.class, "coordinate;locations;values;derivatives;minValue;maxValue", "coordinate", "locations", "values", "derivatives", "minValue", "maxValue"}, this, o);
        }

        public I coordinate() {
            return this.coordinate;
        }

        public float[] locations() {
            return this.locations;
        }

        public List<CubicSpline<I>> values() {
            return this.values;
        }

        public float[] derivatives() {
            return this.derivatives;
        }

        @Override
        public float minValue() {
            return this.minValue;
        }

        @Override
        public float maxValue() {
            return this.maxValue;
        }

        private record Point<I extends BoundedFloatFunction<?>>(float location, CubicSpline<I> value, float derivative) {
            public static <I extends BoundedFloatFunction<?>> Codec<Point<I>> codec(Codec<CubicSpline<I>> subSplineCodec) {
                return RecordCodecBuilder.create(i -> i.group((App)Codec.FLOAT.fieldOf("location").forGetter(Point::location), (App)subSplineCodec.fieldOf("value").forGetter(Point::value), (App)Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)).apply((Applicative)i, Point::new));
            }
        }
    }

    @VisibleForDebug
    public record Constant<I>(float value) implements CubicSpline<I>
    {
        @Override
        public String parityString() {
            return String.format(Locale.ROOT, "k=%.3f", Float.valueOf(this.value));
        }

        @Override
        public float minValue() {
            return this.value;
        }

        @Override
        public float maxValue() {
            return this.value;
        }

        @Override
        public CubicSpline<I> mapCoordinates(UnaryOperator<I> mapper) {
            return this;
        }
    }

    public static final class Builder<I extends BoundedFloatFunction<?>> {
        private final I coordinate;
        private final Float2FloatFunction valueTransformer;
        private final FloatList locations = new FloatArrayList();
        private final List<CubicSpline<I>> values = Lists.newArrayList();
        private final FloatList derivatives = new FloatArrayList();

        private Builder(I coordinate) {
            this(coordinate, Float2FloatFunction.identity());
        }

        private Builder(I coordinate, Float2FloatFunction valueTransformer) {
            this.coordinate = coordinate;
            this.valueTransformer = valueTransformer;
        }

        public Builder<I> addPoint(float location, float value) {
            return this.addPoint(location, new Constant(((Float)this.valueTransformer.apply((Object)Float.valueOf(value))).floatValue()), 0.0f);
        }

        public Builder<I> addPoint(float location, float value, float derivative) {
            return this.addPoint(location, new Constant(((Float)this.valueTransformer.apply((Object)Float.valueOf(value))).floatValue()), derivative);
        }

        public Builder<I> addPoint(float location, CubicSpline<I> sampler) {
            return this.addPoint(location, sampler, 0.0f);
        }

        private Builder<I> addPoint(float location, CubicSpline<I> sampler, float derivative) {
            if (!this.locations.isEmpty() && location <= this.locations.getFloat(this.locations.size() - 1)) {
                throw new IllegalArgumentException("Please register points in ascending order");
            }
            this.locations.add(location);
            this.values.add(sampler);
            this.derivatives.add(derivative);
            return this;
        }

        public CubicSpline<I> build() {
            if (this.locations.isEmpty()) {
                throw new IllegalStateException("No elements added");
            }
            return new Multipoint<I>(this.coordinate, this.locations.toFloatArray(), List.copyOf(this.values), this.derivatives.toFloatArray());
        }
    }
}

