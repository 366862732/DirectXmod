/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class HopperBlockEntity
extends RandomizableContainerBlockEntity
implements Hopper {
    public static final int MOVE_ITEM_SPEED = 8;
    public static final int HOPPER_CONTAINER_SIZE = 5;
    private static final int[][] CACHED_SLOTS = new int[54][];
    private static final int NO_COOLDOWN_TIME = -1;
    private static final Component DEFAULT_NAME = Component.translatable("container.hopper");
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int cooldownTime = -1;
    private long tickedGameTime;
    private Direction facing;

    public HopperBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.HOPPER, worldPosition, blockState);
        this.facing = blockState.getValue(HopperBlock.FACING);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
        }
        this.cooldownTime = input.getIntOr("TransferCooldown", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
        output.putInt("TransferCooldown", this.cooldownTime);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        this.unpackLootTable(null);
        return ContainerHelper.removeItem(this.getItems(), slot, count);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.unpackLootTable(null);
        this.getItems().set(slot, itemStack);
        itemStack.limitSize(this.getMaxStackSize(itemStack));
    }

    @Override
    public void setBlockState(BlockState blockState) {
        super.setBlockState(blockState);
        this.facing = blockState.getValue(HopperBlock.FACING);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    public static void pushItemsTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity) {
        --entity.cooldownTime;
        entity.tickedGameTime = level.getGameTime();
        if (!entity.isOnCooldown()) {
            entity.setCooldown(0);
            HopperBlockEntity.tryMoveItems(level, pos, state, entity, () -> HopperBlockEntity.suckInItems(level, entity));
        }
    }

    private static boolean tryMoveItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity, BooleanSupplier action) {
        if (level.isClientSide()) {
            return false;
        }
        if (!entity.isOnCooldown() && state.getValue(HopperBlock.ENABLED).booleanValue()) {
            boolean changed = false;
            if (!entity.isEmpty()) {
                changed = HopperBlockEntity.ejectItems(level, pos, entity);
            }
            if (!entity.inventoryFull()) {
                changed |= action.getAsBoolean();
            }
            if (changed) {
                entity.setCooldown(8);
                HopperBlockEntity.setChanged(level, pos, state);
                return true;
            }
        }
        return false;
    }

    private boolean inventoryFull() {
        for (ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxStackSize()) continue;
            return false;
        }
        return true;
    }

    private static boolean ejectItems(Level level, BlockPos blockPos, HopperBlockEntity self) {
        Container container = HopperBlockEntity.getAttachedContainer(level, blockPos, self);
        if (container == null) {
            return false;
        }
        Direction direction = self.facing.getOpposite();
        if (HopperBlockEntity.isFullContainer(container, direction)) {
            return false;
        }
        for (int slot = 0; slot < self.getContainerSize(); ++slot) {
            ItemStack itemStack = self.getItem(slot);
            if (itemStack.isEmpty()) continue;
            int originalCount = itemStack.getCount();
            ItemStack result = HopperBlockEntity.addItem(self, container, self.removeItem(slot, 1), direction);
            if (result.isEmpty()) {
                container.setChanged();
                return true;
            }
            itemStack.setCount(originalCount);
            if (originalCount != 1) continue;
            self.setItem(slot, itemStack);
        }
        return false;
    }

    private static int[] getSlots(Container container, Direction direction) {
        if (container instanceof WorldlyContainer) {
            WorldlyContainer worldlyContainer = (WorldlyContainer)container;
            return worldlyContainer.getSlotsForFace(direction);
        }
        int containerSize = container.getContainerSize();
        if (containerSize < CACHED_SLOTS.length) {
            int[] cachedSlots = CACHED_SLOTS[containerSize];
            if (cachedSlots != null) {
                return cachedSlots;
            }
            int[] slots = HopperBlockEntity.createFlatSlots(containerSize);
            HopperBlockEntity.CACHED_SLOTS[containerSize] = slots;
            return slots;
        }
        return HopperBlockEntity.createFlatSlots(containerSize);
    }

    private static int[] createFlatSlots(int containerSize) {
        int[] slots = new int[containerSize];
        for (int i = 0; i < slots.length; ++i) {
            slots[i] = i;
        }
        return slots;
    }

    private static boolean isFullContainer(Container container, Direction direction) {
        int[] slots;
        for (int slot : slots = HopperBlockEntity.getSlots(container, direction)) {
            ItemStack itemStack = container.getItem(slot);
            if (itemStack.getCount() >= itemStack.getMaxStackSize()) continue;
            return false;
        }
        return true;
    }

    public static boolean suckInItems(Level level, Hopper hopper) {
        boolean isBlocked;
        BlockState blockState;
        BlockPos blockPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
        Container container = HopperBlockEntity.getSourceContainer(level, hopper, blockPos, blockState = level.getBlockState(blockPos));
        if (container != null) {
            Direction direction = Direction.DOWN;
            for (int slot : HopperBlockEntity.getSlots(container, direction)) {
                if (!HopperBlockEntity.tryTakeInItemFromSlot(hopper, container, slot, direction)) continue;
                return true;
            }
            return false;
        }
        boolean bl = isBlocked = hopper.isGridAligned() && blockState.isCollisionShapeFullBlock(level, blockPos) && !blockState.is(BlockTags.DOES_NOT_BLOCK_HOPPERS);
        if (!isBlocked) {
            for (ItemEntity entity : HopperBlockEntity.getItemsAtAndAbove(level, hopper)) {
                if (!HopperBlockEntity.addItem(hopper, entity)) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean tryTakeInItemFromSlot(Hopper hopper, Container container, int slot, Direction direction) {
        ItemStack itemStack = container.getItem(slot);
        if (!itemStack.isEmpty() && HopperBlockEntity.canTakeItemFromContainer(hopper, container, itemStack, slot, direction)) {
            int originalCount = itemStack.getCount();
            ItemStack result = HopperBlockEntity.addItem(container, hopper, container.removeItem(slot, 1), null);
            if (result.isEmpty()) {
                container.setChanged();
                return true;
            }
            itemStack.setCount(originalCount);
            if (originalCount == 1) {
                container.setItem(slot, itemStack);
            }
        }
        return false;
    }

    public static boolean addItem(Container container, ItemEntity entity) {
        boolean changed = false;
        ItemStack copy = entity.getItem().copy();
        ItemStack result = HopperBlockEntity.addItem(null, container, copy, null);
        if (result.isEmpty()) {
            changed = true;
            entity.setItem(ItemStack.EMPTY);
            entity.discard();
        } else {
            entity.setItem(result);
        }
        return changed;
    }

    /*
     * Enabled aggressive block sorting
     * Lifted jumps to return sites
     */
    public static ItemStack addItem(@Nullable Container from, Container container, ItemStack itemStack, @Nullable Direction direction) {
        if (container instanceof WorldlyContainer) {
            WorldlyContainer worldly = (WorldlyContainer)container;
            if (direction != null) {
                int[] slots = worldly.getSlotsForFace(direction);
                int i = 0;
                while (i < slots.length) {
                    if (itemStack.isEmpty()) return itemStack;
                    itemStack = HopperBlockEntity.tryMoveInItem(from, container, itemStack, slots[i], direction);
                    ++i;
                }
                return itemStack;
            }
        }
        int size = container.getContainerSize();
        int i = 0;
        while (i < size) {
            if (itemStack.isEmpty()) return itemStack;
            itemStack = HopperBlockEntity.tryMoveInItem(from, container, itemStack, i, direction);
            ++i;
        }
        return itemStack;
    }

    private static boolean canPlaceItemInContainer(Container container, ItemStack itemStack, int slot, @Nullable Direction direction) {
        WorldlyContainer worldly;
        if (!container.canPlaceItem(slot, itemStack)) {
            return false;
        }
        return !(container instanceof WorldlyContainer) || (worldly = (WorldlyContainer)container).canPlaceItemThroughFace(slot, itemStack, direction);
    }

    private static boolean canTakeItemFromContainer(Container into, Container from, ItemStack itemStack, int slot, Direction direction) {
        WorldlyContainer worldly;
        if (!from.canTakeItem(into, slot, itemStack)) {
            return false;
        }
        return !(from instanceof WorldlyContainer) || (worldly = (WorldlyContainer)from).canTakeItemThroughFace(slot, itemStack, direction);
    }

    private static ItemStack tryMoveInItem(@Nullable Container from, Container container, ItemStack itemStack, int slot, @Nullable Direction direction) {
        ItemStack current = container.getItem(slot);
        if (HopperBlockEntity.canPlaceItemInContainer(container, itemStack, slot, direction)) {
            boolean success = false;
            boolean wasEmpty = container.isEmpty();
            if (current.isEmpty()) {
                container.setItem(slot, itemStack);
                itemStack = ItemStack.EMPTY;
                success = true;
            } else if (HopperBlockEntity.canMergeItems(current, itemStack)) {
                int space = itemStack.getMaxStackSize() - current.getCount();
                int count = Math.min(itemStack.getCount(), space);
                itemStack.shrink(count);
                current.grow(count);
                boolean bl = success = count > 0;
            }
            if (success) {
                HopperBlockEntity hopperBlockEntity;
                if (wasEmpty && container instanceof HopperBlockEntity && !(hopperBlockEntity = (HopperBlockEntity)container).isOnCustomCooldown()) {
                    int skipTickCount = 0;
                    if (from instanceof HopperBlockEntity) {
                        HopperBlockEntity fromHopper = (HopperBlockEntity)from;
                        if (hopperBlockEntity.tickedGameTime >= fromHopper.tickedGameTime) {
                            skipTickCount = 1;
                        }
                    }
                    hopperBlockEntity.setCooldown(8 - skipTickCount);
                }
                container.setChanged();
            }
        }
        return itemStack;
    }

    private static @Nullable Container getAttachedContainer(Level level, BlockPos blockPos, HopperBlockEntity self) {
        return HopperBlockEntity.getContainerAt(level, blockPos.relative(self.facing));
    }

    private static @Nullable Container getSourceContainer(Level level, Hopper hopper, BlockPos pos, BlockState state) {
        return HopperBlockEntity.getContainerAt(level, pos, state, hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
    }

    public static List<ItemEntity> getItemsAtAndAbove(Level level, Hopper hopper) {
        AABB aabb = hopper.getSuckAabb().move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5);
        return level.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
    }

    public static @Nullable Container getContainerAt(Level level, BlockPos pos) {
        return HopperBlockEntity.getContainerAt(level, pos, level.getBlockState(pos), (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
    }

    private static @Nullable Container getContainerAt(Level level, BlockPos pos, BlockState state, double x, double y, double z) {
        Container result = HopperBlockEntity.getBlockContainer(level, pos, state);
        if (result == null) {
            result = HopperBlockEntity.getEntityContainer(level, x, y, z);
        }
        return result;
    }

    private static @Nullable Container getBlockContainer(Level level, BlockPos pos, BlockState state) {
        BlockEntity entity;
        Block block = state.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            WorldlyContainerHolder worldlyContainerHolder = (WorldlyContainerHolder)((Object)block);
            return worldlyContainerHolder.getContainer(state, level, pos);
        }
        if (state.hasBlockEntity() && (entity = level.getBlockEntity(pos)) instanceof Container) {
            Container container = (Container)((Object)entity);
            if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
                ChestBlock chestBlock = (ChestBlock)block;
                container = ChestBlock.getContainer(chestBlock, state, level, pos, true);
            }
            return container;
        }
        return null;
    }

    private static @Nullable Container getEntityContainer(Level level, double x, double y, double z) {
        List<Entity> entities = level.getEntities((Entity)null, new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntitySelector.CONTAINER_ENTITY_SELECTOR);
        if (!entities.isEmpty()) {
            return (Container)((Object)entities.get(level.getRandom().nextInt(entities.size())));
        }
        return null;
    }

    private static boolean canMergeItems(ItemStack a, ItemStack b) {
        return a.getCount() <= a.getMaxStackSize() && ItemStack.isSameItemSameComponents(a, b);
    }

    @Override
    public double getLevelX() {
        return (double)this.worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return (double)this.worldPosition.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return (double)this.worldPosition.getZ() + 0.5;
    }

    @Override
    public boolean isGridAligned() {
        return true;
    }

    private void setCooldown(int time) {
        this.cooldownTime = time;
    }

    private boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    private boolean isOnCustomCooldown() {
        return this.cooldownTime > 8;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public static void entityInside(Level level, BlockPos pos, BlockState blockState, Entity entity, HopperBlockEntity hopper) {
        ItemEntity itemEntity;
        if (entity instanceof ItemEntity && !(itemEntity = (ItemEntity)entity).getItem().isEmpty() && entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(hopper.getSuckAabb())) {
            HopperBlockEntity.tryMoveItems(level, pos, blockState, hopper, () -> HopperBlockEntity.addItem(hopper, itemEntity));
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HopperMenu(containerId, inventory, this);
    }
}

