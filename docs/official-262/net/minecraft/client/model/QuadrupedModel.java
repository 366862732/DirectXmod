/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class QuadrupedModel<T extends LivingEntityRenderState>
extends EntityModel<T> {
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightHindLeg;
    protected final ModelPart leftHindLeg;
    protected final ModelPart rightFrontLeg;
    protected final ModelPart leftFrontLeg;

    protected QuadrupedModel(ModelPart root) {
        this(root, RenderTypes::entityCutout);
    }

    protected QuadrupedModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static MeshDefinition createBodyMesh(int legSize, boolean mirrorLeftLeg, boolean mirrorRightLeg, CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -8.0f, 8.0f, 8.0f, 8.0f, g), PartPose.offset(0.0f, 18 - legSize, -6.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8).addBox(-5.0f, -10.0f, -7.0f, 10.0f, 16.0f, 8.0f, g), PartPose.offsetAndRotation(0.0f, 17 - legSize, 2.0f, 1.5707964f, 0.0f, 0.0f));
        QuadrupedModel.createLegs(root, mirrorLeftLeg, mirrorRightLeg, legSize, g);
        return mesh;
    }

    public static void createLegs(PartDefinition root, boolean mirrorLeftLeg, boolean mirrorRightLeg, int legSize, CubeDeformation g) {
        CubeListBuilder rightLeg = CubeListBuilder.create().mirror(mirrorRightLeg).texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, (float)legSize, 4.0f, g);
        CubeListBuilder leftLeg = CubeListBuilder.create().mirror(mirrorLeftLeg).texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, (float)legSize, 4.0f, g);
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-3.0f, 24 - legSize, 7.0f));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(3.0f, 24 - legSize, 7.0f));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-3.0f, 24 - legSize, -5.0f));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(3.0f, 24 - legSize, -5.0f));
    }

    @Override
    public void setupAnim(T state) {
        super.setupAnim(state);
        this.head.xRot = ((LivingEntityRenderState)state).xRot * ((float)Math.PI / 180);
        this.head.yRot = ((LivingEntityRenderState)state).yRot * ((float)Math.PI / 180);
        float animationPos = ((LivingEntityRenderState)state).walkAnimationPos;
        float animationSpeed = ((LivingEntityRenderState)state).walkAnimationSpeed;
        this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
    }
}

