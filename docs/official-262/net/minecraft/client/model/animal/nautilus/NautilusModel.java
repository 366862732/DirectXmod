/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.nautilus;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.NautilusAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class NautilusModel
extends EntityModel<NautilusRenderState> {
    private static final float SWIM_ANIMATION_SPEED_MAX = 2.0f;
    private static final float SWIM_ANIMATION_SCALE_FACTOR = 3.0f;
    private static final float IDLE_SWIM_ANIMATION_SPEED = 0.2f;
    private static final float IDLE_SWIM_ANIMATION_SCALE = 5.0f;
    protected final ModelPart body;
    protected final ModelPart nautilus;
    private final KeyframeAnimation swimAnimation;

    public NautilusModel(ModelPart root) {
        super(root);
        this.nautilus = root.getChild("root");
        this.body = this.nautilus.getChild("body");
        this.swimAnimation = NautilusAnimation.SWIMMING.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(NautilusModel.createBodyMesh(), 128, 128);
    }

    public static MeshDefinition createBodyMesh() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition nautilus = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0f, 29.0f, -6.0f));
        nautilus.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -10.0f, -7.0f, 14.0f, 10.0f, 16.0f, new CubeDeformation(0.0f)).texOffs(0, 26).addBox(-7.0f, 0.0f, -7.0f, 14.0f, 8.0f, 20.0f, new CubeDeformation(0.0f)).texOffs(48, 26).addBox(-7.0f, 0.0f, 6.0f, 14.0f, 8.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -13.0f, 5.0f));
        PartDefinition body = nautilus.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 54).addBox(-5.0f, -4.51f, -3.0f, 10.0f, 8.0f, 14.0f, new CubeDeformation(0.0f)).texOffs(0, 76).addBox(-5.0f, -4.51f, 7.0f, 10.0f, 8.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -8.5f, 12.3f));
        body.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(54, 54).addBox(-5.0f, -2.0f, 0.0f, 10.0f, 4.0f, 4.0f, new CubeDeformation(-0.001f)), PartPose.offset(0.0f, -2.51f, 7.0f));
        body.addOrReplaceChild("inner_mouth", CubeListBuilder.create().texOffs(54, 70).addBox(-3.0f, -2.0f, -0.5f, 6.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -0.51f, 7.5f));
        body.addOrReplaceChild("lower_mouth", CubeListBuilder.create().texOffs(54, 62).addBox(-5.0f, -1.98f, 0.0f, 10.0f, 4.0f, 4.0f, new CubeDeformation(-0.001f)), PartPose.offset(0.0f, 1.49f, 7.0f));
        return meshdefinition;
    }

    public static LayerDefinition createBabyBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition nautilus = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(-0.5f, 28.0f, -0.5f));
        nautilus.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0f, -4.0f, -1.0f, 7.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)).texOffs(0, 11).addBox(-6.0f, 0.0f, -1.0f, 7.0f, 4.0f, 9.0f, new CubeDeformation(0.0f)).texOffs(23, 11).addBox(-6.0f, 0.0f, 5.0f, 7.0f, 4.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(3.0f, -8.0f, -2.0f));
        PartDefinition body = nautilus.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 24).addBox(-2.5f, -3.01f, -1.0f, 5.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)).texOffs(0, 35).addBox(-2.5f, -3.01f, 4.1f, 5.0f, 4.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.5f, -5.0f, 3.0f));
        body.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(24, 24).addBox(-2.5f, -1.0f, 0.0f, 5.0f, 2.0f, 2.0f, new CubeDeformation(-0.001f)), PartPose.offset(0.0f, -2.01f, 3.9f));
        body.addOrReplaceChild("inner_mouth", CubeListBuilder.create().texOffs(24, 32).addBox(-1.5f, -1.0f, -1.0f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -1.01f, 4.9f));
        body.addOrReplaceChild("lower_mouth", CubeListBuilder.create().texOffs(24, 28).addBox(-2.5f, -1.0f, 0.0f, 5.0f, 2.0f, 2.0f, new CubeDeformation(-0.001f)), PartPose.offset(0.0f, -0.01f, 3.9f));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(NautilusRenderState state) {
        super.setupAnim(state);
        this.applyBodyRotation(state.yRot, state.xRot);
        this.swimAnimation.applyWalk(state.walkAnimationPos + state.ageInTicks / 5.0f, state.walkAnimationSpeed + 0.2f, 2.0f, 3.0f);
    }

    private void applyBodyRotation(float yRot, float xRot) {
        yRot = Mth.clamp(yRot, -10.0f, 10.0f);
        xRot = Mth.clamp(xRot, -10.0f, 10.0f);
        this.body.yRot = yRot * ((float)Math.PI / 180);
        this.body.xRot = xRot * ((float)Math.PI / 180);
    }
}

