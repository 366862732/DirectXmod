package com.xgdt.dx11.perf.culling;

import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;

/**
 * Drops terrain faces that are buried inside a canopy.
 *
 * <p>Vanilla already culls a face when the neighbouring block's face occlusion
 * shape covers it: {@code Block.shouldRenderFace} compares the two shapes and
 * returns early for a full {@code Shapes.block()} face.  Leaves opt out of that
 * because a leaf block's occlusion shape is empty, so every leaf-to-leaf face
 * survives into the mesh even when it is three blocks deep inside a canopy.</p>
 *
 * <p>This rule (the idea is MoreCulling's; no code is taken) culls a face only
 * when the block being rendered and the block it faces are both leaves
 * <em>and</em> at least {@code leafCullDepth - 1} further leaf blocks continue
 * behind the neighbour.  The default depth of two therefore removes faces that
 * have a second leaf layer in front of them, which is not distinguishable from
 * the first layer through a cutout texture, while a one-layer canopy keeps its
 * outer surface.</p>
 *
 * <p>All decisions are made from block states through the render region.  The
 * region only holds the rendered section plus one section in each direction, so
 * probes are rejected when they would leave that window: a missing answer must
 * never cull a face.</p>
 */
public final class LeafFaceCulling {
    /** Section edge in blocks; the render region spans one section each way. */
    private static final int SECTION_SHIFT = 4;

    private static long culledFaces;

    private LeafFaceCulling() {
    }

    /**
     * Pure decision: is a face worth dropping?
     *
     * @param depth           consecutive leaf blocks required behind the face
     * @param neighbourIsLeaf whether the block across the face is a leaf
     * @param extraLeafMask   bit {@code step - 1} set when the block {@code step}
     *                        blocks behind the neighbour is a leaf; missing bits
     *                        (unknown or non-leaf) always keep the face
     */
    public static boolean shouldCull(int depth, boolean neighbourIsLeaf, int extraLeafMask) {
        if (!neighbourIsLeaf || depth < 1) {
            return false;
        }
        for (int step = 1; step < depth; step++) {
            if ((extraLeafMask & (1 << (step - 1))) == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * True when two block coordinates are at most one section apart, i.e. when
     * the second one is still inside the render region of the first.
     */
    public static boolean withinOneSection(int fromBlock, int toBlock) {
        int delta = (fromBlock >> SECTION_SHIFT) - (toBlock >> SECTION_SHIFT);
        return delta >= -1 && delta <= 1;
    }

    /** Records one culled face for {@code /dx11perf stats}. */
    public static void faceCulled() {
        culledFaces++;
    }

    public static void resetStats() {
        culledFaces = 0;
    }

    public static String describe() {
        PerfConfig config = PerfState.current();
        if (config == null || !config.cullLeafFaces) {
            return "leafCull=off";
        }
        return "leafCull=on"
            + " depth=" + config.leafCullDepth
            + " culled=" + culledFaces;
    }
}
