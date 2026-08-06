/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.squid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabySquidModel
extends SquidModel {
    public BabySquidModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition root = meshdefinition.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -5.0f, -4.0f, 8.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 13.0f, 0.0f));
        int tentacleCount = 8;
        CubeListBuilder tentacle = CubeListBuilder.create().texOffs(0, 18).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 6.0f, 2.0f);
        for (int i = 0; i < 8; ++i) {
            double angle = (double)i * Math.PI * 2.0 / 8.0;
            float x = (float)Math.cos(angle) * 3.0f;
            float y = 18.5f;
            float z = (float)Math.sin(angle) * 3.0f;
            angle = (double)i * Math.PI * -2.0 / 8.0 + 1.5707963267948966;
            float yRot = (float)angle;
            root.addOrReplaceChild(BabySquidModel.createTentacleName(i), tentacle, PartPose.offsetAndRotation(x, 18.5f, z, 0.0f, yRot, 0.0f));
        }
        return LayerDefinition.create(meshdefinition, 32, 32);
    }
}

