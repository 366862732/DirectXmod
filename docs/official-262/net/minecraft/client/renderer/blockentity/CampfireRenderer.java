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
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CampfireRenderer
implements BlockEntityRenderer<CampfireBlockEntity, CampfireRenderState> {
    private static final float SIZE = 0.375f;
    private final ItemModelResolver itemModelResolver;

    public CampfireRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public CampfireRenderState createRenderState() {
        return new CampfireRenderState();
    }

    @Override
    public void extractRenderState(CampfireBlockEntity blockEntity, CampfireRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(CampfireBlock.FACING);
        int seed = (int)blockEntity.getBlockPos().asLong();
        state.items = new ArrayList<ItemStackRenderState>();
        for (int slot = 0; slot < blockEntity.getItems().size(); ++slot) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(itemState, blockEntity.getItems().get(slot), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + slot);
            state.items.add(itemState);
        }
    }

    @Override
    public void submit(CampfireRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Direction facing = state.facing;
        List<ItemStackRenderState> items = state.items;
        for (int slot = 0; slot < items.size(); ++slot) {
            ItemStackRenderState itemState = items.get(slot);
            if (itemState.isEmpty()) continue;
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.44921875f, 0.5f);
            Direction direction = Direction.from2DDataValue((slot + facing.get2DDataValue()) % 4);
            float angle = -direction.toYRot();
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(angle));
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(90.0f));
            poseStack.translate(-0.3125f, -0.3125f, 0.0f);
            poseStack.scale(0.375f, 0.375f, 0.375f);
            itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}

