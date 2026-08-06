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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class DyeRecipe
extends NormalCraftingRecipe {
    public static final MapCodec<DyeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo), (App)CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo), (App)Ingredient.CODEC.fieldOf("target").forGetter(o -> o.target), (App)Ingredient.CODEC.fieldOf("dye").forGetter(o -> o.dye), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, DyeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DyeRecipe> STREAM_CODEC = StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo, CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo, Ingredient.CONTENTS_STREAM_CODEC, o -> o.target, Ingredient.CONTENTS_STREAM_CODEC, o -> o.dye, ItemStackTemplate.STREAM_CODEC, o -> o.result, DyeRecipe::new);
    public static final RecipeSerializer<DyeRecipe> SERIALIZER = new RecipeSerializer<DyeRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient target;
    private final Ingredient dye;
    private final ItemStackTemplate result;

    public DyeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, Ingredient target, Ingredient dye, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.target = target;
        this.dye = dye;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() < 2) {
            return false;
        }
        boolean hasTarget = false;
        boolean hasDyes = false;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.target.test(itemStack)) {
                if (hasTarget) {
                    return false;
                }
                hasTarget = true;
                continue;
            }
            if (this.dye.test(itemStack) && itemStack.has(DataComponents.DYE)) {
                hasDyes = true;
                continue;
            }
            return false;
        }
        return hasDyes && hasTarget;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ArrayList<DyeColor> dyes = new ArrayList<DyeColor>();
        ItemStack targetStack = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.target.test(itemStack)) {
                if (!targetStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                targetStack = itemStack;
                continue;
            }
            if (this.dye.test(itemStack)) {
                DyeColor dye = itemStack.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
                dyes.add(dye);
                continue;
            }
            return ItemStack.EMPTY;
        }
        if (targetStack.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }
        DyedItemColor currentDye = targetStack.get(DataComponents.DYED_COLOR);
        DyedItemColor newDyedColor = DyedItemColor.applyDyes(currentDye, dyes);
        ItemStack result = TransmuteRecipe.createWithOriginalComponents(this.result, targetStack);
        result.set(DataComponents.DYED_COLOR, newDyedColor);
        return result;
    }

    @Override
    public RecipeSerializer<DyeRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(List.of(this.target, this.dye));
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay.OnlyWithComponent dyesWithDyeComponent = new SlotDisplay.OnlyWithComponent(this.dye.display(), DataComponents.DYE);
        SlotDisplay targetDisplay = this.target.display();
        return List.of(new ShapelessCraftingRecipeDisplay(List.of(targetDisplay, dyesWithDyeComponent), new SlotDisplay.DyedSlotDemo(dyesWithDyeComponent, targetDisplay), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}

