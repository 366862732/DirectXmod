/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.carver.CanyonCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.NetherWorldCarver;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

public abstract class WorldCarver<C extends CarverConfiguration> {
    public static final WorldCarver<CaveCarverConfiguration> CAVE = WorldCarver.register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = WorldCarver.register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
    public static final WorldCarver<CanyonCarverConfiguration> CANYON = WorldCarver.register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
    protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
    protected Set<Fluid> liquids = ImmutableSet.of((Object)Fluids.WATER);
    private final MapCodec<ConfiguredWorldCarver<C>> configuredCodec;

    private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String name, F carver) {
        return (F)Registry.register(BuiltInRegistries.CARVER, name, carver);
    }

    public WorldCarver(Codec<C> codec) {
        this.configuredCodec = codec.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config);
    }

    public ConfiguredWorldCarver<C> configured(C configuration) {
        return new ConfiguredWorldCarver<C>(this, configuration);
    }

    public MapCodec<ConfiguredWorldCarver<C>> configuredCodec() {
        return this.configuredCodec;
    }

    public int getRange() {
        return 4;
    }

    protected boolean carveEllipsoid(CarvingContext context, C configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, Aquifer aquifer, double x, double y, double z, double horizontalRadius, double verticalRadius, CarvingMask mask, CarveSkipChecker skipChecker) {
        ChunkPos chunkPos = chunk.getPos();
        double centerX = chunkPos.getMiddleBlockX();
        double centerZ = chunkPos.getMiddleBlockZ();
        double maxDelta = 16.0 + horizontalRadius * 2.0;
        if (Math.abs(x - centerX) > maxDelta || Math.abs(z - centerZ) > maxDelta) {
            return false;
        }
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int minXIndex = Math.max(Mth.floor(x - horizontalRadius) - chunkMinX - 1, 0);
        int maxXIndex = Math.min(Mth.floor(x + horizontalRadius) - chunkMinX, 15);
        int minY = Math.max(Mth.floor(y - verticalRadius) - 1, context.getMinGenY() + 1);
        int protectedBlocksOnTop = chunk.isUpgrading() ? 0 : 7;
        int maxY = Math.min(Mth.floor(y + verticalRadius) + 1, context.getMinGenY() + context.getGenDepth() - 1 - protectedBlocksOnTop);
        int minZIndex = Math.max(Mth.floor(z - horizontalRadius) - chunkMinZ - 1, 0);
        int maxZIndex = Math.min(Mth.floor(z + horizontalRadius) - chunkMinZ, 15);
        boolean carved = false;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos helperPos = new BlockPos.MutableBlockPos();
        for (int xIndex = minXIndex; xIndex <= maxXIndex; ++xIndex) {
            int worldX = chunkPos.getBlockX(xIndex);
            double xd = ((double)worldX + 0.5 - x) / horizontalRadius;
            for (int zIndex = minZIndex; zIndex <= maxZIndex; ++zIndex) {
                int worldZ = chunkPos.getBlockZ(zIndex);
                double zd = ((double)worldZ + 0.5 - z) / horizontalRadius;
                if (xd * xd + zd * zd >= 1.0) continue;
                MutableBoolean hasGrass = new MutableBoolean(false);
                for (int worldY = maxY; worldY > minY; --worldY) {
                    double yd = ((double)worldY - 0.5 - y) / verticalRadius;
                    if (skipChecker.shouldSkip(context, xd, yd, zd, worldY) || mask.get(xIndex, worldY, zIndex) && !WorldCarver.isDebugEnabled(configuration)) continue;
                    mask.set(xIndex, worldY, zIndex);
                    blockPos.set(worldX, worldY, worldZ);
                    carved |= this.carveBlock(context, configuration, chunk, biomeGetter, mask, blockPos, helperPos, aquifer, hasGrass);
                }
            }
        }
        return carved;
    }

    protected boolean carveBlock(CarvingContext context, C configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, CarvingMask mask, BlockPos.MutableBlockPos blockPos, BlockPos.MutableBlockPos helperPos, Aquifer aquifer, MutableBoolean hasGrass) {
        BlockState blockState = chunk.getBlockState(blockPos);
        if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
            hasGrass.setTrue();
        }
        if (!this.canReplaceBlock(configuration, blockState) && !WorldCarver.isDebugEnabled(configuration)) {
            return false;
        }
        BlockState state = this.getCarveState(context, configuration, blockPos, aquifer);
        if (state == null) {
            return false;
        }
        chunk.setBlockState(blockPos, state);
        if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
            chunk.markPosForPostProcessing(blockPos);
        }
        if (hasGrass.isTrue()) {
            helperPos.setWithOffset((Vec3i)blockPos, Direction.DOWN);
            if (chunk.getBlockState(helperPos).is(Blocks.DIRT)) {
                context.topMaterial(biomeGetter, chunk, helperPos, !state.getFluidState().isEmpty()).ifPresent(topMaterial -> {
                    chunk.setBlockState(helperPos, (BlockState)topMaterial);
                    if (!topMaterial.getFluidState().isEmpty()) {
                        chunk.markPosForPostProcessing(helperPos);
                    }
                });
            }
        }
        return true;
    }

    private @Nullable BlockState getCarveState(CarvingContext context, C configuration, BlockPos blockPos, Aquifer aquifer) {
        if (blockPos.getY() <= ((CarverConfiguration)configuration).lavaLevel.resolveY(context)) {
            return LAVA.createLegacyBlock();
        }
        BlockState state = aquifer.computeSubstance(new DensityFunction.SinglePointContext(blockPos.getX(), blockPos.getY(), blockPos.getZ()), 0.0);
        if (state == null) {
            return WorldCarver.isDebugEnabled(configuration) ? ((CarverConfiguration)configuration).debugSettings.getBarrierState() : null;
        }
        return WorldCarver.isDebugEnabled(configuration) ? WorldCarver.getDebugState(configuration, state) : state;
    }

    private static BlockState getDebugState(CarverConfiguration configuration, BlockState state) {
        if (state.is(Blocks.AIR)) {
            return configuration.debugSettings.getAirState();
        }
        if (state.is(Blocks.WATER)) {
            BlockState debugState = configuration.debugSettings.getWaterState();
            if (debugState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                return (BlockState)debugState.setValue(BlockStateProperties.WATERLOGGED, true);
            }
            return debugState;
        }
        if (state.is(Blocks.LAVA)) {
            return configuration.debugSettings.getLavaState();
        }
        return state;
    }

    public abstract boolean carve(CarvingContext var1, C var2, ChunkAccess var3, Function<BlockPos, Holder<Biome>> var4, RandomSource var5, Aquifer var6, ChunkPos var7, CarvingMask var8);

    public abstract boolean isStartChunk(C var1, RandomSource var2);

    protected boolean canReplaceBlock(C configuration, BlockState state) {
        return state.is(((CarverConfiguration)configuration).replaceable);
    }

    protected static boolean canReach(ChunkPos chunkPos, double x, double z, int currentStep, int totalSteps, float thickness) {
        double rr;
        double remaining;
        double zMid;
        double zd;
        double xMid = chunkPos.getMiddleBlockX();
        double xd = x - xMid;
        return xd * xd + (zd = z - (zMid = (double)chunkPos.getMiddleBlockZ())) * zd - (remaining = (double)(totalSteps - currentStep)) * remaining <= (rr = (double)(thickness + 2.0f + 16.0f)) * rr;
    }

    private static boolean isDebugEnabled(CarverConfiguration configuration) {
        return SharedConstants.DEBUG_CARVERS || configuration.debugSettings.isDebugMode();
    }

    public static interface CarveSkipChecker {
        public boolean shouldSkip(CarvingContext var1, double var2, double var4, double var6, int var8);
    }
}

