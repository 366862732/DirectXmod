/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.parrot;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;

@Environment(value=EnvType.CLIENT)
public class ParrotModel
extends EntityModel<ParrotRenderState> {
    private static final String FEATHER = "feather";
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart head;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ParrotModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.head = root.getChild("head");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 8).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offsetAndRotation(0.0f, 16.5f, -3.0f, 0.4937f, 0.0f, 0.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(22, 1).addBox(-1.5f, -1.0f, -1.0f, 3.0f, 4.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 21.07f, 1.16f, 1.015f, 0.0f, 0.0f));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(19, 8).addBox(-0.5f, 0.0f, -1.5f, 1.0f, 5.0f, 3.0f), PartPose.offsetAndRotation(1.5f, 16.94f, -2.76f, -0.6981f, (float)(-Math.PI), 0.0f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(19, 8).addBox(-0.5f, 0.0f, -1.5f, 1.0f, 5.0f, 3.0f), PartPose.offsetAndRotation(-1.5f, 16.94f, -2.76f, -0.6981f, (float)(-Math.PI), 0.0f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0f, -1.5f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(0.0f, 15.69f, -2.76f));
        head.addOrReplaceChild("head2", CubeListBuilder.create().texOffs(10, 0).addBox(-1.0f, -0.5f, -2.0f, 2.0f, 1.0f, 4.0f), PartPose.offset(0.0f, -2.0f, -1.0f));
        head.addOrReplaceChild("beak1", CubeListBuilder.create().texOffs(11, 7).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f), PartPose.offset(0.0f, -0.5f, -1.5f));
        head.addOrReplaceChild("beak2", CubeListBuilder.create().texOffs(16, 7).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 2.0f, 1.0f), PartPose.offset(0.0f, -1.75f, -2.45f));
        head.addOrReplaceChild(FEATHER, CubeListBuilder.create().texOffs(2, 18).addBox(0.0f, -4.0f, -2.0f, 0.0f, 5.0f, 4.0f), PartPose.offsetAndRotation(0.0f, -2.15f, 0.15f, -0.2214f, 0.0f, 0.0f));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(14, 18).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 2.0f, 1.0f);
        root.addOrReplaceChild("left_leg", leg, PartPose.offsetAndRotation(1.0f, 22.0f, -1.05f, -0.0299f, 0.0f, 0.0f));
        root.addOrReplaceChild("right_leg", leg, PartPose.offsetAndRotation(-1.0f, 22.0f, -1.05f, -0.0299f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(ParrotRenderState state) {
        super.setupAnim(state);
        this.prepare(state.pose);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        switch (state.pose.ordinal()) {
            case 2: {
                break;
            }
            case 3: {
                float xPos = Mth.cos(state.ageInTicks);
                float yPos = Mth.sin(state.ageInTicks);
                this.head.x += xPos;
                this.head.y += yPos;
                this.head.xRot = 0.0f;
                this.head.yRot = 0.0f;
                this.head.zRot = Mth.sin(state.ageInTicks) * 0.4f;
                this.body.x += xPos;
                this.body.y += yPos;
                this.leftWing.zRot = -0.0873f - state.flapAngle;
                this.leftWing.x += xPos;
                this.leftWing.y += yPos;
                this.rightWing.zRot = 0.0873f + state.flapAngle;
                this.rightWing.x += xPos;
                this.rightWing.y += yPos;
                this.tail.x += xPos;
                this.tail.y += yPos;
                break;
            }
            case 1: {
                this.leftLeg.xRot += Mth.cos(state.walkAnimationPos * 0.6662f) * 1.4f * state.walkAnimationSpeed;
                this.rightLeg.xRot += Mth.cos(state.walkAnimationPos * 0.6662f + (float)Math.PI) * 1.4f * state.walkAnimationSpeed;
            }
            default: {
                float bobbingBody = state.flapAngle * 0.3f;
                this.head.y += bobbingBody;
                this.tail.xRot += Mth.cos(state.walkAnimationPos * 0.6662f) * 0.3f * state.walkAnimationSpeed;
                this.tail.y += bobbingBody;
                this.body.y += bobbingBody;
                this.leftWing.zRot = -0.0873f - state.flapAngle;
                this.leftWing.y += bobbingBody;
                this.rightWing.zRot = 0.0873f + state.flapAngle;
                this.rightWing.y += bobbingBody;
                this.leftLeg.y += bobbingBody;
                this.rightLeg.y += bobbingBody;
            }
        }
    }

    private void prepare(Pose pose) {
        switch (pose.ordinal()) {
            case 0: {
                this.leftLeg.xRot += 0.6981317f;
                this.rightLeg.xRot += 0.6981317f;
                break;
            }
            case 2: {
                float sittingYOffset = 1.9f;
                this.head.y += 1.9f;
                this.tail.xRot += 0.5235988f;
                this.tail.y += 1.9f;
                this.body.y += 1.9f;
                this.leftWing.zRot = -0.0873f;
                this.leftWing.y += 1.9f;
                this.rightWing.zRot = 0.0873f;
                this.rightWing.y += 1.9f;
                this.leftLeg.y += 1.9f;
                this.rightLeg.y += 1.9f;
                this.leftLeg.xRot += 1.5707964f;
                this.rightLeg.xRot += 1.5707964f;
                break;
            }
            case 3: {
                this.leftLeg.zRot = -0.34906584f;
                this.rightLeg.zRot = 0.34906584f;
                break;
            }
        }
    }

    public static Pose getPose(Parrot entity) {
        if (entity.isPartyParrot()) {
            return Pose.PARTY;
        }
        if (entity.isInSittingPose()) {
            return Pose.SITTING;
        }
        if (entity.isFlying()) {
            return Pose.FLYING;
        }
        return Pose.STANDING;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Pose {
        FLYING,
        STANDING,
        SITTING,
        PARTY,
        ON_SHOULDER;

    }
}

