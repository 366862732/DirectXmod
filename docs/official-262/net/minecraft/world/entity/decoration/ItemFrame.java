/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.decoration;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemFrame
extends HangingEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.INT);
    public static final int NUM_ROTATIONS = 8;
    private static final float DEPTH = 0.0625f;
    private static final float WIDTH = 0.75f;
    private static final float HEIGHT = 0.75f;
    private static final byte DEFAULT_ROTATION = 0;
    private static final float DEFAULT_DROP_CHANCE = 1.0f;
    private static final boolean DEFAULT_INVISIBLE = false;
    private static final boolean DEFAULT_FIXED = false;
    private float dropChance = 1.0f;
    private boolean fixed = false;

    public ItemFrame(EntityType<? extends ItemFrame> type, Level level) {
        super((EntityType<? extends HangingEntity>)type, level);
        this.setInvisible(false);
    }

    public ItemFrame(Level level, BlockPos pos, Direction direction) {
        this(EntityTypes.ITEM_FRAME, level, pos, direction);
    }

    public ItemFrame(EntityType<? extends ItemFrame> type, Level level, BlockPos pos, Direction direction) {
        super((EntityType<? extends HangingEntity>)type, level, pos);
        this.setDirection(direction);
        this.setInvisible(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
        entityData.define(DATA_ROTATION, 0);
    }

    @Override
    protected void setDirection(Direction direction) {
        Objects.requireNonNull(direction);
        super.setDirectionRaw(direction);
        if (direction.getAxis().isHorizontal()) {
            this.setXRot(0.0f);
            this.setYRot(direction.get2DDataValue() * 90);
        } else {
            this.setXRot(-90 * direction.getAxisDirection().getStep());
            this.setYRot(0.0f);
        }
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.recalculateBoundingBox();
    }

    @Override
    protected final void recalculateBoundingBox() {
        super.recalculateBoundingBox();
        this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos blockPos, Direction direction) {
        return this.createBoundingBox(blockPos, direction, this.hasFramedMap());
    }

    @Override
    protected AABB getPopBox() {
        return this.createBoundingBox(this.pos, this.getDirection(), false);
    }

    private AABB createBoundingBox(BlockPos blockPos, Direction direction, boolean hasFramedMap) {
        float shiftToBlockWall = 0.46875f;
        Vec3 position = Vec3.atCenterOf(blockPos).relative(direction, -0.46875);
        float width = hasFramedMap ? 1.0f : 0.75f;
        float height = hasFramedMap ? 1.0f : 0.75f;
        Direction.Axis axis = direction.getAxis();
        double xSize = axis == Direction.Axis.X ? 0.0625 : (double)width;
        double ySize = axis == Direction.Axis.Y ? 0.0625 : (double)height;
        double zSize = axis == Direction.Axis.Z ? 0.0625 : (double)width;
        return AABB.ofSize(position, xSize, ySize, zSize);
    }

    @Override
    public boolean survives() {
        if (this.fixed) {
            return true;
        }
        if (this.hasLevelCollision(this.getPopBox())) {
            return false;
        }
        BlockState state = this.level().getBlockState(this.pos.relative(this.getDirection().getOpposite()));
        if (!(state.isSolid() || this.getDirection().getAxis().isHorizontal() && DiodeBlock.isDiode(state))) {
            return false;
        }
        return this.canCoexist(true);
    }

    @Override
    public void move(MoverType moverType, Vec3 delta) {
        if (!this.fixed) {
            super.move(moverType, delta);
        }
    }

    @Override
    public void push(double xa, double ya, double za) {
        if (!this.fixed) {
            super.push(xa, ya, za);
        }
    }

    @Override
    public void kill(ServerLevel level) {
        this.removeFramedMap(this.getItem());
        super.kill(level);
    }

    private boolean shouldDamageDropItem(DamageSource source) {
        return !source.is(DamageTypeTags.IS_EXPLOSION) && !this.getItem().isEmpty();
    }

    private static boolean canHurtWhenFixed(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.isCreativePlayer();
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        if (this.fixed && !ItemFrame.canHurtWhenFixed(source)) {
            return false;
        }
        return !this.isInvulnerableToBase(source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.fixed) {
            return ItemFrame.canHurtWhenFixed(source) && super.hurtServer(level, source, damage);
        }
        if (this.isInvulnerableToBase(source)) {
            return false;
        }
        if (this.shouldDamageDropItem(source)) {
            this.dropItem(level, source.getEntity(), false);
            this.gameEvent(GameEvent.BLOCK_CHANGE, source.getEntity());
            this.playSound(this.getRemoveItemSound(), 1.0f, 1.0f);
            return true;
        }
        return super.hurtServer(level, source, damage);
    }

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = 16.0;
        return distance < (size *= 64.0 * ItemFrame.getViewScale()) * size;
    }

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        this.playSound(this.getBreakSound(), 1.0f, 1.0f);
        this.dropItem(level, causedBy, true);
        this.gameEvent(GameEvent.BLOCK_CHANGE, causedBy);
    }

    public SoundEvent getBreakSound() {
        return SoundEvents.ITEM_FRAME_BREAK;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(this.getPlaceSound(), 1.0f, 1.0f);
    }

    public SoundEvent getPlaceSound() {
        return SoundEvents.ITEM_FRAME_PLACE;
    }

    private void dropItem(ServerLevel level, @Nullable Entity causedBy, boolean withFrame) {
        Player player;
        if (this.fixed) {
            return;
        }
        ItemStack itemStack = this.getItem();
        this.setItem(ItemStack.EMPTY);
        if (!level.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
            if (causedBy == null) {
                this.removeFramedMap(itemStack);
            }
            return;
        }
        if (causedBy instanceof Player && (player = (Player)causedBy).hasInfiniteMaterials()) {
            this.removeFramedMap(itemStack);
            return;
        }
        if (withFrame) {
            this.spawnAtLocation(level, this.getFrameItemStack());
        }
        if (!itemStack.isEmpty()) {
            itemStack = itemStack.copy();
            this.removeFramedMap(itemStack);
            if (this.random.nextFloat() < this.dropChance) {
                this.spawnAtLocation(level, itemStack);
            }
        }
    }

    private void removeFramedMap(ItemStack itemStack) {
        MapItemSavedData mapItemSavedData;
        MapId mapId = this.getFramedMapId(itemStack);
        if (mapId != null && (mapItemSavedData = MapItem.getSavedData(mapId, this.level())) != null) {
            mapItemSavedData.removedFromFrame(this.pos, this.getId());
        }
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    public @Nullable MapId getFramedMapId(ItemStack itemStack) {
        return itemStack.get(DataComponents.MAP_ID);
    }

    public boolean hasFramedMap() {
        return this.getItem().has(DataComponents.MAP_ID);
    }

    public void setItem(ItemStack itemStack) {
        this.setItem(itemStack, true);
    }

    public void setItem(ItemStack itemStack, boolean updateNeighbours) {
        if (!itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(1);
        }
        this.onItemChanged(itemStack);
        this.getEntityData().set(DATA_ITEM, itemStack);
        if (!itemStack.isEmpty()) {
            this.playSound(this.getAddItemSound(), 1.0f, 1.0f);
        }
        if (updateNeighbours && this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    public SoundEvent getAddItemSound() {
        return SoundEvents.ITEM_FRAME_ADD_ITEM;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        if (slot == 0) {
            return SlotAccess.of(this::getItem, this::setItem);
        }
        return super.getSlot(slot);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor.equals(DATA_ITEM)) {
            this.onItemChanged(this.getItem());
        }
    }

    private void onItemChanged(ItemStack item) {
        this.recalculateBoundingBox();
    }

    public int getRotation() {
        return this.getEntityData().get(DATA_ROTATION);
    }

    public void setRotation(int rotation) {
        this.setRotation(rotation, true);
    }

    private void setRotation(int rotation, boolean updateNeighbours) {
        this.getEntityData().set(DATA_ROTATION, rotation % 8);
        if (updateNeighbours && this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ItemStack currentItem = this.getItem();
        if (!currentItem.isEmpty()) {
            output.store("Item", ItemStack.CODEC, currentItem);
        }
        output.putByte("ItemRotation", (byte)this.getRotation());
        output.putFloat("ItemDropChance", this.dropChance);
        output.store("Facing", Direction.LEGACY_ID_CODEC, this.getDirection());
        output.putBoolean("Invisible", this.isInvisible());
        output.putBoolean("Fixed", this.fixed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ItemStack itemStack = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ItemStack currentItem = this.getItem();
        if (!currentItem.isEmpty() && !ItemStack.matches(itemStack, currentItem)) {
            this.removeFramedMap(currentItem);
        }
        this.setItem(itemStack, false);
        this.setRotation(input.getByteOr("ItemRotation", (byte)0), false);
        this.dropChance = input.getFloatOr("ItemDropChance", 1.0f);
        this.setDirection(input.read("Facing", Direction.LEGACY_ID_CODEC).orElse(Direction.DOWN));
        this.setInvisible(input.getBooleanOr("Invisible", false));
        this.fixed = input.getBooleanOr("Fixed", false);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        boolean hasHeldItem;
        ItemStack itemStack = player.getItemInHand(hand);
        boolean frameHasItem = !this.getItem().isEmpty();
        boolean bl = hasHeldItem = !itemStack.isEmpty();
        if (this.fixed) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return frameHasItem || hasHeldItem ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!frameHasItem) {
            if (hasHeldItem && !this.isRemoved()) {
                MapItemSavedData data = MapItem.getSavedData(itemStack, this.level());
                if (data != null && data.isTrackedCountOverLimit(256)) {
                    return InteractionResult.FAIL;
                }
                this.setItem(itemStack);
                this.gameEvent(GameEvent.BLOCK_CHANGE, player);
                itemStack.consume(1, player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        this.playSound(this.getRotateItemSound(), 1.0f, 1.0f);
        this.setRotation(this.getRotation() + 1);
        this.gameEvent(GameEvent.BLOCK_CHANGE, player);
        return InteractionResult.SUCCESS;
    }

    public SoundEvent getRotateItemSound() {
        return SoundEvents.ITEM_FRAME_ROTATE_ITEM;
    }

    public int getAnalogOutput() {
        if (this.getItem().isEmpty()) {
            return 0;
        }
        return this.getRotation() % 8 + 1;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket((Entity)this, this.getDirection().get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack framedStack = this.getItem();
        if (framedStack.isEmpty()) {
            return this.getFrameItemStack();
        }
        return framedStack.copy();
    }

    protected ItemStack getFrameItemStack() {
        return new ItemStack(Items.ITEM_FRAME);
    }

    @Override
    public float getVisualRotationYInDegrees() {
        Direction frameDirection = this.getDirection();
        int rotationCorrection = frameDirection.getAxis().isVertical() ? 90 * frameDirection.getAxisDirection().getStep() : 0;
        return Mth.wrapDegrees(180 + frameDirection.get2DDataValue() * 90 + this.getRotation() * 45 + rotationCorrection);
    }
}

