/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class OversizedItemRenderer
extends PictureInPictureRenderer<OversizedItemRenderState> {
    private boolean usedOnThisFrame;
    private @Nullable Object modelOnTextureIdentity;

    public boolean usedOnThisFrame() {
        return this.usedOnThisFrame;
    }

    public void resetUsedOnThisFrame() {
        this.usedOnThisFrame = false;
    }

    public void invalidateTexture() {
        this.modelOnTextureIdentity = null;
    }

    @Override
    public Class<OversizedItemRenderState> getRenderStateClass() {
        return OversizedItemRenderState.class;
    }

    @Override
    protected void renderToTexture(OversizedItemRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        boolean flat;
        poseStack.scale(1.0f, -1.0f, -1.0f);
        GuiItemRenderState guiItemRenderState = renderState.guiItemRenderState();
        ScreenRectangle itemBounds = guiItemRenderState.oversizedItemBounds();
        Objects.requireNonNull(itemBounds);
        float itemBoundsCenterX = (float)(itemBounds.left() + itemBounds.right()) / 2.0f;
        float itemBoundsCenterY = (float)(itemBounds.top() + itemBounds.bottom()) / 2.0f;
        float slotCenterX = (float)guiItemRenderState.x() + 8.0f;
        float slotCenterY = (float)guiItemRenderState.y() + 8.0f;
        poseStack.translate((slotCenterX - itemBoundsCenterX) / 16.0f, (itemBoundsCenterY - slotCenterY) / 16.0f, 0.0f);
        TrackingItemStackRenderState itemStackRenderState = guiItemRenderState.itemStackRenderState();
        boolean bl = flat = !itemStackRenderState.usesBlockLight();
        if (flat) {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        } else {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
        itemStackRenderState.submit(poseStack, submitNodeCollector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        this.modelOnTextureIdentity = itemStackRenderState.getModelIdentity();
    }

    @Override
    public void blitTexture(OversizedItemRenderState renderState, GuiRenderState guiRenderState) {
        super.blitTexture(renderState, guiRenderState);
        this.usedOnThisFrame = true;
    }

    @Override
    public boolean textureIsReadyToBlit(OversizedItemRenderState renderState) {
        TrackingItemStackRenderState itemStackRenderState = renderState.guiItemRenderState().itemStackRenderState();
        return !itemStackRenderState.isAnimated() && itemStackRenderState.getModelIdentity().equals(this.modelOnTextureIdentity);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return (float)height / 2.0f;
    }

    @Override
    protected String getTextureLabel() {
        return "oversized_item";
    }
}

