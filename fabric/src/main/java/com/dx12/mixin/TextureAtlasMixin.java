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

    /** Cached pixel data + dimensions + Preparations position, captured at HEAD. */
    private static class PixelData {
        final byte[] pixels;
        final int w, h, prepX, prepY;
        PixelData(byte[] pixels, int w, int h, int prepX, int prepY) {
            this.pixels = pixels; this.w = w; this.h = h;
            this.prepX = prepX; this.prepY = prepY;
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

            Object fc = contentsField.get(regions.values().iterator().next());
            Field origImgField = fc.getClass().getDeclaredField("originalImage");
            origImgField.setAccessible(true);

            Class<?> niCls = origImgField.get(fc).getClass();
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
                Object ni = origImgField.get(ct);
                if (ni == null) continue;
                int w = niW.getInt(ni), h = niH.getInt(ni);
                if (w <= 0 || h <= 0) continue;
                long ptr = niPtr.getLong(ni);
                if (ptr == 0) continue;

                ByteBuffer src = MemoryUtil.memByteBuffer(ptr, w * h * bpp);
                byte[] data = new byte[w * h * bpp];
                for (int i = 0; i < data.length; i++) data[i] = src.get(i);
                cache.put(id, new PixelData(data, w, h, sp.getX(), sp.getY()));
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

                int rowBytes = pd.w * 4;
                for (int py = 0; py < pd.h; py++) {
                    int srcOff = py * rowBytes;
                    int dstOff = ((sy + py) * w + sx) * 4;
                    if (dstOff + rowBytes > atlas.capacity()) break;
                    for (int px = 0; px < rowBytes; px++) {
                        atlas.put(dstOff + px, pd.pixels[srcOff + px]);
                    }
                }
                count++;
            }

            Dx12Mod.LOGGER.info("[dx12-wm] TAIL: composited {}/{} sprites", count, texturesByName.size());

            // DIAGNOSTIC: check all 4 UV corners from the first chunk quad against all sprites
            float[][] quadUVs = {
                {0.6094f, 0.0312f},  // v0
                {0.6016f, 0.0312f},  // v1
                {0.6016f, 0.0391f},  // v2
                {0.6094f, 0.0391f},  // v3
            };
            for (int qi = 0; qi < quadUVs.length; qi++) {
                float chunkU = quadUVs[qi][0], chunkV = quadUVs[qi][1];
                int chunkPx = (int)(chunkU * w), chunkPy = (int)(chunkV * h);
                boolean foundSprite = false;
                for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                    TextureAtlasSprite ds = de.getValue();
                    PixelData dpd = cache.get(de.getKey());
                    if (dpd == null) continue;
                    int dsx = ds.getX(), dsy = ds.getY();
                    float su0 = (float)dsx / w, su1 = (float)(dsx + dpd.w) / w;
                    float sv0 = (float)dsy / h, sv1 = (float)(dsy + dpd.h) / h;
                    if (chunkU >= su0 && chunkU <= su1 && chunkV >= sv0 && chunkV <= sv1) {
                        int off = (chunkPy * w + chunkPx) * 4;
                        Dx12Mod.LOGGER.info("[dx12-wm] QUAD v{} UV ({},{}) INSIDE {} at ({},{}) {}x{}  atlasRGBA=({},{},{},{})",
                            qi, chunkU, chunkV, de.getKey(), dsx, dsy, dpd.w, dpd.h,
                            atlas.get(off) & 0xFF, atlas.get(off+1) & 0xFF,
                            atlas.get(off+2) & 0xFF, atlas.get(off+3) & 0xFF);
                        foundSprite = true;
                        break;
                    }
                }
                if (!foundSprite) {
                    Dx12Mod.LOGGER.info("[dx12-wm] QUAD v{} UV ({},{}) → pixel ({},{}) NOT covered by any sprite", qi, chunkU, chunkV, chunkPx, chunkPy);
                }
            }
            // Extended near-sprite search (by edge distance) around the quad center
            float centerU = 0.6055f, centerV = 0.03515f;  // midpoint of the quad
            int cpx = (int)(centerU * w), cpy = (int)(centerV * h);
            Dx12Mod.LOGGER.info("[dx12-wm]   Nearest sprites to quad center ({},{}):", cpx, cpy);
            int nearCount = 0;
            for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                TextureAtlasSprite ds = de.getValue();
                PixelData dpd = cache.get(de.getKey());
                if (dpd == null) continue;
                int dsx = ds.getX(), dsy = ds.getY(), dsw = dpd.w, dsh = dpd.h;
                // Edge distance: 0 if (cpx,cpy) inside rect, else distance to nearest edge
                int dx = cpx < dsx ? dsx - cpx : (cpx > dsx + dsw ? cpx - (dsx + dsw) : 0);
                int dy = cpy < dsy ? dsy - cpy : (cpy > dsy + dsh ? cpy - (dsy + dsh) : 0);
                int dist = Math.max(dx, 0) + Math.max(dy, 0);
                if (dist < 100 && nearCount < 12) {
                    Dx12Mod.LOGGER.info("[dx12-wm]   EdgeNear {}: rect=({}-{},{}-{}) edgeDist={}",
                        de.getKey(), dsx, dsx + dsw, dsy, dsy + dsh, dist);
                    nearCount++;
                }
            }

            // DIAGNOSTIC: compare Preparations vs texturesByName positions for first 5 sprites
            int cmpCount = 0;
            for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                if (cmpCount >= 5) break;
                TextureAtlasSprite ds = de.getValue();
                int tx = ds.getX(), ty = ds.getY();
                PixelData cp = cache.get(de.getKey());
                if (cp != null) {
                    boolean match = (tx == cp.prepX && ty == cp.prepY);
                    Dx12Mod.LOGGER.info("[dx12-wm] CMP {}: prep=({},{}), tx=({},{}) {}",
                        de.getKey(), cp.prepX, cp.prepY, tx, ty,
                        match ? "MATCH" : "DIFFER!");
                }
                cmpCount++;
            }
            // Specific check: grass_block_top (the closest sprite to the first quad)
            Identifier grassTopId = Identifier.withDefaultNamespace("block/grass_block_top");
            TextureAtlasSprite grassTop = texturesByName.get(grassTopId);
            PixelData grassTopPd = cache.get(grassTopId);
            if (grassTop != null && grassTopPd != null) {
                boolean match = (grassTop.getX() == grassTopPd.prepX && grassTop.getY() == grassTopPd.prepY);
                Dx12Mod.LOGGER.info("[dx12-wm] CMP_GRASS_BLOCK_TOP: prep=({},{}), tx=({},{}) {}",
                    grassTopPd.prepX, grassTopPd.prepY, grassTop.getX(), grassTop.getY(),
                    match ? "MATCH" : "DIFFER! ← THIS IS THE BUG");
            } else {
                Dx12Mod.LOGGER.info("[dx12-wm] CMP_GRASS_BLOCK_TOP: sprite={} pd={}", grassTop != null, grassTopPd != null);
            }
            // Search: which sprite in Preparations was at quad pixel (1232,64)?
            int qPx = 1232, qPy = 64;
            boolean foundPrep = false;
            for (Map.Entry<Identifier, PixelData> ce : cache.entrySet()) {
                PixelData cpd = ce.getValue();
                if (cpd.prepX == qPx && cpd.prepY == qPy && cpd.w > 0 && cpd.h > 0) {
                    Dx12Mod.LOGGER.info("[dx12-wm] PREP_SPRITE_AT ({},{}): {} ({}x{}) prepRGB=({},{},{})",
                        qPx, qPy, ce.getKey(), cpd.w, cpd.h,
                        cpd.pixels[0] & 0xFF, cpd.pixels[1] & 0xFF, cpd.pixels[2] & 0xFF);
                    foundPrep = true;
                }
            }
            if (!foundPrep) {
                Dx12Mod.LOGGER.info("[dx12-wm] PREP_SPRITE_AT ({},{}): NOT FOUND in {} cached sprites", qPx, qPy, cache.size());
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
