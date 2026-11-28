package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.xgdt.dx12.mixin.SpriteContentsAccessor;
import com.xgdt.dx12.mixin.TextureAtlasAccessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 给体素 LOD 用的方块图集 mip 链。
 *
 * <p>为什么需要单独再造一份：原版图集在当前配置下只有 mip0（日志
 * {@code 体素图集 SRV 已绑定：1024x1024 mips=1}）。LOD 远景一格只有一两个像素，
 * 一像素要覆盖几十个 texel，却只能采样 mip0 的某一个 texel —— 结果就是整片远景
 * 变成随机噪点，相机一动就「疯狂闪烁」。之前像素着色器手写的 4 抽平均只能覆盖
 * 很小的缩小倍率，而且会跨 sprite 取到相邻方块的随机颜色（彩虹噪点）。
 *
 * <p>做法：
 * <ol>
 *   <li>按原图集同样的布局，把每个 sprite 的原始 PNG 像素拷进一张 RGBA8 图；</li>
 *   <li>逐 sprite、逐级生成 mip：每个目标 texel 只平均<b>该 sprite 自己</b>的像素，
 *       不会把相邻方块的贴图混进来；</li>
 *   <li>mip 4 时 16px 方块正好收敛成 1 个 texel（该方块的平均色），远处是稳定的
 *       自然色块，而不是闪烁的噪点。</li>
 * </ol>
 *
 * <p>alpha 用预乘权重平均：树叶/玻璃的透明像素不会把颜色拉黑，镂空边缘在粗 mip
 * 里也不会糊成黑边。
 */
@Environment(EnvType.CLIENT)
final class LodMipAtlas {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-lod");

    /** mip0..mip4：16px 贴图正好收敛到 1 px，再深就会跨 sprite 串色。 */
    private static final int MIP_LEVELS = 5;

    private LodMipAtlas() {
    }

    record Result(Dx12GpuTexture texture, Dx12GpuTextureView view) {
    }

    /**
     * 原图集已经有 mip 时直接复用它的 view；否则用 CPU 生成一份带 mip 的副本。
     */
    static Result ensure(CommandEncoderBackend encoder, TextureAtlas atlas) {
        GpuTexture source = atlas.getTexture();
        if (source == null) {
            return null;
        }
        if (source.getMipLevels() > 1) {
            return new Result(null, new Dx12GpuTextureView(source, 0, source.getMipLevels()));
        }
        return build(encoder, atlas, source);
    }

    private static Result build(CommandEncoderBackend encoder, TextureAtlas atlas,
                                GpuTexture source) {
        int width = source.getWidth(0);
        int height = source.getHeight(0);
        if (width <= 0 || height <= 0) {
            return null;
        }
        int[] base = new int[width * height];
        List<int[]> levels = new ArrayList<>(MIP_LEVELS);
        levels.add(base);
        int lw = width;
        int lh = height;
        for (int level = 1; level < MIP_LEVELS; level++) {
            lw = Math.max(1, lw >> 1);
            lh = Math.max(1, lh >> 1);
            levels.add(new int[lw * lh]);
        }

        int copied = 0;
        List<TextureAtlasSprite> sprites =
            ((TextureAtlasAccessor) (Object) atlas).gl4dx12$sprites();
        for (TextureAtlasSprite sprite : sprites) {
            if (sprite == null || sprite.contents() == null) {
                continue;
            }
            SpriteContents contents = sprite.contents();
            NativeImage image =
                ((SpriteContentsAccessor) (Object) contents).gl4dx12$originalImage();
            if (image == null || image.isClosed()) {
                continue;
            }
            // getU0 已经带上了图集 padding，直接换算成实际像素起点；
            // 宽高用 SpriteContents 的帧尺寸（动画贴图只取第一帧）。
            int ox = Math.round(sprite.getU0() * width);
            int oy = Math.round(sprite.getV0() * height);
            int sw = Math.min(contents.width(), image.getWidth());
            int sh = Math.min(contents.height(), image.getHeight());
            sw = Math.min(sw, width - ox);
            sh = Math.min(sh, height - oy);
            if (sw <= 0 || sh <= 0) {
                continue;
            }
            copySprite(image, base, width, height, ox, oy, sw, sh);
            buildSpriteMips(base, width, height, ox, oy, sw, sh, levels);
            copied++;
        }
        if (copied == 0) {
            LOGGER.warn("[lod] 体素图集 mip 生成失败：没有可读的 sprite 像素");
            return null;
        }

        Dx12GpuTexture texture = new Dx12GpuTexture(
            GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
            "gl4dx12 lod atlas mips", GpuFormat.RGBA8_UNORM, width, height, 1, MIP_LEVELS);
        for (int level = 0; level < MIP_LEVELS; level++) {
            int[] pixels = levels.get(level);
            int mipW = Math.max(1, width >> level);
            int mipH = Math.max(1, height >> level);
            ByteBuffer data = ByteBuffer.allocateDirect(pixels.length * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
            for (int pixel : pixels) {
                // 内存里是 RGBA 顺序，int 打包成 0xAABBGGRR 才能按小端写出 R,G,B,A
                data.putInt(pixel);
            }
            data.flip();
            encoder.writeToTexture(texture, data, level, 0, 0, 0, mipW, mipH);
        }
        Dx12GpuTextureView view = new Dx12GpuTextureView(texture, 0, MIP_LEVELS);
        LOGGER.info("[lod] 体素图集 mip 链已生成：{}x{} levels={} sprites={}（原图集 mips={}）",
            width, height, MIP_LEVELS, copied, source.getMipLevels());
        return new Result(texture, view);
    }

    /** 把一个 sprite 的帧 0 像素拷进 base 图集；越界像素丢弃。 */
    private static void copySprite(NativeImage image, int[] base, int width, int height,
                                   int ox, int oy, int sw, int sh) {
        int imageW = image.getWidth();
        int imageH = image.getHeight();
        int[] pixels = null;
        try {
            pixels = image.getPixels();
        } catch (Throwable ignored) {
            // 非 RGBA 格式（少见）：退回逐像素读取
        }
        for (int y = 0; y < sh; y++) {
            int srcY = y;
            if (srcY < 0 || srcY >= imageH) {
                continue;
            }
            int dstRow = (oy + y) * width + ox;
            int srcRow = srcY * imageW;
            for (int x = 0; x < sw; x++) {
                int argb;
                if (pixels != null) {
                    argb = pixels[srcRow + x];
                } else {
                    argb = image.getPixel(x, srcY);
                }
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                base[dstRow + x] = (a << 24) | (b << 16) | (g << 8) | r;
            }
        }
    }

    /**
     * 逐级生成这个 sprite 的 mip。
     *
     * <p>每个目标 texel 只从<b>同一 sprite</b>的 mip0 区域里采样，因此不会把图集里
     * 相邻方块的颜色混进来。粗级别下 sprite 区域会缩到 1 个 texel，就是它的平均色。
     */
    private static void buildSpriteMips(int[] base, int width, int height,
                                        int x0, int y0, int sw, int sh, List<int[]> levels) {
        int x1 = x0 + sw;
        int y1 = y0 + sh;
        for (int level = 1; level < levels.size(); level++) {
            int scale = 1 << level;
            int lw = Math.max(1, width >> level);
            int lh = Math.max(1, height >> level);
            int[] dst = levels.get(level);
            int dx0 = Math.max(0, x0 / scale);
            int dy0 = Math.max(0, y0 / scale);
            int dx1 = Math.min(lw - 1, (x1 - 1) / scale);
            int dy1 = Math.min(lh - 1, (y1 - 1) / scale);
            for (int dy = dy0; dy <= dy1; dy++) {
                int sy0 = Math.max(y0, dy * scale);
                int sy1 = Math.min(y1, (dy + 1) * scale);
                if (sy1 <= sy0) {
                    continue;
                }
                for (int dx = dx0; dx <= dx1; dx++) {
                    int sx0 = Math.max(x0, dx * scale);
                    int sx1 = Math.min(x1, (dx + 1) * scale);
                    if (sx1 <= sx0) {
                        continue;
                    }
                    long sumA = 0L;
                    long sumR = 0L;
                    long sumG = 0L;
                    long sumB = 0L;
                    for (int sy = sy0; sy < sy1; sy++) {
                        int row = sy * width;
                        for (int sx = sx0; sx < sx1; sx++) {
                            int pixel = base[row + sx];
                            int a = (pixel >>> 24) & 0xFF;
                            if (a == 0) {
                                continue;
                            }
                            sumA += a;
                            sumR += (long) (pixel & 0xFF) * a;
                            sumG += (long) ((pixel >>> 8) & 0xFF) * a;
                            sumB += (long) ((pixel >>> 16) & 0xFF) * a;
                        }
                    }
                    int texels = (sx1 - sx0) * (sy1 - sy0);
                    int a = (int) (sumA / texels);
                    int r = sumA > 0 ? (int) (sumR / sumA) : 0;
                    int g = sumA > 0 ? (int) (sumG / sumA) : 0;
                    int b = sumA > 0 ? (int) (sumB / sumA) : 0;
                    dst[dy * lw + dx] = (a << 24) | (b << 16) | (g << 8) | r;
                }
            }
        }
    }
}
