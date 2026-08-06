/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class EndCrystalRenderer
extends EntityRenderer<EndCrystal, EndCrystalRenderState> {
    private static final Identifier END_CRYSTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private final EndCrystalModel model;

    public EndCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.model = new EndCrystalModel(context.bakeLayer(ModelLayers.END_CRYSTAL));
    }

    @Override
    public void submit(EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 2.0f);
        poseStack.translate(0.0f, -0.5f, 0.0f);
        submitNodeCollector.submitModel(this.model, state, poseStack, END_CRYSTAL_LOCATION, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        Vec3 beamOffset = state.beamOffset;
        if (beamOffset != null) {
            float crystalY = EndCrystalRenderer.getY(state.ageInTicks);
            float deltaX = (float)beamOffset.x;
            float deltaY = (float)beamOffset.y;
            float deltaZ = (float)beamOffset.z;
            poseStack.translate(beamOffset);
            EnderDragonRenderer.submitCrystalBeams(-deltaX, -deltaY + crystalY, -deltaZ, state.ageInTicks, poseStack, submitNodeCollector, state.lightCoords);
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static float getY(float timeInTicks) {
        float hh = Mth.sin(timeInTicks * 0.2f) / 2.0f + 0.5f;
        hh = (hh * hh + hh) * 0.4f;
        return hh - 1.4f;
    }

    @Override
    public EndCrystalRenderState createRenderState() {
        return new EndCrystalRenderState();
    }

    @Override
    public void extractRenderState(EndCrystal entity, EndCrystalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = (float)entity.time + partialTicks;
        state.showsBottom = entity.showsBottom();
        BlockPos beamTarget = entity.getBeamTarget();
        state.beamOffset = beamTarget != null ? Vec3.atCenterOf(beamTarget).subtract(entity.getPosition(partialTicks)) : null;
    }

    @Override
    public boolean shouldRender(EndCrystal entity, Frustum culler, double camX, double camY, double camZ) {
        return super.shouldRender(entity, culler, camX, camY, camZ) || entity.getBeamTarget() != null;
    }
}

