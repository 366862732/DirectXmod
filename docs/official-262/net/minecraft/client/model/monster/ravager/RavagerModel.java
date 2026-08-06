/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.ravager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class RavagerModel
extends EntityModel<RavagerRenderState> {
    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart neck;

    public RavagerModel(ModelPart root) {
        super(root);
        this.neck = root.getChild("neck");
        this.head = this.neck.getChild("head");
        this.mouth = this.head.getChild("mouth");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int legSize = 16;
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(68, 73).addBox(-5.0f, -1.0f, -18.0f, 10.0f, 10.0f, 18.0f), PartPose.offset(0.0f, -7.0f, 5.5f));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -20.0f, -14.0f, 16.0f, 20.0f, 16.0f).texOffs(0, 0).addBox(-2.0f, -6.0f, -18.0f, 4.0f, 8.0f, 4.0f), PartPose.offset(0.0f, 16.0f, -17.0f));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(74, 55).addBox(0.0f, -14.0f, -2.0f, 2.0f, 14.0f, 4.0f), PartPose.offsetAndRotation(-10.0f, -14.0f, -8.0f, 1.0995574f, 0.0f, 0.0f));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(74, 55).mirror().addBox(0.0f, -14.0f, -2.0f, 2.0f, 14.0f, 4.0f), PartPose.offsetAndRotation(8.0f, -14.0f, -8.0f, 1.0995574f, 0.0f, 0.0f));
        head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(0, 36).addBox(-8.0f, 0.0f, -16.0f, 16.0f, 3.0f, 16.0f), PartPose.offset(0.0f, -2.0f, 2.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 55).addBox(-7.0f, -10.0f, -7.0f, 14.0f, 16.0f, 20.0f).texOffs(0, 91).addBox(-6.0f, 6.0f, -7.0f, 12.0f, 13.0f, 18.0f), PartPose.offsetAndRotation(0.0f, 1.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(96, 0).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 37.0f, 8.0f), PartPose.offset(-8.0f, -13.0f, 18.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(96, 0).mirror().addBox(-4.0f, 0.0f, -4.0f, 8.0f, 37.0f, 8.0f), PartPose.offset(8.0f, -13.0f, 18.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 37.0f, 8.0f), PartPose.offset(-8.0f, -13.0f, -5.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(-4.0f, 0.0f, -4.0f, 8.0f, 37.0f, 8.0f), PartPose.offset(8.0f, -13.0f, -5.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(RavagerRenderState state) {
        super.setupAnim(state);
        float stunnedTick = state.stunnedTicksRemaining;
        float attackTick = state.attackTicksRemaining;
        int attackTime = 10;
        if (attackTick > 0.0f) {
            float headAnim = Mth.triangleWave(attackTick, 10.0f);
            float scaled = (1.0f + headAnim) * 0.5f;
            float headPos = scaled * scaled * scaled * 12.0f;
            float yOffset = headPos * Mth.sin(this.neck.xRot);
            this.neck.z = -6.5f + headPos;
            this.neck.y = -7.0f - yOffset;
            this.mouth.xRot = attackTick > 5.0f ? Mth.sin((-4.0f + attackTick) / 4.0f) * (float)Math.PI * 0.4f : 0.15707964f * Mth.sin((float)Math.PI * attackTick / 10.0f);
        } else {
            float headPos = -1.0f;
            float yOffset = -1.0f * Mth.sin(this.neck.xRot);
            this.neck.x = 0.0f;
            this.neck.y = -7.0f - yOffset;
            this.neck.z = 5.5f;
            boolean isStunned = stunnedTick > 0.0f;
            this.neck.xRot = isStunned ? 0.21991149f : 0.0f;
            this.mouth.xRot = (float)Math.PI * (isStunned ? 0.05f : 0.01f);
            if (isStunned) {
                double speed = (double)stunnedTick / 40.0;
                this.neck.x = (float)Math.sin(speed * 10.0) * 3.0f;
            } else if ((double)state.roarAnimation > 0.0) {
                float mouthAnim = Mth.sin(state.roarAnimation * (float)Math.PI * 0.25f);
                this.mouth.xRot = 1.5707964f * mouthAnim;
            }
        }
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        float animationPos = state.walkAnimationPos;
        float legRot = 0.4f * state.walkAnimationSpeed;
        this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * legRot;
        this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * legRot;
        this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * legRot;
        this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * legRot;
    }
}

