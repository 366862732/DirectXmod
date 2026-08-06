/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.mutable.MutableDouble
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jspecify.annotations.Nullable;

public interface Aquifer {
    public static Aquifer create(NoiseChunk noiseChunk, ChunkPos pos, NoiseRouter router, PositionalRandomFactory positionalRandomFactory, int minBlockY, int yBlockSize, FluidPicker fluidRule) {
        return new NoiseBasedAquifer(noiseChunk, pos, router, positionalRandomFactory, minBlockY, yBlockSize, fluidRule);
    }

    public static Aquifer createDisabled(final FluidPicker fluidRule) {
        return new Aquifer(){

            @Override
            public @Nullable BlockState computeSubstance(DensityFunction.FunctionContext context, double density) {
                if (density > 0.0) {
                    return null;
                }
                return fluidRule.computeFluid(context.blockX(), context.blockY(), context.blockZ()).at(context.blockY());
            }

            @Override
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }

    public @Nullable BlockState computeSubstance(DensityFunction.FunctionContext var1, double var2);

    public boolean shouldScheduleFluidUpdate();

    public static class NoiseBasedAquifer
    implements Aquifer {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SEPARATION = 6;
        private static final int Y_SEPARATION = 3;
        private static final int Z_SEPARATION = 6;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private static final int X_SPACING_SHIFT = 4;
        private static final int Z_SPACING_SHIFT = 4;
        private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
        private static final double FLOWING_UPDATE_SIMULARITY = NoiseBasedAquifer.similarity(Mth.square(10), Mth.square(12));
        private static final int SAMPLE_OFFSET_X = -5;
        private static final int SAMPLE_OFFSET_Y = 1;
        private static final int SAMPLE_OFFSET_Z = -5;
        private static final int MIN_CELL_SAMPLE_X = 0;
        private static final int MIN_CELL_SAMPLE_Y = -1;
        private static final int MIN_CELL_SAMPLE_Z = 0;
        private static final int MAX_CELL_SAMPLE_X = 1;
        private static final int MAX_CELL_SAMPLE_Y = 1;
        private static final int MAX_CELL_SAMPLE_Z = 1;
        private final NoiseChunk noiseChunk;
        private final DensityFunction barrierNoise;
        private final DensityFunction fluidLevelFloodednessNoise;
        private final DensityFunction fluidLevelSpreadNoise;
        private final DensityFunction lavaNoise;
        private final PositionalRandomFactory positionalRandomFactory;
        private final @Nullable FluidStatus[] aquiferCache;
        private final long[] aquiferLocationCache;
        private final FluidPicker globalFluidPicker;
        private final DensityFunction erosion;
        private final DensityFunction depth;
        private boolean shouldScheduleFluidUpdate;
        private final int skipSamplingAboveY;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;
        private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{{0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}};

        private NoiseBasedAquifer(NoiseChunk noiseChunk, ChunkPos pos, NoiseRouter router, PositionalRandomFactory positionalRandomFactory, int minBlockY, int yBlockSize, FluidPicker globalFluidPicker) {
            this.noiseChunk = noiseChunk;
            this.barrierNoise = router.barrierNoise();
            this.fluidLevelFloodednessNoise = router.fluidLevelFloodednessNoise();
            this.fluidLevelSpreadNoise = router.fluidLevelSpreadNoise();
            this.lavaNoise = router.lavaNoise();
            this.erosion = router.erosion();
            this.depth = router.depth();
            this.positionalRandomFactory = positionalRandomFactory;
            this.minGridX = NoiseBasedAquifer.gridX(pos.getMinBlockX() + -5) + 0;
            this.globalFluidPicker = globalFluidPicker;
            int maxGridX = NoiseBasedAquifer.gridX(pos.getMaxBlockX() + -5) + 1;
            this.gridSizeX = maxGridX - this.minGridX + 1;
            this.minGridY = NoiseBasedAquifer.gridY(minBlockY + 1) + -1;
            int maxGridY = NoiseBasedAquifer.gridY(minBlockY + yBlockSize + 1) + 1;
            int gridSizeY = maxGridY - this.minGridY + 1;
            this.minGridZ = NoiseBasedAquifer.gridZ(pos.getMinBlockZ() + -5) + 0;
            int maxGridZ = NoiseBasedAquifer.gridZ(pos.getMaxBlockZ() + -5) + 1;
            this.gridSizeZ = maxGridZ - this.minGridZ + 1;
            int totalGridSize = this.gridSizeX * gridSizeY * this.gridSizeZ;
            this.aquiferCache = new FluidStatus[totalGridSize];
            this.aquiferLocationCache = new long[totalGridSize];
            Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
            int maxAdjustedSurfaceLevel = this.adjustSurfaceLevel(noiseChunk.maxPreliminarySurfaceLevel(NoiseBasedAquifer.fromGridX(this.minGridX, 0), NoiseBasedAquifer.fromGridZ(this.minGridZ, 0), NoiseBasedAquifer.fromGridX(maxGridX, 9), NoiseBasedAquifer.fromGridZ(maxGridZ, 9)));
            int skipSamplingAboveGridY = NoiseBasedAquifer.gridY(maxAdjustedSurfaceLevel + 12) - -1;
            this.skipSamplingAboveY = NoiseBasedAquifer.fromGridY(skipSamplingAboveGridY, 11) - 1;
        }

        private int getIndex(int gridX, int gridY, int gridZ) {
            int x = gridX - this.minGridX;
            int y = gridY - this.minGridY;
            int z = gridZ - this.minGridZ;
            return (y * this.gridSizeZ + z) * this.gridSizeX + x;
        }

        @Override
        public @Nullable BlockState computeSubstance(DensityFunction.FunctionContext context, double density) {
            boolean mayFlow13;
            double barrier23;
            double barrier13;
            BlockState actualFluidState;
            if (density > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
            int posX = context.blockX();
            int posY = context.blockY();
            int posZ = context.blockZ();
            FluidStatus globalFluid = this.globalFluidPicker.computeFluid(posX, posY, posZ);
            if (posY > this.skipSamplingAboveY) {
                this.shouldScheduleFluidUpdate = false;
                return globalFluid.at(posY);
            }
            if (globalFluid.at(posY).is(Blocks.LAVA)) {
                this.shouldScheduleFluidUpdate = false;
                return SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : Blocks.LAVA.defaultBlockState();
            }
            int xAnchor = NoiseBasedAquifer.gridX(posX + -5);
            int yAnchor = NoiseBasedAquifer.gridY(posY + 1);
            int zAnchor = NoiseBasedAquifer.gridZ(posZ + -5);
            int distanceSqr1 = Integer.MAX_VALUE;
            int distanceSqr2 = Integer.MAX_VALUE;
            int distanceSqr3 = Integer.MAX_VALUE;
            int distanceSqr4 = Integer.MAX_VALUE;
            int closestIndex1 = 0;
            int closestIndex2 = 0;
            int closestIndex3 = 0;
            int closestIndex4 = 0;
            for (int x1 = 0; x1 <= 1; ++x1) {
                for (int y1 = -1; y1 <= 1; ++y1) {
                    for (int z1 = 0; z1 <= 1; ++z1) {
                        long location;
                        int spacedGridX = xAnchor + x1;
                        int spacedGridY = yAnchor + y1;
                        int spacedGridZ = zAnchor + z1;
                        int index = this.getIndex(spacedGridX, spacedGridY, spacedGridZ);
                        long existingLocation = this.aquiferLocationCache[index];
                        if (existingLocation != Long.MAX_VALUE) {
                            location = existingLocation;
                        } else {
                            RandomSource random = this.positionalRandomFactory.at(spacedGridX, spacedGridY, spacedGridZ);
                            this.aquiferLocationCache[index] = location = BlockPos.asLong(NoiseBasedAquifer.fromGridX(spacedGridX, random.nextInt(10)), NoiseBasedAquifer.fromGridY(spacedGridY, random.nextInt(9)), NoiseBasedAquifer.fromGridZ(spacedGridZ, random.nextInt(10)));
                        }
                        int dx = BlockPos.getX(location) - posX;
                        int dy = BlockPos.getY(location) - posY;
                        int dz = BlockPos.getZ(location) - posZ;
                        int newDistance = dx * dx + dy * dy + dz * dz;
                        if (distanceSqr1 >= newDistance) {
                            closestIndex4 = closestIndex3;
                            closestIndex3 = closestIndex2;
                            closestIndex2 = closestIndex1;
                            closestIndex1 = index;
                            distanceSqr4 = distanceSqr3;
                            distanceSqr3 = distanceSqr2;
                            distanceSqr2 = distanceSqr1;
                            distanceSqr1 = newDistance;
                            continue;
                        }
                        if (distanceSqr2 >= newDistance) {
                            closestIndex4 = closestIndex3;
                            closestIndex3 = closestIndex2;
                            closestIndex2 = index;
                            distanceSqr4 = distanceSqr3;
                            distanceSqr3 = distanceSqr2;
                            distanceSqr2 = newDistance;
                            continue;
                        }
                        if (distanceSqr3 >= newDistance) {
                            closestIndex4 = closestIndex3;
                            closestIndex3 = index;
                            distanceSqr4 = distanceSqr3;
                            distanceSqr3 = newDistance;
                            continue;
                        }
                        if (distanceSqr4 < newDistance) continue;
                        closestIndex4 = index;
                        distanceSqr4 = newDistance;
                    }
                }
            }
            FluidStatus closestStatus1 = this.getAquiferStatus(closestIndex1);
            double similarity12 = NoiseBasedAquifer.similarity(distanceSqr1, distanceSqr2);
            BlockState fluidState = closestStatus1.at(posY);
            BlockState blockState = actualFluidState = SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : fluidState;
            if (similarity12 <= 0.0) {
                FluidStatus closestStatus2;
                this.shouldScheduleFluidUpdate = similarity12 >= FLOWING_UPDATE_SIMULARITY ? !closestStatus1.equals(closestStatus2 = this.getAquiferStatus(closestIndex2)) : false;
                return actualFluidState;
            }
            if (fluidState.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(posX, posY - 1, posZ).at(posY - 1).is(Blocks.LAVA)) {
                this.shouldScheduleFluidUpdate = true;
                return actualFluidState;
            }
            MutableDouble barrierNoiseValue = new MutableDouble(Double.NaN);
            FluidStatus closestStatus2 = this.getAquiferStatus(closestIndex2);
            double barrier12 = similarity12 * this.calculatePressure(context, barrierNoiseValue, closestStatus1, closestStatus2);
            if (density + barrier12 > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
            FluidStatus closestStatus3 = this.getAquiferStatus(closestIndex3);
            double similarity13 = NoiseBasedAquifer.similarity(distanceSqr1, distanceSqr3);
            if (similarity13 > 0.0 && density + (barrier13 = similarity12 * similarity13 * this.calculatePressure(context, barrierNoiseValue, closestStatus1, closestStatus3)) > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
            double similarity23 = NoiseBasedAquifer.similarity(distanceSqr2, distanceSqr3);
            if (similarity23 > 0.0 && density + (barrier23 = similarity12 * similarity23 * this.calculatePressure(context, barrierNoiseValue, closestStatus2, closestStatus3)) > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }
            boolean mayFlow12 = !closestStatus1.equals(closestStatus2);
            boolean mayFlow23 = similarity23 >= FLOWING_UPDATE_SIMULARITY && !closestStatus2.equals(closestStatus3);
            boolean bl = mayFlow13 = similarity13 >= FLOWING_UPDATE_SIMULARITY && !closestStatus1.equals(closestStatus3);
            this.shouldScheduleFluidUpdate = mayFlow12 || mayFlow23 || mayFlow13 ? true : similarity13 >= FLOWING_UPDATE_SIMULARITY && NoiseBasedAquifer.similarity(distanceSqr1, distanceSqr4) >= FLOWING_UPDATE_SIMULARITY && !closestStatus1.equals(this.getAquiferStatus(closestIndex4));
            return actualFluidState;
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }

        private static double similarity(int distanceSqr1, int distanceSqr2) {
            double threshold = 25.0;
            return 1.0 - (double)(distanceSqr2 - distanceSqr1) / 25.0;
        }

        private double calculatePressure(DensityFunction.FunctionContext context, MutableDouble barrierNoiseValue, FluidStatus statusClosest1, FluidStatus statusClosest2) {
            double noiseValue;
            double centerPoint;
            int posY = context.blockY();
            BlockState type1 = statusClosest1.at(posY);
            BlockState type2 = statusClosest2.at(posY);
            if (type1.is(Blocks.LAVA) && type2.is(Blocks.WATER) || type1.is(Blocks.WATER) && type2.is(Blocks.LAVA)) {
                return 2.0;
            }
            int fluidYDiff = Math.abs(statusClosest1.fluidLevel - statusClosest2.fluidLevel);
            if (fluidYDiff == 0) {
                return 0.0;
            }
            double averageFluidY = 0.5 * (double)(statusClosest1.fluidLevel + statusClosest2.fluidLevel);
            double howFarAboveAverageFluidPoint = (double)posY + 0.5 - averageFluidY;
            double baseValue = (double)fluidYDiff / 2.0;
            double topBias = 0.0;
            double furthestRocksFromTopBias = 2.5;
            double furthestHolesFromTopBias = 1.5;
            double bottomBias = 3.0;
            double furthestRocksFromBottomBias = 10.0;
            double furthestHolesFromBottomBias = 3.0;
            double distanceFromBarrierEdgeTowardsMiddle = baseValue - Math.abs(howFarAboveAverageFluidPoint);
            double gradient = howFarAboveAverageFluidPoint > 0.0 ? ((centerPoint = 0.0 + distanceFromBarrierEdgeTowardsMiddle) > 0.0 ? centerPoint / 1.5 : centerPoint / 2.5) : ((centerPoint = 3.0 + distanceFromBarrierEdgeTowardsMiddle) > 0.0 ? centerPoint / 3.0 : centerPoint / 10.0);
            double amplitude = 2.0;
            if (gradient < -2.0 || gradient > 2.0) {
                noiseValue = 0.0;
            } else {
                double currentNoiseValue = barrierNoiseValue.doubleValue();
                if (Double.isNaN(currentNoiseValue)) {
                    double barrierNoise = this.barrierNoise.compute(context);
                    barrierNoiseValue.setValue(barrierNoise);
                    noiseValue = barrierNoise;
                } else {
                    noiseValue = currentNoiseValue;
                }
            }
            return 2.0 * (noiseValue + gradient);
        }

        private static int gridX(int blockCoord) {
            return blockCoord >> 4;
        }

        private static int fromGridX(int gridCoord, int blockOffset) {
            return (gridCoord << 4) + blockOffset;
        }

        private static int gridY(int blockCoord) {
            return Math.floorDiv(blockCoord, 12);
        }

        private static int fromGridY(int gridCoord, int blockOffset) {
            return gridCoord * 12 + blockOffset;
        }

        private static int gridZ(int blockCoord) {
            return blockCoord >> 4;
        }

        private static int fromGridZ(int gridCoord, int blockOffset) {
            return (gridCoord << 4) + blockOffset;
        }

        private FluidStatus getAquiferStatus(int index) {
            FluidStatus status;
            FluidStatus oldStatus = this.aquiferCache[index];
            if (oldStatus != null) {
                return oldStatus;
            }
            long location = this.aquiferLocationCache[index];
            this.aquiferCache[index] = status = this.computeFluid(BlockPos.getX(location), BlockPos.getY(location), BlockPos.getZ(location));
            return status;
        }

        private FluidStatus computeFluid(int x, int y, int z) {
            FluidStatus globalFluid = this.globalFluidPicker.computeFluid(x, y, z);
            int lowestPreliminarySurface = Integer.MAX_VALUE;
            int topOfAquiferCell = y + 12;
            int bottomOfAquiferCell = y - 12;
            boolean surfaceAtCenterIsUnderGlobalFluidLevel = false;
            for (int[] offset : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
                FluidStatus globalFluidAtSurface;
                boolean topOfAquiferCellPokesAboveSurface;
                boolean start;
                int sampleX = x + SectionPos.sectionToBlockCoord(offset[0]);
                int sampleZ = z + SectionPos.sectionToBlockCoord(offset[1]);
                int preliminarySurfaceLevel = this.noiseChunk.preliminarySurfaceLevel(sampleX, sampleZ);
                int adjustedSurfaceLevel = this.adjustSurfaceLevel(preliminarySurfaceLevel);
                boolean bl = start = offset[0] == 0 && offset[1] == 0;
                if (start && bottomOfAquiferCell > adjustedSurfaceLevel) {
                    return globalFluid;
                }
                boolean bl2 = topOfAquiferCellPokesAboveSurface = topOfAquiferCell > adjustedSurfaceLevel;
                if ((topOfAquiferCellPokesAboveSurface || start) && !(globalFluidAtSurface = this.globalFluidPicker.computeFluid(sampleX, adjustedSurfaceLevel, sampleZ)).at(adjustedSurfaceLevel).isAir()) {
                    if (start) {
                        surfaceAtCenterIsUnderGlobalFluidLevel = true;
                    }
                    if (topOfAquiferCellPokesAboveSurface) {
                        return globalFluidAtSurface;
                    }
                }
                lowestPreliminarySurface = Math.min(lowestPreliminarySurface, preliminarySurfaceLevel);
            }
            int fluidSurfaceLevel = this.computeSurfaceLevel(x, y, z, globalFluid, lowestPreliminarySurface, surfaceAtCenterIsUnderGlobalFluidLevel);
            return new FluidStatus(fluidSurfaceLevel, this.computeFluidType(x, y, z, globalFluid, fluidSurfaceLevel));
        }

        private int adjustSurfaceLevel(int preliminarySurfaceLevel) {
            return preliminarySurfaceLevel + 8;
        }

        private int computeSurfaceLevel(int x, int y, int z, FluidStatus globalFluid, int lowestPreliminarySurface, boolean surfaceAtCenterIsUnderGlobalFluidLevel) {
            double fullyFloodidness;
            double partiallyFloodedness;
            DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(x, y, z);
            if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, context)) {
                partiallyFloodedness = -1.0;
                fullyFloodidness = -1.0;
            } else {
                int distanceBelowSurface = lowestPreliminarySurface + 8 - y;
                int floodednessMaxDepth = 64;
                double floodednessFactor = surfaceAtCenterIsUnderGlobalFluidLevel ? Mth.clampedMap((double)distanceBelowSurface, 0.0, 64.0, 1.0, 0.0) : 0.0;
                double floodednessNoiseValue = Mth.clamp(this.fluidLevelFloodednessNoise.compute(context), -1.0, 1.0);
                double fullyFloodedThreshold = Mth.map(floodednessFactor, 1.0, 0.0, -0.3, 0.8);
                double partiallyFloodedThreshold = Mth.map(floodednessFactor, 1.0, 0.0, -0.8, 0.4);
                partiallyFloodedness = floodednessNoiseValue - partiallyFloodedThreshold;
                fullyFloodidness = floodednessNoiseValue - fullyFloodedThreshold;
            }
            int fluidSurfaceLevel = fullyFloodidness > 0.0 ? globalFluid.fluidLevel : (partiallyFloodedness > 0.0 ? this.computeRandomizedFluidSurfaceLevel(x, y, z, lowestPreliminarySurface) : DimensionType.WAY_BELOW_MIN_Y);
            return fluidSurfaceLevel;
        }

        private int computeRandomizedFluidSurfaceLevel(int x, int y, int z, int lowestPreliminarySurface) {
            int fluidCellWidth = 16;
            int fluidCellHeight = 40;
            int fluidLevelCellX = Math.floorDiv(x, 16);
            int fluidLevelCellY = Math.floorDiv(y, 40);
            int fluidLevelCellZ = Math.floorDiv(z, 16);
            int fluidCellMiddleY = fluidLevelCellY * 40 + 20;
            int maxSpread = 10;
            double fluidLevelSpread = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(fluidLevelCellX, fluidLevelCellY, fluidLevelCellZ)) * 10.0;
            int fluidLevelSpreadQuantized = Mth.quantize(fluidLevelSpread, 3);
            int targetFluidSurfaceLevel = fluidCellMiddleY + fluidLevelSpreadQuantized;
            return Math.min(lowestPreliminarySurface, targetFluidSurfaceLevel);
        }

        private BlockState computeFluidType(int x, int y, int z, FluidStatus globalFluid, int fluidSurfaceLevel) {
            BlockState fluidType = globalFluid.fluidType;
            if (fluidSurfaceLevel <= -10 && fluidSurfaceLevel != DimensionType.WAY_BELOW_MIN_Y && globalFluid.fluidType != Blocks.LAVA.defaultBlockState()) {
                int fluidTypeCellZ;
                int fluidTypeCellY;
                int fluidTypeCellWidth = 64;
                int fluidTypeCellHeight = 40;
                int fluidTypeCellX = Math.floorDiv(x, 64);
                double lavaNoiseValue = this.lavaNoise.compute(new DensityFunction.SinglePointContext(fluidTypeCellX, fluidTypeCellY = Math.floorDiv(y, 40), fluidTypeCellZ = Math.floorDiv(z, 64)));
                if (Math.abs(lavaNoiseValue) > 0.3) {
                    fluidType = Blocks.LAVA.defaultBlockState();
                }
            }
            return fluidType;
        }
    }

    public static interface FluidPicker {
        public FluidStatus computeFluid(int var1, int var2, int var3);
    }

    public record FluidStatus(int fluidLevel, BlockState fluidType) {
        public BlockState at(int blockY) {
            return blockY < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
        }
    }
}

