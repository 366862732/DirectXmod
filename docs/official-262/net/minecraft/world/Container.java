/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface Container
extends Clearable,
Iterable<ItemStack>,
SlotProvider {
    public static final float DEFAULT_DISTANCE_BUFFER = 4.0f;

    public int getContainerSize();

    public boolean isEmpty();

    public ItemStack getItem(int var1);

    public ItemStack removeItem(int var1, int var2);

    public ItemStack removeItemNoUpdate(int var1);

    public void setItem(int var1, ItemStack var2);

    default public int getMaxStackSize() {
        return 99;
    }

    default public int getMaxStackSize(ItemStack itemStack) {
        return Math.min(this.getMaxStackSize(), itemStack.getMaxStackSize());
    }

    public void setChanged();

    public boolean stillValid(Player var1);

    default public void startOpen(ContainerUser containerUser) {
    }

    default public void stopOpen(ContainerUser containerUser) {
    }

    default public List<ContainerUser> getEntitiesWithContainerOpen() {
        return List.of();
    }

    default public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return true;
    }

    default public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
        return true;
    }

    default public int countItem(Item item) {
        int count = 0;
        for (ItemStack slotItem : this) {
            if (!slotItem.getItem().equals(item)) continue;
            count += slotItem.getCount();
        }
        return count;
    }

    default public boolean hasAnyOf(Set<Item> item) {
        return this.hasAnyMatching(stack -> !stack.isEmpty() && item.contains(stack.getItem()));
    }

    default public boolean hasAnyMatching(Predicate<ItemStack> predicate) {
        for (ItemStack slotItem : this) {
            if (!predicate.test(slotItem)) continue;
            return true;
        }
        return false;
    }

    public static boolean stillValidBlockEntity(BlockEntity blockEntity, Player player) {
        return Container.stillValidBlockEntity(blockEntity, player, 4.0f);
    }

    public static boolean stillValidBlockEntity(BlockEntity blockEntity, Player player, float distanceBuffer) {
        Level level = blockEntity.getLevel();
        BlockPos worldPosition = blockEntity.getBlockPos();
        if (level == null) {
            return false;
        }
        if (level.getBlockEntity(worldPosition) != blockEntity) {
            return false;
        }
        return player.isWithinBlockInteractionRange(worldPosition, distanceBuffer);
    }

    @Override
    default public @Nullable SlotAccess getSlot(final int slot) {
        if (slot < 0 || slot >= this.getContainerSize()) {
            return null;
        }
        return new SlotAccess(){
            final /* synthetic */ Container this$0;
            {
                Container container = this$0;
                Objects.requireNonNull(container);
                this.this$0 = container;
            }

            @Override
            public ItemStack get() {
                return this.this$0.getItem(slot);
            }

            @Override
            public boolean set(ItemStack itemStack) {
                this.this$0.setItem(slot, itemStack);
                return true;
            }
        };
    }

    @Override
    default public Iterator<ItemStack> iterator() {
        return new ContainerIterator(this);
    }

    public static class ContainerIterator
    implements Iterator<ItemStack> {
        private final Container container;
        private int index;
        private final int size;

        public ContainerIterator(Container container) {
            this.container = container;
            this.size = container.getContainerSize();
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        public ItemStack next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            return this.container.getItem(this.index++);
        }
    }
}

