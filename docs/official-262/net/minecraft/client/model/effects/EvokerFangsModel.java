/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class EvokerFangsModel
extends EntityModel<EvokerFangsRenderState> {
    private static final String BASE = "base";
    private static final String UPPER_JAW = "upper_jaw";
    private static final String LOWER_JAW = "lower_jaw";
    private final ModelPart base;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;

    public EvokerFangsModel(ModelPart root) {
        super(root);
        this.base = root.getChild(BASE);
        this.upperJaw = this.base.getChild(UPPER_JAW);
        this.lowerJaw = this.base.getChild(LOWER_JAW);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild(BASE, CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 0.0f, 0.0f, 10.0f, 12.0f, 10.0f), PartPose.offset(-5.0f, 24.0f, -5.0f));
        CubeListBuilder jaw = CubeListBuilder.create().texOffs(40, 0).addBox(0.0f, 0.0f, 0.0f, 4.0f, 14.0f, 8.0f);
        base.addOrReplaceChild(UPPER_JAW, jaw, PartPose.offsetAndRotation(6.5f, 0.0f, 1.0f, 0.0f, 0.0f, 2.042035f));
        base.addOrReplaceChild(LOWER_JAW, jaw, PartPose.offsetAndRotation(3.5f, 0.0f, 9.0f, 0.0f, (float)Math.PI, 4.2411504f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EvokerFangsRenderState state) {
        super.setupAnim(state);
        float biteProgress = state.biteProgress;
        float biteAmount = Math.min(biteProgress * 2.0f, 1.0f);
        biteAmount = 1.0f - biteAmount * biteAmount * biteAmount;
        this.upperJaw.zRot = (float)Math.PI - biteAmount * 0.35f * (float)Math.PI;
        this.lowerJaw.zRot = (float)Math.PI + biteAmount * 0.35f * (float)Math.PI;
        this.base.y -= (biteProgress + Mth.sin(biteProgress * 2.7f)) * 7.2f;
        float preScale = 1.0f;
        if (biteProgress > 0.9f) {
            preScale *= (1.0f - biteProgress) / 0.1f;
        }
        this.root.y = 24.0f - 20.0f * preScale;
        this.root.xScale = preScale;
        this.root.yScale = preScale;
        this.root.zScale = preScale;
    }
}

