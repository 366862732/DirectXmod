/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.allay.AllayAi;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Allay
extends PathfinderMob
implements InventoryCarrier,
VibrationSystem {
    private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 1, 1);
    private static final int LIFTING_ITEM_ANIMATION_DURATION = 5;
    private static final float DANCING_LOOP_DURATION = 55.0f;
    private static final float SPINNING_ANIMATION_DURATION = 15.0f;
    private static final int DEFAULT_DUPLICATION_COOLDOWN = 0;
    private static final int DUPLICATION_COOLDOWN_TICKS = 6000;
    private static final int NUM_OF_DUPLICATION_HEARTS = 3;
    public static final int MAX_NOTEBLOCK_DISTANCE = 1024;
    private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(Allay.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CAN_DUPLICATE = SynchedEntityData.defineId(Allay.class, EntityDataSerializers.BOOLEAN);
    private static final Brain.Provider<Allay> BRAIN_PROVIDER = Brain.provider(List.of(MemoryModuleType.LIKED_PLAYER, MemoryModuleType.LIKED_NOTEBLOCK_POSITION, MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS), List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.NEAREST_ITEMS), allay -> AllayAi.getActivities());
    public static final ImmutableList<Float> THROW_SOUND_PITCHES = ImmutableList.of((Object)Float.valueOf(0.5625f), (Object)Float.valueOf(0.625f), (Object)Float.valueOf(0.75f), (Object)Float.valueOf(0.9375f), (Object)Float.valueOf(1.0f), (Object)Float.valueOf(1.0f), (Object)Float.valueOf(1.125f), (Object)Float.valueOf(1.25f), (Object)Float.valueOf(1.5f), (Object)Float.valueOf(1.875f), (Object)Float.valueOf(2.0f), (Object)Float.valueOf(2.25f), (Object[])new Float[]{Float.valueOf(2.5f), Float.valueOf(3.0f), Float.valueOf(3.75f), Float.valueOf(4.0f)});
    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicVibrationListener;
    private VibrationSystem.Data vibrationData;
    private final VibrationSystem.User vibrationUser;
    private final DynamicGameEventListener<JukeboxListener> dynamicJukeboxListener;
    private final SimpleContainer inventory = new SimpleContainer(1);
    private @Nullable BlockPos jukeboxPos;
    private long duplicationCooldown = 0L;
    private float holdingItemAnimationTicks;
    private float holdingItemAnimationTicks0;
    private float dancingAnimationTicks;
    private float spinningAnimationTicks;
    private float spinningAnimationTicks0;

    public Allay(EntityType<? extends Allay> type, Level level) {
        super((EntityType<? extends PathfinderMob>)type, level);
        this.moveControl = new FlyingMoveControl<Allay>(this, 20, true);
        this.setCanPickUpLoot(this.canPickUpLoot());
        this.vibrationUser = new VibrationUser(this);
        this.vibrationData = new VibrationSystem.Data();
        this.dynamicVibrationListener = new DynamicGameEventListener<VibrationSystem.Listener>(new VibrationSystem.Listener(this));
        this.dynamicJukeboxListener = new DynamicGameEventListener<JukeboxListener>(new JukeboxListener(this, this.vibrationUser.getPositionSource(), GameEvent.JUKEBOX_PLAY.value().notificationRadius()));
    }

    protected Brain<Allay> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public Brain<Allay> getBrain() {
        return super.getBrain();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.FLYING_SPEED, 0.1f).add(Attributes.MOVEMENT_SPEED, 0.1f).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level);
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(true);
        flyingPathNavigation.setRequiredPathLength(48.0f);
        return flyingPathNavigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_DANCING, false);
        entityData.define(DATA_CAN_DUPLICATE, true);
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isLikedPlayer(source.getEntity())) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected boolean considersEntityAsAlly(Entity other) {
        return this.isLikedPlayer(other) || super.considersEntityAsAlly(other);
    }

    private boolean isLikedPlayer(@Nullable Entity other) {
        if (other instanceof Player) {
            Player player = (Player)other;
            Optional<UUID> likedPlayer = this.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
            return likedPlayer.isPresent() && player.getUUID().equals(likedPlayer.get());
        }
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.hasItemInSlot(EquipmentSlot.MAINHAND) ? SoundEvents.ALLAY_AMBIENT_WITH_ITEM : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("allayBrain");
        this.getBrain().tick(level, this);
        profiler.pop();
        profiler.push("allayActivityUpdate");
        AllayAi.updateActivity(this);
        profiler.pop();
        super.customServerAiStep(level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.isAlive() && this.tickCount % 10 == 0) {
            this.heal(1.0f);
        }
        if (this.isDancing() && this.shouldStopDancing() && this.tickCount % 20 == 0) {
            this.setDancing(false);
            this.jukeboxPos = null;
        }
        this.updateDuplicationCooldown();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.holdingItemAnimationTicks0 = this.holdingItemAnimationTicks;
            this.holdingItemAnimationTicks = this.hasItemInHand() ? Mth.clamp(this.holdingItemAnimationTicks + 1.0f, 0.0f, 5.0f) : Mth.clamp(this.holdingItemAnimationTicks - 1.0f, 0.0f, 5.0f);
            if (this.isDancing()) {
                this.dancingAnimationTicks += 1.0f;
                this.spinningAnimationTicks0 = this.spinningAnimationTicks;
                this.spinningAnimationTicks = this.isSpinning() ? (this.spinningAnimationTicks += 1.0f) : (this.spinningAnimationTicks -= 1.0f);
                this.spinningAnimationTicks = Mth.clamp(this.spinningAnimationTicks, 0.0f, 15.0f);
            } else {
                this.dancingAnimationTicks = 0.0f;
                this.spinningAnimationTicks = 0.0f;
                this.spinningAnimationTicks0 = 0.0f;
            }
        } else {
            VibrationSystem.Ticker.tick(this.level(), this.vibrationData, this.vibrationUser);
            if (this.isPanicking()) {
                this.setDancing(false);
            }
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return !this.isOnPickupCooldown() && this.hasItemInHand();
    }

    public boolean hasItemInHand() {
        return !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return false;
    }

    private boolean isOnPickupCooldown() {
        return this.getBrain().checkMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack interactionItem = player.getItemInHand(hand);
        ItemStack itemInHand = this.getItemInHand(InteractionHand.MAIN_HAND);
        if (this.isDancing() && interactionItem.is(ItemTags.DUPLICATES_ALLAYS) && this.canDuplicate()) {
            this.duplicateAllay();
            this.level().broadcastEntityEvent(this, (byte)18);
            this.level().playSound((Entity)player, this, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0f, 1.0f);
            this.removeInteractionItem(player, interactionItem);
            return InteractionResult.SUCCESS;
        }
        if (itemInHand.isEmpty() && !interactionItem.isEmpty()) {
            ItemStack itemToGive = interactionItem.copyWithCount(1);
            this.setItemInHand(InteractionHand.MAIN_HAND, itemToGive);
            this.removeInteractionItem(player, interactionItem);
            this.level().playSound((Entity)player, this, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 2.0f, 1.0f);
            this.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, player.getUUID());
            return InteractionResult.SUCCESS;
        }
        if (!itemInHand.isEmpty() && hand == InteractionHand.MAIN_HAND && interactionItem.isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.level().playSound((Entity)player, this, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 2.0f, 1.0f);
            this.swing(InteractionHand.MAIN_HAND);
            for (ItemStack itemStack : this.getInventory().removeAllItems()) {
                BehaviorUtils.throwItem(this, itemStack, this.position());
            }
            this.getBrain().eraseMemory(MemoryModuleType.LIKED_PLAYER);
            player.addItem(itemInHand);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public void setJukeboxPlaying(BlockPos jukebox, boolean isPlaying) {
        if (isPlaying) {
            if (!this.isDancing()) {
                this.jukeboxPos = jukebox;
                this.setDancing(true);
            }
        } else if (jukebox.equals(this.jukeboxPos) || this.jukeboxPos == null) {
            this.jukeboxPos = null;
            this.setDancing(false);
        }
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    protected Vec3i getPickupReach() {
        return ITEM_PICKUP_REACH;
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
        ItemStack itemInHand = this.getItemInHand(InteractionHand.MAIN_HAND);
        return !itemInHand.isEmpty() && level.getGameRules().get(GameRules.MOB_GRIEFING) != false && this.inventory.canAddItem(itemStack) && this.allayConsidersItemEqual(itemInHand, itemStack);
    }

    private boolean allayConsidersItemEqual(ItemStack item1, ItemStack item2) {
        return ItemStack.isSameItem(item1, item2) && !this.hasNonMatchingPotion(item1, item2);
    }

    private boolean hasNonMatchingPotion(ItemStack itemInHand, ItemStack pickupItem) {
        PotionContents potionInPickupItem;
        PotionContents potionInHand = itemInHand.get(DataComponents.POTION_CONTENTS);
        return !Objects.equals(potionInHand, potionInPickupItem = pickupItem.get(DataComponents.POTION_CONTENTS));
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        InventoryCarrier.pickUpItem(level, this, this, entity);
    }

    @Override
    public boolean isFlapping() {
        return !this.onGround();
    }

    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> action) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            action.accept(this.dynamicVibrationListener, serverLevel);
            action.accept(this.dynamicJukeboxListener, serverLevel);
        }
    }

    public boolean isDancing() {
        return this.entityData.get(DATA_DANCING);
    }

    public void setDancing(boolean isDancing) {
        if (this.level().isClientSide() || !this.isEffectiveAi() || isDancing && this.isPanicking()) {
            return;
        }
        this.entityData.set(DATA_DANCING, isDancing);
    }

    private boolean shouldStopDancing() {
        return this.jukeboxPos == null || !this.jukeboxPos.closerToCenterThan(this.position(), GameEvent.JUKEBOX_PLAY.value().notificationRadius()) || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX);
    }

    public float getHoldingItemAnimationProgress(float a) {
        return Mth.lerp(a, this.holdingItemAnimationTicks0, this.holdingItemAnimationTicks) / 5.0f;
    }

    public boolean isSpinning() {
        float spinningProgress = this.dancingAnimationTicks % 55.0f;
        return spinningProgress < 15.0f;
    }

    public float getSpinningProgress(float a) {
        return Mth.lerp(a, this.spinningAnimationTicks0, this.spinningAnimationTicks) / 15.0f;
    }

    @Override
    public boolean equipmentHasChanged(ItemStack previous, ItemStack current) {
        return !this.allayConsidersItemEqual(previous, current);
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        this.inventory.removeAllItems().forEach(stack -> this.spawnAtLocation(level, (ItemStack)stack));
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!itemStack.isEmpty() && !EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            this.spawnAtLocation(level, itemStack);
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.writeInventoryToTag(output);
        output.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
        output.putLong("DuplicationCooldown", this.duplicationCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.readInventoryFromTag(input);
        this.vibrationData = input.read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
        this.setDuplicationCooldown(input.getIntOr("DuplicationCooldown", 0));
    }

    @Override
    protected boolean shouldStayCloseToLeashHolder() {
        return false;
    }

    private void updateDuplicationCooldown() {
        if (!this.level().isClientSide() && this.duplicationCooldown > 0L) {
            this.setDuplicationCooldown(this.duplicationCooldown - 1L);
        }
    }

    private void setDuplicationCooldown(long duplicationCooldown) {
        this.duplicationCooldown = duplicationCooldown;
        this.entityData.set(DATA_CAN_DUPLICATE, duplicationCooldown == 0L);
    }

    private void duplicateAllay() {
        Allay allay = EntityTypes.ALLAY.create(this.level(), EntitySpawnReason.BREEDING);
        if (allay != null) {
            allay.snapTo(this.position());
            allay.setPersistenceRequired();
            allay.resetDuplicationCooldown();
            this.resetDuplicationCooldown();
            this.level().addFreshEntity(allay);
        }
    }

    private void resetDuplicationCooldown() {
        this.setDuplicationCooldown(6000L);
    }

    private boolean canDuplicate() {
        return this.entityData.get(DATA_CAN_DUPLICATE);
    }

    private void removeInteractionItem(Player player, ItemStack interactionItem) {
        interactionItem.consume(1, player);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, (double)this.getEyeHeight() * 0.6, (double)this.getBbWidth() * 0.1);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 18) {
            for (int i = 0; i < 3; ++i) {
                this.spawnHeartParticle();
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void spawnHeartParticle() {
        double xd = this.random.nextGaussian() * 0.02;
        double yd = this.random.nextGaussian() * 0.02;
        double zd = this.random.nextGaussian() * 0.02;
        this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xd, yd, zd);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    private class VibrationUser
    implements VibrationSystem.User {
        private static final int VIBRATION_EVENT_LISTENER_RANGE = 16;
        private final PositionSource positionSource;
        final /* synthetic */ Allay this$0;

        private VibrationUser(Allay allay) {
            Allay allay2 = allay;
            Objects.requireNonNull(allay2);
            this.this$0 = allay2;
            this.positionSource = new EntityPositionSource(this.this$0, this.this$0.getEyeHeight());
        }

        @Override
        public int getListenerRadius() {
            return 16;
        }

        @Override
        public PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context) {
            if (this.this$0.isNoAi()) {
                return false;
            }
            Optional<GlobalPos> maybeGlobalPos = this.this$0.getBrain().getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
            if (maybeGlobalPos.isEmpty()) {
                return true;
            }
            GlobalPos globalPos = maybeGlobalPos.get();
            return globalPos.isCloseEnough(level.dimension(), this.this$0.blockPosition(), 1024) && globalPos.pos().equals(pos);
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float receivingDistance) {
            if (event.is(GameEvent.NOTE_BLOCK_PLAY)) {
                AllayAi.hearNoteblock(this.this$0, new BlockPos(pos));
            }
        }

        @Override
        public TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.ALLAY_CAN_LISTEN;
        }
    }

    private class JukeboxListener
    implements GameEventListener {
        private final PositionSource listenerSource;
        private final int listenerRadius;
        final /* synthetic */ Allay this$0;

        public JukeboxListener(Allay allay, PositionSource listenerSource, int listenerRadius) {
            Allay allay2 = allay;
            Objects.requireNonNull(allay2);
            this.this$0 = allay2;
            this.listenerSource = listenerSource;
            this.listenerRadius = listenerRadius;
        }

        @Override
        public PositionSource getListenerSource() {
            return this.listenerSource;
        }

        @Override
        public int getListenerRadius() {
            return this.listenerRadius;
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 sourcePosition) {
            if (event.is(GameEvent.JUKEBOX_PLAY)) {
                this.this$0.setJukeboxPlaying(BlockPos.containing(sourcePosition), true);
                return true;
            }
            if (event.is(GameEvent.JUKEBOX_STOP_PLAY)) {
                this.this$0.setJukeboxPlaying(BlockPos.containing(sourcePosition), false);
                return true;
            }
            return false;
        }
    }
}

