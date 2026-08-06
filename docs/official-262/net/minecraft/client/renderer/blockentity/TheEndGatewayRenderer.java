/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.state.EndGatewayRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TheEndGatewayRenderer
extends AbstractEndPortalRenderer<TheEndGatewayBlockEntity, EndGatewayRenderState> {
    private static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_portal/end_gateway_beam.png");

    @Override
    public EndGatewayRenderState createRenderState() {
        return new EndGatewayRenderState();
    }

    @Override
    public void extractRenderState(TheEndGatewayBlockEntity blockEntity, EndGatewayRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        Level level = blockEntity.getLevel();
        if (blockEntity.isSpawning() || blockEntity.isCoolingDown() && level != null) {
            state.scale = blockEntity.isSpawning() ? blockEntity.getSpawnPercent(partialTicks) : blockEntity.getCooldownPercent(partialTicks);
            double beamDistance = blockEntity.isSpawning() ? (double)blockEntity.getLevel().getMaxY() : 50.0;
            state.scale = Mth.sin(state.scale * (float)Math.PI);
            state.height = Mth.floor((double)state.scale * beamDistance);
            state.color = blockEntity.isSpawning() ? DyeColor.MAGENTA.getTextureDiffuseColor() : DyeColor.PURPLE.getTextureDiffuseColor();
            state.animationTime = blockEntity.getLevel() != null ? (float)Math.floorMod(blockEntity.getLevel().getGameTime(), 40) + partialTicks : 0.0f;
        } else {
            state.height = 0;
        }
    }

    @Override
    public void submit(EndGatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.height > 0) {
            BeaconRenderer.submitBeaconBeam(poseStack, submitNodeCollector, BEAM_LOCATION, state.scale, state.animationTime, -state.height, state.height * 2, state.color, 0.15f, 0.175f);
        }
        TheEndGatewayRenderer.submitCube(state.facesToShow, RenderTypes.endGateway(), poseStack, submitNodeCollector);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}

