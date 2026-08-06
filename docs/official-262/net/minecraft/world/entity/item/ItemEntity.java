/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.item;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemEntity
extends Entity
implements TraceableEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final float FLOAT_HEIGHT = 0.1f;
    public static final float EYE_HEIGHT = 0.2125f;
    private static final int LIFETIME = 6000;
    private static final int INFINITE_PICKUP_DELAY = Short.MAX_VALUE;
    private static final int INFINITE_LIFETIME = Short.MIN_VALUE;
    public static final int DEFAULT_HEALTH = 5;
    private static final short DEFAULT_AGE = 0;
    private static final short DEFAULT_PICKUP_DELAY = 0;
    private int age = 0;
    private int pickupDelay = 0;
    private int health = 5;
    private @Nullable EntityReference<Entity> thrower;
    private @Nullable UUID target;
    public final float bobOffs = this.random.nextFloat() * (float)Math.PI * 2.0f;

    public ItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        this.setYRot(this.random.nextFloat() * 360.0f);
    }

    public ItemEntity(Level level, double x, double y, double z, ItemStack itemStack) {
        this((EntityType<? extends ItemEntity>)EntityTypes.ITEM, level);
        this.setPos(x, y, z);
        this.setItem(itemStack);
        this.setDeltaMovement(this.random.nextDouble() * 0.2 - 0.1, 0.2, this.random.nextDouble() * 0.2 - 0.1);
    }

    public ItemEntity(Level level, double x, double y, double z, ItemStack itemStack, double deltaX, double deltaY, double deltaZ) {
        this((EntityType<? extends ItemEntity>)EntityTypes.ITEM, level);
        this.setPos(x, y, z);
        this.setItem(itemStack);
        this.setDeltaMovement(deltaX, deltaY, deltaZ);
    }

    @Override
    public boolean dampensVibrations() {
        return this.getItem().is(ItemTags.DAMPENS_VIBRATIONS);
    }

    @Override
    public @Nullable Entity getOwner() {
        return EntityReference.getEntity(this.thrower, this.level());
    }

    @Override
    public void restoreFrom(Entity oldEntity) {
        super.restoreFrom(oldEntity);
        if (oldEntity instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)oldEntity;
            this.thrower = item.thrower;
        }
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        double value;
        int rate;
        if (this.getItem().isEmpty()) {
            this.discard();
            return;
        }
        super.tick();
        if (this.pickupDelay > 0 && this.pickupDelay != Short.MAX_VALUE) {
            --this.pickupDelay;
        }
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        Vec3 oldMovement = this.getDeltaMovement();
        if (this.isInWater() && this.getFluidHeight(FluidTags.WATER) > (double)0.1f) {
            this.setUnderwaterMovement();
        } else if (this.isInLava() && this.getFluidHeight(FluidTags.LAVA) > (double)0.1f) {
            this.setUnderLavaMovement();
        } else {
            this.applyGravity();
        }
        if (this.level().isClientSide()) {
            this.noPhysics = false;
        } else {
            boolean bl = this.noPhysics = !this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7));
            if (this.noPhysics) {
                this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
            }
        }
        if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > (double)1.0E-5f || (this.tickCount + this.getId()) % 4 == 0) {
            float airDrag;
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.applyEffectsFromBlocks();
            float groundFriction = airDrag = this.getAirDrag();
            if (this.onGround()) {
                groundFriction *= this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
            }
            this.setDeltaMovement(this.getDeltaMovement().multiply(groundFriction, airDrag, groundFriction));
            if (this.onGround()) {
                Vec3 movement = this.getDeltaMovement();
                if (movement.y < 0.0) {
                    this.setDeltaMovement(movement.multiply(1.0, -0.5, 1.0));
                }
            }
        } else {
            this.applyEffectsFromBlocksForLastMovements();
        }
        boolean moved = Mth.floor(this.xo) != Mth.floor(this.getX()) || Mth.floor(this.yo) != Mth.floor(this.getY()) || Mth.floor(this.zo) != Mth.floor(this.getZ());
        int n = rate = moved ? 2 : 40;
        if (this.tickCount % rate == 0 && !this.level().isClientSide() && this.isMergable()) {
            this.mergeWithNeighbours();
        }
        if (this.age != Short.MIN_VALUE) {
            ++this.age;
        }
        this.needsSync |= this.updateFluidInteraction();
        if (!this.level().isClientSide() && (value = this.getDeltaMovement().subtract(oldMovement).lengthSqr()) > 0.01) {
            this.needsSync = true;
        }
        if (!this.level().isClientSide() && this.age >= 6000) {
            this.discard();
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999f);
    }

    private void setUnderwaterMovement() {
        this.setFluidMovement(0.99f);
    }

    private void setUnderLavaMovement() {
        this.setFluidMovement(0.95f);
    }

    private void setFluidMovement(double multiplier) {
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x * multiplier, movement.y + (double)(movement.y < (double)0.06f ? 5.0E-4f : 0.0f), movement.z * multiplier);
    }

    private void mergeWithNeighbours() {
        if (!this.isMergable()) {
            return;
        }
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.5, 0.0, 0.5), other -> other != this && other.isMergable());
        for (ItemEntity entity : items) {
            if (!entity.isMergable()) continue;
            this.tryToMerge(entity);
            if (!this.isRemoved()) continue;
            break;
        }
    }

    private boolean isMergable() {
        ItemStack item = this.getItem();
        return this.isAlive() && this.pickupDelay != Short.MAX_VALUE && this.age != Short.MIN_VALUE && this.age < 6000 && item.getCount() < item.getMaxStackSize();
    }

    private void tryToMerge(ItemEntity other) {
        ItemStack thisItemStack = this.getItem();
        ItemStack otherItemStack = other.getItem();
        if (!Objects.equals(this.target, other.target) || !ItemEntity.areMergable(thisItemStack, otherItemStack)) {
            return;
        }
        if (otherItemStack.getCount() < thisItemStack.getCount()) {
            ItemEntity.merge(this, thisItemStack, other, otherItemStack);
        } else {
            ItemEntity.merge(other, otherItemStack, this, thisItemStack);
        }
    }

    public static boolean areMergable(ItemStack thisItemStack, ItemStack otherItemStack) {
        if (otherItemStack.getCount() + thisItemStack.getCount() > otherItemStack.getMaxStackSize()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(thisItemStack, otherItemStack);
    }

    public static ItemStack merge(ItemStack toStack, ItemStack fromStack, int maxCount) {
        int delta = Math.min(Math.min(toStack.getMaxStackSize(), maxCount) - toStack.getCount(), fromStack.getCount());
        ItemStack newToStack = toStack.copyWithCount(toStack.getCount() + delta);
        fromStack.shrink(delta);
        return newToStack;
    }

    private static void merge(ItemEntity toItem, ItemStack toStack, ItemStack fromStack) {
        ItemStack newToStack = ItemEntity.merge(toStack, fromStack, 64);
        toItem.setItem(newToStack);
    }

    private static void merge(ItemEntity toItem, ItemStack toStack, ItemEntity fromItem, ItemStack fromStack) {
        ItemEntity.merge(toItem, toStack, fromStack);
        toItem.pickupDelay = Math.max(toItem.pickupDelay, fromItem.pickupDelay);
        toItem.age = Math.min(toItem.age, fromItem.age);
        if (fromStack.isEmpty()) {
            fromItem.discard();
        }
    }

    @Override
    public boolean fireImmune() {
        return !this.getItem().canBeHurtBy(this.damageSources().inFire()) || super.fireImmune();
    }

    @Override
    protected boolean shouldPlayLavaHurtSound() {
        if (this.health <= 0) {
            return true;
        }
        return this.tickCount % 10 == 0;
    }

    @Override
    public final boolean hurtClient(DamageSource source) {
        if (this.isInvulnerableToBase(source)) {
            return false;
        }
        return this.getItem().canBeHurtBy(source);
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableToBase(source)) {
            return false;
        }
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue() && source.getEntity() instanceof Mob) {
            return false;
        }
        if (!this.getItem().canBeHurtBy(source)) {
            return false;
        }
        this.markHurt();
        this.health = (int)((float)this.health - damage);
        this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        if (this.health <= 0) {
            this.getItem().onDestroyed(this);
            this.discard();
        }
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        if (explosion.shouldAffectBlocklikeEntities()) {
            return super.ignoreExplosion(explosion);
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("Health", (short)this.health);
        output.putShort("Age", (short)this.age);
        output.putShort("PickupDelay", (short)this.pickupDelay);
        EntityReference.store(this.thrower, output, "Thrower");
        output.storeNullable("Owner", UUIDUtil.CODEC, this.target);
        if (!this.getItem().isEmpty()) {
            output.store("Item", ItemStack.CODEC, this.getItem());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.health = input.getShortOr("Health", (short)5);
        this.age = input.getShortOr("Age", (short)0);
        this.pickupDelay = input.getShortOr("PickupDelay", (short)0);
        this.target = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.thrower = EntityReference.read(input, "Thrower");
        this.setItem(input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        if (this.getItem().isEmpty()) {
            this.discard();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide()) {
            return;
        }
        ItemStack itemStack = this.getItem();
        Item item = itemStack.getItem();
        int orgCount = itemStack.getCount();
        if (this.pickupDelay == 0 && (this.target == null || this.target.equals(player.getUUID())) && player.getInventory().add(itemStack)) {
            player.take(this, orgCount);
            if (itemStack.isEmpty()) {
                this.discard();
                itemStack.setCount(orgCount);
            }
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), orgCount);
            player.onItemPickup(this);
        }
    }

    @Override
    public Component getName() {
        Component name = this.getCustomName();
        if (name != null) {
            return name;
        }
        return this.getItem().getItemName();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        Entity entity = super.teleport(transition);
        if (!this.level().isClientSide() && entity instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)entity;
            item.mergeWithNeighbours();
        }
        return entity;
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack itemStack) {
        this.getEntityData().set(DATA_ITEM, itemStack);
    }

    public void setTarget(@Nullable UUID target) {
        this.target = target;
    }

    public void setThrower(Entity thrower) {
        this.thrower = EntityReference.of(thrower);
    }

    public int getAge() {
        return this.age;
    }

    public void setDefaultPickUpDelay() {
        this.pickupDelay = 10;
    }

    public void setNoPickUpDelay() {
        this.pickupDelay = 0;
    }

    public void setNeverPickUp() {
        this.pickupDelay = Short.MAX_VALUE;
    }

    public void setPickUpDelay(int ticks) {
        this.pickupDelay = ticks;
    }

    public boolean hasPickUpDelay() {
        return this.pickupDelay > 0;
    }

    public void setUnlimitedLifetime() {
        this.age = Short.MIN_VALUE;
    }

    public void setExtendedLifetime() {
        this.age = -6000;
    }

    public void makeFakeItem() {
        this.setNeverPickUp();
        this.age = 5999;
    }

    public static float getSpin(float ageInTicks, float bobOffset) {
        return ageInTicks / 20.0f + bobOffset;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return 180.0f - ItemEntity.getSpin((float)this.getAge() + 0.5f, this.bobOffs) / ((float)Math.PI * 2) * 360.0f;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        if (slot == 0) {
            return SlotAccess.of(this::getItem, this::setItem);
        }
        return super.getSlot(slot);
    }
}

