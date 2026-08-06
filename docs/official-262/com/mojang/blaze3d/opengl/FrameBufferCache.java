/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FrameBufferCache {
    private final Object2IntMap<CacheKey> cache = new Object2IntOpenHashMap();

    public int getFbo(DirectStateAccess dsa, List<@Nullable FrameBufferAttachment> colorTextures, @Nullable FrameBufferAttachment depthTexture) {
        CacheKey cacheKey = new CacheKey(colorTextures, depthTexture);
        return this.cache.computeIfAbsent((Object)cacheKey, object -> this.createFbo(cacheKey, dsa, colorTextures, depthTexture));
    }

    private int createFbo(CacheKey key, DirectStateAccess dsa, List<@Nullable FrameBufferAttachment> colorAttachments, @Nullable FrameBufferAttachment depthAttachment) {
        int fbo = dsa.createFrameBufferObject();
        int colorAttachmentCount = colorAttachments.size();
        int[] colorIds = new int[colorAttachmentCount];
        int[] mipLevels = new int[colorAttachmentCount];
        for (int i = 0; i < colorAttachmentCount; ++i) {
            FrameBufferAttachment attachment = colorAttachments.get(i);
            if (attachment != null) {
                colorIds[i] = attachment.glId();
                mipLevels[i] = attachment.fboMipLevel();
                attachment.addAssociatedFbo(key);
                continue;
            }
            colorIds[i] = 0;
            mipLevels[i] = 0;
        }
        if (depthAttachment != null) {
            depthAttachment.addAssociatedFbo(key);
        }
        dsa.bindFrameBufferTextures(fbo, colorIds, mipLevels, depthAttachment == null ? 0 : depthAttachment.glId(), depthAttachment == null ? 0 : depthAttachment.fboMipLevel(), 0);
        return fbo;
    }

    public void destroyFbo(CacheKey key) {
        if (!this.cache.containsKey((Object)key)) {
            return;
        }
        for (FrameBufferAttachment associatedAttachment : key.associatedAttachments) {
            if (associatedAttachment == null) continue;
            associatedAttachment.removeAssociatedFbo(key);
        }
        int fboId = this.cache.removeInt((Object)key);
        GlStateManager._glDeleteFramebuffers(fboId);
    }

    @Environment(value=EnvType.CLIENT)
    public static class CacheKey {
        private final int[] data;
        private final int hash;
        public final List<@Nullable FrameBufferAttachment> associatedAttachments;

        public CacheKey(List<@Nullable FrameBufferAttachment> colorAttachments, @Nullable FrameBufferAttachment depthAttachment) {
            int colorAttachmentCount = colorAttachments.size();
            this.data = new int[(colorAttachmentCount + (depthAttachment != null ? 1 : 0)) * 2];
            for (int i = 0; i < colorAttachmentCount; ++i) {
                FrameBufferAttachment attachment = colorAttachments.get(i);
                if (attachment != null) {
                    this.data[i * 2] = attachment.glId();
                    this.data[i * 2 + 1] = attachment.fboMipLevel();
                    continue;
                }
                this.data[i * 2] = 0;
                this.data[i * 2 + 1] = 0;
            }
            this.associatedAttachments = new ArrayList<FrameBufferAttachment>(colorAttachments);
            if (depthAttachment != null) {
                this.data[colorAttachmentCount * 2] = depthAttachment.glId();
                this.data[colorAttachmentCount * 2 + 1] = depthAttachment.fboMipLevel();
                this.associatedAttachments.add(depthAttachment);
            }
            this.hash = Arrays.hashCode(this.data);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey)obj;
            if (this.hash != other.hash) {
                return false;
            }
            return Arrays.equals(this.data, other.data);
        }
    }
}

