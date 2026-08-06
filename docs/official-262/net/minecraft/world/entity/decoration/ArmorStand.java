/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.decoration;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ArmorStand
extends LivingEntity {
    public static final int WOBBLE_TIME = 5;
    private static final boolean ENABLE_ARMS = true;
    public static final Rotations DEFAULT_HEAD_POSE = new Rotations(0.0f, 0.0f, 0.0f);
    public static final Rotations DEFAULT_BODY_POSE = new Rotations(0.0f, 0.0f, 0.0f);
    public static final Rotations DEFAULT_LEFT_ARM_POSE = new Rotations(-10.0f, 0.0f, -10.0f);
    public static final Rotations DEFAULT_RIGHT_ARM_POSE = new Rotations(-15.0f, 0.0f, 10.0f);
    public static final Rotations DEFAULT_LEFT_LEG_POSE = new Rotations(-1.0f, 0.0f, -1.0f);
    public static final Rotations DEFAULT_RIGHT_LEG_POSE = new Rotations(1.0f, 0.0f, 1.0f);
    private static final EntityDimensions MARKER_DIMENSIONS = EntityDimensions.fixed(0.0f, 0.0f);
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.ARMOR_STAND.getDimensions().scale(0.5f).withEyeHeight(0.9875f);
    private static final double FEET_OFFSET = 0.1;
    private static final double CHEST_OFFSET = 0.9;
    private static final double LEGS_OFFSET = 0.4;
    private static final double HEAD_OFFSET = 1.6;
    public static final int DISABLE_TAKING_OFFSET = 8;
    public static final int DISABLE_PUTTING_OFFSET = 16;
    public static final int CLIENT_FLAG_SMALL = 1;
    public static final int CLIENT_FLAG_SHOW_ARMS = 4;
    public static final int CLIENT_FLAG_NO_BASEPLATE = 8;
    public static final int CLIENT_FLAG_MARKER = 16;
    public static final EntityDataAccessor<Byte> DATA_CLIENT_FLAGS = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Rotations> DATA_HEAD_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_BODY_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_ARM_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_ARM_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_LEG_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_LEG_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    private static final Predicate<Entity> RIDABLE_MINECARTS = entity -> {
        AbstractMinecart minecart;
        return entity instanceof AbstractMinecart && (minecart = (AbstractMinecart)entity).isRideable();
    };
    private static final boolean DEFAULT_INVISIBLE = false;
    private static final int DEFAULT_DISABLED_SLOTS = 0;
    private static final boolean DEFAULT_SMALL = false;
    private static final boolean DEFAULT_SHOW_ARMS = false;
    private static final boolean DEFAULT_NO_BASE_PLATE = false;
    private static final boolean DEFAULT_MARKER = false;
    private boolean invisible = false;
    public long lastHit;
    private int disabledSlots = 0;

    public ArmorStand(EntityType<? extends ArmorStand> type, Level level) {
        super((EntityType<? extends LivingEntity>)type, level);
    }

    public ArmorStand(Level level, double x, double y, double z) {
        this((EntityType<? extends ArmorStand>)EntityTypes.ARMOR_STAND, level);
        this.setPos(x, y, z);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ArmorStand.createLivingAttributes().add(Attributes.STEP_HEIGHT, 0.0);
    }

    @Override
    public void refreshDimensions() {
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();
        super.refreshDimensions();
        this.setPos(oldX, oldY, oldZ);
    }

    private boolean hasPhysics() {
        return !this.isMarker() && !this.isNoGravity();
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && this.hasPhysics();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_CLIENT_FLAGS, (byte)0);
        entityData.define(DATA_HEAD_POSE, DEFAULT_HEAD_POSE);
        entityData.define(DATA_BODY_POSE, DEFAULT_BODY_POSE);
        entityData.define(DATA_LEFT_ARM_POSE, DEFAULT_LEFT_ARM_POSE);
        entityData.define(DATA_RIGHT_ARM_POSE, DEFAULT_RIGHT_ARM_POSE);
        entityData.define(DATA_LEFT_LEG_POSE, DEFAULT_LEFT_LEG_POSE);
        entityData.define(DATA_RIGHT_LEG_POSE, DEFAULT_RIGHT_LEG_POSE);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return slot != EquipmentSlot.BODY && slot != EquipmentSlot.SADDLE && !this.isDisabled(slot);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Invisible", this.isInvisible());
        output.putBoolean("Small", this.isSmall());
        output.putBoolean("ShowArms", this.showArms());
        output.putInt("DisabledSlots", this.disabledSlots);
        output.putBoolean("NoBasePlate", !this.showBasePlate());
        if (this.isMarker()) {
            output.putBoolean("Marker", this.isMarker());
        }
        output.store("Pose", ArmorStandPose.CODEC, this.getArmorStandPose());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setInvisible(input.getBooleanOr("Invisible", false));
        this.setSmall(input.getBooleanOr("Small", false));
        this.setShowArms(input.getBooleanOr("ShowArms", false));
        this.disabledSlots = input.getIntOr("DisabledSlots", 0);
        this.setNoBasePlate(input.getBooleanOr("NoBasePlate", false));
        this.setMarker(input.getBooleanOr("Marker", false));
        this.noPhysics = !this.hasPhysics();
        input.read("Pose", ArmorStandPose.CODEC).ifPresent(this::setArmorStandPose);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox(), RIDABLE_MINECARTS);
        for (Entity entity : entities) {
            if (!(this.distanceToSqr(entity) <= 0.2)) continue;
            entity.push(this);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isMarker() || itemStack.is(Items.NAME_TAG)) {
            return super.interact(player, hand, location);
        }
        if (player.isSpectator()) {
            return InteractionResult.SUCCESS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        EquipmentSlot itemInHandSlot = this.getEquipmentSlotForItem(itemStack);
        if (itemStack.isEmpty()) {
            EquipmentSlot targetSlot;
            EquipmentSlot clickedSlot = this.getClickedSlot(location);
            EquipmentSlot equipmentSlot = targetSlot = this.isDisabled(clickedSlot) ? itemInHandSlot : clickedSlot;
            if (this.hasItemInSlot(targetSlot) && this.swapItem(player, targetSlot, itemStack, hand)) {
                return InteractionResult.SUCCESS_SERVER;
            }
        } else {
            if (this.isDisabled(itemInHandSlot)) {
                return InteractionResult.FAIL;
            }
            if (itemInHandSlot.getType() == EquipmentSlot.Type.HAND && !this.showArms()) {
                return InteractionResult.FAIL;
            }
            if (this.swapItem(player, itemInHandSlot, itemStack, hand)) {
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return super.interact(player, hand, location);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private EquipmentSlot getClickedSlot(Vec3 location) {
        EquipmentSlot slotClicked = EquipmentSlot.MAINHAND;
        boolean small = this.isSmall();
        double clickYPosition = location.y / (double)(this.getScale() * this.getAgeScale());
        EquipmentSlot feet = EquipmentSlot.FEET;
        if (clickYPosition >= 0.1) {
            double d = small ? 0.8 : 0.45;
            if (clickYPosition < 0.1 + d && this.hasItemInSlot(feet)) {
                return EquipmentSlot.FEET;
            }
        }
        double d = small ? 0.3 : 0.0;
        if (clickYPosition >= 0.9 + d) {
            double d2 = small ? 1.0 : 0.7;
            if (clickYPosition < 0.9 + d2 && this.hasItemInSlot(EquipmentSlot.CHEST)) {
                return EquipmentSlot.CHEST;
            }
        }
        if (clickYPosition >= 0.4) {
            double d3 = small ? 1.0 : 0.8;
            if (clickYPosition < 0.4 + d3 && this.hasItemInSlot(EquipmentSlot.LEGS)) {
                return EquipmentSlot.LEGS;
            }
        }
        if (clickYPosition >= 1.6 && this.hasItemInSlot(EquipmentSlot.HEAD)) {
            return EquipmentSlot.HEAD;
        }
        if (this.hasItemInSlot(EquipmentSlot.MAINHAND)) return slotClicked;
        if (!this.hasItemInSlot(EquipmentSlot.OFFHAND)) return slotClicked;
        return EquipmentSlot.OFFHAND;
    }

    private boolean isDisabled(EquipmentSlot slot) {
        return (this.disabledSlots & 1 << slot.getFilterBit(0)) != 0 || slot.getType() == EquipmentSlot.Type.HAND && !this.showArms();
    }

    private boolean swapItem(Player player, EquipmentSlot slot, ItemStack playerItemStack, InteractionHand hand) {
        ItemStack itemStack = this.getItemBySlot(slot);
        if (!itemStack.isEmpty() && (this.disabledSlots & 1 << slot.getFilterBit(8)) != 0) {
            return false;
        }
        if (itemStack.isEmpty() && (this.disabledSlots & 1 << slot.getFilterBit(16)) != 0) {
            return false;
        }
        if (player.hasInfiniteMaterials() && itemStack.isEmpty() && !playerItemStack.isEmpty()) {
            this.setItemSlot(slot, playerItemStack.copyWithCount(1));
            return true;
        }
        if (!playerItemStack.isEmpty() && playerItemStack.getCount() > 1) {
            if (!itemStack.isEmpty()) {
                return false;
            }
            this.setItemSlot(slot, playerItemStack.split(1));
            return true;
        }
        this.setItemSlot(slot, playerItemStack);
        player.setItemInHand(hand, itemStack);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isRemoved()) {
            return false;
        }
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue() && source.getEntity() instanceof Mob) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.kill(level);
            return false;
        }
        if (this.isInvulnerableTo(level, source) || this.invisible || this.isMarker()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            this.brokenByAnything(level, source);
            this.kill(level);
            return false;
        }
        if (source.is(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
            if (this.isOnFire()) {
                this.causeDamage(level, source, 0.15f);
            } else {
                this.igniteForSeconds(5.0f);
            }
            return false;
        }
        if (source.is(DamageTypeTags.BURNS_ARMOR_STANDS) && this.getHealth() > 0.5f) {
            this.causeDamage(level, source, 4.0f);
            return false;
        }
        boolean allowIncrementalBreaking = source.is(DamageTypeTags.CAN_BREAK_ARMOR_STAND);
        boolean shouldKill = source.is(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS);
        if (!allowIncrementalBreaking && !shouldKill) {
            return false;
        }
        Entity entity = source.getEntity();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (!player.getAbilities().mayBuild) {
                return false;
            }
        }
        if (source.isCreativePlayer()) {
            this.playBrokenSound();
            this.showBreakingParticles();
            this.kill(level);
            return true;
        }
        long time = level.getGameTime();
        if (time - this.lastHit <= 5L || shouldKill) {
            this.brokenByPlayer(level, source);
            this.showBreakingParticles();
            this.kill(level);
        } else {
            level.broadcastEntityEvent(this, (byte)32);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
            this.lastHit = time;
        }
        return true;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 32) {
            if (this.level().isClientSide()) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_HIT, this.getSoundSource(), 0.3f, 1.0f, false);
                this.lastHit = this.level().getGameTime();
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize() * 4.0;
        if (Double.isNaN(size) || size == 0.0) {
            size = 4.0;
        }
        return distance < (size *= 64.0) * size;
    }

    private void showBreakingParticles() {
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), this.getX(), this.getY(0.6666666666666666), this.getZ(), 10, this.getBbWidth() / 4.0f, this.getBbHeight() / 4.0f, this.getBbWidth() / 4.0f, 0.05);
        }
    }

    private void causeDamage(ServerLevel level, DamageSource source, float dmg) {
        float health = this.getHealth();
        if ((health -= dmg) <= 0.5f) {
            this.brokenByAnything(level, source);
            this.kill(level);
        } else {
            this.setHealth(health);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        }
    }

    private void brokenByPlayer(ServerLevel level, DamageSource source) {
        ItemStack result = new ItemStack(Items.ARMOR_STAND);
        result.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        Block.popResource(this.level(), this.blockPosition(), result);
        this.brokenByAnything(level, source);
    }

    private void brokenByAnything(ServerLevel level, DamageSource source) {
        this.playBrokenSound();
        this.dropAllDeathLoot(level, source);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = this.equipment.set(slot, ItemStack.EMPTY);
            if (itemStack.isEmpty() || EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) continue;
            Block.popResource(this.level(), this.blockPosition().above(), itemStack);
        }
    }

    private void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_BREAK, this.getSoundSource(), 1.0f, 1.0f);
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        this.yBodyRotO = this.yRotO;
        this.yBodyRot = this.getYRot();
    }

    @Override
    public void travel(Vec3 input) {
        if (!this.hasPhysics()) {
            return;
        }
        super.travel(input);
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
        this.yBodyRotO = this.yRotO = yBodyRot;
        this.yHeadRotO = this.yHeadRot = yBodyRot;
    }

    @Override
    public void setYHeadRot(float yHeadRot) {
        this.yBodyRotO = this.yRotO = yHeadRot;
        this.yHeadRotO = this.yHeadRot = yHeadRot;
    }

    @Override
    protected void updateInvisibilityStatus() {
        this.setInvisible(this.invisible);
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        super.setInvisible(invisible);
    }

    @Override
    public boolean isBaby() {
        return this.isSmall();
    }

    @Override
    public void kill(ServerLevel level) {
        this.remove(Entity.RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE);
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        if (explosion.shouldAffectBlocklikeEntities()) {
            return this.isInvisible();
        }
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        if (this.isMarker()) {
            return PushReaction.IGNORE;
        }
        return super.getPistonPushReaction();
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return this.isMarker();
    }

    private void setSmall(boolean value) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 1, value));
    }

    public boolean isSmall() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 1) != 0;
    }

    public void setShowArms(boolean value) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 4, value));
    }

    public boolean showArms() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 4) != 0;
    }

    public void setNoBasePlate(boolean value) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 8, value));
    }

    public boolean showBasePlate() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 8) == 0;
    }

    private void setMarker(boolean value) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 16, value));
    }

    public boolean isMarker() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 0x10) != 0;
    }

    private byte setBit(byte data, int bit, boolean value) {
        data = value ? (byte)(data | bit) : (byte)(data & ~bit);
        return data;
    }

    public void setHeadPose(Rotations headPose) {
        this.entityData.set(DATA_HEAD_POSE, headPose);
    }

    public void setBodyPose(Rotations bodyPose) {
        this.entityData.set(DATA_BODY_POSE, bodyPose);
    }

    public void setLeftArmPose(Rotations leftArmPose) {
        this.entityData.set(DATA_LEFT_ARM_POSE, leftArmPose);
    }

    public void setRightArmPose(Rotations rightArmPose) {
        this.entityData.set(DATA_RIGHT_ARM_POSE, rightArmPose);
    }

    public void setLeftLegPose(Rotations leftLegPose) {
        this.entityData.set(DATA_LEFT_LEG_POSE, leftLegPose);
    }

    public void setRightLegPose(Rotations rightLegPose) {
        this.entityData.set(DATA_RIGHT_LEG_POSE, rightLegPose);
    }

    public Rotations getHeadPose() {
        return this.entityData.get(DATA_HEAD_POSE);
    }

    public Rotations getBodyPose() {
        return this.entityData.get(DATA_BODY_POSE);
    }

    public Rotations getLeftArmPose() {
        return this.entityData.get(DATA_LEFT_ARM_POSE);
    }

    public Rotations getRightArmPose() {
        return this.entityData.get(DATA_RIGHT_ARM_POSE);
    }

    public Rotations getLeftLegPose() {
        return this.entityData.get(DATA_LEFT_LEG_POSE);
    }

    public Rotations getRightLegPose() {
        return this.entityData.get(DATA_RIGHT_LEG_POSE);
    }

    @Override
    public boolean isPickable() {
        return super.isPickable() && !this.isMarker();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean skipAttackInteraction(Entity source) {
        if (!(source instanceof Player)) return false;
        Player playerSource = (Player)source;
        if (this.level().mayInteract(playerSource, this.blockPosition())) return false;
        return true;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.ARMOR_STAND_FALL, SoundEvents.ARMOR_STAND_FALL);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMOR_STAND_HIT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_CLIENT_FLAGS.equals(accessor)) {
            this.refreshDimensions();
            this.blocksBuilding = !this.isMarker();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public boolean attackable() {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getDimensionsMarker(this.isMarker());
    }

    private EntityDimensions getDimensionsMarker(boolean isMarker) {
        if (isMarker) {
            return MARKER_DIMENSIONS;
        }
        return this.isBaby() ? BABY_DIMENSIONS : this.getType().getDimensions();
    }

    @Override
    public Vec3 getLightProbePosition(float partialTickTime) {
        if (this.isMarker()) {
            AABB box = this.getDimensionsMarker(false).makeBoundingBox(this.position());
            BlockPos probePos = this.blockPosition();
            int brightestLight = Integer.MIN_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                int blockBrightness = Math.max(this.level().getBrightness(LightLayer.BLOCK, pos), this.level().getBrightness(LightLayer.SKY, pos));
                if (blockBrightness == 15) {
                    return Vec3.atCenterOf(pos);
                }
                if (blockBrightness <= brightestLight) continue;
                brightestLight = blockBrightness;
                probePos = pos.immutable();
            }
            return Vec3.atCenterOf(probePos);
        }
        return super.getLightProbePosition(partialTickTime);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.ARMOR_STAND);
    }

    @Override
    public boolean canBeSeenByAnyone() {
        return !this.isInvisible() && !this.isMarker();
    }

    public void setArmorStandPose(ArmorStandPose pose) {
        this.setHeadPose(pose.head());
        this.setBodyPose(pose.body());
        this.setLeftArmPose(pose.leftArm());
        this.setRightArmPose(pose.rightArm());
        this.setLeftLegPose(pose.leftLeg());
        this.setRightLegPose(pose.rightLeg());
    }

    public ArmorStandPose getArmorStandPose() {
        return new ArmorStandPose(this.getHeadPose(), this.getBodyPose(), this.getLeftArmPose(), this.getRightArmPose(), this.getLeftLegPose(), this.getRightLegPose());
    }

    public record ArmorStandPose(Rotations head, Rotations body, Rotations leftArm, Rotations rightArm, Rotations leftLeg, Rotations rightLeg) {
        public static final ArmorStandPose DEFAULT = new ArmorStandPose(DEFAULT_HEAD_POSE, DEFAULT_BODY_POSE, DEFAULT_LEFT_ARM_POSE, DEFAULT_RIGHT_ARM_POSE, DEFAULT_LEFT_LEG_POSE, DEFAULT_RIGHT_LEG_POSE);
        public static final Codec<ArmorStandPose> CODEC = RecordCodecBuilder.create(i -> i.group((App)Rotations.CODEC.optionalFieldOf("Head", (Object)DEFAULT_HEAD_POSE).forGetter(ArmorStandPose::head), (App)Rotations.CODEC.optionalFieldOf("Body", (Object)DEFAULT_BODY_POSE).forGetter(ArmorStandPose::body), (App)Rotations.CODEC.optionalFieldOf("LeftArm", (Object)DEFAULT_LEFT_ARM_POSE).forGetter(ArmorStandPose::leftArm), (App)Rotations.CODEC.optionalFieldOf("RightArm", (Object)DEFAULT_RIGHT_ARM_POSE).forGetter(ArmorStandPose::rightArm), (App)Rotations.CODEC.optionalFieldOf("LeftLeg", (Object)DEFAULT_LEFT_LEG_POSE).forGetter(ArmorStandPose::leftLeg), (App)Rotations.CODEC.optionalFieldOf("RightLeg", (Object)DEFAULT_RIGHT_LEG_POSE).forGetter(ArmorStandPose::rightLeg)).apply((Applicative)i, ArmorStandPose::new));
    }
}

