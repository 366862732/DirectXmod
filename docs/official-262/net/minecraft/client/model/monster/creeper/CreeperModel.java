/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.creeper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class CreeperModel
extends EntityModel<CreeperRenderState> {
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private static final int Y_OFFSET = 6;

    public CreeperModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftHindLeg = root.getChild("right_hind_leg");
        this.rightHindLeg = root.getChild("left_hind_leg");
        this.leftFrontLeg = root.getChild("right_front_leg");
        this.rightFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, g), PartPose.offset(0.0f, 6.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, g), PartPose.offset(0.0f, 6.0f, 0.0f));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, g);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.0f, 18.0f, 4.0f));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.0f, 18.0f, 4.0f));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.0f, 18.0f, -4.0f));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.0f, 18.0f, -4.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(CreeperRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
    }
}

