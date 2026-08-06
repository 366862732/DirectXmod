/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GlTextureView
extends GpuTextureView
implements FrameBufferAttachment {
    private static final int EMPTY = -1;
    private boolean closed;
    private final FrameBufferCache frameBufferCache;
    private final List<FrameBufferCache.CacheKey> fboKeys = new ArrayList<FrameBufferCache.CacheKey>();

    protected GlTextureView(GlTexture texture, int baseMipLevel, int mipLevels, FrameBufferCache frameBufferCache) {
        super(texture, baseMipLevel, mipLevels);
        texture.addViews();
        this.frameBufferCache = frameBufferCache;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.texture().removeViews();
            while (!this.fboKeys.isEmpty()) {
                this.frameBufferCache.destroyFbo(this.fboKeys.getLast());
            }
        }
    }

    @Override
    public GlTexture texture() {
        return (GlTexture)super.texture();
    }

    @Override
    public int glId() {
        return this.texture().id;
    }

    @Override
    public int fboMipLevel() {
        return this.baseMipLevel();
    }

    @Override
    public void addAssociatedFbo(FrameBufferCache.CacheKey fboKey) {
        this.fboKeys.add(fboKey);
    }

    @Override
    public void removeAssociatedFbo(FrameBufferCache.CacheKey fboKey) {
        this.fboKeys.remove(fboKey);
    }
}

