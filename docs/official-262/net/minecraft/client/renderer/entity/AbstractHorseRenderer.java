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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractHorseRenderer<T extends AbstractHorse, S extends EquineRenderState, M extends EntityModel<? super S>>
extends AgeableMobRenderer<T, S, M> {
    public AbstractHorseRenderer(EntityRendererProvider.Context context, M model, M babyModel) {
        super(context, model, babyModel, 0.75f);
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((EquineRenderState)state).saddle = ((LivingEntity)entity).getItemBySlot(EquipmentSlot.SADDLE).copy();
        ((EquineRenderState)state).bodyArmorItem = ((Mob)entity).getBodyArmorItem().copy();
        ((EquineRenderState)state).isRidden = ((Entity)entity).isVehicle();
        ((EquineRenderState)state).eatAnimation = ((AbstractHorse)entity).getEatAnim(partialTicks);
        ((EquineRenderState)state).standAnimation = ((AbstractHorse)entity).getStandAnim(partialTicks);
        ((EquineRenderState)state).feedingAnimation = ((AbstractHorse)entity).getMouthAnim(partialTicks);
        ((EquineRenderState)state).animateTail = ((AbstractHorse)entity).tailCounter > 0;
    }
}

