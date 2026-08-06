/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class GuiBookModelRenderer
extends PictureInPictureRenderer<GuiBookModelRenderState> {
    @Override
    public Class<GuiBookModelRenderState> getRenderStateClass() {
        return GuiBookModelRenderState.class;
    }

    @Override
    protected void renderToTexture(GuiBookModelRenderState bookModelState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(25.0f));
        float open = bookModelState.open();
        poseStack.translate((1.0f - open) * 0.2f, (1.0f - open) * 0.1f, (1.0f - open) * 0.25f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-(1.0f - open) * 90.0f - 90.0f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(180.0f));
        float flip = bookModelState.flip();
        float pageFlip1 = Mth.clamp(Mth.frac(flip + 0.25f) * 1.6f - 0.3f, 0.0f, 1.0f);
        float pageFlip2 = Mth.clamp(Mth.frac(flip + 0.75f) * 1.6f - 0.3f, 0.0f, 1.0f);
        BookModel.State state = BookModel.State.forAnimation(0.0f, pageFlip1, pageFlip2, open);
        submitNodeCollector.submitModel(bookModelState.bookModel(), state, poseStack, bookModelState.texture(), 0xF000F0, OverlayTexture.NO_OVERLAY, 0, null);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return 17 * guiScale;
    }

    @Override
    protected String getTextureLabel() {
        return "book model";
    }
}

