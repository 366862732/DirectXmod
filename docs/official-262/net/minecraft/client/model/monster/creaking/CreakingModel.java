/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.creaking;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.CreakingAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;

@Environment(value=EnvType.CLIENT)
public class CreakingModel
extends EntityModel<CreakingRenderState> {
    private final ModelPart head;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation invulnerableAnimation;
    private final KeyframeAnimation deathAnimation;

    public CreakingModel(ModelPart roots) {
        super(roots);
        ModelPart root = roots.getChild("root");
        ModelPart upperBody = root.getChild("upper_body");
        this.head = upperBody.getChild("head");
        this.walkAnimation = CreakingAnimation.CREAKING_WALK.bake(root);
        this.attackAnimation = CreakingAnimation.CREAKING_ATTACK.bake(root);
        this.invulnerableAnimation = CreakingAnimation.CREAKING_INVULNERABLE.bake(root);
        this.deathAnimation = CreakingAnimation.CREAKING_DEATH.bake(root);
    }

    private static MeshDefinition createMesh() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        PartDefinition root = partDefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0f, 24.0f, 0.0f));
        PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0f, -19.0f, 0.0f));
        upperBody.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -10.0f, -3.0f, 6.0f, 10.0f, 6.0f).texOffs(28, 31).addBox(-3.0f, -13.0f, -3.0f, 6.0f, 3.0f, 6.0f).texOffs(12, 40).addBox(3.0f, -13.0f, 0.0f, 9.0f, 14.0f, 0.0f).texOffs(34, 12).addBox(-12.0f, -14.0f, 0.0f, 9.0f, 14.0f, 0.0f), PartPose.offset(-3.0f, -11.0f, 0.0f));
        upperBody.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(0.0f, -3.0f, -3.0f, 6.0f, 13.0f, 5.0f).texOffs(24, 0).addBox(-6.0f, -4.0f, -3.0f, 6.0f, 7.0f, 5.0f), PartPose.offset(0.0f, -7.0f, 1.0f));
        upperBody.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(22, 13).addBox(-2.0f, -1.5f, -1.5f, 3.0f, 21.0f, 3.0f).texOffs(46, 0).addBox(-2.0f, 19.5f, -1.5f, 3.0f, 4.0f, 3.0f), PartPose.offset(-7.0f, -9.5f, 1.5f));
        upperBody.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(30, 40).addBox(0.0f, -1.0f, -1.5f, 3.0f, 16.0f, 3.0f).texOffs(52, 12).addBox(0.0f, -5.0f, -1.5f, 3.0f, 4.0f, 3.0f).texOffs(52, 19).addBox(0.0f, 15.0f, -1.5f, 3.0f, 4.0f, 3.0f), PartPose.offset(6.0f, -9.0f, 0.5f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(42, 40).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 16.0f, 3.0f).texOffs(45, 55).addBox(-1.5f, 15.7f, -4.5f, 5.0f, 0.0f, 9.0f), PartPose.offset(1.5f, -16.0f, 0.5f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-3.0f, -1.5f, -1.5f, 3.0f, 19.0f, 3.0f).texOffs(45, 46).addBox(-5.0f, 17.2f, -4.5f, 5.0f, 0.0f, 9.0f).texOffs(12, 34).addBox(-3.0f, -4.5f, -1.5f, 3.0f, 3.0f, 3.0f), PartPose.offset(-1.0f, -17.5f, 0.5f));
        return meshDefinition;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = CreakingModel.createMesh();
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createEyesLayer() {
        MeshDefinition mesh = CreakingModel.createMesh();
        mesh.getRoot().retainExactParts(Set.of("head"));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CreakingRenderState state) {
        super.setupAnim(state);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        if (state.canMove) {
            this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.0f, 1.0f);
        }
        this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
        this.invulnerableAnimation.apply(state.invulnerabilityAnimationState, state.ageInTicks);
        this.deathAnimation.apply(state.deathAnimationState, state.ageInTicks);
    }
}

