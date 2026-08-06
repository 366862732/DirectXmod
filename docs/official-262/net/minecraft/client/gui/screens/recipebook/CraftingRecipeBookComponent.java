/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.recipebook;

import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

@Environment(value=EnvType.CLIENT)
public class CraftingRecipeBookComponent
extends RecipeBookComponent<AbstractCraftingMenu> {
    private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("recipe_book/filter_enabled"), Identifier.withDefaultNamespace("recipe_book/filter_disabled"), Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"), Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final Component ONLY_CRAFTABLES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.craftable");
    private static final List<RecipeBookComponent.TabInfo> TABS = List.of(new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.CRAFTING), new RecipeBookComponent.TabInfo(Items.IRON_AXE, Items.GOLDEN_SWORD, RecipeBookCategories.CRAFTING_EQUIPMENT), new RecipeBookComponent.TabInfo(Items.BRICKS, RecipeBookCategories.CRAFTING_BUILDING_BLOCKS), new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.APPLE, RecipeBookCategories.CRAFTING_MISC), new RecipeBookComponent.TabInfo(Items.REDSTONE, RecipeBookCategories.CRAFTING_REDSTONE));

    public CraftingRecipeBookComponent(AbstractCraftingMenu menu) {
        super(menu, TABS);
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return ((AbstractCraftingMenu)this.menu).getResultSlot() == slot || ((AbstractCraftingMenu)this.menu).getInputGridSlots().contains(slot);
    }

    private boolean canDisplay(RecipeDisplay display) {
        int gridWidth = ((AbstractCraftingMenu)this.menu).getGridWidth();
        int gridHeight = ((AbstractCraftingMenu)this.menu).getGridHeight();
        RecipeDisplay recipeDisplay = display;
        Objects.requireNonNull(recipeDisplay);
        RecipeDisplay recipeDisplay2 = recipeDisplay;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ShapedCraftingRecipeDisplay.class, ShapelessCraftingRecipeDisplay.class}, (RecipeDisplay)recipeDisplay2, n)) {
            case 0 -> {
                ShapedCraftingRecipeDisplay shaped = (ShapedCraftingRecipeDisplay)recipeDisplay2;
                if (gridWidth >= shaped.width() && gridHeight >= shaped.height()) {
                    yield true;
                }
                yield false;
            }
            case 1 -> {
                ShapelessCraftingRecipeDisplay shapeless = (ShapelessCraftingRecipeDisplay)recipeDisplay2;
                if (gridWidth * gridHeight >= shapeless.ingredients().size()) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context) {
        ghostSlots.setResult(((AbstractCraftingMenu)this.menu).getResultSlot(), context, recipe.result());
        RecipeDisplay recipeDisplay = recipe;
        Objects.requireNonNull(recipeDisplay);
        RecipeDisplay recipeDisplay2 = recipeDisplay;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ShapedCraftingRecipeDisplay.class, ShapelessCraftingRecipeDisplay.class}, (RecipeDisplay)recipeDisplay2, n)) {
            case 0: {
                ShapedCraftingRecipeDisplay shaped = (ShapedCraftingRecipeDisplay)recipeDisplay2;
                List<Slot> inputSlots = ((AbstractCraftingMenu)this.menu).getInputGridSlots();
                PlaceRecipeHelper.placeRecipe(((AbstractCraftingMenu)this.menu).getGridWidth(), ((AbstractCraftingMenu)this.menu).getGridHeight(), shaped.width(), shaped.height(), shaped.ingredients(), (ingredient, gridIndex, gridXPos, gridYPos) -> {
                    Slot slot = (Slot)inputSlots.get(gridIndex);
                    ghostSlots.setInput(slot, context, (SlotDisplay)ingredient);
                });
                break;
            }
            case 1: {
                ShapelessCraftingRecipeDisplay shapeless = (ShapelessCraftingRecipeDisplay)recipeDisplay2;
                List<Slot> inputSlots = ((AbstractCraftingMenu)this.menu).getInputGridSlots();
                int slotCount = Math.min(shapeless.ingredients().size(), inputSlots.size());
                for (int i = 0; i < slotCount; ++i) {
                    ghostSlots.setInput(inputSlots.get(i), context, shapeless.ingredients().get(i));
                }
                break;
            }
        }
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_BUTTON_SPRITES;
    }

    @Override
    protected Component getRecipeFilterName() {
        return ONLY_CRAFTABLES_TOOLTIP;
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents) {
        collection.selectRecipes(stackedContents, this::canDisplay);
    }
}

