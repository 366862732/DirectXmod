/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.sniffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.SnifferAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;

@Environment(value=EnvType.CLIENT)
public class SnifferModel
extends EntityModel<SnifferRenderState> {
    private static final float WALK_ANIMATION_SPEED_MAX = 9.0f;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 100.0f;
    private final ModelPart head;
    private final KeyframeAnimation sniffSearchAnimation;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation digAnimation;
    private final KeyframeAnimation longSniffAnimation;
    private final KeyframeAnimation standUpAnimation;
    private final KeyframeAnimation happyAnimation;
    private final KeyframeAnimation sniffSniffAnimation;

    public SnifferModel(ModelPart root) {
        super(root);
        this.head = root.getChild("bone").getChild("body").getChild("head");
        this.sniffSearchAnimation = SnifferAnimation.SNIFFER_SNIFF_SEARCH.bake(root);
        this.walkAnimation = SnifferAnimation.SNIFFER_WALK.bake(root);
        this.digAnimation = SnifferAnimation.SNIFFER_DIG.bake(root);
        this.longSniffAnimation = SnifferAnimation.SNIFFER_LONGSNIFF.bake(root);
        this.standUpAnimation = SnifferAnimation.SNIFFER_STAND_UP.bake(root);
        this.happyAnimation = SnifferAnimation.SNIFFER_HAPPY.bake(root);
        this.sniffSniffAnimation = SnifferAnimation.SNIFFER_SNIFFSNIFF.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0f, 5.0f, 0.0f));
        PartDefinition body = bone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(62, 68).addBox(-12.5f, -14.0f, -20.0f, 25.0f, 29.0f, 40.0f, new CubeDeformation(0.0f)).texOffs(62, 0).addBox(-12.5f, -14.0f, -20.0f, 25.0f, 24.0f, 40.0f, new CubeDeformation(0.5f)).texOffs(87, 68).addBox(-12.5f, 12.0f, -20.0f, 25.0f, 0.0f, 40.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        bone.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(32, 87).addBox(-3.5f, -1.0f, -4.0f, 7.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(-7.5f, 10.0f, -15.0f));
        bone.addOrReplaceChild("right_mid_leg", CubeListBuilder.create().texOffs(32, 105).addBox(-3.5f, -1.0f, -4.0f, 7.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(-7.5f, 10.0f, 0.0f));
        bone.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(32, 123).addBox(-3.5f, -1.0f, -4.0f, 7.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(-7.5f, 10.0f, 15.0f));
        bone.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 87).addBox(-3.5f, -1.0f, -4.0f, 7.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(7.5f, 10.0f, -15.0f));
        bone.addOrReplaceChild("left_mid_leg", CubeListBuilder.create().texOffs(0, 105).addBox(-3.5f, -1.0f, -4.0f, 7.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(7.5f, 10.0f, 0.0f));
        bone.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 123).addBox(-3.5f, -1.0f, -4.0f, 7.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(7.5f, 10.0f, 15.0f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(8, 15).addBox(-6.5f, -7.5f, -11.5f, 13.0f, 18.0f, 11.0f, new CubeDeformation(0.0f)).texOffs(8, 4).addBox(-6.5f, 7.5f, -11.5f, 13.0f, 0.0f, 11.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 6.5f, -19.48f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(2, 0).addBox(0.0f, 0.0f, -3.0f, 1.0f, 19.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offset(6.51f, -7.5f, -4.51f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0f, 0.0f, -3.0f, 1.0f, 19.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offset(-6.51f, -7.5f, -4.51f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(10, 45).addBox(-6.5f, -2.0f, -9.0f, 13.0f, 2.0f, 9.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -4.5f, -11.5f));
        head.addOrReplaceChild("lower_beak", CubeListBuilder.create().texOffs(10, 57).addBox(-6.5f, -7.0f, -8.0f, 13.0f, 12.0f, 9.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 2.5f, -12.5f));
        return LayerDefinition.create(mesh, 192, 192);
    }

    @Override
    public void setupAnim(SnifferRenderState state) {
        super.setupAnim(state);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        if (state.isSearching) {
            this.sniffSearchAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 9.0f, 100.0f);
        } else {
            this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 9.0f, 100.0f);
        }
        this.digAnimation.apply(state.diggingAnimationState, state.ageInTicks);
        this.longSniffAnimation.apply(state.sniffingAnimationState, state.ageInTicks);
        this.standUpAnimation.apply(state.risingAnimationState, state.ageInTicks);
        this.happyAnimation.apply(state.feelingHappyAnimationState, state.ageInTicks);
        this.sniffSniffAnimation.apply(state.scentingAnimationState, state.ageInTicks);
    }
}

