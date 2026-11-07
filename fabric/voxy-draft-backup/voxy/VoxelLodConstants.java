package com.xgdt.dx12.lod.voxy;

/**
 * Constants for Voxy-style hierarchical voxel LOD system.
 * Mirrors Voxy's 8-level octree LOD with 32x32x32 sections per node.
 */
public final class VoxelLodConstants {
    private VoxelLodConstants() {}

    /** Maximum LOD level (0 = finest, 7 = coarsest). Voxy uses 8 levels. */
    public static final int MAX_LOD_LEVEL = 7;
    /** Number of LOD levels. */
    public static final int LOD_LEVELS = MAX_LOD_LEVEL + 1;

    /** Section size in blocks (Voxy uses 32x32x32). */
    public static final int SECTION_SIZE = 32;
    /** Section size in blocks per axis. */
    public static final int SECTION_BLOCKS = SECTION_SIZE;
    /** Section volume (32*32*32 = 32768). */
    public static final int SECTION_VOLUME = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    /** Section face area (32*32 = 1024). */
    public static final int SECTION_FACE_AREA = SECTION_SIZE * SECTION_SIZE;

    /** Child count per node (octree = 8). */
    public static final int CHILD_COUNT = 8;
    /** Bits per child index (3 bits for 8 children). */
    public static final int CHILD_INDEX_BITS = 3;

    /** Maximum number of nodes per LOD level. */
    public static final int MAX_NODES_PER_LEVEL = 1 << 20; // 1M nodes per level
    /** Total maximum nodes across all levels. */
    public static final int MAX_TOTAL_NODES = MAX_NODES_PER_LEVEL * LOD_LEVELS;

    /** Maximum geometry capacity in bytes (512 MB for geometry buffers). */
    public static final long MAX_GEOMETRY_CAPACITY = 512L * 1024 * 1024;
    /** Maximum geometry buffer count. */
    public static final int MAX_GEOMETRY_BUFFERS = 8;

    /** Quad size in bytes (8 bytes = 2 uints for position + data). */
    public static final int QUAD_SIZE_BYTES = 8;
    /** Maximum quads per section (65536). */
    public static final int MAX_QUADS_PER_SECTION = 65536;

    /** Compute shader workgroup size. */
    public static final int WORKGROUP_SIZE = 64;
    /** Local work size bits (5 = 32 threads). */
    public static final int LOCAL_WORK_SIZE_BITS = 5;

    /** Request queue max size. */
    public static final int MAX_REQUEST_QUEUE_SIZE = 50;
    /** Node queue max size. */
    public static final int MAX_QUEUE_SIZE = 200_000;

    /** Uniform buffer slot size (256 byte alignment). */
    public static final int UNIFORM_SLOT_SIZE = 256;
    /** In-flight uniform slots. */
    public static final int UNIFORM_RING_SIZE = 3;

    /** Geometry ring buffers (matches frame in-flight count). */
    public static final int GEOMETRY_RING_SIZE = 3;

    /** Visibility buffer format (R32_UINT for frame IDs). */
    public static final int VISIBILITY_BUFFER_FORMAT = 0x8236; // R32_UINT

    /** Node data buffer stride (16 bytes = 2 x uint64). */
    public static final int NODE_DATA_STRIDE = 16;

    /** Section metadata size in bytes (32 bytes). */
    public static final int SECTION_METADATA_SIZE = 32;

    /** Maximum render distance in sections. */
    public static final int MAX_RENDER_DISTANCE_SECTIONS = 64;
    /** Minimum render distance in sections. */
    public static final int MIN_RENDER_DISTANCE_SECTIONS = 4;

    /** Root section size in blocks (32 * 2^7 = 4096). */
    public static final int ROOT_SECTION_SIZE = SECTION_SIZE << MAX_LOD_LEVEL;

    /** Sky light bit mask in packed data. */
    public static final long SKY_LIGHT_MASK = 0xFFL << 56;
    /** Block light mask. */
    public static final long BLOCK_LIGHT_MASK = 0xFL << 59;
    /** Light emission mask. */
    public static final long EMISSION_MASK = 0xFL << 63;

    /** Face direction encoding: axis * 2 + side (0=-X, 1=+X, 2=-Z, 3=+Z, 4=-Y, 5=+Y). */
    public static final int FACE_COUNT = 6;

    // Face indices
    public static final int FACE_NEG_X = 0;
    public static final int FACE_POS_X = 1;
    public static final int FACE_NEG_Z = 2;
    public static final int FACE_POS_Z = 3;
    public static final int FACE_NEG_Y = 4;
    public static final int FACE_POS_Y = 5;

    /** Child octant encoding: bit 0 = +X, bit 1 = +Y, bit 2 = +Z. */
    public static final int OCTANT_NEG_X_NEG_Y_NEG_Z = 0;
    public static final int OCTANT_POS_X_NEG_Y_NEG_Z = 1;
    public static final int OCTANT_NEG_X_POS_Y_NEG_Z = 2;
    public static final int OCTANT_POS_X_POS_Y_NEG_Z = 3;
    public static final int OCTANT_NEG_X_NEG_Y_POS_Z = 4;
    public static final int OCTANT_POS_X_NEG_Y_POS_Z = 5;
    public static final int OCTANT_NEG_X_POS_Y_POS_Z = 6;
    public static final int OCTANT_POS_X_POS_Y_POS_Z = 7;

    /** Invalid node ID. */
    public static final int INVALID_NODE_ID = -1;
    /** Invalid buffer offset. */
    public static final int INVALID_BUFFER_OFFSET = -1;

    /** Minimum geometry free space required for uploads (50 MB). */
    public static final long MIN_GEOMETRY_FREE_SPACE = 50_000_000L;
    /** Maximum geometry upload per frame (1 MB). */
    public static final long MAX_GEOMETRY_UPLOAD_PER_FRAME = 1L << 20;
}