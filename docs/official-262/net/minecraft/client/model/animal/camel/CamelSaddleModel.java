/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.camel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.camel.AdultCamelModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CamelRenderState;

@Environment(value=EnvType.CLIENT)
public class CamelSaddleModel
extends AdultCamelModel {
    private static final String SADDLE = "saddle";
    private static final String BRIDLE = "bridle";
    private static final String REINS = "reins";
    private final ModelPart reins;

    public CamelSaddleModel(ModelPart root) {
        super(root);
        this.reins = this.head.getChild(REINS);
    }

    public static LayerDefinition createSaddleLayer() {
        MeshDefinition mesh = CamelSaddleModel.createBodyMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        PartDefinition head = body.getChild("head");
        CubeDeformation inflate = new CubeDeformation(0.05f);
        body.addOrReplaceChild(SADDLE, CubeListBuilder.create().texOffs(74, 64).addBox(-4.5f, -17.0f, -15.5f, 9.0f, 5.0f, 11.0f, inflate).texOffs(92, 114).addBox(-3.5f, -20.0f, -15.5f, 7.0f, 3.0f, 11.0f, inflate).texOffs(0, 89).addBox(-7.5f, -12.0f, -23.5f, 15.0f, 12.0f, 27.0f, inflate), PartPose.offset(0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild(REINS, CubeListBuilder.create().texOffs(98, 42).addBox(3.51f, -18.0f, -17.0f, 0.0f, 7.0f, 15.0f).texOffs(84, 57).addBox(-3.5f, -18.0f, -2.0f, 7.0f, 7.0f, 0.0f).texOffs(98, 42).addBox(-3.51f, -18.0f, -17.0f, 0.0f, 7.0f, 15.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild(BRIDLE, CubeListBuilder.create().texOffs(60, 87).addBox(-3.5f, -7.0f, -15.0f, 7.0f, 8.0f, 19.0f, inflate).texOffs(21, 64).addBox(-3.5f, -21.0f, -15.0f, 7.0f, 14.0f, 7.0f, inflate).texOffs(50, 64).addBox(-2.5f, -21.0f, -21.0f, 5.0f, 5.0f, 6.0f, inflate).texOffs(74, 70).addBox(2.5f, -19.0f, -18.0f, 1.0f, 2.0f, 2.0f).texOffs(74, 70).mirror().addBox(-3.5f, -19.0f, -18.0f, 1.0f, 2.0f, 2.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(CamelRenderState state) {
        super.setupAnim(state);
        this.reins.visible = state.isRidden;
    }
}

