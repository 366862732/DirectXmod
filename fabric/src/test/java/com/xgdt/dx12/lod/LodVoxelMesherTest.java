package com.xgdt.dx12.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 体素网格化器的离线测试。
 *
 * <p>核心要验证的是「3D」这件事本身：高度场只能把一列画成一个实心方块，而这里必须
 * 画出屋顶、屋顶下方的空气、以及地面。下面的「小房子」用例把这三层都摆出来，逐个
 * 平面数面片。
 */
class LodVoxelMesherTest {

    /** 主世界：minY = -64、总高 384。用真实的 minY 才能测出「绝对 y / 区块内 y」混用。 */
    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;

    /** 一个小方块集合 → VoxelChunk 的简易构造器。 */
    private static final class Builder {
        private final Map<Integer, Integer> cells = new HashMap<>();
        private int topY = MIN_Y;

        Builder put(int x, int y, int z, int rgb) {
            // blocks 的下标是「区块内 y」，不是绝对 y
            this.cells.put(((y - MIN_Y) * 16 + z) * 16 + x, rgb);
            this.topY = Math.max(this.topY, y);
            return this;
        }

        LodRegionCache.VoxelChunk build() {
            short[] blocks = new short[16 * 16 * HEIGHT];
            int[] palette = new int[this.cells.size() + 1];
            palette[0] = -1;
            int next = 1;
            Map<Integer, Integer> colorToIndex = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : this.cells.entrySet()) {
                Integer index = colorToIndex.get(entry.getValue());
                if (index == null) {
                    index = next;
                    palette[next++] = entry.getValue();
                    colorToIndex.put(entry.getValue(), index);
                }
                blocks[entry.getKey()] = (short) (int) index;
            }
            return new LodRegionCache.VoxelChunk(MIN_Y, HEIGHT, this.topY,
                java.util.Arrays.copyOf(palette, next), blocks);
        }
    }

    /** 面片在某个水平面上的数量：四个顶点的 y 都等于 {@code planeY}。 */
    private static int facesOnPlane(LodVoxelMesher.Mesh mesh, float planeY) {
        ByteBuffer buffer = ByteBuffer.wrap(mesh.vertices()).order(ByteOrder.LITTLE_ENDIAN);
        int count = 0;
        for (int face = 0; face < mesh.faces(); face++) {
            boolean flat = true;
            for (int corner = 0; corner < 4; corner++) {
                int offset = (face * 4 + corner) * LodVoxelMesher.VERTEX_STRIDE;
                if (Math.abs(buffer.getFloat(offset + 4) - planeY) > 1.0e-4f) {
                    flat = false;
                    break;
                }
            }
            if (flat) {
                count++;
            }
        }
        return count;
    }

    private static int alphaOfFirstVertex(LodVoxelMesher.Mesh mesh) {
        ByteBuffer buffer = ByteBuffer.wrap(mesh.vertices()).order(ByteOrder.LITTLE_ENDIAN);
        return (buffer.getInt(12) >>> 24) & 0xFF;
    }

    @Test
    void singleBlockEmitsSixFaces() {
        LodVoxelMesher.Mesh mesh = LodVoxelMesher.mesh(
            new Builder().put(5, 64, 5, 0x808080).build(), 0, 0, 0, 0, 0);
        assertEquals(6, mesh.faces(), "一个孤立方块应该有 6 个面");
        assertEquals(24, mesh.vertices().length / LodVoxelMesher.VERTEX_STRIDE);
    }

    @Test
    void solidCubeCullsInteriorFaces() {
        Builder builder = new Builder();
        for (int y = 64; y < 66; y++) {
            for (int z = 5; z < 7; z++) {
                for (int x = 5; x < 7; x++) {
                    builder.put(x, y, z, 0x808080);
                }
            }
        }
        LodVoxelMesher.Mesh mesh = LodVoxelMesher.mesh(builder.build(), 0, 0, 0, 0, 0);
        // 2×2×2 实心：6 个面各 4 片，内部的 12 个面必须被剔除
        assertEquals(24, mesh.faces(), "实心立方体只该有外壳，内部面必须剔除");
    }

    @Test
    void hollowHouseKeepsRoofAirAndGround() {
        // 地面 5×5 @ y=63
        // 墙   y=64..65，只有外圈（中间 3×3 是空气）
        // 屋顶 5×5 @ y=66
        Builder builder = new Builder();
        for (int z = 5; z < 10; z++) {
            for (int x = 5; x < 10; x++) {
                builder.put(x, 63, z, 0x7A5A3A);          // 地面
                builder.put(x, 66, z, 0xB03A2E);          // 屋顶
            }
        }
        for (int y = 64; y < 66; y++) {
            for (int z = 5; z < 10; z++) {
                for (int x = 5; x < 10; x++) {
                    boolean ring = x == 5 || x == 9 || z == 5 || z == 9;
                    if (ring) {
                        builder.put(x, y, z, 0xD8C8A8);   // 墙
                    }
                }
            }
        }
        LodVoxelMesher.Mesh mesh = LodVoxelMesher.mesh(builder.build(), 0, 0, 0, 0, 0);

        // 屋顶顶面 5×5
        assertEquals(25, facesOnPlane(mesh, 67.0f), "屋顶顶面应有 25 片");
        // 屋顶底面只出现在中间 3×3 的架空处 —— 高度场做不到这一点
        assertEquals(9, facesOnPlane(mesh, 66.0f), "屋顶底面（架空处）应有 9 片");
        // 地面顶面同样只在中间 3×3 露出来
        assertEquals(9, facesOnPlane(mesh, 64.0f), "地面顶面（墙内）应有 9 片");
        // 地面底面
        assertEquals(25, facesOnPlane(mesh, 63.0f), "地面底面应有 25 片");
        // 两层墙之间不该出现任何水平面
        assertEquals(0, facesOnPlane(mesh, 65.0f), "两层墙之间不该有水平面");
        // 总面数：地面 54 + 墙 32×2 + 屋顶 54
        assertEquals(172, mesh.faces(), "小房子总面数");
    }

    @Test
    void waterIsMarkedInAlpha() {
        int water = LodRegionCache.FLAG_WATER | 0x3F76E4;
        LodVoxelMesher.Mesh mesh = LodVoxelMesher.mesh(
            new Builder().put(5, 64, 5, water).build(), 0, 0, 0, 0, 0);
        assertEquals(6, mesh.faces());
        assertEquals(0, alphaOfFirstVertex(mesh), "水体顶点的 alpha 标记应为 0");
    }

    @Test
    void downsamplingKeepsSurfaceAndDropsFaceCount() {
        // 4×4×4 实心块，按 2 方块降采样后变成 2×2×2
        Builder builder = new Builder();
        for (int y = 64; y < 68; y++) {
            for (int z = 4; z < 8; z++) {
                for (int x = 4; x < 8; x++) {
                    builder.put(x, y, z, 0x808080);
                }
            }
        }
        LodVoxelMesher.Mesh full = LodVoxelMesher.mesh(builder.build(), 0, 0, 0, 0, 0);
        LodVoxelMesher.Mesh half = LodVoxelMesher.mesh(builder.build(), 0, 0, 0, 0, 1);
        assertEquals(6 * 16, full.faces(), "4×4×4 实心：6 个面各 16 片");
        assertEquals(6 * 4, half.faces(), "2 方块降采样后：6 个面各 4 片");
        assertTrue(half.vertices().length < full.vertices().length);
    }

    @Test
    void undergroundCaveFacesArePruned() {
        // 一整块 16×16×24 的实心地形：只有外表面。再在深处挖一个 4×4×4 的洞，
        // 洞壁在远景里永远看不到，面数必须和不挖洞时完全一样。
        Builder solid = new Builder();
        for (int y = 40; y <= 63; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    solid.put(x, y, z, 0x808080);
                }
            }
        }
        Builder cave = new Builder();
        for (int y = 40; y <= 63; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    boolean hole = x >= 6 && x < 10 && z >= 6 && z < 10 && y >= 48 && y < 52;
                    if (!hole) {
                        cave.put(x, y, z, 0x808080);
                    }
                }
            }
        }
        LodVoxelMesher.Mesh solidMesh = LodVoxelMesher.mesh(solid.build(), 0, 0, 0, 0, 0);
        LodVoxelMesher.Mesh caveMesh = LodVoxelMesher.mesh(cave.build(), 0, 0, 0, 0, 0);
        assertEquals(solidMesh.faces(), caveMesh.faces(), "地下洞穴的洞壁不该产生任何面");
    }
}
