/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jspecify.annotations.Nullable;

public class Climate {
    private static final boolean DEBUG_SLOW_BIOME_SEARCH = false;
    private static final float QUANTIZATION_FACTOR = 10000.0f;
    @VisibleForTesting
    protected static final int PARAMETER_COUNT = 7;

    public static TargetPoint target(float temperature, float humidity, float continentalness, float erosion, float depth, float weirdness) {
        return new TargetPoint(Climate.quantizeCoord(temperature), Climate.quantizeCoord(humidity), Climate.quantizeCoord(continentalness), Climate.quantizeCoord(erosion), Climate.quantizeCoord(depth), Climate.quantizeCoord(weirdness));
    }

    public static ParameterPoint parameters(float temperature, float humidity, float continentalness, float erosion, float depth, float weirdness, float offset) {
        return new ParameterPoint(Parameter.point(temperature), Parameter.point(humidity), Parameter.point(continentalness), Parameter.point(erosion), Parameter.point(depth), Parameter.point(weirdness), Climate.quantizeCoord(offset));
    }

    public static ParameterPoint parameters(Parameter temperature, Parameter humidity, Parameter continentalness, Parameter erosion, Parameter depth, Parameter weirdness, float offset) {
        return new ParameterPoint(temperature, humidity, continentalness, erosion, depth, weirdness, Climate.quantizeCoord(offset));
    }

    public static long quantizeCoord(float coord) {
        return (long)(coord * 10000.0f);
    }

    public static float unquantizeCoord(long coord) {
        return (float)coord / 10000.0f;
    }

    public static Sampler empty() {
        DensityFunction zero = DensityFunctions.zero();
        return new Sampler(zero, zero, zero, zero, zero, zero, List.of());
    }

    public static BlockPos findSpawnPosition(List<ParameterPoint> targetClimates, Sampler sampler) {
        return new SpawnFinder(targetClimates, (Sampler)sampler).result.location();
    }

    public record TargetPoint(long temperature, long humidity, long continentalness, long erosion, long depth, long weirdness) {
        @VisibleForTesting
        long[] toParameterArray() {
            return new long[]{this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, 0L};
        }
    }

    public record ParameterPoint(Parameter temperature, Parameter humidity, Parameter continentalness, Parameter erosion, Parameter depth, Parameter weirdness, long offset) {
        public static final Codec<ParameterPoint> CODEC = RecordCodecBuilder.create(i -> i.group((App)Parameter.CODEC.fieldOf("temperature").forGetter(p -> p.temperature), (App)Parameter.CODEC.fieldOf("humidity").forGetter(p -> p.humidity), (App)Parameter.CODEC.fieldOf("continentalness").forGetter(p -> p.continentalness), (App)Parameter.CODEC.fieldOf("erosion").forGetter(p -> p.erosion), (App)Parameter.CODEC.fieldOf("depth").forGetter(p -> p.depth), (App)Parameter.CODEC.fieldOf("weirdness").forGetter(p -> p.weirdness), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("offset").xmap(Climate::quantizeCoord, Climate::unquantizeCoord).forGetter(p -> p.offset)).apply((Applicative)i, ParameterPoint::new));

        private long fitness(TargetPoint target) {
            return Mth.square(this.temperature.distance(target.temperature)) + Mth.square(this.humidity.distance(target.humidity)) + Mth.square(this.continentalness.distance(target.continentalness)) + Mth.square(this.erosion.distance(target.erosion)) + Mth.square(this.depth.distance(target.depth)) + Mth.square(this.weirdness.distance(target.weirdness)) + Mth.square(this.offset);
        }

        List<Parameter> parameterSpace() {
            return ImmutableList.of((Object)this.temperature, (Object)this.humidity, (Object)this.continentalness, (Object)this.erosion, (Object)this.depth, (Object)this.weirdness, (Object)new Parameter(this.offset, this.offset));
        }
    }

    public record Parameter(long min, long max) {
        public static final Codec<Parameter> CODEC = ExtraCodecs.intervalCodec(Codec.floatRange((float)-2.0f, (float)2.0f), "min", "max", (min, max) -> {
            if (min.compareTo((Float)max) > 0) {
                return DataResult.error(() -> "Cannon construct interval, min > max (" + min + " > " + max + ")");
            }
            return DataResult.success((Object)new Parameter(Climate.quantizeCoord(min.floatValue()), Climate.quantizeCoord(max.floatValue())));
        }, p -> Float.valueOf(Climate.unquantizeCoord(p.min())), p -> Float.valueOf(Climate.unquantizeCoord(p.max())));

        public static Parameter point(float min) {
            return Parameter.span(min, min);
        }

        public static Parameter span(float min, float max) {
            if (min > max) {
                throw new IllegalArgumentException("min > max: " + min + " " + max);
            }
            return new Parameter(Climate.quantizeCoord(min), Climate.quantizeCoord(max));
        }

        public static Parameter span(Parameter min, Parameter max) {
            if (min.min() > max.max()) {
                throw new IllegalArgumentException("min > max: " + String.valueOf(min) + " " + String.valueOf(max));
            }
            return new Parameter(min.min(), max.max());
        }

        @Override
        public String toString() {
            return this.min == this.max ? String.format(Locale.ROOT, "%d", this.min) : String.format(Locale.ROOT, "[%d-%d]", this.min, this.max);
        }

        public long distance(long target) {
            long above = target - this.max;
            long below = this.min - target;
            if (above > 0L) {
                return above;
            }
            return Math.max(below, 0L);
        }

        public long distance(Parameter target) {
            long above = target.min() - this.max;
            long below = this.min - target.max();
            if (above > 0L) {
                return above;
            }
            return Math.max(below, 0L);
        }

        public Parameter span(@Nullable Parameter other) {
            return other == null ? this : new Parameter(Math.min(this.min, other.min()), Math.max(this.max, other.max()));
        }
    }

    public record Sampler(DensityFunction temperature, DensityFunction humidity, DensityFunction continentalness, DensityFunction erosion, DensityFunction depth, DensityFunction weirdness, List<ParameterPoint> spawnTarget) {
        public TargetPoint sample(int quartX, int quartY, int quartZ) {
            int blockX = QuartPos.toBlock(quartX);
            int blockY = QuartPos.toBlock(quartY);
            int blockZ = QuartPos.toBlock(quartZ);
            DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
            return Climate.target((float)this.temperature.compute(context), (float)this.humidity.compute(context), (float)this.continentalness.compute(context), (float)this.erosion.compute(context), (float)this.depth.compute(context), (float)this.weirdness.compute(context));
        }

        public BlockPos findSpawnPosition() {
            if (this.spawnTarget.isEmpty()) {
                return BlockPos.ZERO;
            }
            return Climate.findSpawnPosition(this.spawnTarget, this);
        }
    }

    private static class SpawnFinder {
        private static final long MAX_RADIUS = 2048L;
        private Result result;

        private SpawnFinder(List<ParameterPoint> targetClimates, Sampler sampler) {
            this.result = SpawnFinder.getSpawnPositionAndFitness(targetClimates, sampler, 0, 0);
            this.radialSearch(targetClimates, sampler, 2048.0f, 512.0f);
            this.radialSearch(targetClimates, sampler, 512.0f, 32.0f);
        }

        private void radialSearch(List<ParameterPoint> targetClimates, Sampler sampler, float maxRadius, float radiusIncrement) {
            float angle = 0.0f;
            float radius = radiusIncrement;
            BlockPos searchOrigin = this.result.location();
            while (radius <= maxRadius) {
                int z;
                int x = searchOrigin.getX() + (int)(Math.sin(angle) * (double)radius);
                Result candidate = SpawnFinder.getSpawnPositionAndFitness(targetClimates, sampler, x, z = searchOrigin.getZ() + (int)(Math.cos(angle) * (double)radius));
                if (candidate.fitness() < this.result.fitness()) {
                    this.result = candidate;
                }
                if (!((double)(angle += radiusIncrement / radius) > Math.PI * 2)) continue;
                angle = 0.0f;
                radius += radiusIncrement;
            }
        }

        private static Result getSpawnPositionAndFitness(List<ParameterPoint> targetClimates, Sampler sampler, int blockX, int blockZ) {
            TargetPoint targetPoint = sampler.sample(QuartPos.fromBlock(blockX), 0, QuartPos.fromBlock(blockZ));
            TargetPoint zeroDepthTargetPoint = new TargetPoint(targetPoint.temperature(), targetPoint.humidity(), targetPoint.continentalness(), targetPoint.erosion(), 0L, targetPoint.weirdness());
            long minFitness = Long.MAX_VALUE;
            for (ParameterPoint point : targetClimates) {
                minFitness = Math.min(minFitness, point.fitness(zeroDepthTargetPoint));
            }
            long distanceBiasToWorldOrigin = Mth.square((long)blockX) + Mth.square((long)blockZ);
            long fitnessWithDistance = minFitness * Mth.square(2048L) + distanceBiasToWorldOrigin;
            return new Result(new BlockPos(blockX, 0, blockZ), fitnessWithDistance);
        }

        private record Result(BlockPos location, long fitness) {
        }
    }

    public static class ParameterList<T> {
        private final List<Pair<ParameterPoint, T>> values;
        private final RTree<T> index;

        public static <T> Codec<ParameterList<T>> codec(MapCodec<T> valueCodec) {
            return ExtraCodecs.nonEmptyList(RecordCodecBuilder.create(i -> i.group((App)ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), (App)valueCodec.forGetter(Pair::getSecond)).apply((Applicative)i, Pair::of)).listOf()).xmap(ParameterList::new, ParameterList::values);
        }

        public ParameterList(List<Pair<ParameterPoint, T>> values) {
            this.values = values;
            this.index = RTree.create(values);
        }

        public List<Pair<ParameterPoint, T>> values() {
            return this.values;
        }

        public T findValue(TargetPoint target) {
            return this.findValueIndex(target);
        }

        @VisibleForTesting
        public T findValueBruteForce(TargetPoint target) {
            Iterator<Pair<ParameterPoint, T>> iterator = this.values().iterator();
            Pair<ParameterPoint, T> first = iterator.next();
            long bestFitness = ((ParameterPoint)first.getFirst()).fitness(target);
            Object best = first.getSecond();
            while (iterator.hasNext()) {
                Pair<ParameterPoint, T> parameter = iterator.next();
                long fitness = ((ParameterPoint)parameter.getFirst()).fitness(target);
                if (fitness >= bestFitness) continue;
                bestFitness = fitness;
                best = parameter.getSecond();
            }
            return (T)best;
        }

        public T findValueIndex(TargetPoint target) {
            return this.findValueIndex(target, RTree.Node::distance);
        }

        protected T findValueIndex(TargetPoint target, DistanceMetric<T> distanceMetric) {
            return this.index.search(target, distanceMetric);
        }
    }

    protected static final class RTree<T> {
        private static final int CHILDREN_PER_NODE = 6;
        private final Node<T> root;
        private final ThreadLocal<@Nullable Leaf<T>> lastResult = new ThreadLocal();

        private RTree(Node<T> root) {
            this.root = root;
        }

        public static <T> RTree<T> create(List<Pair<ParameterPoint, T>> values) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Need at least one value to build the search tree.");
            }
            int dimensions = ((ParameterPoint)values.get(0).getFirst()).parameterSpace().size();
            if (dimensions != 7) {
                throw new IllegalStateException("Expecting parameter space to be 7, got " + dimensions);
            }
            List leaves = values.stream().map(p -> new Leaf<Object>((ParameterPoint)p.getFirst(), p.getSecond())).collect(Collectors.toCollection(ArrayList::new));
            return new RTree<T>(RTree.build(dimensions, leaves));
        }

        private static <T> Node<T> build(int dimensions, List<? extends Node<T>> children) {
            if (children.isEmpty()) {
                throw new IllegalStateException("Need at least one child to build a node");
            }
            if (children.size() == 1) {
                return children.get(0);
            }
            if (children.size() <= 6) {
                children.sort(Comparator.comparingLong(leaf -> {
                    long totalMagnitude = 0L;
                    for (int d = 0; d < dimensions; ++d) {
                        Parameter parameter = leaf.parameterSpace[d];
                        totalMagnitude += Math.abs((parameter.min() + parameter.max()) / 2L);
                    }
                    return totalMagnitude;
                }));
                return new SubTree(children);
            }
            long minCost = Long.MAX_VALUE;
            int minDimension = -1;
            List<SubTree<T>> minBuckets = null;
            for (int d = 0; d < dimensions; ++d) {
                RTree.sort(children, dimensions, d, false);
                List<SubTree<T>> buckets = RTree.bucketize(children);
                long totalCost = 0L;
                for (SubTree<T> bucket : buckets) {
                    totalCost += RTree.cost(bucket.parameterSpace);
                }
                if (minCost <= totalCost) continue;
                minCost = totalCost;
                minDimension = d;
                minBuckets = buckets;
            }
            RTree.sort(minBuckets, dimensions, minDimension, true);
            return new SubTree(minBuckets.stream().map(b -> RTree.build(dimensions, Arrays.asList(b.children))).collect(Collectors.toList()));
        }

        private static <T> void sort(List<? extends Node<T>> children, int dimensions, int dimension, boolean absolute) {
            Comparator<Node<Node<T>>> comparator = RTree.comparator(dimension, absolute);
            for (int d = 1; d < dimensions; ++d) {
                comparator = comparator.thenComparing(RTree.comparator((dimension + d) % dimensions, absolute));
            }
            children.sort(comparator);
        }

        private static <T> Comparator<Node<T>> comparator(int dimension, boolean absolute) {
            return Comparator.comparingLong(leaf -> {
                Parameter parameter = leaf.parameterSpace[dimension];
                long center = (parameter.min() + parameter.max()) / 2L;
                return absolute ? Math.abs(center) : center;
            });
        }

        private static <T> List<SubTree<T>> bucketize(List<? extends Node<T>> nodes) {
            ArrayList buckets = Lists.newArrayList();
            ArrayList children = Lists.newArrayList();
            int expectedChildrenCount = (int)Math.pow(6.0, Math.floor(Math.log((double)nodes.size() - 0.01) / Math.log(6.0)));
            for (Node<T> child : nodes) {
                children.add(child);
                if (children.size() < expectedChildrenCount) continue;
                buckets.add(new SubTree(children));
                children = Lists.newArrayList();
            }
            if (!children.isEmpty()) {
                buckets.add(new SubTree(children));
            }
            return buckets;
        }

        private static long cost(Parameter[] parameterSpace) {
            long result = 0L;
            for (Parameter parameter : parameterSpace) {
                result += Math.abs(parameter.max() - parameter.min());
            }
            return result;
        }

        private static <T> List<Parameter> buildParameterSpace(List<? extends Node<T>> children) {
            if (children.isEmpty()) {
                throw new IllegalArgumentException("SubTree needs at least one child");
            }
            int dimensions = 7;
            ArrayList bounds = Lists.newArrayList();
            for (int d = 0; d < 7; ++d) {
                bounds.add(null);
            }
            for (Node<T> child : children) {
                for (int d = 0; d < 7; ++d) {
                    bounds.set(d, child.parameterSpace[d].span((Parameter)bounds.get(d)));
                }
            }
            return bounds;
        }

        public T search(TargetPoint target, DistanceMetric<T> distanceMetric) {
            long[] targetArray = target.toParameterArray();
            Leaf<T> leaf = this.root.search(targetArray, this.lastResult.get(), distanceMetric);
            this.lastResult.set(leaf);
            return leaf.value;
        }

        @VisibleForTesting
        static abstract class Node<T> {
            protected final Parameter[] parameterSpace;

            protected Node(List<Parameter> parameterSpace) {
                this.parameterSpace = parameterSpace.toArray(new Parameter[0]);
            }

            protected abstract Leaf<T> search(long[] var1, @Nullable Leaf<T> var2, DistanceMetric<T> var3);

            protected long distance(long[] target) {
                long distance = 0L;
                for (int i = 0; i < 7; ++i) {
                    distance += Mth.square(this.parameterSpace[i].distance(target[i]));
                }
                return distance;
            }

            public String toString() {
                return Arrays.toString(this.parameterSpace);
            }
        }

        private static final class SubTree<T>
        extends Node<T> {
            private final Node<T>[] children;

            public SubTree(List<? extends Node<T>> children) {
                this(RTree.buildParameterSpace(children), children);
            }

            public SubTree(List<Parameter> parameterSpace, List<? extends Node<T>> children) {
                super(parameterSpace);
                this.children = children.toArray(new Node[0]);
            }

            @Override
            protected Leaf<T> search(long[] target, @Nullable Leaf<T> candidate, DistanceMetric<T> distanceMetric) {
                long minDistance = candidate == null ? Long.MAX_VALUE : distanceMetric.distance(candidate, target);
                Leaf<T> closestLeaf = candidate;
                for (Node<T> child : this.children) {
                    long leafDistance;
                    long childDistance = distanceMetric.distance(child, target);
                    if (minDistance <= childDistance) continue;
                    Leaf<T> leaf = child.search(target, closestLeaf, distanceMetric);
                    long l = leafDistance = child == leaf ? childDistance : distanceMetric.distance(leaf, target);
                    if (minDistance <= leafDistance) continue;
                    minDistance = leafDistance;
                    closestLeaf = leaf;
                }
                return closestLeaf;
            }
        }

        private static final class Leaf<T>
        extends Node<T> {
            private final T value;

            private Leaf(ParameterPoint parameterPoint, T value) {
                super(parameterPoint.parameterSpace());
                this.value = value;
            }

            @Override
            protected Leaf<T> search(long[] target, @Nullable Leaf<T> candidate, DistanceMetric<T> distanceMetric) {
                return this;
            }
        }
    }

    @VisibleForTesting
    static interface DistanceMetric<T> {
        public long distance(RTree.Node<T> var1, long[] var2);
    }
}

