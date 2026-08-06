/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.slime;

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
import net.minecraft.client.renderer.entity.state.SlimeRenderState;

@Environment(value=EnvType.CLIENT)
public class MagmaCubeModel
extends EntityModel<SlimeRenderState> {
    private static final int SEGMENT_COUNT = 8;
    private final ModelPart[] bodyCubes = new ModelPart[8];

    public MagmaCubeModel(ModelPart root) {
        super(root);
        Arrays.setAll(this.bodyCubes, i -> root.getChild(MagmaCubeModel.getSegmentName(i)));
    }

    private static String getSegmentName(int i) {
        return "cube" + i;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 8; ++i) {
            int u = 0;
            int v = 0;
            if (i > 0 && i < 4) {
                v += 9 * i;
            } else if (i > 3) {
                u = 32;
                v += 9 * i - 36;
            }
            root.addOrReplaceChild(MagmaCubeModel.getSegmentName(i), CubeListBuilder.create().texOffs(u, v).addBox(-4.0f, 16 + i, -4.0f, 8.0f, 1.0f, 8.0f), PartPose.ZERO);
        }
        root.addOrReplaceChild("inside_cube", CubeListBuilder.create().texOffs(24, 40).addBox(-2.0f, 18.0f, -2.0f, 4.0f, 4.0f, 4.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SlimeRenderState state) {
        super.setupAnim(state);
        float slimeSquish = Math.max(0.0f, state.squish);
        for (int i = 0; i < this.bodyCubes.length; ++i) {
            this.bodyCubes[i].y = (float)(-(4 - i)) * slimeSquish * 1.7f;
        }
    }
}

