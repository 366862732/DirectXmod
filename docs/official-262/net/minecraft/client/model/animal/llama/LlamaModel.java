/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.llama;

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
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class LlamaModel
extends EntityModel<LlamaRenderState> {
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightChest;
    private final ModelPart leftChest;

    public LlamaModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightChest = root.getChild("right_chest");
        this.leftChest = root.getChild("left_chest");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -14.0f, -10.0f, 4.0f, 4.0f, 9.0f, g).texOffs(0, 14).addBox("neck", -4.0f, -16.0f, -6.0f, 8.0f, 18.0f, 6.0f, g).texOffs(17, 0).addBox("ear", -4.0f, -19.0f, -4.0f, 3.0f, 3.0f, 2.0f, g).texOffs(17, 0).addBox("ear", 1.0f, -19.0f, -4.0f, 3.0f, 3.0f, 2.0f, g), PartPose.offset(0.0f, 7.0f, -6.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(29, 0).addBox(-6.0f, -10.0f, -7.0f, 12.0f, 18.0f, 10.0f, g), PartPose.offsetAndRotation(0.0f, 5.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild("right_chest", CubeListBuilder.create().texOffs(45, 28).addBox(-3.0f, 0.0f, 0.0f, 8.0f, 8.0f, 3.0f, g), PartPose.offsetAndRotation(-8.5f, 3.0f, 3.0f, 0.0f, 1.5707964f, 0.0f));
        root.addOrReplaceChild("left_chest", CubeListBuilder.create().texOffs(45, 41).addBox(-3.0f, 0.0f, 0.0f, 8.0f, 8.0f, 3.0f, g), PartPose.offsetAndRotation(5.5f, 3.0f, 3.0f, 0.0f, 1.5707964f, 0.0f));
        int legWidth = 4;
        int legHeight = 14;
        CubeListBuilder leg = CubeListBuilder.create().texOffs(29, 29).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 14.0f, 4.0f, g);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.5f, 10.0f, 6.0f));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(3.5f, 10.0f, 6.0f));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.5f, 10.0f, -5.0f));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(3.5f, 10.0f, -5.0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LlamaRenderState state) {
        super.setupAnim(state);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        this.rightChest.visible = state.hasChest;
        this.leftChest.visible = state.hasChest;
    }
}

