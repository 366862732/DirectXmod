/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.parrot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Parrot
extends ShoulderRidingEntity {
    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Parrot.class, EntityDataSerializers.INT);
    private static final Predicate<Mob> NOT_PARROT_PREDICATE = new Predicate<Mob>(){

        @Override
        public boolean test(@Nullable Mob input) {
            return input != null && MOB_SOUND_MAP.containsKey(input.getType());
        }
    };
    private static final Map<EntityType<?>, SoundEvent> MOB_SOUND_MAP = Util.make(Maps.newHashMap(), map -> {
        map.put(EntityTypes.BLAZE, SoundEvents.PARROT_IMITATE_BLAZE);
        map.put(EntityTypes.BOGGED, SoundEvents.PARROT_IMITATE_BOGGED);
        map.put(EntityTypes.BREEZE, SoundEvents.PARROT_IMITATE_BREEZE);
        map.put(EntityTypes.CAMEL_HUSK, SoundEvents.PARROT_IMITATE_CAMEL_HUSK);
        map.put(EntityTypes.CAVE_SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
        map.put(EntityTypes.CREAKING, SoundEvents.PARROT_IMITATE_CREAKING);
        map.put(EntityTypes.CREEPER, SoundEvents.PARROT_IMITATE_CREEPER);
        map.put(EntityTypes.DROWNED, SoundEvents.PARROT_IMITATE_DROWNED);
        map.put(EntityTypes.ELDER_GUARDIAN, SoundEvents.PARROT_IMITATE_ELDER_GUARDIAN);
        map.put(EntityTypes.ENDER_DRAGON, SoundEvents.PARROT_IMITATE_ENDER_DRAGON);
        map.put(EntityTypes.ENDERMITE, SoundEvents.PARROT_IMITATE_ENDERMITE);
        map.put(EntityTypes.EVOKER, SoundEvents.PARROT_IMITATE_EVOKER);
        map.put(EntityTypes.GHAST, SoundEvents.PARROT_IMITATE_GHAST);
        map.put(EntityTypes.HAPPY_GHAST, SoundEvents.EMPTY);
        map.put(EntityTypes.GUARDIAN, SoundEvents.PARROT_IMITATE_GUARDIAN);
        map.put(EntityTypes.HOGLIN, SoundEvents.PARROT_IMITATE_HOGLIN);
        map.put(EntityTypes.HUSK, SoundEvents.PARROT_IMITATE_HUSK);
        map.put(EntityTypes.ILLUSIONER, SoundEvents.PARROT_IMITATE_ILLUSIONER);
        map.put(EntityTypes.MAGMA_CUBE, SoundEvents.PARROT_IMITATE_MAGMA_CUBE);
        map.put(EntityTypes.PARCHED, SoundEvents.PARROT_IMITATE_PARCHED);
        map.put(EntityTypes.PHANTOM, SoundEvents.PARROT_IMITATE_PHANTOM);
        map.put(EntityTypes.PIGLIN, SoundEvents.PARROT_IMITATE_PIGLIN);
        map.put(EntityTypes.PIGLIN_BRUTE, SoundEvents.PARROT_IMITATE_PIGLIN_BRUTE);
        map.put(EntityTypes.PILLAGER, SoundEvents.PARROT_IMITATE_PILLAGER);
        map.put(EntityTypes.RAVAGER, SoundEvents.PARROT_IMITATE_RAVAGER);
        map.put(EntityTypes.SHULKER, SoundEvents.PARROT_IMITATE_SHULKER);
        map.put(EntityTypes.SILVERFISH, SoundEvents.PARROT_IMITATE_SILVERFISH);
        map.put(EntityTypes.SKELETON, SoundEvents.PARROT_IMITATE_SKELETON);
        map.put(EntityTypes.SLIME, SoundEvents.PARROT_IMITATE_SLIME);
        map.put(EntityTypes.SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
        map.put(EntityTypes.STRAY, SoundEvents.PARROT_IMITATE_STRAY);
        map.put(EntityTypes.VEX, SoundEvents.PARROT_IMITATE_VEX);
        map.put(EntityTypes.VINDICATOR, SoundEvents.PARROT_IMITATE_VINDICATOR);
        map.put(EntityTypes.WARDEN, SoundEvents.PARROT_IMITATE_WARDEN);
        map.put(EntityTypes.WITCH, SoundEvents.PARROT_IMITATE_WITCH);
        map.put(EntityTypes.WITHER, SoundEvents.PARROT_IMITATE_WITHER);
        map.put(EntityTypes.WITHER_SKELETON, SoundEvents.PARROT_IMITATE_WITHER_SKELETON);
        map.put(EntityTypes.ZOGLIN, SoundEvents.PARROT_IMITATE_ZOGLIN);
        map.put(EntityTypes.ZOMBIE, SoundEvents.PARROT_IMITATE_ZOMBIE);
        map.put(EntityTypes.ZOMBIE_HORSE, SoundEvents.PARROT_IMITATE_ZOMBIE_HORSE);
        map.put(EntityTypes.ZOMBIE_NAUTILUS, SoundEvents.PARROT_IMITATE_ZOMBIE_NAUTILUS);
        map.put(EntityTypes.ZOMBIE_VILLAGER, SoundEvents.PARROT_IMITATE_ZOMBIE_VILLAGER);
    });
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1.0f;
    private float nextFlap = 1.0f;
    private boolean partyParrot;
    private @Nullable BlockPos jukebox;

    public Parrot(EntityType<? extends Parrot> type, Level level) {
        super((EntityType<? extends ShoulderRidingEntity>)type, level);
        this.moveControl = new FlyingMoveControl<Parrot>(this, 10, false);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0f);
        this.setPathfindingMalus(PathType.FIRE, -1.0f);
        this.setPathfindingMalus(PathType.COCOA, -1.0f);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.setVariant(Util.getRandom(Variant.values(), level.getRandom()));
        if (groupData == null) {
            groupData = new AgeableMob.AgeableMobGroupData(false);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TamableAnimal.TamableAnimalPanicGoal(this, 1.25));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0f, 1.0f));
        this.goalSelector.addGoal(2, new ParrotWanderGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LandOnOwnersShoulderGoal(this));
        this.goalSelector.addGoal(3, new FollowMobGoal(this, 1.0, 3.0f, 7.0f));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.FLYING_SPEED, 0.4f).add(Attributes.MOVEMENT_SPEED, 0.2f).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level);
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(true);
        return flyingPathNavigation;
    }

    @Override
    public void aiStep() {
        if (this.jukebox == null || !this.jukebox.closerToCenterThan(this.position(), 3.46) || !this.level().getBlockState(this.jukebox).is(Blocks.JUKEBOX)) {
            this.partyParrot = false;
            this.jukebox = null;
        }
        if (this.level().getRandom().nextInt(400) == 0) {
            Parrot.imitateNearbyMobs(this.level(), this);
        }
        super.aiStep();
        this.calculateFlapping();
    }

    @Override
    public void setRecordPlayingNearby(BlockPos jukebox, boolean isPlaying) {
        this.jukebox = jukebox;
        this.partyParrot = isPlaying;
    }

    public boolean isPartyParrot() {
        return this.partyParrot;
    }

    private void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed += (float)(this.onGround() || this.isPassenger() ? -1 : 4) * 0.3f;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0f, 1.0f);
        if (!this.onGround() && this.flapping < 1.0f) {
            this.flapping = 1.0f;
        }
        this.flapping *= 0.9f;
        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround() && movement.y < 0.0) {
            this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
        }
        this.flap += this.flapping * 2.0f;
    }

    public static boolean imitateNearbyMobs(Level level, Entity entity) {
        Mob mob;
        RandomSource random = level.getRandom();
        if (!entity.isAlive() || entity.isSilent() || random.nextInt(2) != 0) {
            return false;
        }
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(20.0), NOT_PARROT_PREDICATE);
        if (!mobs.isEmpty() && !(mob = mobs.get(random.nextInt(mobs.size()))).isSilent()) {
            SoundEvent soundEvent = Parrot.getImitatedSound(mob.getType());
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, entity.getSoundSource(), 0.7f, Parrot.getPitch(random));
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!this.isTame() && itemStack.is(ItemTags.PARROT_FOOD)) {
            this.usePlayerItem(player, hand, itemStack);
            if (!this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PARROT_EAT, this.getSoundSource(), 1.0f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f);
            }
            if (!this.level().isClientSide()) {
                if (this.random.nextInt(10) == 0) {
                    this.tame(player);
                    this.level().broadcastEntityEvent(this, (byte)7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (itemStack.is(ItemTags.PARROT_POISONOUS_FOOD)) {
            this.usePlayerItem(player, hand, itemStack);
            this.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
            if (player.isCreative() || !this.isInvulnerable()) {
                this.hurt(this.damageSources().playerAttack(player), Float.MAX_VALUE);
            }
            return InteractionResult.SUCCESS;
        }
        if (!this.isFlying() && this.isTame() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide()) {
                this.setOrderedToSit(!this.isOrderedToSit());
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    public static boolean checkParrotSpawnRules(EntityType<Parrot> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.PARROTS_SPAWNABLE_ON) && Parrot.isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public @Nullable SoundEvent getAmbientSound() {
        return Parrot.getAmbient(this.level(), this.level().getRandom());
    }

    public static SoundEvent getAmbient(Level level, RandomSource random) {
        if (level.getDifficulty() != Difficulty.PEACEFUL && random.nextInt(1000) == 0) {
            ArrayList keys = Lists.newArrayList(MOB_SOUND_MAP.keySet());
            return Parrot.getImitatedSound((EntityType)keys.get(random.nextInt(keys.size())));
        }
        return SoundEvents.PARROT_AMBIENT;
    }

    private static SoundEvent getImitatedSound(EntityType<?> id) {
        return MOB_SOUND_MAP.getOrDefault(id, SoundEvents.PARROT_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.PARROT_STEP, 0.15f, 1.0f);
    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.playSound(SoundEvents.PARROT_FLY, 0.15f, 1.0f);
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0f;
    }

    @Override
    public float getVoicePitch() {
        return Parrot.getPitch(this.random);
    }

    public static float getPitch(RandomSource random) {
        return (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        if (entity instanceof Player) {
            return;
        }
        super.doPush(entity);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        this.setOrderedToSit(false);
        return super.hurtServer(level, source, damage);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    private void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT_ID, variant.id);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.PARROT_VARIANT) {
            return Parrot.castComponentValue(type, this.getVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.PARROT_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.PARROT_VARIANT) {
            this.setVariant(Parrot.castComponentValue(DataComponents.PARROT_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_VARIANT_ID, Variant.DEFAULT.id);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Variant", Variant.LEGACY_CODEC, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.read("Variant", Variant.LEGACY_CODEC).orElse(Variant.DEFAULT));
    }

    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    public static enum Variant implements StringRepresentable
    {
        RED_BLUE(0, "red_blue"),
        BLUE(1, "blue"),
        GREEN(2, "green"),
        YELLOW_BLUE(3, "yellow_blue"),
        GRAY(4, "gray");

        public static final Variant DEFAULT;
        private static final IntFunction<Variant> BY_ID;
        public static final Codec<Variant> CODEC;
        @Deprecated
        public static final Codec<Variant> LEGACY_CODEC;
        public static final StreamCodec<ByteBuf, Variant> STREAM_CODEC;
        private final int id;
        private final String name;

        private Variant(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            DEFAULT = RED_BLUE;
            BY_ID = ByIdMap.continuous(Variant::getId, Variant.values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
            CODEC = StringRepresentable.fromEnum(Variant::values);
            LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Variant::getId);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Variant::getId);
        }
    }

    private static class ParrotWanderGoal
    extends WaterAvoidingRandomFlyingGoal {
        public ParrotWanderGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier);
        }

        @Override
        protected @Nullable Vec3 getPosition() {
            Vec3 pos = null;
            if (this.mob.isInWater()) {
                pos = LandRandomPos.getPos(this.mob, 15, 15);
            }
            if (this.mob.getRandom().nextFloat() >= this.probability) {
                pos = this.getTreePos();
            }
            return pos == null ? super.getPosition() : pos;
        }

        private @Nullable Vec3 getTreePos() {
            BlockPos mobPos = this.mob.blockPosition();
            BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
            Iterable<BlockPos> between = BlockPos.betweenClosed(Mth.floor(this.mob.getX() - 3.0), Mth.floor(this.mob.getY() - 6.0), Mth.floor(this.mob.getZ() - 3.0), Mth.floor(this.mob.getX() + 3.0), Mth.floor(this.mob.getY() + 6.0), Mth.floor(this.mob.getZ() + 3.0));
            for (BlockPos pos : between) {
                BlockState state;
                boolean canSitOn;
                if (mobPos.equals(pos) || !(canSitOn = (state = this.mob.level().getBlockState(belowPos.setWithOffset((Vec3i)pos, Direction.DOWN))).getBlock() instanceof LeavesBlock || state.is(BlockTags.LOGS)) || !this.mob.level().isEmptyBlock(pos) || !this.mob.level().isEmptyBlock(abovePos.setWithOffset((Vec3i)pos, Direction.UP))) continue;
                return Vec3.atBottomCenterOf(pos);
            }
            return null;
        }
    }
}

