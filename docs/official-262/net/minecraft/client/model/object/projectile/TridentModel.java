/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.projectile;

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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

@Environment(value=EnvType.CLIENT)
public class TridentModel
extends Model<Unit> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/trident/trident.png");

    public TridentModel(ModelPart root) {
        super(root, RenderTypes::entitySolid);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition pole = root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(0, 6).addBox(-0.5f, 2.0f, -0.5f, 1.0f, 25.0f, 1.0f), PartPose.ZERO);
        pole.addOrReplaceChild("base", CubeListBuilder.create().texOffs(4, 0).addBox(-1.5f, 0.0f, -0.5f, 3.0f, 2.0f, 1.0f), PartPose.ZERO);
        pole.addOrReplaceChild("left_spike", CubeListBuilder.create().texOffs(4, 3).addBox(-2.5f, -3.0f, -0.5f, 1.0f, 4.0f, 1.0f), PartPose.ZERO);
        pole.addOrReplaceChild("middle_spike", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f), PartPose.ZERO);
        pole.addOrReplaceChild("right_spike", CubeListBuilder.create().texOffs(4, 3).mirror().addBox(1.5f, -3.0f, -0.5f, 1.0f, 4.0f, 1.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }
}

