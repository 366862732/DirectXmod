/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.nautilus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.nautilus.NautilusAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractNautilus
extends TamableAnimal
implements PlayerRideableJumping,
HasCustomInventoryScreen {
    public static final int INVENTORY_SLOT_OFFSET = 500;
    public static final int INVENTORY_ROWS = 3;
    public static final int SMALL_RESTRICTION_RADIUS = 16;
    public static final int LARGE_RESTRICTION_RADIUS = 32;
    public static final int RESTRICTION_RADIUS_BUFFER = 8;
    private static final int EFFECT_DURATION = 60;
    private static final int EFFECT_REFRESH_RATE = 40;
    private static final double NAUTILUS_WATER_RESISTANCE = 0.9;
    private static final float IN_WATER_SPEED_MODIFIER = 0.011f;
    private static final float RIDDEN_SPEED_MODIFIER_IN_WATER = 0.0325f;
    private static final float RIDDEN_SPEED_MODIFIER_ON_LAND = 0.02f;
    private static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(AbstractNautilus.class, EntityDataSerializers.BOOLEAN);
    private static final int DASH_COOLDOWN_TICKS = 40;
    private static final int DASH_MINIMUM_DURATION_TICKS = 5;
    private static final float DASH_MOMENTUM_IN_WATER = 1.2f;
    private static final float DASH_MOMENTUM_ON_LAND = 0.5f;
    private int dashCooldown = 0;
    protected float playerJumpPendingScale;
    protected SimpleContainer inventory;
    private static final double BUBBLE_SPREAD_FACTOR = 0.8;
    private static final double BUBBLE_DIRECTION_SCALE = 1.1;
    private static final double BUBBLE_Y_OFFSET = 0.25;
    private static final double BUBBLE_PROBABILITY_MULTIPLIER = 2.0;
    private static final float BUBBLE_PROBABILITY_MIN = 0.15f;
    private static final float BUBBLE_PROBABILITY_MAX = 1.0f;

    protected AbstractNautilus(EntityType<? extends AbstractNautilus> type, Level level) {
        super((EntityType<? extends TamableAnimal>)type, level);
        this.moveControl = new SmoothSwimmingMoveControl<AbstractNautilus>(this, 85, 10, 0.011f, 0.0f, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(PathType.WATER, 0.0f);
        this.createInventory();
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return this.isTame() || this.isBaby() ? itemStack.is(ItemTags.NAUTILUS_FOOD) : itemStack.is(ItemTags.NAUTILUS_TAMING_ITEMS);
    }

    @Override
    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack itemStack) {
        if (itemStack.is(ItemTags.NAUTILUS_BUCKET_FOOD)) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.WATER_BUCKET)));
        } else {
            super.usePlayerItem(player, hand, itemStack);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.MOVEMENT_SPEED, 1.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3f);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0f;
    }

    public static boolean checkNautilusSpawnRules(EntityType<? extends AbstractNautilus> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        int seaLevel = level.getSeaLevel();
        int minSpawnLevel = seaLevel - 25;
        return pos.getY() >= minSpawnLevel && pos.getY() <= seaLevel - 5 && level.getFluidState(pos.below()).is(FluidTags.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.SADDLE || slot == EquipmentSlot.BODY) {
            return this.isAlive() && !this.isBaby() && this.isTame();
        }
        return super.canUseSlot(slot);
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity firstPassenger = this.getFirstPassenger();
        if (this.isSaddled() && firstPassenger instanceof Player) {
            Player player = (Player)firstPassenger;
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa;
        float forward = 0.0f;
        float up = 0.0f;
        if (controller.zza != 0.0f) {
            float forwardLook = Mth.cos(controller.getXRot() * ((float)Math.PI / 180));
            float upLook = -Mth.sin(controller.getXRot() * ((float)Math.PI / 180));
            if (controller.zza < 0.0f) {
                forwardLook *= -0.5f;
                upLook *= -0.5f;
            }
            up = upLook;
            forward = forwardLook;
        }
        return new Vec3(strafe, up, forward);
    }

    protected Vec2 getRiddenRotation(LivingEntity controller) {
        return new Vec2(controller.getXRot() * 0.5f, controller.getYRot());
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        Vec2 rotation = this.getRiddenRotation(controller);
        float yRot = this.getYRot();
        float diff = Mth.wrapDegrees(rotation.y - yRot);
        float turnSpeed = 0.5f;
        this.setRot(yRot += diff * 0.5f, rotation.x);
        this.yBodyRot = this.yHeadRot = yRot;
        this.yRotO = this.yHeadRot;
        if (this.isLocalInstanceAuthoritative()) {
            if (this.playerJumpPendingScale > 0.0f && !this.isJumping()) {
                this.executeRidersJump(this.playerJumpPendingScale, controller);
            }
            this.playerJumpPendingScale = 0.0f;
        }
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        float speed = this.getSpeed();
        this.moveRelative(speed, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return this.isInWater() ? 0.0325f * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0.02f * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    protected void doPlayerRide(Player player) {
        if (!this.level().isClientSide()) {
            player.startRiding(this);
            if (!this.isVehicle()) {
                this.clearHome();
            }
        }
    }

    private int getNautilusRestrictionRadius() {
        if (!this.isBaby() && this.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
            return 32;
        }
        return 16;
    }

    protected void checkRestriction() {
        if (this.isLeashed() || this.isVehicle() || !this.isTame()) {
            return;
        }
        int radius = this.getNautilusRestrictionRadius();
        if (this.hasHome() && this.getHomePosition().closerThan(this.blockPosition(), radius + 8) && radius == this.getHomeRadius()) {
            return;
        }
        this.setHomeTo(this.blockPosition(), radius);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        this.checkRestriction();
        super.customServerAiStep(level);
    }

    private void applyEffects(Level level) {
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player) {
            boolean shouldRefresh;
            Player player = (Player)passenger;
            boolean hasEffect = player.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
            boolean bl = shouldRefresh = level.getGameTime() % 40L == 0L;
            if (!hasEffect || shouldRefresh) {
                player.addEffect(new MobEffectInstance(MobEffects.BREATH_OF_THE_NAUTILUS, 60, 0, true, true, true));
            }
        }
    }

    private void spawnBubbles() {
        double speed = this.getDeltaMovement().length();
        double bubbleProbability = Mth.clamp(speed * 2.0, (double)0.15f, 1.0);
        if ((double)this.random.nextFloat() < bubbleProbability) {
            float yRot = this.getYRot();
            float xRot = Mth.clamp(this.getXRot(), -10.0f, 10.0f);
            Vec3 mouthDirectionVector = this.calculateViewVector(xRot, yRot);
            double spread = this.random.nextDouble() * 0.8 * (1.0 + speed);
            double dx = ((double)this.random.nextFloat() - 0.5) * spread;
            double dy = ((double)this.random.nextFloat() - 0.5) * spread;
            double dz = ((double)this.random.nextFloat() - 0.5) * spread;
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - mouthDirectionVector.x * 1.1, this.getY() - mouthDirectionVector.y + 0.25, this.getZ() - mouthDirectionVector.z * 1.1, dx, dy, dz);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.applyEffects(this.level());
        }
        if (this.isDashing() && this.dashCooldown < 35) {
            this.setDashing(false);
        }
        if (this.dashCooldown > 0) {
            --this.dashCooldown;
            if (this.dashCooldown == 0) {
                this.makeSound(this.getDashReadySound());
            }
        }
        if (this.isInWater()) {
            this.spawnBubbles();
        }
    }

    @Override
    public boolean canJump() {
        return this.isSaddled();
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
        if (!this.isSaddled() || this.dashCooldown > 0) {
            return;
        }
        this.playerJumpPendingScale = this.getPlayerJumpPendingScale(jumpAmount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DASH, false);
    }

    public boolean isDashing() {
        return this.entityData.get(DASH);
    }

    public void setDashing(boolean isDashing) {
        this.entityData.set(DASH, isDashing);
    }

    protected void executeRidersJump(float amount, Player controller) {
        this.addDeltaMovement(controller.getLookAngle().scale((double)((this.isInWater() ? 1.2f : 0.5f) * amount) * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double)this.getBlockSpeedFactor()));
        this.dashCooldown = 40;
        this.setDashing(true);
        this.needsSync = true;
    }

    @Override
    public void handleStartJump(int jumpScale) {
        this.makeSound(this.getDashSound());
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setDashing(true);
    }

    @Override
    public int getJumpCooldown() {
        return this.dashCooldown;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (!this.firstTick && DASH.equals(accessor)) {
            this.dashCooldown = this.dashCooldown == 0 ? 40 : this.dashCooldown;
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
    }

    protected @Nullable SoundEvent getDashSound() {
        return null;
    }

    protected @Nullable SoundEvent getDashReadySound() {
        return null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        this.setPersistenceRequired();
        return super.interact(player, hand, location);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (this.isTame() && player.isSecondaryUseActive()) {
            this.openCustomInventoryScreen(player);
            return InteractionResult.SUCCESS;
        }
        if (!itemStack.isEmpty()) {
            if (!this.level().isClientSide() && !this.isTame() && this.isFood(itemStack)) {
                this.usePlayerItem(player, hand, itemStack);
                this.tryToTame(player);
                return InteractionResult.SUCCESS_SERVER;
            }
            if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                this.feed(player, hand, itemStack, 2.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
            InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
            if (interactionResult.consumesAction()) {
                return interactionResult;
            }
        }
        if (this.isTame() && !player.isSecondaryUseActive() && !this.isFood(itemStack)) {
            this.doPlayerRide(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.navigation.stop();
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }
        this.playEatingSound();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity entity;
        boolean wasHurt = super.hurtServer(level, source, damage);
        if (wasHurt && (entity = source.getEntity()) instanceof LivingEntity) {
            LivingEntity sourceEntity = (LivingEntity)entity;
            NautilusAi.setAngerTarget(level, this, sourceEntity);
        }
        return wasHurt;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance newEffect) {
        if (newEffect.getEffect() == MobEffects.POISON) {
            return false;
        }
        return super.canBeAffected(newEffect);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        RandomSource random = level.getRandom();
        NautilusAi.initMemories(this, random);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        if (slot == EquipmentSlot.SADDLE && this.isUnderWater()) {
            return SoundEvents.NAUTILUS_SADDLE_UNDERWATER_EQUIP;
        }
        if (slot == EquipmentSlot.SADDLE) {
            return SoundEvents.NAUTILUS_SADDLE_EQUIP;
        }
        return super.getEquipSound(slot, stack, equippable);
    }

    public final int getInventorySize() {
        return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
    }

    protected void createInventory() {
        SimpleContainer old = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (old != null) {
            int max = Math.min(old.getContainerSize(), this.inventory.getContainerSize());
            for (int slot = 0; slot < max; ++slot) {
                ItemStack itemStack = old.getItem(slot);
                if (itemStack.isEmpty()) continue;
                this.inventory.setItem(slot, itemStack.copy());
            }
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!this.level().isClientSide() && (!this.isVehicle() || this.hasPassenger(player)) && this.isTame()) {
            player.openNautilusInventory(this, this.inventory);
        }
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        int inventorySlot = slot - 500;
        if (inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize()) {
            return this.inventory.getSlot(inventorySlot);
        }
        return super.getSlot(slot);
    }

    public boolean hasInventoryChanged(Container oldInventory) {
        return this.inventory != oldInventory;
    }

    public int getInventoryColumns() {
        return 0;
    }

    protected boolean isMobControlled() {
        return this.getFirstPassenger() instanceof Mob;
    }

    protected boolean isAggravated() {
        return this.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT) || this.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isTame();
    }
}

