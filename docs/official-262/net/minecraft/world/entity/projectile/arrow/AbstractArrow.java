/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.projectile.arrow;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class AbstractArrow
extends Projectile {
    private static final double ARROW_BASE_DAMAGE = 2.0;
    private static final int SHAKE_TIME = 7;
    private static final float WATER_INERTIA = 0.6f;
    private static final float INERTIA = 0.99f;
    private static final short DEFAULT_LIFE = 0;
    private static final byte DEFAULT_SHAKE = 0;
    private static final boolean DEFAULT_IN_GROUND = false;
    private static final boolean DEFAULT_CRIT = false;
    private static final byte DEFAULT_PIERCE_LEVEL = 0;
    private static final EntityDataAccessor<Byte> ID_FLAGS = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> PIERCE_LEVEL = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> IN_GROUND = SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BOOLEAN);
    private static final int FLAG_CRIT = 1;
    private static final int FLAG_NOPHYSICS = 2;
    private @Nullable BlockState lastState;
    protected int inGroundTime;
    public Pickup pickup = Pickup.DISALLOWED;
    public int shakeTime = 0;
    private int life = 0;
    private double baseDamage = 2.0;
    private SoundEvent soundEvent = this.getDefaultHitGroundSoundEvent();
    private @Nullable IntOpenHashSet piercingIgnoreEntityIds;
    private @Nullable List<Entity> piercedAndKilledEntities;
    private ItemStack pickupItemStack = this.getDefaultPickupItem();
    private @Nullable ItemStack firedFromWeapon = null;

    protected AbstractArrow(EntityType<? extends AbstractArrow> type, Level level) {
        super((EntityType<? extends Projectile>)type, level);
    }

    protected AbstractArrow(EntityType<? extends AbstractArrow> type, double x, double y, double z, Level level, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        this(type, level);
        this.pickupItemStack = pickupItemStack.copy();
        this.applyComponentsFromItemStack(pickupItemStack);
        Unit intangible = pickupItemStack.remove(DataComponents.INTANGIBLE_PROJECTILE);
        if (intangible != null) {
            this.pickup = Pickup.CREATIVE_ONLY;
        }
        this.setPos(x, y, z);
        if (firedFromWeapon != null && level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (firedFromWeapon.isEmpty()) {
                throw new IllegalArgumentException("Invalid weapon firing an arrow");
            }
            this.firedFromWeapon = firedFromWeapon.copy();
            int pierceLevel = EnchantmentHelper.getPiercingCount(serverLevel, firedFromWeapon, this.pickupItemStack);
            if (pierceLevel > 0) {
                this.setPierceLevel((byte)pierceLevel);
            }
        }
    }

    protected AbstractArrow(EntityType<? extends AbstractArrow> type, LivingEntity mob, Level level, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        this(type, mob.getX(), mob.getEyeY() - (double)0.1f, mob.getZ(), level, pickupItemStack, firedFromWeapon);
        this.setOwner(mob);
    }

    public void setSoundEvent(SoundEvent soundEvent) {
        this.soundEvent = soundEvent;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(size)) {
            size = 1.0;
        }
        return distance < (size *= 64.0 * AbstractArrow.getViewScale()) * size;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(ID_FLAGS, (byte)0);
        entityData.define(PIERCE_LEVEL, (byte)0);
        entityData.define(IN_GROUND, false);
    }

    @Override
    public void shoot(double xd, double yd, double zd, float pow, float uncertainty) {
        super.shoot(xd, yd, zd, pow, uncertainty);
        this.life = 0;
    }

    @Override
    public void lerpMotion(Vec3 movement) {
        super.lerpMotion(movement);
        this.life = 0;
        if (this.isInGround() && movement.lengthSqr() > 0.0) {
            this.setInGround(false);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (!this.firstTick && this.shakeTime <= 0 && accessor.equals(IN_GROUND) && this.isInGround()) {
            this.shakeTime = 7;
        }
    }

    @Override
    public void tick() {
        VoxelShape shape;
        boolean physicsEnabled = !this.isNoPhysics();
        Vec3 movement = this.getDeltaMovement();
        BlockPos blockPos = this.blockPosition();
        BlockState blockState = this.level().getBlockState(blockPos);
        if (!blockState.isAir() && physicsEnabled && !(shape = blockState.getCollisionShape(this.level(), blockPos)).isEmpty()) {
            Vec3 position = this.position();
            for (AABB aabb : shape.toAabbs()) {
                if (!aabb.move(blockPos).contains(position)) continue;
                this.setDeltaMovement(Vec3.ZERO);
                this.setInGround(true);
                break;
            }
        }
        if (this.shakeTime > 0) {
            --this.shakeTime;
        }
        if (this.isInWaterOrRain()) {
            this.clearFire();
        }
        if (this.isInGround() && physicsEnabled) {
            if (!this.level().isClientSide()) {
                if (this.lastState != blockState && this.shouldFall()) {
                    this.startFalling();
                } else {
                    this.tickDespawn();
                }
            }
            ++this.inGroundTime;
            if (this.isAlive()) {
                this.applyEffectsFromBlocks();
            }
            if (!this.level().isClientSide()) {
                this.setSharedFlagOnFire(this.getRemainingFireTicks() > 0);
            }
            return;
        }
        this.inGroundTime = 0;
        Vec3 originalPosition = this.position();
        if (this.isInWater()) {
            this.applyInertia(this.getWaterInertia());
            this.addBubbleParticles(originalPosition);
        }
        if (this.isCritArrow()) {
            for (int i = 0; i < 4; ++i) {
                this.level().addParticle(ParticleTypes.CRIT, originalPosition.x + movement.x * (double)i / 4.0, originalPosition.y + movement.y * (double)i / 4.0, originalPosition.z + movement.z * (double)i / 4.0, -movement.x, -movement.y + 0.2, -movement.z);
            }
        }
        float yRot = !physicsEnabled ? (float)(Mth.atan2(-movement.x, -movement.z) * 57.2957763671875) : (float)(Mth.atan2(movement.x, movement.z) * 57.2957763671875);
        float xRot = (float)(Mth.atan2(movement.y, movement.horizontalDistance()) * 57.2957763671875);
        this.setXRot(AbstractArrow.lerpRotation(this.getXRot(), xRot));
        this.setYRot(AbstractArrow.lerpRotation(this.getYRot(), yRot));
        this.checkLeftOwner();
        if (physicsEnabled) {
            BlockHitResult blockHitResult = this.level().clipIncludingBorder(new ClipContext(originalPosition, originalPosition.add(movement), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            this.stepMoveAndHit(blockHitResult);
        } else {
            this.setPos(originalPosition.add(movement));
            this.applyEffectsFromBlocks();
        }
        if (!this.isInWater()) {
            this.applyInertia(this.getAirDrag());
        }
        if (physicsEnabled && !this.isInGround()) {
            this.applyGravity();
        }
        super.tick();
    }

    @Override
    protected float getAirDrag() {
        return 0.99f;
    }

    private void stepMoveAndHit(BlockHitResult blockHitResult) {
        while (this.isAlive()) {
            Vec3 initialPosition = this.position();
            ArrayList<EntityHitResult> entitiesHit = new ArrayList<EntityHitResult>(this.findHitEntities(initialPosition, blockHitResult.getLocation()));
            entitiesHit.sort(Comparator.comparingDouble(c -> initialPosition.distanceToSqr(c.getEntity().position())));
            EntityHitResult firstEntityHit = entitiesHit.isEmpty() ? null : entitiesHit.getFirst();
            Vec3 nextLocation = ((HitResult)Objects.requireNonNullElse(firstEntityHit, blockHitResult)).getLocation();
            this.setPos(nextLocation);
            this.applyEffectsFromBlocks(initialPosition, nextLocation);
            if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
                this.handlePortal();
            }
            if (entitiesHit.isEmpty()) {
                if (!this.isAlive() || blockHitResult.getType() == HitResult.Type.MISS) break;
                this.hitTargetOrDeflectSelf(blockHitResult);
                this.needsSync = true;
                break;
            }
            if (!this.isAlive() || this.noPhysics) continue;
            ProjectileDeflection deflection = this.hitTargetsOrDeflectSelf(entitiesHit);
            this.needsSync = true;
            if (this.getPierceLevel() > 0 && deflection == ProjectileDeflection.NONE) continue;
            break;
        }
    }

    private ProjectileDeflection hitTargetsOrDeflectSelf(Collection<EntityHitResult> entityHitResults) {
        for (EntityHitResult e : entityHitResults) {
            ProjectileDeflection deflection = this.hitTargetOrDeflectSelf(e);
            if (this.isAlive() && deflection == ProjectileDeflection.NONE) continue;
            return deflection;
        }
        return ProjectileDeflection.NONE;
    }

    private void applyInertia(float inertia) {
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.scale(inertia));
    }

    private void addBubbleParticles(Vec3 position) {
        Vec3 movement = this.getDeltaMovement();
        for (int i = 0; i < 4; ++i) {
            float s = 0.25f;
            this.level().addParticle(ParticleTypes.BUBBLE, position.x - movement.x * 0.25, position.y - movement.y * 0.25, position.z - movement.z * 0.25, movement.x, movement.y, movement.z);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    private boolean shouldFall() {
        return this.isInGround() && this.level().noCollision(new AABB(this.position(), this.position()).inflate(0.06));
    }

    private void startFalling() {
        this.setInGround(false);
        Vec3 deltaMovement = this.getDeltaMovement();
        this.setDeltaMovement(deltaMovement.multiply(this.random.nextFloat() * 0.2f, this.random.nextFloat() * 0.2f, this.random.nextFloat() * 0.2f));
        this.life = 0;
    }

    protected boolean isInGround() {
        return this.entityData.get(IN_GROUND);
    }

    protected void setInGround(boolean inGround) {
        this.entityData.set(IN_GROUND, inGround);
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isInGround();
    }

    @Override
    public void move(MoverType moverType, Vec3 delta) {
        super.move(moverType, delta);
        if (moverType != MoverType.SELF && this.shouldFall()) {
            this.startFalling();
        }
    }

    protected void tickDespawn() {
        ++this.life;
        if (this.life >= 1200) {
            this.discard();
        }
    }

    private void resetPiercedEntities() {
        if (this.piercedAndKilledEntities != null) {
            this.piercedAndKilledEntities.clear();
        }
        if (this.piercingIgnoreEntityIds != null) {
            this.piercingIgnoreEntityIds.clear();
        }
    }

    @Override
    public void onItemBreak(Item item) {
        this.firedFromWeapon = null;
    }

    @Override
    public void onAboveBubbleColumn(boolean dragDown, BlockPos pos) {
        if (this.isInGround()) {
            return;
        }
        super.onAboveBubbleColumn(dragDown, pos);
    }

    @Override
    public void onInsideBubbleColumn(boolean dragDown) {
        if (this.isInGround()) {
            return;
        }
        super.onInsideBubbleColumn(dragDown);
    }

    @Override
    public void push(double xa, double ya, double za) {
        if (this.isInGround()) {
            return;
        }
        super.push(xa, ya, za);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Level level;
        super.onHitEntity(hitResult);
        Entity entity = hitResult.getEntity();
        float pow = (float)this.getDeltaMovement().length();
        double arrowDamage = this.baseDamage;
        Entity currentOwner = this.getOwner();
        DamageSource damageSource = this.damageSources().arrow(this, currentOwner != null ? currentOwner : this);
        if (this.getWeaponItem() != null && (level = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            arrowDamage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), entity, damageSource, (float)arrowDamage);
        }
        int damage = Mth.ceil(Mth.clamp((double)pow * arrowDamage, 0.0, 2.147483647E9));
        if (this.getPierceLevel() > 0) {
            if (this.piercingIgnoreEntityIds == null) {
                this.piercingIgnoreEntityIds = new IntOpenHashSet(5);
            }
            if (this.piercedAndKilledEntities == null) {
                this.piercedAndKilledEntities = Lists.newArrayListWithCapacity((int)5);
            }
            if (this.piercingIgnoreEntityIds.size() < this.getPierceLevel() + 1) {
                this.piercingIgnoreEntityIds.add(entity.getId());
            } else {
                this.discard();
                return;
            }
        }
        if (this.isCritArrow()) {
            long dmgIncrease = this.random.nextInt(damage / 2 + 2);
            damage = (int)Math.min(dmgIncrease + (long)damage, Integer.MAX_VALUE);
        }
        if (currentOwner instanceof LivingEntity) {
            LivingEntity livingOwner = (LivingEntity)currentOwner;
            livingOwner.setLastHurtMob(entity);
        }
        boolean isEnderman = entity.is(EntityTypes.ENDERMAN);
        int remainingFireTicks = entity.getRemainingFireTicks();
        if (this.isOnFire() && !isEnderman) {
            entity.igniteForSeconds(5.0f);
        }
        if (entity.hurtOrSimulate(damageSource, damage)) {
            if (isEnderman) {
                return;
            }
            if (entity instanceof LivingEntity) {
                LivingEntity mob = (LivingEntity)entity;
                if (!this.level().isClientSide() && this.getPierceLevel() <= 0) {
                    mob.setArrowCount(mob.getArrowCount() + 1);
                }
                this.doKnockback(mob, damageSource);
                Level level2 = this.level();
                if (level2 instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)level2;
                    EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, mob, damageSource, this.getWeaponItem());
                }
                this.doPostHurtEffects(mob);
                if (mob instanceof Player && currentOwner instanceof ServerPlayer) {
                    ServerPlayer ownerPlayer = (ServerPlayer)currentOwner;
                    if (!this.isSilent() && mob != ownerPlayer) {
                        ownerPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND, 0.0f));
                    }
                }
                if (!entity.isAlive() && this.piercedAndKilledEntities != null) {
                    this.piercedAndKilledEntities.add(mob);
                }
                if (!this.level().isClientSide() && currentOwner instanceof ServerPlayer) {
                    ServerPlayer player = (ServerPlayer)currentOwner;
                    if (this.piercedAndKilledEntities != null) {
                        CriteriaTriggers.KILLED_BY_ARROW.trigger(player, this.piercedAndKilledEntities, this.firedFromWeapon);
                    } else if (!entity.isAlive()) {
                        CriteriaTriggers.KILLED_BY_ARROW.trigger(player, List.of(entity), this.firedFromWeapon);
                    }
                }
            }
            this.playSound(this.soundEvent, 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
            if (this.getPierceLevel() <= 0) {
                this.discard();
            }
        } else {
            entity.setRemainingFireTicks(remainingFireTicks);
            this.deflect(ProjectileDeflection.REVERSE, entity, this.owner, false);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
            Level level3 = this.level();
            if (level3 instanceof ServerLevel) {
                ServerLevel level4 = (ServerLevel)level3;
                if (this.getDeltaMovement().lengthSqr() < 1.0E-7) {
                    if (this.pickup == Pickup.ALLOWED) {
                        this.spawnAtLocation(level4, this.getPickupItem(), 0.1f);
                    }
                    this.discard();
                }
            }
        }
    }

    protected void doKnockback(LivingEntity mob, DamageSource damageSource) {
        float f;
        Level level;
        if (this.firedFromWeapon != null && (level = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            f = EnchantmentHelper.modifyKnockback(serverLevel, this.firedFromWeapon, mob, damageSource, 0.0f);
        } else {
            f = 0.0f;
        }
        double knockback = f;
        if (knockback > 0.0) {
            double knockbackResistance = Math.max(0.0, 1.0 - mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 movement = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * knockbackResistance);
            if (movement.lengthSqr() > 0.0) {
                mob.push(movement.x, 0.1, movement.z);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        this.lastState = this.level().getBlockState(hitResult.getBlockPos());
        super.onHitBlock(hitResult);
        ItemStack weaponItem = this.getWeaponItem();
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (weaponItem != null) {
                this.hitBlockEnchantmentEffects(serverLevel, hitResult, weaponItem);
            }
        }
        Vec3 movement = this.getDeltaMovement();
        Vec3 offsetDirection = new Vec3(Math.signum(movement.x), Math.signum(movement.y), Math.signum(movement.z));
        Vec3 scaledMovement = offsetDirection.scale(0.05f);
        this.setPos(this.position().subtract(scaledMovement));
        this.setDeltaMovement(Vec3.ZERO);
        this.playSound(this.getHitGroundSoundEvent(), 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
        this.setInGround(true);
        this.shakeTime = 7;
        this.setCritArrow(false);
        this.setPierceLevel((byte)0);
        this.setSoundEvent(SoundEvents.ARROW_HIT);
        this.resetPiercedEntities();
    }

    protected void hitBlockEnchantmentEffects(ServerLevel serverLevel, BlockHitResult hitResult, ItemStack weapon) {
        LivingEntity livingOwner;
        Vec3 compensatedHitPosition = hitResult.getBlockPos().clampLocationWithin(hitResult.getLocation());
        Entity entity = this.getOwner();
        EnchantmentHelper.onHitBlock(serverLevel, weapon, entity instanceof LivingEntity ? (livingOwner = (LivingEntity)entity) : null, this, null, compensatedHitPosition, serverLevel.getBlockState(hitResult.getBlockPos()), item -> {
            this.firedFromWeapon = null;
        });
    }

    @Override
    public @Nullable ItemStack getWeaponItem() {
        return this.firedFromWeapon;
    }

    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.ARROW_HIT;
    }

    protected final SoundEvent getHitGroundSoundEvent() {
        return this.soundEvent;
    }

    protected void doPostHurtEffects(LivingEntity mob) {
    }

    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, from, to, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity);
    }

    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        return ProjectileUtil.getManyEntityHitResult(this.level(), this, from, to, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity, false);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof Player) {
            Player player;
            Player playerEntity = (Player)entity;
            Entity entity2 = this.getOwner();
            if (entity2 instanceof Player && !(player = (Player)entity2).canHarmPlayer(playerEntity)) {
                return false;
            }
        }
        return super.canHitEntity(entity) && (this.piercingIgnoreEntityIds == null || !this.piercingIgnoreEntityIds.contains(entity.getId()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putShort("life", (short)this.life);
        output.storeNullable("inBlockState", BlockState.CODEC, this.lastState);
        output.putByte("shake", (byte)this.shakeTime);
        output.putBoolean("inGround", this.isInGround());
        output.store("pickup", Pickup.LEGACY_CODEC, this.pickup);
        output.putDouble("damage", this.baseDamage);
        output.putBoolean("crit", this.isCritArrow());
        output.putByte("PierceLevel", this.getPierceLevel());
        output.store("SoundEvent", BuiltInRegistries.SOUND_EVENT.byNameCodec(), this.soundEvent);
        output.store("item", ItemStack.CODEC, this.pickupItemStack);
        output.storeNullable("weapon", ItemStack.CODEC, this.firedFromWeapon);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.life = input.getShortOr("life", (short)0);
        this.lastState = input.read("inBlockState", BlockState.CODEC).orElse(null);
        this.shakeTime = input.getByteOr("shake", (byte)0) & 0xFF;
        this.setInGround(input.getBooleanOr("inGround", false));
        this.baseDamage = input.getDoubleOr("damage", 2.0);
        this.pickup = input.read("pickup", Pickup.LEGACY_CODEC).orElse(Pickup.DISALLOWED);
        this.setCritArrow(input.getBooleanOr("crit", false));
        this.setPierceLevel(input.getByteOr("PierceLevel", (byte)0));
        this.soundEvent = input.read("SoundEvent", BuiltInRegistries.SOUND_EVENT.byNameCodec()).orElse(this.getDefaultHitGroundSoundEvent());
        this.setPickupItemStack(input.read("item", ItemStack.CODEC).orElse(this.getDefaultPickupItem()));
        this.firedFromWeapon = input.read("weapon", ItemStack.CODEC).orElse(null);
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        Pickup pickup;
        super.setOwner(owner);
        Entity entity = owner;
        int n = 0;
        block4: while (true) {
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Player.class, OminousItemSpawner.class}, (Entity)entity, n)) {
                case 0: {
                    Player ignored = (Player)entity;
                    if (this.pickup != Pickup.DISALLOWED) {
                        n = 1;
                        continue block4;
                    }
                    pickup = Pickup.ALLOWED;
                    break block4;
                }
                case 1: {
                    OminousItemSpawner ignored = (OminousItemSpawner)entity;
                    pickup = Pickup.DISALLOWED;
                    break block4;
                }
                default: {
                    pickup = this.pickup;
                    break block4;
                }
            }
            break;
        }
        this.pickup = pickup;
    }

    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide() || !this.isInGround() && !this.isNoPhysics() || this.shakeTime > 0) {
            return;
        }
        if (this.tryPickup(player)) {
            player.take(this, 1);
            this.discard();
        }
    }

    protected boolean tryPickup(Player player) {
        return switch (this.pickup.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> false;
            case 1 -> player.getInventory().add(this.getPickupItem());
            case 2 -> player.hasInfiniteMaterials();
        };
    }

    protected ItemStack getPickupItem() {
        return this.pickupItemStack.copy();
    }

    protected abstract ItemStack getDefaultPickupItem();

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    public ItemStack getPickupItemStackOrigin() {
        return this.pickupItemStack;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    @Override
    public boolean isAttackable() {
        return this.is(EntityTypeTags.REDIRECTABLE_PROJECTILE);
    }

    public void setCritArrow(boolean critArrow) {
        this.setFlag(1, critArrow);
    }

    private void setPierceLevel(byte pieceLevel) {
        this.entityData.set(PIERCE_LEVEL, pieceLevel);
    }

    private void setFlag(int flag, boolean value) {
        byte flags = this.entityData.get(ID_FLAGS);
        if (value) {
            this.entityData.set(ID_FLAGS, (byte)(flags | flag));
        } else {
            this.entityData.set(ID_FLAGS, (byte)(flags & ~flag));
        }
    }

    protected void setPickupItemStack(ItemStack itemStack) {
        this.pickupItemStack = !itemStack.isEmpty() ? itemStack : this.getDefaultPickupItem();
    }

    public boolean isCritArrow() {
        byte flags = this.entityData.get(ID_FLAGS);
        return (flags & 1) != 0;
    }

    public byte getPierceLevel() {
        return this.entityData.get(PIERCE_LEVEL);
    }

    public void setBaseDamageFromMob(float power) {
        this.setBaseDamage((double)(power * 2.0f) + this.random.triangle((double)this.level().getDifficulty().getId() * 0.11, 0.57425));
    }

    protected float getWaterInertia() {
        return 0.6f;
    }

    public void setNoPhysics(boolean noPhysics) {
        this.noPhysics = noPhysics;
        this.setFlag(2, noPhysics);
    }

    public boolean isNoPhysics() {
        if (!this.level().isClientSide()) {
            return this.noPhysics;
        }
        return (this.entityData.get(ID_FLAGS) & 2) != 0;
    }

    @Override
    public boolean isPickable() {
        return super.isPickable() && !this.isInGround();
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        if (slot == 0) {
            return SlotAccess.of(this::getPickupItemStackOrigin, this::setPickupItemStack);
        }
        return super.getSlot(slot);
    }

    @Override
    protected boolean shouldBounceOnWorldBorder() {
        return true;
    }

    public static enum Pickup {
        DISALLOWED,
        ALLOWED,
        CREATIVE_ONLY;

        public static final Codec<Pickup> LEGACY_CODEC;

        public static Pickup byOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal > Pickup.values().length) {
                ordinal = 0;
            }
            return Pickup.values()[ordinal];
        }

        static {
            LEGACY_CODEC = Codec.BYTE.xmap(Pickup::byOrdinal, p -> (byte)p.ordinal());
        }
    }
}

