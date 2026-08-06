/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.frog;

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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class TadpoleModel
extends EntityModel<LivingEntityRenderState> {
    private final ModelPart tail;

    public TadpoleModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float xo = 0.0f;
        float yo = 22.0f;
        float zo = -3.0f;
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -1.0f, 0.0f, 3.0f, 2.0f, 3.0f), PartPose.offset(0.0f, 22.0f, -3.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -1.0f, 0.0f, 0.0f, 2.0f, 7.0f), PartPose.offset(0.0f, 22.0f, 0.0f));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float amplitudeMultiplier = state.isInWater ? 1.0f : 1.5f;
        this.tail.yRot = -amplitudeMultiplier * 0.25f * Mth.sin(0.3f * state.ageInTicks);
    }
}

