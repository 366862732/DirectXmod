/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class CustomHeadLayer<S extends LivingEntityRenderState, M extends EntityModel<S>>
extends RenderLayer<S, M> {
    private static final float ITEM_SCALE = 0.625f;
    private static final float SKULL_SCALE = 1.1875f;
    private final Transforms transforms;
    private final Function<SkullBlock.Type, SkullModelBase> skullModels;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public CustomHeadLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet, PlayerSkinRenderCache playerSkinRenderCache) {
        this(renderer, modelSet, playerSkinRenderCache, Transforms.DEFAULT);
    }

    public CustomHeadLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet, PlayerSkinRenderCache playerSkinRenderCache, Transforms transforms) {
        super(renderer);
        this.transforms = transforms;
        this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(modelSet, type));
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (((LivingEntityRenderState)state).headItem.isEmpty() && ((LivingEntityRenderState)state).wornHeadType == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(this.transforms.horizontalScale(), 1.0f, this.transforms.horizontalScale());
        Object parentModel = this.getParentModel();
        ((Model)parentModel).root().translateAndRotate(poseStack);
        ((HeadedModel)parentModel).translateToHead(poseStack);
        if (((LivingEntityRenderState)state).wornHeadType != null) {
            poseStack.translate(0.0f, this.transforms.skullYOffset(), 0.0f);
            poseStack.scale(1.1875f, 1.1875f, 1.1875f);
            SkullBlock.Type type = ((LivingEntityRenderState)state).wornHeadType;
            SkullModelBase skullModel = this.skullModels.apply(type);
            RenderType renderType = this.resolveSkullRenderType((LivingEntityRenderState)state, type);
            SkullBlockRenderer.submitSkull(((LivingEntityRenderState)state).wornHeadAnimationPos, poseStack, submitNodeCollector, lightCoords, skullModel, renderType, ((LivingEntityRenderState)state).outlineColor, null);
        } else {
            CustomHeadLayer.translateToHead(poseStack, this.transforms);
            ((LivingEntityRenderState)state).headItem.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, ((LivingEntityRenderState)state).outlineColor);
        }
        poseStack.popPose();
    }

    private RenderType resolveSkullRenderType(LivingEntityRenderState state, SkullBlock.Type type) {
        ResolvableProfile profile;
        if (type == SkullBlock.Types.PLAYER && (profile = state.wornHeadProfile) != null) {
            return this.playerSkinRenderCache.getOrDefault(profile).renderType();
        }
        return SkullBlockRenderer.getSkullRenderType(type, null);
    }

    public static void translateToHead(PoseStack poseStack, Transforms transforms) {
        poseStack.translate(0.0f, -0.25f + transforms.yOffset(), 0.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(0.625f, -0.625f, -0.625f);
    }

    @Environment(value=EnvType.CLIENT)
    public record Transforms(float yOffset, float skullYOffset, float horizontalScale) {
        public static final Transforms DEFAULT = new Transforms(0.0f, 0.0f, 1.0f);
    }
}

