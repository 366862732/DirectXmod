package com.dx12.mixin;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;
import com.dx12.Dx12Mod;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * Capture terrain atlas via two-phase composition.
 *
 * Phase 1 (@HEAD): read NativeImage pixel data from Preparations.regions.
 * Phase 2 (@TAIL): read final sprite positions from texturesByName, composite, upload.
 *
 * This avoids the sprite position mismatch between Preparations and the GPU atlas.
 */
@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private Map<Identifier, TextureAtlasSprite> texturesByName;

    /**
     * Cached pixel data + dimensions + Preparations position, captured at HEAD.
     * Uses FRAME dimensions (fw,fh) for writing, not NativeImage dimensions (w,h).
     * MC's SpriteContents.originalImage may be larger than the frame for
     * animated sprites (e.g. lava: 16x64 NativeImage but only 16x16 frame).
     * Using frame dimensions prevents writing extra animation frames that
     * overwrite adjacent sprite data in the composited atlas.
     */
    private static class PixelData {
        final byte[] pixels;
        final int w, h;       // NativeImage dimensions (may include all animation frames)
        final int fw, fh;     // Frame dimensions (the actual rendered size)
        final int prepX, prepY;
        final int padding;    // MC's sprite border padding (usually 1px per side)
        PixelData(byte[] pixels, int w, int h, int fw, int fh, int prepX, int prepY, int padding) {
            this.pixels = pixels; this.w = w; this.h = h;
            this.fw = fw; this.fh = fh;
            this.prepX = prepX; this.prepY = prepY;
            this.padding = padding;
        }
    }

    private static final ThreadLocal<Map<Identifier, PixelData>> PIXEL_CACHE = ThreadLocal.withInitial(HashMap::new);

    @Inject(method = "upload", at = @At("HEAD"))
    private void onAtlasUploadHead(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        TextureAtlas self = (TextureAtlas)(Object)this;
        if (!self.location().equals(Identifier.withDefaultNamespace("textures/atlas/blocks.png"))) return;

        PIXEL_CACHE.get().clear();

        try {
            // Access Preparations.regions
            Map<?, ?> regions = null;
            for (Field f : preparations.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == Map.class) { regions = (Map<?, ?>) f.get(preparations); break; }
            }
            if (regions == null || regions.isEmpty()) return;

            // Find field handles
            Field contentsField = TextureAtlasSprite.class.getDeclaredField("contents");
            contentsField.setAccessible(true);

            Object firstContents = contentsField.get(regions.values().iterator().next());

            // MC uploads byMipLevel[0] to GPU (uploadFirstFrame uses byMipLevel[level]).
            // byMipLevel[0] after mipmap generation may differ from originalImage.
            Field byMipLevelField = firstContents.getClass().getDeclaredField("byMipLevel");
            byMipLevelField.setAccessible(true);
            Object[] firstMips = (Object[]) byMipLevelField.get(firstContents);
            Object firstNI = (firstMips != null && firstMips.length > 0) ? firstMips[0] : null;

            // Sprite padding from TextureAtlasSprite (usually 1px per side)
            Field paddingField = TextureAtlasSprite.class.getDeclaredField("padding");
            paddingField.setAccessible(true);

            // Frame dimensions from SpriteContents.width/height (not NativeImage dimensions)
            Field contentsW = firstContents.getClass().getDeclaredField("width");
            Field contentsH = firstContents.getClass().getDeclaredField("height");
            contentsW.setAccessible(true);
            contentsH.setAccessible(true);

            Class<?> niCls = (firstNI != null) ? firstNI.getClass() : null;
            if (niCls == null) return;
            Field niPtr = null, niW = null, niH = null;
            for (Field f : niCls.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == long.class && f.getName().equals("pixels")) niPtr = f;
                if (f.getType() == int.class && f.getName().equals("width")) niW = f;
                if (f.getType() == int.class && f.getName().equals("height")) niH = f;
            }
            if (niPtr == null) return;

            Map<Identifier, PixelData> cache = PIXEL_CACHE.get();
            int bpp = 4;

            for (Object eo : regions.entrySet()) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) eo;
                Identifier id = (Identifier) e.getKey();
                TextureAtlasSprite sp = (TextureAtlasSprite) e.getValue();
                Object ct = contentsField.get(sp);
                // Read byMipLevel[0] instead of originalImage — matches uploadFirstFrame()
                Object[] mips = (Object[]) byMipLevelField.get(ct);
                Object ni = (mips != null && mips.length > 0) ? mips[0] : null;
                if (ni == null) continue;
                int w = niW.getInt(ni), h = niH.getInt(ni);
                if (w <= 0 || h <= 0) continue;
                long ptr = niPtr.getLong(ni);
                if (ptr == 0) continue;

                ByteBuffer src = MemoryUtil.memByteBuffer(ptr, w * h * bpp);
                byte[] data = new byte[w * h * bpp];
                for (int i = 0; i < data.length; i++) data[i] = src.get(i);
                // SpriteContents.width/height = frame dimensions (from frameSize in constructor).
                // These differ from NativeImage dimensions for animated sprites
                // (e.g. lava: 16x64 NativeImage but only 16x16 frame).
                int fw = contentsW.getInt(ct), fh = contentsH.getInt(ct);
                int pad = paddingField.getInt(sp);
                cache.put(id, new PixelData(data, w, h, fw, fh, sp.getX(), sp.getY(), pad));
            }

            Dx12Mod.LOGGER.info("[dx12-wm] HEAD: cached {} sprite pixel buffers", cache.size());
        } catch (Exception e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] HEAD cache failed: {}", e.toString());
            PIXEL_CACHE.get().clear();
        }
    }

    @Inject(method = "upload", at = @At("TAIL"))
    private void onAtlasUploadTail(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        TextureAtlas self = (TextureAtlas)(Object)this;
        if (!self.location().equals(Identifier.withDefaultNamespace("textures/atlas/blocks.png"))) return;

        Map<Identifier, PixelData> cache = PIXEL_CACHE.get();
        if (cache.isEmpty()) return;

        try {
            int w = this.width, h = this.height;
            if (w <= 0 || h <= 0) return;

            Dx12Mod.LOGGER.info("[dx12-wm] TAIL: compositing {}x{} from texturesByName ({} sprites)", w, h, texturesByName.size());

            ByteBuffer atlas = MemoryUtil.memAlloc(w * h * 4);
            for (int i = 0; i < atlas.capacity(); i++) atlas.put(i, (byte) 0);

            int count = 0;
            for (Map.Entry<Identifier, TextureAtlasSprite> e : texturesByName.entrySet()) {
                TextureAtlasSprite sp = e.getValue();
                int sx = sp.getX(), sy = sp.getY();
                PixelData pd = cache.get(e.getKey());
                if (pd == null) continue;

                // Use FRAME dimensions (fw,fh) to write only the first frame.
                // pd.w/pd.h = NativeImage size (may include all animation frames).
                // pd.fw/pd.fh = frame size (from SpriteContents.frameSize).
                // MC's uploadFirstFrame() also only writes frameSize region.
                //
                // CRITICAL: MC's TextureAtlasSprite UV coordinates are computed as
                //   u0 = (x + padding) / atlasWidth
                //   v0 = (y + padding) / atlasHeight
                // where padding is the transparent border around each sprite (usually 1px).
                // The GPU atlas blit also positions sprites at (x, y) with padding.
                // Since padding shifts the actual pixel data right/down by `padding` px,
                // we must write pixel data at (sx + padding, sy + padding).
                int pad = pd.padding;
                int srcStride = pd.w * 4;           // NativeImage row stride
                int writeBytes = pd.fw * 4;          // Only write frame-width pixels
                int writeRows = Math.min(pd.fh, pd.h); // Clamp to available data
                for (int py = 0; py < writeRows; py++) {
                    int srcOff = py * srcStride;
                    int dstOff = ((sy + pad + py) * w + (sx + pad)) * 4;
                    if (dstOff + writeBytes > atlas.capacity()) break;
                    for (int px = 0; px < writeBytes; px++) {
                        atlas.put(dstOff + px, pd.pixels[srcOff + px]);
                    }
                }
                count++;
            }

            // Count how many sprites were skipped (missing from HEAD cache)
            int missingCount = 0;
            for (Identifier key : texturesByName.keySet()) {
                if (!cache.containsKey(key)) missingCount++;
            }
            Dx12Mod.LOGGER.info("[dx12-wm] TAIL: composited {}/{} sprites ({} missing from cache)",
                count, texturesByName.size(), missingCount);

            // DIAGNOSTIC: sprite position consistency check
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int mismatchCount = 0, totalChecked = 0;
            for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                TextureAtlasSprite ds = de.getValue();
                int tx = ds.getX(), ty = ds.getY();
                PixelData cp = cache.get(de.getKey());
                if (cp == null) continue;
                totalChecked++;
                minY = Math.min(minY, ty);
                maxY = Math.max(maxY, ty);
                if (tx != cp.prepX || ty != cp.prepY) {
                    mismatchCount++;
                    if (mismatchCount <= 3) {
                        Dx12Mod.LOGGER.info("[dx12-wm] CMP_MISMATCH: {} prep=({},{}), tx=({},{})",
                            de.getKey(), cp.prepX, cp.prepY, tx, ty);
                    }
                }
            }
            Dx12Mod.LOGGER.info("[dx12-wm] SPRITE_Y_RANGE: y=[{},{}] total={} mismatches={}",
                minY, maxY, totalChecked, mismatchCount);

            // DIAGNOSTIC: Check key atlas pixels right before JNI
            int[][] checkXY = {{16,1232},{1616,256},{1632,320}};
            String[] checkLabels = {"old_chunk","prev_chunk","this_chunk"};
            for (int ck = 0; ck < checkXY.length; ck++) {
                int cx = checkXY[ck][0], cy = checkXY[ck][1];
                int off = (cy * w + cx) * 4;
                if (off + 3 < atlas.capacity()) {
                    int r = atlas.get(off) & 0xFF, g = atlas.get(off+1) & 0xFF;
                    int b = atlas.get(off+2) & 0xFF, a = atlas.get(off+3) & 0xFF;
                    Dx12Mod.LOGGER.info("[dx12-wm] PIXEL ({},{}) {} RGBA=({},{},{},{})",
                        cx, cy, checkLabels[ck], r, g, b, a);
                    // Find covering sprite (accounting for padding)
                    for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                        TextureAtlasSprite ds = de.getValue();
                        PixelData dpd = cache.get(de.getKey());
                        if (dpd == null) continue;
                        int spx = ds.getX() + dpd.padding;
                        int spy = ds.getY() + dpd.padding;
                        if (cx >= spx && cx < spx + dpd.fw &&
                            cy >= spy && cy < spy + dpd.fh) {
                            int pixOff = ((cy - spy) * dpd.w + (cx - spx)) * 4;
                            int pr = dpd.pixels[pixOff] & 0xFF, pg = dpd.pixels[pixOff+1] & 0xFF;
                            int pb = dpd.pixels[pixOff+2] & 0xFF, pa = dpd.pixels[pixOff+3] & 0xFF;
                            Dx12Mod.LOGGER.info("[dx12-wm]   → sprite {} at ({},{}) padding={} src_pixel=({},{},{},{})",
                                de.getKey(), ds.getX(), ds.getY(), dpd.padding, pr, pg, pb, pa);
                        }
                    }
                }
            }
            // Also check the first-pixel of sprite pixel data
            int zeroSrcCount = 0;
            for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                PixelData dpd = cache.get(de.getKey());
                if (dpd == null) continue;
                if (dpd.pixels.length >= 4) {
                    int r = dpd.pixels[0] & 0xFF, g = dpd.pixels[1] & 0xFF;
                    int b = dpd.pixels[2] & 0xFF, a = dpd.pixels[3] & 0xFF;
                    if (r == 0 && g == 0 && b == 0 && a == 0) {
                        zeroSrcCount++;
                        if (zeroSrcCount <= 3) {
                            Dx12Mod.LOGGER.info("[dx12-wm] ZERO_PIXEL_DATA: sprite {} at ({},{}) fw={} fh={}",
                                de.getKey(), de.getValue().getX(), de.getValue().getY(),
                                dpd.fw, dpd.fh);
                        }
                    }
                }
            }
            Dx12Mod.LOGGER.info("[dx12-wm] ZERO_PIXEL_COUNT: {} out of {} sprites have zero first pixel",
                zeroSrcCount, cache.size());

            // DIAGNOSTIC: dump first 10 sprite padding values to verify fix
            int padDumpCount = 0;
            for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                PixelData dpd = cache.get(de.getKey());
                if (dpd == null) continue;
                if (padDumpCount < 10) {
                    Dx12Mod.LOGGER.info("[dx12-wm] PADDING_CHECK: {} at ({},{}) padding={} fw={} fh={}",
                        de.getKey(), dpd.prepX, dpd.prepY, dpd.padding, dpd.fw, dpd.fh);
                    padDumpCount++;
                }
            }
            // Also check if (1023,1039) area has sprite data in the ATLAS (post-composite)
            // This helps verify the padding-fix write position
            int[][] checkChunkPositions = {{1023,1039},{1024,1040},{1022,1038}};
            String[] chunkLabels = {"near", "exact", "far"};
            for (int ck = 0; ck < checkChunkPositions.length; ck++) {
                int cx = checkChunkPositions[ck][0], cy = checkChunkPositions[ck][1];
                int off = (cy * w + cx) * 4;
                if (off + 3 < atlas.capacity()) {
                    int r = atlas.get(off) & 0xFF, g = atlas.get(off+1) & 0xFF;
                    int b = atlas.get(off+2) & 0xFF, a = atlas.get(off+3) & 0xFF;
                    Dx12Mod.LOGGER.info("[dx12-wm] CHUNK_PIXEL ({},{}) {} RGBA=({},{},{},{})",
                        cx, cy, chunkLabels[ck], r, g, b, a);
                }
            }

            D3D12Bridge.uploadTerrainAtlas(atlas, w, h);
            MemoryUtil.memFree(atlas);
        } catch (Exception e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] TAIL failed: {}", e.toString());
            for (StackTraceElement se : e.getStackTrace()) Dx12Mod.LOGGER.warn("  at {}", se.toString());
        } finally {
            cache.clear();
        }
    }
}
