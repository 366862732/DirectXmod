/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class HumanoidModel<T extends HumanoidRenderState>
extends EntityModel<T>
implements ArmedModel<T>,
HeadedModel {
    public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 16.0f, 0.0f, 2.0f, 2.0f, 24.0f, Set.of("head"));
    public static final float OVERLAY_SCALE = 0.25f;
    public static final float HAT_OVERLAY_SCALE = 0.5f;
    public static final float LEGGINGS_OVERLAY_SCALE = -0.1f;
    private static final float DUCK_WALK_ROTATION = 0.005f;
    private static final float SPYGLASS_ARM_ROT_Y = 0.2617994f;
    private static final float SPYGLASS_ARM_ROT_X = 1.9198622f;
    private static final float SPYGLASS_ARM_CROUCH_ROT_X = 0.2617994f;
    private static final float HIGHEST_SHIELD_BLOCKING_ANGLE = -1.3962634f;
    private static final float LOWEST_SHIELD_BLOCKING_ANGLE = 0.43633232f;
    private static final float HORIZONTAL_SHIELD_MOVEMENT_LIMIT = 0.5235988f;
    public static final float TOOT_HORN_XROT_BASE = 1.4835298f;
    public static final float TOOT_HORN_YROT_BASE = 0.5235988f;
    protected static final Map<EquipmentSlot, Set<String>> ADULT_ARMOR_PARTS_PER_SLOT = Maps.newEnumMap(Map.of(EquipmentSlot.HEAD, Set.of("head"), EquipmentSlot.CHEST, Set.of("body", "left_arm", "right_arm"), EquipmentSlot.LEGS, Set.of("left_leg", "right_leg", "body"), EquipmentSlot.FEET, Set.of("left_leg", "right_leg")));
    protected static final Map<EquipmentSlot, Set<String>> BABY_ARMOR_PARTS_PER_SLOT = Maps.newEnumMap(Map.of(EquipmentSlot.HEAD, Set.of("head"), EquipmentSlot.CHEST, Set.of("body", "left_arm", "right_arm"), EquipmentSlot.LEGS, Set.of("left_leg", "right_leg", "waist"), EquipmentSlot.FEET, Set.of("left_foot", "right_foot")));
    public final ModelPart head;
    public final ModelPart hat;
    public final ModelPart body;
    public final ModelPart rightArm;
    public final ModelPart leftArm;
    public final ModelPart rightLeg;
    public final ModelPart leftLeg;

    public HumanoidModel(ModelPart root) {
        this(root, RenderTypes::entityCutout);
    }

    public HumanoidModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.head = root.getChild("head");
        this.hat = this.head.getChild("hat");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static MeshDefinition createMesh(CubeDeformation g, float yOffset) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, g), PartPose.offset(0.0f, 0.0f + yOffset, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, g.extend(0.5f)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, g), PartPose.offset(0.0f, 0.0f + yOffset, 0.0f));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, g), PartPose.offset(-5.0f, 2.0f + yOffset, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, g), PartPose.offset(5.0f, 2.0f + yOffset, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g), PartPose.offset(-1.9f, 12.0f + yOffset, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g), PartPose.offset(1.9f, 12.0f + yOffset, 0.0f));
        return mesh;
    }

    public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation innerDeformation, CubeDeformation outerDeformation) {
        return HumanoidModel.createArmorMeshSet(HumanoidModel::createBaseArmorMesh, ADULT_ARMOR_PARTS_PER_SLOT, innerDeformation, outerDeformation);
    }

    public static ArmorModelSet<MeshDefinition> createBabyArmorMeshSet(CubeDeformation innerDeformation, CubeDeformation outerDeformation, PartPose armOffset) {
        return HumanoidModel.createArmorMeshSet(cube -> HumanoidModel.createBabyArmorMesh(cube, armOffset), BABY_ARMOR_PARTS_PER_SLOT, innerDeformation, outerDeformation);
    }

    protected static ArmorModelSet<MeshDefinition> createArmorMeshSet(Function<CubeDeformation, MeshDefinition> baseFactory, Map<EquipmentSlot, Set<String>> partsPerSlot, CubeDeformation innerDeformation, CubeDeformation outerDeformation) {
        MeshDefinition head = baseFactory.apply(outerDeformation);
        head.getRoot().retainPartsAndChildren(partsPerSlot.get(EquipmentSlot.HEAD));
        MeshDefinition chest = baseFactory.apply(outerDeformation);
        chest.getRoot().retainExactParts(partsPerSlot.get(EquipmentSlot.CHEST));
        MeshDefinition legs = baseFactory.apply(innerDeformation);
        legs.getRoot().retainExactParts(partsPerSlot.get(EquipmentSlot.LEGS));
        MeshDefinition feet = baseFactory.apply(outerDeformation);
        feet.getRoot().retainExactParts(partsPerSlot.get(EquipmentSlot.FEET));
        return new ArmorModelSet<MeshDefinition>(head, chest, legs, feet);
    }

    private static MeshDefinition createBaseArmorMesh(CubeDeformation g) {
        MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g.extend(-0.1f)), PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g.extend(-0.1f)), PartPose.offset(1.9f, 12.0f, 0.0f));
        return mesh;
    }

    private static MeshDefinition createBabyArmorMesh(CubeDeformation g, PartPose armOffset) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5f, -7.0f, -4.5f, 9.0f, 8.0f, 8.0f, g), PartPose.offset(0.0f, 15.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0f, -3.0f, -1.5f, 6.0f, 5.0f, 3.0f, g), PartPose.offset(0.0f, 18.0f, 0.0f));
        root.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(0, 36).addBox(-3.0f, -1.2f, -1.49f, 5.9f, 2.0f, 2.9f, g.extend(-0.1f)), PartPose.offset(0.0f, 19.0f, 0.0f));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(30, 25).addBox(-1.0f, 0.0f, -1.53f, 2.0f, 5.0f, 3.0f, g), PartPose.offset(-3.5f - armOffset.x(), 15.5f + armOffset.y(), 0.0f + armOffset.z()));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(30, 17).addBox(-1.0f, 0.0f, -1.53f, 2.0f, 5.0f, 3.0f, g), PartPose.offset(3.5f + armOffset.x(), 15.5f + armOffset.y(), 0.0f + armOffset.z()));
        root.addOrReplaceChild("inner_body", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0f, -3.0f, -1.5f, 6.0f, 5.0f, 3.0f, g), PartPose.offset(0.0f, 18.0f, 0.0f));
        PartDefinition rightLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(18, 24).addBox(-2.0f, -0.2f, -2.0f, 3.0f, 4.0f, 3.0f, g.extend(-0.1f)), PartPose.offset(1.5f, 20.0f, 0.5f));
        PartDefinition leftLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(18, 17).addBox(-1.0f, -0.2f, -2.0f, 3.0f, 4.0f, 3.0f, g.extend(-0.1f)), PartPose.offset(-1.5f, 20.0f, 0.5f));
        rightLeg.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0f, 2.9f, -2.0f, 3.0f, 1.0f, 3.0f, g), PartPose.offset(0.0f, 0.0f, 0.0f));
        leftLeg.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-1.0f, 2.9f, -2.0f, 3.0f, 1.0f, 3.0f, g).mirror(false), PartPose.offset(0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    @Override
    public void setupAnim(T state) {
        boolean rightHanded;
        super.setupAnim(state);
        ArmPose leftArmPose = ((HumanoidRenderState)state).leftArmPose;
        ArmPose rightArmPose = ((HumanoidRenderState)state).rightArmPose;
        float swimAmount = ((HumanoidRenderState)state).swimAmount;
        boolean fallFlying = ((HumanoidRenderState)state).isFallFlying;
        this.head.xRot = ((HumanoidRenderState)state).xRot * ((float)Math.PI / 180);
        this.head.yRot = ((HumanoidRenderState)state).yRot * ((float)Math.PI / 180);
        if (fallFlying) {
            this.head.xRot = -0.7853982f;
        } else if (swimAmount > 0.0f) {
            this.head.xRot = Mth.rotLerpRad(swimAmount, this.head.xRot, -0.7853982f);
        }
        float animationPos = ((HumanoidRenderState)state).walkAnimationPos;
        float animationSpeed = ((HumanoidRenderState)state).walkAnimationSpeed;
        this.rightArm.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 2.0f * animationSpeed * 0.5f / ((HumanoidRenderState)state).speedValue;
        this.leftArm.xRot = Mth.cos(animationPos * 0.6662f) * 2.0f * animationSpeed * 0.5f / ((HumanoidRenderState)state).speedValue;
        this.rightLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed / ((HumanoidRenderState)state).speedValue;
        this.leftLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed / ((HumanoidRenderState)state).speedValue;
        this.rightLeg.yRot = 0.005f;
        this.leftLeg.yRot = -0.005f;
        this.rightLeg.zRot = 0.005f;
        this.leftLeg.zRot = -0.005f;
        if (((HumanoidRenderState)state).isPassenger) {
            this.rightArm.xRot += -0.62831855f;
            this.leftArm.xRot += -0.62831855f;
            this.rightLeg.xRot = -1.4137167f;
            this.rightLeg.yRot = 0.31415927f;
            this.rightLeg.zRot = 0.07853982f;
            this.leftLeg.xRot = -1.4137167f;
            this.leftLeg.yRot = -0.31415927f;
            this.leftLeg.zRot = -0.07853982f;
        }
        boolean bl = rightHanded = ((HumanoidRenderState)state).mainArm == HumanoidArm.RIGHT;
        if (((HumanoidRenderState)state).isUsingItem) {
            boolean mainHandUsed;
            boolean bl2 = mainHandUsed = ((HumanoidRenderState)state).useItemHand == InteractionHand.MAIN_HAND;
            if (mainHandUsed == rightHanded) {
                this.poseRightArm(state);
                if (!((HumanoidRenderState)state).rightArmPose.affectsOffhandPose()) {
                    this.poseLeftArm(state);
                }
            } else {
                this.poseLeftArm(state);
                if (!((HumanoidRenderState)state).leftArmPose.affectsOffhandPose()) {
                    this.poseRightArm(state);
                }
            }
        } else {
            boolean twoHandedOffhand;
            boolean bl3 = twoHandedOffhand = rightHanded ? leftArmPose.isTwoHanded() : rightArmPose.isTwoHanded();
            if (rightHanded != twoHandedOffhand) {
                this.poseLeftArm(state);
                if (!((HumanoidRenderState)state).leftArmPose.affectsOffhandPose()) {
                    this.poseRightArm(state);
                }
            } else {
                this.poseRightArm(state);
                if (!((HumanoidRenderState)state).rightArmPose.affectsOffhandPose()) {
                    this.poseLeftArm(state);
                }
            }
        }
        this.setupAttackAnimation(state);
        if (((HumanoidRenderState)state).isCrouching) {
            this.body.xRot = 0.5f;
            this.rightArm.xRot += 0.4f;
            this.leftArm.xRot += 0.4f;
            this.rightLeg.z += 4.0f;
            this.leftLeg.z += 4.0f;
            this.head.y += 4.2f;
            this.body.y += 3.2f;
            this.leftArm.y += 3.2f;
            this.rightArm.y += 3.2f;
        }
        if (rightArmPose != ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.rightArm, ((HumanoidRenderState)state).ageInTicks, 1.0f);
        }
        if (leftArmPose != ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.leftArm, ((HumanoidRenderState)state).ageInTicks, -1.0f);
        }
        if (swimAmount > 0.0f) {
            float leftArmSwimAmount;
            float swimPos = animationPos % 26.0f;
            HumanoidArm attackArm = ((HumanoidRenderState)state).attackArm;
            float rightArmSwimAmount = ((HumanoidRenderState)state).rightArmPose == ArmPose.SPEAR || attackArm == HumanoidArm.RIGHT && ((HumanoidRenderState)state).attackTime > 0.0f ? 0.0f : swimAmount;
            float f = leftArmSwimAmount = ((HumanoidRenderState)state).leftArmPose == ArmPose.SPEAR || attackArm == HumanoidArm.LEFT && ((HumanoidRenderState)state).attackTime > 0.0f ? 0.0f : swimAmount;
            if (!((HumanoidRenderState)state).isUsingItem) {
                if (swimPos < 14.0f) {
                    this.leftArm.xRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.xRot, 0.0f);
                    this.rightArm.xRot = Mth.lerp(rightArmSwimAmount, this.rightArm.xRot, 0.0f);
                    this.leftArm.yRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.yRot, (float)Math.PI);
                    this.rightArm.yRot = Mth.lerp(rightArmSwimAmount, this.rightArm.yRot, (float)Math.PI);
                    this.leftArm.zRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.zRot, (float)Math.PI + 1.8707964f * this.quadraticArmUpdate(swimPos) / this.quadraticArmUpdate(14.0f));
                    this.rightArm.zRot = Mth.lerp(rightArmSwimAmount, this.rightArm.zRot, (float)Math.PI - 1.8707964f * this.quadraticArmUpdate(swimPos) / this.quadraticArmUpdate(14.0f));
                } else if (swimPos >= 14.0f && swimPos < 22.0f) {
                    internalSwimPos = (swimPos - 14.0f) / 8.0f;
                    this.leftArm.xRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.xRot, 1.5707964f * internalSwimPos);
                    this.rightArm.xRot = Mth.lerp(rightArmSwimAmount, this.rightArm.xRot, 1.5707964f * internalSwimPos);
                    this.leftArm.yRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.yRot, (float)Math.PI);
                    this.rightArm.yRot = Mth.lerp(rightArmSwimAmount, this.rightArm.yRot, (float)Math.PI);
                    this.leftArm.zRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.zRot, 5.012389f - 1.8707964f * internalSwimPos);
                    this.rightArm.zRot = Mth.lerp(rightArmSwimAmount, this.rightArm.zRot, 1.2707963f + 1.8707964f * internalSwimPos);
                } else if (swimPos >= 22.0f && swimPos < 26.0f) {
                    internalSwimPos = (swimPos - 22.0f) / 4.0f;
                    this.leftArm.xRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.xRot, 1.5707964f - 1.5707964f * internalSwimPos);
                    this.rightArm.xRot = Mth.lerp(rightArmSwimAmount, this.rightArm.xRot, 1.5707964f - 1.5707964f * internalSwimPos);
                    this.leftArm.yRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.yRot, (float)Math.PI);
                    this.rightArm.yRot = Mth.lerp(rightArmSwimAmount, this.rightArm.yRot, (float)Math.PI);
                    this.leftArm.zRot = Mth.rotLerpRad(leftArmSwimAmount, this.leftArm.zRot, (float)Math.PI);
                    this.rightArm.zRot = Mth.lerp(rightArmSwimAmount, this.rightArm.zRot, (float)Math.PI);
                }
            }
            float amplitude = 0.3f;
            float slowdown = 0.33333334f;
            this.leftLeg.xRot = Mth.lerp(swimAmount, this.leftLeg.xRot, 0.3f * Mth.cos(animationPos * 0.33333334f + (float)Math.PI));
            this.rightLeg.xRot = Mth.lerp(swimAmount, this.rightLeg.xRot, 0.3f * Mth.cos(animationPos * 0.33333334f));
        }
    }

    private void poseRightArm(T state) {
        switch (((HumanoidRenderState)state).rightArmPose.ordinal()) {
            case 0: {
                this.rightArm.yRot = 0.0f;
                break;
            }
            case 2: {
                this.poseBlockingArm(this.rightArm, true);
                break;
            }
            case 1: {
                this.rightArm.xRot = this.rightArm.xRot * 0.5f - 0.31415927f;
                this.rightArm.yRot = 0.0f;
                break;
            }
            case 4: {
                this.rightArm.xRot = this.rightArm.xRot * 0.5f - (float)Math.PI;
                this.rightArm.yRot = 0.0f;
                break;
            }
            case 10: {
                SpearAnimations.thirdPersonHandUse(this.rightArm, this.head, true, ((ArmedEntityRenderState)state).getUseItemStackForArm(HumanoidArm.RIGHT), state);
                break;
            }
            case 3: {
                this.rightArm.yRot = -0.1f + this.head.yRot;
                this.leftArm.yRot = 0.1f + this.head.yRot + 0.4f;
                this.rightArm.xRot = -1.5707964f + this.head.xRot;
                this.leftArm.xRot = -1.5707964f + this.head.xRot;
                break;
            }
            case 5: {
                AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, ((HumanoidRenderState)state).maxCrossbowChargeDuration, ((HumanoidRenderState)state).ticksUsingItem, true);
                break;
            }
            case 6: {
                AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
                break;
            }
            case 9: {
                this.rightArm.xRot = this.rightArm.xRot * 0.5f - 0.62831855f;
                this.rightArm.yRot = 0.0f;
                break;
            }
            case 7: {
                this.rightArm.xRot = Mth.clamp(this.head.xRot - 1.9198622f - (((HumanoidRenderState)state).isCrouching ? 0.2617994f : 0.0f), -2.4f, 3.3f);
                this.rightArm.yRot = this.head.yRot - 0.2617994f;
                break;
            }
            case 8: {
                this.rightArm.xRot = Mth.clamp(this.head.xRot, -1.2f, 1.2f) - 1.4835298f;
                this.rightArm.yRot = this.head.yRot - 0.5235988f;
            }
        }
    }

    private void poseLeftArm(T state) {
        switch (((HumanoidRenderState)state).leftArmPose.ordinal()) {
            case 0: {
                this.leftArm.yRot = 0.0f;
                break;
            }
            case 2: {
                this.poseBlockingArm(this.leftArm, false);
                break;
            }
            case 1: {
                this.leftArm.xRot = this.leftArm.xRot * 0.5f - 0.31415927f;
                this.leftArm.yRot = 0.0f;
                break;
            }
            case 4: {
                this.leftArm.xRot = this.leftArm.xRot * 0.5f - (float)Math.PI;
                this.leftArm.yRot = 0.0f;
                break;
            }
            case 10: {
                SpearAnimations.thirdPersonHandUse(this.leftArm, this.head, false, ((ArmedEntityRenderState)state).getUseItemStackForArm(HumanoidArm.LEFT), state);
                break;
            }
            case 3: {
                this.rightArm.yRot = -0.1f + this.head.yRot - 0.4f;
                this.leftArm.yRot = 0.1f + this.head.yRot;
                this.rightArm.xRot = -1.5707964f + this.head.xRot;
                this.leftArm.xRot = -1.5707964f + this.head.xRot;
                break;
            }
            case 5: {
                AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, ((HumanoidRenderState)state).maxCrossbowChargeDuration, ((HumanoidRenderState)state).ticksUsingItem, false);
                break;
            }
            case 6: {
                AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, false);
                break;
            }
            case 9: {
                this.leftArm.xRot = this.leftArm.xRot * 0.5f - 0.62831855f;
                this.leftArm.yRot = 0.0f;
                break;
            }
            case 7: {
                this.leftArm.xRot = Mth.clamp(this.head.xRot - 1.9198622f - (((HumanoidRenderState)state).isCrouching ? 0.2617994f : 0.0f), -2.4f, 3.3f);
                this.leftArm.yRot = this.head.yRot + 0.2617994f;
                break;
            }
            case 8: {
                this.leftArm.xRot = Mth.clamp(this.head.xRot, -1.2f, 1.2f) - 1.4835298f;
                this.leftArm.yRot = this.head.yRot + 0.5235988f;
            }
        }
    }

    private void poseBlockingArm(ModelPart arm, boolean right) {
        arm.xRot = arm.xRot * 0.5f - 0.9424779f + Mth.clamp(this.head.xRot, -1.3962634f, 0.43633232f);
        arm.yRot = (right ? -30.0f : 30.0f) * ((float)Math.PI / 180) + Mth.clamp(this.head.yRot, -0.5235988f, 0.5235988f);
    }

    protected void setupAttackAnimation(T state) {
        float attackTime = ((HumanoidRenderState)state).attackTime;
        if (attackTime <= 0.0f) {
            return;
        }
        this.body.yRot = Mth.sin(Mth.sqrt(attackTime) * ((float)Math.PI * 2)) * 0.2f;
        if (((HumanoidRenderState)state).attackArm == HumanoidArm.LEFT) {
            this.body.yRot *= -1.0f;
        }
        float ageScale = ((HumanoidRenderState)state).ageScale;
        this.rightArm.z = Mth.sin(this.body.yRot) * 5.0f * ageScale;
        this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0f * ageScale;
        this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0f * ageScale;
        this.leftArm.x = Mth.cos(this.body.yRot) * 5.0f * ageScale;
        this.rightArm.yRot += this.body.yRot;
        this.leftArm.yRot += this.body.yRot;
        this.leftArm.xRot += this.body.yRot;
        switch (((HumanoidRenderState)state).swingAnimationType) {
            case WHACK: {
                float swing = Ease.outQuart(attackTime);
                float aa = Mth.sin(swing * (float)Math.PI);
                float bb = Mth.sin(attackTime * (float)Math.PI) * -(this.head.xRot - 0.7f) * 0.75f;
                ModelPart attackArm = this.getArm(((HumanoidRenderState)state).attackArm);
                attackArm.xRot -= aa * 1.2f + bb;
                attackArm.yRot += this.body.yRot * 2.0f;
                attackArm.zRot += Mth.sin(attackTime * (float)Math.PI) * -0.4f;
                break;
            }
            case NONE: {
                break;
            }
            case STAB: {
                SpearAnimations.thirdPersonAttackHand(this, state);
            }
        }
    }

    private float quadraticArmUpdate(float x) {
        return -65.0f * x + x * x;
    }

    @Override
    public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.getArm(arm).translateAndRotate(poseStack);
    }

    public ModelPart getArm(HumanoidArm arm) {
        if (arm == HumanoidArm.LEFT) {
            return this.leftArm;
        }
        return this.rightArm;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum ArmPose {
        EMPTY(false, false),
        ITEM(false, false),
        BLOCK(false, false),
        BOW_AND_ARROW(true, true),
        THROW_TRIDENT(false, true),
        CROSSBOW_CHARGE(true, true),
        CROSSBOW_HOLD(true, true),
        SPYGLASS(false, false),
        TOOT_HORN(false, false),
        BRUSH(false, false),
        SPEAR(false, true){

            @Override
            public <S extends ArmedEntityRenderState> void animateUseItem(S state, PoseStack poseStack, float ticksUsingItem, HumanoidArm arm, ItemStack actualItem) {
                SpearAnimations.thirdPersonUseItem(state, poseStack, ticksUsingItem, arm, actualItem);
            }
        };

        private final boolean twoHanded;
        private final boolean affectsOffhandPose;

        private ArmPose(boolean twoHanded, boolean affectsOffhandPose) {
            this.twoHanded = twoHanded;
            this.affectsOffhandPose = affectsOffhandPose;
        }

        public boolean isTwoHanded() {
            return this.twoHanded;
        }

        public boolean affectsOffhandPose() {
            return this.affectsOffhandPose;
        }

        public <S extends ArmedEntityRenderState> void animateUseItem(S state, PoseStack poseStack, float ticksUsingItem, HumanoidArm arm, ItemStack actualItem) {
        }
    }
}

