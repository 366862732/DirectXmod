/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.ghast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.ghast.GhastModel;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;

@Environment(value=EnvType.CLIENT)
public class HappyGhastModel
extends EntityModel<HappyGhastRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.2375f);
    private static final float BODY_SQUEEZE = 0.9375f;
    private final ModelPart[] tentacles = new ModelPart[9];
    private final ModelPart body;

    public HappyGhastModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        for (int i = 0; i < this.tentacles.length; ++i) {
            this.tentacles[i] = this.body.getChild(PartNames.tentacle(i));
        }
    }

    public static LayerDefinition createBodyLayer(boolean isBaby, CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -8.0f, -8.0f, 16.0f, 16.0f, 16.0f, deformation), PartPose.offset(0.0f, 16.0f, 0.0f));
        if (isBaby) {
            body.addOrReplaceChild("inner_body", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0f, -16.0f, -8.0f, 16.0f, 16.0f, 16.0f, deformation.extend(-0.5f)), PartPose.offset(0.0f, 8.0f, 0.0f));
        }
        body.addOrReplaceChild(PartNames.tentacle(0), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, deformation), PartPose.offset(-3.75f, 7.0f, -5.0f));
        body.addOrReplaceChild(PartNames.tentacle(1), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f, deformation), PartPose.offset(1.25f, 7.0f, -5.0f));
        body.addOrReplaceChild(PartNames.tentacle(2), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, deformation), PartPose.offset(6.25f, 7.0f, -5.0f));
        body.addOrReplaceChild(PartNames.tentacle(3), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, deformation), PartPose.offset(-6.25f, 7.0f, 0.0f));
        body.addOrReplaceChild(PartNames.tentacle(4), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, deformation), PartPose.offset(-1.25f, 7.0f, 0.0f));
        body.addOrReplaceChild(PartNames.tentacle(5), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f, deformation), PartPose.offset(3.75f, 7.0f, 0.0f));
        body.addOrReplaceChild(PartNames.tentacle(6), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, deformation), PartPose.offset(-3.75f, 7.0f, 5.0f));
        body.addOrReplaceChild(PartNames.tentacle(7), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, deformation), PartPose.offset(1.25f, 7.0f, 5.0f));
        body.addOrReplaceChild(PartNames.tentacle(8), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, deformation), PartPose.offset(6.25f, 7.0f, 5.0f));
        return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(4.0f));
    }

    @Override
    public void setupAnim(HappyGhastRenderState state) {
        super.setupAnim(state);
        if (!state.bodyItem.isEmpty()) {
            this.body.xScale = 0.9375f;
            this.body.yScale = 0.9375f;
            this.body.zScale = 0.9375f;
        }
        GhastModel.animateTentacles(state, this.tentacles);
    }
}

