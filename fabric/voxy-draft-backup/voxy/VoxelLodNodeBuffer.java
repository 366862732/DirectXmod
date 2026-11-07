package com.xgdt.dx12.lod.voxy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.xgdt.dx12.dx12.Dx12GpuBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the hierarchical octree node buffer for Voxy-style LOD.
 * Handles allocation, deallocation, and GPU buffer synchronization.
 */
@Environment(EnvType.CLIENT)
public final class VoxelLodNodeBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-voxy-nodes");

    /** Maximum nodes per LOD level. */
    private final int maxNodesPerLevel;
    /** Total nodes across all levels. */
    private final int maxTotalNodes;
    /** Number of LOD levels. */
    private final int lodLevels;

    /** Node pool per LOD level. */
    private final VoxelLodNode[][] nodePools;
    /** Free list per LOD level (simple array-based stack). */
    private final int[][] freeLists;
    /** Free list tops per LOD level. */
    private final int[] freeListTops;
    /** Allocated count per LOD level. */
    private final int[] allocatedCounts;

    /** GPU node data buffer (16 bytes per node: 2 x uint64). */
    private Dx12GpuBuffer nodeDataBuffer;
    /** GPU node data mapped view. */
    private GpuBufferSlice.MappedView nodeDataView;
    /** GPU node data byte buffer. */
    private ByteBuffer nodeDataBytes;
    /** Current GPU buffer capacity in nodes. */
    private int gpuNodeCapacity = 0;

    /** Top-level node IDs (LOD 7) for traversal root. */
    private final int[] topLevelNodes;
    /** Top-level node count. */
    private int topLevelNodeCount = 0;

    /** Frame counter for temporal coherence. */
    private int currentFrame = 0;
    /** Dirty nodes that need GPU upload. */
    private final int[] dirtyNodes;
    private int dirtyNodeCount = 0;

    /** Allocation arena for fast node allocation. */
    private final AtomicInteger allocationCounter = new AtomicInteger(0);

    public VoxelLodNodeBuffer(int maxNodesPerLevel, int lodLevels) {
        this.maxNodesPerLevel = maxNodesPerLevel;
        this.lodLevels = lodLevels;
        this.maxTotalNodes = maxNodesPerLevel * lodLevels;

        this.nodePools = new VoxelLodNode[lodLevels][maxNodesPerLevel];
        this.freeLists = new int[lodLevels][maxNodesPerLevel];
        this.freeListTops = new int[lodLevels];
        this.allocatedCounts = new int[lodLevels];
        this.topLevelNodes = new int[MAX_NODES_PER_LEVEL]; // Max top-level nodes
        this.dirtyNodes = new int[maxTotalNodes];

        // Initialize free lists
        for (int level = 0; level < lodLevels; level++) {
            for (int i = 0; i < maxNodesPerLevel; i++) {
                this.freeLists[level][i] = i;
            }
            this.freeListTops[level] = maxNodesPerLevel;
        }
    }

    /**
     * Allocate a new node at the specified LOD level.
     * @param sectionX Section X coordinate
     * @param sectionY Section Y coordinate
     * @param sectionZ Section Z coordinate
     * @param lodLevel LOD level (0-7)
     * @return Node ID (packed as level << 20 | index), or INVALID_NODE_ID if full
     */
    public int allocateNode(int sectionX, int sectionY, int sectionZ, int lodLevel) {
        if (lodLevel < 0 || lodLevel >= this.lodLevels) {
            return VoxelLodConstants.INVALID_NODE_ID;
        }
        if (this.freeListTops[lodLevel] == 0) {
            // Try to expand or return invalid
            LOGGER.warn("[voxy] Node pool exhausted at LOD {}", lodLevel);
            return VoxelLodConstants.INVALID_NODE_ID;
        }

        int index = this.freeLists[lodLevel][--this.freeListTops[lodLevel]];
        VoxelLodNode node = this.nodePools[lodLevel][index];
        if (node == null) {
            node = new VoxelLodNode(sectionX, sectionY, sectionZ, lodLevel);
            this.nodePools[lodLevel][index] = node;
        } else {
            // Reuse existing node object
            node.sectionPos.set(sectionX, sectionY, sectionZ);
            node.lodLevel = lodLevel;
            node.isLeaf = (lodLevel == 0);
            node.reset();

            // Recompute bounds
            int blockSize = VoxelLodConstants.SECTION_SIZE << lodLevel;
            node.minX = sectionX * blockSize;
            node.minY = sectionY * blockSize;
            node.minZ = sectionZ * blockSize;
            node.maxX = node.minX + blockSize;
            node.maxY = node.minY + blockSize;
            node.maxZ = node.minZ + blockSize;
            node.computeAABB();
        }

        this.allocatedCounts[lodLevel]++;
        int nodeId = packNodeId(lodLevel, index);

        // If this is a top-level node (LOD 7), add to root list
        if (lodLevel == this.lodLevels - 1) {
            if (this.topLevelNodeCount < this.topLevelNodes.length) {
                this.topLevelNodes[this.topLevelNodeCount++] = nodeId;
            }
        }

        markDirty(nodeId);
        return nodeId;
    }

    /**
     * Free a node and return it to the pool.
     */
    public void freeNode(int nodeId) {
        int lodLevel = unpackLevel(nodeId);
        int index = unpackIndex(nodeId);

        if (lodLevel < 0 || lodLevel >= this.lodLevels || index < 0 || index >= this.maxNodesPerLevel) {
            return;
        }

        VoxelLodNode node = this.nodePools[lodLevel][index];
        if (node != null) {
            // Recursively free children
            for (int i = 0; i < 8; i++) {
                int childId = node.childIds[i];
                if (childId != VoxelLodConstants.INVALID_NODE_ID) {
                    freeNode(childId);
                    node.childIds[i] = VoxelLodConstants.INVALID_NODE_ID;
                }
            }
            node.reset();
        }

        // Return to free list
        if (this.freeListTops[lodLevel] < this.maxNodesPerLevel) {
            this.freeLists[lodLevel][this.freeListTops[lodLevel]++] = index;
        }
        this.allocatedCounts[lodLevel]--;

        // Remove from top-level list if needed
        if (lodLevel == this.lodLevels - 1) {
            for (int i = 0; i < this.topLevelNodeCount; i++) {
                if (this.topLevelNodes[i] == nodeId) {
                    this.topLevelNodes[i] = this.topLevelNodes[--this.topLevelNodeCount];
                    break;
                }
            }
        }

        markDirty(nodeId);
    }

    /** Get a node by its packed ID. */
    public VoxelLodNode getNode(int nodeId) {
        int lodLevel = unpackLevel(nodeId);
        int index = unpackIndex(nodeId);
        if (lodLevel < 0 || lodLevel >= this.lodLevels || index < 0 || index >= this.maxNodesPerLevel) {
            return null;
        }
        return this.nodePools[lodLevel][index];
    }

    /** Get or create a child node. */
    public int getOrCreateChild(int parentId, int octant) {
        VoxelLodNode parent = getNode(parentId);
        if (parent == null || parent.isLeaf) {
            return VoxelLodConstants.INVALID_NODE_ID;
        }

        int childId = parent.childIds[octant];
        if (childId != VoxelLodConstants.INVALID_NODE_ID) {
            return childId;
        }

        Vector3i childPos = parent.getChildSectionPos(octant);
        if (childPos == null) {
            return VoxelLodConstants.INVALID_NODE_ID;
        }

        childId = allocateNode(childPos.x, childPos.y, childPos.z, parent.lodLevel - 1);
        if (childId != VoxelLodConstants.INVALID_NODE_ID) {
            parent.childIds[octant] = childId;
            VoxelLodNode child = getNode(childId);
            if (child != null) {
                child.parentId = parentId;
            }
            parent.dirty = true;
            markDirty(parentId);
        }
        return childId;
    }

    /** Pack level and index into a single 32-bit ID. */
    public static int packNodeId(int level, int index) {
        return (level << 20) | (index & 0xFFFFF);
    }

    /** Unpack level from node ID. */
    public static int unpackLevel(int nodeId) {
        return nodeId >>> 20;
    }

    /** Unpack index from node ID. */
    public static int unpackIndex(int nodeId) {
        return nodeId & 0xFFFFF;
    }

    /** Mark a node as dirty (needs GPU upload). */
    public void markDirty(int nodeId) {
        if (this.dirtyNodeCount < this.dirtyNodes.length) {
            this.dirtyNodes[this.dirtyNodeCount++] = nodeId;
        }
    }

    /** Get dirty nodes since last flush. */
    public int[] getDirtyNodes() {
        int[] result = new int[this.dirtyNodeCount];
        System.arraycopy(this.dirtyNodes, 0, result, 0, this.dirtyNodeCount);
        this.dirtyNodeCount = 0;
        return result;
    }

    /** Get all top-level node IDs. */
    public int[] getTopLevelNodes() {
        int[] result = new int[this.topLevelNodeCount];
        System.arraycopy(this.topLevelNodes, 0, result, 0, this.topLevelNodeCount);
        return result;
    }

    /** Ensure GPU buffer has enough capacity. */
    public void ensureGpuBuffer(int requiredNodes) {
        if (this.gpuNodeCapacity >= requiredNodes) {
            return;
        }

        int newCapacity = Math.max(requiredNodes, Math.max(this.gpuNodeCapacity * 2, 1024));
        long bytes = (long) newCapacity * VoxelLodConstants.NODE_DATA_STRIDE;

        if (this.nodeDataView != null) {
            try { this.nodeDataView.close(); } catch (Throwable ignored) {}
            this.nodeDataView = null;
        }
        if (this.nodeDataBuffer != null) {
            try { this.nodeDataBuffer.close(); } catch (Throwable ignored) {}
            this.nodeDataBuffer = null;
        }

        this.nodeDataBuffer = new Dx12GpuBuffer(
            VoxelLodConstants.BUFFER_USAGE_UAV | GpuBuffer.USAGE_STORAGE_BUFFER,
            bytes);
        this.nodeDataView = this.nodeDataBuffer.map(0L, bytes, false, true);
        this.nodeDataBytes = this.nodeDataView.data();
        this.nodeDataBytes.order(ByteOrder.LITTLE_ENDIAN);
        this.gpuNodeCapacity = newCapacity;

        LOGGER.info("[voxy] Node buffer expanded to {} nodes ({} MB)", newCapacity, bytes / (1024*1024));
    }

    /** Upload dirty nodes to GPU buffer. */
    public void uploadDirtyNodes() {
        if (this.dirtyNodeCount == 0) return;

        ensureGpuBuffer(this.maxTotalNodes);

        for (int i = 0; i < this.dirtyNodeCount; i++) {
            int nodeId = this.dirtyNodes[i];
            VoxelLodNode node = getNode(nodeId);
            if (node != null) {
                writeNodeToGpu(nodeId, node);
                node.uploaded = true;
                node.dirty = false;
            }
        }
        this.dirtyNodeCount = 0;
    }

    /** Write a single node's data to GPU buffer. */
    private void writeNodeToGpu(int nodeId, VoxelLodNode node) {
        int index = unpackIndex(nodeId);
        int level = unpackLevel(nodeId);
        long offset = ((long) level * this.maxNodesPerLevel + index) * VoxelLodConstants.NODE_DATA_STRIDE;

        // Node data format (16 bytes):
        // uint64 0: geometryOffset (32 bits) | quadCount (32 bits)
        // uint64 1: aabbMin (32 bits) | aabbMax (32 bits)
        long data0 = ((long) (node.geometryOffset & 0xFFFFFFFFL) << 32) |
                     (node.quadCount & 0xFFFFFFFFL);
        long data1 = ((long) (node.aabbMin & 0xFFFFFFFFL) << 32) |
                     (node.aabbMax & 0xFFFFFFFFL);

        this.nodeDataBytes.putLong(offset, data0);
        this.nodeDataBytes.putLong(offset + 8, data1);
    }

    /** Advance frame counter. */
    public void nextFrame() {
        this.currentFrame++;
    }

    /** Get current frame ID. */
    public int getCurrentFrame() {
        return this.currentFrame;
    }

    /** Get GPU buffer handle. */
    public long getGpuBufferHandle() {
        return this.nodeDataBuffer != null ? this.nodeDataBuffer.handle() : 0L;
    }

    /** Get allocated node count per level. */
    public int[] getAllocatedCounts() {
        return Arrays.copyOf(this.allocatedCounts, this.lodLevels);
    }

    /** Clear all nodes. */
    public void clear() {
        for (int level = 0; level < this.lodLevels; level++) {
            for (int i = 0; i < this.allocatedCounts[level]; i++) {
                VoxelLodNode node = this.nodePools[level][i];
                if (node != null) {
                    node.reset();
                }
            }
            this.freeListTops[level] = this.maxNodesPerLevel;
            for (int i = 0; i < this.maxNodesPerLevel; i++) {
                this.freeLists[level][i] = i;
            }
            this.allocatedCounts[level] = 0;
        }
        this.topLevelNodeCount = 0;
        this.dirtyNodeCount = 0;
    }

    /** Close and release resources. */
    public void close() {
        if (this.nodeDataView != null) {
            try { this.nodeDataView.close(); } catch (Throwable ignored) {}
            this.nodeDataView = null;
        }
        if (this.nodeDataBuffer != null) {
            try { this.nodeDataBuffer.close(); } catch (Throwable ignored) {}
            this.nodeDataBuffer = null;
        }
        this.gpuNodeCapacity = 0;
    }
}