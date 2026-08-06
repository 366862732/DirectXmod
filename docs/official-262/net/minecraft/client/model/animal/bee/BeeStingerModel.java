/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.bee;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

@Environment(value=EnvType.CLIENT)
public class BeeStingerModel
extends Model<Unit> {
    public BeeStingerModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder cross = CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -0.5f, 0.0f, 2.0f, 1.0f, 0.0f);
        root.addOrReplaceChild("cross_1", cross, PartPose.rotation(0.7853982f, 0.0f, 0.0f));
        root.addOrReplaceChild("cross_2", cross, PartPose.rotation(2.3561945f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 16, 16);
    }
}

