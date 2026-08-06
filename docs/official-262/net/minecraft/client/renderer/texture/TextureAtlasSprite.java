/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 */
package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public class TextureAtlasSprite
implements AutoCloseable {
    private final Identifier atlasLocation;
    private final SpriteContents contents;
    private final int x;
    private final int y;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final int padding;

    protected TextureAtlasSprite(Identifier atlasLocation, SpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, int padding) {
        this.atlasLocation = atlasLocation;
        this.contents = contents;
        this.padding = padding;
        this.x = x;
        this.y = y;
        this.u0 = (float)(x + padding) / (float)atlasWidth;
        this.u1 = (float)(x + padding + contents.width()) / (float)atlasWidth;
        this.v0 = (float)(y + padding) / (float)atlasHeight;
        this.v1 = (float)(y + padding + contents.height()) / (float)atlasHeight;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public float getU0() {
        return this.u0;
    }

    public float getU1() {
        return this.u1;
    }

    public SpriteContents contents() {
        return this.contents;
    }

    public @Nullable SpriteContents.AnimationState createAnimationState(GpuBufferSlice uboSlice, int spriteUboSize) {
        return this.contents.createAnimationState(uboSlice, spriteUboSize);
    }

    public Transparency transparency() {
        return this.contents.transparency();
    }

    public float getU(float offset) {
        float diff = this.u1 - this.u0;
        return this.u0 + diff * offset;
    }

    public float getV0() {
        return this.v0;
    }

    public float getV1() {
        return this.v1;
    }

    public float getV(float offset) {
        float diff = this.v1 - this.v0;
        return this.v0 + diff * offset;
    }

    public Identifier atlasLocation() {
        return this.atlasLocation;
    }

    public String toString() {
        return "TextureAtlasSprite{contents='" + String.valueOf(this.contents) + "', u0=" + this.u0 + ", u1=" + this.u1 + ", v0=" + this.v0 + ", v1=" + this.v1 + "}";
    }

    public void uploadFirstFrame(GpuTexture destination, int level) {
        this.contents.uploadFirstFrame(destination, level);
    }

    public VertexConsumer wrap(VertexConsumer buffer) {
        return new SpriteCoordinateExpander(buffer, this);
    }

    public boolean isAnimated() {
        return this.contents.isAnimated();
    }

    public void uploadSpriteUbo(ByteBuffer uboBuffer, int startOffset, int maxMipLevel, int atlasWidth, int atlasHeight, int spriteUboSize) {
        for (int level = 0; level <= maxMipLevel; ++level) {
            Std140Builder.intoBuffer(MemoryUtil.memSlice((ByteBuffer)uboBuffer, (int)(startOffset + level * spriteUboSize), (int)spriteUboSize)).putMat4f((Matrix4fc)new Matrix4f().ortho2D(0.0f, (float)(atlasWidth >> level), 0.0f, (float)(atlasHeight >> level))).putMat4f((Matrix4fc)new Matrix4f().translate((float)(this.x >> level), (float)(this.y >> level), 0.0f).scale((float)(this.contents.width() + this.padding * 2 >> level), (float)(this.contents.height() + this.padding * 2 >> level), 1.0f)).putFloat((float)this.padding / (float)this.contents.width()).putFloat((float)this.padding / (float)this.contents.height()).putInt(level);
        }
    }

    @Override
    public void close() {
        this.contents.close();
    }
}

