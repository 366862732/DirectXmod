/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.axolotl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.BabyAxolotlAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;

@Environment(value=EnvType.CLIENT)
public class BabyAxolotlModel
extends EntityModel<AxolotlRenderState> {
    private static final float MAX_WALK_ANIMATION_SPEED = 15.0f;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 30.0f;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation walkUnderwaterAnimation;
    private final KeyframeAnimation swimAnimation;
    private final KeyframeAnimation idleOnGroundAnimation;
    private final KeyframeAnimation idleOnGroundUnderWaterAnimation;
    private final KeyframeAnimation idleUnderWaterAnimation;
    private final KeyframeAnimation playDeadAnimation;

    public BabyAxolotlModel(ModelPart root) {
        super(root);
        this.swimAnimation = BabyAxolotlAnimation.BABY_AXOLOTL_SWIM.bake(root);
        this.walkAnimation = BabyAxolotlAnimation.AXOLOTL_WALK_FLOOR.bake(root);
        this.walkUnderwaterAnimation = BabyAxolotlAnimation.WALK_FLOOR_UNDERWATER.bake(root);
        this.idleUnderWaterAnimation = BabyAxolotlAnimation.IDLE_UNDERWATER.bake(root);
        this.idleOnGroundUnderWaterAnimation = BabyAxolotlAnimation.IDLE_FLOOR_UNDERWATER.bake(root);
        this.idleOnGroundAnimation = BabyAxolotlAnimation.BABY_AXOLOTL_IDLE_FLOOR.bake(root);
        this.playDeadAnimation = BabyAxolotlAnimation.BABY_AXOLOTL_PLAY_DEAD.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0f, 24.0f, 0.0f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -0.75f, -2.75f, 4.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(0, 12).addBox(0.0f, -1.75f, -2.75f, 0.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -1.25f, 1.75f));
        body.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(20, 16).addBox(-3.0f, 0.0f, -0.5f, 3.0f, 0.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.0f, 0.25f, -1.25f));
        PartDefinition right_leg = body.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0f, 0.25f, 1.75f, 0.0f, 1.5708f, 1.5708f));
        right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(20, 14).addBox(0.0f, 0.0f, -0.5f, 3.0f, 0.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -1.5708f, 0.0f, 1.5708f));
        body.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(20, 13).addBox(0.0f, 0.0f, -0.5f, 3.0f, 0.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offset(2.0f, 0.25f, -1.25f));
        body.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(20, 14).addBox(0.0f, 0.0f, -0.5f, 3.0f, 0.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offset(2.0f, 0.25f, 1.75f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(10, 9).addBox(0.0f, -1.5f, -1.0f, 0.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -0.25f, 3.25f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-3.0f, -2.0f, -4.0f, 6.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.25f, -2.75f));
        head.addOrReplaceChild("left_gills", CubeListBuilder.create().texOffs(20, 8).addBox(0.0f, -3.5f, 0.0f, 3.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(3.0f, -0.5f, -2.0f));
        head.addOrReplaceChild("right_gills", CubeListBuilder.create().texOffs(20, 3).addBox(-3.0f, -3.5f, 0.0f, 3.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(-3.0f, -0.5f, -2.0f));
        head.addOrReplaceChild("top_gills", CubeListBuilder.create().texOffs(20, 0).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 3.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -2.0f, -2.0f));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(AxolotlRenderState state) {
        super.setupAnim(state);
        if (state.walkAnimationState.isStarted()) {
            this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 15.0f, 30.0f);
        }
        this.swimAnimation.apply(state.swimAnimation, state.ageInTicks);
        this.walkUnderwaterAnimation.apply(state.walkAnimationState, state.ageInTicks);
        this.idleOnGroundAnimation.apply(state.idleOnGroundAnimationState, state.ageInTicks);
        this.idleUnderWaterAnimation.apply(state.idleUnderWaterAnimationState, state.ageInTicks);
        this.idleOnGroundUnderWaterAnimation.apply(state.idleUnderWaterOnGroundAnimationState, state.ageInTicks);
        this.playDeadAnimation.apply(state.playDeadAnimationState, state.ageInTicks);
    }
}

