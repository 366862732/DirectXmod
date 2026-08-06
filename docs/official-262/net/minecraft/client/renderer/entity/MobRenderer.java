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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@Environment(value=EnvType.CLIENT)
public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
extends LivingEntityRenderer<T, S, M> {
    public MobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    @Override
    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return super.shouldShowName(entity, distanceToCameraSq) && (((LivingEntity)entity).shouldShowName() || ((Entity)entity).hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    @Override
    protected float getShadowRadius(S state) {
        return super.getShadowRadius(state) * ((LivingEntityRenderState)state).ageScale;
    }

    protected static boolean checkMagicName(Entity entity, String magicName) {
        Component customName = entity.getCustomName();
        return customName != null && magicName.equals(customName.getString());
    }
}

