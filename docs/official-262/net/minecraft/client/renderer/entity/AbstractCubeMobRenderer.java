/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractCubeMobRenderer<T extends AbstractCubeMob, S extends SlimeRenderState, M extends EntityModel<? super S>>
extends MobRenderer<T, S, M> {
    public AbstractCubeMobRenderer(EntityRendererProvider.Context context, M model) {
        super(context, model, 0.25f);
    }

    @Override
    protected float getShadowRadius(SlimeRenderState state) {
        return (float)state.size * 0.25f;
    }

    @Override
    protected void scale(S state, PoseStack poseStack) {
        super.scale(state, poseStack);
        this.applySizeAndSquish(state, poseStack);
    }

    protected void downscaleSlightly(PoseStack poseStack) {
        float s = 0.999f;
        poseStack.scale(0.999f, 0.999f, 0.999f);
        poseStack.translate(0.0f, 0.001f, 0.0f);
    }

    protected void applySizeAndSquish(S state, PoseStack poseStack) {
        float size = ((SlimeRenderState)state).size;
        float ss = ((SlimeRenderState)state).squish / (size * 0.5f + 1.0f);
        float w = 1.0f / (ss + 1.0f);
        poseStack.scale(w * size, 1.0f / w * size, w * size);
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((SlimeRenderState)state).squish = Mth.lerp(partialTicks, ((AbstractCubeMob)entity).oSquish, ((AbstractCubeMob)entity).squish);
        ((SlimeRenderState)state).size = ((AbstractCubeMob)entity).getSize();
    }
}

