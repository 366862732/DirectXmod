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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class GuiSkinRenderer
extends PictureInPictureRenderer<GuiSkinRenderState> {
    @Override
    public Class<GuiSkinRenderState> getRenderStateClass() {
        return GuiSkinRenderState.class;
    }

    @Override
    protected void renderToTexture(GuiSkinRenderState skinState, PoseStack modelStack, SubmitNodeCollector submitNodeCollector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.PLAYER_SKIN);
        int guiScale = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.guiScale;
        float scale = skinState.scale() * (float)guiScale;
        RenderSystem.getModelViewStack().rotateAround((Quaternionfc)Axis.XP.rotationDegrees(skinState.rotationX()), 0.0f, scale * -skinState.pivotY(), 0.0f);
        modelStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-skinState.rotationY()));
        modelStack.translate(0.0f, -1.6010001f, 0.0f);
        submitNodeCollector.submitModel(skinState.playerModel(), Unit.INSTANCE, modelStack, skinState.texture(), 0xF000F0, OverlayTexture.NO_OVERLAY, 0, null);
    }

    @Override
    protected String getTextureLabel() {
        return "player skin";
    }
}

