/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.slot;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public interface SlotCollection {
    public static final SlotCollection EMPTY = Stream::empty;

    public Stream<ItemStack> itemCopies();

    default public SlotCollection filter(Predicate<? super ItemStack> predicate) {
        return new Filtered(this, predicate);
    }

    default public SlotCollection flatMap(Function<ItemStack, ? extends SlotCollection> mapper) {
        return new FlatMapped(this, mapper);
    }

    default public SlotCollection limit(int limit) {
        return new Limited(this, limit);
    }

    public static SlotCollection of(SlotAccess slotAccess) {
        return () -> Stream.of(slotAccess.get().copy());
    }

    public static SlotCollection of(Collection<? extends SlotAccess> slots) {
        return switch (slots.size()) {
            case 0 -> EMPTY;
            case 1 -> SlotCollection.of(slots.iterator().next());
            default -> () -> slots.stream().map(SlotAccess::get).map(ItemStack::copy);
        };
    }

    public static SlotCollection concat(SlotCollection first, SlotCollection second) {
        return () -> Stream.concat(first.itemCopies(), second.itemCopies());
    }

    public static SlotCollection concat(List<? extends SlotCollection> terms) {
        return switch (terms.size()) {
            case 0 -> EMPTY;
            case 1 -> terms.getFirst();
            case 2 -> SlotCollection.concat(terms.get(0), terms.get(1));
            default -> () -> terms.stream().flatMap(SlotCollection::itemCopies);
        };
    }

    public record Filtered(SlotCollection slots, Predicate<? super ItemStack> filter) implements SlotCollection
    {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().filter(this.filter);
        }

        @Override
        public SlotCollection filter(Predicate<? super ItemStack> predicate) {
            Objects.requireNonNull(predicate);
            return new Filtered(this.slots, t -> this.filter.test((ItemStack)t) && predicate.test((ItemStack)t));
        }
    }

    public record FlatMapped(SlotCollection slots, Function<ItemStack, ? extends SlotCollection> mapper) implements SlotCollection
    {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().map(this.mapper).flatMap(SlotCollection::itemCopies);
        }
    }

    public record Limited(SlotCollection slots, int limit) implements SlotCollection
    {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().limit(this.limit);
        }

        @Override
        public SlotCollection limit(int limit) {
            return new Limited(this.slots, Math.min(this.limit, limit));
        }
    }
}

