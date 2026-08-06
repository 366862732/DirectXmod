/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ExperienceOrb
extends Entity {
    protected static final EntityDataAccessor<Integer> DATA_VALUE = SynchedEntityData.defineId(ExperienceOrb.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 6000;
    private static final int ENTITY_SCAN_PERIOD = 20;
    private static final int MAX_FOLLOW_DIST = 8;
    private static final int ORB_GROUPS_PER_AREA = 40;
    private static final double ORB_MERGE_DISTANCE = 0.5;
    private static final short DEFAULT_HEALTH = 5;
    private static final short DEFAULT_AGE = 0;
    private static final short DEFAULT_VALUE = 0;
    private static final int DEFAULT_COUNT = 1;
    private int age = 0;
    private int health = 5;
    private int count = 1;
    private @Nullable Player followingPlayer;
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public ExperienceOrb(Level level, double x, double y, double z, int value) {
        this(level, new Vec3(x, y, z), Vec3.ZERO, value);
    }

    public ExperienceOrb(Level level, Vec3 pos, Vec3 roughly, int value) {
        this((EntityType<? extends ExperienceOrb>)EntityTypes.EXPERIENCE_ORB, level);
        this.setPos(pos);
        if (!level.isClientSide()) {
            this.setYRot(this.random.nextFloat() * 360.0f);
            Vec3 randomMovement = new Vec3((this.random.nextDouble() * 0.2 - 0.1) * 2.0, this.random.nextDouble() * 0.2 * 2.0, (this.random.nextDouble() * 0.2 - 0.1) * 2.0);
            if (roughly.lengthSqr() > 0.0 && roughly.dot(randomMovement) < 0.0) {
                randomMovement = randomMovement.scale(-1.0);
            }
            double size = this.getBoundingBox().getSize();
            this.setPos(pos.add(roughly.normalize().scale(size * 0.5)));
            this.setDeltaMovement(randomMovement);
            if (!level.noCollision(this.getBoundingBox())) {
                this.unstuckIfPossible(size);
            }
        }
        this.setValue(value);
    }

    public ExperienceOrb(EntityType<? extends ExperienceOrb> type, Level level) {
        super(type, level);
    }

    protected void unstuckIfPossible(double maxDistance) {
        Vec3 center = this.position().add(0.0, (double)this.getBbHeight() / 2.0, 0.0);
        VoxelShape allowedCenters = Shapes.create(AABB.ofSize(center, maxDistance, maxDistance, maxDistance));
        this.level().findFreePosition(this, allowedCenters, center, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()).ifPresent(pos -> this.setPos(pos.add(0.0, (double)(-this.getBbHeight()) / 2.0, 0.0)));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_VALUE, 0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        boolean colliding;
        this.interpolation.interpolate();
        if (this.firstTick && this.level().isClientSide()) {
            this.firstTick = false;
            return;
        }
        super.tick();
        boolean bl = colliding = !this.level().noCollision(this.getBoundingBox());
        if (this.isEyeInFluid(FluidTags.WATER)) {
            this.setUnderwaterMovement();
        } else if (!colliding) {
            this.applyGravity();
        }
        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
            this.setDeltaMovement((this.random.nextFloat() - this.random.nextFloat()) * 0.2f, 0.2f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f);
        }
        if (this.tickCount % 20 == 1) {
            this.scanForMerges();
        }
        this.followNearbyPlayer();
        if (this.followingPlayer == null && !this.level().isClientSide() && colliding) {
            boolean nextColliding;
            boolean bl2 = nextColliding = !this.level().noCollision(this.getBoundingBox().move(this.getDeltaMovement()));
            if (nextColliding) {
                this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
                this.needsSync = true;
            }
        }
        double fallSpeed = this.getDeltaMovement().y;
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        float friction = this.getAirDrag();
        if (this.onGround()) {
            friction *= this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(friction));
        if (this.verticalCollisionBelow && fallSpeed < -this.getGravity()) {
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, -fallSpeed * 0.4, this.getDeltaMovement().z));
        }
        ++this.age;
        if (this.age >= 6000) {
            this.discard();
        }
    }

    @Override
    protected float getAirDrag() {
        return 0.98f;
    }

    private void followNearbyPlayer() {
        if (this.followingPlayer == null || this.followingPlayer.isSpectator() || this.followingPlayer.distanceToSqr(this) > 64.0) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 8.0);
            this.followingPlayer = nearestPlayer != null && !nearestPlayer.isSpectator() && !nearestPlayer.isDeadOrDying() ? nearestPlayer : null;
        }
        if (this.followingPlayer != null) {
            Vec3 delta = new Vec3(this.followingPlayer.getX() - this.getX(), this.followingPlayer.getY() + (double)this.followingPlayer.getEyeHeight() / 2.0 - this.getY(), this.followingPlayer.getZ() - this.getZ());
            double length = delta.lengthSqr();
            double power = 1.0 - Math.sqrt(length) / 8.0;
            this.setDeltaMovement(this.getDeltaMovement().add(delta.normalize().scale(power * power * 0.1)));
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999f);
    }

    private void scanForMerges() {
        if (this.level() instanceof ServerLevel) {
            List<ExperienceOrb> orbs = this.level().getEntities(EntityTypeTest.forClass(ExperienceOrb.class), this.getBoundingBox().inflate(0.5), this::canMerge);
            for (ExperienceOrb orb : orbs) {
                this.merge(orb);
            }
        }
    }

    public static void award(ServerLevel level, Vec3 pos, int amount) {
        ExperienceOrb.awardWithDirection(level, pos, Vec3.ZERO, amount);
    }

    public static void awardWithDirection(ServerLevel level, Vec3 pos, Vec3 roughDirection, int amount) {
        while (amount > 0) {
            int newCount = ExperienceOrb.getExperienceValue(amount);
            amount -= newCount;
            if (ExperienceOrb.tryMergeToExisting(level, pos, newCount)) continue;
            level.addFreshEntity(new ExperienceOrb(level, pos, roughDirection, newCount));
        }
    }

    private static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int value) {
        AABB box = AABB.ofSize(pos, 1.0, 1.0, 1.0);
        int id = level.getRandom().nextInt(40);
        List<ExperienceOrb> orbs = level.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), box, orb -> ExperienceOrb.canMerge(orb, id, value));
        if (!orbs.isEmpty()) {
            ExperienceOrb orb2 = orbs.get(0);
            ++orb2.count;
            orb2.age = 0;
            return true;
        }
        return false;
    }

    private boolean canMerge(ExperienceOrb orb) {
        return orb != this && ExperienceOrb.canMerge(orb, this.getId(), this.getValue());
    }

    private static boolean canMerge(ExperienceOrb orb, int id, int value) {
        return !orb.isRemoved() && (orb.getId() - id) % 40 == 0 && orb.getValue() == value;
    }

    private void merge(ExperienceOrb orb) {
        this.count += orb.count;
        this.age = Math.min(this.age, orb.age);
        orb.discard();
    }

    private void setUnderwaterMovement() {
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x * (double)0.99f, Math.min(movement.y + (double)5.0E-4f, (double)0.06f), movement.z * (double)0.99f);
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public final boolean hurtClient(DamageSource source) {
        return !this.isInvulnerableToBase(source);
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableToBase(source)) {
            return false;
        }
        this.markHurt();
        this.health = (int)((float)this.health - damage);
        if (this.health <= 0) {
            this.discard();
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("Health", (short)this.health);
        output.putShort("Age", (short)this.age);
        output.putShort("Value", (short)this.getValue());
        output.putInt("Count", this.count);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.health = input.getShortOr("Health", (short)5);
        this.age = input.getShortOr("Age", (short)0);
        this.setValue(input.getShortOr("Value", (short)0));
        this.count = input.read("Count", ExtraCodecs.POSITIVE_INT).orElse(1);
    }

    @Override
    public void playerTouch(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        if (player.takeXpDelay == 0) {
            player.takeXpDelay = 2;
            player.take(this, 1);
            int remaining = this.repairPlayerItems(serverPlayer, this.getValue());
            if (remaining > 0) {
                player.giveExperiencePoints(remaining);
            }
            --this.count;
            if (this.count == 0) {
                this.discard();
            }
        }
    }

    private int repairPlayerItems(ServerPlayer player, int amount) {
        Optional<EnchantedItemInUse> selected = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        if (selected.isPresent()) {
            int remaining;
            ItemStack itemStack = selected.get().itemStack();
            int toRepairFromXpAmount = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), itemStack, amount);
            int repair = Math.min(toRepairFromXpAmount, itemStack.getDamageValue());
            itemStack.setDamageValue(itemStack.getDamageValue() - repair);
            if (repair > 0 && (remaining = amount - repair * amount / toRepairFromXpAmount) > 0) {
                return this.repairPlayerItems(player, remaining);
            }
            return 0;
        }
        return amount;
    }

    public int getValue() {
        return this.entityData.get(DATA_VALUE);
    }

    private void setValue(int value) {
        this.entityData.set(DATA_VALUE, value);
    }

    public int getIcon() {
        int value = this.getValue();
        if (value >= 2477) {
            return 10;
        }
        if (value >= 1237) {
            return 9;
        }
        if (value >= 617) {
            return 8;
        }
        if (value >= 307) {
            return 7;
        }
        if (value >= 149) {
            return 6;
        }
        if (value >= 73) {
            return 5;
        }
        if (value >= 37) {
            return 4;
        }
        if (value >= 17) {
            return 3;
        }
        if (value >= 7) {
            return 2;
        }
        if (value >= 3) {
            return 1;
        }
        return 0;
    }

    public static int getExperienceValue(int maxValue) {
        if (maxValue >= 2477) {
            return 2477;
        }
        if (maxValue >= 1237) {
            return 1237;
        }
        if (maxValue >= 617) {
            return 617;
        }
        if (maxValue >= 307) {
            return 307;
        }
        if (maxValue >= 149) {
            return 149;
        }
        if (maxValue >= 73) {
            return 73;
        }
        if (maxValue >= 37) {
            return 37;
        }
        if (maxValue >= 17) {
            return 17;
        }
        if (maxValue >= 7) {
            return 7;
        }
        if (maxValue >= 3) {
            return 3;
        }
        return 1;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }
}

