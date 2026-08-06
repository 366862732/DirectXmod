/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class TheEndPortalRenderer
extends AbstractEndPortalRenderer<TheEndPortalBlockEntity, EndPortalRenderState> {
    private static final float BOTTOM = 0.375f;
    private static final float TOP = 0.75f;
    public static final Transformation TRANSFORMATION = new Transformation((Vector3fc)new Vector3f(0.0f, 0.375f, 0.0f), null, (Vector3fc)new Vector3f(1.0f, 0.375f, 1.0f), null);

    @Override
    public EndPortalRenderState createRenderState() {
        return new EndPortalRenderState();
    }

    @Override
    public void submit(EndPortalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(TRANSFORMATION);
        TheEndPortalRenderer.submitCube(state.facesToShow, RenderTypes.endPortal(), poseStack, submitNodeCollector);
        poseStack.popPose();
    }
}

