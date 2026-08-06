/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.ghast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.GhastRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class GhastModel
extends EntityModel<GhastRenderState> {
    private final ModelPart[] tentacles = new ModelPart[9];

    public GhastModel(ModelPart root) {
        super(root);
        for (int i = 0; i < this.tentacles.length; ++i) {
            this.tentacles[i] = root.getChild(PartNames.tentacle(i));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -8.0f, -8.0f, 16.0f, 16.0f, 16.0f), PartPose.offset(0.0f, 17.6f, 0.0f));
        RandomSource random = RandomSource.createThreadLocalInstance(1660L);
        for (int i = 0; i < 9; ++i) {
            float xo = (((float)(i % 3) - (float)(i / 3 % 2) * 0.5f + 0.25f) / 2.0f * 2.0f - 1.0f) * 5.0f;
            float yo = ((float)(i / 3) / 2.0f * 2.0f - 1.0f) * 5.0f;
            int len = random.nextInt(7) + 8;
            root.addOrReplaceChild(PartNames.tentacle(i), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, len, 2.0f), PartPose.offset(xo, 24.6f, yo));
        }
        return LayerDefinition.create(mesh, 64, 32).apply(MeshTransformer.scaling(4.5f));
    }

    @Override
    public void setupAnim(GhastRenderState state) {
        super.setupAnim(state);
        GhastModel.animateTentacles(state, this.tentacles);
    }

    public static void animateTentacles(EntityRenderState state, ModelPart[] tentacles) {
        for (int i = 0; i < tentacles.length; ++i) {
            tentacles[i].xRot = 0.2f * Mth.sin(state.ageInTicks * 0.3f + (float)i) + 0.4f;
        }
    }
}

