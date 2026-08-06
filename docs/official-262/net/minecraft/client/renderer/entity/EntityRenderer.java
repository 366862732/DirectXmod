/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5f;
    private static final float MAX_SHADOW_RADIUS = 32.0f;
    public static final float NAMETAG_SCALE = 0.025f;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    protected float shadowRadius;
    protected float shadowStrength = 1.0f;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.font = context.getFont();
    }

    public final int getPackedLightCoords(T entity, float partialTickTime) {
        BlockPos blockPos = BlockPos.containing(((Entity)entity).getLightProbePosition(partialTickTime));
        return LightCoordsUtil.pack(this.getBlockLightLevel(entity, blockPos), this.getSkyLightLevel(entity, blockPos));
    }

    protected int getSkyLightLevel(T entity, BlockPos blockPos) {
        return ((Entity)entity).level().getBrightness(LightLayer.SKY, blockPos);
    }

    protected int getBlockLightLevel(T entity, BlockPos blockPos) {
        if (((Entity)entity).isOnFire()) {
            return 15;
        }
        return ((Entity)entity).level().getBrightness(LightLayer.BLOCK, blockPos);
    }

    public boolean shouldRender(T entity, Frustum culler, double camX, double camY, double camZ) {
        Leashable leashable;
        Entity leashHolder;
        if (!((Entity)entity).shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (!this.affectedByCulling(entity)) {
            return true;
        }
        AABB boundingBox = this.getBoundingBoxForCulling(entity).inflate(0.5);
        if (boundingBox.hasNaN() || boundingBox.getSize() == 0.0) {
            boundingBox = new AABB(((Entity)entity).getX() - 2.0, ((Entity)entity).getY() - 2.0, ((Entity)entity).getZ() - 2.0, ((Entity)entity).getX() + 2.0, ((Entity)entity).getY() + 2.0, ((Entity)entity).getZ() + 2.0);
        }
        if (culler.isVisible(boundingBox)) {
            return true;
        }
        if (entity instanceof Leashable && (leashHolder = (leashable = (Leashable)entity).getLeashHolder()) != null) {
            AABB leasherBox = this.entityRenderDispatcher.getRenderer(leashHolder).getBoundingBoxForCulling(leashHolder);
            return culler.isVisible(leasherBox) || culler.isVisible(boundingBox.minmax(leasherBox));
        }
        return false;
    }

    protected AABB getBoundingBoxForCulling(T entity) {
        return ((Entity)entity).getBoundingBox();
    }

    protected boolean affectedByCulling(T entity) {
        return true;
    }

    public Vec3 getRenderOffset(S state) {
        if (((EntityRenderState)state).passengerOffset != null) {
            return ((EntityRenderState)state).passengerOffset;
        }
        return Vec3.ZERO;
    }

    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (((EntityRenderState)state).leashStates != null) {
            for (EntityRenderState.LeashState leashState : ((EntityRenderState)state).leashStates) {
                submitNodeCollector.submitLeash(poseStack, leashState);
            }
        }
        this.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return ((Entity)entity).shouldShowName() || ((Entity)entity).hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    public Font getFont() {
        return this.font;
    }

    protected void submitNameDisplay(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.submitNameDisplay(state, poseStack, submitNodeCollector, camera, 0);
    }

    protected final <S extends EntityRenderState> void submitNameDisplay(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, int offset) {
        poseStack.pushPose();
        if (state.scoreText != null) {
            submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.scoreText, !state.isDiscrete, state.lightCoords, camera);
            Objects.requireNonNull(this.getFont());
            poseStack.translate(0.0f, 9.0f * 1.15f * 0.025f, 0.0f);
        }
        if (state.nameTag != null) {
            submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.nameTag, !state.isDiscrete, state.lightCoords, camera);
        }
        poseStack.popPose();
    }

    protected @Nullable Component getNameTag(T entity) {
        return ((Entity)entity).getDisplayName();
    }

    protected float getShadowRadius(S state) {
        return this.shadowRadius;
    }

    protected float getShadowStrength(S state) {
        return this.shadowStrength;
    }

    public abstract S createRenderState();

    public final S createRenderState(T entity, float partialTicks) {
        S state = this.createRenderState();
        this.extractRenderState(entity, state, partialTicks);
        this.finalizeRenderState(entity, state);
        return state;
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        Leashable leashable;
        NewMinecartBehavior behavior;
        AbstractMinecart minecart;
        Object object;
        ((EntityRenderState)state).entityType = ((Entity)entity).getType();
        ((EntityRenderState)state).x = Mth.lerp((double)partialTicks, ((Entity)entity).xOld, ((Entity)entity).getX());
        ((EntityRenderState)state).y = Mth.lerp((double)partialTicks, ((Entity)entity).yOld, ((Entity)entity).getY());
        ((EntityRenderState)state).z = Mth.lerp((double)partialTicks, ((Entity)entity).zOld, ((Entity)entity).getZ());
        ((EntityRenderState)state).isInvisible = ((Entity)entity).isInvisible();
        ((EntityRenderState)state).ageInTicks = (float)((Entity)entity).tickCount + partialTicks;
        ((EntityRenderState)state).boundingBoxWidth = ((Entity)entity).getBbWidth();
        ((EntityRenderState)state).boundingBoxHeight = ((Entity)entity).getBbHeight();
        ((EntityRenderState)state).eyeHeight = ((Entity)entity).getEyeHeight();
        if (((Entity)entity).isPassenger() && (object = ((Entity)entity).getVehicle()) instanceof AbstractMinecart && (object = (minecart = (AbstractMinecart)object).getBehavior()) instanceof NewMinecartBehavior && (behavior = (NewMinecartBehavior)object).cartHasPosRotLerp()) {
            double cartLerpX = Mth.lerp((double)partialTicks, minecart.xOld, minecart.getX());
            double cartLerpY = Mth.lerp((double)partialTicks, minecart.yOld, minecart.getY());
            double cartLerpZ = Mth.lerp((double)partialTicks, minecart.zOld, minecart.getZ());
            ((EntityRenderState)state).passengerOffset = behavior.getCartLerpPosition(partialTicks).subtract(new Vec3(cartLerpX, cartLerpY, cartLerpZ));
        } else {
            ((EntityRenderState)state).passengerOffset = null;
        }
        this.extractNameTags(entity, state, partialTicks);
        ((EntityRenderState)state).isDiscrete = ((Entity)entity).isDiscrete();
        Level level = ((Entity)entity).level();
        if (entity instanceof Leashable && (leashable = (Leashable)entity).getLeashHolder() != null) {
            int leashCount;
            Entity roper = leashable.getLeashHolder();
            float entityYRot = ((Entity)entity).getPreciseBodyRotation(partialTicks) * ((float)Math.PI / 180);
            Vec3 attachOffset = leashable.getLeashOffset(partialTicks);
            BlockPos entityEyePos = BlockPos.containing(((Entity)entity).getEyePosition(partialTicks));
            BlockPos roperEyePos = BlockPos.containing(roper.getEyePosition(partialTicks));
            int startBlockLight = this.getBlockLightLevel(entity, entityEyePos);
            int endBlockLight = this.entityRenderDispatcher.getRenderer(roper).getBlockLightLevel(roper, roperEyePos);
            int startSkyLight = level.getBrightness(LightLayer.SKY, entityEyePos);
            int endSkyLight = level.getBrightness(LightLayer.SKY, roperEyePos);
            boolean quadConnection = roper.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
            int n = leashCount = quadConnection ? 4 : 1;
            if (((EntityRenderState)state).leashStates == null || ((EntityRenderState)state).leashStates.size() != leashCount) {
                ((EntityRenderState)state).leashStates = new ArrayList<EntityRenderState.LeashState>(leashCount);
                for (int i = 0; i < leashCount; ++i) {
                    ((EntityRenderState)state).leashStates.add(new EntityRenderState.LeashState());
                }
            }
            if (quadConnection) {
                float roperYRot = roper.getPreciseBodyRotation(partialTicks) * ((float)Math.PI / 180);
                Vec3 holderPos = roper.getPosition(partialTicks);
                Vec3[] leashableAttachmentPoints = leashable.getQuadLeashOffsets();
                Vec3[] roperAttachmentPoints = roper.getQuadLeashHolderOffsets();
                for (int i = 0; i < leashCount; ++i) {
                    EntityRenderState.LeashState leashState = ((EntityRenderState)state).leashStates.get(i);
                    leashState.offset = leashableAttachmentPoints[i].yRot(-entityYRot);
                    leashState.start = ((Entity)entity).getPosition(partialTicks).add(leashState.offset);
                    leashState.end = holderPos.add(roperAttachmentPoints[i].yRot(-roperYRot));
                    leashState.startBlockLight = startBlockLight;
                    leashState.endBlockLight = endBlockLight;
                    leashState.startSkyLight = startSkyLight;
                    leashState.endSkyLight = endSkyLight;
                    leashState.slack = false;
                }
            } else {
                Vec3 rotatedAttachOffset = attachOffset.yRot(-entityYRot);
                EntityRenderState.LeashState leashState = ((EntityRenderState)state).leashStates.getFirst();
                leashState.offset = rotatedAttachOffset;
                leashState.start = ((Entity)entity).getPosition(partialTicks).add(rotatedAttachOffset);
                leashState.end = roper.getRopeHoldPosition(partialTicks);
                leashState.startBlockLight = startBlockLight;
                leashState.endBlockLight = endBlockLight;
                leashState.startSkyLight = startSkyLight;
                leashState.endSkyLight = endSkyLight;
            }
        } else {
            ((EntityRenderState)state).leashStates = null;
        }
        ((EntityRenderState)state).displayFireAnimation = ((Entity)entity).displayFireAnimation();
        Minecraft minecraft = Minecraft.getInstance();
        boolean appearsGlowing = minecraft.shouldEntityAppearGlowing((Entity)entity);
        ((EntityRenderState)state).outlineColor = appearsGlowing ? ARGB.opaque(((Entity)entity).getTeamColor()) : 0;
        ((EntityRenderState)state).lightCoords = this.getPackedLightCoords(entity, partialTicks);
    }

    protected void extractNameTags(T entity, S state, float partialTicks) {
        this.extractNameTags(entity, state, partialTicks, 64.0, 10.0);
    }

    protected final void extractNameTags(T entity, S state, float partialTicks, double nameTagDistance, double belowNameDistance) {
        if (this.entityRenderDispatcher.camera != null) {
            boolean shouldShowName;
            ((EntityRenderState)state).distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr((Entity)entity);
            boolean bl = shouldShowName = ((EntityRenderState)state).distanceToCameraSq < Mth.square(nameTagDistance) && this.shouldShowName(entity, ((EntityRenderState)state).distanceToCameraSq);
            if (shouldShowName) {
                ((EntityRenderState)state).nameTag = this.getNameTag(entity);
                ((EntityRenderState)state).nameTagAttachment = ((Entity)entity).getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, ((Entity)entity).getYRot(partialTicks));
            } else {
                ((EntityRenderState)state).nameTag = null;
            }
            ((EntityRenderState)state).scoreText = ((EntityRenderState)state).distanceToCameraSq < Mth.square(belowNameDistance) ? ((Entity)entity).belowNameDisplay() : null;
        }
    }

    protected void finalizeRenderState(T entity, S state) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = ((Entity)entity).level();
        this.extractShadow(state, minecraft, level);
    }

    private void extractShadow(S state, Minecraft minecraft, Level level) {
        ((EntityRenderState)state).shadowPieces.clear();
        if (minecraft.options.entityShadows().get().booleanValue() && !((EntityRenderState)state).isInvisible) {
            double distSq;
            float pow;
            float shadowRadius;
            ((EntityRenderState)state).shadowRadius = shadowRadius = Math.min(this.getShadowRadius(state), 32.0f);
            if (shadowRadius > 0.0f && (pow = (float)((1.0 - (distSq = ((EntityRenderState)state).distanceToCameraSq) / 256.0) * (double)this.getShadowStrength(state))) > 0.0f) {
                int x0 = Mth.floor(((EntityRenderState)state).x - (double)shadowRadius);
                int x1 = Mth.floor(((EntityRenderState)state).x + (double)shadowRadius);
                int z0 = Mth.floor(((EntityRenderState)state).z - (double)shadowRadius);
                int z1 = Mth.floor(((EntityRenderState)state).z + (double)shadowRadius);
                float depth = Math.min(pow / 0.5f - 1.0f, shadowRadius);
                int y0 = Mth.floor(((EntityRenderState)state).y - (double)depth);
                int y1 = Mth.floor(((EntityRenderState)state).y);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int z = z0; z <= z1; ++z) {
                    for (int x = x0; x <= x1; ++x) {
                        pos.set(x, 0, z);
                        ChunkAccess chunk = level.getChunk(pos);
                        for (int y = y0; y <= y1; ++y) {
                            pos.setY(y);
                            this.extractShadowPiece(state, level, pow, pos, chunk);
                        }
                    }
                }
            }
        } else {
            ((EntityRenderState)state).shadowRadius = 0.0f;
        }
    }

    private void extractShadowPiece(S state, Level level, float pow, BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        float powerAtDepth = pow - (float)(((EntityRenderState)state).y - (double)pos.getY()) * 0.5f;
        Vec3i belowPos = pos.below();
        BlockState belowState = chunk.getBlockState((BlockPos)belowPos);
        if (belowState.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        int brightness = level.getMaxLocalRawBrightness(pos);
        if (brightness <= 3) {
            return;
        }
        if (!belowState.isCollisionShapeFullBlock(chunk, (BlockPos)belowPos)) {
            return;
        }
        VoxelShape belowShape = belowState.getShape(chunk, (BlockPos)belowPos);
        if (belowShape.isEmpty()) {
            return;
        }
        float alpha = Mth.clamp(powerAtDepth * 0.5f * Lightmap.getBrightness(level.dimensionType(), brightness), 0.0f, 1.0f);
        float relativeX = (float)((double)pos.getX() - ((EntityRenderState)state).x);
        float relativeY = (float)((double)pos.getY() - ((EntityRenderState)state).y);
        float relativeZ = (float)((double)pos.getZ() - ((EntityRenderState)state).z);
        ((EntityRenderState)state).shadowPieces.add(new EntityRenderState.ShadowPiece(relativeX, relativeY, relativeZ, belowShape, alpha));
    }

    private static @Nullable Entity getServerSideEntity(Entity entity) {
        ServerLevel level;
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && (level = server.getLevel(entity.level().dimension())) != null) {
            return level.getEntity(entity.getId());
        }
        return null;
    }
}

