/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class ImbueRecipe
extends NormalCraftingRecipe {
    public static final MapCodec<ImbueRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo), (App)CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo), (App)Ingredient.CODEC.fieldOf("source").forGetter(o -> o.source), (App)Ingredient.CODEC.fieldOf("material").forGetter(o -> o.material), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, ImbueRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImbueRecipe> STREAM_CODEC = StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo, CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo, Ingredient.CONTENTS_STREAM_CODEC, o -> o.source, Ingredient.CONTENTS_STREAM_CODEC, o -> o.material, ItemStackTemplate.STREAM_CODEC, o -> o.result, ImbueRecipe::new);
    public static final RecipeSerializer<ImbueRecipe> SERIALIZER = new RecipeSerializer<ImbueRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient source;
    private final Ingredient material;
    private final ItemStackTemplate result;

    public ImbueRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, Ingredient source, Ingredient material, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.source = source;
        this.material = material;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) {
            return false;
        }
        for (int y = 0; y < input.height(); ++y) {
            for (int x = 0; x < input.width(); ++x) {
                Ingredient ingredient;
                ItemStack itemStack = input.getItem(x, y);
                if (itemStack.isEmpty()) {
                    return false;
                }
                Ingredient ingredient2 = ingredient = x == 1 && y == 1 ? this.source : this.material;
                if (ingredient.test(itemStack)) continue;
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack source = input.getItem(1, 1);
        ItemStack result = this.result.create();
        result.set(DataComponents.POTION_CONTENTS, source.get(DataComponents.POTION_CONTENTS));
        return result;
    }

    @Override
    public RecipeSerializer<ImbueRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(List.of(this.material, this.material, this.material, this.material, this.source, this.material, this.material, this.material, this.material));
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay material = this.material.display();
        SlotDisplay.WithAnyPotion source = new SlotDisplay.WithAnyPotion(this.source.display());
        return List.of(new ShapedCraftingRecipeDisplay(3, 3, List.of(material, material, material, material, source, material, material, material, material), new SlotDisplay.WithAnyPotion(new SlotDisplay.ItemStackSlotDisplay(this.result)), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}

