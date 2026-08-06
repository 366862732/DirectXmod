/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Camera
implements TrackedWaypoint.Camera {
    public static final float PROJECTION_Z_NEAR = 0.05f;
    private static final float DEFAULT_CAMERA_DISTANCE = 4.0f;
    private static final Vector3fc FORWARDS = new Vector3f(0.0f, 0.0f, -1.0f);
    private static final Vector3fc UP = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3fc LEFT = new Vector3f(-1.0f, 0.0f, 0.0f);
    public static final float BASE_HUD_FOV = 70.0f;
    private boolean initialized;
    private @Nullable Level level;
    private @Nullable Entity entity;
    private Vec3 position = Vec3.ZERO;
    private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
    private final Vector3f forwards = new Vector3f(FORWARDS);
    private final Vector3f panoramicForwards = new Vector3f(FORWARDS);
    private final Vector3f up = new Vector3f(UP);
    private final Vector3f left = new Vector3f(LEFT);
    private float xRot;
    private float yRot;
    private final Quaternionf rotation = new Quaternionf();
    private boolean detached;
    private float eyeHeight;
    private float eyeHeightOld;
    private final Projection projection = new Projection();
    private Frustum cullFrustum = new Frustum((Matrix4fc)new Matrix4f(), new Matrix4f());
    private @Nullable Frustum capturedFrustum;
    private boolean captureFrustum;
    private final Matrix4f cachedViewRotMatrix = new Matrix4f();
    private final Matrix4f cachedViewRotProjMatrix = new Matrix4f();
    private long lastProjectionVersion = -1L;
    private int matrixPropertiesDirty = -1;
    private static final int DIRTY_VIEW_ROT = 1;
    private static final int DIRTY_VIEW_ROT_PROJ = 2;
    private float fovModifier;
    private float oldFovModifier;
    private float fov;
    private float hudFov;
    private float depthFar;
    private boolean isPanoramicMode;
    private final EnvironmentAttributeProbe attributeProbe = new EnvironmentAttributeProbe();
    private final Minecraft minecraft = Minecraft.getInstance();

    public void tick() {
        if (this.level != null && this.entity != null) {
            this.eyeHeightOld = this.eyeHeight;
            this.eyeHeight += (this.entity.getEyeHeight() - this.eyeHeight) * 0.5f;
            this.attributeProbe.tick(this.level, this.position);
        }
        this.tickFov();
    }

    public void update(DeltaTracker deltaTracker) {
        float renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;
        this.depthFar = Math.max(renderDistance * 4.0f, (float)(this.minecraft.options.cloudRange().get() * 16));
        LocalPlayer player = this.minecraft.player;
        if (player == null || this.level == null) {
            return;
        }
        if (this.entity == null) {
            this.setEntity(player);
        }
        float partialTicks = this.getCameraEntityPartialTicks(deltaTracker);
        this.alignWithEntity(partialTicks);
        this.fov = this.calculateFov(partialTicks);
        this.hudFov = this.calculateHudFov(partialTicks);
        this.prepareCullFrustum((Matrix4fc)this.getViewRotationMatrix(this.cachedViewRotMatrix), this.createProjectionMatrixForCulling(), this.position);
        float windowWidth = this.minecraft.getWindow().getWidth();
        float windowHeight = this.minecraft.getWindow().getHeight();
        this.setupPerspective(0.05f, this.depthFar, this.fov, windowWidth, windowHeight);
        this.initialized = true;
    }

    public float getCameraEntityPartialTicks(DeltaTracker deltaTracker) {
        return this.level.tickRateManager().isEntityFrozen(this.entity) ? 1.0f : deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    public void extractRenderState(CameraRenderState cameraState, float cameraEntityPartialTicks) {
        cameraState.initialized = this.isInitialized();
        cameraState.isPanoramicMode = this.isPanoramicMode;
        cameraState.isFrustumCaptured = this.capturedFrustum != null;
        cameraState.smartCull = this.minecraft.smartCull;
        if (this.minecraft.player.isSpectator() && this.level.getBlockState(this.blockPosition).isSolidRender()) {
            cameraState.smartCull = false;
        }
        cameraState.pos = this.position();
        cameraState.xRot = this.xRot;
        cameraState.yRot = this.yRot;
        cameraState.blockPos = this.blockPosition();
        cameraState.orientation.set((Quaternionfc)this.rotation());
        cameraState.cullFrustum.set(this.cullFrustum);
        cameraState.depthFar = this.depthFar;
        this.projection.getMatrix(cameraState.projectionMatrix);
        this.getViewRotationMatrix(cameraState.viewRotationMatrix);
        Entity entity = this.entity;
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            cameraState.entityRenderState.isLiving = true;
            cameraState.entityRenderState.isSleeping = livingEntity.isSleeping();
            cameraState.entityRenderState.doesMobEffectBlockSky = livingEntity.hasEffect(MobEffects.BLINDNESS) || livingEntity.hasEffect(MobEffects.DARKNESS);
            cameraState.entityRenderState.isDeadOrDying = livingEntity.isDeadOrDying();
            cameraState.entityRenderState.hurtDir = livingEntity.getHurtDir();
            cameraState.entityRenderState.hurtTime = (float)livingEntity.hurtTime - cameraEntityPartialTicks;
            cameraState.entityRenderState.deathTime = (float)livingEntity.deathTime + cameraEntityPartialTicks;
            cameraState.entityRenderState.hurtDuration = livingEntity.hurtDuration;
        } else {
            cameraState.entityRenderState.isLiving = false;
            cameraState.entityRenderState.isSleeping = false;
            cameraState.entityRenderState.doesMobEffectBlockSky = false;
        }
        entity = this.entity;
        if (entity instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer)entity;
            cameraState.entityRenderState.isPlayer = true;
            ClientAvatarState avatarState = player.avatarState();
            cameraState.entityRenderState.backwardsInterpolatedWalkDistance = avatarState.getBackwardsInterpolatedWalkDistance(cameraEntityPartialTicks);
            cameraState.entityRenderState.bob = avatarState.getInterpolatedBob(cameraEntityPartialTicks);
        } else {
            cameraState.entityRenderState.isPlayer = false;
        }
        cameraState.hudFov = this.hudFov;
    }

    private void tickFov() {
        float targetFovModifier;
        Entity entity = this.minecraft.getCameraEntity();
        if (entity instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer)entity;
            Options options = this.minecraft.options;
            boolean firstPerson = options.getCameraType().isFirstPerson();
            float effectScale = options.fovEffectScale().get().floatValue();
            targetFovModifier = player.getFieldOfViewModifier(firstPerson, effectScale);
        } else {
            targetFovModifier = 1.0f;
        }
        this.oldFovModifier = this.fovModifier;
        this.fovModifier += (targetFovModifier - this.fovModifier) * 0.5f;
        this.fovModifier = Mth.clamp(this.fovModifier, 0.1f, 1.5f);
    }

    private Matrix4f createProjectionMatrixForCulling() {
        float fovForCulling = Math.max(this.fov, (float)this.minecraft.options.fov().get().intValue());
        Matrix4f projection = new Matrix4f();
        return projection.perspective(fovForCulling * ((float)Math.PI / 180), (float)this.minecraft.getWindow().getWidth() / (float)this.minecraft.getWindow().getHeight(), 0.05f, this.depthFar, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
    }

    public Frustum getCullFrustum() {
        return this.cullFrustum;
    }

    private void prepareCullFrustum(Matrix4fc modelViewMatrix, Matrix4f projectionMatrixForCulling, Vec3 cameraPos) {
        if (this.capturedFrustum != null && !this.captureFrustum) {
            this.cullFrustum = this.capturedFrustum;
        } else {
            this.cullFrustum = new Frustum(modelViewMatrix, projectionMatrixForCulling);
            this.cullFrustum.prepare(cameraPos.x(), cameraPos.y(), cameraPos.z());
        }
        if (this.captureFrustum) {
            this.capturedFrustum = this.cullFrustum;
            this.captureFrustum = false;
        }
    }

    public @Nullable Frustum getCapturedFrustum() {
        return this.capturedFrustum;
    }

    public void captureFrustum() {
        this.captureFrustum = true;
    }

    public void killFrustum() {
        this.capturedFrustum = null;
    }

    private float calculateFov(float partialTicks) {
        if (this.isPanoramicMode) {
            return 90.0f;
        }
        float fov = (float)this.minecraft.options.fov().get().intValue() * Mth.lerp(partialTicks, this.oldFovModifier, this.fovModifier);
        return this.modifyFovBasedOnDeathOrFluid(partialTicks, fov);
    }

    private float calculateHudFov(float partialTicks) {
        return this.modifyFovBasedOnDeathOrFluid(partialTicks, 70.0f);
    }

    private float modifyFovBasedOnDeathOrFluid(float partialTicks, float fov) {
        FogType state;
        LivingEntity cameraEntity;
        Entity entity = this.entity;
        if (entity instanceof LivingEntity && (cameraEntity = (LivingEntity)entity).isDeadOrDying()) {
            float duration = Math.min((float)cameraEntity.deathTime + partialTicks, 20.0f);
            fov /= (1.0f - 500.0f / (duration + 500.0f)) * 2.0f + 1.0f;
        }
        if ((state = this.getFluidInCamera()) == FogType.LAVA || state == FogType.WATER) {
            float effectScale = this.minecraft.options.fovEffectScale().get().floatValue();
            fov *= Mth.lerp(effectScale, 1.0f, 0.85714287f);
        }
        return fov;
    }

    private void alignWithEntity(float partialTicks) {
        NewMinecartBehavior behavior;
        Minecart minecart;
        Object object;
        if (this.entity.isPassenger() && (object = this.entity.getVehicle()) instanceof Minecart && (object = (minecart = (Minecart)object).getBehavior()) instanceof NewMinecartBehavior && (behavior = (NewMinecartBehavior)object).cartHasPosRotLerp()) {
            Vec3 positionOffset = minecart.getPassengerRidingPosition(this.entity).subtract(minecart.position()).subtract(this.entity.getVehicleAttachmentPoint(minecart)).add(new Vec3(0.0, Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight), 0.0));
            this.setRotation(this.entity.getViewYRot(partialTicks), this.entity.getViewXRot(partialTicks));
            this.setPosition(behavior.getCartLerpPosition(partialTicks).add(positionOffset));
        } else {
            this.setRotation(this.entity.getViewYRot(partialTicks), this.entity.getViewXRot(partialTicks));
            this.setPosition(Mth.lerp((double)partialTicks, this.entity.xo, this.entity.getX()), Mth.lerp((double)partialTicks, this.entity.yo, this.entity.getY()) + (double)Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight), Mth.lerp((double)partialTicks, this.entity.zo, this.entity.getZ()));
        }
        boolean bl = this.detached = !this.minecraft.options.getCameraType().isFirstPerson();
        if (this.detached) {
            Entity entity;
            if (this.minecraft.options.getCameraType().isMirrored()) {
                this.setRotation(this.yRot + 180.0f, -this.xRot);
            }
            float cameraDistance = 4.0f;
            float cameraScale = 1.0f;
            Entity entity2 = this.entity;
            if (entity2 instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity2;
                cameraScale = living.getScale();
                cameraDistance = (float)living.getAttributeValue(Attributes.CAMERA_DISTANCE);
            }
            float mountScale = cameraScale;
            float mountDistance = cameraDistance;
            if (this.entity.isPassenger() && (entity = this.entity.getVehicle()) instanceof LivingEntity) {
                LivingEntity mount = (LivingEntity)entity;
                mountScale = mount.getScale();
                mountDistance = (float)mount.getAttributeValue(Attributes.CAMERA_DISTANCE);
            }
            this.move(-this.getMaxZoom(Math.max(cameraScale * cameraDistance, mountScale * mountDistance)), 0.0f, 0.0f);
        } else if (this.entity instanceof LivingEntity && ((LivingEntity)this.entity).isSleeping()) {
            Direction bedOrientation = ((LivingEntity)this.entity).getBedOrientation();
            this.setRotation(bedOrientation != null ? bedOrientation.toYRot() - 180.0f : 0.0f, 0.0f);
            this.move(0.0f, 0.15f, 0.0f);
        }
    }

    private float getMaxZoom(float cameraDist) {
        float jitterScale = 0.1f;
        for (int i = 0; i < 8; ++i) {
            float distSq;
            Vec3 to;
            float offsetX = (i & 1) * 2 - 1;
            float offsetY = (i >> 1 & 1) * 2 - 1;
            float offsetZ = (i >> 2 & 1) * 2 - 1;
            Vec3 from = this.position.add(offsetX * 0.1f, offsetY * 0.1f, offsetZ * 0.1f);
            BlockHitResult hitResult = this.level.clip(new ClipContext(from, to = from.add(new Vec3((Vector3fc)this.forwards).scale(-cameraDist)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));
            if (((HitResult)hitResult).getType() == HitResult.Type.MISS || !((distSq = (float)hitResult.getLocation().distanceToSqr(this.position)) < Mth.square(cameraDist))) continue;
            cameraDist = Mth.sqrt(distSq);
        }
        return cameraDist;
    }

    public boolean isPanoramicMode() {
        return this.isPanoramicMode;
    }

    public float getFov() {
        return this.fov;
    }

    private void setupPerspective(float zNear, float zFar, float fov, float width, float height) {
        this.projection.setupPerspective(zNear, zFar, fov, width, height);
    }

    private void setupOrtho(float zNear, float zFar, float width, float height, boolean invertY) {
        this.projection.setupOrtho(zNear, zFar, width, height, invertY);
    }

    protected void move(float forwards, float up, float right) {
        Vector3f offset = new Vector3f(right, up, -forwards).rotate((Quaternionfc)this.rotation);
        this.setPosition(new Vec3(this.position.x + (double)offset.x, this.position.y + (double)offset.y, this.position.z + (double)offset.z));
    }

    protected void setRotation(float yRot, float xRot) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.rotation.rotationYXZ((float)Math.PI - yRot * ((float)Math.PI / 180), -xRot * ((float)Math.PI / 180), 0.0f);
        FORWARDS.rotate((Quaternionfc)this.rotation, this.forwards);
        UP.rotate((Quaternionfc)this.rotation, this.up);
        LEFT.rotate((Quaternionfc)this.rotation, this.left);
        this.matrixPropertiesDirty |= 3;
    }

    protected void setPosition(double x, double y, double z) {
        this.setPosition(new Vec3(x, y, z));
    }

    protected void setPosition(Vec3 position) {
        this.position = position;
        this.blockPosition.set(position.x, position.y, position.z);
    }

    @Override
    public Vec3 position() {
        return this.position;
    }

    public BlockPos blockPosition() {
        return this.blockPosition;
    }

    public float xRot() {
        return this.xRot;
    }

    public float yRot() {
        return this.yRot;
    }

    @Override
    public float yaw() {
        return Mth.wrapDegrees(this.yRot());
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public Matrix4f getViewRotationMatrix(Matrix4f dest) {
        if ((this.matrixPropertiesDirty & 1) != 0) {
            Quaternionf inverseRotation = this.rotation().conjugate(new Quaternionf());
            this.cachedViewRotMatrix.rotation((Quaternionfc)inverseRotation);
            this.matrixPropertiesDirty &= 0xFFFFFFFE;
        }
        return dest.set((Matrix4fc)this.cachedViewRotMatrix);
    }

    public Matrix4f getViewRotationProjectionMatrix(Matrix4f dest) {
        long projectionVersion = this.projection.getMatrixVersion();
        if ((this.matrixPropertiesDirty & 2) != 0 || this.lastProjectionVersion != this.projection.getMatrixVersion()) {
            this.getViewRotationMatrix(this.cachedViewRotMatrix);
            this.projection.getMatrix(this.cachedViewRotProjMatrix);
            this.cachedViewRotProjMatrix.mul((Matrix4fc)this.cachedViewRotMatrix);
            this.matrixPropertiesDirty &= 0xFFFFFFFD;
            this.lastProjectionVersion = projectionVersion;
        }
        return dest.set((Matrix4fc)this.cachedViewRotProjMatrix);
    }

    public @Nullable Entity entity() {
        return this.entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isDetached() {
        return this.detached;
    }

    public EnvironmentAttributeProbe attributeProbe() {
        return this.attributeProbe;
    }

    public NearPlane getNearPlane(float fov) {
        double aspectRatio = (double)this.projection.width() / (double)this.projection.height();
        double planeHeight = Math.tan((double)(fov * ((float)Math.PI / 180)) / 2.0) * (double)this.projection.zNear();
        double planeWidth = planeHeight * aspectRatio;
        Vec3 forwardsVec3 = new Vec3((Vector3fc)this.forwards).scale(this.projection.zNear());
        Vec3 leftVec3 = new Vec3((Vector3fc)this.left).scale(planeWidth);
        Vec3 upVec3 = new Vec3((Vector3fc)this.up).scale(planeHeight);
        return new NearPlane(forwardsVec3, leftVec3, upVec3);
    }

    public FogType getFluidInCamera() {
        if (!this.initialized) {
            return FogType.NONE;
        }
        FluidState fluidState1 = this.level.getFluidState(this.blockPosition);
        if (fluidState1.is(FluidTags.WATER) && this.position.y < (double)((float)this.blockPosition.getY() + fluidState1.getHeight(this.level, this.blockPosition))) {
            return FogType.WATER;
        }
        NearPlane plane = this.getNearPlane(this.minecraft.options.fov().get().intValue());
        List<Vec3> points = Arrays.asList(plane.forward, plane.getTopLeft(), plane.getTopRight(), plane.getBottomLeft(), plane.getBottomRight());
        for (Vec3 point : points) {
            Vec3 offsetPos = this.position.add(point);
            BlockPos checkPos = BlockPos.containing(offsetPos);
            FluidState fluidState = this.level.getFluidState(checkPos);
            if (fluidState.is(FluidTags.LAVA)) {
                if (!(offsetPos.y <= (double)(fluidState.getHeight(this.level, checkPos) + (float)checkPos.getY()))) continue;
                return FogType.LAVA;
            }
            BlockState state = this.level.getBlockState(checkPos);
            if (!state.is(Blocks.POWDER_SNOW)) continue;
            return FogType.POWDER_SNOW;
        }
        return FogType.NONE;
    }

    public Vector3fc forwardVector() {
        return this.forwards;
    }

    public Vector3fc panoramicForwards() {
        return this.panoramicForwards;
    }

    public Vector3fc upVector() {
        return this.up;
    }

    public Vector3fc leftVector() {
        return this.left;
    }

    public void reset() {
        this.level = null;
        this.entity = null;
        this.attributeProbe.reset();
        this.initialized = false;
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.level = level;
    }

    public void enablePanoramicMode() {
        this.isPanoramicMode = true;
        this.panoramicForwards.set((Vector3fc)this.forwards);
    }

    public void disablePanoramicMode() {
        this.isPanoramicMode = false;
    }

    @Environment(value=EnvType.CLIENT)
    public static class NearPlane {
        private final Vec3 forward;
        private final Vec3 left;
        private final Vec3 up;

        private NearPlane(Vec3 forward, Vec3 left, Vec3 up) {
            this.forward = forward;
            this.left = left;
            this.up = up;
        }

        public Vec3 getTopLeft() {
            return this.forward.add(this.up).add(this.left);
        }

        public Vec3 getTopRight() {
            return this.forward.add(this.up).subtract(this.left);
        }

        public Vec3 getBottomLeft() {
            return this.forward.subtract(this.up).add(this.left);
        }

        public Vec3 getBottomRight() {
            return this.forward.subtract(this.up).subtract(this.left);
        }

        public Vec3 getPointOnPlane(float x, float y) {
            return this.forward.add(this.up.scale(y)).subtract(this.left.scale(x));
        }
    }
}

