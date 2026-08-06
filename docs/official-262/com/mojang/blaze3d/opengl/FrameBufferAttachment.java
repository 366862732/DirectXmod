/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.FrameBufferCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface FrameBufferAttachment {
    public int glId();

    public int fboMipLevel();

    public void addAssociatedFbo(FrameBufferCache.CacheKey var1);

    public void removeAssociatedFbo(FrameBufferCache.CacheKey var1);
}

