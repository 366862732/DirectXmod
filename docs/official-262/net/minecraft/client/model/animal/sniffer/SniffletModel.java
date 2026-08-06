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
import net.minecraft.client.model.animal.sniffer.SnifferModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;

@Environment(value=EnvType.CLIENT)
public class SniffletModel
extends SnifferModel {
    private static final String LOWER_BEAK = "lower_beak";
    private final KeyframeAnimation babyTransform;

    public SniffletModel(ModelPart root) {
        super(root);
        this.babyTransform = SnifferAnimation.BABY_TRANSFORM.bake(root);
    }

    @Override
    public void setupAnim(SnifferRenderState state) {
        super.setupAnim(state);
        this.babyTransform.applyStatic();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0f, 24.0f, 0.0f));
        PartDefinition body = bone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 35).addBox(-13.0f, -14.0f, -0.5f, 14.0f, 14.0f, 20.0f, new CubeDeformation(0.25f)).texOffs(0, 0).addBox(-13.0f, -14.0f, -0.5f, 14.0f, 15.0f, 20.0f).texOffs(68, 0).addBox(-13.0f, 0.0f, -0.5f, 14.0f, 0.0f, 20.0f), PartPose.offset(6.0f, -3.0f, -9.5f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(68, 20).addBox(-5.0f, -4.25f, -7.5f, 10.0f, 9.0f, 9.0f).texOffs(88, 20).addBox(-5.0f, 3.75f, -7.5f, 10.0f, 0.0f, 9.0f), PartPose.offset(-6.0f, -4.75f, 0.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(104, 38).addBox(0.0f, 0.0f, -2.0f, 1.0f, 11.0f, 3.0f), PartPose.offset(5.0f, -4.25f, -1.5f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(96, 38).addBox(-1.0f, 0.0f, -2.0f, 1.0f, 11.0f, 3.0f), PartPose.offset(-5.0f, -4.25f, -1.5f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(68, 47).addBox(-5.0f, -3.0f, -2.0f, 10.0f, 3.0f, 4.0f), PartPose.offset(0.0f, -1.25f, -9.5f));
        head.addOrReplaceChild(LOWER_BEAK, CubeListBuilder.create().texOffs(68, 38).addBox(-5.0f, -2.5f, -2.0f, 10.0f, 5.0f, 4.0f), PartPose.offset(0.0f, 1.25f, -9.5f));
        bone.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 69).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 5.0f, 4.0f), PartPose.offset(-4.0f, -4.0f, -7.0f));
        bone.addOrReplaceChild("right_mid_leg", CubeListBuilder.create().texOffs(0, 78).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 5.0f, 4.0f), PartPose.offset(-4.0f, -4.0f, 0.0f));
        bone.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 87).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 5.0f, 4.0f), PartPose.offset(-4.0f, -4.0f, 7.0f));
        bone.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(16, 69).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 5.0f, 4.0f), PartPose.offset(4.0f, -4.0f, -7.0f));
        bone.addOrReplaceChild("left_mid_leg", CubeListBuilder.create().texOffs(16, 78).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 5.0f, 4.0f), PartPose.offset(4.0f, -4.0f, 0.0f));
        bone.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(16, 87).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 5.0f, 4.0f), PartPose.offset(4.0f, -4.0f, 7.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }
}

