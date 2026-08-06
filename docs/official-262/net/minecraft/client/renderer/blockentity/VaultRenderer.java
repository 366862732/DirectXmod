/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.VaultRenderState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class VaultRenderer
implements BlockEntityRenderer<VaultBlockEntity, VaultRenderState> {
    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    public VaultRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public VaultRenderState createRenderState() {
        return new VaultRenderState();
    }

    @Override
    public void extractRenderState(VaultBlockEntity blockEntity, VaultRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        ItemStack displayItem = blockEntity.getSharedData().getDisplayItem();
        if (!VaultBlockEntity.Client.shouldDisplayActiveEffects(blockEntity.getSharedData()) || displayItem.isEmpty() || blockEntity.getLevel() == null) {
            return;
        }
        state.displayItem = new ItemClusterRenderState();
        this.itemModelResolver.updateForTopItem(state.displayItem.item, displayItem, ItemDisplayContext.GROUND, blockEntity.getLevel(), null, 0);
        state.displayItem.count = ItemClusterRenderState.getRenderedAmount(displayItem.getCount());
        state.displayItem.seed = ItemClusterRenderState.getSeedForItemStack(displayItem);
        VaultClientData clientData = blockEntity.getClientData();
        state.spin = Mth.rotLerp(partialTicks, clientData.previousSpin(), clientData.currentSpin());
    }

    @Override
    public void submit(VaultRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.displayItem == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.4f, 0.5f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(state.spin));
        ItemEntityRenderer.renderMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state.displayItem, this.random);
        poseStack.popPose();
    }
}

