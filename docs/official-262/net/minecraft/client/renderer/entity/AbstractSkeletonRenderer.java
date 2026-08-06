/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Items;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState>
extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
    public AbstractSkeletonRenderer(EntityRendererProvider.Context context, ModelLayerLocation body, ArmorModelSet<ModelLayerLocation> armorSet) {
        this(context, armorSet, new SkeletonModel(context.bakeLayer(body)));
    }

    public AbstractSkeletonRenderer(EntityRendererProvider.Context context, ArmorModelSet<ModelLayerLocation> armorSet, SkeletonModel<S> bodyModel) {
        super(context, bodyModel, 0.5f);
        this.addLayer(new HumanoidArmorLayer(this, ArmorModelSet.bake(armorSet, context.getModelSet(), SkeletonModel::new), context.getEquipmentRenderer()));
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((SkeletonRenderState)state).isAggressive = ((Mob)entity).isAggressive();
        ((SkeletonRenderState)state).isShaking = ((AbstractSkeleton)entity).isShaking();
        ((SkeletonRenderState)state).isHoldingBow = ((LivingEntity)entity).getMainHandItem().is(Items.BOW);
    }

    @Override
    protected boolean isShaking(S state) {
        return ((SkeletonRenderState)state).isShaking;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
        if (((Mob)mob).getMainArm() == arm && ((Mob)mob).isAggressive() && ((LivingEntity)mob).getMainHandItem().is(Items.BOW)) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        return super.getArmPose(mob, arm);
    }
}

