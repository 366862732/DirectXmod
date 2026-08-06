/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.leash;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Environment(value=EnvType.CLIENT)
public class LeashKnotModel
extends EntityModel<EntityRenderState> {
    private static final String KNOT = "knot";
    private final ModelPart knot;

    public LeashKnotModel(ModelPart root) {
        super(root);
        this.knot = root.getChild(KNOT);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(KNOT, CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -8.0f, -3.0f, 6.0f, 8.0f, 6.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }
}

