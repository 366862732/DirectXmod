/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.equine;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractEquineModel<T extends EquineRenderState>
extends EntityModel<T> {
    private static final float DEG_125 = 2.1816616f;
    private static final float DEG_60 = 1.0471976f;
    private static final float DEG_45 = 0.7853982f;
    private static final float DEG_30 = 0.5235988f;
    private static final float DEG_15 = 0.2617994f;
    protected static final String HEAD_PARTS = "head_parts";
    protected final ModelPart body;
    protected final ModelPart headParts;
    protected final ModelPart rightHindLeg;
    protected final ModelPart leftHindLeg;
    protected final ModelPart rightFrontLeg;
    protected final ModelPart leftFrontLeg;
    private final ModelPart tail;

    public AbstractEquineModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.headParts = root.getChild(HEAD_PARTS);
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = this.body.getChild("tail");
    }

    public AbstractEquineModel(ModelPart root, ModelPart headParts, ModelPart rightHindLeg, ModelPart rightFrontLeg, ModelPart leftHindLeg, ModelPart leftFrontLeg, ModelPart tail) {
        super(root);
        this.body = root.getChild("body");
        this.headParts = headParts;
        this.rightHindLeg = rightHindLeg;
        this.leftHindLeg = leftHindLeg;
        this.rightFrontLeg = rightFrontLeg;
        this.leftFrontLeg = leftFrontLeg;
        this.tail = tail;
    }

    public static MeshDefinition createBodyMesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 32).addBox(-5.0f, -8.0f, -17.0f, 10.0f, 10.0f, 22.0f, new CubeDeformation(0.05f)), PartPose.offset(0.0f, 11.0f, 5.0f));
        PartDefinition headParts = root.addOrReplaceChild(HEAD_PARTS, CubeListBuilder.create().texOffs(0, 35).addBox(-2.05f, -6.0f, -2.0f, 4.0f, 12.0f, 7.0f), PartPose.offsetAndRotation(0.0f, 4.0f, -12.0f, 0.5235988f, 0.0f, 0.0f));
        PartDefinition head = headParts.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0f, -11.0f, -2.0f, 6.0f, 5.0f, 7.0f, g), PartPose.ZERO);
        headParts.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(56, 36).addBox(-1.0f, -11.0f, 5.01f, 2.0f, 16.0f, 2.0f, g), PartPose.ZERO);
        headParts.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0f, -11.0f, -7.0f, 4.0f, 5.0f, 5.0f, g), PartPose.ZERO);
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0f, -1.01f, -1.0f, 4.0f, 11.0f, 4.0f, g), PartPose.offset(4.0f, 14.0f, 7.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(48, 21).addBox(-1.0f, -1.01f, -1.0f, 4.0f, 11.0f, 4.0f, g), PartPose.offset(-4.0f, 14.0f, 7.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0f, -1.01f, -1.9f, 4.0f, 11.0f, 4.0f, g), PartPose.offset(4.0f, 14.0f, -10.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(48, 21).addBox(-1.0f, -1.01f, -1.9f, 4.0f, 11.0f, 4.0f, g), PartPose.offset(-4.0f, 14.0f, -10.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(42, 36).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 14.0f, 4.0f, g), PartPose.offsetAndRotation(0.0f, -5.0f, 2.0f, 0.5235988f, 0.0f, 0.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(19, 16).addBox(0.55f, -13.0f, 4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(-0.001f)), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(19, 16).addBox(-2.55f, -13.0f, 4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(-0.001f)), PartPose.ZERO);
        return mesh;
    }

    @Override
    public void setupAnim(T state) {
        super.setupAnim(state);
        float clampedYRot = Mth.clamp(((EquineRenderState)state).yRot, -20.0f, 20.0f);
        float headRotXRad = ((EquineRenderState)state).xRot * ((float)Math.PI / 180);
        float animationSpeed = ((EquineRenderState)state).walkAnimationSpeed;
        float animationPos = ((EquineRenderState)state).walkAnimationPos;
        if (animationSpeed > 0.2f) {
            headRotXRad += Mth.cos(animationPos * 0.8f) * 0.15f * animationSpeed;
        }
        float eating = ((EquineRenderState)state).eatAnimation;
        float standing = ((EquineRenderState)state).standAnimation;
        float iStanding = 1.0f - standing;
        float feedingAnim = ((EquineRenderState)state).feedingAnimation;
        boolean animateTail = ((EquineRenderState)state).animateTail;
        this.headParts.xRot = 0.5235988f + headRotXRad;
        this.headParts.yRot = clampedYRot * ((float)Math.PI / 180);
        float waterMultiplier = ((EquineRenderState)state).isInWater ? 0.2f : 1.0f;
        float legAnim1 = Mth.cos(waterMultiplier * animationPos * 0.6662f + (float)Math.PI);
        float legXRotAnim = legAnim1 * 0.8f * animationSpeed;
        float baseHeadAngle = (1.0f - Math.max(standing, eating)) * (0.5235988f + headRotXRad + feedingAnim * Mth.sin(((EquineRenderState)state).ageInTicks) * 0.05f);
        this.headParts.xRot = standing * (0.2617994f + headRotXRad) + eating * (2.1816616f + Mth.sin(((EquineRenderState)state).ageInTicks) * 0.05f) + baseHeadAngle;
        this.headParts.yRot = standing * clampedYRot * ((float)Math.PI / 180) + (1.0f - Math.max(standing, eating)) * this.headParts.yRot;
        this.animateHeadPartsPlacement(eating, standing);
        this.body.xRot = standing * -0.7853982f + iStanding * this.body.xRot;
        this.leftFrontLeg.y -= this.getLegStandingYOffset() * standing;
        this.leftFrontLeg.z += this.getLegStandingZOffset() * standing;
        this.rightFrontLeg.y = this.leftFrontLeg.y;
        this.rightFrontLeg.z = this.leftFrontLeg.z;
        float standAngle = this.getLegStandAngle() * standing;
        float bobValue = Mth.cos(((EquineRenderState)state).ageInTicks * 0.6f + (float)Math.PI);
        float legStandingXRotOffset = this.getLegStandingXRotOffset();
        float rlegRot = (legStandingXRotOffset + bobValue) * standing + legXRotAnim * iStanding;
        float llegRot = (legStandingXRotOffset - bobValue) * standing - legXRotAnim * iStanding;
        this.leftHindLeg.xRot = standAngle - legAnim1 * 0.5f * animationSpeed * iStanding;
        this.rightHindLeg.xRot = standAngle + legAnim1 * 0.5f * animationSpeed * iStanding;
        this.leftFrontLeg.xRot = rlegRot;
        this.rightFrontLeg.xRot = llegRot;
        this.offsetLegPositionWhenStanding(standing);
        float ageScale = ((EquineRenderState)state).ageScale;
        this.tail.xRot = this.getTailXRotOffset() + 0.5235988f + animationSpeed * 0.75f;
        this.tail.y += animationSpeed * ageScale;
        this.tail.z += animationSpeed * 2.0f * ageScale;
        this.tail.yRot = animateTail ? Mth.cos(((EquineRenderState)state).ageInTicks * 0.7f) : 0.0f;
    }

    protected void offsetLegPositionWhenStanding(float standing) {
    }

    protected float getLegStandAngle() {
        return 0.2617994f;
    }

    protected float getLegStandingYOffset() {
        return 12.0f;
    }

    protected float getLegStandingZOffset() {
        return 4.0f;
    }

    protected float getLegStandingXRotOffset() {
        return -1.0471976f;
    }

    protected float getTailXRotOffset() {
        return 0.0f;
    }

    protected void animateHeadPartsPlacement(float eating, float standing) {
        this.headParts.y += Mth.lerp(eating, Mth.lerp(standing, 0.0f, -8.0f), 7.0f);
        this.headParts.z = Mth.lerp(standing, this.headParts.z, -4.0f);
    }
}

