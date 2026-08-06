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
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState>
extends EntityRenderer<T, S> {
    private static final Identifier MINECART_LOCATION = Identifier.withDefaultNamespace("textures/entity/minecart/minecart.png");
    private static final float DISPLAY_BLOCK_SCALE = 0.75f;
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    protected final MinecartModel model;
    private final BlockModelResolver blockModelResolver;

    public AbstractMinecartRenderer(EntityRendererProvider.Context context, ModelLayerLocation model) {
        super(context);
        this.shadowRadius = 0.7f;
        this.model = new MinecartModel(context.bakeLayer(model));
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BlockModelRenderState displayBlockModel;
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();
        long seed = ((MinecartRenderState)state).offsetSeed;
        float offsetX = (((float)(seed >> 16 & 7L) + 0.5f) / 8.0f - 0.5f) * 0.004f;
        float offsetY = (((float)(seed >> 20 & 7L) + 0.5f) / 8.0f - 0.5f) * 0.004f;
        float offsetZ = (((float)(seed >> 24 & 7L) + 0.5f) / 8.0f - 0.5f) * 0.004f;
        poseStack.translate(offsetX, offsetY, offsetZ);
        if (((MinecartRenderState)state).isNewRender) {
            AbstractMinecartRenderer.newRender(state, poseStack);
        } else {
            AbstractMinecartRenderer.oldRender(state, poseStack);
        }
        float hurt = ((MinecartRenderState)state).hurtTime;
        if (hurt > 0.0f) {
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * ((MinecartRenderState)state).damageTime / 10.0f * (float)((MinecartRenderState)state).hurtDir));
        }
        if (!(displayBlockModel = ((MinecartRenderState)state).displayBlockModel).isEmpty()) {
            poseStack.pushPose();
            poseStack.scale(0.75f, 0.75f, 0.75f);
            poseStack.translate(-0.5f, (float)(((MinecartRenderState)state).displayOffset - 8) / 16.0f, 0.5f);
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(90.0f));
            this.submitMinecartContents(state, displayBlockModel, poseStack, submitNodeCollector, ((MinecartRenderState)state).lightCoords);
            poseStack.popPose();
        }
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        submitNodeCollector.submitModel(this.model, state, poseStack, MINECART_LOCATION, ((MinecartRenderState)state).lightCoords, OverlayTexture.NO_OVERLAY, ((MinecartRenderState)state).outlineColor, null);
        poseStack.popPose();
    }

    private static <S extends MinecartRenderState> void newRender(S state, PoseStack poseStack) {
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(state.yRot));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(-state.xRot));
        poseStack.translate(0.0f, 0.375f, 0.0f);
    }

    private static <S extends MinecartRenderState> void oldRender(S state, PoseStack poseStack) {
        double entityX = state.x;
        double entityY = state.y;
        double entityZ = state.z;
        float xRot = state.xRot;
        float rotation = state.yRot;
        if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
            Vec3 frontPos = state.frontPos;
            Vec3 backPos = state.backPos;
            poseStack.translate(state.posOnRail.x - entityX, (frontPos.y + backPos.y) / 2.0 - entityY, state.posOnRail.z - entityZ);
            Vec3 direction = backPos.add(-frontPos.x, -frontPos.y, -frontPos.z);
            if (direction.length() != 0.0) {
                direction = direction.normalize();
                rotation = (float)(Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                xRot = (float)(Math.atan(direction.y) * 73.0);
            }
        }
        poseStack.translate(0.0f, 0.375f, 0.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f - rotation));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(-xRot));
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        MinecartBehavior minecartBehavior = ((AbstractMinecart)entity).getBehavior();
        if (minecartBehavior instanceof NewMinecartBehavior) {
            NewMinecartBehavior behavior = (NewMinecartBehavior)minecartBehavior;
            AbstractMinecartRenderer.newExtractState(entity, behavior, state, partialTicks);
            ((MinecartRenderState)state).isNewRender = true;
        } else {
            minecartBehavior = ((AbstractMinecart)entity).getBehavior();
            if (minecartBehavior instanceof OldMinecartBehavior) {
                OldMinecartBehavior behavior = (OldMinecartBehavior)minecartBehavior;
                AbstractMinecartRenderer.oldExtractState(entity, behavior, state, partialTicks);
                ((MinecartRenderState)state).isNewRender = false;
            }
        }
        long seed = (long)((Entity)entity).getId() * 493286711L;
        ((MinecartRenderState)state).offsetSeed = seed * seed * 4392167121L + seed * 98761L;
        ((MinecartRenderState)state).hurtTime = (float)((VehicleEntity)entity).getHurtTime() - partialTicks;
        ((MinecartRenderState)state).hurtDir = ((VehicleEntity)entity).getHurtDir();
        ((MinecartRenderState)state).damageTime = Math.max(((VehicleEntity)entity).getDamage() - partialTicks, 0.0f);
        ((MinecartRenderState)state).displayOffset = ((AbstractMinecart)entity).getDisplayOffset();
        this.blockModelResolver.update(((MinecartRenderState)state).displayBlockModel, ((AbstractMinecart)entity).getDisplayBlockState(), BLOCK_DISPLAY_CONTEXT);
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void newExtractState(T entity, NewMinecartBehavior behavior, S state, float partialTicks) {
        if (behavior.cartHasPosRotLerp()) {
            state.renderPos = behavior.getCartLerpPosition(partialTicks);
            state.xRot = behavior.getCartLerpXRot(partialTicks);
            state.yRot = behavior.getCartLerpYRot(partialTicks);
        } else {
            state.renderPos = null;
            state.xRot = entity.getXRot();
            state.yRot = entity.getYRot();
        }
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void oldExtractState(T entity, OldMinecartBehavior behavior, S state, float partialTicks) {
        float HALF_LENGTH = 0.3f;
        state.xRot = entity.getXRot(partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        double entityX = state.x;
        double entityY = state.y;
        double entityZ = state.z;
        Vec3 pos = behavior.getPos(entityX, entityY, entityZ);
        if (pos != null) {
            state.posOnRail = pos;
            Vec3 p0 = behavior.getPosOffs(entityX, entityY, entityZ, 0.3f);
            Vec3 p1 = behavior.getPosOffs(entityX, entityY, entityZ, -0.3f);
            state.frontPos = Objects.requireNonNullElse(p0, pos);
            state.backPos = Objects.requireNonNullElse(p1, pos);
        } else {
            state.posOnRail = null;
            state.frontPos = null;
            state.backPos = null;
        }
    }

    protected void submitMinecartContents(S state, BlockModelRenderState blockModel, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        blockModel.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, ((MinecartRenderState)state).outlineColor);
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity) {
        AABB aabb = super.getBoundingBoxForCulling(entity);
        if (!((AbstractMinecart)entity).getDisplayBlockState().isAir()) {
            return aabb.expandTowards(0.0, (float)((AbstractMinecart)entity).getDisplayOffset() * 0.75f / 16.0f, 0.0);
        }
        return aabb;
    }

    @Override
    public Vec3 getRenderOffset(S state) {
        Vec3 offset = super.getRenderOffset(state);
        if (((MinecartRenderState)state).isNewRender && ((MinecartRenderState)state).renderPos != null) {
            return offset.add(((MinecartRenderState)state).renderPos.x - ((MinecartRenderState)state).x, ((MinecartRenderState)state).renderPos.y - ((MinecartRenderState)state).y, ((MinecartRenderState)state).renderPos.z - ((MinecartRenderState)state).z);
        }
        return offset;
    }
}

