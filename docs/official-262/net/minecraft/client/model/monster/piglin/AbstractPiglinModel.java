/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.piglin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractPiglinModel<S extends HumanoidRenderState>
extends HumanoidModel<S> {
    protected static final float ADULT_EAR_ANGLE_IN_DEGREES = 30.0f;
    protected static final float BABY_EAR_ANGLE_IN_DEGREES = 5.0f;
    public final ModelPart rightEar;
    public final ModelPart leftEar;

    public AbstractPiglinModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
    }

    public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation innerDeformation, CubeDeformation outerDeformation) {
        return PlayerModel.createArmorMeshSet(innerDeformation, outerDeformation).map(AbstractPiglinModel::removeEars);
    }

    private static MeshDefinition removeEars(MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    public static PartDefinition addHead(CubeDeformation g, MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -8.0f, -4.0f, 10.0f, 8.0f, 8.0f, g).texOffs(31, 1).addBox(-2.0f, -4.0f, -5.0f, 4.0f, 4.0f, 1.0f, g).texOffs(2, 4).addBox(2.0f, -2.0f, -5.0f, 1.0f, 2.0f, 1.0f, g).texOffs(2, 0).addBox(-3.0f, -2.0f, -5.0f, 1.0f, 2.0f, 1.0f, g), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(51, 6).addBox(0.0f, 0.0f, -2.0f, 1.0f, 5.0f, 4.0f, g), PartPose.offsetAndRotation(4.5f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(39, 6).addBox(-1.0f, 0.0f, -2.0f, 1.0f, 5.0f, 4.0f, g), PartPose.offsetAndRotation(-4.5f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));
        return head;
    }

    public static ArmorModelSet<MeshDefinition> createBabyArmorMeshSet(CubeDeformation innerDeformation, CubeDeformation outerDeformation, PartPose armOffset) {
        return PlayerModel.createBabyArmorMeshSet(innerDeformation, outerDeformation, armOffset).map(AbstractPiglinModel::removeEars);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        float animationPos = ((HumanoidRenderState)state).walkAnimationPos;
        float animationSpeed = ((HumanoidRenderState)state).walkAnimationSpeed;
        float defaultAngle = this.getDefaultEarAngleInDegrees() * ((float)Math.PI / 180);
        float frequency = ((HumanoidRenderState)state).ageInTicks * 0.1f + animationPos * 0.5f;
        float amplitude = 0.08f + animationSpeed * 0.4f;
        this.leftEar.zRot = -defaultAngle - Mth.cos(frequency * 1.2f) * amplitude;
        this.rightEar.zRot = defaultAngle + Mth.cos(frequency) * amplitude;
    }

    protected abstract float getDefaultEarAngleInDegrees();
}

