/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.shulker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class ShulkerModel
extends EntityModel<ShulkerRenderState> {
    public static final String LID = "lid";
    private static final String BASE = "base";
    private final ModelPart lid;
    private final ModelPart head;

    public ShulkerModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutZOffset);
        this.lid = root.getChild(LID);
        this.head = root.getChild("head");
    }

    private static MeshDefinition createShellMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -16.0f, -8.0f, 16.0f, 12.0f, 16.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        root.addOrReplaceChild(BASE, CubeListBuilder.create().texOffs(0, 28).addBox(-8.0f, -8.0f, -8.0f, 16.0f, 8.0f, 16.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        return mesh;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = ShulkerModel.createShellMesh();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 52).addBox(-3.0f, 0.0f, -3.0f, 6.0f, 6.0f, 6.0f), PartPose.offset(0.0f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createBoxLayer() {
        MeshDefinition mesh = ShulkerModel.createShellMesh();
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ShulkerRenderState state) {
        super.setupAnim(state);
        float bs = (0.5f + state.peekAmount) * (float)Math.PI;
        float q = -1.0f + Mth.sin(bs);
        float extra = 0.0f;
        if (bs > (float)Math.PI) {
            extra = Mth.sin(state.ageInTicks * 0.1f) * 0.7f;
        }
        this.lid.setPos(0.0f, 16.0f + Mth.sin(bs) * 8.0f + extra, 0.0f);
        this.lid.yRot = state.peekAmount > 0.3f ? q * q * q * q * (float)Math.PI * 0.125f : 0.0f;
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = (state.yHeadRot - 180.0f - state.yBodyRot) * ((float)Math.PI / 180);
    }
}

