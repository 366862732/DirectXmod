package com.dx12.mixin;

import java.nio.ByteBuffer;
import java.util.Map;

import org.lwjgl.opengl.GL32C;
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
 * Capture the terrain atlas directly from the GL texture after MC uploads it.
 *
 * Solves two problems:
 * 1. Finding the right GL texture — when upload() returns, the atlas is STILL
 *    bound as GL_TEXTURE_2D (MC binds it and doesn't unbind).
 * 2. Y-flip — glGetTexImage returns bottom-up; we flip to top-down for D3D12.
 */
@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private Map<Identifier, TextureAtlasSprite> texturesByName;

    @Inject(method = "upload", at = @At("TAIL"))
    private void onAtlasUploaded(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        TextureAtlas self = (TextureAtlas)(Object)this;
        Identifier blocksId = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
        if (!self.location().equals(blocksId)) return;

        int atlasWidth = this.width;
        int atlasHeight = this.height;

        if (atlasWidth <= 0 || atlasHeight <= 0) {
            Dx12Mod.LOGGER.warn("[dx12-wm] Atlas has zero size {}x{}, skipping GL capture", atlasWidth, atlasHeight);
            return;
        }

        Dx12Mod.LOGGER.info("[dx12-wm] Capturing atlas from GL: {}x{}", atlasWidth, atlasHeight);

        try {
            // At @TAIL, verify what texture is currently bound
            int boundTex = GL32C.glGetInteger(GL32C.GL_TEXTURE_BINDING_2D);
            int texWidth = GL32C.glGetTexLevelParameteri(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_TEXTURE_WIDTH);
            int texHeight = GL32C.glGetTexLevelParameteri(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_TEXTURE_HEIGHT);
            Dx12Mod.LOGGER.info("[dx12-wm] Currently bound GL texture: id={}, size={}x{} (expect {}x{})",
                boundTex, texWidth, texHeight, atlasWidth, atlasHeight);

            // If the currently-bound texture doesn't match the atlas size, try binding by ID.
            // The GL texture ID lives in AbstractTexture — find it via reflection.
            if (texWidth != atlasWidth || texHeight != atlasHeight) {
                Dx12Mod.LOGGER.info("[dx12-wm] Bound texture mismatch, searching for atlas GL ID...");
                int glId = findGlId(self);
                if (glId > 0) {
                    GL32C.glBindTexture(GL32C.GL_TEXTURE_2D, glId);
                    texWidth = GL32C.glGetTexLevelParameteri(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_TEXTURE_WIDTH);
                    texHeight = GL32C.glGetTexLevelParameteri(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_TEXTURE_HEIGHT);
                    Dx12Mod.LOGGER.info("[dx12-wm] Bound atlas GL id={}: size={}x{}", glId, texWidth, texHeight);
                }
            }

            int readWidth = atlasWidth;
            int readHeight = atlasHeight;
            if (texWidth > 0 && texHeight > 0) {
                readWidth = texWidth;
                readHeight = texHeight;
            }

            ByteBuffer atlasBuffer = MemoryUtil.memAlloc(readWidth * readHeight * 4);
            GL32C.glGetTexImage(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_RGBA, GL32C.GL_UNSIGNED_BYTE, atlasBuffer);

            int glErr = GL32C.glGetError();
            if (glErr != 0) {
                Dx12Mod.LOGGER.warn("[dx12-wm] GL error reading atlas: 0x{}", Integer.toHexString(glErr));
                MemoryUtil.memFree(atlasBuffer);
                return;
            }

            // Diagnostic: dump a grid of pixels to understand atlas layout
            for (int y = 0; y < readHeight && y < 2048; y += 128) {
                int off = (y * readWidth + 0) * 4;  // x=0
                if (off + 4 <= atlasBuffer.capacity()) {
                    Dx12Mod.LOGGER.info("[dx12-wm] atlas pixel (x=0,y={}): RGBA=({},{},{},{})",
                        y,
                        atlasBuffer.get(off) & 0xFF,
                        atlasBuffer.get(off+1) & 0xFF,
                        atlasBuffer.get(off+2) & 0xFF,
                        atlasBuffer.get(off+3) & 0xFF);
                }
            }

            Dx12Mod.LOGGER.info("[dx12-wm] GL atlas captured: {}x{}, uploading to D3D12", readWidth, readHeight);

            // Upload directly (no Y-flip — wgpu/D3D12 and OpenGL texture conventions
            // are handled by the texture sampler's address mode)
            D3D12Bridge.uploadTerrainAtlas(atlasBuffer, readWidth, readHeight);

            MemoryUtil.memFree(atlasBuffer);
        } catch (Exception e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] GL atlas capture failed: {}", e.toString());
        }
    }

    /** Find the GL texture ID field in AbstractTexture via reflection. */
    private static int findGlId(Object obj) {
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.getType() == int.class) {
                    try {
                        field.setAccessible(true);
                        int val = field.getInt(obj);
                        if (val > 0 && val < 100000) {
                            return val;
                        }
                    } catch (Exception ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
        return -1;
    }
}
