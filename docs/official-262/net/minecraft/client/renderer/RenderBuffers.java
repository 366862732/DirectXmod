/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.StagedVertexBuffer;

@Environment(value=EnvType.CLIENT)
public class RenderBuffers
implements AutoCloseable {
    private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
    private final SectionBufferBuilderPool sectionBufferPool;
    private final StagedVertexBuffer stagedVertexBuffer;

    public RenderBuffers(int maxSectionBuilders) {
        this.sectionBufferPool = SectionBufferBuilderPool.allocate(maxSectionBuilders);
        this.stagedVertexBuffer = new StagedVertexBuffer(() -> "Shared Buffer", 0x400000);
    }

    public SectionBufferBuilderPack fixedBufferPack() {
        return this.fixedBufferPack;
    }

    public SectionBufferBuilderPool sectionBufferPool() {
        return this.sectionBufferPool;
    }

    public StagedVertexBuffer stagedVertexBuffer() {
        return this.stagedVertexBuffer;
    }

    public void endFrame() {
        this.stagedVertexBuffer.endFrame();
    }

    @Override
    public void close() {
        this.sectionBufferPool.close();
        this.stagedVertexBuffer.close();
    }
}

