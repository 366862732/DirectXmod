package com.xgdt.dx12.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 需要做与Voxy差不多的网格化，3D 体素 LOD 的核心。
 * 
 * 把一块区块的完整方块数据网格化成「只有暴露面」的三角面片。
 *
 * <p>这是 3D 体素 LOD 的核心：不再像高度场那样「一列一个高度、整列涂一种颜色」，
 * 而是逐格判断六个方向有没有邻居，只画露在外面的面。于是房屋有屋顶和墙、树有树干和
 * 树冠、悬崖有真实的竖直岩壁，而不会塌成一块实心色块。
 *
 * <p><b>贪心合并</b>：同一层、同一方向、同一种方块（名字 + 颜色 + 水体标记都相同）
 * 的相邻面会合并成一个矩形 quad。Voxy 的远景之所以能铺得远，靠的就是这一步：
 * 一整片 16×16 的草地顶面只发 1 个 quad，而不是 256 个。合并只改变顶点数量，
 * 不改变几何形状；UV 按方块空间 0..格数 写入，像素着色器 {@code frac} 后仍然是
 * 每方块一张贴图。
 *
 * <p><b>降采样</b>：{@code shift > 0} 时把 {@code 2^shift} 见方的一团方块压成一格，
 * 只要团里有实心方块这一格就算实心，颜色取团里最上面那块。用于远处保持面数可控。
 *
 * <p><b>区块边界</b>：邻居在区块外时一律当成空气。相邻两块实心方块之间的面本来就埋在
 * 实体内部看不见，多画一层不影响观感，却省掉了跨区块取邻居的复杂度。
 *
 * <p><b>纹理</b>：顶点带「方块空间 UV」+「图集矩形」。UV 按方块重复（一格是
 * {@code 2^shift} 个方块宽，UV 就是 0..{@code 2^shift}），像素着色器里 {@code frac}
 * 回落到单个方块，再乘图集矩形——和 voxy 一样，32 格宽的大面不会把一张 16px 贴图
 * 拉成一块纯色。矩形取自 {@code LodRegionCache} 的每面 sprite（草地侧面/原木年轮各自
 * 正确）；查不到时矩形为 0，着色器退回顶点色。
 *
 * <p>顶点格式：{@code float3 position + RGBA8 color + float2 uv + R16G16B16A16 rect}，
 * 32 字节。
 * 每个面 4 个顶点，索引由渲染端按 {@code 0,1,2, 0,2,3} 静态生成。
 */
public final class LodVoxelMesher {

    /** 每顶点字节数：float3(12) + RGBA8(4) + float2(8) + 4×UNORM16(8) = 32。 */
    public static final int VERTEX_STRIDE = 32;
    /**
     * 地下剪枝的深度余量（格）。一格低于四邻列最高实心格超过这么多，就认为它被
     * 地表包住、永远看不见。留一点余量是为了不误伤贴着地表的结构（房屋地板、洞口）；
     * 远景里 16 格以下的洞穴入口已经小于一个像素，多剪掉一层能显著减少面数。
     */
    private static final int PRUNE_DEPTH = 16;

    private LodVoxelMesher() {
    }

    /** 建网结果：顶点字节（4 顶点 × 32B / 面）与面数。 */
    public record Mesh(byte[] vertices, int faces) {
        public static final Mesh EMPTY = new Mesh(new byte[0], 0);
        /** 区块数据不可用（未生成 / 未保存 / 读盘失败）。和「空气区块」不是一回事。 */
        public static final Mesh UNAVAILABLE = new Mesh(new byte[0], -1);
    }

    /**
     * 网格化一块区块（无纹理，方块内 0..1 UV，兼容旧测试）。
     */
    public static Mesh mesh(LodRegionCache.VoxelChunk chunk, int chunkX, int chunkZ,
                            int centerX, int centerZ, int shift) {
        return mesh(chunk, chunkX, chunkZ, centerX, centerZ, shift, null);
    }

    /**
     * 网格化一块区块，UV 映射到图集坐标。
     *
     * @param cache 提供方块名→图集 UV 矩形查询；{@code null} 时退化为方块内 0..1 UV
     */
    public static Mesh mesh(LodRegionCache.VoxelChunk chunk, int chunkX, int chunkZ,
                            int centerX, int centerZ, int shift, LodRegionCache cache) {
        int size = 1 << shift;
        int cells = 16 >> shift;
        int cellsY = chunk.height() >> shift;
        int baseX = (chunkX << 4) - centerX;
        int baseZ = (chunkZ << 4) - centerZ;
        int minY = chunk.minY();
        int topCell = Math.min(cellsY - 1, (chunk.topY() - minY) >> shift);

        // 每列最高实心格（区块内坐标）。地下洞穴里那些「六面都朝空气、但从地表永远
        // 看不到」的面是面数的大头：一个区块动辄上千片。若一格的高度低于四邻列的最高
        // 实心格，说明它被四周的实心地形包住（悬崖面也满足这一点：更低那一侧的地表
        // 就在它上方），四个侧面必然被邻居遮住，顶/底面也埋在地表之下，直接跳过。
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

        // 1) 每格只取一次代表方块。key 高 32 位 = 方块名编号，低 32 位 = RGB
        //    （含水体标记）；-1 = 空气。名字编号用来保证「同颜色不同方块」不会被
        //    贪心合并到一起（否则石头和圆石可能共用一张贴图）。
        long[] grid = new long[cells * cellsY * cells];
        Arrays.fill(grid, -1L);
        Map<String, Integer> nameIds = new HashMap<>();
        List<String> idNames = new ArrayList<>();
        for (int cy = 0; cy <= topCell; cy++) {
            int y0 = minY + cy * size;
            for (int cz = 0; cz < cells; cz++) {
                int z0 = cz * size;
                for (int cx = 0; cx < cells; cx++) {
                    int x0 = cx * size;
                    int rgb = -1;
                    String name = null;
                    // 和旧版 sample()/topBlockName() 一致：取这一团里最上面的实心方块。
                    // 降采样时用最上面那块，地形表面才不会因为取到地下泥土而整片刷成棕色。
                    outer:
                    for (int y = y0 + size - 1; y >= y0; y--) {
                        for (int z = z0; z < z0 + size; z++) {
                            for (int x = x0; x < x0 + size; x++) {
                                int c = chunk.color(x, y, z);
                                if (c != -1) {
                                    rgb = c;
                                    name = chunk.blockName(x, y, z);
                                    break outer;
                                }
                            }
                        }
                    }
                    if (rgb < 0) {
                        continue;
                    }
                    int nameId = 0;
                    if (name != null) {
                        Integer id = nameIds.get(name);
                        if (id == null) {
                            id = idNames.size() + 1;
                            nameIds.put(name, id);
                            idNames.add(name);
                        }
                        nameId = id;
                    }
                    grid[(cy * cells + cz) * cells + cx] =
                        ((long) nameId << 32) | (rgb & 0xFFFFFFFFL);
                }
            }
        }

        // 2) 标记被四邻列地表包住的地下格。邻居是否实心仍然按「团里有没有实心方块」
        //    判断（和旧版一致），这样剪枝不会把原本被遮住的面重新露出来。
        boolean[] pruned = new boolean[cells * cellsY * cells];
        for (int cy = 0; cy <= topCell; cy++) {
            for (int cz = 0; cz < cells; cz++) {
                for (int cx = 0; cx < cells; cx++) {
                    int idx = (cy * cells + cz) * cells + cx;
                    if (grid[idx] == -1L) {
                        continue;
                    }
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
                        pruned[idx] = true;
                    }
                }
            }
        }

        Emitter out = new Emitter();
        long[] mask = new long[cells * cellsY];
        // 六个方向分别做一次「切片 → 掩码 → 贪心矩形合并」。方向编号与
        // Direction.ordinal() 一致：0=DOWN 1=UP 2=NORTH 3=SOUTH 4=WEST 5=EAST。
        for (int dir = 0; dir < 6; dir++) {
            int slices;
            int w;
            int h;
            switch (dir) {
                case 0, 1 -> {
                    // 水平切片：i = x，j = z
                    slices = cellsY;
                    w = cells;
                    h = cells;
                }
                case 2, 3 -> {
                    // Z 方向切片：i = x，j = y
                    slices = cells;
                    w = cells;
                    h = cellsY;
                }
                default -> {
                    // X 方向切片：i = z，j = y
                    slices = cells;
                    w = cells;
                    h = cellsY;
                }
            }
            for (int s = 0; s < slices; s++) {
                Arrays.fill(mask, 0, w * h, -1L);
                // 3) 生成这一层的可见面掩码
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        int cx;
                        int cy;
                        int cz;
                        switch (dir) {
                            case 0, 1 -> {
                                cx = i;
                                cy = s;
                                cz = j;
                            }
                            case 2, 3 -> {
                                cx = i;
                                cy = j;
                                cz = s;
                            }
                            default -> {
                                cx = s;
                                cy = j;
                                cz = i;
                            }
                        }
                        int idx = (cy * cells + cz) * cells + cx;
                        long key = grid[idx];
                        if (key == -1L || pruned[idx]) {
                            continue;
                        }
                        int nx = cx;
                        int ny = cy;
                        int nz = cz;
                        switch (dir) {
                            case 0 -> ny--;
                            case 1 -> ny++;
                            case 2 -> nz--;
                            case 3 -> nz++;
                            case 4 -> nx--;
                            default -> nx++;
                        }
                        if (solidAt(grid, nx, ny, nz, cells, cellsY)) {
                            continue;
                        }
                        mask[j * w + i] = key;
                    }
                }
                // 4) 贪心合并：先向右找同 key 的连续宽度，再整行向下扩展高度，
                //    最后把矩形覆盖的掩码清空。每个矩形只发 4 个顶点。
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        long key = mask[j * w + i];
                        if (key == -1L) {
                            continue;
                        }
                        int width = 1;
                        while (i + width < w && mask[j * w + i + width] == key) {
                            width++;
                        }
                        int height = 1;
                        scan:
                        while (j + height < h) {
                            int row = (j + height) * w;
                            for (int k = 0; k < width; k++) {
                                if (mask[row + i + k] != key) {
                                    break scan;
                                }
                            }
                            height++;
                        }
                        for (int jj = 0; jj < height; jj++) {
                            int row = (j + jj) * w;
                            for (int k = 0; k < width; k++) {
                                mask[row + i + k] = -1L;
                            }
                        }

                        int cx;
                        int cy;
                        int cz;
                        switch (dir) {
                            case 0, 1 -> {
                                cx = i;
                                cy = s;
                                cz = j;
                            }
                            case 2, 3 -> {
                                cx = i;
                                cy = j;
                                cz = s;
                            }
                            default -> {
                                cx = s;
                                cy = j;
                                cz = i;
                            }
                        }
                        int idx = (cy * cells + cz) * cells + cx;
                        long cellKey = grid[idx];
                        int rgb = (int) cellKey;
                        int nameId = (int) (cellKey >>> 32);
                        String name = nameId == 0 ? null : idNames.get(nameId - 1);
                        float[][] rects = cache == null ? null : cache.getSpriteUv(name);
                        int[] tints = cache == null ? null : cache.getSpriteTint(name);
                        float[] rect = pickRect(rects, dir);
                        int color = faceColor(rect, tints, dir, rgb);
                        int alpha = LodRegionCache.isWater(rgb) ? 0 : 255;
                        float u = width * (float) size;
                        float v = height * (float) size;
                        switch (dir) {
                            case 0 -> { // DOWN
                                float x0 = baseX + i * (float) size;
                                float z0 = baseZ + j * (float) size;
                                float y = minY + s * (float) size;
                                emitFace(out, dir, x0, y, z0, x0 + u, y, z0 + v,
                                    u, v, color, alpha, rect);
                            }
                            case 1 -> { // UP
                                float x0 = baseX + i * (float) size;
                                float z0 = baseZ + j * (float) size;
                                float y = minY + (s + 1) * (float) size;
                                emitFace(out, dir, x0, y, z0, x0 + u, y, z0 + v,
                                    u, v, color, alpha, rect);
                            }
                            case 2 -> { // NORTH
                                float x0 = baseX + i * (float) size;
                                float y0 = minY + j * (float) size;
                                float z = baseZ + s * (float) size;
                                emitFace(out, dir, x0, y0, z, x0 + u, y0 + v, z,
                                    u, v, color, alpha, rect);
                            }
                            case 3 -> { // SOUTH
                                float x0 = baseX + i * (float) size;
                                float y0 = minY + j * (float) size;
                                float z = baseZ + (s + 1) * (float) size;
                                emitFace(out, dir, x0, y0, z, x0 + u, y0 + v, z,
                                    u, v, color, alpha, rect);
                            }
                            case 4 -> { // WEST
                                float z0 = baseZ + i * (float) size;
                                float y0 = minY + j * (float) size;
                                float x = baseX + s * (float) size;
                                emitFace(out, dir, x, y0, z0, x, y0 + v, z0 + u,
                                    u, v, color, alpha, rect);
                            }
                            default -> { // EAST
                                float z0 = baseZ + i * (float) size;
                                float y0 = minY + j * (float) size;
                                float x = baseX + (s + 1) * (float) size;
                                emitFace(out, dir, x, y0, z0, x, y0 + v, z0 + u,
                                    u, v, color, alpha, rect);
                            }
                        }
                    }
                }
            }
        }
        return out.faces == 0 ? Mesh.EMPTY
            : new Mesh(Arrays.copyOf(out.out.array(), out.vertex * VERTEX_STRIDE), out.faces);
    }

    /** 顶点缓冲 + 计数器。贪心合并后每个矩形 4 个顶点，索引由渲染端静态生成。 */
    private static final class Emitter {
        ByteBuffer out;
        int vertex;
        int faces;

        Emitter() {
            this.out = ByteBuffer.allocate(4096 * VERTEX_STRIDE).order(ByteOrder.LITTLE_ENDIAN);
        }

        void ensure(int vertices) {
            while ((this.vertex + vertices) * VERTEX_STRIDE > this.out.capacity()) {
                this.out = grow(this.out, this.out.capacity() * 2, this.vertex * VERTEX_STRIDE);
            }
        }
    }

    /** 邻居格是否实心；越界当空气（与旧版区块边界行为一致）。 */
    private static boolean solidAt(long[] grid, int x, int y, int z, int cells, int cellsY) {
        if (x < 0 || y < 0 || z < 0 || x >= cells || y >= cellsY || z >= cells) {
            return false;
        }
        return grid[(y * cells + z) * cells + x] != -1L;
    }

    /**
     * 发一个矩形面。{@code (x0,y0,z0)..(x1,y1,z1)} 是矩形包围盒，其中法线方向的两个
     * 端点相等；{@code u/v} 是方块空间 UV 的跨度（格数 × 2^shift）。
     *
     * <p>顶点顺序沿用旧版逐格发面的顺序，只是把 size 换成了合并后的 u/v，保证
     * 三角形绕序、UV 朝向、图集矩形都和以前完全一致。
     */
    private static void emitFace(Emitter e, int dir,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float u, float v, int color, int alpha, float[] rect) {
        e.ensure(4);
        int o = e.vertex;
        switch (dir) {
            case 0 -> { // DOWN
                putAtlas(e.out, o, x0, y0, z0, color, alpha, 0, 0, rect);
                putAtlas(e.out, o + 1, x1, y0, z0, color, alpha, u, 0, rect);
                putAtlas(e.out, o + 2, x1, y0, z1, color, alpha, u, v, rect);
                putAtlas(e.out, o + 3, x0, y0, z1, color, alpha, 0, v, rect);
            }
            case 1 -> { // UP
                putAtlas(e.out, o, x0, y1, z0, color, alpha, 0, 0, rect);
                putAtlas(e.out, o + 1, x0, y1, z1, color, alpha, 0, v, rect);
                putAtlas(e.out, o + 2, x1, y1, z1, color, alpha, u, v, rect);
                putAtlas(e.out, o + 3, x1, y1, z0, color, alpha, u, 0, rect);
            }
            case 2 -> { // NORTH
                putAtlas(e.out, o, x0, y0, z0, color, alpha, 0, 0, rect);
                putAtlas(e.out, o + 1, x1, y0, z0, color, alpha, u, 0, rect);
                putAtlas(e.out, o + 2, x1, y1, z0, color, alpha, u, v, rect);
                putAtlas(e.out, o + 3, x0, y1, z0, color, alpha, 0, v, rect);
            }
            case 3 -> { // SOUTH
                putAtlas(e.out, o, x0, y0, z1, color, alpha, 0, 0, rect);
                putAtlas(e.out, o + 1, x0, y1, z1, color, alpha, 0, v, rect);
                putAtlas(e.out, o + 2, x1, y1, z1, color, alpha, u, v, rect);
                putAtlas(e.out, o + 3, x1, y0, z1, color, alpha, u, 0, rect);
            }
            case 4 -> { // WEST
                putAtlas(e.out, o, x0, y0, z0, color, alpha, 0, 0, rect);
                putAtlas(e.out, o + 1, x0, y1, z0, color, alpha, 0, v, rect);
                putAtlas(e.out, o + 2, x0, y1, z1, color, alpha, u, v, rect);
                putAtlas(e.out, o + 3, x0, y0, z1, color, alpha, u, 0, rect);
            }
            default -> { // EAST
                putAtlas(e.out, o, x1, y0, z0, color, alpha, 0, 0, rect);
                putAtlas(e.out, o + 1, x1, y0, z1, color, alpha, 0, v, rect);
                putAtlas(e.out, o + 2, x1, y1, z1, color, alpha, u, v, rect);
                putAtlas(e.out, o + 3, x1, y1, z0, color, alpha, u, 0, rect);
            }
        }
        e.vertex += 4;
        e.faces++;
    }

    // ------------------------------------------------------------------
    // UV 映射工具
    // ------------------------------------------------------------------

    /** 取某方向（{@code Direction.ordinal()}）的图集 UV 矩形；缺失返回 {@code null}。 */
    private static float[] pickRect(float[][] rects, int ordinal) {
        if (rects == null || ordinal < 0 || ordinal >= rects.length) {
            return null;
        }
        return rects[ordinal];
    }

    /**
     * 一个面的顶点色：有图集矩形时写「生物群系染色值」（白 = 不染色，着色器乘 1），
     * 没有贴图时退回方块自己的 MapColor 兜底。
     */
    private static int faceColor(float[] rect, int[] tints, int ordinal, int base) {
        if (rect == null) {
            return base;
        }
        if (tints == null || ordinal < 0 || ordinal >= tints.length) {
            return 0xFFFFFF;
        }
        return tints[ordinal];
    }

    /** 把 0..1 的图集 UV 写成 UNORM16（缺失视为 0，着色器据此退回顶点色）。 */
    private static int unorm16(float[] rect, int index) {
        if (rect == null) {
            return 0;
        }
        float value = Math.max(0f, Math.min(1f, rect[index]));
        return Math.round(value * 65535f);
    }

    private static ByteBuffer grow(ByteBuffer buffer, int capacity, int used) {
        ByteBuffer bigger = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        System.arraycopy(buffer.array(), 0, bigger.array(), 0, used);
        return bigger;
    }

    /**
     * 写一个 32B 顶点：position + color + 块空间 uv + 图集矩形（4×UNORM16）。
     *
     * <p>uv 是「方块空间」坐标（一格 1.0，整面 {@code 0..size}），像素着色器
     * {@code frac} 之后再用矩形还原到图集里那一格。
     */
    private static void putAtlas(ByteBuffer buffer, int vertex, float x, float y, float z,
                                 int rgb, int alpha, float u, float v, float[] rect) {
        int offset = vertex * VERTEX_STRIDE;
        buffer.putFloat(offset, x);
        buffer.putFloat(offset + 4, y);
        buffer.putFloat(offset + 8, z);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        buffer.putInt(offset + 12, r | (g << 8) | (b << 16) | ((alpha & 0xFF) << 24));
        buffer.putFloat(offset + 16, u);
        buffer.putFloat(offset + 20, v);
        buffer.putShort(offset + 24, (short) unorm16(rect, 0));
        buffer.putShort(offset + 26, (short) unorm16(rect, 1));
        buffer.putShort(offset + 28, (short) unorm16(rect, 2));
        buffer.putShort(offset + 30, (short) unorm16(rect, 3));
    }
}
