/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.base.MoreObjects
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class ItemInHandRenderer {
    private static final RenderType MAP_BACKGROUND = RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png"));
    private static final float ITEM_SWING_X_POS_SCALE = -0.4f;
    private static final float ITEM_SWING_Y_POS_SCALE = 0.2f;
    private static final float ITEM_SWING_Z_POS_SCALE = -0.2f;
    private static final float ITEM_HEIGHT_SCALE = -0.6f;
    private static final float ITEM_POS_X = 0.56f;
    private static final float ITEM_POS_Y = -0.52f;
    private static final float ITEM_POS_Z = -0.72f;
    private static final float ITEM_PRESWING_ROT_Y = 45.0f;
    private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0f;
    private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0f;
    private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0f;
    private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0f;
    private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0f;
    private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0f;
    private static final float EAT_JIGGLE_X_POS_SCALE = 0.6f;
    private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5f;
    private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0f;
    private static final double EAT_JIGGLE_EXPONENT = 27.0;
    private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8f;
    private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1f;
    private static final float ARM_SWING_X_POS_SCALE = -0.3f;
    private static final float ARM_SWING_Y_POS_SCALE = 0.4f;
    private static final float ARM_SWING_Z_POS_SCALE = -0.4f;
    private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0f;
    private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0f;
    private static final float ARM_HEIGHT_SCALE = -0.6f;
    private static final float ARM_POS_SCALE = 0.8f;
    private static final float ARM_POS_X = 0.8f;
    private static final float ARM_POS_Y = -0.75f;
    private static final float ARM_POS_Z = -0.9f;
    private static final float ARM_PRESWING_ROT_Y = 45.0f;
    private static final float ARM_PREROTATION_X_OFFSET = -1.0f;
    private static final float ARM_PREROTATION_Y_OFFSET = 3.6f;
    private static final float ARM_PREROTATION_Z_OFFSET = 3.5f;
    private static final float ARM_POSTROTATION_X_OFFSET = 5.6f;
    private static final int ARM_ROT_X = 200;
    private static final int ARM_ROT_Y = -135;
    private static final int ARM_ROT_Z = 120;
    private static final float MAP_SWING_X_POS_SCALE = -0.4f;
    private static final float MAP_SWING_Z_POS_SCALE = -0.2f;
    private static final float MAP_HANDS_POS_X = 0.0f;
    private static final float MAP_HANDS_POS_Y = 0.04f;
    private static final float MAP_HANDS_POS_Z = -0.72f;
    private static final float MAP_HANDS_HEIGHT_SCALE = -1.2f;
    private static final float MAP_HANDS_TILT_SCALE = -0.5f;
    private static final float MAP_PLAYER_PITCH_SCALE = 45.0f;
    private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0f;
    private static final float MAPHAND_X_ROT_AMOUNT = 45.0f;
    private static final float MAPHAND_Y_ROT_AMOUNT = 92.0f;
    private static final float MAPHAND_Z_ROT_AMOUNT = -41.0f;
    private static final float MAP_HAND_X_POS = 0.3f;
    private static final float MAP_HAND_Y_POS = -1.1f;
    private static final float MAP_HAND_Z_POS = 0.45f;
    private static final float MAP_SWING_X_ROT_AMOUNT = 20.0f;
    private static final float MAP_PRE_ROT_SCALE = 0.38f;
    private static final float MAP_GLOBAL_X_POS = -0.5f;
    private static final float MAP_GLOBAL_Y_POS = -0.5f;
    private static final float MAP_GLOBAL_Z_POS = 0.0f;
    private static final float MAP_FINAL_SCALE = 0.0078125f;
    private static final int MAP_BORDER = 7;
    private static final int MAP_HEIGHT = 128;
    private static final int MAP_WIDTH = 128;
    private static final float BOW_CHARGE_X_POS_SCALE = 0.0f;
    private static final float BOW_CHARGE_Y_POS_SCALE = 0.0f;
    private static final float BOW_CHARGE_Z_POS_SCALE = 0.04f;
    private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0f;
    private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004f;
    private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0f;
    private static final float BOW_CHARGE_Z_SCALE = 0.2f;
    private static final float BOW_MIN_SHAKE_CHARGE = 0.1f;
    private final Minecraft minecraft;
    private final MapRenderState mapRenderState = new MapRenderState();
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private float mainHandHeight;
    private float oMainHandHeight;
    private float offHandHeight;
    private float oOffHandHeight;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemModelResolver itemModelResolver;

    public ItemInHandRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, ItemModelResolver itemModelResolver) {
        this.minecraft = minecraft;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.itemModelResolver = itemModelResolver;
    }

    public void renderItem(LivingEntity mob, ItemStack itemStack, ItemDisplayContext type, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        if (itemStack.isEmpty()) {
            return;
        }
        ItemStackRenderState renderState = new ItemStackRenderState();
        this.itemModelResolver.updateForTopItem(renderState, itemStack, type, mob.level(), mob, mob.getId() + type.ordinal());
        renderState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

    private float calculateMapTilt(float xRot) {
        float tilt = 1.0f - xRot / 45.0f + 0.1f;
        tilt = Mth.clamp(tilt, 0.0f, 1.0f);
        tilt = -Mth.cos(tilt * (float)Math.PI) * 0.5f + 0.5f;
        return tilt;
    }

    private void renderMapHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidArm arm) {
        AvatarRenderer<AbstractClientPlayer> avatarRenderer = this.entityRenderDispatcher.getPlayerRenderer(this.minecraft.player);
        poseStack.pushPose();
        float invert = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(92.0f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(45.0f));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(invert * -41.0f));
        poseStack.translate(invert * 0.3f, -1.1f, 0.45f);
        Identifier skinTexture = this.minecraft.player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            avatarRenderer.renderRightHand(poseStack, submitNodeCollector, lightCoords, skinTexture, this.minecraft.player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            avatarRenderer.renderLeftHand(poseStack, submitNodeCollector, lightCoords, skinTexture, this.minecraft.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
        poseStack.popPose();
    }

    private void renderOneHandedMap(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, HumanoidArm arm, float attackValue, ItemStack map) {
        float invert = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        poseStack.translate(invert * 0.125f, -0.125f, 0.0f);
        if (!this.minecraft.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(invert * 10.0f));
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, attackValue, arm);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(invert * 0.51f, -0.08f + inverseArmHeight * -1.2f, -0.75f);
        float sqrtAttackValue = Mth.sqrt(attackValue);
        float xSwing = Mth.sin(sqrtAttackValue * (float)Math.PI);
        float xSwingPosition = -0.5f * xSwing;
        float ySwingPosition = 0.4f * Mth.sin(sqrtAttackValue * ((float)Math.PI * 2));
        float zSwingPosition = -0.3f * Mth.sin(attackValue * (float)Math.PI);
        poseStack.translate(invert * xSwingPosition, ySwingPosition - 0.3f * xSwing, zSwingPosition);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(xSwing * -45.0f));
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(invert * xSwing * -30.0f));
        this.renderMap(poseStack, submitNodeCollector, lightCoords, map);
        poseStack.popPose();
    }

    private void renderTwoHandedMap(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float xRot, float inverseArmHeight, float attackValue) {
        float sqrtAttackValue = Mth.sqrt(attackValue);
        float ySwingPosition = -0.2f * Mth.sin(attackValue * (float)Math.PI);
        float zSwingPosition = -0.4f * Mth.sin(sqrtAttackValue * (float)Math.PI);
        poseStack.translate(0.0f, -ySwingPosition / 2.0f, zSwingPosition);
        float mapTilt = this.calculateMapTilt(xRot);
        poseStack.translate(0.0f, 0.04f + inverseArmHeight * -1.2f + mapTilt * -0.5f, -0.72f);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(mapTilt * -85.0f));
        if (!this.minecraft.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(90.0f));
            this.renderMapHand(poseStack, submitNodeCollector, lightCoords, HumanoidArm.RIGHT);
            this.renderMapHand(poseStack, submitNodeCollector, lightCoords, HumanoidArm.LEFT);
            poseStack.popPose();
        }
        float xzSwingRotation = Mth.sin(sqrtAttackValue * (float)Math.PI);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(xzSwingRotation * 20.0f));
        poseStack.scale(2.0f, 2.0f, 2.0f);
        this.renderMap(poseStack, submitNodeCollector, lightCoords, this.mainHandItem);
    }

    private void renderMap(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemStack itemStack) {
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(180.0f));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5f, -0.5f, 0.0f);
        poseStack.scale(0.0078125f, 0.0078125f, 0.0078125f);
        MapId id = itemStack.get(DataComponents.MAP_ID);
        MapItemSavedData data = MapItem.getSavedData(id, (Level)this.minecraft.level);
        RenderType renderType = data == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            buffer.addVertex(pose, -7.0f, 135.0f, 0.0f).setColor(-1).setUv(0.0f, 1.0f).setLight(lightCoords);
            buffer.addVertex(pose, 135.0f, 135.0f, 0.0f).setColor(-1).setUv(1.0f, 1.0f).setLight(lightCoords);
            buffer.addVertex(pose, 135.0f, -7.0f, 0.0f).setColor(-1).setUv(1.0f, 0.0f).setLight(lightCoords);
            buffer.addVertex(pose, -7.0f, -7.0f, 0.0f).setColor(-1).setUv(0.0f, 0.0f).setLight(lightCoords);
        });
        if (data != null) {
            MapRenderer mapRenderer = this.minecraft.getMapRenderer();
            mapRenderer.extractRenderState(id, data, this.mapRenderState);
            mapRenderer.render(this.mapRenderState, poseStack, submitNodeCollector, false, lightCoords);
        }
    }

    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm) {
        boolean isRightArm = arm != HumanoidArm.LEFT;
        float invert = isRightArm ? 1.0f : -1.0f;
        float sqrtAttackValue = Mth.sqrt(attackValue);
        float xSwingPosition = -0.3f * Mth.sin(sqrtAttackValue * (float)Math.PI);
        float ySwingPosition = 0.4f * Mth.sin(sqrtAttackValue * ((float)Math.PI * 2));
        float zSwingPosition = -0.4f * Mth.sin(attackValue * (float)Math.PI);
        poseStack.translate(invert * (xSwingPosition + 0.64000005f), ySwingPosition + -0.6f + inverseArmHeight * -0.6f, zSwingPosition + -0.71999997f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(invert * 45.0f));
        float zSwingRotation = Mth.sin(attackValue * attackValue * (float)Math.PI);
        float ySwingRotation = Mth.sin(sqrtAttackValue * (float)Math.PI);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(invert * ySwingRotation * 70.0f));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(invert * zSwingRotation * -20.0f));
        LocalPlayer player = this.minecraft.player;
        poseStack.translate(invert * -1.0f, 3.6f, 3.5f);
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(invert * 120.0f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(200.0f));
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(invert * -135.0f));
        poseStack.translate(invert * 5.6f, 0.0f, 0.0f);
        AvatarRenderer<AbstractClientPlayer> avatarRenderer = this.entityRenderDispatcher.getPlayerRenderer(player);
        Identifier skinTexture = player.getSkin().body().texturePath();
        if (isRightArm) {
            avatarRenderer.renderRightHand(poseStack, submitNodeCollector, lightCoords, skinTexture, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            avatarRenderer.renderLeftHand(poseStack, submitNodeCollector, lightCoords, skinTexture, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
    }

    private void applyEatTransform(PoseStack poseStack, float frameInterp, HumanoidArm arm, ItemStack itemStack, Player player) {
        float currUsageTime = (float)player.getUseItemRemainingTicks() - frameInterp + 1.0f;
        float scaledUsageTime = currUsageTime / (float)itemStack.getUseDuration(player);
        if (scaledUsageTime < 0.8f) {
            float extraHeightOffset = Mth.abs(Mth.cos(currUsageTime / 4.0f * (float)Math.PI) * 0.1f);
            poseStack.translate(0.0f, extraHeightOffset, 0.0f);
        }
        float eatJiggle = 1.0f - (float)Math.pow(scaledUsageTime, 27.0);
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(eatJiggle * 0.6f * (float)invert, eatJiggle * -0.5f, eatJiggle * 0.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * eatJiggle * 90.0f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(eatJiggle * 10.0f));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * eatJiggle * 30.0f));
    }

    private void applyBrushTransform(PoseStack poseStack, float frameInterp, HumanoidArm arm, Player player) {
        float brushAnimationRemainingTicks = player.getUseItemRemainingTicks() % 10;
        float deltaSinceLastUpdate = brushAnimationRemainingTicks - frameInterp + 1.0f;
        float scaledUsageTime = 1.0f - deltaSinceLastUpdate / 10.0f;
        float minSwipeAngle = -90.0f;
        float maxSwipeAngle = 60.0f;
        float swipeRange = 150.0f;
        float swipeCenter = -15.0f;
        int swipeSpeed = 2;
        float currentSwipeAngle = -15.0f + 75.0f * Mth.cos(scaledUsageTime * 2.0f * (float)Math.PI);
        if (arm != HumanoidArm.RIGHT) {
            poseStack.translate(0.1, 0.83, 0.35);
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-80.0f));
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-90.0f));
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(currentSwipeAngle));
            poseStack.translate(-0.3, 0.22, 0.35);
        } else {
            poseStack.translate(-0.25, 0.22, 0.35);
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-80.0f));
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(90.0f));
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(0.0f));
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(currentSwipeAngle));
        }
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackValue) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        float ySwingRotation = Mth.sin(attackValue * attackValue * (float)Math.PI);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * (45.0f + ySwingRotation * -20.0f)));
        float xzSwingRotation = Mth.sin(Mth.sqrt(attackValue) * (float)Math.PI);
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * xzSwingRotation * -20.0f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(xzSwingRotation * -80.0f));
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * -45.0f));
    }

    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate((float)invert * 0.56f, -0.52f + inverseArmHeight * -0.6f, -0.72f);
    }

    public void submitHandsWithItems(float frameInterp, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int lightCoords) {
        float attackValue = player.getAttackAnim(frameInterp);
        InteractionHand attackHand = (InteractionHand)((Object)MoreObjects.firstNonNull((Object)((Object)player.swingingArm), (Object)((Object)InteractionHand.MAIN_HAND)));
        float xRot = player.getXRot(frameInterp);
        HandRenderSelection handRenderSelection = ItemInHandRenderer.evaluateWhichHandsToRender(player);
        float xBob = Mth.lerp(frameInterp, player.xBobO, player.xBob);
        float yBob = Mth.lerp(frameInterp, player.yBobO, player.yBob);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees((player.getViewXRot(frameInterp) - xBob) * 0.1f));
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((player.getViewYRot(frameInterp) - yBob) * 0.1f));
        if (handRenderSelection.renderMainHand) {
            float mainHandAttack = attackHand == InteractionHand.MAIN_HAND ? attackValue : 0.0f;
            float mainhandInverseArmHeight = this.itemModelResolver.swapAnimationScale(this.mainHandItem) * (1.0f - Mth.lerp(frameInterp, this.oMainHandHeight, this.mainHandHeight));
            this.submitArmWithItem(player, frameInterp, xRot, InteractionHand.MAIN_HAND, mainHandAttack, this.mainHandItem, mainhandInverseArmHeight, poseStack, submitNodeCollector, lightCoords);
        }
        if (handRenderSelection.renderOffHand) {
            float offHandAttack = attackHand == InteractionHand.OFF_HAND ? attackValue : 0.0f;
            float offhandInverseArmHeight = this.itemModelResolver.swapAnimationScale(this.offHandItem) * (1.0f - Mth.lerp(frameInterp, this.oOffHandHeight, this.offHandHeight));
            this.submitArmWithItem(player, frameInterp, xRot, InteractionHand.OFF_HAND, offHandAttack, this.offHandItem, offhandInverseArmHeight, poseStack, submitNodeCollector, lightCoords);
        }
    }

    @VisibleForTesting
    static HandRenderSelection evaluateWhichHandsToRender(LocalPlayer player) {
        boolean holdsCrossbow;
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offhandItem = player.getOffhandItem();
        boolean holdsBow = mainHandItem.is(Items.BOW) || offhandItem.is(Items.BOW);
        boolean bl = holdsCrossbow = mainHandItem.is(Items.CROSSBOW) || offhandItem.is(Items.CROSSBOW);
        if (!holdsBow && !holdsCrossbow) {
            return HandRenderSelection.RENDER_BOTH_HANDS;
        }
        if (player.isUsingItem()) {
            return ItemInHandRenderer.selectionUsingItemWhileHoldingBowLike(player);
        }
        if (ItemInHandRenderer.isChargedCrossbow(mainHandItem)) {
            return HandRenderSelection.RENDER_MAIN_HAND_ONLY;
        }
        return HandRenderSelection.RENDER_BOTH_HANDS;
    }

    private static HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer player) {
        ItemStack usedItemStack = player.getUseItem();
        InteractionHand usedHand = player.getUsedItemHand();
        if (usedItemStack.is(Items.BOW) || usedItemStack.is(Items.CROSSBOW)) {
            return HandRenderSelection.onlyForHand(usedHand);
        }
        return usedHand == InteractionHand.MAIN_HAND && ItemInHandRenderer.isChargedCrossbow(player.getOffhandItem()) ? HandRenderSelection.RENDER_MAIN_HAND_ONLY : HandRenderSelection.RENDER_BOTH_HANDS;
    }

    private static boolean isChargedCrossbow(ItemStack item) {
        return item.is(Items.CROSSBOW) && CrossbowItem.isCharged(item);
    }

    private void submitArmWithItem(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        if (player.isScoping()) {
            return;
        }
        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        poseStack.pushPose();
        if (itemStack.isEmpty()) {
            if (isMainHand && !player.isInvisible()) {
                this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, attack, arm);
            }
        } else if (itemStack.has(DataComponents.MAP_ID)) {
            if (isMainHand && this.offHandItem.isEmpty()) {
                this.renderTwoHandedMap(poseStack, submitNodeCollector, lightCoords, xRot, inverseArmHeight, attack);
            } else {
                this.renderOneHandedMap(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, arm, attack, itemStack);
            }
        } else if (itemStack.is(Items.CROSSBOW)) {
            int invert;
            this.applyItemArmTransform(poseStack, arm, inverseArmHeight);
            boolean charged = CrossbowItem.isCharged(itemStack);
            boolean isRightArm = arm == HumanoidArm.RIGHT;
            int n = invert = isRightArm ? 1 : -1;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand && !charged) {
                poseStack.translate((float)invert * -0.4785682f, -0.094387f, 0.05731531f);
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-11.935f));
                poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * 65.3f));
                poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * -9.785f));
                float timeHeld = (float)itemStack.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - frameInterp + 1.0f);
                float power = timeHeld / (float)CrossbowItem.getChargeDuration(itemStack, player);
                if (power > 1.0f) {
                    power = 1.0f;
                }
                if (power > 0.1f) {
                    float shakeOffset = Mth.sin((timeHeld - 0.1f) * 1.3f);
                    float shakeIntensity = power - 0.1f;
                    float shake = shakeOffset * shakeIntensity;
                    poseStack.translate(shake * 0.0f, shake * 0.004f, shake * 0.0f);
                }
                poseStack.translate(power * 0.0f, power * 0.0f, power * 0.04f);
                poseStack.scale(1.0f, 1.0f, 1.0f + power * 0.2f);
                poseStack.mulPose((Quaternionfc)Axis.YN.rotationDegrees((float)invert * 45.0f));
            } else {
                this.swingArm(attack, poseStack, invert, arm);
                if (charged && attack < 0.001f && isMainHand) {
                    poseStack.translate((float)invert * -0.641864f, 0.0f, 0.0f);
                    poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * 10.0f));
                }
            }
            this.renderItem(player, itemStack, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, submitNodeCollector, lightCoords);
        } else {
            int invert;
            boolean isRightArm = arm == HumanoidArm.RIGHT;
            int n = invert = isRightArm ? 1 : -1;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                ItemUseAnimation useAnimation = itemStack.getUseAnimation();
                if (!useAnimation.hasCustomArmTransform()) {
                    this.applyItemArmTransform(poseStack, arm, inverseArmHeight);
                }
                switch (useAnimation) {
                    case NONE: {
                        break;
                    }
                    case EAT: 
                    case DRINK: {
                        this.applyEatTransform(poseStack, frameInterp, arm, itemStack, player);
                        this.applyItemArmTransform(poseStack, arm, inverseArmHeight);
                        break;
                    }
                    case BLOCK: {
                        if (itemStack.getItem() instanceof ShieldItem) break;
                        poseStack.translate((float)invert * -0.14142136f, 0.08f, 0.14142136f);
                        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-102.25f));
                        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * 13.365f));
                        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * 78.05f));
                        break;
                    }
                    case BOW: {
                        poseStack.translate((float)invert * -0.2785682f, 0.18344387f, 0.15731531f);
                        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-13.935f));
                        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * 35.3f));
                        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * -9.785f));
                        float timeHeld = (float)itemStack.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - frameInterp + 1.0f);
                        float power = timeHeld / 20.0f;
                        power = (power * power + power * 2.0f) / 3.0f;
                        if (power > 1.0f) {
                            power = 1.0f;
                        }
                        if (power > 0.1f) {
                            float shakeOffset = Mth.sin((timeHeld - 0.1f) * 1.3f);
                            float shakeIntensity = power - 0.1f;
                            float shake = shakeOffset * shakeIntensity;
                            poseStack.translate(shake * 0.0f, shake * 0.004f, shake * 0.0f);
                        }
                        poseStack.translate(power * 0.0f, power * 0.0f, power * 0.04f);
                        poseStack.scale(1.0f, 1.0f, 1.0f + power * 0.2f);
                        poseStack.mulPose((Quaternionfc)Axis.YN.rotationDegrees((float)invert * 45.0f));
                        break;
                    }
                    case TRIDENT: {
                        poseStack.translate((float)invert * -0.5f, 0.7f, 0.1f);
                        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-55.0f));
                        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * 35.3f));
                        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * -9.785f));
                        float timeHeld = (float)itemStack.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - frameInterp + 1.0f);
                        float power = timeHeld / 10.0f;
                        if (power > 1.0f) {
                            power = 1.0f;
                        }
                        if (power > 0.1f) {
                            float shakeOffset = Mth.sin((timeHeld - 0.1f) * 1.3f);
                            float shakeIntensity = power - 0.1f;
                            float shake = shakeOffset * shakeIntensity;
                            poseStack.translate(shake * 0.0f, shake * 0.004f, shake * 0.0f);
                        }
                        poseStack.translate(0.0f, 0.0f, power * 0.2f);
                        poseStack.scale(1.0f, 1.0f, 1.0f + power * 0.2f);
                        poseStack.mulPose((Quaternionfc)Axis.YN.rotationDegrees((float)invert * 45.0f));
                        break;
                    }
                    case BRUSH: {
                        this.applyBrushTransform(poseStack, frameInterp, arm, player);
                        break;
                    }
                    case BUNDLE: {
                        this.swingArm(attack, poseStack, invert, arm);
                        break;
                    }
                    case SPEAR: {
                        poseStack.translate((float)invert * 0.56f, -0.52f, -0.72f);
                        float timeHeld = (float)itemStack.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - frameInterp + 1.0f);
                        SpearAnimations.firstPersonUse(player.getTicksSinceLastKineticHitFeedback(frameInterp), poseStack, timeHeld, arm, itemStack);
                    }
                }
            } else if (player.isAutoSpinAttack()) {
                this.applyItemArmTransform(poseStack, arm, inverseArmHeight);
                poseStack.translate((float)invert * -0.4f, 0.8f, 0.3f);
                poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)invert * 65.0f));
                poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)invert * -85.0f));
            } else {
                this.applyItemArmTransform(poseStack, arm, inverseArmHeight);
                switch (itemStack.getSwingAnimation().type()) {
                    case NONE: {
                        break;
                    }
                    case WHACK: {
                        this.swingArm(attack, poseStack, invert, arm);
                        break;
                    }
                    case STAB: {
                        SpearAnimations.firstPersonAttack(attack, poseStack, invert, arm);
                    }
                }
            }
            this.renderItem(player, itemStack, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, submitNodeCollector, lightCoords);
        }
        poseStack.popPose();
    }

    private void swingArm(float attack, PoseStack poseStack, int invert, HumanoidArm arm) {
        float xSwingPosition = -0.4f * Mth.sin(Mth.sqrt(attack) * (float)Math.PI);
        float ySwingPosition = 0.2f * Mth.sin(Mth.sqrt(attack) * ((float)Math.PI * 2));
        float zSwingPosition = -0.2f * Mth.sin(attack * (float)Math.PI);
        poseStack.translate((float)invert * xSwingPosition, ySwingPosition, zSwingPosition);
        this.applyItemArmAttackTransform(poseStack, arm, attack);
    }

    private boolean shouldInstantlyReplaceVisibleItem(ItemStack currentlyVisibleItem, ItemStack expectedItem) {
        if (ItemStack.matchesIgnoringComponents(currentlyVisibleItem, expectedItem, DataComponentType::ignoreSwapAnimation)) {
            return true;
        }
        return !this.itemModelResolver.shouldPlaySwapAnimation(expectedItem);
    }

    public void tick() {
        this.oMainHandHeight = this.mainHandHeight;
        this.oOffHandHeight = this.offHandHeight;
        LocalPlayer player = this.minecraft.player;
        ItemStack nextMainHand = player.getMainHandItem();
        ItemStack nextOffHand = player.getOffhandItem();
        if (this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, nextMainHand)) {
            this.mainHandItem = nextMainHand;
        }
        if (this.shouldInstantlyReplaceVisibleItem(this.offHandItem, nextOffHand)) {
            this.offHandItem = nextOffHand;
        }
        if (player.isHandsBusy()) {
            this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4f, 0.0f, 1.0f);
            this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4f, 0.0f, 1.0f);
        } else {
            float attackAnim = player.getItemSwapScale(1.0f);
            float mainHandTargetHeight = this.mainHandItem != nextMainHand ? 0.0f : attackAnim * attackAnim * attackAnim;
            float offHandTargetHeight = this.offHandItem != nextOffHand ? 0.0f : 1.0f;
            this.mainHandHeight += Mth.clamp(mainHandTargetHeight - this.mainHandHeight, -0.4f, 0.4f);
            this.offHandHeight += Mth.clamp(offHandTargetHeight - this.offHandHeight, -0.4f, 0.4f);
        }
        if (this.mainHandHeight < 0.1f) {
            this.mainHandItem = nextMainHand;
        }
        if (this.offHandHeight < 0.1f) {
            this.offHandItem = nextOffHand;
        }
    }

    public void itemUsed(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandHeight = 0.0f;
        } else {
            this.offHandHeight = 0.0f;
        }
    }

    @Environment(value=EnvType.CLIENT)
    @VisibleForTesting
    static enum HandRenderSelection {
        RENDER_BOTH_HANDS(true, true),
        RENDER_MAIN_HAND_ONLY(true, false),
        RENDER_OFF_HAND_ONLY(false, true);

        public final boolean renderMainHand;
        public final boolean renderOffHand;

        private HandRenderSelection(boolean renderMainHand, boolean renderOffHand) {
            this.renderMainHand = renderMainHand;
            this.renderOffHand = renderOffHand;
        }

        public static HandRenderSelection onlyForHand(InteractionHand hand) {
            return hand == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
        }
    }
}

