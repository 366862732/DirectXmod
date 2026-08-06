/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GuiItemAtlas
implements AutoCloseable {
    private static final int MINIMUM_TEXTURE_SIZE = 512;
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
    private final FeatureRenderDispatcher featureRenderDispatcher;
    private final int textureSize;
    private final int slotTextureSize;
    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private final GpuTexture depthTexture;
    private final GpuTextureView depthTextureView;
    private final DynamicAtlasAllocator<Object> allocator;
    private final PoseStack poseStack = new PoseStack();
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer("items");

    public GuiItemAtlas(FeatureRenderDispatcher featureRenderDispatcher, int textureSize, int slotTextureSize) {
        this.featureRenderDispatcher = featureRenderDispatcher;
        int storageSize = textureSize / slotTextureSize;
        this.textureSize = textureSize;
        this.slotTextureSize = slotTextureSize;
        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture("UI items atlas", 13, GpuFormat.RGBA8_UNORM, textureSize, textureSize, 1, 1);
        this.textureView = device.createTextureView(this.texture);
        this.depthTexture = device.createTexture("UI items atlas depth", 9, GpuFormat.D32_FLOAT, textureSize, textureSize, 1, 1);
        this.depthTextureView = device.createTextureView(this.depthTexture);
        this.allocator = new DynamicAtlasAllocator(storageSize, storageSize);
        device.createCommandEncoder().clearColorAndDepthTextures(this.texture, GuiRenderer.CLEAR_COLOR, this.depthTexture, 0.0);
    }

    public static int computeTextureSizeFor(int slotTextureSize, int requiredSlotCount) {
        int preferredSlotCount = requiredSlotCount + requiredSlotCount / 2;
        int atlasSize = Mth.smallestSquareSide(preferredSlotCount);
        return Math.clamp((long)Mth.smallestEncompassingPowerOfTwo(atlasSize * slotTextureSize), 512, RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSizeForFormat(GpuFormat.RGBA8_UNORM));
    }

    public void endFrame() {
        this.allocator.endFrame();
    }

    public boolean tryPrepareFor(Set<Object> items) {
        if (this.allocator.hasSpaceForAll(items)) {
            return true;
        }
        return this.allocator.reclaimSpaceFor(items);
    }

    public @Nullable SlotView getOrUpdate(TrackingItemStackRenderState item) {
        DynamicAtlasAllocator.Slot slot = this.allocator.getOrAllocate(item.getModelIdentity(), item.isAnimated());
        if (slot == null) {
            return null;
        }
        switch (slot.state()) {
            case EMPTY: {
                this.drawToSlot(slot.x(), slot.y(), false, item);
                break;
            }
            case STALE: {
                this.drawToSlot(slot.x(), slot.y(), true, item);
                break;
            }
        }
        float slotUvSize = (float)this.slotTextureSize / (float)this.textureSize;
        float u0 = (float)slot.x() * slotUvSize;
        float v0 = 1.0f - (float)slot.y() * slotUvSize;
        return new SlotView(this.textureView, u0, v0, u0 + slotUvSize, v0 - slotUvSize);
    }

    private void drawToSlot(int slotX, int slotY, boolean clear, ItemStackRenderState item) {
        int left = slotX * this.slotTextureSize;
        int top = slotY * this.slotTextureSize;
        int bottom = top + this.slotTextureSize;
        GpuDevice device = RenderSystem.getDevice();
        if (clear) {
            device.createCommandEncoder().clearColorAndDepthTextures(this.texture, GuiRenderer.CLEAR_COLOR, this.depthTexture, 0.0, left, this.textureSize - bottom, this.slotTextureSize, this.slotTextureSize);
        }
        this.poseStack.pushPose();
        this.poseStack.translate((float)left + (float)this.slotTextureSize / 2.0f, (float)top + (float)this.slotTextureSize / 2.0f, 0.0f);
        this.poseStack.scale(this.slotTextureSize, -this.slotTextureSize, this.slotTextureSize);
        RenderSystem.outputColorTextureOverride = this.textureView;
        RenderSystem.outputDepthTextureOverride = this.depthTextureView;
        this.projection.setupOrtho(-1000.0f, 1000.0f, this.textureSize, this.textureSize, true);
        RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(left, this.textureSize - bottom, this.slotTextureSize, this.slotTextureSize);
        Lighting.Entry lighting = item.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT;
        Minecraft.getInstance().gameRenderer.lighting().setupFor(lighting);
        item.submit(this.poseStack, this.submitNodeStorage, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        this.featureRenderDispatcher.renderAllFeatures(this.submitNodeStorage);
        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        this.poseStack.popPose();
    }

    public int textureSize() {
        return this.textureSize;
    }

    @Override
    public void close() {
        this.texture.close();
        this.textureView.close();
        this.depthTexture.close();
        this.depthTextureView.close();
        this.projectionMatrixBuffer.close();
    }

    @Environment(value=EnvType.CLIENT)
    public record SlotView(GpuTextureView textureView, float u0, float v0, float u1, float v1) {
    }
}

