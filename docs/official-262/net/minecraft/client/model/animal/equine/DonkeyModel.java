/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.equine;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;

@Environment(value=EnvType.CLIENT)
public class DonkeyModel
extends AbstractEquineModel<DonkeyRenderState> {
    public static final float DONKEY_SCALE = 0.87f;
    public static final float MULE_SCALE = 0.92f;
    private static final MeshTransformer DONKEY_TRANSFORMER = mesh -> {
        DonkeyModel.modifyMesh(mesh.getRoot());
        return mesh;
    };
    private final ModelPart leftChest;
    private final ModelPart rightChest;

    public DonkeyModel(ModelPart root) {
        super(root);
        this.leftChest = this.body.getChild("left_chest");
        this.rightChest = this.body.getChild("right_chest");
    }

    public DonkeyModel(ModelPart root, ModelPart headParts, ModelPart rightHindLeg, ModelPart rightFrontLeg, ModelPart leftHindLeg, ModelPart leftFrontLeg, ModelPart tail) {
        super(root, headParts, rightHindLeg, rightFrontLeg, leftHindLeg, leftFrontLeg, tail);
        this.leftChest = this.body.getChild("left_chest");
        this.rightChest = this.body.getChild("right_chest");
    }

    public static LayerDefinition createBodyLayer(float scale) {
        return LayerDefinition.create(AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64).apply(DONKEY_TRANSFORMER).apply(MeshTransformer.scaling(scale));
    }

    public static LayerDefinition createSaddleLayer(float scale) {
        return EquineSaddleModel.createSaddleLayer().apply(DONKEY_TRANSFORMER).apply(MeshTransformer.scaling(scale));
    }

    private static void modifyMesh(PartDefinition root) {
        PartDefinition body = root.getChild("body");
        CubeListBuilder chest = CubeListBuilder.create().texOffs(26, 21).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 8.0f, 3.0f);
        body.addOrReplaceChild("left_chest", chest, PartPose.offsetAndRotation(6.0f, -8.0f, 0.0f, 0.0f, -1.5707964f, 0.0f));
        body.addOrReplaceChild("right_chest", chest, PartPose.offsetAndRotation(-6.0f, -8.0f, 0.0f, 0.0f, 1.5707964f, 0.0f));
        PartDefinition head = root.getChild("head_parts").getChild("head");
        CubeListBuilder ear = CubeListBuilder.create().texOffs(0, 12).addBox(-1.0f, -7.0f, 0.0f, 2.0f, 7.0f, 1.0f);
        head.addOrReplaceChild("left_ear", ear, PartPose.offsetAndRotation(1.25f, -10.0f, 4.0f, 0.2617994f, 0.0f, 0.2617994f));
        head.addOrReplaceChild("right_ear", ear, PartPose.offsetAndRotation(-1.25f, -10.0f, 4.0f, 0.2617994f, 0.0f, -0.2617994f));
    }

    @Override
    public void setupAnim(DonkeyRenderState state) {
        super.setupAnim(state);
        this.leftChest.visible = state.hasChest;
        this.rightChest.visible = state.hasChest;
    }
}

