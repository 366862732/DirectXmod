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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;

@Environment(value=EnvType.CLIENT)
public class TntMinecartRenderer
extends AbstractMinecartRenderer<MinecartTNT, MinecartTntRenderState> {
    public TntMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.TNT_MINECART);
    }

    @Override
    protected void submitMinecartContents(MinecartTntRenderState state, BlockModelRenderState blockModel, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        float fuse = state.fuseRemainingInTicks;
        if (fuse > -1.0f && fuse < 10.0f) {
            float swell = TntRenderer.getSwellAmount(fuse);
            poseStack.translate((double)(-swell) * 0.5, 0.0, (double)(-swell) * 0.5);
            float scale = 1.0f + swell;
            poseStack.scale(scale, scale, scale);
        }
        TntMinecartRenderer.submitWhiteSolidBlock(blockModel, poseStack, submitNodeCollector, lightCoords, TntRenderer.isLit(fuse), state.outlineColor);
    }

    public static void submitWhiteSolidBlock(BlockModelRenderState blockModel, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, boolean white, int outlineColor) {
        int overlayCoords = white ? OverlayTexture.pack(OverlayTexture.u(1.0f), 10) : OverlayTexture.NO_OVERLAY;
        blockModel.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public MinecartTntRenderState createRenderState() {
        return new MinecartTntRenderState();
    }

    @Override
    public void extractRenderState(MinecartTNT entity, MinecartTntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fuseRemainingInTicks = entity.getFuse() > -1 ? (float)entity.getFuse() - partialTicks + 1.0f : -1.0f;
    }
}

