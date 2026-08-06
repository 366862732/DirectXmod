/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.warden;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.WardenAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class WardenModel
extends EntityModel<WardenRenderState> {
    private static final float DEFAULT_ARM_X_Y = 13.0f;
    private static final float DEFAULT_ARM_Z = 1.0f;
    protected final ModelPart bone;
    protected final ModelPart body;
    protected final ModelPart head;
    protected final ModelPart rightTendril;
    protected final ModelPart leftTendril;
    protected final ModelPart leftLeg;
    protected final ModelPart leftArm;
    protected final ModelPart leftRibcage;
    protected final ModelPart rightArm;
    protected final ModelPart rightLeg;
    protected final ModelPart rightRibcage;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation sonicBoomAnimation;
    private final KeyframeAnimation diggingAnimation;
    private final KeyframeAnimation emergeAnimation;
    private final KeyframeAnimation roarAnimation;
    private final KeyframeAnimation sniffAnimation;

    public WardenModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.bone = root.getChild("bone");
        this.body = this.bone.getChild("body");
        this.head = this.body.getChild("head");
        this.rightLeg = this.bone.getChild("right_leg");
        this.leftLeg = this.bone.getChild("left_leg");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightTendril = this.head.getChild("right_tendril");
        this.leftTendril = this.head.getChild("left_tendril");
        this.rightRibcage = this.body.getChild("right_ribcage");
        this.leftRibcage = this.body.getChild("left_ribcage");
        this.attackAnimation = WardenAnimation.WARDEN_ATTACK.bake(root);
        this.sonicBoomAnimation = WardenAnimation.WARDEN_SONIC_BOOM.bake(root);
        this.diggingAnimation = WardenAnimation.WARDEN_DIG.bake(root);
        this.emergeAnimation = WardenAnimation.WARDEN_EMERGE.bake(root);
        this.roarAnimation = WardenAnimation.WARDEN_ROAR.bake(root);
        this.sniffAnimation = WardenAnimation.WARDEN_SNIFF.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0f, 24.0f, 0.0f));
        PartDefinition body = bone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0f, -13.0f, -4.0f, 18.0f, 21.0f, 11.0f), PartPose.offset(0.0f, -21.0f, 0.0f));
        body.addOrReplaceChild("right_ribcage", CubeListBuilder.create().texOffs(90, 11).addBox(-2.0f, -11.0f, -0.1f, 9.0f, 21.0f, 0.0f), PartPose.offset(-7.0f, -2.0f, -4.0f));
        body.addOrReplaceChild("left_ribcage", CubeListBuilder.create().texOffs(90, 11).mirror().addBox(-7.0f, -11.0f, -0.1f, 9.0f, 21.0f, 0.0f).mirror(false), PartPose.offset(7.0f, -2.0f, -4.0f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0f, -16.0f, -5.0f, 16.0f, 16.0f, 10.0f), PartPose.offset(0.0f, -13.0f, 0.0f));
        head.addOrReplaceChild("right_tendril", CubeListBuilder.create().texOffs(52, 32).addBox(-16.0f, -13.0f, 0.0f, 16.0f, 16.0f, 0.0f), PartPose.offset(-8.0f, -12.0f, 0.0f));
        head.addOrReplaceChild("left_tendril", CubeListBuilder.create().texOffs(58, 0).addBox(0.0f, -13.0f, 0.0f, 16.0f, 16.0f, 0.0f), PartPose.offset(8.0f, -12.0f, 0.0f));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 50).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 28.0f, 8.0f), PartPose.offset(-13.0f, -13.0f, 1.0f));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 58).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 28.0f, 8.0f), PartPose.offset(13.0f, -13.0f, 1.0f));
        bone.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(76, 48).addBox(-3.1f, 0.0f, -3.0f, 6.0f, 13.0f, 6.0f), PartPose.offset(-5.9f, -13.0f, 0.0f));
        bone.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(76, 76).addBox(-2.9f, 0.0f, -3.0f, 6.0f, 13.0f, 6.0f), PartPose.offset(5.9f, -13.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createTendrilsLayer() {
        return WardenModel.createBodyLayer().apply(mesh -> {
            mesh.getRoot().retainExactParts(Set.of("left_tendril", "right_tendril"));
            return mesh;
        });
    }

    public static LayerDefinition createHeartLayer() {
        return WardenModel.createBodyLayer().apply(mesh -> {
            mesh.getRoot().retainExactParts(Set.of("body"));
            return mesh;
        });
    }

    public static LayerDefinition createBioluminescentLayer() {
        return WardenModel.createBodyLayer().apply(mesh -> {
            mesh.getRoot().retainExactParts(Set.of("head", "left_arm", "right_arm", "left_leg", "right_leg"));
            return mesh;
        });
    }

    public static LayerDefinition createPulsatingSpotsLayer() {
        return WardenModel.createBodyLayer().apply(mesh -> {
            mesh.getRoot().retainExactParts(Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"));
            return mesh;
        });
    }

    @Override
    public void setupAnim(WardenRenderState state) {
        super.setupAnim(state);
        this.animateHeadLookTarget(state.yRot, state.xRot);
        this.animateWalk(state.walkAnimationPos, state.walkAnimationSpeed);
        this.animateIdlePose(state.ageInTicks);
        this.animateTendrils(state, state.ageInTicks);
        this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
        this.sonicBoomAnimation.apply(state.sonicBoomAnimationState, state.ageInTicks);
        this.diggingAnimation.apply(state.diggingAnimationState, state.ageInTicks);
        this.emergeAnimation.apply(state.emergeAnimationState, state.ageInTicks);
        this.roarAnimation.apply(state.roarAnimationState, state.ageInTicks);
        this.sniffAnimation.apply(state.sniffAnimationState, state.ageInTicks);
    }

    private void animateHeadLookTarget(float yRot, float xRot) {
        this.head.xRot = xRot * ((float)Math.PI / 180);
        this.head.yRot = yRot * ((float)Math.PI / 180);
    }

    private void animateIdlePose(float ageInTicks) {
        float scaledAge = ageInTicks * 0.1f;
        float wobbleCosine = Mth.cos(scaledAge);
        float wobbleSine = Mth.sin(scaledAge);
        this.head.zRot += 0.06f * wobbleCosine;
        this.head.xRot += 0.06f * wobbleSine;
        this.body.zRot += 0.025f * wobbleSine;
        this.body.xRot += 0.025f * wobbleCosine;
    }

    private void animateWalk(float animationPos, float animationSpeed) {
        float speedModifier = Math.min(0.5f, 3.0f * animationSpeed);
        float adjustedPos = animationPos * 0.8662f;
        float adjustedPosCosine = Mth.cos(adjustedPos);
        float adjustedPosSine = Mth.sin(adjustedPos);
        float speedModifierWithMin = Math.min(0.35f, speedModifier);
        this.head.zRot += 0.3f * adjustedPosSine * speedModifier;
        this.head.xRot += 1.2f * Mth.cos(adjustedPos + 1.5707964f) * speedModifierWithMin;
        this.body.zRot = 0.1f * adjustedPosSine * speedModifier;
        this.body.xRot = 1.0f * adjustedPosCosine * speedModifierWithMin;
        this.leftLeg.xRot = 1.0f * adjustedPosCosine * speedModifier;
        this.rightLeg.xRot = 1.0f * Mth.cos(adjustedPos + (float)Math.PI) * speedModifier;
        this.leftArm.xRot = -(0.8f * adjustedPosCosine * speedModifier);
        this.leftArm.zRot = 0.0f;
        this.rightArm.xRot = -(0.8f * adjustedPosSine * speedModifier);
        this.rightArm.zRot = 0.0f;
        this.resetArmPoses();
    }

    private void resetArmPoses() {
        this.leftArm.yRot = 0.0f;
        this.leftArm.z = 1.0f;
        this.leftArm.x = 13.0f;
        this.leftArm.y = -13.0f;
        this.rightArm.yRot = 0.0f;
        this.rightArm.z = 1.0f;
        this.rightArm.x = -13.0f;
        this.rightArm.y = -13.0f;
    }

    private void animateTendrils(WardenRenderState state, float ageInTicks) {
        float tendrilXRot;
        this.leftTendril.xRot = tendrilXRot = state.tendrilAnimation * (float)(Math.cos((double)ageInTicks * 2.25) * Math.PI * (double)0.1f);
        this.rightTendril.xRot = -tendrilXRot;
    }
}

