/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.blaze;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BlazeModel
extends EntityModel<LivingEntityRenderState> {
    private final ModelPart[] upperBodyParts;
    private final ModelPart head;

    public BlazeModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.upperBodyParts = new ModelPart[12];
        Arrays.setAll(this.upperBodyParts, i -> root.getChild(BlazeModel.getPartName(i)));
    }

    private static String getPartName(int i) {
        return "part" + i;
    }

    public static LayerDefinition createBodyLayer() {
        float z;
        float y;
        float x;
        int i;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.ZERO);
        float angle = 0.0f;
        CubeListBuilder rod = CubeListBuilder.create().texOffs(0, 16).addBox(0.0f, 0.0f, 0.0f, 2.0f, 8.0f, 2.0f);
        for (i = 0; i < 4; ++i) {
            x = Mth.cos(angle) * 9.0f;
            y = -2.0f + Mth.cos((float)(i * 2) * 0.25f);
            z = Mth.sin(angle) * 9.0f;
            root.addOrReplaceChild(BlazeModel.getPartName(i), rod, PartPose.offset(x, y, z));
            angle += 1.5707964f;
        }
        angle = 0.7853982f;
        for (i = 4; i < 8; ++i) {
            x = Mth.cos(angle) * 7.0f;
            y = 2.0f + Mth.cos((float)(i * 2) * 0.25f);
            z = Mth.sin(angle) * 7.0f;
            root.addOrReplaceChild(BlazeModel.getPartName(i), rod, PartPose.offset(x, y, z));
            angle += 1.5707964f;
        }
        angle = 0.47123894f;
        for (i = 8; i < 12; ++i) {
            x = Mth.cos(angle) * 5.0f;
            y = 11.0f + Mth.cos((float)i * 1.5f * 0.5f);
            z = Mth.sin(angle) * 5.0f;
            root.addOrReplaceChild(BlazeModel.getPartName(i), rod, PartPose.offset(x, y, z));
            angle += 1.5707964f;
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        int i;
        super.setupAnim(state);
        float angle = state.ageInTicks * (float)Math.PI * -0.1f;
        for (i = 0; i < 4; ++i) {
            this.upperBodyParts[i].y = -2.0f + Mth.cos(((float)(i * 2) + state.ageInTicks) * 0.25f);
            this.upperBodyParts[i].x = Mth.cos(angle) * 9.0f;
            this.upperBodyParts[i].z = Mth.sin(angle) * 9.0f;
            angle += 1.5707964f;
        }
        angle = 0.7853982f + state.ageInTicks * (float)Math.PI * 0.03f;
        for (i = 4; i < 8; ++i) {
            this.upperBodyParts[i].y = 2.0f + Mth.cos(((float)(i * 2) + state.ageInTicks) * 0.25f);
            this.upperBodyParts[i].x = Mth.cos(angle) * 7.0f;
            this.upperBodyParts[i].z = Mth.sin(angle) * 7.0f;
            angle += 1.5707964f;
        }
        angle = 0.47123894f + state.ageInTicks * (float)Math.PI * -0.05f;
        for (i = 8; i < 12; ++i) {
            this.upperBodyParts[i].y = 11.0f + Mth.cos(((float)i * 1.5f + state.ageInTicks) * 0.5f);
            this.upperBodyParts[i].x = Mth.cos(angle) * 5.0f;
            this.upperBodyParts[i].z = Mth.sin(angle) * 5.0f;
            angle += 1.5707964f;
        }
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
    }
}

