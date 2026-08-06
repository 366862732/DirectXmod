/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Octree {
    private final Branch root;
    private final BlockPos cameraSectionCenter;

    public Octree(SectionPos cameraSection, int renderDistance, int sectionsPerChunk, int minBlockY) {
        int visibleAreaDiameterInSections = renderDistance * 2 + 1;
        int boundingBoxSizeInSections = Mth.smallestEncompassingPowerOfTwo(visibleAreaDiameterInSections);
        int distanceToBBEdgeInBlocks = renderDistance * 16;
        BlockPos cameraSectionOrigin = cameraSection.origin();
        this.cameraSectionCenter = cameraSection.center();
        int minX = cameraSectionOrigin.getX() - distanceToBBEdgeInBlocks;
        int maxX = minX + boundingBoxSizeInSections * 16 - 1;
        int minY = boundingBoxSizeInSections >= sectionsPerChunk ? minBlockY : cameraSectionOrigin.getY() - distanceToBBEdgeInBlocks;
        int maxY = minY + boundingBoxSizeInSections * 16 - 1;
        int minZ = cameraSectionOrigin.getZ() - distanceToBBEdgeInBlocks;
        int maxZ = minZ + boundingBoxSizeInSections * 16 - 1;
        this.root = new Branch(this, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public boolean add(SectionRenderDispatcher.RenderSection section) {
        return this.root.add(section);
    }

    public void visitNodes(OctreeVisitor visitor, Frustum frustum, int closeDistance) {
        this.root.visitNodes(visitor, false, frustum, 0, closeDistance, true);
    }

    private boolean isClose(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int closeDistance) {
        int cameraX = this.cameraSectionCenter.getX();
        int cameraY = this.cameraSectionCenter.getY();
        int cameraZ = this.cameraSectionCenter.getZ();
        return (double)cameraX > minX - (double)closeDistance && (double)cameraX < maxX + (double)closeDistance && (double)cameraY > minY - (double)closeDistance && (double)cameraY < maxY + (double)closeDistance && (double)cameraZ > minZ - (double)closeDistance && (double)cameraZ < maxZ + (double)closeDistance;
    }

    @Environment(value=EnvType.CLIENT)
    private class Branch
    implements Node {
        private final @Nullable Node[] nodes;
        private final BoundingBox boundingBox;
        private final int bbCenterX;
        private final int bbCenterY;
        private final int bbCenterZ;
        private final AxisSorting sorting;
        private final boolean cameraXDiffNegative;
        private final boolean cameraYDiffNegative;
        private final boolean cameraZDiffNegative;
        final /* synthetic */ Octree this$0;

        public Branch(Octree octree, BoundingBox boundingBox) {
            Octree octree2 = octree;
            Objects.requireNonNull(octree2);
            this.this$0 = octree2;
            this.nodes = new Node[8];
            this.boundingBox = boundingBox;
            this.bbCenterX = this.boundingBox.minX() + this.boundingBox.getXSpan() / 2;
            this.bbCenterY = this.boundingBox.minY() + this.boundingBox.getYSpan() / 2;
            this.bbCenterZ = this.boundingBox.minZ() + this.boundingBox.getZSpan() / 2;
            int cameraXDiff = octree.cameraSectionCenter.getX() - this.bbCenterX;
            int cameraYDiff = octree.cameraSectionCenter.getY() - this.bbCenterY;
            int cameraZDiff = octree.cameraSectionCenter.getZ() - this.bbCenterZ;
            this.sorting = AxisSorting.getAxisSorting(Math.abs(cameraXDiff), Math.abs(cameraYDiff), Math.abs(cameraZDiff));
            this.cameraXDiffNegative = cameraXDiff < 0;
            this.cameraYDiffNegative = cameraYDiff < 0;
            this.cameraZDiffNegative = cameraZDiff < 0;
        }

        public boolean add(SectionRenderDispatcher.RenderSection section) {
            long sectionNode = section.getSectionNode();
            boolean sectionXDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode)) - this.bbCenterX < 0;
            boolean sectionYDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode)) - this.bbCenterY < 0;
            boolean sectionZDiffNegative = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode)) - this.bbCenterZ < 0;
            boolean xDiffsOppositeSides = sectionXDiffNegative != this.cameraXDiffNegative;
            boolean yDiffsOppositeSides = sectionYDiffNegative != this.cameraYDiffNegative;
            boolean zDiffsOppositeSides = sectionZDiffNegative != this.cameraZDiffNegative;
            int nodeIndex = Branch.getNodeIndex(this.sorting, xDiffsOppositeSides, yDiffsOppositeSides, zDiffsOppositeSides);
            if (this.areChildrenLeaves()) {
                boolean alreadyExisted = this.nodes[nodeIndex] != null;
                this.nodes[nodeIndex] = new Leaf(this.this$0, section);
                return !alreadyExisted;
            }
            if (this.nodes[nodeIndex] != null) {
                Branch branch = (Branch)this.nodes[nodeIndex];
                return branch.add(section);
            }
            BoundingBox childBoundingBox = this.createChildBoundingBox(sectionXDiffNegative, sectionYDiffNegative, sectionZDiffNegative);
            Branch branch = new Branch(this.this$0, childBoundingBox);
            this.nodes[nodeIndex] = branch;
            return branch.add(section);
        }

        private static int getNodeIndex(AxisSorting sorting, boolean xDiffsOppositeSides, boolean yDiffsOppositeSides, boolean zDiffsOppositeSides) {
            int index = 0;
            if (xDiffsOppositeSides) {
                index += sorting.xShift;
            }
            if (yDiffsOppositeSides) {
                index += sorting.yShift;
            }
            if (zDiffsOppositeSides) {
                index += sorting.zShift;
            }
            return index;
        }

        private boolean areChildrenLeaves() {
            return this.boundingBox.getXSpan() == 32;
        }

        private BoundingBox createChildBoundingBox(boolean sectionXDiffNegative, boolean sectionYDiffNegative, boolean sectionZDiffNegative) {
            int maxZ;
            int minZ;
            int maxY;
            int minY;
            int maxX;
            int minX;
            if (sectionXDiffNegative) {
                minX = this.boundingBox.minX();
                maxX = this.bbCenterX - 1;
            } else {
                minX = this.bbCenterX;
                maxX = this.boundingBox.maxX();
            }
            if (sectionYDiffNegative) {
                minY = this.boundingBox.minY();
                maxY = this.bbCenterY - 1;
            } else {
                minY = this.bbCenterY;
                maxY = this.boundingBox.maxY();
            }
            if (sectionZDiffNegative) {
                minZ = this.boundingBox.minZ();
                maxZ = this.bbCenterZ - 1;
            } else {
                minZ = this.bbCenterZ;
                maxZ = this.boundingBox.maxZ();
            }
            return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        @Override
        public void visitNodes(OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, int closeDistance, boolean isClose) {
            boolean isVisible = skipFrustumCheck;
            if (!skipFrustumCheck) {
                int checkResult = frustum.cubeInFrustum(this.boundingBox);
                skipFrustumCheck = checkResult == -2;
                boolean bl = isVisible = checkResult == -2 || checkResult == -1;
            }
            if (isVisible) {
                isClose = isClose && this.this$0.isClose(this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ(), closeDistance);
                visitor.visit(this, skipFrustumCheck, depth, isClose);
                for (Node node : this.nodes) {
                    if (node == null) continue;
                    node.visitNodes(visitor, skipFrustumCheck, frustum, depth + 1, closeDistance, isClose);
                }
            }
        }

        @Override
        public @Nullable SectionRenderDispatcher.RenderSection getSection() {
            return null;
        }

        @Override
        public AABB getAABB() {
            return new AABB(this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() + 1, this.boundingBox.maxY() + 1, this.boundingBox.maxZ() + 1);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface OctreeVisitor {
        public void visit(Node var1, boolean var2, int var3, boolean var4);
    }

    @Environment(value=EnvType.CLIENT)
    private static enum AxisSorting {
        XYZ(4, 2, 1),
        XZY(4, 1, 2),
        YXZ(2, 4, 1),
        YZX(1, 4, 2),
        ZXY(2, 1, 4),
        ZYX(1, 2, 4);

        private final int xShift;
        private final int yShift;
        private final int zShift;

        private AxisSorting(int xShift, int yShift, int zShift) {
            this.xShift = xShift;
            this.yShift = yShift;
            this.zShift = zShift;
        }

        public static AxisSorting getAxisSorting(int absXDiff, int absYDiff, int absZDiff) {
            if (absXDiff > absYDiff && absXDiff > absZDiff) {
                if (absYDiff > absZDiff) {
                    return XYZ;
                }
                return XZY;
            }
            if (absYDiff > absXDiff && absYDiff > absZDiff) {
                if (absXDiff > absZDiff) {
                    return YXZ;
                }
                return YZX;
            }
            if (absXDiff > absYDiff) {
                return ZXY;
            }
            return ZYX;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Node {
        public void visitNodes(OctreeVisitor var1, boolean var2, Frustum var3, int var4, int var5, boolean var6);

        public @Nullable SectionRenderDispatcher.RenderSection getSection();

        public AABB getAABB();
    }

    @Environment(value=EnvType.CLIENT)
    private final class Leaf
    implements Node {
        private final SectionRenderDispatcher.RenderSection section;
        final /* synthetic */ Octree this$0;

        private Leaf(Octree octree, SectionRenderDispatcher.RenderSection section) {
            Octree octree2 = octree;
            Objects.requireNonNull(octree2);
            this.this$0 = octree2;
            this.section = section;
        }

        @Override
        public void visitNodes(OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, int closeDistance, boolean isClose) {
            AABB boundingBox = this.section.getBoundingBox();
            if (skipFrustumCheck || frustum.isVisible(this.getSection().getBoundingBox())) {
                isClose = isClose && this.this$0.isClose(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, closeDistance);
                visitor.visit(this, skipFrustumCheck, depth, isClose);
            }
        }

        @Override
        public SectionRenderDispatcher.RenderSection getSection() {
            return this.section;
        }

        @Override
        public AABB getAABB() {
            return this.section.getBoundingBox();
        }
    }
}

