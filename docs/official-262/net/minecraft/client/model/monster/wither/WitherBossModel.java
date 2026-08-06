/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.wither;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class WitherBossModel
extends EntityModel<WitherRenderState> {
    private static final String RIBCAGE = "ribcage";
    private static final String CENTER_HEAD = "center_head";
    private static final String RIGHT_HEAD = "right_head";
    private static final String LEFT_HEAD = "left_head";
    private static final float RIBCAGE_X_ROT_OFFSET = 0.065f;
    private static final float TAIL_X_ROT_OFFSET = 0.265f;
    private final ModelPart centerHead;
    private final ModelPart rightHead;
    private final ModelPart leftHead;
    private final ModelPart ribcage;
    private final ModelPart tail;

    public WitherBossModel(ModelPart root) {
        super(root);
        this.ribcage = root.getChild(RIBCAGE);
        this.tail = root.getChild("tail");
        this.centerHead = root.getChild(CENTER_HEAD);
        this.rightHead = root.getChild(RIGHT_HEAD);
        this.leftHead = root.getChild(LEFT_HEAD);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(0, 16).addBox(-10.0f, 3.9f, -0.5f, 20.0f, 3.0f, 3.0f, g), PartPose.ZERO);
        float ribcageXRot = 0.20420352f;
        root.addOrReplaceChild(RIBCAGE, CubeListBuilder.create().texOffs(0, 22).addBox(0.0f, 0.0f, 0.0f, 3.0f, 10.0f, 3.0f, g).texOffs(24, 22).addBox(-4.0f, 1.5f, 0.5f, 11.0f, 2.0f, 2.0f, g).texOffs(24, 22).addBox(-4.0f, 4.0f, 0.5f, 11.0f, 2.0f, 2.0f, g).texOffs(24, 22).addBox(-4.0f, 6.5f, 0.5f, 11.0f, 2.0f, 2.0f, g), PartPose.offsetAndRotation(-2.0f, 6.9f, -0.5f, 0.20420352f, 0.0f, 0.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(12, 22).addBox(0.0f, 0.0f, 0.0f, 3.0f, 6.0f, 3.0f, g), PartPose.offsetAndRotation(-2.0f, 6.9f + Mth.cos(0.2042035162448883) * 10.0f, -0.5f + Mth.sin(0.2042035162448883) * 10.0f, 0.83252203f, 0.0f, 0.0f));
        root.addOrReplaceChild(CENTER_HEAD, CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f, g), PartPose.ZERO);
        CubeListBuilder sideHead = CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -4.0f, -4.0f, 6.0f, 6.0f, 6.0f, g);
        root.addOrReplaceChild(RIGHT_HEAD, sideHead, PartPose.offset(-8.0f, 4.0f, 0.0f));
        root.addOrReplaceChild(LEFT_HEAD, sideHead, PartPose.offset(10.0f, 4.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WitherRenderState state) {
        super.setupAnim(state);
        WitherBossModel.setupHeadRotation(state, this.rightHead, 0);
        WitherBossModel.setupHeadRotation(state, this.leftHead, 1);
        float anim = Mth.cos(state.ageInTicks * 0.1f);
        this.ribcage.xRot = (0.065f + 0.05f * anim) * (float)Math.PI;
        this.tail.setPos(-2.0f, 6.9f + Mth.cos(this.ribcage.xRot) * 10.0f, -0.5f + Mth.sin(this.ribcage.xRot) * 10.0f);
        this.tail.xRot = (0.265f + 0.1f * anim) * (float)Math.PI;
        this.centerHead.yRot = state.yRot * ((float)Math.PI / 180);
        this.centerHead.xRot = state.xRot * ((float)Math.PI / 180);
    }

    private static void setupHeadRotation(WitherRenderState state, ModelPart head, int headIndex) {
        head.yRot = (state.yHeadRots[headIndex] - state.bodyRot) * ((float)Math.PI / 180);
        head.xRot = state.xHeadRots[headIndex] * ((float)Math.PI / 180);
    }
}

