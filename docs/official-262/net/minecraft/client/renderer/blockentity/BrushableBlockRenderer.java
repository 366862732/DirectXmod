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
import net.minecraft.client.renderer.blockentity.state.BrushableBlockRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BrushableBlockRenderer
implements BlockEntityRenderer<BrushableBlockEntity, BrushableBlockRenderState> {
    private final ItemModelResolver itemModelResolver;

    public BrushableBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public BrushableBlockRenderState createRenderState() {
        return new BrushableBlockRenderState();
    }

    @Override
    public void extractRenderState(BrushableBlockEntity blockEntity, BrushableBlockRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.hitDirection = blockEntity.getHitDirection();
        state.dustProgress = blockEntity.getBlockState().getValue(BlockStateProperties.DUSTED);
        if (blockEntity.getLevel() != null && blockEntity.getHitDirection() != null) {
            state.lightCoords = LightCoordsUtil.getLightCoords(LightCoordsUtil.BrightnessGetter.DEFAULT, blockEntity.getLevel(), blockEntity.getBlockState(), blockEntity.getBlockPos().relative(blockEntity.getHitDirection()));
        }
        this.itemModelResolver.updateForTopItem(state.itemState, blockEntity.getItem(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
    }

    @Override
    public void submit(BrushableBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.dustProgress <= 0 || state.hitDirection == null || state.itemState.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.5f, 0.0f);
        float[] translations = this.translations(state.hitDirection, state.dustProgress);
        poseStack.translate(translations[0], translations[1], translations[2]);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(75.0f));
        boolean eastWest = state.hitDirection == Direction.EAST || state.hitDirection == Direction.WEST;
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((eastWest ? 90 : 0) + 11));
        poseStack.scale(0.5f, 0.5f, 0.5f);
        state.itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private float[] translations(Direction direction, int completionState) {
        float[] xyzTranslations = new float[]{0.5f, 0.0f, 0.5f};
        float completionOffset = (float)completionState / 10.0f * 0.75f;
        switch (direction) {
            case EAST: {
                xyzTranslations[0] = 0.73f + completionOffset;
                break;
            }
            case WEST: {
                xyzTranslations[0] = 0.25f - completionOffset;
                break;
            }
            case UP: {
                xyzTranslations[1] = 0.25f + completionOffset;
                break;
            }
            case DOWN: {
                xyzTranslations[1] = -0.23f - completionOffset;
                break;
            }
            case NORTH: {
                xyzTranslations[2] = 0.25f - completionOffset;
                break;
            }
            case SOUTH: {
                xyzTranslations[2] = 0.73f + completionOffset;
            }
        }
        return xyzTranslations;
    }
}

