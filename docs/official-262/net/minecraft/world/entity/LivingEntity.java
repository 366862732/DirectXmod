/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.base.Objects
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair
 *  it.unimi.dsi.fastutil.objects.Object2LongMap
 *  it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMap
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.ElytraAnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class LivingEntity
extends Entity
implements Attackable,
WaypointTransmitter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_ACTIVE_EFFECTS = "active_effects";
    public static final String TAG_ATTRIBUTES = "attributes";
    public static final String TAG_SLEEPING_POS = "sleeping_pos";
    public static final String TAG_EQUIPMENT = "equipment";
    public static final String TAG_BRAIN = "Brain";
    public static final String TAG_FALL_FLYING = "FallFlying";
    public static final String TAG_HURT_TIME = "HurtTime";
    public static final String TAG_DEATH_TIME = "DeathTime";
    public static final String TAG_HEALTH = "Health";
    private static final Identifier SPEED_MODIFIER_POWDER_SNOW_ID = Identifier.withDefaultNamespace("powder_snow");
    private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(SPRINTING_MODIFIER_ID, 0.3f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int BODY_ARMOR_OFFSET = 105;
    public static final int SADDLE_OFFSET = 106;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003;
    public static final double DEFAULT_BASE_GRAVITY = 0.08;
    public static final int DEATH_DURATION = 20;
    protected static final float INPUT_FRICTION = 0.98f;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final float BASE_JUMP_POWER = 0.42f;
    protected static final float DEFAULT_KNOCKBACK = 0.4f;
    protected static final int INVULNERABLE_DURATION = 20;
    protected static final int HURT_DURATION_TICKS = 10;
    private static final double CLIMBING_VERTICAL_SPEED = 0.2;
    private static final float SWIM_AMOUNT_PER_TICK = 0.09f;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.PARTICLES);
    private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
    protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2f, 0.2f).withEyeHeight(0.2f);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5f;
    public static final float DEFAULT_BABY_SCALE = 0.5f;
    protected static final float SWIMMING_VERTICAL_SPEED = 0.04f;
    private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
    private static final int DEFAULT_CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME = 0;
    public static final float BASE_HORIZONTAL_AIR_DRAG = 0.91f;
    public static final float BASE_VERTICAL_AIR_DRAG = 0.98f;
    public static final float WATER_DRAG = 0.8f;
    public static final float SPRINTING_WATER_DRAG = 0.9f;
    public static final float LAVA_DRAG = 0.5f;
    public static final float LAVA_SHALLOW_VERTICAL_DRAG = 0.8f;
    public static final float DOLPHINS_GRACE_WATER_DRAG = 0.96f;
    public static final float FLYING_AIR_DRAG = 0.91f;
    public static final float FLYING_VERTICAL_AIR_DRAG = 0.91f;
    public static final float FLYING_LAVA_DRAG = 0.5f;
    public static final float FLYING_WATER_DRAG = 0.8f;
    public static final float ELYTRA_HORIZONTAL_AIR_DRAG = 0.99f;
    public static final float ELYTRA_VERTICAL_AIR_DRAG = 0.98f;
    public static final float BASE_SWIM_SPEED = 0.02f;
    private int currentImpulseContextResetGraceTime = 0;
    public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = livingEntity -> {
        if (!(livingEntity instanceof Player)) {
            return true;
        }
        Player player = (Player)livingEntity;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return !helmet.is(ItemTags.GAZE_DISGUISE_EQUIPMENT);
    };
    private final AttributeMap attributes;
    private final CombatTracker combatTracker = new CombatTracker(this);
    private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
    private final Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, slot -> ItemStack.EMPTY);
    public boolean swinging;
    private boolean discardFriction = false;
    public @Nullable InteractionHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    protected int itemSwapTicker;
    public final WalkAnimationState walkAnimation = new WalkAnimationState();
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
    protected @Nullable EntityReference<Player> lastHurtByPlayer;
    protected int lastHurtByPlayerMemoryTime;
    protected boolean dead;
    protected int noActionTime;
    protected float lastHurt;
    protected boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected final InterpolationHandler interpolation = new InterpolationHandler(this);
    protected double lerpYHeadRot;
    protected int lerpHeadSteps;
    private boolean effectsDirty = true;
    private @Nullable EntityReference<LivingEntity> lastHurtByMob;
    private int lastHurtByMobTimestamp;
    private @Nullable LivingEntity lastHurtMob;
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem = ItemStack.EMPTY;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private long lastKineticHitFeedbackTime = Integer.MIN_VALUE;
    private BlockPos lastPos;
    private Optional<BlockPos> lastClimbablePos = Optional.empty();
    private @Nullable DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    protected float autoSpinAttackDmg;
    protected @Nullable ItemStack autoSpinAttackItemStack;
    protected @Nullable Object2LongMap<Entity> recentKineticEnemies;
    private float swimAmount;
    private float swimAmountO;
    protected Brain<?> brain;
    private boolean skipDropExperience;
    private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap(EquipmentSlot.class);
    protected final EntityEquipment equipment;
    private Waypoint.Icon locatorBarIcon = new Waypoint.Icon();
    public @Nullable Vec3 currentImpulseImpactPos;

    protected LivingEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.attributes = new AttributeMap(DefaultAttributes.getSupplier(type));
        this.setHealth(this.getMaxHealth());
        this.equipment = this.createEquipment();
        this.blocksBuilding = true;
        this.reapplyPosition();
        this.setYRot(this.random.nextFloat() * ((float)Math.PI * 2));
        this.yHeadRot = this.getYRot();
        this.brain = this.makeBrain(Brain.Packed.EMPTY);
    }

    @Override
    public @Nullable LivingEntity asLivingEntity() {
        return this;
    }

    @Contract(pure=true)
    protected EntityEquipment createEquipment() {
        return new EntityEquipment();
    }

    public Brain<? extends LivingEntity> getBrain() {
        return this.brain;
    }

    protected Brain<? extends LivingEntity> makeBrain(Brain.Packed packedBrain) {
        return new Brain();
    }

    @Override
    public void kill(ServerLevel level) {
        this.hurtServer(level, this.damageSources().genericKill(), Float.MAX_VALUE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
        entityData.define(DATA_EFFECT_PARTICLES, List.of());
        entityData.define(DATA_EFFECT_AMBIENCE_ID, false);
        entityData.define(DATA_ARROW_COUNT_ID, 0);
        entityData.define(DATA_STINGER_COUNT_ID, 0);
        entityData.define(DATA_HEALTH_ID, Float.valueOf(1.0f));
        entityData.define(SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeSupplier.Builder createLivingAttributes() {
        return AttributeSupplier.builder().add(Attributes.MAX_HEALTH).add(Attributes.KNOCKBACK_RESISTANCE).add(Attributes.MOVEMENT_SPEED).add(Attributes.ARMOR).add(Attributes.ARMOR_TOUGHNESS).add(Attributes.MAX_ABSORPTION).add(Attributes.STEP_HEIGHT).add(Attributes.SCALE).add(Attributes.GRAVITY).add(Attributes.SAFE_FALL_DISTANCE).add(Attributes.FALL_DAMAGE_MULTIPLIER).add(Attributes.JUMP_STRENGTH).add(Attributes.ENTITY_INTERACTION_RANGE).add(Attributes.OXYGEN_BONUS).add(Attributes.BURNING_TIME).add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).add(Attributes.WATER_MOVEMENT_EFFICIENCY).add(Attributes.MOVEMENT_EFFICIENCY).add(Attributes.ATTACK_KNOCKBACK).add(Attributes.CAMERA_DISTANCE).add(Attributes.WAYPOINT_TRANSMIT_RANGE).add(Attributes.BOUNCINESS).add(Attributes.AIR_DRAG_MODIFIER).add(Attributes.FRICTION_MODIFIER).add(Attributes.NAME_TAG_DISTANCE).add(Attributes.BELOW_NAME_DISTANCE);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        Level level;
        if (!this.isInWater()) {
            this.updateFluidInteraction();
        }
        if ((level = this.level()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            if (onGround && this.fallDistance > 0.0) {
                this.onChangedBlock(level2, pos);
                double power = Math.max(0, Mth.floor(this.calculateFallPower(this.fallDistance)));
                if (power > 0.0 && !onState.isAir()) {
                    double x = this.getX();
                    double y = this.getY();
                    double z = this.getZ();
                    BlockPos entityPos = this.blockPosition();
                    if (pos.getX() != entityPos.getX() || pos.getZ() != entityPos.getZ()) {
                        double xDiff = x - (double)pos.getX() - 0.5;
                        double zDiff = z - (double)pos.getZ() - 0.5;
                        double maxDiff = Math.max(Math.abs(xDiff), Math.abs(zDiff));
                        x = (double)pos.getX() + 0.5 + xDiff / maxDiff * 0.5;
                        z = (double)pos.getZ() + 0.5 + zDiff / maxDiff * 0.5;
                    }
                    double scale = Math.min((double)0.2f + power / 15.0, 2.5);
                    int particles = (int)(150.0 * scale);
                    level2.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, onState), x, y, z, particles, 0.0, 0.0, 0.0, 0.15f);
                }
            }
        }
        super.checkFallDamage(ya, onGround, onState, pos);
        if (onGround) {
            this.lastClimbablePos = Optional.empty();
        }
    }

    public boolean canBreatheUnderwater() {
        return this.is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public float getSwimAmount(float a) {
        return Mth.lerp(a, this.swimAmountO, this.swimAmount);
    }

    public boolean hasLandedInLiquid() {
        return this.getDeltaMovement().y() < (double)1.0E-5f && this.isInLiquid();
    }

    @Override
    public void baseTick() {
        LivingEntity hurtByMob;
        Level level;
        Level level2;
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }
        if ((level2 = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level2;
            EnchantmentHelper.tickEffects(serverLevel, this);
        }
        super.baseTick();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("livingEntityBaseTick");
        if (this.isAlive() && (level = this.level()) instanceof ServerLevel) {
            double damagePerBlock;
            double dist;
            ServerLevel level3 = (ServerLevel)level;
            boolean isPlayer = this instanceof Player;
            if (this.isInWall()) {
                this.hurtServer(level3, this.damageSources().inWall(), 1.0f);
            } else if (isPlayer && !level3.getWorldBorder().isWithinBounds(this.getBoundingBox()) && (dist = level3.getWorldBorder().getDistanceToBorder(this) + level3.getWorldBorder().getSafeZone()) < 0.0 && (damagePerBlock = level3.getWorldBorder().getDamagePerBlock()) > 0.0) {
                this.hurtServer(level3, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-dist * damagePerBlock)));
            }
            if (this.isEyeInFluid(FluidTags.WATER) && !level3.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean canDrownInWater;
                boolean bl = canDrownInWater = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!isPlayer || !((Player)this).getAbilities().invulnerable);
                if (canDrownInWater) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.shouldTakeDrowningDamage()) {
                        this.setAirSupply(0);
                        level3.broadcastEntityEvent(this, (byte)67);
                        this.hurtServer(level3, this.damageSources().drown(), 2.0f);
                    }
                } else if (this.getAirSupply() < this.getMaxAirSupply() && MobEffectUtil.shouldEffectsRefillAirsupply(this)) {
                    this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
                }
                if (this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }
            BlockPos pos = this.blockPosition();
            if (!Objects.equal((Object)this.lastPos, (Object)pos)) {
                this.lastPos = pos;
                this.onChangedBlock(level3, pos);
            }
        }
        if (this.hurtTime > 0) {
            --this.hurtTime;
        }
        if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
            --this.invulnerableTime;
        }
        if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
            this.tickDeath();
        }
        if (this.lastHurtByPlayerMemoryTime > 0) {
            --this.lastHurtByPlayerMemoryTime;
        } else {
            this.lastHurtByPlayer = null;
        }
        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }
        if ((hurtByMob = this.getLastHurtByMob()) != null) {
            if (!hurtByMob.isAlive()) {
                this.setLastHurtByMob(null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob(null);
            }
        }
        this.tickEffects();
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        profiler.pop();
    }

    protected boolean shouldTakeDrowningDamage() {
        return this.getAirSupply() <= -20;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0f);
    }

    private static float computeModifiedFriction(float friction, float modifier) {
        return Mth.clamp(1.0f - (1.0f - friction) * modifier, 0.0f, 1.0f);
    }

    public float getLuck() {
        return 0.0f;
    }

    protected void removeFrost() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (speed.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
            speed.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
        }
    }

    protected void tryAddFrost() {
        int ticksFrozen;
        if (!this.getBlockStateOnLegacy().isAir() && (ticksFrozen = this.getTicksFrozen()) > 0) {
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) {
                return;
            }
            float slowAmount = -0.05f * this.getPercentFrozen();
            speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, slowAmount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    protected void onChangedBlock(ServerLevel level, BlockPos pos) {
        EnchantmentHelper.runLocationChangedEffects(level, this);
    }

    public boolean isBaby() {
        return false;
    }

    public float getAgeScale() {
        return this.isBaby() ? 0.5f : 1.0f;
    }

    public final float getScale() {
        AttributeMap attributes = this.getAttributes();
        if (attributes == null) {
            return 1.0f;
        }
        return this.sanitizeScale((float)attributes.getValue(Attributes.SCALE));
    }

    protected float sanitizeScale(float scale) {
        return scale;
    }

    public boolean isAffectedByFluids() {
        return true;
    }

    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot(ServerLevel level) {
        return !this.isBaby() && level.getGameRules().get(GameRules.MOB_DROPS) != false;
    }

    protected int decreaseAirSupply(int currentSupply) {
        AttributeInstance respiration = this.getAttribute(Attributes.OXYGEN_BONUS);
        double oxygenBonus = respiration != null ? respiration.getValue() : 0.0;
        if (oxygenBonus > 0.0 && this.random.nextDouble() >= 1.0 / (oxygenBonus + 1.0)) {
            return currentSupply;
        }
        return currentSupply - 1;
    }

    protected int increaseAirSupply(int currentSupply) {
        return Math.min(currentSupply + 4, this.getMaxAirSupply());
    }

    public final int getExperienceReward(ServerLevel level, @Nullable Entity killer) {
        return EnchantmentHelper.processMobExperience(level, killer, this, this.getBaseExperienceReward(level));
    }

    protected int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }

    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    public @Nullable LivingEntity getLastHurtByMob() {
        return EntityReference.getLivingEntity(this.lastHurtByMob, this.level());
    }

    public @Nullable Player getLastHurtByPlayer() {
        return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
    }

    @Override
    public LivingEntity getLastAttacker() {
        return this.getLastHurtByMob();
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(Player player, int timeToRemember) {
        this.setLastHurtByPlayer(EntityReference.of(player), timeToRemember);
    }

    public void setLastHurtByPlayer(UUID player, int timeToRemember) {
        this.setLastHurtByPlayer(EntityReference.of(player), timeToRemember);
    }

    private void setLastHurtByPlayer(EntityReference<Player> player, int timeToRemember) {
        this.lastHurtByPlayer = player;
        this.lastHurtByPlayerMemoryTime = timeToRemember;
    }

    public void setLastHurtByMob(@Nullable LivingEntity hurtBy) {
        this.lastHurtByMob = EntityReference.of(hurtBy);
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    public @Nullable LivingEntity getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity target) {
        LivingEntity livingEntity;
        this.lastHurtMob = target instanceof LivingEntity ? (livingEntity = (LivingEntity)target) : null;
        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int noActionTime) {
        this.noActionTime = noActionTime;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean discardFriction) {
        this.discardFriction = discardFriction;
    }

    protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
        return true;
    }

    public void onEquipItem(EquipmentSlot slot, ItemStack oldStack, ItemStack stack) {
        if (this.level().isClientSide() || this.isSpectator()) {
            return;
        }
        if (ItemStack.isSameItemSameComponents(oldStack, stack) || this.firstTick) {
            return;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (!this.isSilent() && equippable != null && slot == equippable.slot()) {
            this.level().playSeededSound(null, this.getX(), this.getY(), this.getZ(), this.getEquipSound(slot, stack, equippable), this.getSoundSource(), 1.0f, 1.0f, this.random.nextLong());
        }
        if (this.doesEmitEquipEvent(slot)) {
            this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
        }
    }

    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        return equippable.equipSound();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        Level level;
        if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) && (level = this.level()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            this.triggerOnDeathMobEffects(level2, reason);
        }
        super.remove(reason);
        this.brain.clearMemories();
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        super.onRemoval(reason);
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.getWaypointManager().untrackWaypoint(this);
        }
    }

    protected void triggerOnDeathMobEffects(ServerLevel level, Entity.RemovalReason reason) {
        for (MobEffectInstance effect : this.getActiveEffects()) {
            effect.onMobRemoved(level, this, reason);
        }
        this.activeEffects.clear();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat(TAG_HEALTH, this.getHealth());
        output.putShort(TAG_HURT_TIME, (short)this.hurtTime);
        output.putShort(TAG_DEATH_TIME, (short)this.deathTime);
        output.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
        output.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
        output.storeNullable("current_explosion_impact_pos", Vec3.CODEC, this.currentImpulseImpactPos);
        output.store(TAG_ATTRIBUTES, AttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
        if (!this.activeEffects.isEmpty()) {
            output.store(TAG_ACTIVE_EFFECTS, MobEffectInstance.CODEC.listOf(), List.copyOf(this.activeEffects.values()));
        }
        output.putBoolean(TAG_FALL_FLYING, this.isFallFlying());
        this.getSleepingPos().ifPresent(sleepingPos -> output.store(TAG_SLEEPING_POS, BlockPos.CODEC, sleepingPos));
        output.store(TAG_BRAIN, Brain.Packed.CODEC, this.brain.pack());
        if (this.lastHurtByPlayer != null) {
            this.lastHurtByPlayer.store(output, "last_hurt_by_player");
            output.putInt("last_hurt_by_player_memory_time", this.lastHurtByPlayerMemoryTime);
        }
        if (this.lastHurtByMob != null) {
            this.lastHurtByMob.store(output, "last_hurt_by_mob");
            output.putInt("ticks_since_last_hurt_by_mob", this.tickCount - this.lastHurtByMobTimestamp);
        }
        if (!this.equipment.isEmpty()) {
            output.store(TAG_EQUIPMENT, EntityEquipment.CODEC, this.equipment);
        }
        if (this.locatorBarIcon.hasData()) {
            output.store("locator_bar_icon", Waypoint.Icon.CODEC, this.locatorBarIcon);
        }
    }

    public @Nullable ItemEntity drop(ItemStack itemStack, boolean randomly, boolean thrownFromHand) {
        if (itemStack.isEmpty()) {
            return null;
        }
        if (this.level().isClientSide()) {
            this.swing(InteractionHand.MAIN_HAND);
            return null;
        }
        ItemEntity entity = this.createItemStackToDrop(itemStack, randomly, thrownFromHand);
        if (entity != null) {
            this.level().addFreshEntity(entity);
        }
        return entity;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.internalSetAbsorptionAmount(input.getFloatOr("AbsorptionAmount", 0.0f));
        if (this.level() != null && !this.level().isClientSide()) {
            input.read(TAG_ATTRIBUTES, AttributeInstance.Packed.LIST_CODEC).ifPresent(this.getAttributes()::apply);
        }
        List effects = input.read(TAG_ACTIVE_EFFECTS, MobEffectInstance.CODEC.listOf()).orElse(List.of());
        this.activeEffects.clear();
        for (MobEffectInstance effect : effects) {
            this.activeEffects.put(effect.getEffect(), effect);
            this.effectsDirty = true;
        }
        this.setHealth(input.getFloatOr(TAG_HEALTH, this.getMaxHealth()));
        this.hurtTime = input.getShortOr(TAG_HURT_TIME, (short)0);
        this.deathTime = input.getShortOr(TAG_DEATH_TIME, (short)0);
        input.getString("Team").ifPresent(teamName -> {
            boolean success;
            Scoreboard scoreboard = this.level().getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam((String)teamName);
            boolean bl = success = team != null && scoreboard.addPlayerToTeam(this.getStringUUID(), team);
            if (!success) {
                LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", teamName);
            }
        });
        this.setSharedFlag(7, input.getBooleanOr(TAG_FALL_FLYING, false));
        input.read(TAG_SLEEPING_POS, BlockPos.CODEC).ifPresentOrElse(sleepingPos -> {
            this.setSleepingPos((BlockPos)sleepingPos);
            this.entityData.set(DATA_POSE, Pose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed((BlockPos)sleepingPos);
            }
        }, this::clearSleepingPos);
        input.read(TAG_BRAIN, Brain.Packed.CODEC).ifPresent(packedBrain -> {
            this.brain = this.makeBrain((Brain.Packed)packedBrain);
        });
        this.lastHurtByPlayer = EntityReference.read(input, "last_hurt_by_player");
        this.lastHurtByPlayerMemoryTime = input.getIntOr("last_hurt_by_player_memory_time", 0);
        this.lastHurtByMob = EntityReference.read(input, "last_hurt_by_mob");
        this.lastHurtByMobTimestamp = this.tickCount - input.getIntOr("ticks_since_last_hurt_by_mob", 0);
        this.equipment.setAll(input.read(TAG_EQUIPMENT, EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
        this.locatorBarIcon = input.read("locator_bar_icon", Waypoint.Icon.CODEC).orElseGet(Waypoint.Icon::new);
        this.currentImpulseContextResetGraceTime = input.getIntOr("current_impulse_context_reset_grace_time", 0);
        this.currentImpulseImpactPos = input.read("current_explosion_impact_pos", Vec3.CODEC).orElse(null);
    }

    @Override
    public void updateDataBeforeSync() {
        super.updateDataBeforeSync();
        this.updateDirtyEffects();
    }

    protected void tickEffects() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Iterator<Object> iterator = this.activeEffects.keySet().iterator();
            try {
                while (iterator.hasNext()) {
                    Holder mobEffect = (Holder)iterator.next();
                    MobEffectInstance effect = this.activeEffects.get(mobEffect);
                    if (!effect.tickServer(serverLevel, this, () -> this.onEffectUpdated(effect, true, null))) {
                        iterator.remove();
                        this.onEffectsRemoved(List.of(effect));
                        continue;
                    }
                    if (effect.getDuration() % 600 != 0) continue;
                    this.onEffectUpdated(effect, false, null);
                }
            }
            catch (ConcurrentModificationException mobEffect) {}
        } else {
            for (MobEffectInstance effect : this.activeEffects.values()) {
                effect.tickClient();
            }
            List<ParticleOptions> particles = this.entityData.get(DATA_EFFECT_PARTICLES);
            if (!particles.isEmpty()) {
                int ambientFactor;
                boolean isAmbient = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
                int bound = this.isInvisible() ? 15 : 4;
                int n = ambientFactor = isAmbient ? 5 : 1;
                if (this.random.nextInt(bound * ambientFactor) == 0) {
                    this.level().addParticle(Util.getRandom(particles, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
                }
            }
        }
    }

    private void updateDirtyEffects() {
        if (this.effectsDirty) {
            this.updateInvisibilityStatus();
            this.updateGlowingStatus();
            this.effectsDirty = false;
        }
    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
            return;
        }
        this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
        this.updateSynchronizedMobEffectParticles();
    }

    private void updateSynchronizedMobEffectParticles() {
        List<ParticleOptions> visibleEffectParticles = this.activeEffects.values().stream().filter(MobEffectInstance::isVisible).map(MobEffectInstance::getParticleOptions).toList();
        this.entityData.set(DATA_EFFECT_PARTICLES, visibleEffectParticles);
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, LivingEntity.areAllEffectsAmbient(this.activeEffects.values()));
    }

    private void updateGlowingStatus() {
        boolean glowingState = this.isCurrentlyGlowing();
        if (this.getSharedFlag(6) != glowingState) {
            this.setSharedFlag(6, glowingState);
        }
    }

    public double getVisibilityPercent(@Nullable Entity targetingEntity) {
        double visibilityPercent = 1.0;
        if (this.isDiscrete()) {
            visibilityPercent *= 0.8;
        }
        if (this.isInvisible()) {
            float coverPercentage = this.getArmorCoverPercentage();
            if (coverPercentage < 0.1f) {
                coverPercentage = 0.1f;
            }
            visibilityPercent *= 0.7 * (double)coverPercentage;
        }
        if (targetingEntity != null) {
            ItemStack itemStack = this.getItemBySlot(EquipmentSlot.HEAD);
            if (targetingEntity.is(EntityTypes.SKELETON) && itemStack.is(Items.SKELETON_SKULL) || targetingEntity.is(EntityTypes.ZOMBIE) && itemStack.is(Items.ZOMBIE_HEAD) || targetingEntity.is(EntityTypes.PIGLIN) && itemStack.is(Items.PIGLIN_HEAD) || targetingEntity.is(EntityTypes.PIGLIN_BRUTE) && itemStack.is(Items.PIGLIN_HEAD) || targetingEntity.is(EntityTypes.CREEPER) && itemStack.is(Items.CREEPER_HEAD)) {
                visibilityPercent *= 0.5;
            }
        }
        return visibilityPercent;
    }

    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return target.canBeSeenAsEnemy();
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> effects) {
        for (MobEffectInstance effect : effects) {
            if (!effect.isVisible() || effect.isAmbient()) continue;
            return false;
        }
        return true;
    }

    protected void removeEffectParticles() {
        this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
    }

    public boolean removeAllEffects() {
        if (this.level().isClientSide()) {
            return false;
        }
        if (this.activeEffects.isEmpty()) {
            return false;
        }
        HashMap copy = Maps.newHashMap(this.activeEffects);
        this.activeEffects.clear();
        this.onEffectsRemoved(copy.values());
        return true;
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        return this.activeEffects.values();
    }

    public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(Holder<MobEffect> effect) {
        return this.activeEffects.containsKey(effect);
    }

    public @Nullable MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return this.activeEffects.get(effect);
    }

    public float getEffectBlendFactor(Holder<MobEffect> effect, float partialTicks) {
        MobEffectInstance instance = this.getEffect(effect);
        if (instance != null) {
            return instance.getBlendFactor(this, partialTicks);
        }
        return 0.0f;
    }

    public final boolean addEffect(MobEffectInstance newEffect) {
        return this.addEffect(newEffect, null);
    }

    public boolean addEffect(MobEffectInstance newEffect, @Nullable Entity source) {
        if (!this.canBeAffected(newEffect)) {
            return false;
        }
        MobEffectInstance effect = this.activeEffects.get(newEffect.getEffect());
        boolean changed = false;
        if (effect == null) {
            this.activeEffects.put(newEffect.getEffect(), newEffect);
            this.onEffectAdded(newEffect, source);
            changed = true;
            newEffect.onEffectAdded(this);
        } else if (effect.update(newEffect)) {
            this.onEffectUpdated(effect, true, source);
            changed = true;
        }
        newEffect.onEffectStarted(this);
        return changed;
    }

    public boolean canBeAffected(MobEffectInstance newEffect) {
        if (this.is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
            return !newEffect.is(MobEffects.INFESTED);
        }
        if (this.is(EntityTypeTags.IMMUNE_TO_OOZING)) {
            return !newEffect.is(MobEffects.OOZING);
        }
        if (this.is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
            return !newEffect.is(MobEffects.REGENERATION) && !newEffect.is(MobEffects.POISON);
        }
        return true;
    }

    public void forceAddEffect(MobEffectInstance newEffect, @Nullable Entity source) {
        if (!this.canBeAffected(newEffect)) {
            return;
        }
        MobEffectInstance previousEffect = this.activeEffects.put(newEffect.getEffect(), newEffect);
        if (previousEffect == null) {
            this.onEffectAdded(newEffect, source);
        } else {
            newEffect.copyBlendState(previousEffect);
            this.onEffectUpdated(newEffect, true, source);
        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
    }

    public final @Nullable MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
        return this.activeEffects.remove(effect);
    }

    public boolean removeEffect(Holder<MobEffect> effect) {
        MobEffectInstance effectInstance = this.removeEffectNoUpdate(effect);
        if (effectInstance != null) {
            this.onEffectsRemoved(List.of(effectInstance));
            return true;
        }
        return false;
    }

    protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;
            effect.getEffect().value().addAttributeModifiers(this.getAttributes(), effect.getAmplifier());
            this.sendEffectToPassengers(effect);
        }
    }

    public void sendEffectToPassengers(MobEffectInstance effect) {
        for (Entity passenger : this.getPassengers()) {
            if (!(passenger instanceof ServerPlayer)) continue;
            ServerPlayer serverPlayer = (ServerPlayer)passenger;
            serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect, false));
        }
    }

    protected void onEffectUpdated(MobEffectInstance effect, boolean doRefreshAttributes, @Nullable Entity source) {
        if (this.level().isClientSide()) {
            return;
        }
        this.effectsDirty = true;
        if (doRefreshAttributes) {
            MobEffect mobEffect = effect.getEffect().value();
            mobEffect.removeAttributeModifiers(this.getAttributes());
            mobEffect.addAttributeModifiers(this.getAttributes(), effect.getAmplifier());
            this.refreshDirtyAttributes();
        }
        this.sendEffectToPassengers(effect);
    }

    protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
        if (this.level().isClientSide()) {
            return;
        }
        this.effectsDirty = true;
        for (MobEffectInstance effect : effects) {
            effect.getEffect().value().removeAttributeModifiers(this.getAttributes());
            for (Entity passenger : this.getPassengers()) {
                if (!(passenger instanceof ServerPlayer)) continue;
                ServerPlayer serverPlayer = (ServerPlayer)passenger;
                serverPlayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), effect.getEffect()));
            }
        }
        this.refreshDirtyAttributes();
    }

    private void refreshDirtyAttributes() {
        Set<AttributeInstance> attributesToUpdate = this.getAttributes().getAttributesToUpdate();
        for (AttributeInstance changedAttributeInstance : attributesToUpdate) {
            this.onAttributeUpdated(changedAttributeInstance.getAttribute());
        }
        attributesToUpdate.clear();
    }

    protected void onAttributeUpdated(Holder<Attribute> attribute) {
        Level level;
        if (attribute.is(Attributes.MAX_HEALTH)) {
            float currentMaxHealth = this.getMaxHealth();
            if (this.getHealth() > currentMaxHealth) {
                this.setHealth(currentMaxHealth);
            }
        } else if (attribute.is(Attributes.MAX_ABSORPTION)) {
            float currentMaxAbsorption = this.getMaxAbsorption();
            if (this.getAbsorptionAmount() > currentMaxAbsorption) {
                this.setAbsorptionAmount(currentMaxAbsorption);
            }
        } else if (attribute.is(Attributes.SCALE)) {
            this.refreshDimensions();
        } else if (attribute.is(Attributes.WAYPOINT_TRANSMIT_RANGE) && (level = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            ServerWaypointManager waypointManager = serverLevel.getWaypointManager();
            if (this.attributes.getValue(attribute) > 0.0) {
                waypointManager.trackWaypoint(this);
            } else {
                waypointManager.untrackWaypoint(this);
            }
        }
    }

    public void heal(float heal) {
        float health = this.getHealth();
        if (health > 0.0f) {
            this.setHealth(health + heal);
        }
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH_ID).floatValue();
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH_ID, Float.valueOf(Mth.clamp(health, 0.0f, this.getMaxHealth())));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0f || this.dead;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity entity;
        boolean success;
        boolean blocked;
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if (this.isDeadOrDying()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        }
        if (this.isSleeping()) {
            this.stopSleeping();
        }
        this.noActionTime = 0;
        if (damage < 0.0f) {
            damage = 0.0f;
        }
        float originalDamage = damage;
        ItemStack itemInUse = this.getUseItem();
        float damageBlocked = this.applyItemBlocking(level, source, damage);
        damage -= damageBlocked;
        boolean bl = blocked = damageBlocked > 0.0f;
        if (source.is(DamageTypeTags.IS_FREEZING) && this.is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            damage *= 5.0f;
        }
        if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.hurtHelmet(source, damage);
            damage *= 0.75f;
        }
        if (Float.isNaN(damage) || Float.isInfinite(damage)) {
            damage = Float.MAX_VALUE;
        }
        boolean tookFullDamage = true;
        if ((float)this.invulnerableTime > 10.0f && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (damage <= this.lastHurt) {
                return false;
            }
            this.actuallyHurt(level, source, damage - this.lastHurt);
            this.lastHurt = damage;
            tookFullDamage = false;
        } else {
            this.lastHurt = damage;
            this.invulnerableTime = 20;
            this.actuallyHurt(level, source, damage);
            this.hurtTime = this.hurtDuration = 10;
        }
        this.resolveMobResponsibleForDamage(source);
        this.resolvePlayerResponsibleForDamage(source);
        if (tookFullDamage) {
            BlocksAttacks blocksAttacks = itemInUse.get(DataComponents.BLOCKS_ATTACKS);
            if (blocked && blocksAttacks != null) {
                blocksAttacks.onBlocked(level, this);
            } else {
                level.broadcastDamageEvent(this, source);
            }
            if (!(source.is(DamageTypeTags.NO_IMPACT) || blocked && !(damage > 0.0f))) {
                this.markHurt();
            }
            if (!source.is(DamageTypeTags.NO_KNOCKBACK)) {
                this.dealDefaultKnockback(source, damage, blocked);
            }
        }
        if (this.isDeadOrDying()) {
            if (!this.checkTotemDeathProtection(source)) {
                if (tookFullDamage) {
                    this.makeSound(this.getDeathSound());
                    this.playSecondaryHurtSound(source);
                }
                this.die(source);
            }
        } else if (tookFullDamage) {
            this.playHurtSound(source);
            this.playSecondaryHurtSound(source);
        }
        boolean bl2 = success = !blocked || damage > 0.0f;
        if (success) {
            this.lastDamageSource = source;
            this.lastDamageStamp = this.level().getGameTime();
            for (MobEffectInstance effect : this.getActiveEffects()) {
                effect.onMobHurt(level, this, source, damage);
            }
        }
        if ((entity = this) instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverPlayer, source, originalDamage, damage, blocked);
            if (damageBlocked > 0.0f && damageBlocked < 3.4028235E37f) {
                serverPlayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(damageBlocked * 10.0f));
            }
        }
        if ((entity = source.getEntity()) instanceof ServerPlayer) {
            ServerPlayer sourcePlayer = (ServerPlayer)entity;
            CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(sourcePlayer, this, source, originalDamage, damage, blocked);
        }
        return success;
    }

    public void dealDefaultKnockback(DamageSource source, float damage, boolean blocked) {
        double xd = 0.0;
        double zd = 0.0;
        Entity entity = source.getDirectEntity();
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile)entity;
            DoubleDoubleImmutablePair knockbackDirection = projectile.calculateHorizontalHurtKnockbackDirection(this, source);
            xd = -knockbackDirection.leftDouble();
            zd = -knockbackDirection.rightDouble();
        } else if (source.getSourcePosition() != null) {
            xd = source.getSourcePosition().x() - this.getX();
            zd = source.getSourcePosition().z() - this.getZ();
        }
        this.knockback(0.4f, xd, zd, source, damage);
        if (!blocked) {
            this.indicateDamage(xd, zd);
        }
    }

    public float applyItemBlocking(ServerLevel level, DamageSource source, float damage) {
        Entity directEntity;
        double angle;
        AbstractArrow abstractArrow;
        if (damage <= 0.0f) {
            return 0.0f;
        }
        ItemStack blockingWith = this.getItemBlockingWith();
        if (blockingWith == null) {
            return 0.0f;
        }
        BlocksAttacks blocksAttacks = blockingWith.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null || blocksAttacks.bypassedBy().map(t -> t.contains(source.typeHolder())).orElse(false).booleanValue()) {
            return 0.0f;
        }
        Entity entity = source.getDirectEntity();
        if (entity instanceof AbstractArrow && (abstractArrow = (AbstractArrow)entity).getPierceLevel() > 0) {
            return 0.0f;
        }
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition != null) {
            Vec3 viewVector = this.calculateViewVector(0.0f, this.getYHeadRot());
            Vec3 vectorTo = sourcePosition.subtract(this.position());
            vectorTo = new Vec3(vectorTo.x, 0.0, vectorTo.z).normalize();
            angle = Math.acos(vectorTo.dot(viewVector));
        } else {
            angle = 3.1415927410125732;
        }
        float damageBlocked = blocksAttacks.resolveBlockedDamage(source, damage, angle);
        blocksAttacks.hurtBlockingItem(this.level(), blockingWith, this, this.getUsedItemHand(), damageBlocked);
        if (damageBlocked > 0.0f && !source.is(DamageTypeTags.IS_PROJECTILE) && (directEntity = source.getDirectEntity()) instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)directEntity;
            this.blockUsingItem(level, livingEntity, source, damage);
        }
        return damageBlocked;
    }

    private void playSecondaryHurtSound(DamageSource source) {
        if (source.is(DamageTypes.THORNS)) {
            SoundSource soundSource = this instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            this.level().playSound(null, this.position().x, this.position().y, this.position().z, SoundEvents.THORNS_HIT, soundSource);
        }
    }

    protected void resolveMobResponsibleForDamage(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof LivingEntity) {
            LivingEntity livingSource = (LivingEntity)entity;
            if (!(source.is(DamageTypeTags.NO_ANGER) || source.is(DamageTypes.WIND_CHARGE) && this.is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
                this.setLastHurtByMob(livingSource);
            }
        }
    }

    protected @Nullable Player resolvePlayerResponsibleForDamage(DamageSource source) {
        Wolf wolf;
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof Player) {
            Player playerSource = (Player)sourceEntity;
            this.setLastHurtByPlayer(playerSource, 100);
        } else if (sourceEntity instanceof Wolf && (wolf = (Wolf)sourceEntity).isTame()) {
            if (wolf.getOwnerReference() != null) {
                this.setLastHurtByPlayer(wolf.getOwnerReference().getUUID(), 100);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
            }
        }
        return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
    }

    protected void blockUsingItem(ServerLevel level, LivingEntity attacker, DamageSource source, float damage) {
        attacker.blockedByItem(this, source, damage);
    }

    protected void blockedByItem(LivingEntity defender, DamageSource source, float damage) {
        defender.knockback(0.5, defender.getX() - this.getX(), defender.getZ() - this.getZ(), source, damage);
    }

    private boolean checkTotemDeathProtection(DamageSource killingDamage) {
        if (killingDamage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        ItemStack protectionItem = null;
        DeathProtection protection = null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack itemStack = this.getItemInHand(hand);
            protection = itemStack.get(DataComponents.DEATH_PROTECTION);
            if (protection == null) continue;
            protectionItem = itemStack.copy();
            itemStack.shrink(1);
            break;
        }
        if (protectionItem != null) {
            LivingEntity livingEntity = this;
            if (livingEntity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)livingEntity;
                player.awardStat(Stats.ITEM_USED.get(protectionItem.getItem()));
                CriteriaTriggers.USED_TOTEM.trigger(player, protectionItem);
                protectionItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
            }
            this.setHealth(1.0f);
            protection.applyEffects(protectionItem, this);
            this.level().broadcastEntityEvent(this, (byte)35);
        }
        return protection != null;
    }

    public @Nullable DamageSource getLastDamageSource() {
        if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }
        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource source) {
        this.makeSound(this.getHurtSound(source));
    }

    public void makeSound(@Nullable SoundEvent sound) {
        if (sound != null) {
            this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    private void breakItem(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            Holder<SoundEvent> breakSound = itemStack.get(DataComponents.BREAK_SOUND);
            if (breakSound != null && !this.isSilent()) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), breakSound.value(), this.getSoundSource(), 0.8f, 0.8f + this.random.nextFloat() * 0.4f, false);
            }
            this.spawnItemParticles(itemStack, 5);
        }
    }

    public void die(DamageSource source) {
        if (this.isRemoved() || this.dead) {
            return;
        }
        Entity sourceEntity = source.getEntity();
        LivingEntity killer = this.getKillCredit();
        if (killer != null) {
            killer.awardKillScore(this, source);
        }
        if (this.isSleeping()) {
            this.stopSleeping();
        }
        this.stopUsingItem();
        if (!this.level().isClientSide() && this.hasCustomName()) {
            LOGGER.info("Named entity {} died: {}", (Object)this, (Object)this.getCombatTracker().getDeathMessage().getString());
        }
        this.handleKillingBlow();
        this.getCombatTracker().recheckStatus();
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (sourceEntity == null || sourceEntity.killedEntity(serverLevel, this, source)) {
                this.gameEvent(GameEvent.ENTITY_DIE);
                this.dropAllDeathLoot(serverLevel, source);
                this.createWitherRose(killer);
            }
            this.level().broadcastEntityEvent(this, (byte)3);
        }
        this.setPose(Pose.DYING);
    }

    protected void handleKillingBlow() {
        this.dead = true;
    }

    protected void createWitherRose(@Nullable LivingEntity killer) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        boolean plantedWitherRose = false;
        if (killer instanceof WitherBoss) {
            if (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                BlockPos pos = this.blockPosition();
                BlockState state = Blocks.WITHER_ROSE.defaultBlockState();
                if (this.level().getBlockState(pos).isAir() && state.canSurvive(this.level(), pos)) {
                    this.level().setBlock(pos, state, 3);
                    plantedWitherRose = true;
                }
            }
            if (!plantedWitherRose) {
                ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
                this.level().addFreshEntity(itemEntity);
            }
        }
    }

    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        boolean playerKilled;
        boolean bl = playerKilled = this.lastHurtByPlayerMemoryTime > 0;
        if (this.shouldDropLoot(level)) {
            this.dropFromLootTable(level, source, playerKilled);
            this.dropCustomDeathLoot(level, source, playerKilled);
        }
        this.dropEquipment(level);
        this.dropExperience(level, source.getEntity());
    }

    protected void dropEquipment(ServerLevel level) {
    }

    protected void dropExperience(ServerLevel level, @Nullable Entity killer) {
        if (!this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && level.getGameRules().get(GameRules.MOB_DROPS).booleanValue())) {
            ExperienceOrb.award(level, this.position(), this.getExperienceReward(level, killer));
        }
    }

    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
    }

    public long getLootTableSeed() {
        return 0L;
    }

    protected float getKnockback(Entity target, DamageSource damageSource) {
        float knockback = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            return EnchantmentHelper.modifyKnockback(level2, this.getWeaponItem(), target, damageSource, knockback) / 2.0f;
        }
        return knockback / 2.0f;
    }

    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        Optional<ResourceKey<LootTable>> lootTable = this.getLootTable();
        if (lootTable.isEmpty()) {
            return;
        }
        this.dropFromLootTable(level, source, playerKilled, lootTable.get());
    }

    public void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled, ResourceKey<LootTable> lootTable) {
        this.dropFromLootTable(level, source, playerKilled, lootTable, itemStack -> this.spawnAtLocation(level, (ItemStack)itemStack));
    }

    public void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled, ResourceKey<LootTable> lootTable, Consumer<ItemStack> itemStackConsumer) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(lootTable);
        LootParams.Builder builder = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.DAMAGE_SOURCE, source).withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity()).withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
        Player killerPlayer = this.getLastHurtByPlayer();
        if (playerKilled && killerPlayer != null) {
            builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killerPlayer).withLuck(killerPlayer.getLuck());
        }
        LootParams params = builder.create(LootContextParamSets.ENTITY);
        table.getRandomItems(params, this.getLootTableSeed(), itemStackConsumer);
    }

    public boolean dropFromEntityInteractLootTable(ServerLevel level, ResourceKey<LootTable> key, @Nullable Entity interactingEntity, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> consumer) {
        return this.dropFromLootTable(level, key, params -> params.withParameter(LootContextParams.TARGET_ENTITY, this).withOptionalParameter(LootContextParams.INTERACTING_ENTITY, interactingEntity).withParameter(LootContextParams.TOOL, tool).create(LootContextParamSets.ENTITY_INTERACT), consumer);
    }

    public boolean dropFromGiftLootTable(ServerLevel level, ResourceKey<LootTable> key, BiConsumer<ServerLevel, ItemStack> consumer) {
        return this.dropFromLootTable(level, key, params -> params.withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.GIFT), consumer);
    }

    protected void dropFromShearingLootTable(ServerLevel level, ResourceKey<LootTable> key, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> consumer) {
        this.dropFromLootTable(level, key, params -> params.withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.TOOL, tool).create(LootContextParamSets.SHEARING), consumer);
    }

    protected boolean dropFromLootTable(ServerLevel level, ResourceKey<LootTable> key, Function<LootParams.Builder, LootParams> paramsBuilder, BiConsumer<ServerLevel, ItemStack> consumer) {
        LootParams params;
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(key);
        ObjectArrayList<ItemStack> drops = lootTable.getRandomItems(params = paramsBuilder.apply(new LootParams.Builder(level)));
        if (!drops.isEmpty()) {
            drops.forEach(stack -> consumer.accept(level, (ItemStack)stack));
            return true;
        }
        return false;
    }

    public void knockback(double power, double xd, double zd, DamageSource source, float damage, boolean comesFromEffect) {
        if ((power *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) <= 0.0) {
            return;
        }
        this.needsSync = true;
        Vec3 deltaMovement = this.getDeltaMovement();
        while (xd * xd + zd * zd < (double)1.0E-5f) {
            xd = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
            zd = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
        }
        Vec3 deltaVector = new Vec3(xd, 0.0, zd).normalize().scale(power);
        this.setDeltaMovement(deltaMovement.x / 2.0 - deltaVector.x, this.onGround() ? Math.min(0.4, deltaMovement.y / 2.0 + power) : deltaMovement.y, deltaMovement.z / 2.0 - deltaVector.z);
    }

    public void knockback(double power, double xd, double zd, DamageSource source, float damage) {
        this.knockback(power, xd, zd, source, damage, false);
    }

    public void indicateDamage(double xd, double zd) {
    }

    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    private SoundEvent getFallDamageSound(int dmg) {
        return dmg > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    public void skipDropExperience() {
        this.skipDropExperience = true;
    }

    public boolean wasExperienceConsumed() {
        return this.skipDropExperience;
    }

    public float getHurtDir() {
        return 0.0f;
    }

    protected AABB getHitbox() {
        AABB aabb = this.getBoundingBox();
        Entity vehicle = this.getVehicle();
        if (vehicle != null) {
            Vec3 pos = vehicle.getPassengerRidingPosition(this);
            return aabb.setMinY(Math.max(pos.y, aabb.minY));
        }
        return aabb;
    }

    public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot slot) {
        return (Map)this.activeLocationDependentEnchantments.computeIfAbsent(slot, s -> new Reference2ObjectArrayMap());
    }

    public void postPiercingAttack() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            EnchantmentHelper.doPostPiercingAttackEffects(serverLevel, this);
        }
    }

    public Fallsounds getFallSounds() {
        return new Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
    }

    public Optional<BlockPos> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        }
        BlockPos ladderCheckPos = this.blockPosition();
        BlockState state = this.getInBlockState();
        if (this.isFallFlying() && state.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return false;
        }
        if (state.is(BlockTags.CLIMBABLE)) {
            this.lastClimbablePos = Optional.of(ladderCheckPos);
            return true;
        }
        if (state.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(ladderCheckPos, state)) {
            this.lastClimbablePos = Optional.of(ladderCheckPos);
            return true;
        }
        return false;
    }

    private boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state) {
        if (state.getValue(TrapDoorBlock.OPEN).booleanValue()) {
            BlockState belowState = this.level().getBlockState(pos.below());
            return belowState.is(Blocks.LADDER) && belowState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
        }
        return false;
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0f;
    }

    public boolean isLookingAtMe(LivingEntity target, double coneSize, boolean adjustForDistance, boolean seeThroughTransparentBlocks, double ... gazeHeights) {
        Vec3 look = target.getViewVector(1.0f).normalize();
        for (double gazeHeight : gazeHeights) {
            Vec3 dir = new Vec3(this.getX() - target.getX(), gazeHeight - target.getEyeY(), this.getZ() - target.getZ());
            double dist = dir.length();
            dir = dir.normalize();
            double dot = look.dot(dir);
            double d = adjustForDistance ? dist : 1.0;
            if (!(dot > 1.0 - coneSize / d) || !target.hasLineOfSight(this, seeThroughTransparentBlocks ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, gazeHeight)) continue;
            return true;
        }
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return this.getComfortableFallDistance(0.0f);
    }

    protected final int getComfortableFallDistance(float allowedDamage) {
        return Mth.floor(allowedDamage + 3.0f);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        double effectiveFallDistance;
        if (this.isIgnoringFallDamageFromCurrentImpulse()) {
            boolean hasLandedAboveCurrentImpulseImpactPosY;
            effectiveFallDistance = Math.min(fallDistance, this.currentImpulseImpactPos.y - this.getY());
            boolean bl = hasLandedAboveCurrentImpulseImpactPosY = effectiveFallDistance <= 0.0;
            if (hasLandedAboveCurrentImpulseImpactPosY) {
                this.resetCurrentImpulseContext();
            } else {
                this.tryResetCurrentImpulseContext();
            }
        } else {
            effectiveFallDistance = fallDistance;
        }
        boolean damaged = super.causeFallDamage(effectiveFallDistance, damageModifier, damageSource);
        int dmg = this.calculateFallDamage(effectiveFallDistance, damageModifier);
        if (dmg > 0) {
            this.resetCurrentImpulseContext();
            this.playSound(this.getFallDamageSound(dmg), 1.0f, 1.0f);
            this.playBlockFallSound();
            this.hurt(damageSource, dmg);
            return true;
        }
        return damaged;
    }

    public void setIgnoreFallDamageFromCurrentImpulse(boolean ignoreFallDamage, Vec3 newImpulseImpactPos) {
        if (ignoreFallDamage) {
            this.applyPostImpulseGraceTime(40);
            this.currentImpulseImpactPos = newImpulseImpactPos;
        } else {
            this.currentImpulseContextResetGraceTime = 0;
        }
    }

    public void applyPostImpulseGraceTime(int ticks) {
        this.currentImpulseContextResetGraceTime = Math.max(this.currentImpulseContextResetGraceTime, ticks);
    }

    public boolean isIgnoringFallDamageFromCurrentImpulse() {
        return this.currentImpulseImpactPos != null;
    }

    public void tryResetCurrentImpulseContext() {
        if (this.currentImpulseContextResetGraceTime == 0) {
            this.resetCurrentImpulseContext();
        }
    }

    public boolean isInPostImpulseGraceTime() {
        return this.currentImpulseContextResetGraceTime > 0;
    }

    public void resetCurrentImpulseContext() {
        this.currentImpulseContextResetGraceTime = 0;
        this.currentImpulseImpactPos = null;
    }

    protected int calculateFallDamage(double fallDistance, float damageModifier) {
        if (this.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return 0;
        }
        double baseDamage = this.calculateFallPower(fallDistance);
        return Mth.floor(baseDamage * (double)damageModifier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    }

    private double calculateFallPower(double fallDistance) {
        return fallDistance + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
    }

    protected void playBlockFallSound() {
        if (this.isSilent()) {
            return;
        }
        int xx = Mth.floor(this.getX());
        int yy = Mth.floor(this.getY() - (double)0.2f);
        int zz = Mth.floor(this.getZ());
        BlockState state = this.level().getBlockState(new BlockPos(xx, yy, zz));
        if (!state.isAir()) {
            SoundType soundType = state.getSoundType();
            this.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5f, soundType.getPitch() * 0.75f);
        }
    }

    @Override
    public void animateHurt(float yaw) {
        this.hurtTime = this.hurtDuration = 10;
    }

    public int getArmorValue() {
        return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
    }

    protected void hurtArmor(DamageSource damageSource, float damage) {
    }

    protected void hurtHelmet(DamageSource damageSource, float damage) {
    }

    protected void doHurtEquipment(DamageSource damageSource, float damage, EquipmentSlot ... slots) {
        if (damage <= 0.0f) {
            return;
        }
        int durabilityDamage = (int)Math.max(1.0f, damage / 4.0f);
        for (EquipmentSlot slot : slots) {
            ItemStack itemStack = this.getItemBySlot(slot);
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            if (equippable == null || !equippable.damageOnHurt() || !itemStack.isDamageableItem() || !itemStack.canBeHurtBy(damageSource)) continue;
            itemStack.hurtAndBreak(durabilityDamage, this, slot);
        }
    }

    protected float getDamageAfterArmorAbsorb(DamageSource damageSource, float damage) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
            this.hurtArmor(damageSource, damage);
            damage = CombatRules.getDamageAfterAbsorb(this, damage, damageSource, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }
        return damage;
    }

    protected float getDamageAfterMagicAbsorb(DamageSource damageSource, float damage) {
        float enchantmentArmor;
        int absorbValue;
        int absorb;
        float v;
        float oldDamage;
        float damageResisted;
        if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return damage;
        }
        if (this.hasEffect(MobEffects.RESISTANCE) && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE) && (damageResisted = (oldDamage = damage) - (damage = Math.max((v = damage * (float)(absorb = 25 - (absorbValue = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5))) / 25.0f, 0.0f))) > 0.0f && damageResisted < 3.4028235E37f) {
            LivingEntity livingEntity = this;
            if (livingEntity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)livingEntity;
                serverPlayer.awardStat(Stats.DAMAGE_RESISTED, Math.round(damageResisted * 10.0f));
            } else if (damageSource.getEntity() instanceof ServerPlayer) {
                ((ServerPlayer)damageSource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(damageResisted * 10.0f));
            }
        }
        if (damage <= 0.0f) {
            return 0.0f;
        }
        if (damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return damage;
        }
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            enchantmentArmor = EnchantmentHelper.getDamageProtection(serverLevel, this, damageSource);
        } else {
            enchantmentArmor = 0.0f;
        }
        if (enchantmentArmor > 0.0f) {
            damage = CombatRules.getDamageAfterMagicAbsorb(damage, enchantmentArmor);
        }
        return damage;
    }

    protected void actuallyHurt(ServerLevel level, DamageSource source, float dmg) {
        Entity entity;
        if (this.isInvulnerableTo(level, source)) {
            return;
        }
        dmg = this.getDamageAfterArmorAbsorb(source, dmg);
        float originalDamage = dmg = this.getDamageAfterMagicAbsorb(source, dmg);
        dmg = Math.max(dmg - this.getAbsorptionAmount(), 0.0f);
        this.setAbsorptionAmount(this.getAbsorptionAmount() - (originalDamage - dmg));
        float absorbedDamage = originalDamage - dmg;
        if (absorbedDamage > 0.0f && absorbedDamage < 3.4028235E37f && (entity = source.getEntity()) instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            serverPlayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbedDamage * 10.0f));
        }
        if (dmg == 0.0f) {
            return;
        }
        this.getCombatTracker().recordDamage(source, dmg);
        this.setHealth(this.getHealth() - dmg);
        this.setAbsorptionAmount(this.getAbsorptionAmount() - dmg);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    public @Nullable LivingEntity getKillCredit() {
        if (this.lastHurtByPlayer != null) {
            return this.lastHurtByPlayer.getEntity(this.level(), Player.class);
        }
        if (this.lastHurtByMob != null) {
            return this.lastHurtByMob.getEntity(this.level(), LivingEntity.class);
        }
        return null;
    }

    public final float getMaxHealth() {
        return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
    }

    public final float getMaxAbsorption() {
        return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
    }

    public final int getArrowCount() {
        return this.entityData.get(DATA_ARROW_COUNT_ID);
    }

    public final void setArrowCount(int count) {
        this.entityData.set(DATA_ARROW_COUNT_ID, count);
    }

    public final int getStingerCount() {
        return this.entityData.get(DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int count) {
        this.entityData.set(DATA_STINGER_COUNT_ID, count);
    }

    private int getCurrentSwingDuration() {
        InteractionHand hand = this.swingingArm != null ? this.swingingArm : InteractionHand.MAIN_HAND;
        ItemStack handStack = this.getItemInHand(hand);
        int swingDuration = handStack.getSwingAnimation().duration();
        if (MobEffectUtil.hasDigSpeed(this)) {
            return swingDuration - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        }
        if (this.hasEffect(MobEffects.MINING_FATIGUE)) {
            return swingDuration + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2;
        }
        return swingDuration;
    }

    public void swing(InteractionHand hand) {
        this.swing(hand, false);
    }

    public void swing(InteractionHand hand, boolean sendToSwingingEntity) {
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = hand;
            if (this.level() instanceof ServerLevel) {
                ClientboundAnimatePacket packet = new ClientboundAnimatePacket(this, hand == InteractionHand.MAIN_HAND ? 0 : 3);
                ServerChunkCache chunkSource = ((ServerLevel)this.level()).getChunkSource();
                if (sendToSwingingEntity) {
                    chunkSource.sendToTrackingPlayersAndSelf(this, packet);
                } else {
                    chunkSource.sendToTrackingPlayers(this, packet);
                }
            }
        }
    }

    @Override
    public void handleDamageEvent(DamageSource source) {
        this.walkAnimation.setSpeed(1.5f);
        this.invulnerableTime = 20;
        this.hurtTime = this.hurtDuration = 10;
        SoundEvent hurtSound = this.getHurtSound(source);
        if (hurtSound != null) {
            this.playSound(hurtSound, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
        }
        this.lastDamageSource = source;
        this.lastDamageStamp = this.level().getGameTime();
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 3: {
                SoundEvent deathSound = this.getDeathSound();
                if (deathSound != null) {
                    this.playSound(deathSound, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                }
                if (this instanceof Player) break;
                this.setHealth(0.0f);
                this.die(this.damageSources().generic());
                break;
            }
            case 46: {
                int count = 128;
                for (int i = 0; i < 128; ++i) {
                    double d = (double)i / 127.0;
                    float xa = (this.random.nextFloat() - 0.5f) * 0.2f;
                    float ya = (this.random.nextFloat() - 0.5f) * 0.2f;
                    float za = (this.random.nextFloat() - 0.5f) * 0.2f;
                    double x = Mth.lerp(d, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0;
                    double y = Mth.lerp(d, this.yo, this.getY()) + this.random.nextDouble() * (double)this.getBbHeight();
                    double z = Mth.lerp(d, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0;
                    this.level().addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
                }
                break;
            }
            case 47: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
                break;
            }
            case 48: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
                break;
            }
            case 49: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
                break;
            }
            case 50: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
                break;
            }
            case 51: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
                break;
            }
            case 52: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
                break;
            }
            case 65: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
                break;
            }
            case 68: {
                this.breakItem(this.getItemBySlot(EquipmentSlot.SADDLE));
                break;
            }
            case 54: {
                HoneyBlock.showJumpParticles(this);
                break;
            }
            case 55: {
                this.swapHandItems();
                break;
            }
            case 60: {
                this.makePoofParticles();
                break;
            }
            case 67: {
                this.makeDrownParticles();
                break;
            }
            case 2: {
                this.onKineticHit();
                break;
            }
            default: {
                super.handleEntityEvent(id);
            }
        }
    }

    public float getTicksSinceLastKineticHitFeedback(float partial) {
        if (this.lastKineticHitFeedbackTime < 0L) {
            return 0.0f;
        }
        return (float)(this.level().getGameTime() - this.lastKineticHitFeedbackTime) + partial;
    }

    public void makePoofParticles() {
        for (int i = 0; i < 20; ++i) {
            double xa = this.random.nextGaussian() * 0.02;
            double ya = this.random.nextGaussian() * 0.02;
            double za = this.random.nextGaussian() * 0.02;
            double dd = 10.0;
            this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - xa * 10.0, this.getRandomY() - ya * 10.0, this.getRandomZ(1.0) - za * 10.0, xa, ya, za);
        }
    }

    private void makeDrownParticles() {
        Vec3 movement = this.getDeltaMovement();
        for (int i = 0; i < 8; ++i) {
            double offsetX = this.random.triangle(0.0, 1.0);
            double offsetY = this.random.triangle(0.0, 1.0);
            double offsetZ = this.random.triangle(0.0, 1.0);
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, movement.x, movement.y, movement.z);
        }
    }

    private void onKineticHit() {
        if (this.level().getGameTime() - this.lastKineticHitFeedbackTime <= 10L) {
            return;
        }
        this.lastKineticHitFeedbackTime = this.level().getGameTime();
        KineticWeapon kineticWeapon = this.useItem.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return;
        }
        kineticWeapon.makeLocalHitSound(this);
    }

    private void swapHandItems() {
        ItemStack tmp = this.getItemBySlot(EquipmentSlot.OFFHAND);
        this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
        this.setItemSlot(EquipmentSlot.MAINHAND, tmp);
    }

    @Override
    protected void onBelowWorld() {
        this.hurt(this.damageSources().fellOutOfWorld(), 4.0f);
    }

    protected void updateSwingTime() {
        int currentSwingDuration = this.getCurrentSwingDuration();
        if (this.swinging) {
            ++this.swingTime;
            if (this.swingTime >= currentSwingDuration) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }
        this.attackAnim = (float)this.swingTime / (float)currentSwingDuration;
    }

    @Override
    protected double getEntityBounciness() {
        return this.getAttributeValue(Attributes.BOUNCINESS);
    }

    public @Nullable AttributeInstance getAttribute(Holder<Attribute> attribute) {
        return this.getAttributes().getInstance(attribute);
    }

    public double getAttributeValue(Holder<Attribute> attribute) {
        return this.getAttributes().getValue(attribute);
    }

    public double getAttributeBaseValue(Holder<Attribute> attribute) {
        return this.getAttributes().getBaseValue(attribute);
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EquipmentSlot.MAINHAND);
    }

    public ItemStack getOffhandItem() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND);
    }

    public ItemStack getItemHeldByArm(HumanoidArm arm) {
        return this.getMainArm() == arm ? this.getMainHandItem() : this.getOffhandItem();
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getMainHandItem();
    }

    public AttackRange getAttackRangeWith(ItemStack weaponItem) {
        AttackRange attackRange = weaponItem.get(DataComponents.ATTACK_RANGE);
        return attackRange != null ? attackRange : AttackRange.defaultFor(this);
    }

    public ItemStack getActiveItem() {
        if (this.isSpectator()) {
            return ItemStack.EMPTY;
        }
        if (this.isUsingItem()) {
            return this.getUseItem();
        }
        return this.getMainHandItem();
    }

    public boolean isHolding(Item item) {
        return this.isHolding((ItemStack heldItem) -> heldItem.is(item));
    }

    public boolean isHolding(Predicate<ItemStack> itemPredicate) {
        return itemPredicate.test(this.getMainHandItem()) || itemPredicate.test(this.getOffhandItem());
    }

    public ItemStack getItemInHand(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.getItemBySlot(EquipmentSlot.MAINHAND);
        }
        if (hand == InteractionHand.OFF_HAND) {
            return this.getItemBySlot(EquipmentSlot.OFFHAND);
        }
        throw new IllegalArgumentException("Invalid hand " + String.valueOf((Object)hand));
    }

    public void setItemInHand(InteractionHand hand, ItemStack itemStack) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
        } else if (hand == InteractionHand.OFF_HAND) {
            this.setItemSlot(EquipmentSlot.OFFHAND, itemStack);
        } else {
            throw new IllegalArgumentException("Invalid hand " + String.valueOf((Object)hand));
        }
    }

    public boolean hasItemInSlot(EquipmentSlot slot) {
        return !this.getItemBySlot(slot).isEmpty();
    }

    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return this.equipment.get(slot);
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack itemStack) {
        this.onEquipItem(slot, this.equipment.set(slot, itemStack), itemStack);
    }

    public float getArmorCoverPercentage() {
        int total = 0;
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack itemStack = this.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
                ++count;
            }
            ++total;
        }
        return total > 0 ? (float)count / (float)total : 0.0f;
    }

    @Override
    public void setSprinting(boolean isSprinting) {
        super.setSprinting(isSprinting);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(SPEED_MODIFIER_SPRINTING.id());
        if (isSprinting) {
            speed.addTransientModifier(SPEED_MODIFIER_SPRINTING);
        }
    }

    protected float getSoundVolume() {
        return 1.0f;
    }

    public float getVoicePitch() {
        if (this.isBaby()) {
            return (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.5f;
        }
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f;
    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    @Override
    public void push(Entity entity) {
        if (!this.isSleeping()) {
            super.push(entity);
        }
    }

    private void dismountVehicle(Entity vehicle) {
        Vec3 teleportTarget;
        if (this.isRemoved()) {
            teleportTarget = this.position();
        } else if (vehicle.isRemoved() || this.level().getBlockState(vehicle.blockPosition()).is(BlockTags.PORTALS)) {
            boolean isSmall;
            double maxY = Math.max(this.getY(), vehicle.getY());
            teleportTarget = new Vec3(this.getX(), maxY, this.getZ());
            boolean bl = isSmall = this.getBbWidth() <= 4.0f && this.getBbHeight() <= 4.0f;
            if (isSmall) {
                double halfHeight = (double)this.getBbHeight() / 2.0;
                Vec3 center = teleportTarget.add(0.0, halfHeight, 0.0);
                VoxelShape allowedCenters = Shapes.create(AABB.ofSize(center, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
                teleportTarget = this.level().findFreePosition(this, allowedCenters, center, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()).map(pos -> pos.add(0.0, -halfHeight, 0.0)).orElse(teleportTarget);
            }
        } else {
            teleportTarget = vehicle.getDismountLocationForPassenger(this);
        }
        this.dismountTo(teleportTarget.x, teleportTarget.y, teleportTarget.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    protected float getJumpPower() {
        return this.getJumpPower(1.0f);
    }

    protected float getJumpPower(float multiplier) {
        return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    public float getJumpBoostPower() {
        return this.hasEffect(MobEffects.JUMP_BOOST) ? 0.1f * ((float)this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0f) : 0.0f;
    }

    @VisibleForTesting
    public void jumpFromGround() {
        float jumpPower = this.getJumpPower();
        if (jumpPower <= 1.0E-5f) {
            return;
        }
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x, Math.max((double)jumpPower, movement.y), movement.z);
        if (this.isSprinting()) {
            float angle = this.getYRot() * ((float)Math.PI / 180);
            this.addDeltaMovement(new Vec3((double)(-Mth.sin(angle)) * 0.2, 0.0, (double)Mth.cos(angle) * 0.2));
        }
        this.needsSync = true;
    }

    protected void goDownInWater() {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04f, 0.0));
    }

    protected void jumpInLiquid(TagKey<Fluid> type) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04f, 0.0));
    }

    protected float getWaterSlowDown() {
        return 0.8f;
    }

    public boolean canStandOnFluid(FluidState fluid) {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return this.getAttributeValue(Attributes.GRAVITY);
    }

    @Override
    protected double getEffectiveGravity() {
        boolean isFalling;
        boolean bl = isFalling = this.getDeltaMovement().y <= 0.0;
        if (isFalling && this.hasEffect(MobEffects.SLOW_FALLING)) {
            return Math.min(this.getGravity(), 0.01);
        }
        return this.getGravity();
    }

    public void travel(Vec3 input) {
        if (this.shouldTravelInFluid(this.level().getFluidState(this.blockPosition()))) {
            this.travelInFluid(input);
        } else if (this.isFallFlying()) {
            this.travelFallFlying(input);
        } else {
            this.travelInAir(input);
        }
    }

    public VoxelShape getLiquidCollisionShape() {
        return Shapes.empty();
    }

    protected boolean shouldTravelInFluid(FluidState fluidState) {
        return (this.isInWater() || this.isInLava()) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState);
    }

    protected void travelFlying(Vec3 input, float speed) {
        this.travelFlying(input, 0.02f, 0.02f, speed);
    }

    protected void travelFlying(Vec3 input, float waterSpeed, float lavaSpeed, float airSpeed) {
        if (this.isInWater()) {
            this.moveRelative(waterSpeed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8f));
        } else if (this.isInLava()) {
            this.moveRelative(lavaSpeed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        } else {
            this.moveRelative(airSpeed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91f));
        }
    }

    private void travelInAir(Vec3 input) {
        BlockPos posBelow = this.getBlockPosBelowThatAffectsMyMovement();
        float blockFriction = this.onGround() ? LivingEntity.computeModifiedFriction(this.level().getBlockState(posBelow).getBlock().getFriction(), (float)this.getAttributeValue(Attributes.FRICTION_MODIFIER)) : 1.0f;
        Vec3 movement = this.handleRelativeFrictionAndCalculateMovement(input, blockFriction);
        double movementY = movement.y;
        MobEffectInstance levitationEffect = this.getEffect(MobEffects.LEVITATION);
        movementY = levitationEffect != null ? (movementY += (0.05 * (double)(levitationEffect.getAmplifier() + 1) - movement.y) * 0.2) : (!this.level().isClientSide() || this.level().hasChunkAt(posBelow) ? (movementY -= this.getEffectiveGravity()) : (this.getY() > (double)this.level().getMinY() ? -0.1 : 0.0));
        if (this.shouldDiscardFriction()) {
            this.setDeltaMovement(movement.x, movementY, movement.z);
        } else {
            float entityAirDragModifier = (float)this.getAttributeValue(Attributes.AIR_DRAG_MODIFIER);
            float airDrag = LivingEntity.computeModifiedFriction(0.91f, entityAirDragModifier);
            float friction = blockFriction * airDrag;
            float verticalFriction = this.omnidirectionalAirMover() ? airDrag : LivingEntity.computeModifiedFriction(0.98f, entityAirDragModifier);
            this.setDeltaMovement(movement.x * (double)friction, movementY * (double)verticalFriction, movement.z * (double)friction);
        }
    }

    @Override
    protected float getAirDrag() {
        return LivingEntity.computeModifiedFriction(this.omnidirectionalAirMover() ? 0.91f : 0.98f, (float)this.getAttributeValue(Attributes.AIR_DRAG_MODIFIER));
    }

    protected void travelInFluid(Vec3 input) {
        boolean isFalling = this.getDeltaMovement().y <= 0.0;
        double oldY = this.getY();
        double baseGravity = this.getEffectiveGravity();
        if (this.isInWater()) {
            this.travelInWater(input, baseGravity, isFalling, oldY);
            this.floatInWaterWhileRidden();
        } else {
            this.travelInLava(input, baseGravity, isFalling, oldY);
        }
    }

    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        float slowDown = this.isSprinting() ? 0.9f : this.getWaterSlowDown();
        float speed = 0.02f;
        float waterWalker = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (!this.onGround()) {
            waterWalker *= 0.5f;
        }
        if (waterWalker > 0.0f) {
            slowDown += (0.54600006f - slowDown) * waterWalker;
            speed += (this.getSpeed() - speed) * waterWalker;
        }
        if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
            slowDown = 0.96f;
        }
        this.moveRelative(speed, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 movement = this.getDeltaMovement();
        if (this.horizontalCollision && this.onClimbable()) {
            movement = new Vec3(movement.x, 0.2, movement.z);
        }
        movement = movement.multiply(slowDown, 0.8f, slowDown);
        this.setDeltaMovement(this.getFluidFallingAdjustedMovement(baseGravity, isFalling, movement));
        this.jumpOutOfFluid(oldY);
    }

    protected boolean isInShallowFluid(TagKey<Fluid> fluidTag) {
        return this.getFluidHeight(fluidTag) <= this.getFluidJumpThreshold();
    }

    private void travelInLava(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(0.02f, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.isInShallowFluid(FluidTags.LAVA)) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8f, 0.5));
            Vec3 movement = this.getFluidFallingAdjustedMovement(baseGravity, isFalling, this.getDeltaMovement());
            this.setDeltaMovement(movement);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }
        if (baseGravity != 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -baseGravity / 4.0, 0.0));
        }
        this.jumpOutOfFluid(oldY);
    }

    private void jumpOutOfFluid(double oldY) {
        Vec3 movement = this.getDeltaMovement();
        if (this.horizontalCollision && this.isFree(movement.x, movement.y + (double)0.6f - this.getY() + oldY, movement.z)) {
            this.setDeltaMovement(movement.x, 0.3f, movement.z);
        }
    }

    private void floatInWaterWhileRidden() {
        boolean canEntityFloatInWater = this.is(EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN);
        if (canEntityFloatInWater && this.isVehicle() && this.getFluidHeight(FluidTags.WATER) > this.getFluidJumpThreshold()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04f, 0.0));
        }
    }

    private void travelFallFlying(Vec3 input) {
        if (this.onClimbable()) {
            this.travelInAir(input);
            this.stopFallFlying();
            return;
        }
        Vec3 lastMovement = this.getDeltaMovement();
        double lastSpeed = lastMovement.horizontalDistance();
        this.setDeltaMovement(this.updateFallFlyingMovement(lastMovement));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.level().isClientSide()) {
            double newSpeed = this.getDeltaMovement().horizontalDistance();
            this.handleFallFlyingCollisions(lastSpeed, newSpeed);
        }
    }

    public void stopFallFlying() {
        this.setSharedFlag(7, true);
        this.setSharedFlag(7, false);
    }

    private Vec3 updateFallFlyingMovement(Vec3 movement) {
        double convert;
        Vec3 lookAngle = this.getLookAngle();
        float leanAngle = this.getXRot() * ((float)Math.PI / 180);
        double lookHorLength = Math.sqrt(lookAngle.x * lookAngle.x + lookAngle.z * lookAngle.z);
        double moveHorLength = movement.horizontalDistance();
        double gravity = this.getEffectiveGravity();
        double liftForce = Mth.square(Math.cos(leanAngle));
        movement = movement.add(0.0, gravity * (-1.0 + liftForce * 0.75), 0.0);
        if (movement.y < 0.0 && lookHorLength > 0.0) {
            convert = movement.y * -0.1 * liftForce;
            movement = movement.add(lookAngle.x * convert / lookHorLength, convert, lookAngle.z * convert / lookHorLength);
        }
        if (leanAngle < 0.0f && lookHorLength > 0.0) {
            convert = moveHorLength * (double)(-Mth.sin(leanAngle)) * 0.04;
            movement = movement.add(-lookAngle.x * convert / lookHorLength, convert * 3.2, -lookAngle.z * convert / lookHorLength);
        }
        if (lookHorLength > 0.0) {
            movement = movement.add((lookAngle.x / lookHorLength * moveHorLength - movement.x) * 0.1, 0.0, (lookAngle.z / lookHorLength * moveHorLength - movement.z) * 0.1);
        }
        return movement.multiply(0.99f, 0.98f, 0.99f);
    }

    private void handleFallFlyingCollisions(double moveHorLength, double newMoveHorLength) {
        double diff;
        float dmg;
        if (this.horizontalCollision && (dmg = (float)((diff = moveHorLength - newMoveHorLength) * 10.0 - 3.0)) > 0.0f) {
            this.playSound(this.getFallDamageSound((int)dmg), 1.0f, 1.0f);
            this.hurt(this.damageSources().flyIntoWall(), dmg);
        }
    }

    private void travelRidden(Player controller, Vec3 selfInput) {
        Vec3 riddenInput = this.getRiddenInput(controller, selfInput);
        this.tickRidden(controller, riddenInput);
        if (this.canSimulateMovement()) {
            this.setSpeed(this.getRiddenSpeed(controller));
            this.travel(riddenInput);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    protected void tickRidden(Player controller, Vec3 riddenInput) {
    }

    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        return selfInput;
    }

    protected float getRiddenSpeed(Player controller) {
        return this.getSpeed();
    }

    public void calculateEntityAnimation(boolean useY) {
        float distance = (float)Mth.length(this.getX() - this.xo, useY ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
        if (this.isPassenger() || !this.isAlive()) {
            this.walkAnimation.stop();
        } else {
            this.updateWalkAnimation(distance);
        }
    }

    protected void updateWalkAnimation(float distance) {
        float targetSpeed = Math.min(distance * 4.0f, 1.0f);
        this.walkAnimation.update(targetSpeed, 0.4f, this.isBaby() ? 3.0f : 1.0f);
    }

    private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 input, float friction) {
        this.moveRelative(this.getFrictionInfluencedSpeed(friction), input);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 movement = this.getDeltaMovement();
        if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
            movement = new Vec3(movement.x, 0.2, movement.z);
        }
        return movement;
    }

    public Vec3 getFluidFallingAdjustedMovement(double baseGravity, boolean isFalling, Vec3 movement) {
        if (baseGravity != 0.0 && !this.isSprinting()) {
            double yd = isFalling && Math.abs(movement.y - 0.005) >= 0.003 && Math.abs(movement.y - baseGravity / 16.0) < 0.003 ? -0.003 : movement.y - baseGravity / 16.0;
            return new Vec3(movement.x, yd, movement.z);
        }
        return movement;
    }

    private Vec3 handleOnClimbable(Vec3 delta) {
        if (this.onClimbable()) {
            this.resetFallDistance();
            float max = 0.15f;
            double xd = Mth.clamp(delta.x, (double)-0.15f, (double)0.15f);
            double zd = Mth.clamp(delta.z, (double)-0.15f, (double)0.15f);
            double yd = Math.max(delta.y, (double)-0.15f);
            if (yd < 0.0 && !this.getInBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
                yd = 0.0;
            }
            delta = new Vec3(xd, yd, zd);
        }
        return delta;
    }

    private float getFrictionInfluencedSpeed(float blockFriction) {
        if (this.onGround()) {
            if ((double)blockFriction > 0.6) {
                return this.getSpeed() * (0.21600002f / (blockFriction * blockFriction * blockFriction));
            }
            return this.getSpeed();
        }
        return this.getFlyingSpeed();
    }

    protected float getFlyingSpeed() {
        return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1f : 0.02f;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.setLastHurtMob(target);
        return false;
    }

    public void causeExtraKnockback(Entity target, float knockback, Vec3 oldMovement, DamageSource damageSource, float damage, boolean comesFromEffect) {
        if (knockback > 0.0f && target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity)target;
            livingTarget.knockback(knockback, Mth.sin(this.getYRot() * ((float)Math.PI / 180)), -Mth.cos(this.getYRot() * ((float)Math.PI / 180)), damageSource, damage, comesFromEffect);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
        }
    }

    protected void playAttackSound() {
    }

    @Override
    public void tick() {
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level().isClientSide()) {
            int stingerCount;
            int arrowCount = this.getArrowCount();
            if (arrowCount > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - arrowCount);
                }
                --this.removeArrowTime;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(arrowCount - 1);
                }
            }
            if ((stingerCount = this.getStingerCount()) > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - stingerCount);
                }
                --this.removeStingerTime;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(stingerCount - 1);
                }
            }
            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }
            if (!(!this.isSleeping() || this.canInteractWithLevel() && this.checkBedExists())) {
                this.stopSleeping();
            }
        }
        if (!this.isRemoved()) {
            this.aiStep();
        }
        double xd = this.getX() - this.xo;
        double zd = this.getZ() - this.zo;
        float sideDist = (float)(xd * xd + zd * zd);
        float yBodyRotT = this.yBodyRot;
        if (sideDist > 0.0025000002f) {
            float walkDirection = (float)Mth.atan2(zd, xd) * 57.295776f - 90.0f;
            float diffBetweenDirectionAndFacing = Mth.abs(Mth.wrapDegrees(this.getYRot()) - walkDirection);
            yBodyRotT = 95.0f < diffBetweenDirectionAndFacing && diffBetweenDirectionAndFacing < 265.0f ? walkDirection - 180.0f : walkDirection;
        }
        if (this.attackAnim > 0.0f) {
            yBodyRotT = this.getYRot();
        }
        ProfilerFiller profiler = Profiler.get();
        profiler.push("headTurn");
        this.tickHeadTurn(yBodyRotT);
        profiler.pop();
        profiler.push("rangeChecks");
        while (this.getYRot() - this.yRotO < -180.0f) {
            this.yRotO -= 360.0f;
        }
        while (this.getYRot() - this.yRotO >= 180.0f) {
            this.yRotO += 360.0f;
        }
        while (this.yBodyRot - this.yBodyRotO < -180.0f) {
            this.yBodyRotO -= 360.0f;
        }
        while (this.yBodyRot - this.yBodyRotO >= 180.0f) {
            this.yBodyRotO += 360.0f;
        }
        while (this.getXRot() - this.xRotO < -180.0f) {
            this.xRotO -= 360.0f;
        }
        while (this.getXRot() - this.xRotO >= 180.0f) {
            this.xRotO += 360.0f;
        }
        while (this.yHeadRot - this.yHeadRotO < -180.0f) {
            this.yHeadRotO -= 360.0f;
        }
        while (this.yHeadRot - this.yHeadRotO >= 180.0f) {
            this.yHeadRotO += 360.0f;
        }
        profiler.pop();
        this.fallFlyTicks = this.isFallFlying() ? ++this.fallFlyTicks : 0;
        if (this.isSleeping()) {
            this.setXRot(0.0f);
        }
        this.refreshDirtyAttributes();
        this.elytraAnimationState.tick();
        if (this.currentImpulseContextResetGraceTime > 0) {
            --this.currentImpulseContextResetGraceTime;
        }
    }

    public boolean wasRecentlyStabbed(Entity target, int allowedTime) {
        if (this.recentKineticEnemies == null) {
            return false;
        }
        if (this.recentKineticEnemies.containsKey((Object)target)) {
            return this.level().getGameTime() - this.recentKineticEnemies.getLong((Object)target) < (long)allowedTime;
        }
        return false;
    }

    public void rememberStabbedEntity(Entity target) {
        if (this.recentKineticEnemies != null) {
            this.recentKineticEnemies.put((Object)target, this.level().getGameTime());
        }
    }

    public int stabbedEntities(Predicate<Entity> filter) {
        if (this.recentKineticEnemies == null) {
            return 0;
        }
        return (int)this.recentKineticEnemies.keySet().stream().filter(filter).count();
    }

    public boolean stabAttack(EquipmentSlot weaponSlot, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        ItemStack weaponItem = this.getItemBySlot(weaponSlot);
        DamageSource damageSource = weaponItem.getDamageSource(this);
        float postEnchantmentDamage = EnchantmentHelper.modifyDamage(serverLevel, weaponItem, target, damageSource, baseDamage);
        Vec3 oldMovement = target.getDeltaMovement();
        boolean affected = dealsKnockback;
        boolean dealtDamage = dealsDamage && target.hurtServer(serverLevel, damageSource, postEnchantmentDamage);
        affected |= dealtDamage;
        if (dealsKnockback) {
            this.causeExtraKnockback(target, 0.4f, oldMovement, damageSource, postEnchantmentDamage, false);
            this.causeExtraKnockback(target, this.getKnockback(target, damageSource), oldMovement, damageSource, postEnchantmentDamage, true);
        }
        if (dismounts && target.isPassenger()) {
            affected = true;
            target.stopRiding();
        }
        if (target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity)target;
            weaponItem.hurtEnemy(livingTarget, this);
        }
        if (dealtDamage) {
            EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
        }
        if (!affected) {
            return false;
        }
        this.setLastHurtMob(target);
        this.playAttackSound();
        return true;
    }

    public void onAttack() {
    }

    private void detectEquipmentUpdates() {
        Map<EquipmentSlot, ItemStack> changedItems = this.collectEquipmentChanges(this.lastEquipmentItems);
        if (changedItems != null) {
            this.handleHandSwap(changedItems);
            if (!changedItems.isEmpty()) {
                this.handleEquipmentChanges(changedItems);
            }
        }
    }

    protected @Nullable Map<EquipmentSlot, ItemStack> collectEquipmentChanges(Map<EquipmentSlot, ItemStack> lastEquipmentItems) {
        ItemStack current;
        Map changedItems = null;
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            ItemStack previous = lastEquipmentItems.get(equipmentSlot);
            if (!this.equipmentHasChanged(previous, current = this.getItemBySlot(equipmentSlot))) continue;
            if (changedItems == null) {
                changedItems = Maps.newEnumMap(EquipmentSlot.class);
            }
            changedItems.put(equipmentSlot, current);
            AttributeMap attributes = this.getAttributes();
            if (previous.isEmpty()) continue;
            this.stopLocationBasedEffects(previous, equipmentSlot, attributes);
        }
        if (changedItems != null) {
            for (Map.Entry entry : changedItems.entrySet()) {
                EquipmentSlot slot = (EquipmentSlot)entry.getKey();
                current = (ItemStack)entry.getValue();
                if (current.isEmpty() || current.isBroken()) continue;
                current.forEachModifier(slot, (attribute, modifier) -> {
                    AttributeInstance instance = this.attributes.getInstance((Holder<Attribute>)attribute);
                    if (instance != null) {
                        instance.removeModifier(modifier.id());
                        instance.addTransientModifier((AttributeModifier)modifier);
                    }
                });
                Level level = this.level();
                if (!(level instanceof ServerLevel)) continue;
                ServerLevel serverLevel = (ServerLevel)level;
                EnchantmentHelper.runLocationChangedEffects(serverLevel, current, this, slot);
            }
        }
        return changedItems;
    }

    public boolean equipmentHasChanged(ItemStack previous, ItemStack current) {
        return !ItemStack.matches(current, previous);
    }

    private void handleHandSwap(Map<EquipmentSlot, ItemStack> changedItems) {
        ItemStack currentMainHand = changedItems.get(EquipmentSlot.MAINHAND);
        ItemStack currentOffHand = changedItems.get(EquipmentSlot.OFFHAND);
        if (currentMainHand != null && currentOffHand != null && ItemStack.matches(currentMainHand, this.lastEquipmentItems.get(EquipmentSlot.OFFHAND)) && ItemStack.matches(currentOffHand, this.lastEquipmentItems.get(EquipmentSlot.MAINHAND))) {
            ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundEntityEventPacket(this, 55));
            changedItems.remove(EquipmentSlot.MAINHAND);
            changedItems.remove(EquipmentSlot.OFFHAND);
            this.lastEquipmentItems.put(EquipmentSlot.MAINHAND, currentMainHand.copy());
            this.lastEquipmentItems.put(EquipmentSlot.OFFHAND, currentOffHand.copy());
        }
    }

    private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> changedItems) {
        ArrayList itemsToSend = Lists.newArrayListWithCapacity((int)changedItems.size());
        changedItems.forEach((slot, newItem) -> {
            ItemStack newItemToStore = newItem.copy();
            itemsToSend.add(Pair.of((Object)slot, (Object)newItemToStore));
            this.lastEquipmentItems.put((EquipmentSlot)slot, newItemToStore);
        });
        ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(this.getId(), itemsToSend));
    }

    protected void tickHeadTurn(float yBodyRotT) {
        float yBodyRotD = Mth.wrapDegrees(yBodyRotT - this.yBodyRot);
        this.yBodyRot += yBodyRotD * 0.3f;
        float headDiff = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
        float maxHeadRotation = this.getMaxHeadRotationRelativeToBody();
        if (Math.abs(headDiff) > maxHeadRotation) {
            this.yBodyRot += headDiff - (float)Mth.sign(headDiff) * maxHeadRotation;
        }
    }

    protected float getMaxHeadRotationRelativeToBody() {
        return 50.0f;
    }

    /*
     * Unable to fully structure code
     */
    public void aiStep() {
        if (this.noJumpDelay > 0) {
            --this.noJumpDelay;
        }
        if (this.isInterpolating()) {
            this.getInterpolation().interpolate();
        } else if (!this.canSimulateMovement()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            --this.lerpHeadSteps;
        }
        this.equipment.tick(this);
        movement = this.getDeltaMovement();
        dx = movement.x;
        dy = movement.y;
        dz = movement.z;
        if (this.is(EntityTypes.PLAYER)) {
            if (movement.horizontalDistanceSqr() < 9.0E-6) {
                dx = 0.0;
                dz = 0.0;
            }
        } else {
            if (Math.abs(movement.x) < 0.003) {
                dx = 0.0;
            }
            if (Math.abs(movement.z) < 0.003) {
                dz = 0.0;
            }
        }
        if (Math.abs(movement.y) < 0.003) {
            dy = 0.0;
        }
        this.setDeltaMovement(dx, dy, dz);
        profiler = Profiler.get();
        profiler.push("ai");
        this.applyInput();
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0f;
            this.zza = 0.0f;
        } else if (this.isEffectiveAi() && !this.level().isClientSide()) {
            profiler.push("newAi");
            this.serverAiStep();
            profiler.pop();
        }
        profiler.pop();
        profiler.push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            fluidHeight = this.isInLava() != false ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            inWaterAndHasFluidHeight = this.isInWater() != false && fluidHeight > 0.0;
            fluidJumpThreshold = this.getFluidJumpThreshold();
            if (inWaterAndHasFluidHeight && (!this.onGround() || fluidHeight > fluidJumpThreshold)) {
                this.jumpInLiquid(FluidTags.WATER);
            } else if (!(!this.isInLava() || this.onGround() && this.isInShallowFluid(FluidTags.LAVA))) {
                this.jumpInLiquid(FluidTags.LAVA);
            } else if ((this.onGround() || inWaterAndHasFluidHeight && fluidHeight <= fluidJumpThreshold) && this.noJumpDelay == 0) {
                this.jumpFromGround();
                this.noJumpDelay = 10;
            }
        } else {
            this.noJumpDelay = 0;
        }
        profiler.pop();
        profiler.push("travel");
        if (this.isFallFlying()) {
            this.updateFallFlying();
        }
        beforeTravelBox = this.getBoundingBox();
        input = new Vec3(this.xxa, this.yya, this.zza);
        if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
        }
        if (!((var12_13 = this.getControllingPassenger()) instanceof Player)) ** GOTO lbl-1000
        controller = (Player)var12_13;
        if (this.isAlive()) {
            this.travelRidden(controller, input);
        } else if (this.canSimulateMovement() && this.isEffectiveAi()) {
            this.travel(input);
        }
        if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
            this.applyEffectsFromBlocks();
        }
        if (this.level().isClientSide()) {
            this.calculateEntityAnimation(this.omnidirectionalAirMover());
        }
        profiler.pop();
        var12_13 = this.level();
        if (var12_13 instanceof ServerLevel) {
            serverLevel = (ServerLevel)var12_13;
            profiler.push("freezing");
            if (!this.isInPowderSnow || !this.canFreeze()) {
                this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));
            }
            this.removeFrost();
            this.tryAddFrost();
            if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
                this.hurtServer(serverLevel, this.damageSources().freeze(), 1.0f);
            }
            profiler.pop();
        }
        profiler.push("push");
        if (this.autoSpinAttackTicks > 0) {
            --this.autoSpinAttackTicks;
            this.checkAutoSpinAttack(beforeTravelBox, this.getBoundingBox());
        }
        this.pushEntities();
        profiler.pop();
        var12_13 = this.level();
        if (var12_13 instanceof ServerLevel) {
            serverLevel = (ServerLevel)var12_13;
            if (this.isSensitiveToWater() && this.isInWaterOrRain()) {
                this.hurtServer(serverLevel, this.damageSources().drown(), 1.0f);
            }
        }
    }

    protected void applyInput() {
        this.xxa *= 0.98f;
        this.zza *= 0.98f;
    }

    public boolean isSensitiveToWater() {
        return false;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    protected void updateFallFlying() {
        this.checkFallDistanceAccumulation();
        if (!this.level().isClientSide()) {
            if (!this.canGlide()) {
                this.setSharedFlag(7, false);
                return;
            }
            int checkFallFlyTicks = this.fallFlyTicks + 1;
            if (checkFallFlyTicks % 10 == 0) {
                int freeFallInterval = checkFallFlyTicks / 10;
                if (freeFallInterval % 2 == 0) {
                    List<EquipmentSlot> slotsWithGliders = EquipmentSlot.VALUES.stream().filter(slot -> LivingEntity.canGlideUsing(this.getItemBySlot((EquipmentSlot)slot), slot)).toList();
                    EquipmentSlot slotToDamage = Util.getRandom(slotsWithGliders, this.random);
                    this.getItemBySlot(slotToDamage).hurtAndBreak(1, this, slotToDamage);
                }
                this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
    }

    protected boolean canGlide() {
        if (this.onGround() || this.isPassenger() || this.hasEffect(MobEffects.LEVITATION)) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (!LivingEntity.canGlideUsing(this.getItemBySlot(slot), slot)) continue;
            return true;
        }
        return false;
    }

    protected void serverAiStep() {
    }

    protected void pushEntities() {
        ServerLevel serverLevel;
        int maxCramming;
        List<Entity> pushableEntities = this.level().getPushableEntities(this, this.getBoundingBox());
        if (pushableEntities.isEmpty()) {
            return;
        }
        Level level = this.level();
        if (level instanceof ServerLevel && (maxCramming = (serverLevel = (ServerLevel)level).getGameRules().get(GameRules.MAX_ENTITY_CRAMMING).intValue()) > 0 && pushableEntities.size() > maxCramming - 1 && this.random.nextInt(4) == 0) {
            int count = 0;
            for (Entity entity : pushableEntities) {
                if (entity.isPassenger()) continue;
                ++count;
            }
            if (count > maxCramming - 1) {
                this.hurtServer(serverLevel, this.damageSources().cramming(), 6.0f);
            }
        }
        for (Entity entity : pushableEntities) {
            this.doPush(entity);
        }
    }

    protected void checkAutoSpinAttack(AABB old, AABB current) {
        AABB minmax = old.minmax(current);
        List<Entity> entities = this.level().getEntities(this, minmax);
        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) continue;
                LivingEntity livingEntity = (LivingEntity)entity;
                this.doAutoAttackOnTouch(livingEntity);
                this.autoSpinAttackTicks = 0;
                this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
                break;
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }
        if (!this.level().isClientSide() && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
            this.autoSpinAttackDmg = 0.0f;
            this.autoSpinAttackItemStack = null;
        }
    }

    protected void doPush(Entity entity) {
        entity.push(this);
    }

    protected void doAutoAttackOnTouch(LivingEntity entity) {
    }

    public boolean isAutoSpinAttack() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity oldVehicle = this.getVehicle();
        super.stopRiding();
        if (oldVehicle != null && oldVehicle != this.getVehicle() && !this.level().isClientSide()) {
            this.dismountVehicle(oldVehicle);
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.resetFallDistance();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void lerpHeadTo(float yRot, int steps) {
        this.lerpYHeadRot = yRot;
        this.lerpHeadSteps = steps;
    }

    public void setJumping(boolean jump) {
        this.jumping = jump;
    }

    public void onItemPickup(ItemEntity entity) {
        Entity thrower = entity.getOwner();
        if (thrower instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)thrower;
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger(serverPlayer, entity.getItem(), this);
        }
    }

    public void take(Entity entity, int orgCount) {
        if (!entity.isRemoved() && !this.level().isClientSide() && (entity instanceof ItemEntity || entity instanceof AbstractArrow || entity instanceof ExperienceOrb)) {
            ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(entity, new ClientboundTakeItemEntityPacket(entity.getId(), this.getId(), orgCount));
        }
    }

    public boolean hasLineOfSight(Entity target) {
        return this.hasLineOfSight(target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target.getEyeY());
    }

    public boolean hasLineOfSight(Entity target, ClipContext.Block blockCollidingContext, ClipContext.Fluid fluidCollidingContext, double eyeHeight) {
        if (target.level() != this.level()) {
            return false;
        }
        Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 to = new Vec3(target.getX(), eyeHeight, target.getZ());
        if (to.distanceTo(from) > 128.0) {
            return false;
        }
        return this.level().clip(new ClipContext(from, to, blockCollidingContext, fluidCollidingContext, this)).getType() == HitResult.Type.MISS;
    }

    @Override
    public float getViewYRot(float a) {
        if (a == 1.0f) {
            return this.yHeadRot;
        }
        return Mth.rotLerp(a, this.yHeadRotO, this.yHeadRot);
    }

    public float getAttackAnim(float a) {
        float diff = this.attackAnim - this.oAttackAnim;
        if (diff < 0.0f) {
            diff += 1.0f;
        }
        return this.oAttackAnim + diff * a;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && !this.isSpectator() && !this.onClimbable();
    }

    @Override
    public float getYHeadRot() {
        return this.yHeadRot;
    }

    @Override
    public void setYHeadRot(float yHeadRot) {
        this.yHeadRot = yHeadRot;
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
        this.yBodyRot = yBodyRot;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portalArea) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portalArea));
    }

    public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 offsets) {
        return new Vec3(offsets.x, offsets.y, 0.0);
    }

    public float getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public final void setAbsorptionAmount(float absorptionAmount) {
        this.internalSetAbsorptionAmount(Mth.clamp(absorptionAmount, 0.0f, this.getMaxAbsorption()));
    }

    protected void internalSetAbsorptionAmount(float absorptionAmount) {
        this.absorptionAmount = absorptionAmount;
    }

    public void onEnterCombat() {
    }

    public void onLeaveCombat() {
    }

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract HumanoidArm getMainArm();

    public boolean isUsingItem() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public InteractionHand getUsedItemHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            if (ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }
    }

    private @Nullable ItemEntity createItemStackToDrop(ItemStack itemStack, boolean randomly, boolean thrownFromHand) {
        if (itemStack.isEmpty()) {
            return null;
        }
        double yHandPos = this.getEyeY() - (double)0.3f;
        ItemEntity entity = new ItemEntity(this.level(), this.getX(), yHandPos, this.getZ(), itemStack);
        entity.setPickUpDelay(40);
        if (thrownFromHand) {
            entity.setThrower(this);
        }
        if (randomly) {
            float pow = this.random.nextFloat() * 0.5f;
            float dir = this.random.nextFloat() * ((float)Math.PI * 2);
            entity.setDeltaMovement(-Mth.sin(dir) * pow, 0.2f, Mth.cos(dir) * pow);
        } else {
            float pow = 0.3f;
            float sinX = Mth.sin(this.getXRot() * ((float)Math.PI / 180));
            float cosX = Mth.cos(this.getXRot() * ((float)Math.PI / 180));
            float sinY = Mth.sin(this.getYRot() * ((float)Math.PI / 180));
            float cosY = Mth.cos(this.getYRot() * ((float)Math.PI / 180));
            float dir = this.random.nextFloat() * ((float)Math.PI * 2);
            float pow2 = 0.02f * this.random.nextFloat();
            entity.setDeltaMovement((double)(-sinY * cosX * 0.3f) + Math.cos(dir) * (double)pow2, -sinX * 0.3f + 0.1f + (this.random.nextFloat() - this.random.nextFloat()) * 0.1f, (double)(cosY * cosX * 0.3f) + Math.sin(dir) * (double)pow2);
        }
        return entity;
    }

    protected void updateUsingItem(ItemStack useItem) {
        useItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
        if (--this.useItemRemaining == 0 && !this.level().isClientSide() && !useItem.useOnRelease()) {
            this.completeUsingItem();
        }
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        this.swimAmount = this.isVisuallySwimming() ? Math.min(1.0f, this.swimAmount + 0.09f) : Math.max(0.0f, this.swimAmount - 0.09f);
    }

    protected void setLivingEntityFlag(int flag, boolean value) {
        int currentFlags = this.entityData.get(DATA_LIVING_ENTITY_FLAGS).byteValue();
        currentFlags = value ? (currentFlags |= flag) : (currentFlags &= ~flag);
        this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)currentFlags);
    }

    public void startUsingItem(InteractionHand hand) {
        ItemStack itemStack = this.getItemInHand(hand);
        if (itemStack.isEmpty() || this.isUsingItem()) {
            return;
        }
        this.useItem = itemStack;
        this.useItemRemaining = itemStack.getUseDuration(this);
        if (!this.level().isClientSide()) {
            this.setLivingEntityFlag(1, true);
            this.setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
            this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_START);
            if (this.useItem.has(DataComponents.KINETIC_WEAPON)) {
                this.recentKineticEnemies = new Object2LongOpenHashMap();
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SLEEPING_POS_ID.equals(accessor)) {
            if (this.level().isClientSide()) {
                this.getSleepingPos().ifPresent(this::setPosToBed);
            }
        } else if (DATA_LIVING_ENTITY_FLAGS.equals(accessor) && this.level().isClientSide()) {
            if (this.isUsingItem() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration(this);
                }
            } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 pos) {
        super.lookAt(anchor, pos);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot = this.yHeadRot;
    }

    @Override
    public float getPreciseBodyRotation(float partial) {
        return Mth.lerp(partial, this.yBodyRotO, this.yBodyRot);
    }

    public void spawnItemParticles(ItemStack itemStack, int count) {
        if (itemStack.isEmpty()) {
            return;
        }
        ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(itemStack));
        for (int i = 0; i < count; ++i) {
            Vec3 d = new Vec3(((double)this.random.nextFloat() - 0.5) * 0.1, (double)this.random.nextFloat() * 0.1 + 0.1, 0.0);
            d = d.xRot(-this.getXRot() * ((float)Math.PI / 180));
            d = d.yRot(-this.getYRot() * ((float)Math.PI / 180));
            double y1 = (double)(-this.random.nextFloat()) * 0.6 - 0.3;
            Vec3 p = new Vec3(((double)this.random.nextFloat() - 0.5) * 0.3, y1, 0.6);
            p = p.xRot(-this.getXRot() * ((float)Math.PI / 180));
            p = p.yRot(-this.getYRot() * ((float)Math.PI / 180));
            p = p.add(this.getX(), this.getEyeY(), this.getZ());
            this.level().addParticle(breakParticle, p.x, p.y, p.z, d.x, d.y + 0.05, d.z);
        }
    }

    protected void completeUsingItem() {
        if (this.level().isClientSide() && !this.isUsingItem()) {
            return;
        }
        InteractionHand hand = this.getUsedItemHand();
        if (!this.useItem.equals(this.getItemInHand(hand))) {
            this.releaseUsingItem();
            return;
        }
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
            ItemStack result = this.useItem.finishUsingItem(this.level(), this);
            if (result != this.useItem) {
                this.setItemInHand(hand, result);
            }
            this.stopUsingItem();
        }
    }

    public void handleExtraItemsCreatedOnUse(ItemStack extraCreatedRemainder) {
    }

    public ItemStack getUseItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        if (this.isUsingItem()) {
            return this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks();
        }
        return 0;
    }

    public float getTicksUsingItem(float partialTicks) {
        if (!this.isUsingItem()) {
            return 0.0f;
        }
        return (float)this.getTicksUsingItem() + partialTicks;
    }

    public void releaseUsingItem() {
        ItemStack itemInUsedHand = this.getItemInHand(this.getUsedItemHand());
        if (!this.useItem.isEmpty() && ItemStack.isSameItem(itemInUsedHand, this.useItem)) {
            this.useItem = itemInUsedHand;
            this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }
        this.stopUsingItem();
    }

    public void stopUsingItem() {
        if (!this.level().isClientSide()) {
            boolean wasUsingItem = this.isUsingItem();
            this.recentKineticEnemies = null;
            this.setLivingEntityFlag(1, false);
            if (wasUsingItem) {
                this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
            }
        }
        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        return this.getItemBlockingWith() != null;
    }

    public @Nullable ItemStack getItemBlockingWith() {
        int elapsedTicks;
        if (!this.isUsingItem()) {
            return null;
        }
        BlocksAttacks blocksAttacks = this.useItem.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks != null && (elapsedTicks = this.useItem.getItem().getUseDuration(this.useItem, this) - this.useItemRemaining) >= blocksAttacks.blockDelayTicks()) {
            return this.useItem;
        }
        return null;
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isShiftKeyDown();
    }

    public boolean isFallFlying() {
        return this.getSharedFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double xx, double yy, double zz, boolean showParticles) {
        LivingEntity livingEntity;
        double xo = this.getX();
        double yo = this.getY();
        double zo = this.getZ();
        double y = yy;
        boolean ok = false;
        BlockPos pos = BlockPos.containing(xx, y, zz);
        Level level = this.level();
        if (level.hasChunkAt(pos)) {
            boolean landed = false;
            while (!landed && pos.getY() > level.getMinY()) {
                BlockPos below = pos.below();
                BlockState state = level.getBlockState(below);
                if (state.blocksMotion()) {
                    landed = true;
                    continue;
                }
                y -= 1.0;
                pos = below;
            }
            if (landed) {
                this.teleportTo(xx, y, zz);
                if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
                    ok = true;
                }
            }
        }
        if (!ok) {
            this.teleportTo(xo, yo, zo);
            return false;
        }
        if (showParticles) {
            level.broadcastEntityEvent(this, (byte)46);
        }
        if ((livingEntity = this) instanceof PathfinderMob) {
            PathfinderMob pathfinderMob = (PathfinderMob)livingEntity;
            pathfinderMob.getNavigation().stop();
        }
        return true;
    }

    public boolean isAffectedByPotions() {
        return !this.isDeadOrDying();
    }

    public boolean attackable() {
        return true;
    }

    public void setRecordPlayingNearby(BlockPos jukebox, boolean isPlaying) {
    }

    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public final EntityDimensions getDimensions(Pose pose) {
        return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pose).scale(this.getScale());
    }

    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getType().getDimensions().scale(this.getAgeScale());
    }

    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of((Object)Pose.STANDING);
    }

    public AABB getLocalBoundsForPose(Pose pose) {
        EntityDimensions dimensions = this.getDimensions(pose);
        return new AABB(-dimensions.width() / 2.0f, 0.0, -dimensions.width() / 2.0f, dimensions.width() / 2.0f, dimensions.height(), dimensions.width() / 2.0f);
    }

    protected boolean wouldNotSuffocateAtTargetPose(Pose pose) {
        AABB targetBB = this.getDimensions(pose).makeBoundingBox(this.position());
        return this.level().noBlockCollision(this, targetBB);
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return super.canUsePortal(ignorePassenger) && !this.isSleeping();
    }

    public Optional<BlockPos> getSleepingPos() {
        return this.entityData.get(SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPos bedPosition) {
        this.entityData.set(SLEEPING_POS_ID, Optional.of(bedPosition));
    }

    public void clearSleepingPos() {
        this.entityData.set(SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    public void startSleeping(BlockPos bedPosition) {
        BlockState blockState;
        if (this.isPassenger()) {
            this.stopRiding();
        }
        if ((blockState = this.level().getBlockState(bedPosition)).getBlock() instanceof BedBlock) {
            this.level().setBlock(bedPosition, (BlockState)blockState.setValue(BedBlock.OCCUPIED, true), 3);
        }
        this.setPose(Pose.SLEEPING);
        this.setPosToBed(bedPosition);
        this.setSleepingPos(bedPosition);
        this.setDeltaMovement(Vec3.ZERO);
        this.needsSync = true;
    }

    private void setPosToBed(BlockPos bedPosition) {
        this.setPos((double)bedPosition.getX() + 0.5, (double)bedPosition.getY() + 0.6875, (double)bedPosition.getZ() + 0.5);
    }

    private boolean checkBedExists() {
        return this.getSleepingPos().map(bedPosition -> this.level().getBlockState((BlockPos)bedPosition).getBlock() instanceof BedBlock).orElse(false);
    }

    public void stopSleeping() {
        this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(bedPosition -> {
            BlockState state = this.level().getBlockState((BlockPos)bedPosition);
            if (state.getBlock() instanceof BedBlock) {
                Direction facing = (Direction)state.getValue(BedBlock.FACING);
                this.level().setBlock((BlockPos)bedPosition, (BlockState)state.setValue(BedBlock.OCCUPIED, false), 3);
                Vec3 standUp = BedBlock.findStandUpPosition(this.getType(), this.level(), bedPosition, facing, this.getYRot()).orElseGet(() -> {
                    BlockPos above = bedPosition.above();
                    return new Vec3((double)above.getX() + 0.5, (double)above.getY() + 0.1, (double)above.getZ() + 0.5);
                });
                Vec3 lookDirection = Vec3.atBottomCenterOf(bedPosition).subtract(standUp).normalize();
                float yaw = (float)Mth.wrapDegrees(Mth.atan2(lookDirection.z, lookDirection.x) * 57.2957763671875 - 90.0);
                this.setPos(standUp.x, standUp.y, standUp.z);
                this.setYRot(yaw);
                this.setXRot(0.0f);
            }
        });
        Vec3 pos = this.position();
        this.setPose(Pose.STANDING);
        this.setPos(pos.x, pos.y, pos.z);
        this.clearSleepingPos();
    }

    public @Nullable Direction getBedOrientation() {
        BlockPos bedPos = this.getSleepingPos().orElse(null);
        return bedPos != null ? BedBlock.getBedOrientation(this.level(), bedPos) : null;
    }

    @Override
    public boolean isInWall() {
        return !this.isSleeping() && super.isInWall();
    }

    public ItemStack getProjectile(ItemStack heldWeapon) {
        return ItemStack.EMPTY;
    }

    private static byte entityEventForEquipmentBreak(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            default -> throw new MatchException(null, null);
            case EquipmentSlot.MAINHAND -> 47;
            case EquipmentSlot.OFFHAND -> 48;
            case EquipmentSlot.HEAD -> 49;
            case EquipmentSlot.CHEST -> 50;
            case EquipmentSlot.FEET -> 52;
            case EquipmentSlot.LEGS -> 51;
            case EquipmentSlot.BODY -> 65;
            case EquipmentSlot.SADDLE -> 68;
        };
    }

    public void onEquippedItemBroken(Item brokenItem, EquipmentSlot inSlot) {
        this.level().broadcastEntityEvent(this, LivingEntity.entityEventForEquipmentBreak(inSlot));
        this.stopLocationBasedEffects(this.getItemBySlot(inSlot), inSlot, this.attributes);
    }

    private void stopLocationBasedEffects(ItemStack previous, EquipmentSlot inSlot, AttributeMap attributes) {
        previous.forEachModifier(inSlot, (attribute, modifier) -> {
            AttributeInstance instance = attributes.getInstance((Holder<Attribute>)attribute);
            if (instance != null) {
                instance.removeModifier((AttributeModifier)modifier);
            }
        });
        EnchantmentHelper.stopLocationBasedEffects(previous, this, inSlot);
    }

    public final boolean canEquipWithDispenser(ItemStack itemStack) {
        if (!this.isAlive() || this.isSpectator()) {
            return false;
        }
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || !equippable.dispensable()) {
            return false;
        }
        EquipmentSlot slot = equippable.slot();
        if (!this.canUseSlot(slot) || !equippable.canBeEquippedBy(this.typeHolder())) {
            return false;
        }
        return this.getItemBySlot(slot).isEmpty() && this.canDispenserEquipIntoSlot(slot);
    }

    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return true;
    }

    public EquipmentSlot getEquipmentSlotForItem(ItemStack itemStack) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && this.canUseSlot(equippable.slot())) {
            return equippable.slot();
        }
        return EquipmentSlot.MAINHAND;
    }

    public boolean isEquippableInSlot(ItemStack itemStack, EquipmentSlot slot) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return slot == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND);
        }
        return slot == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.typeHolder());
    }

    private static SlotAccess createEquipmentSlotAccess(LivingEntity entity, EquipmentSlot equipmentSlot) {
        if (equipmentSlot == EquipmentSlot.HEAD || equipmentSlot == EquipmentSlot.MAINHAND || equipmentSlot == EquipmentSlot.OFFHAND) {
            return SlotAccess.forEquipmentSlot(entity, equipmentSlot);
        }
        return SlotAccess.forEquipmentSlot(entity, equipmentSlot, stack -> stack.isEmpty() || entity.getEquipmentSlotForItem((ItemStack)stack) == equipmentSlot);
    }

    private static @Nullable EquipmentSlot getEquipmentSlot(int slot) {
        if (slot == 100 + EquipmentSlot.HEAD.getIndex()) {
            return EquipmentSlot.HEAD;
        }
        if (slot == 100 + EquipmentSlot.CHEST.getIndex()) {
            return EquipmentSlot.CHEST;
        }
        if (slot == 100 + EquipmentSlot.LEGS.getIndex()) {
            return EquipmentSlot.LEGS;
        }
        if (slot == 100 + EquipmentSlot.FEET.getIndex()) {
            return EquipmentSlot.FEET;
        }
        if (slot == 98) {
            return EquipmentSlot.MAINHAND;
        }
        if (slot == 99) {
            return EquipmentSlot.OFFHAND;
        }
        if (slot == 105) {
            return EquipmentSlot.BODY;
        }
        if (slot == 106) {
            return EquipmentSlot.SADDLE;
        }
        return null;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        EquipmentSlot equipmentSlot = LivingEntity.getEquipmentSlot(slot);
        if (equipmentSlot != null) {
            return LivingEntity.createEquipmentSlotAccess(this, equipmentSlot);
        }
        return super.getSlot(slot);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if (!this.getItemBySlot(slot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) continue;
            return false;
        }
        return super.canFreeze();
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return this.yBodyRot;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float yRot = packet.getYRot();
        float xRot = packet.getXRot();
        this.syncPacketPositionCodec(x, y, z);
        this.yBodyRot = packet.getYHeadRot();
        this.yHeadRot = packet.getYHeadRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setId(packet.getId());
        this.setUUID(packet.getUUID());
        this.absSnapTo(x, y, z, yRot, xRot);
        this.setDeltaMovement(packet.getMovement());
    }

    public float getSecondsToDisableBlocking() {
        ItemStack weaponItem = this.getWeaponItem();
        Weapon weapon = weaponItem.get(DataComponents.WEAPON);
        return weapon != null && weaponItem == this.getActiveItem() ? weapon.disableBlockingForSeconds() : 0.0f;
    }

    @Override
    public float maxUpStep() {
        float maxUpStep = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
        return this.getControllingPassenger() instanceof Player ? Math.max(maxUpStep, 1.0f) : maxUpStep;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(this.getPassengerAttachmentPoint(passenger, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
    }

    protected void lerpHeadRotationStep(int lerpHeadSteps, double targetYHeadRot) {
        this.yHeadRot = (float)Mth.rotLerp(1.0 / (double)lerpHeadSteps, (double)this.yHeadRot, targetYHeadRot);
    }

    @Override
    public void igniteForTicks(int numberOfTicks) {
        super.igniteForTicks(Mth.ceil((double)numberOfTicks * this.getAttributeValue(Attributes.BURNING_TIME)));
    }

    public boolean hasInfiniteMaterials() {
        return false;
    }

    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return this.isInvulnerableToBase(source) || EnchantmentHelper.isImmuneToDamage(level, this, source);
    }

    public static boolean canGlideUsing(ItemStack itemStack, EquipmentSlot slot) {
        if (!itemStack.has(DataComponents.GLIDER)) {
            return false;
        }
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && slot == equippable.slot() && !itemStack.nextDamageWillBreak();
    }

    @VisibleForTesting
    public int getLastHurtByPlayerMemoryTime() {
        return this.lastHurtByPlayerMemoryTime;
    }

    @Override
    public boolean isTransmittingWaypoint() {
        return this.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE) > 0.0;
    }

    @Override
    public Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer player) {
        if (this.firstTick || player == this) {
            return Optional.empty();
        }
        if (WaypointTransmitter.doesSourceIgnoreReceiver(this, player)) {
            return Optional.empty();
        }
        Waypoint.Icon icon = this.locatorBarIcon.cloneAndAssignStyle(this);
        if (WaypointTransmitter.isReallyFar(this, player)) {
            return Optional.of(new WaypointTransmitter.EntityAzimuthConnection(this, icon, player));
        }
        if (!WaypointTransmitter.isChunkVisible(this.chunkPosition(), player)) {
            return Optional.of(new WaypointTransmitter.EntityChunkConnection(this, icon, player));
        }
        return Optional.of(new WaypointTransmitter.EntityBlockConnection(this, icon, player));
    }

    @Override
    public Waypoint.Icon waypointIcon() {
        return this.locatorBarIcon;
    }

    public DamageSource createDamageSource() {
        return this.damageSources().mobAttack(this);
    }

    public record Fallsounds(SoundEvent small, SoundEvent big) {
    }
}

