/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.skull;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModelBase;

@Environment(value=EnvType.CLIENT)
public class DragonHeadModel
extends SkullModelBase {
    private final ModelPart head;
    private final ModelPart jaw;

    public DragonHeadModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
    }

    public static LayerDefinition createHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float zo = -16.0f;
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().addBox("upper_lip", -6.0f, -1.0f, -24.0f, 12, 5, 16, 176, 44).addBox("upper_head", -8.0f, -8.0f, -10.0f, 16, 16, 16, 112, 30).mirror(true).addBox("scale", -5.0f, -12.0f, -4.0f, 2, 4, 6, 0, 0).addBox("nostril", -5.0f, -3.0f, -22.0f, 2, 2, 4, 112, 0).mirror(false).addBox("scale", 3.0f, -12.0f, -4.0f, 2, 4, 6, 0, 0).addBox("nostril", 3.0f, -3.0f, -22.0f, 2, 2, 4, 112, 0), PartPose.offset(0.0f, -7.986666f, 0.0f).scaled(0.75f));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(176, 65).addBox("jaw", -6.0f, 0.0f, -16.0f, 12.0f, 4.0f, 16.0f), PartPose.offset(0.0f, 4.0f, -8.0f));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(SkullModelBase.State state) {
        super.setupAnim(state);
        this.jaw.xRot = (float)(Math.sin(state.animationPos * (float)Math.PI * 0.2f) + 1.0) * 0.2f;
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
    }
}

