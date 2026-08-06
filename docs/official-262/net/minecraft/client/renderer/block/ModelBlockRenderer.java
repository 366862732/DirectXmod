/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.QuadInstance;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ModelBlockRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockModelLighter lighter;
    private final boolean ambientOcclusion;
    private final boolean cull;
    private final BlockColors blockColors;
    private final RandomSource random = RandomSource.createThreadLocalInstance(0L);
    private final List<BlockStateModelPart> parts = new ObjectArrayList();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private final QuadInstance quadInstance = new QuadInstance();
    private int tintCacheIndex = -1;
    private int tintCacheValue;
    private boolean tintSourcesInitialized;
    private final List<@Nullable BlockTintSource> tintSources = new ObjectArrayList();
    private final IntList computedTintValues = new IntArrayList();

    public ModelBlockRenderer(boolean ambientOcclusion, boolean cull, BlockColors blockColors) {
        this.lighter = new BlockModelLighter();
        this.ambientOcclusion = ambientOcclusion;
        this.cull = cull;
        this.blockColors = blockColors;
    }

    public static boolean forceOpaque(boolean cutoutLeaves, BlockState blockState) {
        return !cutoutLeaves && blockState.getBlock() instanceof LeavesBlock;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void tesselateBlock(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState blockState, BlockStateModel model, long seed) {
        this.random.setSeed(seed);
        model.collectParts(this.random, this.parts);
        if (this.parts.isEmpty()) {
            return;
        }
        try {
            Vec3 offset = blockState.getOffset(pos);
            if (this.ambientOcclusion && blockState.getLightEmission() == 0 && this.parts.getFirst().useAmbientOcclusion()) {
                this.tesselateAmbientOcclusion(output, x + (float)offset.x, y + (float)offset.y, z + (float)offset.z, this.parts, level, blockState, pos);
            } else {
                this.tesselateFlat(output, x + (float)offset.x, y + (float)offset.y, z + (float)offset.z, this.parts, level, blockState, pos);
            }
        }
        finally {
            this.parts.clear();
            this.resetTintCache();
        }
    }

    private void configureTintCache(BlockState blockState) {
        List<BlockTintSource> tintSources = this.blockColors.getTintSources(blockState);
        int tintSourceCount = tintSources.size();
        if (tintSourceCount > 0) {
            this.tintSources.addAll(tintSources);
            for (int i = 0; i < tintSourceCount; ++i) {
                this.computedTintValues.add(-1);
            }
        }
    }

    private void resetTintCache() {
        this.tintCacheIndex = -1;
        if (this.tintSourcesInitialized) {
            this.tintSources.clear();
            this.computedTintValues.clear();
            this.tintSourcesInitialized = false;
        }
    }

    private void tesselateAmbientOcclusion(BlockQuadOutput output, float x, float y, float z, List<BlockStateModelPart> parts, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        int cacheValid = 0;
        int shouldRenderFaceCache = 0;
        for (BlockStateModelPart part : parts) {
            for (Direction direction : DIRECTIONS) {
                List<BakedQuad> culledQuads;
                boolean shouldRenderFace;
                int cacheMask = 1 << direction.ordinal();
                boolean validCacheForDirection = (cacheValid & cacheMask) != 0;
                boolean bl = shouldRenderFace = (shouldRenderFaceCache & cacheMask) != 0;
                if (validCacheForDirection && !shouldRenderFace || (culledQuads = part.getQuads(direction)).isEmpty()) continue;
                if (!validCacheForDirection) {
                    shouldRenderFace = this.shouldRenderFace(level, state, direction, this.scratchPos.setWithOffset((Vec3i)pos, direction));
                    cacheValid |= cacheMask;
                    if (shouldRenderFace) {
                        shouldRenderFaceCache |= cacheMask;
                    }
                }
                if (!shouldRenderFace) continue;
                for (BakedQuad quad : culledQuads) {
                    this.lighter.prepareQuadAmbientOcclusion(level, state, pos, quad, this.quadInstance);
                    this.putQuadWithTint(output, x, y, z, level, state, pos, quad);
                }
            }
            List<BakedQuad> unculledQuads = part.getQuads(null);
            for (BakedQuad quad : unculledQuads) {
                this.lighter.prepareQuadAmbientOcclusion(level, state, pos, quad, this.quadInstance);
                this.putQuadWithTint(output, x, y, z, level, state, pos, quad);
            }
        }
    }

    private void tesselateFlat(BlockQuadOutput output, float x, float y, float z, List<BlockStateModelPart> parts, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        int cacheValid = 0;
        int shouldRenderFaceCache = 0;
        for (BlockStateModelPart part : parts) {
            for (Direction direction : DIRECTIONS) {
                List<BakedQuad> culledQuads;
                boolean shouldRenderFace;
                int cacheMask = 1 << direction.ordinal();
                boolean validCacheForDirection = (cacheValid & cacheMask) != 0;
                boolean bl = shouldRenderFace = (shouldRenderFaceCache & cacheMask) != 0;
                if (validCacheForDirection && !shouldRenderFace || (culledQuads = part.getQuads(direction)).isEmpty()) continue;
                BlockPos.MutableBlockPos relativePos = this.scratchPos.setWithOffset((Vec3i)pos, direction);
                if (!validCacheForDirection) {
                    shouldRenderFace = this.shouldRenderFace(level, state, direction, relativePos);
                    cacheValid |= cacheMask;
                    if (shouldRenderFace) {
                        shouldRenderFaceCache |= cacheMask;
                    }
                }
                if (!shouldRenderFace) continue;
                int lightCoords = this.lighter.getLightCoords(state, level, relativePos);
                for (BakedQuad quad : culledQuads) {
                    this.lighter.prepareQuadFlat(level, state, pos, lightCoords, quad, this.quadInstance);
                    this.putQuadWithTint(output, x, y, z, level, state, pos, quad);
                }
            }
            List<BakedQuad> unculledQuads = part.getQuads(null);
            for (BakedQuad quad : unculledQuads) {
                this.lighter.prepareQuadFlat(level, state, pos, -1, quad, this.quadInstance);
                this.putQuadWithTint(output, x, y, z, level, state, pos, quad);
            }
        }
    }

    private boolean shouldRenderFace(BlockAndTintGetter level, BlockState state, Direction direction, BlockPos neighborPos) {
        if (!this.cull) {
            return true;
        }
        BlockState neighborState = level.getBlockState(neighborPos);
        return Block.shouldRenderFace(state, neighborState, direction);
    }

    private void putQuadWithTint(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockState state, BlockPos pos, BakedQuad quad) {
        int tintIndex = quad.materialInfo().tintIndex();
        if (tintIndex != -1) {
            this.quadInstance.multiplyColor(this.getTintColor(level, state, pos, tintIndex));
        }
        output.put(x, y, z, quad, this.quadInstance);
    }

    private int getTintColor(BlockAndTintGetter level, BlockState state, BlockPos pos, int tintIndex) {
        if (this.tintCacheIndex == tintIndex) {
            return this.tintCacheValue;
        }
        int tintColor = this.computeTintColor(level, state, pos, tintIndex);
        this.tintCacheIndex = tintIndex;
        this.tintCacheValue = tintColor;
        return tintColor;
    }

    private int computeTintColor(BlockAndTintGetter level, BlockState state, BlockPos pos, int tintIndex) {
        if (!this.tintSourcesInitialized) {
            this.configureTintCache(state);
            this.tintSourcesInitialized = true;
        }
        if (tintIndex >= this.tintSources.size()) {
            return -1;
        }
        BlockTintSource tintSource = this.tintSources.set(tintIndex, null);
        if (tintSource != null) {
            int computedTintValue = tintSource.colorInWorld(state, level, pos);
            this.computedTintValues.set(tintIndex, computedTintValue);
            return computedTintValue;
        }
        return this.computedTintValues.getInt(tintIndex);
    }
}

