/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.Nullable;

public class SurfaceRules {
    public static final ConditionSource ON_FLOOR = SurfaceRules.stoneDepthCheck(0, false, CaveSurface.FLOOR);
    public static final ConditionSource UNDER_FLOOR = SurfaceRules.stoneDepthCheck(0, true, CaveSurface.FLOOR);
    public static final ConditionSource DEEP_UNDER_FLOOR = SurfaceRules.stoneDepthCheck(0, true, 6, CaveSurface.FLOOR);
    public static final ConditionSource VERY_DEEP_UNDER_FLOOR = SurfaceRules.stoneDepthCheck(0, true, 30, CaveSurface.FLOOR);
    public static final ConditionSource ON_CEILING = SurfaceRules.stoneDepthCheck(0, false, CaveSurface.CEILING);
    public static final ConditionSource UNDER_CEILING = SurfaceRules.stoneDepthCheck(0, true, CaveSurface.CEILING);

    public static ConditionSource stoneDepthCheck(int offset, boolean addSurfaceDepth1, CaveSurface surfaceType) {
        return new StoneDepthCheck(offset, addSurfaceDepth1, 0, surfaceType);
    }

    public static ConditionSource stoneDepthCheck(int offset, boolean addSurfaceDepth1, int secondaryDepthRange, CaveSurface surfaceType) {
        return new StoneDepthCheck(offset, addSurfaceDepth1, secondaryDepthRange, surfaceType);
    }

    public static ConditionSource not(ConditionSource target) {
        return new NotConditionSource(target);
    }

    public static ConditionSource yBlockCheck(VerticalAnchor anchor, int surfaceDepthMultiplier) {
        return new YConditionSource(anchor, surfaceDepthMultiplier, false);
    }

    public static ConditionSource yStartCheck(VerticalAnchor anchor, int surfaceDepthMultiplier) {
        return new YConditionSource(anchor, surfaceDepthMultiplier, true);
    }

    public static ConditionSource waterBlockCheck(int offset, int surfaceDepthMultiplier) {
        return new WaterConditionSource(offset, surfaceDepthMultiplier, false);
    }

    public static ConditionSource waterStartCheck(int offset, int surfaceDepthMultiplier) {
        return new WaterConditionSource(offset, surfaceDepthMultiplier, true);
    }

    @SafeVarargs
    public static ConditionSource isBiome(HolderGetter<Biome> biomes, ResourceKey<Biome> ... target) {
        return new BiomeConditionSource(HolderSet.direct(biomes::getOrThrow, target));
    }

    public static ConditionSource noiseCondition2d(ResourceKey<NormalNoise.NoiseParameters> noise, double minRange) {
        return SurfaceRules.noiseCondition2d(noise, minRange, Double.MAX_VALUE);
    }

    public static ConditionSource noiseCondition2d(ResourceKey<NormalNoise.NoiseParameters> noise, double minRange, double maxRange) {
        return new NoiseThresholdConditionSource(noise, minRange, maxRange, false);
    }

    public static ConditionSource noiseCondition3d(ResourceKey<NormalNoise.NoiseParameters> noise, double minRange) {
        return SurfaceRules.noiseCondition3d(noise, minRange, Double.MAX_VALUE);
    }

    public static ConditionSource noiseCondition3d(ResourceKey<NormalNoise.NoiseParameters> noise, double minRange, double maxRange) {
        return new NoiseThresholdConditionSource(noise, minRange, maxRange, true);
    }

    public static ConditionSource verticalGradient(String randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) {
        return new VerticalGradientConditionSource(Identifier.parse(randomName), trueAtAndBelow, falseAtAndAbove);
    }

    public static ConditionSource steep() {
        return Steep.INSTANCE;
    }

    public static ConditionSource hole() {
        return Hole.INSTANCE;
    }

    public static ConditionSource abovePreliminarySurface() {
        return AbovePreliminarySurface.INSTANCE;
    }

    public static ConditionSource temperature() {
        return Temperature.INSTANCE;
    }

    public static RuleSource ifTrue(ConditionSource condition, RuleSource next) {
        return new TestRuleSource(condition, next);
    }

    public static RuleSource sequence(RuleSource ... rules) {
        if (rules.length == 0) {
            throw new IllegalArgumentException("Need at least 1 rule for a sequence");
        }
        return new SequenceRuleSource(Arrays.asList(rules));
    }

    public static RuleSource state(BlockState state) {
        return new BlockRuleSource(state);
    }

    public static RuleSource bandlands() {
        return Bandlands.INSTANCE;
    }

    private static <A> MapCodec<? extends A> register(Registry<MapCodec<? extends A>> registry, String name, MapCodec<? extends A> codec) {
        return Registry.register(registry, name, codec);
    }

    private record StoneDepthCheck(int offset, boolean addSurfaceDepth, int secondaryDepthRange, CaveSurface surfaceType) implements ConditionSource
    {
        private static final MapCodec<StoneDepthCheck> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.fieldOf("offset").forGetter(StoneDepthCheck::offset), (App)Codec.BOOL.fieldOf("add_surface_depth").forGetter(StoneDepthCheck::addSurfaceDepth), (App)Codec.INT.fieldOf("secondary_depth_range").forGetter(StoneDepthCheck::secondaryDepthRange), (App)CaveSurface.CODEC.fieldOf("surface_type").forGetter(StoneDepthCheck::surfaceType)).apply((Applicative)i, StoneDepthCheck::new));

        public MapCodec<StoneDepthCheck> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(final Context ruleContext) {
            final boolean ceiling = this.surfaceType == CaveSurface.CEILING;
            class StoneDepthCondition
            extends LazyYCondition {
                final /* synthetic */ StoneDepthCheck this$0;

                private StoneDepthCondition() {
                    StoneDepthCheck stoneDepthCheck = this$0;
                    Objects.requireNonNull(stoneDepthCheck);
                    this.this$0 = stoneDepthCheck;
                    super(context);
                }

                @Override
                protected boolean compute() {
                    int stoneDepth = ceiling ? this.context.stoneDepthBelow : this.context.stoneDepthAbove;
                    int surfaceDepth = this.this$0.addSurfaceDepth ? this.context.surfaceDepth : 0;
                    int secondarySurfaceDepth = this.this$0.secondaryDepthRange == 0 ? 0 : (int)Mth.map(this.context.getSurfaceSecondary(), -1.0, 1.0, 0.0, (double)this.this$0.secondaryDepthRange);
                    return stoneDepth <= 1 + this.this$0.offset + surfaceDepth + secondarySurfaceDepth;
                }
            }
            return new StoneDepthCondition();
        }
    }

    private record NotConditionSource(ConditionSource target) implements ConditionSource
    {
        private static final MapCodec<NotConditionSource> CODEC = ConditionSource.CODEC.xmap(NotConditionSource::new, NotConditionSource::target).fieldOf("invert");

        public MapCodec<NotConditionSource> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(Context context) {
            return new NotCondition((Condition)this.target.apply(context));
        }
    }

    public static interface ConditionSource
    extends Function<Context, Condition> {
        public static final Codec<ConditionSource> CODEC = BuiltInRegistries.MATERIAL_CONDITION.byNameCodec().dispatch(ConditionSource::codec, Function.identity());

        public static MapCodec<? extends ConditionSource> bootstrap(Registry<MapCodec<? extends ConditionSource>> registry) {
            SurfaceRules.register(registry, "biome", BiomeConditionSource.CODEC);
            SurfaceRules.register(registry, "noise_threshold", NoiseThresholdConditionSource.CODEC);
            SurfaceRules.register(registry, "vertical_gradient", VerticalGradientConditionSource.CODEC);
            SurfaceRules.register(registry, "y_above", YConditionSource.CODEC);
            SurfaceRules.register(registry, "water", WaterConditionSource.CODEC);
            SurfaceRules.register(registry, "temperature", Temperature.CODEC);
            SurfaceRules.register(registry, "steep", Steep.CODEC);
            SurfaceRules.register(registry, "not", NotConditionSource.CODEC);
            SurfaceRules.register(registry, "hole", Hole.CODEC);
            SurfaceRules.register(registry, "above_preliminary_surface", AbovePreliminarySurface.CODEC);
            return SurfaceRules.register(registry, "stone_depth", StoneDepthCheck.CODEC);
        }

        public MapCodec<? extends ConditionSource> codec();
    }

    private record YConditionSource(VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) implements ConditionSource
    {
        private static final MapCodec<YConditionSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)VerticalAnchor.CODEC.fieldOf("anchor").forGetter(YConditionSource::anchor), (App)Codec.intRange((int)-20, (int)20).fieldOf("surface_depth_multiplier").forGetter(YConditionSource::surfaceDepthMultiplier), (App)Codec.BOOL.fieldOf("add_stone_depth").forGetter(YConditionSource::addStoneDepth)).apply((Applicative)i, YConditionSource::new));

        public MapCodec<YConditionSource> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(final Context ruleContext) {
            class YCondition
            extends LazyYCondition {
                final /* synthetic */ YConditionSource this$0;

                private YCondition() {
                    YConditionSource yConditionSource = this$0;
                    Objects.requireNonNull(yConditionSource);
                    this.this$0 = yConditionSource;
                    super(context);
                }

                @Override
                protected boolean compute() {
                    return this.context.blockY + (this.this$0.addStoneDepth ? this.context.stoneDepthAbove : 0) >= this.this$0.anchor.resolveY(this.context.context) + this.context.surfaceDepth * this.this$0.surfaceDepthMultiplier;
                }
            }
            return new YCondition();
        }
    }

    private record WaterConditionSource(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) implements ConditionSource
    {
        private static final MapCodec<WaterConditionSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.fieldOf("offset").forGetter(WaterConditionSource::offset), (App)Codec.intRange((int)-20, (int)20).fieldOf("surface_depth_multiplier").forGetter(WaterConditionSource::surfaceDepthMultiplier), (App)Codec.BOOL.fieldOf("add_stone_depth").forGetter(WaterConditionSource::addStoneDepth)).apply((Applicative)i, WaterConditionSource::new));

        public MapCodec<WaterConditionSource> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(final Context ruleContext) {
            class WaterCondition
            extends LazyYCondition {
                final /* synthetic */ WaterConditionSource this$0;

                private WaterCondition() {
                    WaterConditionSource waterConditionSource = this$0;
                    Objects.requireNonNull(waterConditionSource);
                    this.this$0 = waterConditionSource;
                    super(context);
                }

                @Override
                protected boolean compute() {
                    return this.context.waterHeight == Integer.MIN_VALUE || this.context.blockY + (this.this$0.addStoneDepth ? this.context.stoneDepthAbove : 0) >= this.context.waterHeight + this.this$0.offset + this.context.surfaceDepth * this.this$0.surfaceDepthMultiplier;
                }
            }
            return new WaterCondition();
        }
    }

    private record BiomeConditionSource(HolderSet<Biome> biomes) implements ConditionSource
    {
        private static final MapCodec<BiomeConditionSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biome_is").forGetter(BiomeConditionSource::biomes)).apply((Applicative)i, BiomeConditionSource::new));

        public MapCodec<BiomeConditionSource> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(final Context ruleContext) {
            if (ruleContext.possibleBiomes != null) {
                if (this.canNeverMatch(ruleContext.possibleBiomes)) {
                    return () -> false;
                }
                if (this.willAlwaysMatch(ruleContext.possibleBiomes)) {
                    return () -> true;
                }
            }
            class BiomeCondition
            extends LazyYCondition {
                final /* synthetic */ BiomeConditionSource this$0;

                private BiomeCondition() {
                    BiomeConditionSource biomeConditionSource = this$0;
                    Objects.requireNonNull(biomeConditionSource);
                    this.this$0 = biomeConditionSource;
                    super(context);
                }

                @Override
                protected boolean compute() {
                    return this.this$0.biomes.contains(this.context.getBiome());
                }
            }
            return new BiomeCondition();
        }

        private boolean canNeverMatch(Set<Holder<Biome>> possibleBiomes) {
            for (Holder holder : this.biomes) {
                if (!possibleBiomes.contains(holder)) continue;
                return false;
            }
            return true;
        }

        private boolean willAlwaysMatch(Set<Holder<Biome>> possibleBiomes) {
            for (Holder<Biome> possibleBiome : possibleBiomes) {
                if (this.biomes.contains(possibleBiome)) continue;
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "BiomeConditionSource[biomes=" + String.valueOf(this.biomes) + "]";
        }
    }

    private record NoiseThresholdConditionSource(ResourceKey<NormalNoise.NoiseParameters> noise, double minThreshold, double maxThreshold, boolean is3d) implements ConditionSource
    {
        private static final MapCodec<NoiseThresholdConditionSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(NoiseThresholdConditionSource::noise), (App)Codec.DOUBLE.fieldOf("min_threshold").forGetter(NoiseThresholdConditionSource::minThreshold), (App)Codec.DOUBLE.fieldOf("max_threshold").forGetter(NoiseThresholdConditionSource::maxThreshold), (App)Codec.BOOL.optionalFieldOf("is_3d", (Object)false).forGetter(NoiseThresholdConditionSource::is3d)).apply((Applicative)i, NoiseThresholdConditionSource::new));

        public MapCodec<NoiseThresholdConditionSource> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(Context ruleContext) {
            final DoubleSupplier noise = ruleContext.getNoiseSampler(this.noise, this.is3d);
            class NoiseThresholdCondition
            implements Condition {
                final /* synthetic */ NoiseThresholdConditionSource this$0;

                NoiseThresholdCondition() {
                    NoiseThresholdConditionSource noiseThresholdConditionSource = this$0;
                    Objects.requireNonNull(noiseThresholdConditionSource);
                    this.this$0 = noiseThresholdConditionSource;
                }

                @Override
                public boolean test() {
                    double value = noise.getAsDouble();
                    return value >= this.this$0.minThreshold && value <= this.this$0.maxThreshold;
                }
            }
            return new NoiseThresholdCondition();
        }
    }

    private record VerticalGradientConditionSource(Identifier randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) implements ConditionSource
    {
        private static final MapCodec<VerticalGradientConditionSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("random_name").forGetter(VerticalGradientConditionSource::randomName), (App)VerticalAnchor.CODEC.fieldOf("true_at_and_below").forGetter(VerticalGradientConditionSource::trueAtAndBelow), (App)VerticalAnchor.CODEC.fieldOf("false_at_and_above").forGetter(VerticalGradientConditionSource::falseAtAndAbove)).apply((Applicative)i, VerticalGradientConditionSource::new));

        public MapCodec<VerticalGradientConditionSource> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(final Context ruleContext) {
            final int trueAtAndBelow = this.trueAtAndBelow().resolveY(ruleContext.context);
            final int falseAtAndAbove = this.falseAtAndAbove().resolveY(ruleContext.context);
            final PositionalRandomFactory randomFactory = ruleContext.randomState.getOrCreateRandomFactory(this.randomName());
            class VerticalGradientCondition
            extends LazyYCondition {
                private VerticalGradientCondition() {
                    Objects.requireNonNull(this$0);
                    super(context);
                }

                @Override
                protected boolean compute() {
                    int blockY = this.context.blockY;
                    if (blockY <= trueAtAndBelow) {
                        return true;
                    }
                    if (blockY >= falseAtAndAbove) {
                        return false;
                    }
                    double probability = Mth.map((double)blockY, (double)trueAtAndBelow, (double)falseAtAndAbove, 1.0, 0.0);
                    RandomSource random = randomFactory.at(this.context.blockX, blockY, this.context.blockZ);
                    return (double)random.nextFloat() < probability;
                }
            }
            return new VerticalGradientCondition();
        }
    }

    private static enum Steep implements ConditionSource
    {
        INSTANCE;

        private static final MapCodec<Steep> CODEC;

        public MapCodec<Steep> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(Context context) {
            return context.steep;
        }

        static {
            CODEC = MapCodec.unit((Object)INSTANCE);
        }
    }

    private static enum Hole implements ConditionSource
    {
        INSTANCE;

        private static final MapCodec<Hole> CODEC;

        public MapCodec<Hole> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(Context context) {
            return context.hole;
        }

        static {
            CODEC = MapCodec.unit((Object)INSTANCE);
        }
    }

    private static enum AbovePreliminarySurface implements ConditionSource
    {
        INSTANCE;

        private static final MapCodec<AbovePreliminarySurface> CODEC;

        public MapCodec<AbovePreliminarySurface> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(Context context) {
            return context.abovePreliminarySurface;
        }

        static {
            CODEC = MapCodec.unit((Object)INSTANCE);
        }
    }

    private static enum Temperature implements ConditionSource
    {
        INSTANCE;

        private static final MapCodec<Temperature> CODEC;

        public MapCodec<Temperature> codec() {
            return CODEC;
        }

        @Override
        public Condition apply(Context context) {
            return context.temperature;
        }

        static {
            CODEC = MapCodec.unit((Object)INSTANCE);
        }
    }

    private record TestRuleSource(ConditionSource ifTrue, RuleSource thenRun) implements RuleSource
    {
        private static final MapCodec<TestRuleSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ConditionSource.CODEC.fieldOf("if_true").forGetter(TestRuleSource::ifTrue), (App)RuleSource.CODEC.fieldOf("then_run").forGetter(TestRuleSource::thenRun)).apply((Applicative)i, TestRuleSource::new));

        public MapCodec<TestRuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRule apply(Context context) {
            return new TestRule((Condition)this.ifTrue.apply(context), (SurfaceRule)this.thenRun.apply(context));
        }
    }

    public static interface RuleSource
    extends Function<Context, SurfaceRule> {
        public static final Codec<RuleSource> CODEC = BuiltInRegistries.MATERIAL_RULE.byNameCodec().dispatch(RuleSource::codec, Function.identity());

        public static MapCodec<? extends RuleSource> bootstrap(Registry<MapCodec<? extends RuleSource>> registry) {
            SurfaceRules.register(registry, "bandlands", Bandlands.CODEC);
            SurfaceRules.register(registry, "block", BlockRuleSource.CODEC);
            SurfaceRules.register(registry, "sequence", SequenceRuleSource.CODEC);
            return SurfaceRules.register(registry, "condition", TestRuleSource.CODEC);
        }

        public MapCodec<? extends RuleSource> codec();
    }

    private record SequenceRuleSource(List<RuleSource> sequence) implements RuleSource
    {
        private static final MapCodec<SequenceRuleSource> CODEC = RuleSource.CODEC.listOf().xmap(SequenceRuleSource::new, SequenceRuleSource::sequence).fieldOf("sequence");

        public MapCodec<SequenceRuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRule apply(Context context) {
            if (this.sequence.size() == 1) {
                return (SurfaceRule)this.sequence.get(0).apply(context);
            }
            ImmutableList.Builder builder = ImmutableList.builder();
            for (RuleSource rule : this.sequence) {
                builder.add((Object)((SurfaceRule)rule.apply(context)));
            }
            return new SequenceRule((List<SurfaceRule>)builder.build());
        }
    }

    private record BlockRuleSource(BlockState resultState, StateRule rule) implements RuleSource
    {
        private static final MapCodec<BlockRuleSource> CODEC = BlockState.CODEC.xmap(BlockRuleSource::new, BlockRuleSource::resultState).fieldOf("result_state");

        private BlockRuleSource(BlockState state) {
            this(state, new StateRule(state));
        }

        public MapCodec<BlockRuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRule apply(Context context) {
            return this.rule;
        }
    }

    private static enum Bandlands implements RuleSource
    {
        INSTANCE;

        private static final MapCodec<Bandlands> CODEC;

        public MapCodec<Bandlands> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRule apply(Context context) {
            return context.system::getBand;
        }

        static {
            CODEC = MapCodec.unit((Object)INSTANCE);
        }
    }

    private record SequenceRule(List<SurfaceRule> rules) implements SurfaceRule
    {
        @Override
        public @Nullable BlockState tryApply(int blockX, int blockY, int blockZ) {
            for (SurfaceRule rule : this.rules) {
                BlockState state = rule.tryApply(blockX, blockY, blockZ);
                if (state == null) continue;
                return state;
            }
            return null;
        }
    }

    private record TestRule(Condition condition, SurfaceRule followup) implements SurfaceRule
    {
        @Override
        public @Nullable BlockState tryApply(int blockX, int blockY, int blockZ) {
            if (!this.condition.test()) {
                return null;
            }
            return this.followup.tryApply(blockX, blockY, blockZ);
        }
    }

    private record StateRule(BlockState state) implements SurfaceRule
    {
        @Override
        public BlockState tryApply(int blockX, int blockY, int blockZ) {
            return this.state;
        }
    }

    protected static interface SurfaceRule {
        public @Nullable BlockState tryApply(int var1, int var2, int var3);
    }

    private record NotCondition(Condition target) implements Condition
    {
        @Override
        public boolean test() {
            return !this.target.test();
        }
    }

    private static abstract class LazyYCondition
    extends LazyCondition {
        protected LazyYCondition(Context context) {
            super(context);
        }

        @Override
        protected long getContextLastUpdate() {
            return this.context.lastUpdateY;
        }
    }

    private static abstract class LazyXZCondition
    extends LazyCondition {
        protected LazyXZCondition(Context context) {
            super(context);
        }

        @Override
        protected long getContextLastUpdate() {
            return this.context.lastUpdateXZ;
        }
    }

    private static abstract class LazyCondition
    implements Condition {
        protected final Context context;
        private long lastUpdate;
        private @Nullable Boolean result;

        protected LazyCondition(Context context) {
            this.context = context;
            this.lastUpdate = this.getContextLastUpdate() - 1L;
        }

        @Override
        public boolean test() {
            long lastContextUpdate = this.getContextLastUpdate();
            if (lastContextUpdate == this.lastUpdate) {
                if (this.result == null) {
                    throw new IllegalStateException("Update triggered but the result is null");
                }
                return this.result;
            }
            this.lastUpdate = lastContextUpdate;
            this.result = this.compute();
            return this.result;
        }

        protected abstract long getContextLastUpdate();

        protected abstract boolean compute();
    }

    private static interface Condition {
        public boolean test();
    }

    protected static final class Context {
        private static final int HOW_FAR_BELOW_PRELIMINARY_SURFACE_LEVEL_TO_BUILD_SURFACE = 8;
        private static final int SURFACE_CELL_BITS = 4;
        private static final int SURFACE_CELL_SIZE = 16;
        private static final int SURFACE_CELL_MASK = 15;
        private final SurfaceSystem system;
        private final Condition temperature = new TemperatureHelperCondition(this);
        private final Condition steep = new SteepMaterialCondition(this);
        private final Condition hole = new HoleCondition(this);
        private final Condition abovePreliminarySurface = new AbovePreliminarySurfaceCondition(this);
        private final RandomState randomState;
        private final ChunkAccess chunk;
        private final NoiseChunk noiseChunk;
        private final Function<BlockPos, Holder<Biome>> biomeGetter;
        private final WorldGenerationContext context;
        private final @Nullable Set<Holder<Biome>> possibleBiomes;
        private long lastPreliminarySurfaceCellOrigin = Long.MAX_VALUE;
        private final int[] preliminarySurfaceCache = new int[4];
        private final Map<ResourceKey<NormalNoise.NoiseParameters>, DoubleSupplier> noiseSamplers2d = new IdentityHashMap<ResourceKey<NormalNoise.NoiseParameters>, DoubleSupplier>();
        private final Map<ResourceKey<NormalNoise.NoiseParameters>, DoubleSupplier> noiseSamplers3d = new IdentityHashMap<ResourceKey<NormalNoise.NoiseParameters>, DoubleSupplier>();
        private long lastUpdateXZ = -9223372036854775807L;
        private int blockX;
        private int blockZ;
        private int surfaceDepth;
        private long lastSurfaceDepth2Update = this.lastUpdateXZ - 1L;
        private double surfaceSecondary;
        private long lastMinSurfaceLevelUpdate = this.lastUpdateXZ - 1L;
        private int minSurfaceLevel;
        private long lastUpdateY = -9223372036854775807L;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private @Nullable Holder<Biome> biome;
        private int blockY;
        private int waterHeight;
        private int stoneDepthBelow;
        private int stoneDepthAbove;

        protected Context(SurfaceSystem system, RandomState randomState, ChunkAccess chunk, NoiseChunk noiseChunk, Function<BlockPos, Holder<Biome>> biomeGetter, WorldGenerationContext context, @Nullable Set<Holder<Biome>> possibleBiomes) {
            this.system = system;
            this.randomState = randomState;
            this.chunk = chunk;
            this.noiseChunk = noiseChunk;
            this.biomeGetter = biomeGetter;
            this.context = context;
            this.possibleBiomes = possibleBiomes;
        }

        protected void updateXZ(int blockX, int blockZ) {
            ++this.lastUpdateXZ;
            ++this.lastUpdateY;
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.surfaceDepth = this.system.getSurfaceDepth(blockX, blockZ);
        }

        protected void updateY(int stoneDepthAbove, int stoneDepthBelow, int waterHeight, int blockY) {
            ++this.lastUpdateY;
            this.biome = null;
            this.blockY = blockY;
            this.waterHeight = waterHeight;
            this.stoneDepthBelow = stoneDepthBelow;
            this.stoneDepthAbove = stoneDepthAbove;
        }

        protected double getSurfaceSecondary() {
            if (this.lastSurfaceDepth2Update != this.lastUpdateXZ) {
                this.lastSurfaceDepth2Update = this.lastUpdateXZ;
                this.surfaceSecondary = this.system.getSurfaceSecondary(this.blockX, this.blockZ);
            }
            return this.surfaceSecondary;
        }

        protected Holder<Biome> getBiome() {
            if (this.biome == null) {
                this.biome = this.biomeGetter.apply(this.pos.set(this.blockX, this.blockY, this.blockZ));
            }
            return this.biome;
        }

        public int getSeaLevel() {
            return this.system.getSeaLevel();
        }

        private static int blockCoordToSurfaceCell(int blockCoord) {
            return blockCoord >> 4;
        }

        private static int surfaceCellToBlockCoord(int cellCoord) {
            return cellCoord << 4;
        }

        protected int getMinSurfaceLevel() {
            if (this.lastMinSurfaceLevelUpdate != this.lastUpdateXZ) {
                int cornerCellZ;
                this.lastMinSurfaceLevelUpdate = this.lastUpdateXZ;
                int cornerCellX = Context.blockCoordToSurfaceCell(this.blockX);
                long preliminarySurfaceCellOrigin = ChunkPos.pack(cornerCellX, cornerCellZ = Context.blockCoordToSurfaceCell(this.blockZ));
                if (this.lastPreliminarySurfaceCellOrigin != preliminarySurfaceCellOrigin) {
                    this.lastPreliminarySurfaceCellOrigin = preliminarySurfaceCellOrigin;
                    this.preliminarySurfaceCache[0] = this.noiseChunk.preliminarySurfaceLevel(Context.surfaceCellToBlockCoord(cornerCellX), Context.surfaceCellToBlockCoord(cornerCellZ));
                    this.preliminarySurfaceCache[1] = this.noiseChunk.preliminarySurfaceLevel(Context.surfaceCellToBlockCoord(cornerCellX + 1), Context.surfaceCellToBlockCoord(cornerCellZ));
                    this.preliminarySurfaceCache[2] = this.noiseChunk.preliminarySurfaceLevel(Context.surfaceCellToBlockCoord(cornerCellX), Context.surfaceCellToBlockCoord(cornerCellZ + 1));
                    this.preliminarySurfaceCache[3] = this.noiseChunk.preliminarySurfaceLevel(Context.surfaceCellToBlockCoord(cornerCellX + 1), Context.surfaceCellToBlockCoord(cornerCellZ + 1));
                }
                int preliminarySurfaceLevel = Mth.floor(Mth.lerp2((float)(this.blockX & 0xF) / 16.0f, (float)(this.blockZ & 0xF) / 16.0f, this.preliminarySurfaceCache[0], this.preliminarySurfaceCache[1], this.preliminarySurfaceCache[2], this.preliminarySurfaceCache[3]));
                this.minSurfaceLevel = preliminarySurfaceLevel + this.surfaceDepth - 8;
            }
            return this.minSurfaceLevel;
        }

        protected DoubleSupplier getNoiseSampler(ResourceKey<NormalNoise.NoiseParameters> noiseId, boolean is3d) {
            if (is3d) {
                return this.noiseSamplers3d.computeIfAbsent(noiseId, this::createNoiseSampler3d);
            }
            return this.noiseSamplers2d.computeIfAbsent(noiseId, this::createNoiseSampler2d);
        }

        private DoubleSupplier createNoiseSampler2d(ResourceKey<NormalNoise.NoiseParameters> noiseId) {
            final NormalNoise noise = this.randomState.getOrCreateNoise(noiseId);
            return new DoubleSupplier(){
                private long lastUpdateXZ;
                private double lastNoise;
                final /* synthetic */ Context this$0;
                {
                    Context context = this$0;
                    Objects.requireNonNull(context);
                    this.this$0 = context;
                    this.lastUpdateXZ = this.this$0.lastUpdateXZ - 1L;
                }

                @Override
                public double getAsDouble() {
                    if (this.lastUpdateXZ != this.this$0.lastUpdateXZ) {
                        this.lastNoise = noise.getValue(this.this$0.blockX, 0.0, this.this$0.blockZ);
                        this.lastUpdateXZ = this.this$0.lastUpdateXZ;
                    }
                    return this.lastNoise;
                }
            };
        }

        private DoubleSupplier createNoiseSampler3d(ResourceKey<NormalNoise.NoiseParameters> noiseId) {
            final NormalNoise noise = this.randomState.getOrCreateNoise(noiseId);
            return new DoubleSupplier(){
                private long lastUpdateY;
                private double lastNoise;
                final /* synthetic */ Context this$0;
                {
                    Context context = this$0;
                    Objects.requireNonNull(context);
                    this.this$0 = context;
                    this.lastUpdateY = this.this$0.lastUpdateY - 1L;
                }

                @Override
                public double getAsDouble() {
                    if (this.lastUpdateY != this.this$0.lastUpdateY) {
                        this.lastNoise = noise.getValue(this.this$0.blockX, this.this$0.blockY, this.this$0.blockZ);
                        this.lastUpdateY = this.this$0.lastUpdateY;
                    }
                    return this.lastNoise;
                }
            };
        }

        private static class TemperatureHelperCondition
        extends LazyYCondition {
            private TemperatureHelperCondition(Context context) {
                super(context);
            }

            @Override
            protected boolean compute() {
                return this.context.getBiome().value().coldEnoughToSnow(this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ), this.context.getSeaLevel());
            }
        }

        private static class SteepMaterialCondition
        extends LazyXZCondition {
            private SteepMaterialCondition(Context context) {
                super(context);
            }

            @Override
            protected boolean compute() {
                int heightEast;
                int chunkBlockX = this.context.blockX & 0xF;
                int chunkBlockZ = this.context.blockZ & 0xF;
                int zNorth = Math.max(chunkBlockZ - 1, 0);
                int zSouth = Math.min(chunkBlockZ + 1, 15);
                ChunkAccess chunk = this.context.chunk;
                int heightNorth = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chunkBlockX, zNorth);
                int heightSouth = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chunkBlockX, zSouth);
                if (heightSouth >= heightNorth + 4) {
                    return true;
                }
                int xWest = Math.max(chunkBlockX - 1, 0);
                int xEast = Math.min(chunkBlockX + 1, 15);
                int heightWest = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, xWest, chunkBlockZ);
                return heightWest >= (heightEast = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, xEast, chunkBlockZ)) + 4;
            }
        }

        private static final class HoleCondition
        extends LazyXZCondition {
            private HoleCondition(Context context) {
                super(context);
            }

            @Override
            protected boolean compute() {
                return this.context.surfaceDepth <= 0;
            }
        }

        private final class AbovePreliminarySurfaceCondition
        implements Condition {
            final /* synthetic */ Context this$0;

            private AbovePreliminarySurfaceCondition(Context context) {
                Context context2 = context;
                Objects.requireNonNull(context2);
                this.this$0 = context2;
            }

            @Override
            public boolean test() {
                return this.this$0.blockY >= this.this$0.getMinSurfaceLevel();
            }
        }
    }
}

