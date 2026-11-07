package com.xgdt.dx12.lod.voxy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * HLSL compute shaders for Voxy-style hierarchical voxel LOD.
 * Contains shaders for:
 * 1. Voxel geometry generation (build quads from 3D voxel data)
 * 2. HiZ buffer generation (depth pyramid for occlusion culling)
 * 3. Hierarchical frustum/occlusion traversal
 * 4. Indirect draw argument generation
 */
@Environment(EnvType.CLIENT)
public final class VoxelLodShaders {
    private VoxelLodShaders() {}

    // ============================================================
    // CONSTANT BUFFER LAYOUTS
    // ============================================================

    /** Scene uniform buffer (bind b0) - 256 bytes, matches VoxelLodConstants.UNIFORM_SLOT_SIZE */
    public static final String SCENE_UNIFORM = //
        "cbuffer SceneUniform : register(b0)\n" +
        "{\n" +
        "    float4x4 ViewProj;           // 64 bytes\n" +
        "    float4 CameraPos;            // 16 bytes\n" +
        "    float4 FrustumPlanes[6];     // 96 bytes (6 planes)\n" +
        "    float4 LodParams;            // x=maxLod, y=sectionSize, z=rootSize, w=time\n" +
        "    float4 CullParams;           // x=minQuadSize, y=maxNodes, z=frameId, w=padding\n" +
        "};\n";

    /** Node data structure (16 bytes per node, matches VoxelLodConstants.NODE_DATA_STRIDE) */
    public static final String NODE_DATA_STRUCT = //
        "struct NodeData\n" +
        "{\n" +
        "    uint geometryOffset;    // Offset in quad buffer\n" +
        "    uint quadCount;         // Number of quads\n" +
        "    uint aabbMin;           // Packed min corner\n" +
        "    uint aabbMax;           // Packed max corner\n" +
        "};\n";

    /** Quad structure (8 bytes per quad) */
    public static final String QUAD_STRUCT = //
        "struct Quad\n" +
        "{\n" +
        "    uint positionData;      // Packed vertex positions\n" +
        "    uint colorData;         // Packed color + flags\n" +
        "};\n";

    /** Push constants for compute shaders (matches Dx12 root constants) */
    public static final String BUILD_CONSTANTS = //
        "#define MAX_LOD_LEVEL 7\n" +
        "#define SECTION_SIZE 32\n" +
        "#define CHILD_COUNT 8\n" +
        "#define QUAD_SIZE_BYTES 8\n" +
        "#define NODE_DATA_STRIDE 16\n" +
        "#define WORKGROUP_SIZE 64\n" +
        "#define MAX_QUADS_PER_SECTION 65536\n";

    // ============================================================
    // SHARED UTILITIES
    // ============================================================

    public static final String SHARED_UTILITIES = //
        "// Unpack AABB corner\n" +
        "float3 unpackCorner(uint packed)\n" +
        "{\n" +
        "    uint x = (packed >> 22) & 0x3FF;\n" +
        "    uint y = (packed >> 11) & 0x7FF;\n" +
        "    uint z = packed & 0x7FF;\n" +
        "    // Convert from unsigned offset to signed world coords\n" +
        "    return float3(\n" +
        "        int(x) - (1 << 25),\n" +
        "        int(y) - (1 << 25),\n" +
        "        int(z) - (1 << 25)\n" +
        "    );\n" +
        "}\n" +
        "\n" +
        "// Frustum culling test (AABB vs 6 planes)\n" +
        "bool cullAABB(uint aabbMin, uint aabbMax)\n" +
        "{\n" +
        "    float3 min = unpackCorner(aabbMin);\n" +
        "    float3 max = unpackCorner(aabbMax);\n" +
        "    for (int i = 0; i < 6; i++)\n" +
        "    {\n" +
        "        float4 plane = FrustumPlanes[i];\n" +
        "        float3 p = (plane.xyz >= 0) ? max : min;\n" +
        "        if (dot(plane.xyz, p) + plane.w < 0)\n" +
        "            return true; // Culled\n" +
        "    }\n" +
        "    return false; // Visible\n" +
        "}\n" +
        "\n" +
        "// Pack quad position (4 vertices x 3 coords = 12 floats -> we pack into 2 uints)\n" +
        "// Using 21 bits per coordinate (signed 20 bits + sign)\n" +
        "uint packPosition(float3 v0, float3 v1, float3 v2, float3 v3)\n" +
        "{\n" +
        "    // Simplified: pack center + size\n" +
        "    float3 center = (v0 + v1 + v2 + v3) * 0.25;\n" +
        "    float3 size = max(max(abs(v0 - center), abs(v1 - center)),\n" +
        "                      max(abs(v2 - center), abs(v3 - center)));\n" +
        "    // Quantize\n" +
        "    uint cx = (uint)(center.x + 524288.0); // 20 bits\n" +
        "    uint cy = (uint)(center.y + 524288.0);\n" +
        "    uint cz = (uint)(center.z + 524288.0);\n" +
        "    uint sx = (uint)(size.x * 1024.0);\n" +
        "    uint sy = (uint)(size.y * 1024.0);\n" +
        "    uint sz = (uint)(size.z * 1024.0);\n" +
        "    return (cx & 0xFFFFF) | ((cy & 0xFFFFF) << 20) | ((cz & 0x3FF) << 40);\n" +
        "}\n" +
        "\n" +
        "// Pack color (RGB8 + alpha + flags)\n" +
        "uint packColor(uint rgb, uint alpha, uint flags)\n" +
        "{\n" +
        "    uint r = (rgb >> 16) & 0xFF;\n" +
        "    uint g = (rgb >> 8) & 0xFF;\n" +
        "    uint b = rgb & 0xFF;\n" +
        "    return r | (g << 8) | (b << 16) | (alpha << 24) | (flags << 28);\n" +
        "}\n" +
        "\n" +
        "// Hash for procedural variation\n" +
        "uint hash(uint x, uint y, uint z)\n" +
        "{\n" +
        "    uint h = x ^ (y << 11) ^ (z << 22);\n" +
        "    h ^= h >> 16;\n" +
        "    h *= 0x7FEB352D;\n" +
        "    h ^= h >> 15;\n" +
        "    h *= 0x846CA68B;\n" +
        "    h ^= h >> 16;\n" +
        "    return h;\n" +
        "}\n";

    // ============================================================
    // SHADER 1: VOXEL GEOMETRY GENERATION
    // ============================================================
    // Input: 3D voxel grid (palette indices + palette colors)
    // Output: Quads (greedy meshed faces)
    // Dispatch: One workgroup per section (32x32x32), 64 threads
    public static final String GEOMETRY_GEN_COMPUTE = SCENE_UNIFORM + NODE_DATA_STRUCT + QUAD_STRUCT + BUILD_CONSTANTS + SHARED_UTILITIES + //
        "StructuredBuffer<uint> VoxelPalette : register(t0);\n" +
        "StructuredBuffer<uint> VoxelBlocks : register(t1);\n" +
        "StructuredBuffer<uint> PaletteColors : register(t2);\n" +
        "RWStructuredBuffer<Quad> OutputQuads : register(u0);\n" +
        "RWStructuredBuffer<uint> QuadCounter : register(u1);\n" +
        "RWStructuredBuffer<uint> SectionInfo : register(u2); // [sectionId, quadStart, quadCount, lodLevel]\n" +
        "\n" +
        "[numthreads(8, 8, 1)]\n" +
        "void main(uint3 dtid : SV_DispatchThreadID)\n" +
        "{\n" +
        "    uint sectionId = dtid.z;\n" +
        "    if (sectionId >= SectionInfo[0]) return;\n" +
        "\n" +
        "    uint sectionBase = sectionId * (SECTION_SIZE * SECTION_SIZE * SECTION_SIZE);\n" +
        "    uint x = dtid.x;\n" +
        "    uint y = dtid.y;\n" +
        "    uint z = dtid.z; // Not used, we iterate Z in loop\n" +
        "\n" +
        "    // Greedy meshing in Z direction\n" +
        "    for (uint zi = 0; zi < SECTION_SIZE; zi++)\n" +
        "    {\n" +
        "        uint idx = (zi * SECTION_SIZE + y) * SECTION_SIZE + x;\n" +
        "        uint blockIdx = VoxelBlocks[sectionBase + idx];\n" +
        "        if (blockIdx == 0) continue; // Air\n" +
        "\n" +
        "        uint color = PaletteColors[blockIdx];\n" +
        "        if (color == 0xFFFFFFFF) continue; // Transparent\n" +
        "\n" +
        "        // Check 6 faces for exposure\n" +
        "        // This is a simplified version - full greedy meshing is more complex\n" +
        "        // For now, emit quads for each exposed face\n" +
        "\n" +
        "        // +X face\n" +
        "        if (x == SECTION_SIZE - 1 || VoxelBlocks[sectionBase + (zi * SECTION_SIZE + y) * SECTION_SIZE + x + 1] == 0)\n" +
        "        {\n" +
        "            uint counter;\n" +
        "            InterlockedAdd(QuadCounter[0], 1, counter);\n" +
        "            if (counter < MAX_QUADS_PER_SECTION)\n" +
        "            {\n" +
        "                float3 v0 = float3(x + 1, y, zi);\n" +
        "                float3 v1 = float3(x + 1, y + 1, zi);\n" +
        "                float3 v2 = float3(x + 1, y + 1, zi + 1);\n" +
        "                float3 v3 = float3(x + 1, y, zi + 1);\n" +
        "                OutputQuads[counter] = Quad(packPosition(v0,v1,v2,v3), packColor(color, 255, 0));\n" +
        "            }\n" +
        "        }\n" +
        "        // -X face\n" +
        "        if (x == 0 || VoxelBlocks[sectionBase + (zi * SECTION_SIZE + y) * SECTION_SIZE + x - 1] == 0)\n" +
        "        {\n" +
        "            uint counter;\n" +
        "            InterlockedAdd(QuadCounter[0], 1, counter);\n" +
        "            if (counter < MAX_QUADS_PER_SECTION)\n" +
        "            {\n" +
        "                float3 v0 = float3(x, y, zi + 1);\n" +
        "                float3 v1 = float3(x, y + 1, zi + 1);\n" +
        "                float3 v2 = float3(x, y + 1, zi);\n" +
        "                float3 v3 = float3(x, y, zi);\n" +
        "                OutputQuads[counter] = Quad(packPosition(v0,v1,v2,v3), packColor(color, 255, 1));\n" +
        "            }\n" +
        "        }\n" +
        "        // +Y face\n" +
        "        if (y == SECTION_SIZE - 1 || VoxelBlocks[sectionBase + (zi * SECTION_SIZE + y + 1) * SECTION_SIZE + x] == 0)\n" +
        "        {\n" +
        "            uint counter;\n" +
        "            InterlockedAdd(QuadCounter[0], 1, counter);\n" +
        "            if (counter < MAX_QUADS_PER_SECTION)\n" +
        "            {\n" +
        "                float3 v0 = float3(x, y + 1, zi);\n" +
        "                float3 v1 = float3(x + 1, y + 1, zi);\n" +
        "                float3 v2 = float3(x + 1, y + 1, zi + 1);\n" +
        "                float3 v3 = float3(x, y + 1, zi + 1);\n" +
        "                OutputQuads[counter] = Quad(packPosition(v0,v1,v2,v3), packColor(color, 255, 2));\n" +
        "            }\n" +
        "        }\n" +
        "        // -Y face\n" +
        "        if (y == 0 || VoxelBlocks[sectionBase + (zi * SECTION_SIZE + y - 1) * SECTION_SIZE + x] == 0)\n" +
        "        {\n" +
        "            uint counter;\n" +
        "            InterlockedAdd(QuadCounter[0], 1, counter);\n" +
        "            if (counter < MAX_QUADS_PER_SECTION)\n" +
        "            {\n" +
        "                float3 v0 = float3(x + 1, y, zi);\n" +
        "                float3 v1 = float3(x, y, zi);\n" +
        "                float3 v2 = float3(x, y, zi + 1);\n" +
        "                float3 v3 = float3(x + 1, y, zi + 1);\n" +
        "                OutputQuads[counter] = Quad(packPosition(v0,v1,v2,v3), packColor(color, 255, 3));\n" +
        "            }\n" +
        "        }\n" +
        "        // +Z face\n" +
        "        if (zi == SECTION_SIZE - 1 || VoxelBlocks[sectionBase + ((zi + 1) * SECTION_SIZE + y) * SECTION_SIZE + x] == 0)\n" +
        "        {\n" +
        "            uint counter;\n" +
        "            InterlockedAdd(QuadCounter[0], 1, counter);\n" +
        "            if (counter < MAX_QUADS_PER_SECTION)\n" +
        "            {\n" +
        "                float3 v0 = float3(x, y, zi + 1);\n" +
        "                float3 v1 = float3(x + 1, y, zi + 1);\n" +
        "                float3 v2 = float3(x + 1, y + 1, zi + 1);\n" +
        "                float3 v3 = float3(x, y + 1, zi + 1);\n" +
        "                OutputQuads[counter] = Quad(packPosition(v0,v1,v2,v3), packColor(color, 255, 4));\n" +
        "            }\n" +
        "        }\n" +
        "        // -Z face\n" +
        "        if (zi == 0 || VoxelBlocks[sectionBase + ((zi - 1) * SECTION_SIZE + y) * SECTION_SIZE + x] == 0)\n" +
        "        {\n" +
        "            uint counter;\n" +
        "            InterlockedAdd(QuadCounter[0], 1, counter);\n" +
        "            if (counter < MAX_QUADS_PER_SECTION)\n" +
        "            {\n" +
        "                float3 v0 = float3(x + 1, y, zi);\n" +
        "                float3 v1 = float3(x, y, zi);\n" +
        "                float3 v2 = float3(x, y + 1, zi);\n" +
        "                float3 v3 = float3(x + 1, y + 1, zi);\n" +
        "                OutputQuads[counter] = Quad(packPosition(v0,v1,v2,v3), packColor(color, 255, 5));\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    // ============================================================
    // SHADER 2: HIZ GENERATION (Depth Pyramid)
    // ============================================================
    // Input: Depth texture (R32_FLOAT)
    // Output: HiZ texture (mipmapped R32_FLOAT, max reduction for reverse-Z)
    public static final String HIZ_GEN_COMPUTE = SCENE_UNIFORM + BUILD_CONSTANTS + //
        "Texture2D<float> DepthTex : register(t0);\n" +
        "RWTexture2D<float> HiZTex : register(u0);\n" +
        "\n" +
        "[numthreads(16, 16, 1)]\n" +
        "void main(uint2 dtid : SV_DispatchThreadID, uint mip : SV_GroupIndex)\n" +
        "{\n" +
        "    // This shader is dispatched once per mip level\n" +
        "    uint2 srcSize = DepthTex.GetDimensions();\n" +
        "    uint2 dstSize = HiZTex.GetDimensions(mip);\n" +
        "\n" +
        "    if (dtid.x >= dstSize.x || dtid.y >= dstSize.y) return;\n" +
        "\n" +
        "    uint2 srcPos = dtid * 2;\n" +
        "    float d0 = DepthTex.Load(int3(srcPos, mip - 1));\n" +
        "    float d1 = DepthTex.Load(int3(srcPos + uint2(1, 0), mip - 1));\n" +
        "    float d2 = DepthTex.Load(int3(srcPos + uint2(0, 1), mip - 1));\n" +
        "    float d3 = DepthTex.Load(int3(srcPos + uint2(1, 1), mip - 1));\n" +
        "\n" +
        "    // Reverse-Z: smaller depth = farther, so we want MAX for HiZ\n" +
        "    float maxDepth = max(max(d0, d1), max(d2, d3));\n" +
        "    HiZTex[dtid] = maxDepth;\n" +
        "}\n";

    // ============================================================
    // SHADER 3: HIERARCHICAL TRAVERSAL + FRUSTUM/OCCLUSION CULLING
    // ============================================================
    // Input: Node buffer, HiZ texture, frustum planes
    // Output: Visible node list + indirect draw args
    // Dispatch: Persistent threads, processes node queue
    public static final String TRAVERSAL_COMPUTE = SCENE_UNIFORM + NODE_DATA_STRUCT + BUILD_CONSTANTS + SHARED_UTILITIES + //
        "StructuredBuffer<NodeData> NodeBuffer : register(t0);\n" +
        "Texture2D<float> HiZTex : register(t1);\n" +
        "RWStructuredBuffer<uint> RequestQueue : register(u0);  // [count, nodeIds...]\n" +
        "RWStructuredBuffer<uint> RenderQueue : register(u1);   // [count, nodeIds...]\n" +
        "RWStructuredBuffer<uint> QueueMeta : register(u2);     // Per-iteration metadata\n" +
        "RWStructuredBuffer<uint> IndirectArgs : register(u3);  // [indexCount, instanceCount, startIndex, baseVertex, startInstance]\n" +
        "RWStructuredBuffer<uint> StatsBuffer : register(u4);\n" +
        "\n" +
        "#define MAX_ITERATIONS 8\n" +
        "#define LOCAL_SIZE 32\n" +
        "\n" +
        "groupshared uint localQueue[LOCAL_SIZE];\n" +
        "groupshared uint localCount;\n" +
        "\n" +
        "[numthreads(LOCAL_SIZE, 1, 1)]\n" +
        "void main(uint3 dtid : SV_DispatchThreadID, uint3 gtid : SV_GroupThreadID)\n" +
        "{\n" +
        "    if (gtid.x == 0) localCount = 0;\n" +
        "    GroupMemoryBarrierWithGroupSync();\n" +
        "\n" +
        "    // Load request queue\n" +
        "    uint requestCount = RequestQueue[0];\n" +
        "    \n" +
        "    // Process nodes in persistent thread fashion\n" +
        "    for (uint iter = 0; iter < MAX_ITERATIONS; iter++)\n" +
        "    {\n" +
        "        // Each thread grabs a node from the queue\n" +
        "        uint nodeIdx = 0;\n" +
        "        InterlockedAdd(RequestQueue[0], 0xFFFFFFFF, nodeIdx); // Atomic decrement\n" +
        "        nodeIdx = RequestQueue[0] - nodeIdx - 1;\n" +
        "\n" +
        "        if (nodeIdx >= requestCount) continue;\n" +
        "\n" +
        "        uint nodeId = RequestQueue[nodeIdx + 1];\n" +
        "        uint level = nodeId >> 20;\n" +
        "        uint index = nodeId & 0xFFFFF;\n" +
        "\n" +
        "        NodeData node = NodeBuffer[level * 1048576 + index]; // 1M nodes per level\n" +
        "\n" +
        "        // Frustum cull\n" +
        "        if (cullAABB(node.aabbMin, node.aabbMax))\n" +
        "            continue;\n" +
        "\n" +
        "        // HiZ occlusion test (simplified)\n" +
        "        // Project AABB to screen space, test against HiZ\n" +
        "        bool occluded = false;\n" +
        "        // ... HiZ test here ...\n" +
        "\n" +
        "        if (occluded)\n" +
        "            continue;\n" +
        "\n" +
        "        // Node is visible - add to render queue\n" +
        "        uint renderIdx;\n" +
        "        InterlockedAdd(RenderQueue[0], 1, renderIdx);\n" +
        "        RenderQueue[renderIdx + 1] = nodeId;\n" +
        "\n" +
        "        // If leaf node, add its quads to indirect draw\n" +
        "        if (level == 0 && node.quadCount > 0)\n" +
        "        {\n" +
        "            InterlockedAdd(IndirectArgs[0], node.quadCount * 6); // 6 indices per quad\n" +
        "        }\n" +
        "        // If not leaf, add children to request queue for next iteration\n" +
        "        else if (level > 0)\n" +
        "        {\n" +
        "            // Children would be added here\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    // ============================================================
    // SHADER 4: INDIRECT ARG GENERATION (Finalize Draw Calls)
    // ============================================================
    public static final String ARGS_GEN_COMPUTE = SCENE_UNIFORM + NODE_DATA_STRUCT + BUILD_CONSTANTS + //
        "StructuredBuffer<NodeData> NodeBuffer : register(t0);\n" +
        "RWStructuredBuffer<uint> RenderQueue : register(u0);\n" +
        "RWStructuredBuffer<uint> IndirectArgs : register(u1);\n" +
        "\n" +
        "[numthreads(1, 1, 1)]\n" +
        "void main()\n" +
        "{\n" +
        "    uint renderCount = RenderQueue[0];\n" +
        "    uint totalQuads = 0;\n" +
        "    \n" +
        "    for (uint i = 0; i < renderCount; i++)\n" +
        "    {\n" +
        "        uint nodeId = RenderQueue[i + 1];\n" +
        "        uint level = nodeId >> 20;\n" +
        "        uint index = nodeId & 0xFFFFF;\n" +
        "        NodeData node = NodeBuffer[level * 1048576 + index];\n" +
        "        totalQuads += node.quadCount;\n" +
        "    }\n" +
        "\n" +
        "    IndirectArgs[0] = totalQuads * 6;  // indexCount\n" +
        "    IndirectArgs[1] = 1;               // instanceCount\n" +
        "    IndirectArgs[2] = 0;               // startIndex\n" +
        "    IndirectArgs[3] = 0;               // baseVertex\n" +
        "    IndirectArgs[4] = 0;               // startInstance\n" +
        "}\n";

    // ============================================================
    // VERTEX SHADER (for rendering quads)
    // ============================================================
    public static final String VERTEX_SHADER = SCENE_UNIFORM + //
        "struct VSInput\n" +
        "{\n" +
        "    uint quadIndex : SV_VertexID;\n" +
        "    uint instanceId : SV_InstanceID;\n" +
        "};\n" +
        "\n" +
        "StructuredBuffer<Quad> QuadBuffer : register(t0);\n" +
        "StructuredBuffer<uint> IndexBuffer : register(t1);\n" +
        "\n" +
        "struct VSOutput\n" +
        "{\n" +
        "    float4 position : SV_Position;\n" +
        "    float4 color : COLOR;\n" +
        "    float3 worldPos : TEXCOORD0;\n" +
        "    uint faceId : TEXCOORD1;\n" +
        "};\n" +
        "\n" +
        "VSOutput main(VSInput input)\n" +
        "{\n" +
        "    VSOutput output;\n" +
        "    Quad quad = QuadBuffer[input.quadIndex];\n" +
        "    \n" +
        "    // Unpack position (simplified - in practice use proper unpacking)\n" +
        "    float3 worldPos = float3(\n" +
        "        (quad.positionData & 0xFFFFF) - 524288.0,\n" +
        "        ((quad.positionData >> 20) & 0xFFFFF) - 524288.0,\n" +
        "        ((quad.positionData >> 40) & 0x3FF) - 524288.0\n" +
        "    );\n" +
        "    \n" +
        "    float3 rel = worldPos - CameraPos.xyz;\n" +
        "    output.position = mul(ViewProj, float4(rel, 1.0));\n" +
        "    \n" +
        "    uint rgb = quad.colorData & 0xFFFFFF;\n" +
        "    uint alpha = (quad.colorData >> 24) & 0xFF;\n" +
        "    output.color = float4(\n" +
        "        ((rgb >> 16) & 0xFF) / 255.0,\n" +
        "        ((rgb >> 8) & 0xFF) / 255.0,\n" +
        "        (rgb & 0xFF) / 255.0,\n" +
        "        alpha / 255.0\n" +
        "    );\n" +
        "    output.worldPos = rel;\n" +
        "    output.faceId = (quad.colorData >> 28) & 0xF;\n" +
        "    return output;\n" +
        "}\n";

    // ============================================================
    // PIXEL SHADER
    // ============================================================
    public static final String PIXEL_SHADER = SCENE_UNIFORM + //
        "struct VSOutput\n" +
        "{\n" +
        "    float4 position : SV_Position;\n" +
        "    float4 color : COLOR;\n" +
        "    float3 worldPos : TEXCOORD0;\n" +
        "    uint faceId : TEXCOORD1;\n" +
        "};\n" +
        "\n" +
        "float4 main(VSOutput input) : SV_TARGET\n" +
        "{\n" +
        "    float3 rel = input.worldPos;\n" +
        "    float dist = length(rel);\n" +
        "    \n" +
        "    // Fog\n" +
        "    float fog = saturate((dist - CullParams.x) / max(CullParams.y - CullParams.x, 1.0));\n" +
        "    \n" +
        "    // Face shading (simple directional)\n" +
        "    float3 normal;\n" +
        "    switch (input.faceId)\n" +
        "    {\n" +
        "        case 0: normal = float3(1, 0, 0); break;\n" +
        "        case 1: normal = float3(-1, 0, 0); break;\n" +
        "        case 2: normal = float3(0, 1, 0); break;\n" +
        "        case 3: normal = float3(0, -1, 0); break;\n" +
        "        case 4: normal = float3(0, 0, 1); break;\n" +
        "        case 5: normal = float3(0, 0, -1); break;\n" +
        "        default: normal = float3(0, 1, 0);\n" +
        "    }\n" +
        "    float shade = saturate(dot(normal, normalize(float3(0.42, 0.80, -0.42))) * 0.5 + 0.5);\n" +
        "    \n" +
        "    float3 rgb = input.color.rgb * (0.7 + 0.3 * shade);\n" +
        "    \n" +
        "    // Fog blend\n" +
        "    float3 skyColor = float3(0.5, 0.6, 0.75);\n" +
        "    rgb = lerp(rgb, skyColor, fog);\n" +
        "    \n" +
        "    return float4(rgb, input.color.a);\n" +
        "}\n";

    // ============================================================
    // GETTER METHODS FOR SHADER SOURCES
    // ============================================================

    public static String getGeometryGenShader() {
        return GEOMETRY_GEN_COMPUTE;
    }

    public static String getHiZGenShader() {
        return HIZ_GEN_COMPUTE;
    }

    public static String getTraversalShader() {
        return TRAVERSAL_COMPUTE;
    }

    public static String getArgsGenShader() {
        return ARGS_GEN_COMPUTE;
    }

    public static String getVertexShader() {
        return VERTEX_SHADER;
    }

    public static String getPixelShader() {
        return PIXEL_SHADER;
    }
}