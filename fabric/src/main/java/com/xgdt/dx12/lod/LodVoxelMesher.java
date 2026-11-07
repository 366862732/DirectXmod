package com.xgdt.dx12.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 把一块区块的完整方块数据网格化成「只有暴露面」的三角面片。
 *
 * <p>这是 3D 体素 LOD 的核心：不再像高度场那样「一列一个高度、整列涂一种颜色」，
 * 而是逐格判断六个方向有没有邻居，只画露在外面的面。于是房屋有屋顶和墙、树有树干和
 * 树冠、悬崖有真实的竖直岩壁，而不会塌成一块实心色块。
 *
 * <p><b>降采样</b>：{@code shift > 0} 时把 {@code 2^shift} 见方的一团方块压成一格，
 * 只要团里有实心方块这一格就算实心，颜色取团里最上面那块。用于远处保持面数可控。
 *
 * <p><b>区块边界</b>：邻居在区块外时一律当成空气。相邻两块实心方块之间的面本来就埋在
 * 实体内部看不见，多画一层不影响观感，却省掉了跨区块取邻居的复杂度。
 *
 * <p>顶点格式与高度场完全一致：{@code float3 position + RGBA8 color}，16 字节。
 * 每个面 4 个顶点，索引由渲染端按 {@code 0,1,2, 0,2,3} 静态生成。
 */
public final class LodVoxelMesher {

    /** 每顶点字节数：float3 + RGBA8。 */
    public static final int VERTEX_STRIDE = 16;
    /**
     * 地下剪枝的深度余量（格）。一格低于四邻列最高实心格超过这么多，就认为它被
     * 地表包住、永远看不见。留一点余量是为了不误伤贴着地表的结构（房屋地板、洞口）。
     */
    private static final int PRUNE_DEPTH = 8;

    private LodVoxelMesher() {
    }

    /** 建网结果：顶点字节（4 顶点 × 16B / 面）与面数。 */
    public record Mesh(byte[] vertices, int faces) {
        public static final Mesh EMPTY = new Mesh(new byte[0], 0);
        /** 区块数据不可用（未生成 / 未保存 / 读盘失败）。和「空气区块」不是一回事。 */
        public static final Mesh UNAVAILABLE = new Mesh(new byte[0], -1);
    }

    /**
     * 网格化一块区块。
     *
     * @param chunk    区块方块数据
     * @param chunkX   区块 X 坐标
     * @param chunkZ   区块 Z 坐标
     * @param centerX  顶点坐标的参考中心 X（顶点写成相对坐标，避免 float 抖动）
     * @param centerZ  顶点坐标的参考中心 Z
     * @param shift    降采样级别，格子边长 = {@code 1 << shift} 方块
     */
    public static Mesh mesh(LodRegionCache.VoxelChunk chunk, int chunkX, int chunkZ,
                            int centerX, int centerZ, int shift) {
        int size = 1 << shift;
        int cells = 16 >> shift;
        int cellsY = chunk.height() >> shift;
        int baseX = (chunkX << 4) - centerX;
        int baseZ = (chunkZ << 4) - centerZ;
        int minY = chunk.minY();
        int topCell = Math.min(cellsY - 1, (chunk.topY() - minY) >> shift);

        ByteBuffer out = ByteBuffer.allocate(4096 * VERTEX_STRIDE).order(ByteOrder.LITTLE_ENDIAN);
        int vertex = 0;
        int faces = 0;

        // 每列最高实心格（区块内坐标）。地下洞穴里那些「六面都朝空气、但从地表永远
        // 看不到」的面是面数的大头：一个区块动辄上千片。若一格的高度低于四邻列的最高
        // 实心格，说明它被四周的实心地形包住（悬崖面也满足这一点：更低那一侧的地表
        // 就在它上方），四个侧面必然被邻居遮住，顶/底面也埋在地表之下，直接跳过。
        // 竖井、洞口那一列的最高实心格会很低，所以洞口不会被削掉。
        int[] cellTops = new int[cells * cells];
        Arrays.fill(cellTops, Integer.MIN_VALUE);
        for (int cz = 0; cz < cells; cz++) {
            for (int cx = 0; cx < cells; cx++) {
                int best = Integer.MIN_VALUE;
                for (int sz = 0; sz < size; sz++) {
                    for (int sx = 0; sx < size; sx++) {
                        int x = cx * size + sx;
                        int z = cz * size + sz;
                        for (int y = chunk.topY(); y >= minY; y--) {
                            if (chunk.color(x, y, z) != -1) {
                                best = Math.max(best, (y - minY) >> shift);
                                break;
                            }
                        }
                    }
                }
                cellTops[cz * cells + cx] = best;
            }
        }

        for (int cy = 0; cy <= topCell; cy++) {
            int y = minY + cy * size;
            for (int cz = 0; cz < cells; cz++) {
                int z = baseZ + cz * size;
                for (int cx = 0; cx < cells; cx++) {
                    int rgb = sample(chunk, cx, cy, cz, cells, cellsY, size);
                    if (rgb < 0) {
                        continue;
                    }
                    // 被四邻列的地表盖住的地下格：看不见，跳过
                    int minTop = Integer.MAX_VALUE;
                    if (cx > 0) {
                        int t = cellTops[cz * cells + cx - 1];
                        if (t != Integer.MIN_VALUE) {
                            minTop = Math.min(minTop, t);
                        }
                    }
                    if (cx + 1 < cells) {
                        int t = cellTops[cz * cells + cx + 1];
                        if (t != Integer.MIN_VALUE) {
                            minTop = Math.min(minTop, t);
                        }
                    }
                    if (cz > 0) {
                        int t = cellTops[(cz - 1) * cells + cx];
                        if (t != Integer.MIN_VALUE) {
                            minTop = Math.min(minTop, t);
                        }
                    }
                    if (cz + 1 < cells) {
                        int t = cellTops[(cz + 1) * cells + cx];
                        if (t != Integer.MIN_VALUE) {
                            minTop = Math.min(minTop, t);
                        }
                    }
                    if (minTop != Integer.MAX_VALUE && cy < minTop - PRUNE_DEPTH) {
                        continue;
                    }
                    int x = baseX + cx * size;
                    int alpha = LodRegionCache.isWater(rgb) ? 0 : 255;

                    // 每个方向都只在邻居不是实心时才出面
                    if (cy + 1 >= cellsY || sample(chunk, cx, cy + 1, cz, cells, cellsY, size) < 0) {
                        faces++;
                        if ((vertex + 4) * VERTEX_STRIDE > out.capacity()) {
                            out = grow(out, out.capacity() * 2, vertex * VERTEX_STRIDE);
                        }
                        // +Y 顶面
                        put(out, vertex, x, y + size, z, rgb, alpha);
                        put(out, vertex + 1, x, y + size, z + size, rgb, alpha);
                        put(out, vertex + 2, x + size, y + size, z + size, rgb, alpha);
                        put(out, vertex + 3, x + size, y + size, z, rgb, alpha);
                        vertex += 4;
                    }
                    if (cy > 0 && sample(chunk, cx, cy - 1, cz, cells, cellsY, size) < 0) {
                        faces++;
                        if ((vertex + 4) * VERTEX_STRIDE > out.capacity()) {
                            out = grow(out, out.capacity() * 2, vertex * VERTEX_STRIDE);
                        }
                        // -Y 底面
                        put(out, vertex, x, y, z, rgb, alpha);
                        put(out, vertex + 1, x + size, y, z, rgb, alpha);
                        put(out, vertex + 2, x + size, y, z + size, rgb, alpha);
                        put(out, vertex + 3, x, y, z + size, rgb, alpha);
                        vertex += 4;
                    }
                    if (cx + 1 >= cells || sample(chunk, cx + 1, cy, cz, cells, cellsY, size) < 0) {
                        faces++;
                        if ((vertex + 4) * VERTEX_STRIDE > out.capacity()) {
                            out = grow(out, out.capacity() * 2, vertex * VERTEX_STRIDE);
                        }
                        // +X 墙面
                        put(out, vertex, x + size, y, z, rgb, alpha);
                        put(out, vertex + 1, x + size, y, z + size, rgb, alpha);
                        put(out, vertex + 2, x + size, y + size, z + size, rgb, alpha);
                        put(out, vertex + 3, x + size, y + size, z, rgb, alpha);
                        vertex += 4;
                    }
                    if (cx == 0 || sample(chunk, cx - 1, cy, cz, cells, cellsY, size) < 0) {
                        faces++;
                        if ((vertex + 4) * VERTEX_STRIDE > out.capacity()) {
                            out = grow(out, out.capacity() * 2, vertex * VERTEX_STRIDE);
                        }
                        // -X 墙面
                        put(out, vertex, x, y, z, rgb, alpha);
                        put(out, vertex + 1, x, y + size, z, rgb, alpha);
                        put(out, vertex + 2, x, y + size, z + size, rgb, alpha);
                        put(out, vertex + 3, x, y, z + size, rgb, alpha);
                        vertex += 4;
                    }
                    if (cz + 1 >= cells || sample(chunk, cx, cy, cz + 1, cells, cellsY, size) < 0) {
                        faces++;
                        if ((vertex + 4) * VERTEX_STRIDE > out.capacity()) {
                            out = grow(out, out.capacity() * 2, vertex * VERTEX_STRIDE);
                        }
                        // +Z 墙面
                        put(out, vertex, x, y, z + size, rgb, alpha);
                        put(out, vertex + 1, x, y + size, z + size, rgb, alpha);
                        put(out, vertex + 2, x + size, y + size, z + size, rgb, alpha);
                        put(out, vertex + 3, x + size, y, z + size, rgb, alpha);
                        vertex += 4;
                    }
                    if (cz == 0 || sample(chunk, cx, cy, cz - 1, cells, cellsY, size) < 0) {
                        faces++;
                        if ((vertex + 4) * VERTEX_STRIDE > out.capacity()) {
                            out = grow(out, out.capacity() * 2, vertex * VERTEX_STRIDE);
                        }
                        // -Z 墙面
                        put(out, vertex, x, y, z, rgb, alpha);
                        put(out, vertex + 1, x + size, y, z, rgb, alpha);
                        put(out, vertex + 2, x + size, y + size, z, rgb, alpha);
                        put(out, vertex + 3, x, y + size, z, rgb, alpha);
                        vertex += 4;
                    }
                }
            }
        }
        return faces == 0 ? Mesh.EMPTY : new Mesh(Arrays.copyOf(out.array(), vertex * VERTEX_STRIDE), faces);
    }

    /**
     * 取一格的颜色：把 {@code 2^shift} 见方的一团方块里<b>最上面</b>的实心方块当代表，
     * 全空返回 {@code -1}。降采样时用最上面那块，地形表面才不会因为取到地下泥土而
     * 整片刷成棕色。
     */
    private static int sample(LodRegionCache.VoxelChunk chunk, int cx, int cy, int cz,
                              int cells, int cellsY, int size) {
        if (cx < 0 || cy < 0 || cz < 0 || cx >= cells || cy >= cellsY || cz >= cells) {
            return -1;
        }
        int x0 = cx * size;
        // VoxelChunk.color 的 y 是绝对世界坐标（内部再减 minY），这里必须补上 minY。
        // 主世界的 minY 是 -64，漏掉它会让整块地形往上错 64 格，取到的全是空气。
        int y0 = chunk.minY() + cy * size;
        int z0 = cz * size;
        for (int y = y0 + size - 1; y >= y0; y--) {
            for (int z = z0; z < z0 + size; z++) {
                for (int x = x0; x < x0 + size; x++) {
                    int rgb = chunk.color(x, y, z);
                    if (rgb != -1) {
                        return rgb;
                    }
                }
            }
        }
        return -1;
    }

    private static ByteBuffer grow(ByteBuffer buffer, int capacity, int used) {
        ByteBuffer bigger = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        System.arraycopy(buffer.array(), 0, bigger.array(), 0, used);
        return bigger;
    }

    private static void put(ByteBuffer buffer, int vertex, int x, int y, int z, int rgb, int alpha) {
        int offset = vertex * VERTEX_STRIDE;
        buffer.putFloat(offset, x);
        buffer.putFloat(offset + 4, y);
        buffer.putFloat(offset + 8, z);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        buffer.putInt(offset + 12, r | (g << 8) | (b << 16) | ((alpha & 0xFF) << 24));
    }
}
