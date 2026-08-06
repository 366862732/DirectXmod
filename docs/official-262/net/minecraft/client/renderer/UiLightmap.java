/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.DynamicTexture;

@Environment(value=EnvType.CLIENT)
public class UiLightmap
implements AutoCloseable {
    private final DynamicTexture texture = new DynamicTexture("UI Lightmap", 1, 1, false);

    public UiLightmap() {
        NativeImage pixels = this.texture.getPixels();
        pixels.setPixel(0, 0, -1);
        this.texture.upload();
    }

    public GpuTextureView getTextureView() {
        return this.texture.getTextureView();
    }

    @Override
    public void close() {
        this.texture.close();
    }
}

