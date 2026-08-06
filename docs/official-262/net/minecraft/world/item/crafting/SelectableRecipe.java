/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record SelectableRecipe<T extends Recipe<?>>(SlotDisplay optionDisplay, Optional<RecipeHolder<T>> recipe) {
    public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe<T>> noRecipeCodec() {
        return StreamCodec.composite(SlotDisplay.STREAM_CODEC, SelectableRecipe::optionDisplay, slotDisplay -> new SelectableRecipe((SlotDisplay)slotDisplay, Optional.empty()));
    }

    public record SingleInputSet<T extends Recipe<?>>(List<SingleInputEntry<T>> entries) {
        public static <T extends Recipe<?>> SingleInputSet<T> empty() {
            return new SingleInputSet<T>(List.of());
        }

        public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SingleInputSet<T>> noRecipeCodec() {
            return StreamCodec.composite(SingleInputEntry.noRecipeCodec().apply(ByteBufCodecs.list()), SingleInputSet::entries, SingleInputSet::new);
        }

        public boolean acceptsInput(ItemStack input) {
            return this.entries.stream().anyMatch(e -> e.input.test(input));
        }

        public SingleInputSet<T> selectByInput(ItemStack input) {
            return new SingleInputSet<T>(this.entries.stream().filter(e -> e.input.test(input)).toList());
        }

        public boolean isEmpty() {
            return this.entries.isEmpty();
        }

        public int size() {
            return this.entries.size();
        }
    }

    public record SingleInputEntry<T extends Recipe<?>>(Ingredient input, SelectableRecipe<T> recipe) {
        public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SingleInputEntry<T>> noRecipeCodec() {
            return StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, SingleInputEntry::input, SelectableRecipe.noRecipeCodec(), SingleInputEntry::recipe, SingleInputEntry::new);
        }
    }
}

