/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@Environment(value=EnvType.CLIENT)
public enum ChunkSectionLayerGroup {
    OPAQUE(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT),
    TRANSLUCENT(ChunkSectionLayer.TRANSLUCENT);

    private final String label;
    private final ChunkSectionLayer[] layers;

    private ChunkSectionLayerGroup(ChunkSectionLayer ... layers) {
        this.layers = layers;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return this.label;
    }

    public ChunkSectionLayer[] layers() {
        return this.layers;
    }

    public RenderTarget outputTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget renderTarget = switch (this.ordinal()) {
            case 1 -> minecraft.levelRenderer.translucentTarget();
            default -> minecraft.gameRenderer.mainRenderTarget();
        };
        return renderTarget != null ? renderTarget : minecraft.gameRenderer.mainRenderTarget();
    }
}

