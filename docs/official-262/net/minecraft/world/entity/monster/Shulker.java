/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class Shulker
extends AbstractGolem
implements Enemy {
    private static final Identifier COVERED_ARMOR_MODIFIER_ID = Identifier.withDefaultNamespace("covered");
    private static final AttributeModifier COVERED_ARMOR_MODIFIER = new AttributeModifier(COVERED_ARMOR_MODIFIER_ID, 20.0, AttributeModifier.Operation.ADD_VALUE);
    protected static final EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.DIRECTION);
    protected static final EntityDataAccessor<Byte> DATA_PEEK_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> DATA_COLOR_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
    private static final int TELEPORT_STEPS = 6;
    private static final byte NO_COLOR = 16;
    private static final byte DEFAULT_COLOR = 16;
    private static final int MAX_TELEPORT_DISTANCE = 8;
    private static final int OTHER_SHULKER_SCAN_RADIUS = 8;
    private static final int OTHER_SHULKER_LIMIT = 5;
    private static final float PEEK_PER_TICK = 0.05f;
    private static final byte DEFAULT_PEEK = 0;
    private static final Direction DEFAULT_ATTACH_FACE = Direction.DOWN;
    private static final Vector3fc FORWARD = Direction.SOUTH.getUnitVec3f();
    private static final float MAX_SCALE = 3.0f;
    private float currentPeekAmountO;
    private float currentPeekAmount;
    private @Nullable BlockPos clientOldAttachPosition;
    private int clientSideTeleportInterpolation;
    private static final float MAX_LID_OPEN = 1.0f;

    public Shulker(EntityType<? extends Shulker> type, Level level) {
        super((EntityType<? extends AbstractGolem>)type, level);
        this.xpReward = 5;
        this.lookControl = new ShulkerLookControl(this, this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f, 0.02f, true));
        this.goalSelector.addGoal(4, new ShulkerAttackGoal(this));
        this.goalSelector.addGoal(7, new ShulkerPeekGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, this.getClass()).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(2, new ShulkerNearestAttackGoal(this, this));
        this.targetSelector.addGoal(3, new ShulkerDefenseAttackGoal(this));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHULKER_AMBIENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isClosed()) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHULKER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.isClosed()) {
            return SoundEvents.SHULKER_HURT_CLOSED;
        }
        return SoundEvents.SHULKER_HURT;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ATTACH_FACE_ID, DEFAULT_ATTACH_FACE);
        entityData.define(DATA_PEEK_ID, (byte)0);
        entityData.define(DATA_COLOR_ID, (byte)16);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new ShulkerBodyRotationControl(this);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setAttachFace(input.read("AttachFace", Direction.LEGACY_ID_CODEC).orElse(DEFAULT_ATTACH_FACE));
        this.entityData.set(DATA_PEEK_ID, input.getByteOr("Peek", (byte)0));
        this.entityData.set(DATA_COLOR_ID, input.getByteOr("Color", (byte)16));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("AttachFace", Direction.LEGACY_ID_CODEC, this.getAttachFace());
        output.putByte("Peek", this.entityData.get(DATA_PEEK_ID));
        output.putByte("Color", this.entityData.get(DATA_COLOR_ID));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level().isClientSide() || this.isPassenger() || this.canStayAt(this.blockPosition(), this.getAttachFace()))) {
            this.findNewAttachment();
        }
        if (this.updatePeekAmount()) {
            this.onPeekAmountChange();
        }
        if (this.level().isClientSide()) {
            if (this.clientSideTeleportInterpolation > 0) {
                --this.clientSideTeleportInterpolation;
            } else {
                this.clientOldAttachPosition = null;
            }
        }
    }

    private void findNewAttachment() {
        Direction attachmentDirection = this.findAttachableSurface(this.blockPosition());
        if (attachmentDirection != null) {
            this.setAttachFace(attachmentDirection);
        } else {
            this.teleportSomewhere();
        }
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        float physPeek = Shulker.getPhysicalPeek(this.currentPeekAmount);
        Direction direction = this.getAttachFace().getOpposite();
        return Shulker.getProgressAabb(this.getScale(), direction, physPeek, position);
    }

    private static float getPhysicalPeek(float amount) {
        return 0.5f - Mth.sin((0.5f + amount) * (float)Math.PI) * 0.5f;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimension = super.getDefaultDimensions(pose);
        if (this.getAttachFace() == Direction.DOWN && this.currentPeekAmount > 0.0f) {
            float heightScaleFactor = 1.0f + this.currentPeekAmount;
            return dimension.scale(1.0f, heightScaleFactor);
        }
        return dimension;
    }

    private boolean updatePeekAmount() {
        this.currentPeekAmountO = this.currentPeekAmount;
        float targetPeekAmount = (float)this.getRawPeekAmount() * 0.01f;
        if (this.currentPeekAmount == targetPeekAmount) {
            return false;
        }
        this.currentPeekAmount = this.currentPeekAmount > targetPeekAmount ? Mth.clamp(this.currentPeekAmount - 0.05f, targetPeekAmount, 1.0f) : Mth.clamp(this.currentPeekAmount + 0.05f, 0.0f, targetPeekAmount);
        return true;
    }

    private void onPeekAmountChange() {
        this.reapplyPosition();
        this.refreshDimensions();
        float physicalPeek = Shulker.getPhysicalPeek(this.currentPeekAmount);
        float physicalPeekOld = Shulker.getPhysicalPeek(this.currentPeekAmountO);
        Direction direction = this.getAttachFace().getOpposite();
        float push = (physicalPeek - physicalPeekOld) * this.getScale();
        if (push <= 0.0f) {
            return;
        }
        List<Entity> entities = this.level().getEntities(this, Shulker.getProgressDeltaAabb(this.getScale(), direction, physicalPeekOld, physicalPeek, this.position()), EntitySelector.NO_SPECTATORS.and(e -> !e.isPassengerOfSameVehicle(this)));
        for (Entity entity : entities) {
            if (entity instanceof Shulker || entity.noPhysics) continue;
            entity.move(MoverType.SHULKER, new Vec3(push * (float)direction.getStepX(), push * (float)direction.getStepY(), push * (float)direction.getStepZ()));
        }
    }

    public static AABB getProgressAabb(float size, Direction direction, float progressTo, Vec3 position) {
        return Shulker.getProgressDeltaAabb(size, direction, -1.0f, progressTo, position);
    }

    public static AABB getProgressDeltaAabb(float size, Direction direction, float progressFrom, float progressTo, Vec3 position) {
        AABB boundsAtBottomCenter = new AABB((double)(-size) * 0.5, 0.0, (double)(-size) * 0.5, (double)size * 0.5, size, (double)size * 0.5);
        double maxMovement = Math.max(progressFrom, progressTo);
        double minMovement = Math.min(progressFrom, progressTo);
        AABB aabb = boundsAtBottomCenter.expandTowards((double)direction.getStepX() * maxMovement * (double)size, (double)direction.getStepY() * maxMovement * (double)size, (double)direction.getStepZ() * maxMovement * (double)size).contract((double)(-direction.getStepX()) * (1.0 + minMovement) * (double)size, (double)(-direction.getStepY()) * (1.0 + minMovement) * (double)size, (double)(-direction.getStepZ()) * (1.0 + minMovement) * (double)size);
        return aabb.move(position.x, position.y, position.z);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force, boolean sendEventAndTriggers) {
        if (this.level().isClientSide()) {
            this.clientOldAttachPosition = null;
            this.clientSideTeleportInterpolation = 0;
        }
        this.setAttachFace(Direction.DOWN);
        return super.startRiding(entity, force, sendEventAndTriggers);
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        if (this.level().isClientSide()) {
            this.clientOldAttachPosition = this.blockPosition();
        }
        this.yBodyRotO = 0.0f;
        this.yBodyRot = 0.0f;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.setYRot(0.0f);
        this.yHeadRot = this.getYRot();
        this.setOldPosAndRot();
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    public void move(MoverType moverType, Vec3 delta) {
        if (moverType == MoverType.SHULKER_BOX) {
            this.teleportSomewhere();
        } else {
            super.move(moverType, delta);
        }
    }

    @Override
    public Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

    @Override
    public void setDeltaMovement(Vec3 deltaMovement) {
    }

    @Override
    public void setPos(double x, double y, double z) {
        BlockPos oldPos = this.blockPosition();
        if (this.isPassenger()) {
            super.setPos(x, y, z);
        } else {
            super.setPos((double)Mth.floor(x) + 0.5, Mth.floor(y + 0.5), (double)Mth.floor(z) + 0.5);
        }
        if (this.tickCount == 0) {
            return;
        }
        BlockPos pos = this.blockPosition();
        if (!pos.equals(oldPos)) {
            this.entityData.set(DATA_PEEK_ID, (byte)0);
            this.needsSync = true;
            if (this.level().isClientSide() && !this.isPassenger() && !pos.equals(this.clientOldAttachPosition)) {
                this.clientOldAttachPosition = oldPos;
                this.clientSideTeleportInterpolation = 6;
                this.xOld = this.getX();
                this.yOld = this.getY();
                this.zOld = this.getZ();
            }
        }
    }

    protected @Nullable Direction findAttachableSurface(BlockPos target) {
        for (Direction direction : Direction.values()) {
            if (!this.canStayAt(target, direction)) continue;
            return direction;
        }
        return null;
    }

    private boolean canStayAt(BlockPos target, Direction face) {
        if (this.isPositionBlocked(target)) {
            return false;
        }
        Direction oppositeFace = face.getOpposite();
        if (!this.level().loadedAndEntityCanStandOnFace(target.relative(face), this, oppositeFace)) {
            return false;
        }
        AABB fullyOpened = Shulker.getProgressAabb(this.getScale(), oppositeFace, 1.0f, Vec3.atBottomCenterOf(target)).deflate(1.0E-6);
        return this.level().noCollision(this, fullyOpened);
    }

    private boolean isPositionBlocked(BlockPos target) {
        BlockState state = this.level().getBlockState(target);
        if (state.isAir()) {
            return false;
        }
        boolean movingPistonInOurCurrentPosition = state.is(Blocks.MOVING_PISTON) && target.equals(this.blockPosition());
        return !movingPistonInOurCurrentPosition;
    }

    protected boolean teleportSomewhere() {
        if (this.isNoAi() || !this.isAlive()) {
            return false;
        }
        BlockPos current = this.blockPosition();
        for (int attempt = 0; attempt < 5; ++attempt) {
            Direction attachmentDirection;
            BlockPos target = current.offset(Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8));
            if (target.getY() <= this.level().getMinY() || !this.level().isEmptyBlock(target) || !this.level().getWorldBorder().isWithinBounds(target) || !this.level().noCollision(this, new AABB(target).deflate(1.0E-6)) || (attachmentDirection = this.findAttachableSurface(target)) == null) continue;
            this.unRide();
            this.setAttachFace(attachmentDirection);
            this.playSound(SoundEvents.SHULKER_TELEPORT, 1.0f, 1.0f);
            this.setPos((double)target.getX() + 0.5, target.getY(), (double)target.getZ() + 0.5);
            this.level().gameEvent(GameEvent.TELEPORT, current, GameEvent.Context.of(this));
            this.entityData.set(DATA_PEEK_ID, (byte)0);
            this.setTarget(null);
            return true;
        }
        return false;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity directEntity;
        if (this.isClosed() && (directEntity = source.getDirectEntity()) instanceof AbstractArrow) {
            return false;
        }
        if (super.hurtServer(level, source, damage)) {
            if ((double)this.getHealth() < (double)this.getMaxHealth() * 0.5 && this.random.nextInt(4) == 0) {
                this.teleportSomewhere();
            } else if (source.is(DamageTypeTags.IS_PROJECTILE) && (directEntity = source.getDirectEntity()) != null && directEntity.is(EntityTypes.SHULKER_BULLET)) {
                this.hitByShulkerBullet();
            }
            return true;
        }
        return false;
    }

    private boolean isClosed() {
        return this.getRawPeekAmount() == 0;
    }

    private void hitByShulkerBullet() {
        Vec3 oldPosition = this.position();
        AABB oldAabb = this.getBoundingBox();
        if (this.isClosed() || !this.teleportSomewhere()) {
            return;
        }
        int shulkerCount = this.level().getEntities(EntityTypes.SHULKER, oldAabb.inflate(8.0), Entity::isAlive).size();
        float failureChance = (float)(shulkerCount - 1) / 5.0f;
        if (this.level().getRandom().nextFloat() < failureChance) {
            return;
        }
        Shulker baby = EntityTypes.SHULKER.create(this.level(), EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setVariant(this.getVariant());
            baby.snapTo(oldPosition);
            this.level().addFreshEntity(baby);
        }
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return this.isAlive();
    }

    public Direction getAttachFace() {
        return this.entityData.get(DATA_ATTACH_FACE_ID);
    }

    private void setAttachFace(Direction attachmentDirection) {
        this.entityData.set(DATA_ATTACH_FACE_ID, attachmentDirection);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_ATTACH_FACE_ID.equals(accessor)) {
            this.setBoundingBox(this.makeBoundingBox());
        }
        super.onSyncedDataUpdated(accessor);
    }

    private int getRawPeekAmount() {
        return this.entityData.get(DATA_PEEK_ID).byteValue();
    }

    private void setRawPeekAmount(int amount) {
        if (!this.level().isClientSide()) {
            this.getAttribute(Attributes.ARMOR).removeModifier(COVERED_ARMOR_MODIFIER_ID);
            if (amount == 0) {
                this.getAttribute(Attributes.ARMOR).addPermanentModifier(COVERED_ARMOR_MODIFIER);
                this.playSound(SoundEvents.SHULKER_CLOSE, 1.0f, 1.0f);
                this.gameEvent(GameEvent.CONTAINER_CLOSE);
            } else {
                this.playSound(SoundEvents.SHULKER_OPEN, 1.0f, 1.0f);
                this.gameEvent(GameEvent.CONTAINER_OPEN);
            }
        }
        this.entityData.set(DATA_PEEK_ID, (byte)amount);
    }

    public float getClientPeekAmount(float a) {
        return Mth.lerp(a, this.currentPeekAmountO, this.currentPeekAmount);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.yBodyRot = 0.0f;
        this.yBodyRotO = 0.0f;
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    public int getMaxHeadYRot() {
        return 180;
    }

    @Override
    public void push(Entity entity) {
    }

    public @Nullable Vec3 getRenderPosition(float a) {
        if (this.clientOldAttachPosition == null || this.clientSideTeleportInterpolation <= 0) {
            return null;
        }
        double scale = (double)((float)this.clientSideTeleportInterpolation - a) / 6.0;
        scale *= scale;
        BlockPos currentPos = this.blockPosition();
        double ox = (double)(currentPos.getX() - this.clientOldAttachPosition.getX()) * (scale *= (double)this.getScale());
        double oy = (double)(currentPos.getY() - this.clientOldAttachPosition.getY()) * scale;
        double oz = (double)(currentPos.getZ() - this.clientOldAttachPosition.getZ()) * scale;
        return new Vec3(-ox, -oy, -oz);
    }

    @Override
    protected float sanitizeScale(float scale) {
        return Math.min(scale, 3.0f);
    }

    private void setVariant(Optional<DyeColor> color) {
        this.entityData.set(DATA_COLOR_ID, color.map(dyeColor -> (byte)dyeColor.getId()).orElse((byte)16));
    }

    public Optional<DyeColor> getVariant() {
        return Optional.ofNullable(this.getColor());
    }

    public @Nullable DyeColor getColor() {
        byte color = this.entityData.get(DATA_COLOR_ID);
        if (color == 16 || color > 15) {
            return null;
        }
        return DyeColor.byId(color);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.SHULKER_COLOR) {
            return Shulker.castComponentValue(type, this.getColor());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.SHULKER_COLOR);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.SHULKER_COLOR) {
            this.setVariant(Optional.of(Shulker.castComponentValue(DataComponents.SHULKER_COLOR, value)));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    private class ShulkerLookControl
    extends LookControl {
        final /* synthetic */ Shulker this$0;

        public ShulkerLookControl(Shulker shulker, Mob mob) {
            Shulker shulker2 = shulker;
            Objects.requireNonNull(shulker2);
            this.this$0 = shulker2;
            super(mob);
        }

        @Override
        protected void clampHeadRotationToBody() {
        }

        @Override
        protected Optional<Float> getYRotD() {
            Direction attachFace = this.this$0.getAttachFace().getOpposite();
            Vector3f forward = attachFace.getRotation().transform(new Vector3f(FORWARD));
            Vec3i upNormal = attachFace.getUnitVec3i();
            Vector3f right = new Vector3f((float)upNormal.getX(), (float)upNormal.getY(), (float)upNormal.getZ());
            right.cross((Vector3fc)forward);
            double xd = this.wantedX - this.mob.getX();
            double yd = this.wantedY - this.mob.getEyeY();
            double zd = this.wantedZ - this.mob.getZ();
            Vector3f out = new Vector3f((float)xd, (float)yd, (float)zd);
            float deltaRight = right.dot((Vector3fc)out);
            float deltaForward = forward.dot((Vector3fc)out);
            return Math.abs(deltaRight) > 1.0E-5f || Math.abs(deltaForward) > 1.0E-5f ? Optional.of(Float.valueOf((float)(Mth.atan2(-deltaRight, deltaForward) * 57.2957763671875))) : Optional.empty();
        }

        @Override
        protected Optional<Float> getXRotD() {
            return Optional.of(Float.valueOf(0.0f));
        }
    }

    private class ShulkerAttackGoal
    extends Goal {
        private int attackTime;
        final /* synthetic */ Shulker this$0;

        public ShulkerAttackGoal(Shulker shulker) {
            Shulker shulker2 = shulker;
            Objects.requireNonNull(shulker2);
            this.this$0 = shulker2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.this$0.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            return this.this$0.level().getDifficulty() != Difficulty.PEACEFUL;
        }

        @Override
        public void start() {
            this.attackTime = 20;
            this.this$0.setRawPeekAmount(100);
        }

        @Override
        public void stop() {
            this.this$0.setRawPeekAmount(0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.this$0.level().getDifficulty() == Difficulty.PEACEFUL) {
                return;
            }
            --this.attackTime;
            LivingEntity target = this.this$0.getTarget();
            if (target == null) {
                return;
            }
            this.this$0.getLookControl().setLookAt(target, 180.0f, 180.0f);
            double distance = this.this$0.distanceToSqr(target);
            if (distance < 400.0) {
                if (this.attackTime <= 0) {
                    this.attackTime = 20 + this.this$0.random.nextInt(10) * 20 / 2;
                    this.this$0.level().addFreshEntity(new ShulkerBullet(this.this$0.level(), this.this$0, target, this.this$0.getAttachFace().getAxis()));
                    this.this$0.playSound(SoundEvents.SHULKER_SHOOT, 2.0f, (this.this$0.random.nextFloat() - this.this$0.random.nextFloat()) * 0.2f + 1.0f);
                }
            } else {
                this.this$0.setTarget(null);
            }
            super.tick();
        }
    }

    private class ShulkerPeekGoal
    extends Goal {
        private int peekTime;
        final /* synthetic */ Shulker this$0;

        private ShulkerPeekGoal(Shulker shulker) {
            Shulker shulker2 = shulker;
            Objects.requireNonNull(shulker2);
            this.this$0 = shulker2;
        }

        @Override
        public boolean canUse() {
            return this.this$0.getTarget() == null && this.this$0.random.nextInt(ShulkerPeekGoal.reducedTickDelay(40)) == 0 && this.this$0.canStayAt(this.this$0.blockPosition(), this.this$0.getAttachFace());
        }

        @Override
        public boolean canContinueToUse() {
            return this.this$0.getTarget() == null && this.peekTime > 0;
        }

        @Override
        public void start() {
            this.peekTime = this.adjustedTickDelay(20 * (1 + this.this$0.random.nextInt(3)));
            this.this$0.setRawPeekAmount(30);
        }

        @Override
        public void stop() {
            if (this.this$0.getTarget() == null) {
                this.this$0.setRawPeekAmount(0);
            }
        }

        @Override
        public void tick() {
            --this.peekTime;
        }
    }

    private class ShulkerNearestAttackGoal
    extends NearestAttackableTargetGoal<Player> {
        final /* synthetic */ Shulker this$0;

        public ShulkerNearestAttackGoal(Shulker shulker, Shulker mob) {
            Shulker shulker2 = shulker;
            Objects.requireNonNull(shulker2);
            this.this$0 = shulker2;
            super((Mob)mob, Player.class, true);
        }

        @Override
        public boolean canUse() {
            if (this.this$0.level().getDifficulty() == Difficulty.PEACEFUL) {
                return false;
            }
            return super.canUse();
        }

        @Override
        protected AABB getTargetSearchArea(double followDistance) {
            Direction attachFace = ((Shulker)this.mob).getAttachFace();
            if (attachFace.getAxis() == Direction.Axis.X) {
                return this.mob.getBoundingBox().inflate(4.0, followDistance, followDistance);
            }
            if (attachFace.getAxis() == Direction.Axis.Z) {
                return this.mob.getBoundingBox().inflate(followDistance, followDistance, 4.0);
            }
            return this.mob.getBoundingBox().inflate(followDistance, 4.0, followDistance);
        }
    }

    private static class ShulkerDefenseAttackGoal
    extends NearestAttackableTargetGoal<LivingEntity> {
        public ShulkerDefenseAttackGoal(Shulker mob) {
            super(mob, LivingEntity.class, 10, true, false, (input, level) -> input instanceof Enemy);
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTeam() == null) {
                return false;
            }
            return super.canUse();
        }

        @Override
        protected AABB getTargetSearchArea(double followDistance) {
            Direction attachFace = ((Shulker)this.mob).getAttachFace();
            if (attachFace.getAxis() == Direction.Axis.X) {
                return this.mob.getBoundingBox().inflate(4.0, followDistance, followDistance);
            }
            if (attachFace.getAxis() == Direction.Axis.Z) {
                return this.mob.getBoundingBox().inflate(followDistance, followDistance, 4.0);
            }
            return this.mob.getBoundingBox().inflate(followDistance, 4.0, followDistance);
        }
    }

    private static class ShulkerBodyRotationControl
    extends BodyRotationControl {
        public ShulkerBodyRotationControl(Mob mob) {
            super(mob);
        }

        @Override
        public void clientTick() {
        }
    }
}

