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
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.CrossbowItem;

@Environment(value=EnvType.CLIENT)
public abstract class IllagerRenderer<T extends AbstractIllager, S extends IllagerRenderState>
extends MobRenderer<T, S, IllagerModel<S>> {
    protected IllagerRenderer(EntityRendererProvider.Context context, IllagerModel<S> model, float shadow) {
        super(context, model, shadow);
        this.addLayer(new CustomHeadLayer(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
        ((IllagerRenderState)state).isRiding = ((Entity)entity).isPassenger();
        ((IllagerRenderState)state).mainArm = ((Mob)entity).getMainArm();
        ((IllagerRenderState)state).armPose = ((AbstractIllager)entity).getArmPose();
        ((IllagerRenderState)state).maxCrossbowChargeDuration = ((IllagerRenderState)state).armPose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE ? CrossbowItem.getChargeDuration(((LivingEntity)entity).getUseItem(), entity) : 0;
        ((IllagerRenderState)state).ticksUsingItem = ((LivingEntity)entity).getTicksUsingItem(partialTicks);
        ((IllagerRenderState)state).attackAnim = ((LivingEntity)entity).getAttackAnim(partialTicks);
        ((IllagerRenderState)state).isAggressive = ((Mob)entity).isAggressive();
    }
}

