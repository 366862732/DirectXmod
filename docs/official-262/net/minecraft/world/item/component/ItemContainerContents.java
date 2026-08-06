/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public final class ItemContainerContents
implements TooltipProvider {
    private static final int NO_SLOT = -1;
    private static final int MAX_SIZE = 256;
    public static final ItemContainerContents EMPTY = new ItemContainerContents(List.of());
    public static final Codec<ItemContainerContents> CODEC = Slot.CODEC.sizeLimitedListOf(256).xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemContainerContents> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional).apply(ByteBufCodecs.list(256)).map(ItemContainerContents::new, c -> c.items);
    private final List<Optional<ItemStackTemplate>> items;
    private final int hashCode;

    private ItemContainerContents(List<Optional<ItemStackTemplate>> items) {
        if (items.size() > 256) {
            throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is 256");
        }
        this.items = items;
        this.hashCode = items.hashCode();
    }

    private static List<Optional<ItemStackTemplate>> emptyContents(int size) {
        return new ArrayList<Optional<ItemStackTemplate>>(Collections.nCopies(size, Optional.empty()));
    }

    private static ItemContainerContents fromSlots(List<Slot> slots) {
        OptionalInt maxSlotIndex = slots.stream().mapToInt(Slot::index).max();
        if (maxSlotIndex.isEmpty()) {
            return EMPTY;
        }
        List<Optional<ItemStackTemplate>> items = ItemContainerContents.emptyContents(maxSlotIndex.getAsInt() + 1);
        for (Slot slot : slots) {
            items.set(slot.index(), Optional.of(slot.item()));
        }
        return new ItemContainerContents(items);
    }

    public static ItemContainerContents fromItems(List<ItemStack> itemStacks) {
        int lastNonEmptySlot = ItemContainerContents.findLastNonEmptySlot(itemStacks);
        if (lastNonEmptySlot == -1) {
            return EMPTY;
        }
        List<Optional<ItemStackTemplate>> items = ItemContainerContents.emptyContents(lastNonEmptySlot + 1);
        for (int i = 0; i <= lastNonEmptySlot; ++i) {
            ItemStack sourceStack = itemStacks.get(i);
            if (sourceStack.isEmpty()) continue;
            items.set(i, Optional.of(ItemStackTemplate.fromNonEmptyStack(sourceStack)));
        }
        return new ItemContainerContents(items);
    }

    private static int findLastNonEmptySlot(List<ItemStack> itemStacks) {
        for (int i = itemStacks.size() - 1; i >= 0; --i) {
            if (itemStacks.get(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    private List<Slot> asSlots() {
        ArrayList<Slot> slots = new ArrayList<Slot>();
        for (int i = 0; i < this.items.size(); ++i) {
            Optional<ItemStackTemplate> item = this.items.get(i);
            if (!item.isPresent()) continue;
            slots.add(new Slot(i, item.get()));
        }
        return slots;
    }

    private ItemStack createStackFromSlot(int slot) {
        Optional<ItemStackTemplate> slotContents;
        if (slot < this.items.size() && (slotContents = this.items.get(slot)).isPresent()) {
            return slotContents.get().create();
        }
        return ItemStack.EMPTY;
    }

    public void copyInto(NonNullList<ItemStack> destination) {
        for (int i = 0; i < destination.size(); ++i) {
            destination.set(i, this.createStackFromSlot(i));
        }
    }

    public ItemStack copyOne() {
        return this.createStackFromSlot(0);
    }

    public Stream<ItemStack> allItemsCopyStream() {
        return this.items.stream().map(i -> i.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY));
    }

    private Stream<ItemStackTemplate> nonEmptyItemsStream() {
        return this.items.stream().flatMap(Optional::stream);
    }

    public Stream<ItemStack> nonEmptyItemCopyStream() {
        return this.nonEmptyItemsStream().map(ItemStackTemplate::create);
    }

    public Iterable<ItemStackTemplate> nonEmptyItems() {
        return () -> this.nonEmptyItemsStream().iterator();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ItemContainerContents)) return false;
        ItemContainerContents contents = (ItemContainerContents)obj;
        if (!this.items.equals(contents.items)) return false;
        return true;
    }

    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        int lineCount = 0;
        int itemCount = 0;
        for (Optional<ItemStackTemplate> item : this.items) {
            if (item.isEmpty()) continue;
            ++itemCount;
            if (lineCount > 4) continue;
            ++lineCount;
            ItemStack itemStack = item.get().create();
            consumer.accept(Component.translatable("item.container.item_count", itemStack.getHoverName(), itemStack.getCount()));
        }
        if (itemCount - lineCount > 0) {
            consumer.accept(Component.translatable("item.container.more_items", itemCount - lineCount).withStyle(ChatFormatting.ITALIC));
        }
    }

    private record Slot(int index, ItemStackTemplate item) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.intRange((int)0, (int)255).fieldOf("slot").forGetter(Slot::index), (App)ItemStackTemplate.CODEC.fieldOf("item").forGetter(Slot::item)).apply((Applicative)i, Slot::new));
    }
}

