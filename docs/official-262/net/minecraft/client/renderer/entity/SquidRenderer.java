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
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.squid.Squid;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class SquidRenderer<T extends Squid>
extends AgeableMobRenderer<T, SquidRenderState, SquidModel> {
    private static final Identifier SQUID_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/squid.png");
    private static final Identifier SQUID_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/squid_baby.png");

    public SquidRenderer(EntityRendererProvider.Context context, SquidModel model, SquidModel babyModel) {
        super(context, model, babyModel, 0.7f);
    }

    @Override
    public Identifier getTextureLocation(SquidRenderState state) {
        return state.isBaby ? SQUID_BABY_LOCATION : SQUID_LOCATION;
    }

    @Override
    public SquidRenderState createRenderState() {
        return new SquidRenderState();
    }

    @Override
    public void extractRenderState(T entity, SquidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tentacleAngle = Mth.lerp(partialTicks, ((Squid)entity).oldTentacleAngle, ((Squid)entity).tentacleAngle);
        state.xBodyRot = Mth.lerp(partialTicks, ((Squid)entity).xBodyRotO, ((Squid)entity).xBodyRot);
        state.zBodyRot = Mth.lerp(partialTicks, ((Squid)entity).zBodyRotO, ((Squid)entity).zBodyRot);
    }

    @Override
    protected void setupRotations(SquidRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.translate(0.0f, state.isBaby ? 0.25f : 0.5f, 0.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f - bodyRot));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(state.xBodyRot));
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(state.zBodyRot));
        poseStack.translate(0.0f, state.isBaby ? -0.6f : -1.2f, 0.0f);
    }
}

