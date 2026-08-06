/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyDrownedModel;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class DrownedRenderer
extends AbstractZombieRenderer<Drowned, ZombieRenderState, DrownedModel> {
    private static final Identifier DROWNED_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned.png");
    private static final Identifier BABY_DROWNED_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_baby.png");

    public DrownedRenderer(EntityRendererProvider.Context context) {
        super(context, new DrownedModel(context.bakeLayer(ModelLayers.DROWNED)), new BabyDrownedModel(context.bakeLayer(ModelLayers.DROWNED_BABY)), ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, context.getModelSet(), DrownedModel::new), ArmorModelSet.bake(ModelLayers.DROWNED_BABY_ARMOR, context.getModelSet(), BabyDrownedModel::new));
        this.addLayer(new DrownedOuterLayer(this, context.getModelSet()));
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return state.isBaby ? BABY_DROWNED_LOCATION : DROWNED_LOCATION;
    }

    @Override
    protected void setupRotations(ZombieRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        float swimAmount = state.swimAmount;
        if (swimAmount > 0.0f) {
            float targetRotationX = -10.0f - state.xRot;
            float rotationX = Mth.lerp(swimAmount, 0.0f, targetRotationX);
            poseStack.rotateAround((Quaternionfc)Axis.XP.rotationDegrees(rotationX), 0.0f, state.boundingBoxHeight / 2.0f / entityScale, 0.0f);
        }
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(Drowned mob, HumanoidArm arm) {
        ItemStack item = mob.getItemHeldByArm(arm);
        if (mob.getMainArm() == arm && mob.isAggressive() && item.is(Items.TRIDENT)) {
            return HumanoidModel.ArmPose.THROW_TRIDENT;
        }
        return super.getArmPose(mob, arm);
    }
}

