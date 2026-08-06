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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class SpinAttackEffectModel
extends EntityModel<AvatarRenderState> {
    private static final int BOX_COUNT = 2;
    private final ModelPart[] boxes = new ModelPart[2];

    public SpinAttackEffectModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 2; ++i) {
            this.boxes[i] = root.getChild(SpinAttackEffectModel.boxName(i));
        }
    }

    private static String boxName(int i) {
        return "box" + i;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 2; ++i) {
            float yOffset = -3.2f + 9.6f * (float)(i + 1);
            float scale = 0.75f * (float)(i + 1);
            root.addOrReplaceChild(SpinAttackEffectModel.boxName(i), CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -16.0f + yOffset, -8.0f, 16.0f, 32.0f, 16.0f), PartPose.ZERO.withScale(scale));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        for (int i = 0; i < this.boxes.length; ++i) {
            float angle = state.ageInTicks * (float)(-(45 + (i + 1) * 5));
            this.boxes[i].yRot = Mth.wrapDegrees(angle) * ((float)Math.PI / 180);
        }
    }
}

