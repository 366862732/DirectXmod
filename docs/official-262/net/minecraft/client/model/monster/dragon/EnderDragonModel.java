/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.dragon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;

@Environment(value=EnvType.CLIENT)
public class EnderDragonModel
extends EntityModel<EnderDragonRenderState> {
    private static final int NECK_PART_COUNT = 5;
    private static final int TAIL_PART_COUNT = 12;
    private final ModelPart head;
    private final ModelPart[] neckParts = new ModelPart[5];
    private final ModelPart[] tailParts = new ModelPart[12];
    private final ModelPart jaw;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart leftFrontLeg;
    private final ModelPart leftFrontLegTip;
    private final ModelPart leftFrontFoot;
    private final ModelPart leftRearLeg;
    private final ModelPart leftRearLegTip;
    private final ModelPart leftRearFoot;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart rightFrontLeg;
    private final ModelPart rightFrontLegTip;
    private final ModelPart rightFrontFoot;
    private final ModelPart rightRearLeg;
    private final ModelPart rightRearLegTip;
    private final ModelPart rightRearFoot;

    private static String neckName(int index) {
        return "neck" + index;
    }

    private static String tailName(int index) {
        return "tail" + index;
    }

    public EnderDragonModel(ModelPart root) {
        super(root);
        int i;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        for (i = 0; i < this.neckParts.length; ++i) {
            this.neckParts[i] = root.getChild(EnderDragonModel.neckName(i));
        }
        for (i = 0; i < this.tailParts.length; ++i) {
            this.tailParts[i] = root.getChild(EnderDragonModel.tailName(i));
        }
        this.body = root.getChild("body");
        this.leftWing = this.body.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.leftFrontLegTip = this.leftFrontLeg.getChild("left_front_leg_tip");
        this.leftFrontFoot = this.leftFrontLegTip.getChild("left_front_foot");
        this.leftRearLeg = this.body.getChild("left_hind_leg");
        this.leftRearLegTip = this.leftRearLeg.getChild("left_hind_leg_tip");
        this.leftRearFoot = this.leftRearLegTip.getChild("left_hind_foot");
        this.rightWing = this.body.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.rightFrontLegTip = this.rightFrontLeg.getChild("right_front_leg_tip");
        this.rightFrontFoot = this.rightFrontLegTip.getChild("right_front_foot");
        this.rightRearLeg = this.body.getChild("right_hind_leg");
        this.rightRearLegTip = this.rightRearLeg.getChild("right_hind_leg_tip");
        this.rightRearFoot = this.rightRearLegTip.getChild("right_hind_foot");
    }

    public static LayerDefinition createBodyLayer() {
        int i;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float zo = -16.0f;
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().addBox("upperlip", -6.0f, -1.0f, -24.0f, 12, 5, 16, 176, 44).addBox("upperhead", -8.0f, -8.0f, -10.0f, 16, 16, 16, 112, 30).mirror().addBox("scale", -5.0f, -12.0f, -4.0f, 2, 4, 6, 0, 0).addBox("nostril", -5.0f, -3.0f, -22.0f, 2, 2, 4, 112, 0).mirror().addBox("scale", 3.0f, -12.0f, -4.0f, 2, 4, 6, 0, 0).addBox("nostril", 3.0f, -3.0f, -22.0f, 2, 2, 4, 112, 0), PartPose.offset(0.0f, 20.0f, -62.0f));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().addBox("jaw", -6.0f, 0.0f, -16.0f, 12, 4, 16, 176, 65), PartPose.offset(0.0f, 4.0f, -8.0f));
        CubeListBuilder spineCubes = CubeListBuilder.create().addBox("box", -5.0f, -5.0f, -5.0f, 10, 10, 10, 192, 104).addBox("scale", -1.0f, -9.0f, -3.0f, 2, 4, 6, 48, 0);
        for (i = 0; i < 5; ++i) {
            root.addOrReplaceChild(EnderDragonModel.neckName(i), spineCubes, PartPose.offset(0.0f, 20.0f, -12.0f - (float)i * 10.0f));
        }
        for (i = 0; i < 12; ++i) {
            root.addOrReplaceChild(EnderDragonModel.tailName(i), spineCubes, PartPose.offset(0.0f, 10.0f, 60.0f + (float)i * 10.0f));
        }
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().addBox("body", -12.0f, 1.0f, -16.0f, 24, 24, 64, 0, 0).addBox("scale", -1.0f, -5.0f, -10.0f, 2, 6, 12, 220, 53).addBox("scale", -1.0f, -5.0f, 10.0f, 2, 6, 12, 220, 53).addBox("scale", -1.0f, -5.0f, 30.0f, 2, 6, 12, 220, 53), PartPose.offset(0.0f, 3.0f, 8.0f));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create().mirror().addBox("bone", 0.0f, -4.0f, -4.0f, 56, 8, 8, 112, 88).addBox("skin", 0.0f, 0.0f, 2.0f, 56, 0, 56, -56, 88), PartPose.offset(12.0f, 2.0f, -6.0f));
        leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().mirror().addBox("bone", 0.0f, -2.0f, -2.0f, 56, 4, 4, 112, 136).addBox("skin", 0.0f, 0.0f, 2.0f, 56, 0, 56, -56, 144), PartPose.offset(56.0f, 0.0f, 0.0f));
        PartDefinition leftFrontLeg = body.addOrReplaceChild("left_front_leg", CubeListBuilder.create().addBox("main", -4.0f, -4.0f, -4.0f, 8, 24, 8, 112, 104), PartPose.offsetAndRotation(12.0f, 17.0f, -6.0f, 1.3f, 0.0f, 0.0f));
        PartDefinition leftFrontLegTip = leftFrontLeg.addOrReplaceChild("left_front_leg_tip", CubeListBuilder.create().addBox("main", -3.0f, -1.0f, -3.0f, 6, 24, 6, 226, 138), PartPose.offsetAndRotation(0.0f, 20.0f, -1.0f, -0.5f, 0.0f, 0.0f));
        leftFrontLegTip.addOrReplaceChild("left_front_foot", CubeListBuilder.create().addBox("main", -4.0f, 0.0f, -12.0f, 8, 4, 16, 144, 104), PartPose.offsetAndRotation(0.0f, 23.0f, 0.0f, 0.75f, 0.0f, 0.0f));
        PartDefinition leftRearLeg = body.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().addBox("main", -8.0f, -4.0f, -8.0f, 16, 32, 16, 0, 0), PartPose.offsetAndRotation(16.0f, 13.0f, 34.0f, 1.0f, 0.0f, 0.0f));
        PartDefinition leftRearLegTip = leftRearLeg.addOrReplaceChild("left_hind_leg_tip", CubeListBuilder.create().addBox("main", -6.0f, -2.0f, 0.0f, 12, 32, 12, 196, 0), PartPose.offsetAndRotation(0.0f, 32.0f, -4.0f, 0.5f, 0.0f, 0.0f));
        leftRearLegTip.addOrReplaceChild("left_hind_foot", CubeListBuilder.create().addBox("main", -9.0f, 0.0f, -20.0f, 18, 6, 24, 112, 0), PartPose.offsetAndRotation(0.0f, 31.0f, 4.0f, 0.75f, 0.0f, 0.0f));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create().addBox("bone", -56.0f, -4.0f, -4.0f, 56, 8, 8, 112, 88).addBox("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56, -56, 88), PartPose.offset(-12.0f, 2.0f, -6.0f));
        rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().addBox("bone", -56.0f, -2.0f, -2.0f, 56, 4, 4, 112, 136).addBox("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56, -56, 144), PartPose.offset(-56.0f, 0.0f, 0.0f));
        PartDefinition rightFrontLeg = body.addOrReplaceChild("right_front_leg", CubeListBuilder.create().addBox("main", -4.0f, -4.0f, -4.0f, 8, 24, 8, 112, 104), PartPose.offsetAndRotation(-12.0f, 17.0f, -6.0f, 1.3f, 0.0f, 0.0f));
        PartDefinition rightFrontLegTip = rightFrontLeg.addOrReplaceChild("right_front_leg_tip", CubeListBuilder.create().addBox("main", -3.0f, -1.0f, -3.0f, 6, 24, 6, 226, 138), PartPose.offsetAndRotation(0.0f, 20.0f, -1.0f, -0.5f, 0.0f, 0.0f));
        rightFrontLegTip.addOrReplaceChild("right_front_foot", CubeListBuilder.create().addBox("main", -4.0f, 0.0f, -12.0f, 8, 4, 16, 144, 104), PartPose.offsetAndRotation(0.0f, 23.0f, 0.0f, 0.75f, 0.0f, 0.0f));
        PartDefinition rightRearLeg = body.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().addBox("main", -8.0f, -4.0f, -8.0f, 16, 32, 16, 0, 0), PartPose.offsetAndRotation(-16.0f, 13.0f, 34.0f, 1.0f, 0.0f, 0.0f));
        PartDefinition rightRearLegTip = rightRearLeg.addOrReplaceChild("right_hind_leg_tip", CubeListBuilder.create().addBox("main", -6.0f, -2.0f, 0.0f, 12, 32, 12, 196, 0), PartPose.offsetAndRotation(0.0f, 32.0f, -4.0f, 0.5f, 0.0f, 0.0f));
        rightRearLegTip.addOrReplaceChild("right_hind_foot", CubeListBuilder.create().addBox("main", -9.0f, 0.0f, -20.0f, 18, 6, 24, 112, 0), PartPose.offsetAndRotation(0.0f, 31.0f, 4.0f, 0.75f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(EnderDragonRenderState state) {
        super.setupAnim(state);
        float flapTime = state.flapTime * ((float)Math.PI * 2);
        this.jaw.xRot = (Mth.sin(flapTime) + 1.0f) * 0.2f;
        float bounce = Mth.sin(flapTime - 1.0f) + 1.0f;
        bounce = (bounce * bounce + bounce * 2.0f) * 0.05f;
        this.root.y = (bounce - 2.0f) * 16.0f;
        this.root.z = -48.0f;
        this.root.xRot = bounce * 2.0f * ((float)Math.PI / 180);
        float xx = this.neckParts[0].x;
        float yy = this.neckParts[0].y;
        float zz = this.neckParts[0].z;
        float rotScale = 1.5f;
        DragonFlightHistory.Sample start = state.getHistoricalPos(6);
        float rot2 = Mth.wrapDegrees(state.getHistoricalPos(5).yRot() - state.getHistoricalPos(10).yRot());
        float rot = Mth.wrapDegrees(state.getHistoricalPos(5).yRot() + rot2 / 2.0f);
        for (int i = 0; i < 5; ++i) {
            ModelPart neck = this.neckParts[i];
            DragonFlightHistory.Sample point = state.getHistoricalPos(5 - i);
            float neckXRot = Mth.cos((float)i * 0.45f + flapTime) * 0.15f;
            neck.yRot = Mth.wrapDegrees(point.yRot() - start.yRot()) * ((float)Math.PI / 180) * 1.5f;
            neck.xRot = neckXRot + state.getHeadPartYOffset(i, start, point) * ((float)Math.PI / 180) * 1.5f * 5.0f;
            neck.zRot = -Mth.wrapDegrees(point.yRot() - rot) * ((float)Math.PI / 180) * 1.5f;
            neck.y = yy;
            neck.z = zz;
            neck.x = xx;
            xx -= Mth.sin(neck.yRot) * Mth.cos(neck.xRot) * 10.0f;
            yy += Mth.sin(neck.xRot) * 10.0f;
            zz -= Mth.cos(neck.yRot) * Mth.cos(neck.xRot) * 10.0f;
        }
        this.head.y = yy;
        this.head.z = zz;
        this.head.x = xx;
        DragonFlightHistory.Sample current = state.getHistoricalPos(0);
        this.head.yRot = Mth.wrapDegrees(current.yRot() - start.yRot()) * ((float)Math.PI / 180);
        this.head.xRot = Mth.wrapDegrees(state.getHeadPartYOffset(6, start, current)) * ((float)Math.PI / 180) * 1.5f * 5.0f;
        this.head.zRot = -Mth.wrapDegrees(current.yRot() - rot) * ((float)Math.PI / 180);
        this.body.zRot = -rot2 * 1.5f * ((float)Math.PI / 180);
        this.leftWing.xRot = 0.125f - Mth.cos(flapTime) * 0.2f;
        this.leftWing.yRot = -0.25f;
        this.leftWing.zRot = -(Mth.sin(flapTime) + 0.125f) * 0.8f;
        this.leftWingTip.zRot = (Mth.sin(flapTime + 2.0f) + 0.5f) * 0.75f;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.zRot = -this.leftWing.zRot;
        this.rightWingTip.zRot = -this.leftWingTip.zRot;
        this.poseLimbs(bounce, this.leftFrontLeg, this.leftFrontLegTip, this.leftFrontFoot, this.leftRearLeg, this.leftRearLegTip, this.leftRearFoot);
        this.poseLimbs(bounce, this.rightFrontLeg, this.rightFrontLegTip, this.rightFrontFoot, this.rightRearLeg, this.rightRearLegTip, this.rightRearFoot);
        float tailXRot = 0.0f;
        yy = this.tailParts[0].y;
        zz = this.tailParts[0].z;
        xx = this.tailParts[0].x;
        start = state.getHistoricalPos(11);
        for (int i = 0; i < 12; ++i) {
            DragonFlightHistory.Sample point = state.getHistoricalPos(12 + i);
            ModelPart tail = this.tailParts[i];
            tail.yRot = (Mth.wrapDegrees(point.yRot() - start.yRot()) * 1.5f + 180.0f) * ((float)Math.PI / 180);
            tail.xRot = (tailXRot += Mth.sin((float)i * 0.45f + flapTime) * 0.05f) + (float)(point.y() - start.y()) * ((float)Math.PI / 180) * 1.5f * 5.0f;
            tail.zRot = Mth.wrapDegrees(point.yRot() - rot) * ((float)Math.PI / 180) * 1.5f;
            tail.y = yy;
            tail.z = zz;
            tail.x = xx;
            yy += Mth.sin(tail.xRot) * 10.0f;
            zz -= Mth.cos(tail.yRot) * Mth.cos(tail.xRot) * 10.0f;
            xx -= Mth.sin(tail.yRot) * Mth.cos(tail.xRot) * 10.0f;
        }
    }

    private void poseLimbs(float bounce, ModelPart frontLeg, ModelPart frontLegTip, ModelPart frontFoot, ModelPart rearLeg, ModelPart rearLegTip, ModelPart rearFoot) {
        rearLeg.xRot = 1.0f + bounce * 0.1f;
        rearLegTip.xRot = 0.5f + bounce * 0.1f;
        rearFoot.xRot = 0.75f + bounce * 0.1f;
        frontLeg.xRot = 1.3f + bounce * 0.1f;
        frontLegTip.xRot = -0.5f - bounce * 0.1f;
        frontFoot.xRot = 0.75f + bounce * 0.1f;
    }
}

