package com.xgdt.dx12.lod.voxy;

import com.mojang.blaze3d.textures.GpuTexture;
import com.xgdt.dx12.dx12.Dx12GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * HiZ (Hierarchical Z) buffer for occlusion culling.
 * Generates mip chain from depth buffer for hierarchical frustum/occlusion culling.
 */
@Environment(EnvType.CLIENT)
public final class VoxelLodHiZBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-voxy-hiz");

    /** Maximum mip levels for HiZ (2048 -> 1 = 11 levels). */
    public static final int MAX_MIP_LEVELS = 12;
    /** HiZ texture format (R32_FLOAT for depth). */
    private static final int HIZ_FORMAT = 41; // R32_FLOAT

    /** HiZ texture (mipmapped depth pyramid). */
    private Dx12GpuTexture hizTexture;
    /** HiZ texture width. */
    private int width = 0;
    /** HiZ texture height. */
    private int height = 0;
    /** Current mip levels. */
    private int mipLevels = 0;

    /** Pipeline for HiZ generation. */
    private long hizGenPipeline = 0;

    public VoxelLodHiZBuffer() {}

    /**
     * Ensure HiZ buffer matches the source depth texture size.
     */
    public void ensureSize(Dx12GpuTexture depthTexture) {
        int w = depthTexture.getWidth(0);
        int h = depthTexture.getHeight(0);

        if (this.hizTexture != null && this.width == w && this.height == h) {
            return;
        }

        // Calculate mip levels
        int levels = 1;
        int tw = w, th = h;
        while (tw > 1 || th > 1) {
            tw = Math.max(1, tw >> 1);
            th = Math.max(1, th >> 1);
            levels++;
        }
        levels = Math.min(levels, MAX_MIP_LEVELS);

        close();

        this.width = w;
        this.height = h;
        this.mipLevels = levels;

        // Create HiZ texture with mip chain
        this.hizTexture = new Dx12GpuTexture(
            new Dx12GpuTexture.Desc()
                .width(w)
                .height(h)
                .mipLevels(levels)
                .format(HIZ_FORMAT)
                .usage(GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_SAMPLED | GpuTexture.USAGE_STORAGE)
        );

        LOGGER.info("[voxy] HiZ buffer created: {}x{} with {} mip levels", w, h, levels);
    }

    /**
     * Generate HiZ mip chain from depth texture.
     * Uses compute shader to do max reduction (since we use reverse-Z, far = small depth).
     */
    public void generateHiZ(long ctx, Dx12GpuTexture depthTexture) {
        if (this.hizTexture == null || this.hizGenPipeline == 0) {
            return;
        }

        // TODO: Dispatch compute shader to generate HiZ mip chain
        // For now, this is a placeholder - the actual compute dispatch
        // would go here using Dx12Native.dx12DispatchCompute
    }

    /** Get HiZ texture. */
    public Dx12GpuTexture getHiZTexture() {
        return this.hizTexture;
    }

    /** Get mip levels. */
    public int getMipLevels() {
        return this.mipLevels;
    }

    /** Set HiZ generation pipeline. */
    public void setPipeline(long pipeline) {
        this.hizGenPipeline = pipeline;
    }

    /** Close resources. */
    public void close() {
        if (this.hizTexture != null) {
            try { this.hizTexture.close(); } catch (Throwable ignored) {}
            this.hizTexture = null;
        }
        this.width = 0;
        this.height = 0;
        this.mipLevels = 0;
    }
}