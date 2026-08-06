/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.fish;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SalmonRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class SalmonModel
extends EntityModel<SalmonRenderState> {
    public static final MeshTransformer SMALL_TRANSFORMER = MeshTransformer.scaling(0.5f);
    public static final MeshTransformer LARGE_TRANSFORMER = MeshTransformer.scaling(1.5f);
    private static final String BODY_FRONT = "body_front";
    private static final String BODY_BACK = "body_back";
    private static final float Z_OFFSET = -7.2f;
    private final ModelPart bodyBack;

    public SalmonModel(ModelPart root) {
        super(root);
        this.bodyBack = root.getChild(BODY_BACK);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int yo = 20;
        PartDefinition bodyFront = root.addOrReplaceChild(BODY_FRONT, CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -2.5f, 0.0f, 3.0f, 5.0f, 8.0f), PartPose.offset(0.0f, 20.0f, -7.2f));
        PartDefinition bodyBack = root.addOrReplaceChild(BODY_BACK, CubeListBuilder.create().texOffs(0, 13).addBox(-1.5f, -2.5f, 0.0f, 3.0f, 5.0f, 8.0f), PartPose.offset(0.0f, 20.0f, 0.8000002f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0f, -2.0f, -3.0f, 2.0f, 4.0f, 3.0f), PartPose.offset(0.0f, 20.0f, -7.2f));
        bodyBack.addOrReplaceChild("back_fin", CubeListBuilder.create().texOffs(20, 10).addBox(0.0f, -2.5f, 0.0f, 0.0f, 5.0f, 6.0f), PartPose.offset(0.0f, 0.0f, 8.0f));
        bodyFront.addOrReplaceChild("top_front_fin", CubeListBuilder.create().texOffs(2, 1).addBox(0.0f, 0.0f, 0.0f, 0.0f, 2.0f, 3.0f), PartPose.offset(0.0f, -4.5f, 5.0f));
        bodyBack.addOrReplaceChild("top_back_fin", CubeListBuilder.create().texOffs(0, 2).addBox(0.0f, 0.0f, 0.0f, 0.0f, 2.0f, 4.0f), PartPose.offset(0.0f, -4.5f, -1.0f));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(-4, 0).addBox(-2.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation(-1.5f, 21.5f, -7.2f, 0.0f, 0.0f, -0.7853982f));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation(1.5f, 21.5f, -7.2f, 0.0f, 0.0f, 0.7853982f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(SalmonRenderState state) {
        super.setupAnim(state);
        float amplitudeMultiplier = 1.0f;
        float angleMultiplier = 1.0f;
        if (!state.isInWater) {
            amplitudeMultiplier = 1.3f;
            angleMultiplier = 1.7f;
        }
        this.bodyBack.yRot = -amplitudeMultiplier * 0.25f * Mth.sin(angleMultiplier * 0.6f * state.ageInTicks);
    }
}

