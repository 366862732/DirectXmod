/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.player;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.HumanoidArm;

@Environment(value=EnvType.CLIENT)
public class PlayerModel
extends HumanoidModel<AvatarRenderState> {
    protected static final String LEFT_SLEEVE = "left_sleeve";
    protected static final String RIGHT_SLEEVE = "right_sleeve";
    protected static final String LEFT_PANTS = "left_pants";
    protected static final String RIGHT_PANTS = "right_pants";
    private final List<ModelPart> bodyParts;
    public final ModelPart leftSleeve;
    public final ModelPart rightSleeve;
    public final ModelPart leftPants;
    public final ModelPart rightPants;
    public final ModelPart jacket;
    private final boolean slim;

    public PlayerModel(ModelPart root, boolean slim) {
        super(root, RenderTypes::entityTranslucent);
        this.slim = slim;
        this.leftSleeve = this.leftArm.getChild(LEFT_SLEEVE);
        this.rightSleeve = this.rightArm.getChild(RIGHT_SLEEVE);
        this.leftPants = this.leftLeg.getChild(LEFT_PANTS);
        this.rightPants = this.rightLeg.getChild(RIGHT_PANTS);
        this.jacket = this.body.getChild("jacket");
        this.bodyParts = List.of(this.head, this.body, this.leftArm, this.rightArm, this.leftLeg, this.rightLeg);
    }

    public static MeshDefinition createMesh(CubeDeformation scale, boolean slim) {
        MeshDefinition mesh = HumanoidModel.createMesh(scale, 0.0f);
        PartDefinition root = mesh.getRoot();
        float overlayScale = 0.25f;
        if (slim) {
            leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, scale), PartPose.offset(5.0f, 2.0f, 0.0f));
            rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, scale), PartPose.offset(-5.0f, 2.0f, 0.0f));
            leftArm.addOrReplaceChild(LEFT_SLEEVE, CubeListBuilder.create().texOffs(48, 48).addBox(-1.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
            rightArm.addOrReplaceChild(RIGHT_SLEEVE, CubeListBuilder.create().texOffs(40, 32).addBox(-2.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
        } else {
            leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, scale), PartPose.offset(5.0f, 2.0f, 0.0f));
            rightArm = root.getChild("right_arm");
            leftArm.addOrReplaceChild(LEFT_SLEEVE, CubeListBuilder.create().texOffs(48, 48).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
            rightArm.addOrReplaceChild(RIGHT_SLEEVE, CubeListBuilder.create().texOffs(40, 32).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
        }
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, scale), PartPose.offset(1.9f, 12.0f, 0.0f));
        PartDefinition rightLeg = root.getChild("right_leg");
        leftLeg.addOrReplaceChild(LEFT_PANTS, CubeListBuilder.create().texOffs(0, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
        rightLeg.addOrReplaceChild(RIGHT_PANTS, CubeListBuilder.create().texOffs(0, 32).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, scale.extend(0.25f)), PartPose.ZERO);
        return mesh;
    }

    public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation innerDeformation, CubeDeformation outerDeformation) {
        return HumanoidModel.createArmorMeshSet(innerDeformation, outerDeformation).map(mesh -> {
            PartDefinition root = mesh.getRoot();
            PartDefinition leftArm = root.getChild("left_arm");
            PartDefinition rightArm = root.getChild("right_arm");
            leftArm.addOrReplaceChild(LEFT_SLEEVE, CubeListBuilder.create(), PartPose.ZERO);
            rightArm.addOrReplaceChild(RIGHT_SLEEVE, CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition leftLeg = root.getChild("left_leg");
            PartDefinition rightLeg = root.getChild("right_leg");
            leftLeg.addOrReplaceChild(LEFT_PANTS, CubeListBuilder.create(), PartPose.ZERO);
            rightLeg.addOrReplaceChild(RIGHT_PANTS, CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition body = root.getChild("body");
            body.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
            return mesh;
        });
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        boolean showBody;
        this.body.visible = showBody = !state.isSpectator;
        this.rightArm.visible = showBody;
        this.leftArm.visible = showBody;
        this.rightLeg.visible = showBody;
        this.leftLeg.visible = showBody;
        this.hat.visible = state.showHat;
        this.jacket.visible = state.showJacket;
        this.leftPants.visible = state.showLeftPants;
        this.rightPants.visible = state.showRightPants;
        this.leftSleeve.visible = state.showLeftSleeve;
        this.rightSleeve.visible = state.showRightSleeve;
        super.setupAnim(state);
    }

    @Override
    public void translateToHand(AvatarRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart part = this.getArm(arm);
        if (this.slim) {
            float offset = 0.5f * (float)(arm == HumanoidArm.RIGHT ? 1 : -1);
            part.x += offset;
            part.translateAndRotate(poseStack);
            part.x -= offset;
        } else {
            part.translateAndRotate(poseStack);
        }
    }

    public ModelPart getRandomBodyPart(RandomSource random) {
        return Util.getRandom(this.bodyParts, random);
    }
}

