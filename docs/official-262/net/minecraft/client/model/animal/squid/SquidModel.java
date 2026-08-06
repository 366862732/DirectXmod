/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.squid;

import java.util.Arrays;
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
import net.minecraft.client.renderer.entity.state.SquidRenderState;

@Environment(value=EnvType.CLIENT)
public class SquidModel
extends EntityModel<SquidRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5f);
    private final ModelPart[] tentacles = new ModelPart[8];

    public SquidModel(ModelPart root) {
        super(root);
        Arrays.setAll(this.tentacles, i -> root.getChild(SquidModel.createTentacleName(i)));
    }

    protected static String createTentacleName(int i) {
        return "tentacle" + i;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation g = new CubeDeformation(0.02f);
        int yoffs = -16;
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0f, -8.0f, -6.0f, 12.0f, 16.0f, 12.0f, g), PartPose.offset(0.0f, 8.0f, 0.0f));
        int tentacleCount = 8;
        CubeListBuilder tentacle = CubeListBuilder.create().texOffs(48, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 18.0f, 2.0f);
        for (int i = 0; i < 8; ++i) {
            double angle = (double)i * Math.PI * 2.0 / 8.0;
            float x = (float)Math.cos(angle) * 5.0f;
            float y = 15.0f;
            float z = (float)Math.sin(angle) * 5.0f;
            angle = (double)i * Math.PI * -2.0 / 8.0 + 1.5707963267948966;
            float yRot = (float)angle;
            root.addOrReplaceChild(SquidModel.createTentacleName(i), tentacle, PartPose.offsetAndRotation(x, 15.0f, z, 0.0f, yRot, 0.0f));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SquidRenderState state) {
        super.setupAnim(state);
        for (ModelPart tentacle : this.tentacles) {
            tentacle.xRot = state.tentacleAngle;
        }
    }
}

