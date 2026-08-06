/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class AvatarRenderer<AvatarlikeEntity extends Avatar>
extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    public AvatarRenderer(EntityRendererProvider.Context context, boolean slimSteve) {
        super(context, new PlayerModel(context.bakeLayer(slimSteve ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slimSteve), 0.5f);
        this.addLayer(new HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel>(this, ArmorModelSet.bake(slimSteve ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel((ModelPart)part, slimSteve)), context.getEquipmentRenderer()));
        this.addLayer(new PlayerItemInHandLayer<AvatarRenderState, PlayerModel>(this));
        this.addLayer(new ArrowLayer(this, context));
        this.addLayer(new Deadmau5EarsLayer(this, context.getModelSet()));
        this.addLayer(new CapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
        this.addLayer(new CustomHeadLayer<AvatarRenderState, PlayerModel>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new WingsLayer<AvatarRenderState, PlayerModel>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new ParrotOnShoulderLayer(this, context.getModelSet()));
        this.addLayer(new SpinAttackEffectLayer(this, context.getModelSet()));
        this.addLayer(new BeeStingerLayer(this, context));
    }

    @Override
    protected boolean shouldRenderLayers(AvatarRenderState state) {
        return !state.isSpectator;
    }

    @Override
    public Vec3 getRenderOffset(AvatarRenderState state) {
        Vec3 offset = super.getRenderOffset(state);
        if (state.isCrouching) {
            return offset.add(0.0, (double)(state.scale * -2.0f) / 16.0, 0.0);
        }
        return offset;
    }

    private static HumanoidModel.ArmPose getArmPose(Avatar avatar, HumanoidArm arm) {
        ItemStack mainHandItem = avatar.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = avatar.getItemInHand(InteractionHand.OFF_HAND);
        HumanoidModel.ArmPose mainHandPose = AvatarRenderer.getArmPose(avatar, mainHandItem, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose offHandPose = AvatarRenderer.getArmPose(avatar, offHandItem, InteractionHand.OFF_HAND);
        if (mainHandPose.isTwoHanded()) {
            HumanoidModel.ArmPose armPose = offHandPose = offHandItem.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }
        if (avatar.getMainArm() == arm) {
            return mainHandPose;
        }
        return offHandPose;
    }

    private static HumanoidModel.ArmPose getArmPose(Avatar avatar, ItemStack itemInHand, InteractionHand hand) {
        SwingAnimation attack;
        if (itemInHand.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (!avatar.swinging && itemInHand.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemInHand)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        if (avatar.getUsedItemHand() == hand && avatar.getUseItemRemainingTicks() > 0) {
            ItemUseAnimation anim = itemInHand.getUseAnimation();
            if (anim == ItemUseAnimation.BLOCK) {
                return HumanoidModel.ArmPose.BLOCK;
            }
            if (anim == ItemUseAnimation.BOW) {
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
            if (anim == ItemUseAnimation.TRIDENT) {
                return HumanoidModel.ArmPose.THROW_TRIDENT;
            }
            if (anim == ItemUseAnimation.CROSSBOW) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (anim == ItemUseAnimation.SPYGLASS) {
                return HumanoidModel.ArmPose.SPYGLASS;
            }
            if (anim == ItemUseAnimation.TOOT_HORN) {
                return HumanoidModel.ArmPose.TOOT_HORN;
            }
            if (anim == ItemUseAnimation.BRUSH) {
                return HumanoidModel.ArmPose.BRUSH;
            }
            if (anim == ItemUseAnimation.SPEAR) {
                return HumanoidModel.ArmPose.SPEAR;
            }
        }
        if ((attack = itemInHand.get(DataComponents.SWING_ANIMATION)) != null && attack.type() == SwingAnimationType.STAB && avatar.swinging) {
            return HumanoidModel.ArmPose.SPEAR;
        }
        if (itemInHand.is(ItemTags.SPEARS)) {
            return HumanoidModel.ArmPose.SPEAR;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        float s = 0.9375f;
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
    }

    @Override
    protected void submitNameDisplay(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        int offset = state.showExtraEars ? -10 : 0;
        this.submitNameDisplay(state, poseStack, submitNodeCollector, camera, offset);
        poseStack.popPose();
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(AvatarlikeEntity entity, AvatarRenderState state, float partialTicks) {
        ItemStack useItem;
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.leftArmPose = AvatarRenderer.getArmPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = AvatarRenderer.getArmPose(entity, HumanoidArm.RIGHT);
        state.skin = ((ClientAvatarEntity)entity).getSkin();
        state.arrowCount = ((LivingEntity)entity).getArrowCount();
        state.stingerCount = ((LivingEntity)entity).getStingerCount();
        state.isSpectator = ((Entity)entity).isSpectator();
        state.showHat = ((Avatar)entity).isModelPartShown(PlayerModelPart.HAT);
        state.showJacket = ((Avatar)entity).isModelPartShown(PlayerModelPart.JACKET);
        state.showLeftPants = ((Avatar)entity).isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        state.showRightPants = ((Avatar)entity).isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        state.showLeftSleeve = ((Avatar)entity).isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        state.showRightSleeve = ((Avatar)entity).isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        state.showCape = ((Avatar)entity).isModelPartShown(PlayerModelPart.CAPE);
        this.extractFlightData(entity, state, partialTicks);
        this.extractCapeState(entity, state, partialTicks);
        state.parrotOnLeftShoulder = ((ClientAvatarEntity)entity).getParrotVariantOnShoulder(true);
        state.parrotOnRightShoulder = ((ClientAvatarEntity)entity).getParrotVariantOnShoulder(false);
        state.id = ((Entity)entity).getId();
        state.showExtraEars = ((ClientAvatarEntity)entity).showExtraEars();
        state.heldOnHead.clear();
        if (state.isUsingItem && (useItem = ((LivingEntity)entity).getItemInHand(state.useItemHand)).is(Items.SPYGLASS)) {
            this.itemModelResolver.updateForLiving(state.heldOnHead, useItem, ItemDisplayContext.HEAD, (LivingEntity)entity);
        }
    }

    @Override
    protected boolean shouldShowName(AvatarlikeEntity entity, double distanceToCameraSq) {
        return super.shouldShowName(entity, distanceToCameraSq) && (((LivingEntity)entity).shouldShowName() || ((Entity)entity).hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    private void extractFlightData(AvatarlikeEntity entity, AvatarRenderState state, float partialTicks) {
        state.fallFlyingTimeInTicks = (float)((LivingEntity)entity).getFallFlyingTicks() + partialTicks;
        Vec3 lookAngle = ((Entity)entity).getViewVector(partialTicks);
        Vec3 movement = ((ClientAvatarEntity)entity).avatarState().deltaMovementOnPreviousTick().lerp(((Entity)entity).getDeltaMovement(), partialTicks);
        if (movement.horizontalDistanceSqr() > (double)1.0E-5f && lookAngle.horizontalDistanceSqr() > (double)1.0E-5f) {
            state.shouldApplyFlyingYRot = true;
            double dot = movement.horizontal().normalize().dot(lookAngle.horizontal().normalize());
            double sign = movement.x * lookAngle.z - movement.z * lookAngle.x;
            state.flyingYRot = (float)(Math.signum(sign) * Math.acos(Math.min(1.0, Math.abs(dot))));
        } else {
            state.shouldApplyFlyingYRot = false;
            state.flyingYRot = 0.0f;
        }
    }

    private void extractCapeState(AvatarlikeEntity entity, AvatarRenderState state, float partialTicks) {
        ClientAvatarState clientState = ((ClientAvatarEntity)entity).avatarState();
        double deltaX = clientState.getInterpolatedCloakX(partialTicks) - Mth.lerp((double)partialTicks, ((Avatar)entity).xo, ((Entity)entity).getX());
        double deltaY = clientState.getInterpolatedCloakY(partialTicks) - Mth.lerp((double)partialTicks, ((Avatar)entity).yo, ((Entity)entity).getY());
        double deltaZ = clientState.getInterpolatedCloakZ(partialTicks) - Mth.lerp((double)partialTicks, ((Avatar)entity).zo, ((Entity)entity).getZ());
        float yBodyRot = Mth.rotLerp(partialTicks, ((Avatar)entity).yBodyRotO, ((Avatar)entity).yBodyRot);
        double forwardX = Mth.sin(yBodyRot * ((float)Math.PI / 180));
        double forwardZ = -Mth.cos(yBodyRot * ((float)Math.PI / 180));
        state.capeFlap = (float)deltaY * 10.0f;
        state.capeFlap = Mth.clamp(state.capeFlap, -6.0f, 32.0f);
        state.capeLean = (float)(deltaX * forwardX + deltaZ * forwardZ) * 100.0f;
        state.capeLean *= 1.0f - state.fallFlyingScale();
        state.capeLean = Mth.clamp(state.capeLean, 0.0f, 150.0f);
        state.capeLean2 = (float)(deltaX * forwardZ - deltaZ * forwardX) * 100.0f;
        state.capeLean2 = Mth.clamp(state.capeLean2, -20.0f, 20.0f);
        float pow = clientState.getInterpolatedBob(partialTicks);
        float walkDistance = clientState.getInterpolatedWalkDistance(partialTicks);
        state.capeFlap += Mth.sin(walkDistance * 6.0f) * 32.0f * pow;
    }

    public void renderRightHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Identifier skinTexture, boolean hasSleeve) {
        this.renderHand(poseStack, submitNodeCollector, lightCoords, skinTexture, ((PlayerModel)this.model).rightArm, hasSleeve);
    }

    public void renderLeftHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Identifier skinTexture, boolean hasSleeve) {
        this.renderHand(poseStack, submitNodeCollector, lightCoords, skinTexture, ((PlayerModel)this.model).leftArm, hasSleeve);
    }

    private void renderHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Identifier skinTexture, ModelPart arm, boolean hasSleeve) {
        PlayerModel model = (PlayerModel)this.getModel();
        arm.resetPose();
        arm.visible = true;
        model.leftSleeve.visible = hasSleeve;
        model.rightSleeve.visible = hasSleeve;
        model.leftArm.zRot = -0.1f;
        model.rightArm.zRot = 0.1f;
        submitNodeCollector.submitModelPart(arm, poseStack, RenderTypes.entityTranslucent(skinTexture), lightCoords, OverlayTexture.NO_OVERLAY, null);
    }

    @Override
    protected void setupRotations(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        float swimAmount = state.swimAmount;
        float xRot = state.xRot;
        if (state.isFallFlying) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            float scale = state.fallFlyingScale();
            if (!state.isAutoSpinAttack) {
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(scale * (-90.0f - xRot)));
            }
            if (state.shouldApplyFlyingYRot) {
                poseStack.mulPose((Quaternionfc)Axis.YP.rotation(state.flyingYRot));
            }
        } else if (swimAmount > 0.0f) {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
            float targetXRot = state.isInWater ? -90.0f - xRot : -90.0f;
            float xAngle = Mth.lerp(swimAmount, 0.0f, targetXRot);
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(xAngle));
            if (state.isVisuallySwimming) {
                poseStack.translate(0.0f, -1.0f, 0.3f);
            }
        } else {
            super.setupRotations(state, poseStack, bodyRot, entityScale);
        }
    }

    @Override
    public boolean isEntityUpsideDown(AvatarlikeEntity mob) {
        if (((Avatar)mob).isModelPartShown(PlayerModelPart.CAPE)) {
            if (mob instanceof Player) {
                Player player = (Player)mob;
                return AvatarRenderer.isPlayerUpsideDown(player);
            }
            return super.isEntityUpsideDown(mob);
        }
        return false;
    }

    public static boolean isPlayerUpsideDown(Player player) {
        return AvatarRenderer.isUpsideDownName(player.getGameProfile().name());
    }
}

