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

            // DIAGNOSTIC: Check atlas pixel at chunk UV (1952,976) right before JNI
            int diagX = 1952, diagY = 976;
            int diagOff = (diagY * w + diagX) * 4;
            if (diagOff + 3 < atlas.capacity()) {
                Dx12Mod.LOGGER.info("[dx12-wm] PRE-JNI pixel ({},{}) RGBA=({},{},{},{})",
                    diagX, diagY,
                    atlas.get(diagOff) & 0xFF,
                    atlas.get(diagOff+1) & 0xFF,
                    atlas.get(diagOff+2) & 0xFF,
                    atlas.get(diagOff+3) & 0xFF);
                // Check if any sprite covers this pixel
                for (Map.Entry<Identifier, TextureAtlasSprite> de : texturesByName.entrySet()) {
                    TextureAtlasSprite ds = de.getValue();
                    int dsx = ds.getX(), dsy = ds.getY();
                    PixelData dpd = cache.get(de.getKey());
                    if (dpd == null) continue;
                    if (diagX >= dsx && diagX < dsx + dpd.w && diagY >= dsy && diagY < dsy + dpd.h) {
                        Dx12Mod.LOGGER.info("[dx12-wm] CHUNK_PX ({},{}) INSIDE sprite {} at ({},{}) {}x{}",
                            diagX, diagY, de.getKey(), dsx, dsy, dpd.w, dpd.h);
                    }
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
