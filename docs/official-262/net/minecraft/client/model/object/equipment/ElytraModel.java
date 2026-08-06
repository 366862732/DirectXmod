/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.equipment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

@Environment(value=EnvType.CLIENT)
public class ElytraModel
extends EntityModel<HumanoidRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5f);
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ElytraModel(ModelPart root) {
        super(root);
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation windDeformation = new CubeDeformation(1.0f);
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(22, 0).addBox(-10.0f, 0.0f, 0.0f, 10.0f, 20.0f, 2.0f, windDeformation), PartPose.offsetAndRotation(5.0f, 0.0f, 0.0f, 0.2617994f, 0.0f, -0.2617994f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0f, 0.0f, 0.0f, 10.0f, 20.0f, 2.0f, windDeformation), PartPose.offsetAndRotation(-5.0f, 0.0f, 0.0f, 0.2617994f, 0.0f, 0.2617994f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        this.leftWing.y = state.isCrouching ? 3.0f : 0.0f;
        this.leftWing.xRot = state.elytraRotX;
        this.leftWing.zRot = state.elytraRotZ;
        this.leftWing.yRot = state.elytraRotY;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.y = this.leftWing.y;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.zRot = -this.leftWing.zRot;
    }
}

