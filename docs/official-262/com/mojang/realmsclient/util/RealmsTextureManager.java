/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsTextureManager {
    private static final Map<String, RealmsTexture> TEXTURES = Maps.newHashMap();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier TEMPLATE_ICON_LOCATION = Identifier.withDefaultNamespace("textures/gui/presets/isles.png");

    public static Identifier worldTemplate(String id, @Nullable String image) {
        if (image == null) {
            return TEMPLATE_ICON_LOCATION;
        }
        return RealmsTextureManager.getTexture(id, image);
    }

    private static Identifier getTexture(String id, String encodedImage) {
        RealmsTexture texture = TEXTURES.get(id);
        if (texture != null && texture.image().equals(encodedImage)) {
            return texture.textureId;
        }
        NativeImage image = RealmsTextureManager.loadImage(encodedImage);
        if (image == null) {
            Identifier missingTexture = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(id, new RealmsTexture(encodedImage, missingTexture));
            return missingTexture;
        }
        Identifier textureId = Identifier.fromNamespaceAndPath("realms", "dynamic/" + id);
        Minecraft.getInstance().getTextureManager().register(textureId, new DynamicTexture(textureId::toString, image));
        TEXTURES.put(id, new RealmsTexture(encodedImage, textureId));
        return textureId;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static @Nullable NativeImage loadImage(String encodedImage) {
        byte[] bytes = Base64.getDecoder().decode(encodedImage);
        ByteBuffer buffer = MemoryUtil.memAlloc((int)bytes.length);
        try {
            NativeImage nativeImage = NativeImage.read(buffer.put(bytes).flip());
            return nativeImage;
        }
        catch (IOException e) {
            LOGGER.warn("Failed to load world image: {}", (Object)encodedImage, (Object)e);
        }
        finally {
            MemoryUtil.memFree((ByteBuffer)buffer);
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public record RealmsTexture(String image, Identifier textureId) {
    }
}

