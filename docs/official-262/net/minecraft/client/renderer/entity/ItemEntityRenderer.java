/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class ItemEntityRenderer
extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
    private static final float ITEM_MIN_HOVER_HEIGHT = 0.0625f;
    private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15f;
    private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625f;
    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    public ItemEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15f;
        this.shadowStrength = 0.75f;
    }

    @Override
    public ItemEntityRenderState createRenderState() {
        return new ItemEntityRenderState();
    }

    @Override
    public void extractRenderState(ItemEntity entity, ItemEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bobOffset = entity.bobOffs;
        state.extractItemGroupRenderState(entity, entity.getItem(), this.itemModelResolver);
    }

    @Override
    public void submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        AABB boundingBox = state.item.getModelBoundingBox();
        float minOffsetY = -((float)boundingBox.minY) + 0.0625f;
        float bob = Mth.sin(state.ageInTicks / 10.0f + state.bobOffset) * 0.1f + 0.1f;
        poseStack.translate(0.0f, bob + minOffsetY, 0.0f);
        float spin = ItemEntity.getSpin(state.ageInTicks, state.bobOffset);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotation(spin));
        ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state, this.random, boundingBox);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static void submitMultipleFromCount(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemClusterRenderState state, RandomSource random) {
        ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, lightCoords, state, random, state.item.getModelBoundingBox());
    }

    public static void submitMultipleFromCount(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemClusterRenderState state, RandomSource random, AABB modelBoundingBox) {
        int amount = state.count;
        if (amount == 0) {
            return;
        }
        random.setSeed(state.seed);
        ItemStackRenderState item = state.item;
        float modelDepth = (float)modelBoundingBox.getZsize();
        if (modelDepth > 0.0625f) {
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            for (int i = 1; i < amount; ++i) {
                poseStack.pushPose();
                float xo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                float yo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                float zo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                poseStack.translate(xo, yo, zo);
                item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
        } else {
            float offsetZ = modelDepth * 1.5f;
            poseStack.translate(0.0f, 0.0f, -(offsetZ * (float)(amount - 1) / 2.0f));
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.translate(0.0f, 0.0f, offsetZ);
            for (int i = 1; i < amount; ++i) {
                poseStack.pushPose();
                float xo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f * 0.5f;
                float yo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f * 0.5f;
                poseStack.translate(xo, yo, 0.0f);
                item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
                poseStack.translate(0.0f, 0.0f, offsetZ);
            }
        }
    }

    public static void renderMultipleFromCount(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemClusterRenderState state, RandomSource random) {
        AABB modelBoundingBox = state.item.getModelBoundingBox();
        int amount = state.count;
        if (amount == 0) {
            return;
        }
        random.setSeed(state.seed);
        ItemStackRenderState item = state.item;
        float modelDepth = (float)modelBoundingBox.getZsize();
        if (modelDepth > 0.0625f) {
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            for (int i = 1; i < amount; ++i) {
                poseStack.pushPose();
                float xo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                float yo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                float zo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                poseStack.translate(xo, yo, zo);
                item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
        } else {
            float offsetZ = modelDepth * 1.5f;
            poseStack.translate(0.0f, 0.0f, -(offsetZ * (float)(amount - 1) / 2.0f));
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.translate(0.0f, 0.0f, offsetZ);
            for (int i = 1; i < amount; ++i) {
                poseStack.pushPose();
                float xo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f * 0.5f;
                float yo = (random.nextFloat() * 2.0f - 1.0f) * 0.15f * 0.5f;
                poseStack.translate(xo, yo, 0.0f);
                item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
                poseStack.translate(0.0f, 0.0f, offsetZ);
            }
        }
    }
}

