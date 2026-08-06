/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;

@Environment(value=EnvType.CLIENT)
public enum ChunkSectionLayer {
    SOLID(RenderPipelines.SOLID_TERRAIN, 0x400000, false),
    CUTOUT(RenderPipelines.CUTOUT_TERRAIN, 0x400000, false),
    TRANSLUCENT(RenderPipelines.TRANSLUCENT_TERRAIN, 786432, true);

    private final RenderPipeline pipeline;
    private final int bufferSize;
    private final boolean translucent;
    private final String label;

    private ChunkSectionLayer(RenderPipeline pipeline, int bufferSize, boolean translucent) {
        this.pipeline = pipeline;
        this.bufferSize = bufferSize;
        this.translucent = translucent;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public static ChunkSectionLayer byTransparency(Transparency transparency) {
        if (transparency.hasTranslucent()) {
            return TRANSLUCENT;
        }
        if (transparency.hasTransparent()) {
            return CUTOUT;
        }
        return SOLID;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public String label() {
        return this.label;
    }

    public boolean translucent() {
        return this.translucent;
    }

    public VertexFormat vertexFormat() {
        return this.pipeline.getVertexFormatBinding(0);
    }
}

