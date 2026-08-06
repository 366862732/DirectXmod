/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

public final class Ingredient
implements Predicate<ItemStack>,
StackedContents.IngredientInfo<Holder<Item>> {
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = ByteBufCodecs.holderSet(Registries.ITEM).map(Ingredient::new, i -> i.values);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = ByteBufCodecs.holderSet(Registries.ITEM).map(ingredient -> ingredient.size() == 0 ? Optional.empty() : Optional.of(new Ingredient((HolderSet<Item>)ingredient)), ingredient -> ingredient.map(i -> i.values).orElse(HolderSet.empty()));
    public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
    public static final Codec<Ingredient> CODEC = ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, i -> i.values);
    private final HolderSet<Item> values;

    private Ingredient(HolderSet<Item> values) {
        values.unwrap().ifRight(directValues -> {
            if (directValues.isEmpty()) {
                throw new UnsupportedOperationException("Ingredients can't be empty");
            }
            if (directValues.contains(Items.AIR.builtInRegistryHolder())) {
                throw new UnsupportedOperationException("Ingredient can't contain air");
            }
        });
        this.values = values;
    }

    public static boolean testOptionalIngredient(Optional<Ingredient> ingredient, ItemStack stack) {
        return ingredient.map(value -> value.test(stack)).orElseGet(stack::isEmpty);
    }

    @Deprecated
    public Stream<Holder<Item>> items() {
        return this.values.stream();
    }

    public boolean isEmpty() {
        return this.values.size() == 0;
    }

    @Override
    public boolean test(ItemStack input) {
        return input.is(this.values);
    }

    @Override
    public boolean acceptsItem(Holder<Item> item) {
        return this.values.contains(item);
    }

    public boolean equals(Object o) {
        if (o instanceof Ingredient) {
            Ingredient other = (Ingredient)o;
            return Objects.equals(this.values, other.values);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hashCode(this.values);
    }

    public static Ingredient of(ItemLike itemLike) {
        return new Ingredient(HolderSet.direct(itemLike.asItem().builtInRegistryHolder()));
    }

    public static Ingredient of(ItemLike ... items) {
        return Ingredient.of(Arrays.stream(items));
    }

    public static Ingredient of(Stream<? extends ItemLike> stream) {
        return new Ingredient(HolderSet.direct(stream.map(e -> e.asItem().builtInRegistryHolder()).toList()));
    }

    public static Ingredient of(HolderSet<Item> tag) {
        return new Ingredient(tag);
    }

    public SlotDisplay display() {
        return (SlotDisplay)this.values.unwrap().map(SlotDisplay.TagSlotDisplay::new, l -> new SlotDisplay.Composite(l.stream().map(Ingredient::displayForSingleItem).toList()));
    }

    public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> ingredient) {
        return ingredient.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
    }

    private static SlotDisplay displayForSingleItem(Holder<Item> item) {
        SlotDisplay.ItemSlotDisplay inputDisplay = new SlotDisplay.ItemSlotDisplay(item);
        ItemStackTemplate remainderStack = item.value().getCraftingRemainder();
        if (remainderStack != null) {
            SlotDisplay.ItemStackSlotDisplay remainderDisplay = new SlotDisplay.ItemStackSlotDisplay(remainderStack);
            return new SlotDisplay.WithRemainder(inputDisplay, remainderDisplay);
        }
        return inputDisplay;
    }
}

