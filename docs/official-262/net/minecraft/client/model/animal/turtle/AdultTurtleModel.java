/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.turtle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.turtle.TurtleModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Environment(value=EnvType.CLIENT)
public class AdultTurtleModel
extends TurtleModel {
    private static final String EGG_BELLY = "egg_belly";
    private final ModelPart eggBelly;

    public AdultTurtleModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.eggBelly = root.getChild(EGG_BELLY);
    }

    @Override
    public void setupAnim(TurtleRenderState state) {
        super.setupAnim(state);
        this.eggBelly.visible = state.hasEgg;
        if (this.eggBelly.visible) {
            this.root.y -= 1.0f;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(3, 0).addBox(-3.0f, -1.0f, -3.0f, 6.0f, 5.0f, 6.0f), PartPose.offset(0.0f, 19.0f, -10.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(7, 37).addBox("shell", -9.5f, 3.0f, -10.0f, 19.0f, 20.0f, 6.0f).texOffs(31, 1).addBox("belly", -5.5f, 3.0f, -13.0f, 11.0f, 18.0f, 3.0f), PartPose.offsetAndRotation(0.0f, 11.0f, -10.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild(EGG_BELLY, CubeListBuilder.create().texOffs(70, 33).addBox(-4.5f, 3.0f, -14.0f, 9.0f, 18.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 11.0f, -10.0f, 1.5707964f, 0.0f, 0.0f));
        boolean legHeight = true;
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(1, 23).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 10.0f), PartPose.offset(-3.5f, 22.0f, 11.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(1, 12).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 10.0f), PartPose.offset(3.5f, 22.0f, 11.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(27, 30).addBox(-13.0f, 0.0f, -2.0f, 13.0f, 1.0f, 5.0f), PartPose.offset(-5.0f, 21.0f, -4.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(27, 24).addBox(0.0f, 0.0f, -2.0f, 13.0f, 1.0f, 5.0f), PartPose.offset(5.0f, 21.0f, -4.0f));
        return LayerDefinition.create(mesh, 128, 64);
    }
}

