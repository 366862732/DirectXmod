package com.xgdt.dx12.lod.voxy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.xgdt.dx12.dx12.Dx12GpuBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3i;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents a single node in the Voxy-style hierarchical voxel LOD octree.
 * Each node covers a 32x32x32 block region at its LOD level.
 */
@Environment(EnvType.CLIENT)
public final class VoxelLodNode {
    /** Node position in section coordinates (each section = 32 blocks). */
    public final Vector3i sectionPos;
    /** LOD level (0 = finest, 7 = coarsest). */
    public final int lodLevel;
    /** World-space bounds: min corner in blocks. */
    public final int minX, minY, minZ;
    /** World-space bounds: max corner in blocks (exclusive). */
    public final int maxX, maxY, maxZ;
    /** Child node IDs (8 octants), -1 if not created. */
    public final int[] childIds = new int[8];
    /** Geometry buffer offset (in quads) for this node's geometry. */
    public int geometryOffset = VoxelLodConstants.INVALID_BUFFER_OFFSET;
    /** Number of quads in this node's geometry. */
    public int quadCount = 0;
    /** AABB for frustum culling (packed as min/max in 32-bit each). */
    public int aabbMin = 0;
    public int aabbMax = 0;
    /** Frame ID when this node was last visible (for temporal coherence). */
    public int lastVisibleFrame = -1;
    /** Whether this node has been uploaded to GPU. */
    public boolean uploaded = false;
    /** Whether this node needs rebuild. */
    public boolean dirty = true;
    /** Whether this node is a leaf (LOD 0). */
    public boolean isLeaf;

    /** Parent node ID. */
    public int parentId = VoxelLodConstants.INVALID_NODE_ID;

    public VoxelLodNode(int x, int y, int z, int lodLevel) {
        this.sectionPos = new Vector3i(x, y, z);
        this.lodLevel = lodLevel;
        this.isLeaf = (lodLevel == 0);

        // Calculate world-space bounds
        int blockSize = VoxelLodConstants.SECTION_SIZE << lodLevel;
        this.minX = x * blockSize;
        this.minY = y * blockSize;
        this.minZ = z * blockSize;
        this.maxX = this.minX + blockSize;
        this.maxY = this.minY + blockSize;
        this.maxZ = this.minZ + blockSize;

        // Initialize children to invalid
        for (int i = 0; i < 8; i++) {
            this.childIds[i] = VoxelLodConstants.INVALID_NODE_ID;
        }

        // Compute AABB packing
        computeAABB();
    }

    /** Pack AABB into two 32-bit ints for GPU upload. */
    private void computeAABB() {
        // Pack as: minX(10) | minY(11) | minZ(11) for min, max similarly
        // Using 10+11+11 = 32 bits per corner
        this.aabbMin = packCorner(this.minX, this.minY, this.minZ);
        this.aabbMax = packCorner(this.maxX, this.maxY, this.maxZ);
    }

    private static int packCorner(int x, int y, int z) {
        // Assume coordinates fit in signed 10/11/11 bits
        // Shift to unsigned for packing
        int ux = (x + (1 << 25)) & 0x3FF;  // 10 bits
        int uy = (y + (1 << 25)) & 0x7FF;  // 11 bits
        int uz = (z + (1 << 25)) & 0x7FF;  // 11 bits
        return (ux << 22) | (uy << 11) | uz;
    }

    /** Check if a world-space point is inside this node's bounds. */
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x < this.maxX &&
               y >= this.minY && y < this.maxY &&
               z >= this.minZ && z < this.maxZ;
    }

    /** Get child octant index for a world position relative to this node. */
    public int getChildOctant(int x, int y, int z) {
        int halfX = (this.maxX - this.minX) >> 1;
        int halfY = (this.maxY - this.minY) >> 1;
        int halfZ = (this.maxZ - this.minZ) >> 1;
        int cx = this.minX + halfX;
        int cy = this.minY + halfY;
        int cz = this.minZ + halfZ;

        int octant = 0;
        if (x >= cx) octant |= 1;      // +X
        if (y >= cy) octant |= 2;      // +Y
        if (z >= cz) octant |= 4;      // +Z
        return octant;
    }

    /** Get child section position for a given octant. */
    public Vector3i getChildSectionPos(int octant) {
        int childLod = this.lodLevel - 1;
        if (childLod < 0) return null;

        int childSize = VoxelLodConstants.SECTION_SIZE << childLod;
        int halfSize = childSize * (VoxelLodConstants.SECTION_SIZE >> 1); // 16 sections at child level

        int cx = this.sectionPos.x * 2 + ((octant & 1) != 0 ? 1 : 0);
        int cy = this.sectionPos.y * 2 + ((octant & 2) != 0 ? 1 : 0);
        int cz = this.sectionPos.z * 2 + ((octant & 4) != 0 ? 1 : 0);

        return new Vector3i(cx, cy, cz);
    }

    /** Reset node state for reuse. */
    public void reset() {
        for (int i = 0; i < 8; i++) {
            this.childIds[i] = VoxelLodConstants.INVALID_NODE_ID;
        }
        this.geometryOffset = VoxelLodConstants.INVALID_BUFFER_OFFSET;
        this.quadCount = 0;
        this.lastVisibleFrame = -1;
        this.uploaded = false;
        this.dirty = true;
        this.parentId = VoxelLodConstants.INVALID_NODE_ID;
    }
}