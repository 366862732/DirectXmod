/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 */
package net.minecraft.client.model.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class PlayerCapeModel
extends PlayerModel {
    private static final String CAPE = "cape";
    private final ModelPart cape;

    public PlayerCapeModel(ModelPart root) {
        super(root, false);
        this.cape = this.body.getChild(CAPE);
    }

    public static LayerDefinition createCapeLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot().clearRecursively();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild(CAPE, CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, 0.0f, -1.0f, 10.0f, 16.0f, 1.0f, CubeDeformation.NONE, 1.0f, 0.5f), PartPose.offsetAndRotation(0.0f, 0.0f, 2.0f, 0.0f, (float)Math.PI, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        this.cape.rotateBy(new Quaternionf().rotateY((float)(-Math.PI)).rotateX((6.0f + state.capeLean / 2.0f + state.capeFlap) * ((float)Math.PI / 180)).rotateZ(state.capeLean2 / 2.0f * ((float)Math.PI / 180)).rotateY((180.0f - state.capeLean2 / 2.0f) * ((float)Math.PI / 180)));
    }
}

