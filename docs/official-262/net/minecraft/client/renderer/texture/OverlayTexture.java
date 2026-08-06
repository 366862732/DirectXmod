/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ARGB;

@Environment(value=EnvType.CLIENT)
public class OverlayTexture
implements AutoCloseable {
    private static final int SIZE = 16;
    public static final int NO_WHITE_U = 0;
    public static final int RED_OVERLAY_V = 3;
    public static final int WHITE_OVERLAY_V = 10;
    public static final int NO_OVERLAY = OverlayTexture.pack(0, 10);
    private final DynamicTexture texture = new DynamicTexture("Entity Color Overlay", 16, 16, false);

    public OverlayTexture() {
        NativeImage pixels = this.texture.getPixels();
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                if (y < 8) {
                    pixels.setPixel(x, y, -1291911168);
                    continue;
                }
                int a = (int)((1.0f - (float)x / 15.0f * 0.75f) * 255.0f);
                pixels.setPixel(x, y, ARGB.white(a));
            }
        }
        this.texture.upload();
    }

    @Override
    public void close() {
        this.texture.close();
    }

    public static int u(float whiteOverlayProgress) {
        return (int)(whiteOverlayProgress * 15.0f);
    }

    public static int v(boolean hurtOverlay) {
        return hurtOverlay ? 3 : 10;
    }

    public static int pack(int u, int v) {
        return u | v << 16;
    }

    public static int pack(float whiteOverlayProgress, boolean redOverlay) {
        return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(redOverlay));
    }

    public GpuTextureView getTextureView() {
        return this.texture.getTextureView();
    }
}

