/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.longs.Long2IntMap
 *  it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;
import org.jspecify.annotations.Nullable;

public class NoiseChunk
implements DensityFunction.FunctionContext,
DensityFunction.ContextProvider {
    private final int cellCountXZ;
    private final int cellCountY;
    private final int cellNoiseMinY;
    private final int firstCellX;
    private final int firstCellZ;
    private final int firstNoiseX;
    private final int firstNoiseZ;
    private final List<NoiseInterpolator> interpolators;
    private final List<CacheAllInCell> cellCaches;
    private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<DensityFunction, DensityFunction>();
    private final Long2IntMap preliminarySurfaceLevelCache = new Long2IntOpenHashMap();
    private final Aquifer aquifer;
    private final DensityFunction preliminarySurfaceLevel;
    private final DensityFunction fullNoiseDensity;
    private final BlockStateFiller blockStateRule;
    private final Blender blender;
    private final @Nullable FlatCache blendAlpha;
    private final @Nullable FlatCache blendOffset;
    private final DensityFunctions.BeardifierOrMarker beardifier;
    private long lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
    private Blender.BlendingOutput lastBlendingOutput = new Blender.BlendingOutput(1.0, 0.0);
    private final int noiseSizeXZ;
    private final int cellWidth;
    private final int cellHeight;
    private boolean interpolating;
    private boolean fillingCell;
    private int cellStartBlockX;
    private int cellStartBlockY;
    private int cellStartBlockZ;
    private int inCellX;
    private int inCellY;
    private int inCellZ;
    private long interpolationCounter;
    private long arrayInterpolationCounter;
    private int arrayIndex;
    private final DensityFunction.ContextProvider sliceFillingContextProvider = new DensityFunction.ContextProvider(this){
        final /* synthetic */ NoiseChunk this$0;
        {
            NoiseChunk noiseChunk = this$0;
            Objects.requireNonNull(noiseChunk);
            this.this$0 = noiseChunk;
        }

        @Override
        public DensityFunction.FunctionContext forIndex(int cellYIndex) {
            this.this$0.cellStartBlockY = (cellYIndex + this.this$0.cellNoiseMinY) * this.this$0.cellHeight;
            ++this.this$0.interpolationCounter;
            this.this$0.inCellY = 0;
            this.this$0.arrayIndex = cellYIndex;
            return this.this$0;
        }

        @Override
        public void fillAllDirectly(double[] output, DensityFunction function) {
            for (int cellYIndex = 0; cellYIndex < this.this$0.cellCountY + 1; ++cellYIndex) {
                this.this$0.cellStartBlockY = (cellYIndex + this.this$0.cellNoiseMinY) * this.this$0.cellHeight;
                ++this.this$0.interpolationCounter;
                this.this$0.inCellY = 0;
                this.this$0.arrayIndex = cellYIndex;
                output[cellYIndex] = function.compute(this.this$0);
            }
        }
    };

    public static NoiseChunk forChunk(ChunkAccess chunk, RandomState randomState, DensityFunctions.BeardifierOrMarker beardifier, NoiseGeneratorSettings settings, Aquifer.FluidPicker globalFluidPicker, Blender blender) {
        NoiseSettings noiseSettings = settings.noiseSettings().clampToHeightAccessor(chunk);
        ChunkPos pos = chunk.getPos();
        int cellCountXZ = 16 / noiseSettings.getCellWidth();
        return new NoiseChunk(cellCountXZ, randomState, pos.getMinBlockX(), pos.getMinBlockZ(), noiseSettings, beardifier, settings, globalFluidPicker, blender);
    }

    public NoiseChunk(int cellCountXZ, RandomState randomState, int chunkMinBlockX, int chunkMinBlockZ, NoiseSettings noiseSettings, DensityFunctions.BeardifierOrMarker beardifier, NoiseGeneratorSettings settings, Aquifer.FluidPicker globalFluidPicker, Blender blender) {
        DensityFunction fullNoiseValue;
        this.cellWidth = noiseSettings.getCellWidth();
        this.cellHeight = noiseSettings.getCellHeight();
        this.cellCountXZ = cellCountXZ;
        this.cellCountY = Mth.floorDiv(noiseSettings.height(), this.cellHeight);
        this.cellNoiseMinY = Mth.floorDiv(noiseSettings.minY(), this.cellHeight);
        this.firstCellX = Math.floorDiv(chunkMinBlockX, this.cellWidth);
        this.firstCellZ = Math.floorDiv(chunkMinBlockZ, this.cellWidth);
        this.interpolators = Lists.newArrayList();
        this.cellCaches = Lists.newArrayList();
        this.firstNoiseX = QuartPos.fromBlock(chunkMinBlockX);
        this.firstNoiseZ = QuartPos.fromBlock(chunkMinBlockZ);
        this.noiseSizeXZ = QuartPos.fromBlock(cellCountXZ * this.cellWidth);
        this.blender = blender;
        this.beardifier = beardifier;
        if (!blender.isEmpty()) {
            this.blendAlpha = new FlatCache(this, new BlendAlpha(this), false);
            this.blendOffset = new FlatCache(this, new BlendOffset(this), false);
            for (int x = 0; x <= this.noiseSizeXZ; ++x) {
                int quartX = this.firstNoiseX + x;
                int blockX = QuartPos.toBlock(quartX);
                for (int z = 0; z <= this.noiseSizeXZ; ++z) {
                    int quartZ = this.firstNoiseZ + z;
                    int blockZ = QuartPos.toBlock(quartZ);
                    Blender.BlendingOutput blendingOutput = blender.blendOffsetAndFactor(blockX, blockZ);
                    this.blendAlpha.values[x + z * this.blendAlpha.sizeXZ] = blendingOutput.alpha();
                    this.blendOffset.values[x + z * this.blendOffset.sizeXZ] = blendingOutput.blendingOffset();
                }
            }
        } else {
            this.blendAlpha = null;
            this.blendOffset = null;
        }
        NoiseRouter router = randomState.router();
        NoiseRouter wrappedRouter = router.mapAll(this::wrap);
        this.preliminarySurfaceLevel = wrappedRouter.preliminarySurfaceLevel();
        if (!settings.isAquifersEnabled()) {
            this.aquifer = Aquifer.createDisabled(globalFluidPicker);
        } else {
            int chunkX = SectionPos.blockToSectionCoord(chunkMinBlockX);
            int chunkZ = SectionPos.blockToSectionCoord(chunkMinBlockZ);
            this.aquifer = Aquifer.create(this, new ChunkPos(chunkX, chunkZ), wrappedRouter, randomState.aquiferRandom(), noiseSettings.minY(), noiseSettings.height(), globalFluidPicker);
        }
        ArrayList<BlockStateFiller> builder = new ArrayList<BlockStateFiller>();
        this.fullNoiseDensity = fullNoiseValue = DensityFunctions.cacheAllInCell(DensityFunctions.add(wrappedRouter.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE)).mapAll(this::wrap);
        builder.add(context -> this.aquifer.computeSubstance(context, fullNoiseValue.compute(context)));
        if (settings.oreVeinsEnabled()) {
            builder.add(OreVeinifier.create(wrappedRouter.veinToggle(), wrappedRouter.veinRidged(), wrappedRouter.veinGap(), randomState.oreRandom()));
        }
        this.blockStateRule = new MaterialRuleList(builder.toArray(new BlockStateFiller[0]));
    }

    protected Climate.Sampler cachedClimateSampler(NoiseRouter noises, List<Climate.ParameterPoint> spawnTarget) {
        return new Climate.Sampler(noises.temperature().mapAll(this::wrap), noises.vegetation().mapAll(this::wrap), noises.continents().mapAll(this::wrap), noises.erosion().mapAll(this::wrap), noises.depth().mapAll(this::wrap), noises.ridges().mapAll(this::wrap), spawnTarget);
    }

    protected @Nullable BlockState getInterpolatedState() {
        return this.blockStateRule.calculate(this);
    }

    protected double getInterpolatedDensity() {
        return this.fullNoiseDensity.compute(this);
    }

    @Override
    public int blockX() {
        return this.cellStartBlockX + this.inCellX;
    }

    @Override
    public int blockY() {
        return this.cellStartBlockY + this.inCellY;
    }

    @Override
    public int blockZ() {
        return this.cellStartBlockZ + this.inCellZ;
    }

    public int maxPreliminarySurfaceLevel(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int maxY = Integer.MIN_VALUE;
        for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ += 4) {
            for (int blockX = minBlockX; blockX <= maxBlockX; blockX += 4) {
                int surfaceLevel = this.preliminarySurfaceLevel(blockX, blockZ);
                if (surfaceLevel <= maxY) continue;
                maxY = surfaceLevel;
            }
        }
        return maxY;
    }

    public int preliminarySurfaceLevel(int sampleX, int sampleZ) {
        int quantizedX = QuartPos.toBlock(QuartPos.fromBlock(sampleX));
        int quantizedZ = QuartPos.toBlock(QuartPos.fromBlock(sampleZ));
        return this.preliminarySurfaceLevelCache.computeIfAbsent(ColumnPos.asLong(quantizedX, quantizedZ), this::computePreliminarySurfaceLevel);
    }

    private int computePreliminarySurfaceLevel(long key) {
        int blockX = ColumnPos.getX(key);
        int blockZ = ColumnPos.getZ(key);
        return Mth.floor(this.preliminarySurfaceLevel.compute(new DensityFunction.SinglePointContext(blockX, 0, blockZ)));
    }

    private void fillSlice(boolean slice0, int cellX) {
        this.cellStartBlockX = cellX * this.cellWidth;
        this.inCellX = 0;
        for (int cellZIndex = 0; cellZIndex < this.cellCountXZ + 1; ++cellZIndex) {
            int cellZ = this.firstCellZ + cellZIndex;
            this.cellStartBlockZ = cellZ * this.cellWidth;
            this.inCellZ = 0;
            ++this.arrayInterpolationCounter;
            for (NoiseInterpolator noiseInterpolator : this.interpolators) {
                double[] slice = (slice0 ? noiseInterpolator.slice0 : noiseInterpolator.slice1)[cellZIndex];
                noiseInterpolator.fillArray(slice, this.sliceFillingContextProvider);
            }
        }
        ++this.arrayInterpolationCounter;
    }

    public void initializeForFirstCellX() {
        if (this.interpolating) {
            throw new IllegalStateException("Staring interpolation twice");
        }
        this.interpolating = true;
        this.interpolationCounter = 0L;
        this.fillSlice(true, this.firstCellX);
    }

    public void advanceCellX(int cellXIndex) {
        this.fillSlice(false, this.firstCellX + cellXIndex + 1);
        this.cellStartBlockX = (this.firstCellX + cellXIndex) * this.cellWidth;
    }

    @Override
    public NoiseChunk forIndex(int cellIndex) {
        int zInCell = Math.floorMod(cellIndex, this.cellWidth);
        int xyIndex = Math.floorDiv(cellIndex, this.cellWidth);
        int xInCell = Math.floorMod(xyIndex, this.cellWidth);
        int yInCell = this.cellHeight - 1 - Math.floorDiv(xyIndex, this.cellWidth);
        this.inCellX = xInCell;
        this.inCellY = yInCell;
        this.inCellZ = zInCell;
        this.arrayIndex = cellIndex;
        return this;
    }

    @Override
    public void fillAllDirectly(double[] output, DensityFunction function) {
        this.arrayIndex = 0;
        for (int yInCell = this.cellHeight - 1; yInCell >= 0; --yInCell) {
            this.inCellY = yInCell;
            for (int xInCell = 0; xInCell < this.cellWidth; ++xInCell) {
                this.inCellX = xInCell;
                int zInCell = 0;
                while (zInCell < this.cellWidth) {
                    this.inCellZ = zInCell++;
                    output[this.arrayIndex++] = function.compute(this);
                }
            }
        }
    }

    public void selectCellYZ(int cellYIndex, int cellZIndex) {
        for (NoiseInterpolator i : this.interpolators) {
            i.selectCellYZ(cellYIndex, cellZIndex);
        }
        this.fillingCell = true;
        this.cellStartBlockY = (cellYIndex + this.cellNoiseMinY) * this.cellHeight;
        this.cellStartBlockZ = (this.firstCellZ + cellZIndex) * this.cellWidth;
        ++this.arrayInterpolationCounter;
        for (CacheAllInCell cellCache : this.cellCaches) {
            cellCache.noiseFiller.fillArray(cellCache.values, this);
        }
        ++this.arrayInterpolationCounter;
        this.fillingCell = false;
    }

    public void updateForY(int posY, double factorY) {
        this.inCellY = posY - this.cellStartBlockY;
        for (NoiseInterpolator i : this.interpolators) {
            i.updateForY(factorY);
        }
    }

    public void updateForX(int posX, double factorX) {
        this.inCellX = posX - this.cellStartBlockX;
        for (NoiseInterpolator i : this.interpolators) {
            i.updateForX(factorX);
        }
    }

    public void updateForZ(int posZ, double factorZ) {
        this.inCellZ = posZ - this.cellStartBlockZ;
        ++this.interpolationCounter;
        for (NoiseInterpolator i : this.interpolators) {
            i.updateForZ(factorZ);
        }
    }

    public void stopInterpolation() {
        if (!this.interpolating) {
            throw new IllegalStateException("Staring interpolation twice");
        }
        this.interpolating = false;
    }

    public void swapSlices() {
        this.interpolators.forEach(NoiseInterpolator::swapSlices);
    }

    public Aquifer aquifer() {
        return this.aquifer;
    }

    protected int cellWidth() {
        return this.cellWidth;
    }

    protected int cellHeight() {
        return this.cellHeight;
    }

    private Blender.BlendingOutput getOrComputeBlendingOutput(int blockX, int blockZ) {
        Blender.BlendingOutput output;
        long pos2D = ChunkPos.pack(blockX, blockZ);
        if (this.lastBlendingDataPos == pos2D) {
            return this.lastBlendingOutput;
        }
        this.lastBlendingDataPos = pos2D;
        this.lastBlendingOutput = output = this.blender.blendOffsetAndFactor(blockX, blockZ);
        return output;
    }

    protected DensityFunction wrap(DensityFunction function) {
        return this.wrapped.computeIfAbsent(function, this::wrapNew);
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private DensityFunction wrapNew(DensityFunction function) {
        Object object;
        DensityFunction densityFunction = function;
        Objects.requireNonNull(densityFunction);
        DensityFunction densityFunction2 = densityFunction;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DensityFunctions.Marker.class, DensityFunctions.HolderHolder.class}, (DensityFunction)densityFunction2, n)) {
            case 0: {
                Object wrapped;
                DensityFunctions.Marker.Type type;
                DensityFunctions.Marker marker = (DensityFunctions.Marker)densityFunction2;
                try {
                    Object object2 = marker.type();
                    type = object2;
                    wrapped = object2 = marker.wrapped();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
                switch (type) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case Interpolated: {
                        object = new NoiseInterpolator(this, (DensityFunction)wrapped);
                        return object;
                    }
                    case FlatCache: {
                        object = new FlatCache(this, (DensityFunction)wrapped, true);
                        return object;
                    }
                    case Cache2D: {
                        object = new Cache2D((DensityFunction)wrapped);
                        return object;
                    }
                    case CacheOnce: {
                        object = new CacheOnce(this, (DensityFunction)wrapped);
                        return object;
                    }
                    case CacheAllInCell: {
                        object = new CacheAllInCell(this, (DensityFunction)wrapped);
                        return object;
                    }
                    case BlendDensity: 
                }
                if (!this.blender.isEmpty()) {
                    object = new BlendDensity(this, (DensityFunction)wrapped);
                    return object;
                }
                object = wrapped;
                return object;
            }
            case 1: {
                DensityFunctions.HolderHolder holderHolder = (DensityFunctions.HolderHolder)densityFunction2;
                {
                    Holder<DensityFunction> holder;
                    Holder<DensityFunction> holder2 = holder = holderHolder.function();
                    object = holder2.value();
                    return object;
                }
            }
        }
        if (function == DensityFunctions.BlendAlpha.INSTANCE && this.blendAlpha != null) {
            object = this.blendAlpha;
            return object;
        }
        if (function == DensityFunctions.BlendOffset.INSTANCE && this.blendOffset != null) {
            object = this.blendOffset;
            return object;
        }
        if (function == DensityFunctions.BeardifierMarker.INSTANCE) {
            object = this.beardifier;
            return object;
        }
        object = function;
        return object;
    }

    private class FlatCache
    implements NoiseChunkDensityFunction,
    DensityFunctions.MarkerOrMarked {
        private final DensityFunction noiseFiller;
        private final double[] values;
        private final int sizeXZ;
        final /* synthetic */ NoiseChunk this$0;

        private FlatCache(NoiseChunk noiseChunk, DensityFunction noiseFiller, boolean fill) {
            NoiseChunk noiseChunk2 = noiseChunk;
            Objects.requireNonNull(noiseChunk2);
            this.this$0 = noiseChunk2;
            this.noiseFiller = noiseFiller;
            this.sizeXZ = noiseChunk.noiseSizeXZ + 1;
            this.values = new double[this.sizeXZ * this.sizeXZ];
            if (fill) {
                for (int x = 0; x <= noiseChunk.noiseSizeXZ; ++x) {
                    int quartX = noiseChunk.firstNoiseX + x;
                    int blockX = QuartPos.toBlock(quartX);
                    for (int z = 0; z <= noiseChunk.noiseSizeXZ; ++z) {
                        int quartZ = noiseChunk.firstNoiseZ + z;
                        int blockZ = QuartPos.toBlock(quartZ);
                        this.values[x + z * this.sizeXZ] = noiseFiller.compute(new DensityFunction.SinglePointContext(blockX, 0, blockZ));
                    }
                }
            }
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int quartX = QuartPos.fromBlock(context.blockX());
            int quartZ = QuartPos.fromBlock(context.blockZ());
            int x = quartX - this.this$0.firstNoiseX;
            int z = quartZ - this.this$0.firstNoiseZ;
            if (x >= 0 && z >= 0 && x < this.sizeXZ && z < this.sizeXZ) {
                return this.values[x + z * this.sizeXZ];
            }
            return this.noiseFiller.compute(context);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.FlatCache;
        }
    }

    private class BlendAlpha
    implements NoiseChunkDensityFunction {
        final /* synthetic */ NoiseChunk this$0;

        private BlendAlpha(NoiseChunk noiseChunk) {
            NoiseChunk noiseChunk2 = noiseChunk;
            Objects.requireNonNull(noiseChunk2);
            this.this$0 = noiseChunk2;
        }

        @Override
        public DensityFunction wrapped() {
            return DensityFunctions.BlendAlpha.INSTANCE;
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return visitor.apply(this.wrapped());
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.this$0.getOrComputeBlendingOutput(context.blockX(), context.blockZ()).alpha();
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return DensityFunctions.BlendAlpha.CODEC;
        }
    }

    private class BlendOffset
    implements NoiseChunkDensityFunction {
        final /* synthetic */ NoiseChunk this$0;

        private BlendOffset(NoiseChunk noiseChunk) {
            NoiseChunk noiseChunk2 = noiseChunk;
            Objects.requireNonNull(noiseChunk2);
            this.this$0 = noiseChunk2;
        }

        @Override
        public DensityFunction wrapped() {
            return DensityFunctions.BlendOffset.INSTANCE;
        }

        @Override
        public DensityFunction mapChildren(DensityFunction.Visitor visitor) {
            return visitor.apply(this.wrapped());
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.this$0.getOrComputeBlendingOutput(context.blockX(), context.blockZ()).blendingOffset();
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return DensityFunctions.BlendOffset.CODEC;
        }
    }

    @FunctionalInterface
    public static interface BlockStateFiller {
        public @Nullable BlockState calculate(DensityFunction.FunctionContext var1);
    }

    public class NoiseInterpolator
    implements NoiseChunkDensityFunction,
    DensityFunctions.MarkerOrMarked {
        private double[][] slice0;
        private double[][] slice1;
        private final DensityFunction noiseFiller;
        private double noise000;
        private double noise001;
        private double noise100;
        private double noise101;
        private double noise010;
        private double noise011;
        private double noise110;
        private double noise111;
        private double valueXZ00;
        private double valueXZ10;
        private double valueXZ01;
        private double valueXZ11;
        private double valueZ0;
        private double valueZ1;
        private double value;
        final /* synthetic */ NoiseChunk this$0;

        private NoiseInterpolator(NoiseChunk this$0, DensityFunction noiseFiller) {
            NoiseChunk noiseChunk = this$0;
            Objects.requireNonNull(noiseChunk);
            this.this$0 = noiseChunk;
            this.noiseFiller = noiseFiller;
            this.slice0 = this.allocateSlice(this$0.cellCountY, this$0.cellCountXZ);
            this.slice1 = this.allocateSlice(this$0.cellCountY, this$0.cellCountXZ);
            this$0.interpolators.add(this);
        }

        private double[][] allocateSlice(int cellCountY, int cellCountZ) {
            int sizeZ = cellCountZ + 1;
            int sizeY = cellCountY + 1;
            double[][] result = new double[sizeZ][sizeY];
            for (int cellZIndex = 0; cellZIndex < sizeZ; ++cellZIndex) {
                result[cellZIndex] = new double[sizeY];
            }
            return result;
        }

        private void selectCellYZ(int cellYIndex, int cellZIndex) {
            this.noise000 = this.slice0[cellZIndex][cellYIndex];
            this.noise001 = this.slice0[cellZIndex + 1][cellYIndex];
            this.noise100 = this.slice1[cellZIndex][cellYIndex];
            this.noise101 = this.slice1[cellZIndex + 1][cellYIndex];
            this.noise010 = this.slice0[cellZIndex][cellYIndex + 1];
            this.noise011 = this.slice0[cellZIndex + 1][cellYIndex + 1];
            this.noise110 = this.slice1[cellZIndex][cellYIndex + 1];
            this.noise111 = this.slice1[cellZIndex + 1][cellYIndex + 1];
        }

        private void updateForY(double factorY) {
            this.valueXZ00 = Mth.lerp(factorY, this.noise000, this.noise010);
            this.valueXZ10 = Mth.lerp(factorY, this.noise100, this.noise110);
            this.valueXZ01 = Mth.lerp(factorY, this.noise001, this.noise011);
            this.valueXZ11 = Mth.lerp(factorY, this.noise101, this.noise111);
        }

        private void updateForX(double factorX) {
            this.valueZ0 = Mth.lerp(factorX, this.valueXZ00, this.valueXZ10);
            this.valueZ1 = Mth.lerp(factorX, this.valueXZ01, this.valueXZ11);
        }

        private void updateForZ(double factorZ) {
            this.value = Mth.lerp(factorZ, this.valueZ0, this.valueZ1);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            if (context != this.this$0) {
                return this.noiseFiller.compute(context);
            }
            if (!this.this$0.interpolating) {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            }
            if (this.this$0.fillingCell) {
                return Mth.lerp3((double)this.this$0.inCellX / (double)this.this$0.cellWidth, (double)this.this$0.inCellY / (double)this.this$0.cellHeight, (double)this.this$0.inCellZ / (double)this.this$0.cellWidth, this.noise000, this.noise100, this.noise010, this.noise110, this.noise001, this.noise101, this.noise011, this.noise111);
            }
            return this.value;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            if (this.this$0.fillingCell) {
                contextProvider.fillAllDirectly(output, this);
                return;
            }
            this.wrapped().fillArray(output, contextProvider);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        private void swapSlices() {
            double[][] tmp = this.slice0;
            this.slice0 = this.slice1;
            this.slice1 = tmp;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Interpolated;
        }
    }

    private class CacheAllInCell
    implements NoiseChunkDensityFunction,
    DensityFunctions.MarkerOrMarked {
        private final DensityFunction noiseFiller;
        private final double[] values;
        final /* synthetic */ NoiseChunk this$0;

        private CacheAllInCell(NoiseChunk noiseChunk, DensityFunction noiseFiller) {
            NoiseChunk noiseChunk2 = noiseChunk;
            Objects.requireNonNull(noiseChunk2);
            this.this$0 = noiseChunk2;
            this.noiseFiller = noiseFiller;
            this.values = new double[noiseChunk.cellWidth * noiseChunk.cellWidth * noiseChunk.cellHeight];
            noiseChunk.cellCaches.add(this);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            if (context != this.this$0) {
                return this.noiseFiller.compute(context);
            }
            if (!this.this$0.interpolating) {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            }
            int x = this.this$0.inCellX;
            int y = this.this$0.inCellY;
            int z = this.this$0.inCellZ;
            if (x >= 0 && y >= 0 && z >= 0 && x < this.this$0.cellWidth && y < this.this$0.cellHeight && z < this.this$0.cellWidth) {
                return this.values[((this.this$0.cellHeight - 1 - y) * this.this$0.cellWidth + x) * this.this$0.cellWidth + z];
            }
            return this.noiseFiller.compute(context);
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheAllInCell;
        }
    }

    private static class Cache2D
    implements NoiseChunkDensityFunction,
    DensityFunctions.MarkerOrMarked {
        private final DensityFunction function;
        private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
        private double lastValue;

        private Cache2D(DensityFunction function) {
            this.function = function;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double value;
            int blockZ;
            int blockX = context.blockX();
            long pos2D = ChunkPos.pack(blockX, blockZ = context.blockZ());
            if (this.lastPos2D == pos2D) {
                return this.lastValue;
            }
            this.lastPos2D = pos2D;
            this.lastValue = value = this.function.compute(context);
            return value;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.function.fillArray(output, contextProvider);
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Cache2D;
        }
    }

    private class CacheOnce
    implements NoiseChunkDensityFunction,
    DensityFunctions.MarkerOrMarked {
        private final DensityFunction function;
        private long lastCounter;
        private long lastArrayCounter;
        private double lastValue;
        private double @Nullable [] lastArray;
        final /* synthetic */ NoiseChunk this$0;

        private CacheOnce(NoiseChunk noiseChunk, DensityFunction function) {
            NoiseChunk noiseChunk2 = noiseChunk;
            Objects.requireNonNull(noiseChunk2);
            this.this$0 = noiseChunk2;
            this.function = function;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double value;
            if (context != this.this$0) {
                return this.function.compute(context);
            }
            if (this.lastArray != null && this.lastArrayCounter == this.this$0.arrayInterpolationCounter) {
                return this.lastArray[this.this$0.arrayIndex];
            }
            if (this.lastCounter == this.this$0.interpolationCounter) {
                return this.lastValue;
            }
            this.lastCounter = this.this$0.interpolationCounter;
            this.lastValue = value = this.function.compute(context);
            return value;
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            if (this.lastArray != null && this.lastArrayCounter == this.this$0.arrayInterpolationCounter) {
                System.arraycopy(this.lastArray, 0, output, 0, output.length);
                return;
            }
            this.wrapped().fillArray(output, contextProvider);
            if (this.lastArray != null && this.lastArray.length == output.length) {
                System.arraycopy(output, 0, this.lastArray, 0, output.length);
            } else {
                this.lastArray = (double[])output.clone();
            }
            this.lastArrayCounter = this.this$0.arrayInterpolationCounter;
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheOnce;
        }
    }

    private class BlendDensity
    implements NoiseChunkDensityFunction,
    DensityFunctions.MarkerOrMarked {
        private final DensityFunction input;
        final /* synthetic */ NoiseChunk this$0;

        private BlendDensity(NoiseChunk noiseChunk, DensityFunction input) {
            NoiseChunk noiseChunk2 = noiseChunk;
            Objects.requireNonNull(noiseChunk2);
            this.this$0 = noiseChunk2;
            this.input = input;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.this$0.blender.blendDensity(context, this.input.compute(context));
        }

        @Override
        public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
            this.input.fillArray(output, contextProvider);
            for (int i = 0; i < output.length; ++i) {
                DensityFunction.FunctionContext context = contextProvider.forIndex(i);
                output[i] = this.this$0.blender.blendDensity(context, output[i]);
            }
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public DensityFunction wrapped() {
            return this.input;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.BlendDensity;
        }
    }

    private static interface NoiseChunkDensityFunction
    extends DensityFunction {
        public DensityFunction wrapped();

        @Override
        default public double minValue() {
            return this.wrapped().minValue();
        }

        @Override
        default public double maxValue() {
            return this.wrapped().maxValue();
        }
    }
}

