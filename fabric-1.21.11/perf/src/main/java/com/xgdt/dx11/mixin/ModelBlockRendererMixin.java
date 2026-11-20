package com.xgdt.dx11.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import com.xgdt.dx11.perf.culling.LeafFaceCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Feeds vanilla's per-face visibility test into leaf face culling.
 *
 * <p>{@code ModelBlockRenderer.shouldRenderFace} is the single funnel for every
 * terrain face: both the smooth-lighting and the flat path call it once per
 * model part and direction, and it already returns false when the neighbour's
 * face occlusion shape covers the face.  Leaves have an empty occlusion shape,
 * so their interior faces reach the mesh; this hook removes the ones that have
 * a second leaf layer behind them.</p>
 *
 * <p>The hook only ever turns {@code true} into {@code false}, only when the
 * caller asked for side checking (the {@code cull} argument), and only when the
 * neighbour and the next {@code leafCullDepth - 1} blocks are all leaves.  A
 * probe that would leave the render region counts as "not a leaf", so a face is
 * never culled on missing information.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @ModifyReturnValue(
        method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Z"
            + "Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("RETURN"))
    private static boolean dx11$cullLeafFaces(
        boolean original,
        BlockAndTintGetter level,
        BlockState state,
        boolean cull,
        Direction direction,
        BlockPos neighbourPos) {
        if (!original || !cull) {
            return original;
        }
        PerfConfig config = PerfState.current();
        if (config == null || !config.cullLeafFaces) {
            return original;
        }
        // Hot gate: one class test rejects every non-leaf face.
        if (!(state.getBlock() instanceof LeavesBlock)) {
            return original;
        }
        if (!level.getBlockState(neighbourPos).is(BlockTags.LEAVES)) {
            return original;
        }

        int depth = config.leafCullDepth;
        int mask = 0;
        if (depth > 1) {
            BlockPos origin = neighbourPos.relative(direction.getOpposite());
            for (int step = 1; step < depth; step++) {
                BlockPos probe = neighbourPos.relative(direction, step);
                if (!insideRegion(origin, probe)) {
                    break;
                }
                if (!level.getBlockState(probe).is(BlockTags.LEAVES)) {
                    break;
                }
                mask |= 1 << (step - 1);
            }
        }
        if (!LeafFaceCulling.shouldCull(depth, true, mask)) {
            return original;
        }
        LeafFaceCulling.faceCulled();
        return false;
    }

    /**
     * The render region only holds the compiled section plus one section in
     * each direction, so a probe two sections away would index off the end of
     * the section array.
     */
    private static boolean insideRegion(BlockPos origin, BlockPos probe) {
        return LeafFaceCulling.withinOneSection(origin.getX(), probe.getX())
            && LeafFaceCulling.withinOneSection(origin.getY(), probe.getY())
            && LeafFaceCulling.withinOneSection(origin.getZ(), probe.getZ());
    }
}
