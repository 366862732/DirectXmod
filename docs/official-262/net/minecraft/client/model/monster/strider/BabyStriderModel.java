/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.strider;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.strider.StriderModel;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BabyStriderModel
extends StriderModel {
    private static final String FRONT_BRISTLE = "bristle2";
    private static final String MIDDLE_BRISTLE = "bristle1";
    private static final String BACK_BRISTLE = "bristle0";
    private final ModelPart frontBristle;
    private final ModelPart middleBristle;
    private final ModelPart bottomBristle;

    public BabyStriderModel(ModelPart root) {
        super(root);
        this.frontBristle = this.body.getChild(FRONT_BRISTLE);
        this.middleBristle = this.body.getChild(MIDDLE_BRISTLE);
        this.bottomBristle = this.body.getChild(BACK_BRISTLE);
    }

    @Override
    protected void customAnimations(float animationPos, float animationSpeed, float ageInTicks) {
        this.body.y = 17.25f;
        this.body.y -= 1.0f * Mth.cos(animationPos * 1.5f) * 2.0f * animationSpeed;
        this.leftLeg.y = 20.0f + 2.0f * Mth.sin(animationPos * 1.5f * 0.5f + (float)Math.PI) * 2.0f * animationSpeed;
        this.rightLeg.y = 20.0f + 2.0f * Mth.sin(animationPos * 1.5f * 0.5f) * 2.0f * animationSpeed;
        float bristleFlow = Mth.cos(animationPos * 1.5f + (float)Math.PI) * animationSpeed;
        this.animateBristle(ageInTicks, bristleFlow, this.frontBristle, this.middleBristle, this.bottomBristle, (modelPart, rotation) -> modelPart.xRot += rotation.floatValue());
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.75f, -4.0f, 7.0f, 7.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 16.75f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-1.5f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 24).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(1.5f, 20.0f, 0.0f));
        body.addOrReplaceChild(BACK_BRISTLE, CubeListBuilder.create().texOffs(0, 21).addBox(-3.5f, -2.5f, 0.0f, 7.0f, 3.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -4.25f, 2.0f));
        body.addOrReplaceChild(MIDDLE_BRISTLE, CubeListBuilder.create().texOffs(0, 18).addBox(-3.5f, -2.5f, 0.0f, 7.0f, 3.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -4.25f, 0.0f));
        body.addOrReplaceChild(FRONT_BRISTLE, CubeListBuilder.create().texOffs(0, 15).addBox(-3.5f, -2.5f, 0.0f, 7.0f, 3.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -4.25f, -2.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }
}

