package com.dx12.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;
import com.dx12.Dx12Mod;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * Capture the terrain atlas texture before MC uploads it to GPU.
 * Reads sprite data from SpriteLoader.Preparations (since self.texturesByName
 * is still empty at HEAD of upload() in MC 26.1.2 Blaze3D).
 */
@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private Map<Identifier, TextureAtlasSprite> texturesByName;

    // Cached reflective access
    private static Field nativeImagePixelsField;
    private static Field spriteContentsImageField;
    private static Field spriteContentsWidthField;
    private static Field spriteContentsHeightField;
    private static Field preparationsWidthField;
    private static Field preparationsHeightField;

    // Preparations sprite Map field (discovered at runtime)
    private static Field preparationsSpritesField;
    private static Method spriteGetXMethod;
    private static Method spriteGetYMethod;

    @Inject(method = "upload", at = @At("HEAD"))
    private void onAtlasUploaded(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        TextureAtlas self = (TextureAtlas)(Object)this;
        Identifier blocksId = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
        if (!self.location().equals(blocksId)) return;

        try {
            initReflection(preparations);

            int atlasWidth = preparationsWidthField.getInt(preparations);
            int atlasHeight = preparationsHeightField.getInt(preparations);

            if (atlasWidth <= 0 || atlasHeight <= 0) {
                Dx12Mod.LOGGER.warn("[dx12-wm] Preparations has zero size {}x{}, skipping", atlasWidth, atlasHeight);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Identifier, ?> spriteMap = (Map<Identifier, ?>) preparationsSpritesField.get(preparations);
            if (spriteMap == null || spriteMap.isEmpty()) {
                Dx12Mod.LOGGER.warn("[dx12-wm] Preparations sprite map is null/empty, skipping");
                return;
            }

            Dx12Mod.LOGGER.info("[dx12-wm] Capturing atlas from Preparations: {}x{} with {} sprites",
                atlasWidth, atlasHeight, spriteMap.size());

            ByteBuffer atlasBuffer = MemoryUtil.memAlloc(atlasWidth * atlasHeight * 4);
            // Fill with opaque white
            for (int i = 0; i < atlasWidth * atlasHeight * 4; i += 4) {
                atlasBuffer.putInt(i, 0xFFFFFFFF);
            }

            int composited = 0;
            boolean firstSampled = false;
            int probeX = 1232, probeY = 1136;  // diagnostic: coords from chunk UV
            int nearProbe = 0, minSpriteX = Integer.MAX_VALUE, maxSpriteX = 0, minSpriteY = Integer.MAX_VALUE, maxSpriteY = 0;
            for (Map.Entry<Identifier, ?> entry : spriteMap.entrySet()) {
                Object spriteValue = entry.getValue();
                if (spriteValue == null) continue;

                // spriteValue could be TextureAtlasSprite (has getX/getY and contents())
                // or SpriteContents (has originalImage but no position)
                TextureAtlasSprite sprite = null;
                SpriteContents contents = null;

                if (spriteValue instanceof TextureAtlasSprite) {
                    sprite = (TextureAtlasSprite) spriteValue;
                    contents = sprite.contents();
                } else if (spriteValue instanceof SpriteContents) {
                    // If Preparations stores SpriteContents directly, we need position from elsewhere
                    // Try texturesByName first, then try calling getX/getY on spriteValue
                    contents = (SpriteContents) spriteValue;
                }

                if (contents == null) continue;

                int sx = 0, sy = 0;
                if (sprite != null) {
                    sx = sprite.getX();
                    sy = sprite.getY();
                } else {
                    // Try reflection for getX/getY on the sprite value
                    try {
                        if (spriteGetXMethod != null) {
                            sx = (int) spriteGetXMethod.invoke(spriteValue);
                            sy = (int) spriteGetYMethod.invoke(spriteValue);
                        }
                    } catch (Exception e) {
                        // No position available, skip this sprite
                        continue;
                    }
                }

                int sw = spriteContentsWidthField.getInt(contents);
                int sh = spriteContentsHeightField.getInt(contents);

                if (sx < 0 || sy < 0 || sx + sw > atlasWidth || sy + sh > atlasHeight) continue;

                // Diagnostic: find which sprite covers probe (1232,1136) — moved before null checks
                if (sx <= probeX && probeX < sx + sw && sy <= probeY && probeY < sy + sh) {
                    eprintln("Sprite '%s' covers probe(%d,%d): at(%d,%d) %dx%d contents=%s nativeImage=%s",
                            entry.getKey(), probeX, probeY, sx, sy, sw, sh,
                            contents != null ? "ok" : "null",
                            "?");
                }
                // Track sprite bounding box coverage
                if (sx + sw >= probeX - 32 && sx <= probeX + 32 && sy + sh >= probeY - 32 && sy <= probeY + 32) nearProbe++;
                if (sx + sw > maxSpriteX) maxSpriteX = sx + sw;
                if (sy + sh > maxSpriteY) maxSpriteY = sy + sh;
                if (sx < minSpriteX) minSpriteX = sx;
                if (sy < minSpriteY) minSpriteY = sy;

                Object nativeImage = spriteContentsImageField.get(contents);
                if (nativeImage == null) continue;

                long pixelsPtr = nativeImagePixelsField.getLong(nativeImage);
                if (pixelsPtr == 0) continue;

                // Diagnostic: dump first sprite's native image pixels
                if (!firstSampled) {
                    firstSampled = true;
                    int sampleCount = Math.min(sw * sh, 4);
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("[dx12-wm] First sprite '%s' at(%d,%d) %dx%d: ", entry.getKey(), sx, sy, sw, sh));
                    for (int k = 0; k < sampleCount; k++) {
                        int r = MemoryUtil.memGetByte(pixelsPtr + (long)k * 4) & 0xFF;
                        int g = MemoryUtil.memGetByte(pixelsPtr + (long)k * 4 + 1) & 0xFF;
                        int b = MemoryUtil.memGetByte(pixelsPtr + (long)k * 4 + 2) & 0xFF;
                        int a = MemoryUtil.memGetByte(pixelsPtr + (long)k * 4 + 3) & 0xFF;
                        sb.append(String.format(" (R%d,G%d,B%d,A%d)", r, g, b, a));
                    }
                    System.err.println(sb.toString());
                }

                // Copy sprite pixels into atlas row by row (RGBA format)
                for (int row = 0; row < sh; row++) {
                    long srcOffset = (long) row * sw * 4;
                    int dstOffset = ((sy + row) * atlasWidth + sx) * 4;
                    MemoryUtil.memCopy(
                        pixelsPtr + srcOffset,
                        MemoryUtil.memAddress(atlasBuffer) + dstOffset,
                        (long) sw * 4
                    );
                }
                composited++;
            }

            Dx12Mod.LOGGER.info("[dx12-wm] Composited {}/{} sprites, uploading to D3D12",
                composited, spriteMap.size());

            eprintln("Sprites near probe(%d,%d): %d (total sprites=%d, atlas=%dx%d)",
                probeX, probeY, nearProbe, spriteMap.size(), atlasWidth, atlasHeight);
            eprintln("Sprite coverage: X[%d..%d] Y[%d..%d] of %dx%d",
                minSpriteX, maxSpriteX, minSpriteY, maxSpriteY, atlasWidth, atlasHeight);

            if (composited > 0) {
                D3D12Bridge.uploadTerrainAtlas(atlasBuffer, atlasWidth, atlasHeight);
            }

            MemoryUtil.memFree(atlasBuffer);
        } catch (Exception e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] Atlas capture failed: {}", e.toString());
        }
    }

    private static void initReflection(Object preparations) throws Exception {
        if (spriteContentsImageField != null) return;

        spriteContentsImageField = SpriteContents.class.getDeclaredField("originalImage");
        spriteContentsImageField.setAccessible(true);
        spriteContentsWidthField = SpriteContents.class.getDeclaredField("width");
        spriteContentsWidthField.setAccessible(true);
        spriteContentsHeightField = SpriteContents.class.getDeclaredField("height");
        spriteContentsHeightField.setAccessible(true);

        Class<?> nativeImageClass = Class.forName("com.mojang.blaze3d.platform.NativeImage");
        nativeImagePixelsField = nativeImageClass.getDeclaredField("pixels");
        nativeImagePixelsField.setAccessible(true);

        Class<?> prepClass = SpriteLoader.Preparations.class;
        preparationsWidthField = prepClass.getDeclaredField("width");
        preparationsWidthField.setAccessible(true);
        preparationsHeightField = prepClass.getDeclaredField("height");
        preparationsHeightField.setAccessible(true);

        // Discover the sprite Map field in Preparations
        // Dump all fields for diagnostic
        for (Field f : prepClass.getDeclaredFields()) {
            f.setAccessible(true);
            eprintln("[dx12-wm] Preparations field: {} : {} = {}",
                f.getName(), f.getType().getSimpleName(),
                f.get(preparations) != null ? f.get(preparations).getClass().getSimpleName() : "null");
        }

        // Try common field names for the sprite map
        for (String name : new String[]{"sprites", "regions", "byName", "textures", "contents", "spriteContents", "textureSprites"}) {
            try {
                Field f = prepClass.getDeclaredField(name);
                f.setAccessible(true);
                if (Map.class.isAssignableFrom(f.getType())) {
                    preparationsSpritesField = f;
                    eprintln("[dx12-wm] Found Preparations sprite map: {} (type={})",
                        name, f.getGenericType());
                    break;
                }
            } catch (NoSuchFieldException ignored) {}
        }

        if (preparationsSpritesField == null) {
            // Fallback: use first Map field found
            for (Field f : prepClass.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    preparationsSpritesField = f;
                    f.setAccessible(true);
                    eprintln("[dx12-wm] Fallback Preparations map: {}", f.getName());
                    break;
                }
            }
        }

        // Try to find getX/getY methods on sprite value type
        if (preparationsSpritesField != null) {
            preparationsSpritesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Identifier, ?> map = (Map<Identifier, ?>) preparationsSpritesField.get(preparations);
            if (map != null && !map.isEmpty()) {
                Object firstValue = map.values().iterator().next();
                if (firstValue != null) {
                    Class<?> valClass = firstValue.getClass();
                    eprintln("[dx12-wm] Sprite map value type: {}", valClass.getName());
                    try {
                        spriteGetXMethod = valClass.getMethod("getX");
                        spriteGetYMethod = valClass.getMethod("getY");
                        eprintln("[dx12-wm] Found getX/getY on {}", valClass.getSimpleName());
                    } catch (NoSuchMethodException e) {
                        // Try contents() method to get TextureAtlasSprite
                        try {
                            Method contentsMethod = valClass.getMethod("contents");
                            Object contents = contentsMethod.invoke(firstValue);
                            if (contents instanceof TextureAtlasSprite) {
                                spriteGetXMethod = TextureAtlasSprite.class.getMethod("getX");
                                spriteGetYMethod = TextureAtlasSprite.class.getMethod("getY");
                                eprintln("[dx12-wm] Found contents() -> TextureAtlasSprite with getX/getY");
                            }
                        } catch (Exception ignored2) {}
                    }
                }
            }
        }
    }

    // Helper for early diagnostic output (before LOGGER is configured)
    private static void eprintln(String fmt, Object... args) {
        String msg = "[dx12-wm] " + String.format(fmt.replace("{}", "%s"), args);
        System.err.println(msg);
    }
}
