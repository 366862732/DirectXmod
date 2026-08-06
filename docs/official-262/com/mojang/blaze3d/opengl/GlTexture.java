/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GlTexture
extends GpuTexture
implements FrameBufferAttachment {
    private static final int EMPTY = -1;
    protected final int id;
    private final FrameBufferCache frameBufferCache;
    private final List<FrameBufferCache.CacheKey> fboKeys = new ArrayList<FrameBufferCache.CacheKey>();
    protected boolean closed;
    private int views;

    protected GlTexture(@GpuTexture.Usage int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels, int id, FrameBufferCache frameBufferCache) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.id = id;
        this.frameBufferCache = frameBufferCache;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.views == 0) {
            this.destroyImmediately();
        }
    }

    private void destroyImmediately() {
        while (!this.fboKeys.isEmpty()) {
            this.frameBufferCache.destroyFbo(this.fboKeys.getLast());
        }
        GlStateManager._deleteTexture(this.id);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public int glId() {
        return this.id;
    }

    @Override
    public int fboMipLevel() {
        return 0;
    }

    @Override
    public void addAssociatedFbo(FrameBufferCache.CacheKey fboKey) {
        this.fboKeys.add(fboKey);
    }

    @Override
    public void removeAssociatedFbo(FrameBufferCache.CacheKey fboKey) {
        this.fboKeys.remove(fboKey);
    }

    public void addViews() {
        ++this.views;
    }

    public void removeViews() {
        --this.views;
        if (this.closed && this.views == 0) {
            this.destroyImmediately();
        }
    }
}

