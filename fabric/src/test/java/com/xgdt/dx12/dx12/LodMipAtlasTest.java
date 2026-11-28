package com.xgdt.dx12.dx12;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 离线校验 LOD 图集 mip 生成：每个目标 texel 只能平均同一个 sprite 自己的像素，
 * 透明像素不能把颜色拉黑。
 */
class LodMipAtlasTest {

    private static int rgba(int r, int g, int b, int a) {
        // base 数组的内存布局：0xAABBGGRR
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int red(int argb) {
        return argb & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static void buildMips(int[] base, int width, int height,
                                  int x0, int y0, int sw, int sh, List<int[]> levels)
        throws Exception {
        Method method = LodMipAtlas.class.getDeclaredMethod("buildSpriteMips",
            int[].class, int.class, int.class, int.class, int.class, int.class, int.class,
            List.class);
        method.setAccessible(true);
        method.invoke(null, base, width, height, x0, y0, sw, sh, levels);
    }

    private static List<int[]> levels(int width, int height, int levels) {
        List<int[]> out = new ArrayList<>();
        out.add(new int[width * height]);
        int w = width;
        int h = height;
        for (int level = 1; level < levels; level++) {
            w = Math.max(1, w >> 1);
            h = Math.max(1, h >> 1);
            out.add(new int[w * h]);
        }
        return out;
    }

    @Test
    void spriteMipsDoNotBleedIntoNeighbours() throws Exception {
        int width = 4;
        int height = 4;
        int[] base = new int[width * height];
        // 左上 2x2 = 纯红 sprite，右下 2x2 = 纯蓝 sprite
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                base[y * width + x] = rgba(255, 0, 0, 255);
                base[(y + 2) * width + (x + 2)] = rgba(0, 0, 255, 255);
            }
        }
        List<int[]> levels = levels(width, height, 3);

        buildMips(base, width, height, 0, 0, 2, 2, levels);
        buildMips(base, width, height, 2, 2, 2, 2, levels);

        int[] mip1 = levels.get(1); // 2x2
        assertEquals(255, red(mip1[0]));
        assertEquals(0, blue(mip1[0]));
        assertEquals(255, blue(mip1[3]));
        assertEquals(0, red(mip1[3]));
        assertEquals(255, alpha(mip1[0]));
    }

    @Test
    void alphaWeightedAverageKeepsColor() throws Exception {
        int width = 2;
        int height = 2;
        int[] base = new int[width * height];
        // 一个完全不透明的红色 + 三个透明黑色
        base[0] = rgba(255, 0, 0, 255);
        base[1] = rgba(0, 0, 0, 0);
        base[2] = rgba(0, 0, 0, 0);
        base[3] = rgba(0, 0, 0, 0);
        List<int[]> levels = levels(width, height, 2);

        buildMips(base, width, height, 0, 0, 2, 2, levels);

        int[] mip1 = levels.get(1); // 1x1
        assertEquals(255, red(mip1[0]), "透明像素不能把红色拉黑");
        assertEquals(0, green(mip1[0]));
        assertEquals(0, blue(mip1[0]));
        // 覆盖率 1/4
        assertEquals(63, alpha(mip1[0]));
    }
}
