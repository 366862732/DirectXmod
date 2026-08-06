/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SlotSelectTime;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class OverlayRecipeComponent
implements GuiEventListener,
Renderable {
    private static final Identifier OVERLAY_RECIPE_SPRITE = Identifier.withDefaultNamespace("recipe_book/overlay_recipe");
    private static final int MAX_ROW = 4;
    private static final int MAX_ROW_LARGE = 5;
    private static final float ITEM_RENDER_SCALE = 0.375f;
    public static final int BUTTON_SIZE = 25;
    private final List<OverlayRecipeButton> recipeButtons = Lists.newArrayList();
    private boolean isVisible;
    private int x;
    private int y;
    private RecipeCollection collection = RecipeCollection.EMPTY;
    private @Nullable RecipeDisplayId lastRecipeClicked;
    private final SlotSelectTime slotSelectTime;
    private final boolean isFurnaceMenu;

    public OverlayRecipeComponent(SlotSelectTime slotSelectTime, boolean isFurnaceMenu) {
        this.slotSelectTime = slotSelectTime;
        this.isFurnaceMenu = isFurnaceMenu;
    }

    public void init(RecipeCollection collection, ContextMap context, boolean isFiltering, int buttonX, int buttonY, int centerX, int centerY, float buttonWidth) {
        float maxTopPos;
        float topPos;
        float maxBottomPos;
        float bottomPos;
        float maxLeftPos;
        this.collection = collection;
        List<RecipeDisplayEntry> craftable = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
        List unCraftable = isFiltering ? Collections.emptyList() : collection.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);
        int craftables = craftable.size();
        int total = craftables + unCraftable.size();
        int maxRow = total <= 16 ? 4 : 5;
        int rows = (int)Math.ceil((float)total / (float)maxRow);
        this.x = buttonX;
        this.y = buttonY;
        float rightPos = this.x + Math.min(total, maxRow) * 25;
        if (rightPos > (maxLeftPos = (float)(centerX + 50))) {
            this.x = (int)((float)this.x - buttonWidth * (float)((int)((rightPos - maxLeftPos) / buttonWidth)));
        }
        if ((bottomPos = (float)(this.y + rows * 25)) > (maxBottomPos = (float)(centerY + 50))) {
            this.y = (int)((float)this.y - buttonWidth * (float)Mth.ceil((bottomPos - maxBottomPos) / buttonWidth));
        }
        if ((topPos = (float)this.y) < (maxTopPos = (float)(centerY - 100))) {
            this.y = (int)((float)this.y - buttonWidth * (float)Mth.ceil((topPos - maxTopPos) / buttonWidth));
        }
        this.isVisible = true;
        this.recipeButtons.clear();
        for (int i = 0; i < total; ++i) {
            boolean canCraft = i < craftables;
            RecipeDisplayEntry recipe = canCraft ? craftable.get(i) : (RecipeDisplayEntry)unCraftable.get(i - craftables);
            int x = this.x + 4 + 25 * (i % maxRow);
            int y = this.y + 5 + 25 * (i / maxRow);
            if (this.isFurnaceMenu) {
                this.recipeButtons.add(new OverlaySmeltingRecipeButton(this, x, y, recipe.id(), recipe.display(), context, canCraft));
                continue;
            }
            this.recipeButtons.add(new OverlayCraftingRecipeButton(this, x, y, recipe.id(), recipe.display(), context, canCraft));
        }
        this.lastRecipeClicked = null;
    }

    public RecipeCollection getRecipeCollection() {
        return this.collection;
    }

    public @Nullable RecipeDisplayId getLastRecipeClicked() {
        return this.lastRecipeClicked;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }
        for (OverlayRecipeButton recipeButton : this.recipeButtons) {
            if (!recipeButton.mouseClicked(event, doubleClick)) continue;
            this.lastRecipeClicked = recipeButton.recipe;
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.isVisible) {
            return;
        }
        int maxRow = this.recipeButtons.size() <= 16 ? 4 : 5;
        int width = Math.min(this.recipeButtons.size(), maxRow);
        int height = Mth.ceil((float)this.recipeButtons.size() / (float)maxRow);
        int border = 4;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OVERLAY_RECIPE_SPRITE, this.x, this.y, width * 25 + 8, height * 25 + 8);
        for (OverlayRecipeButton component : this.recipeButtons) {
            component.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    private class OverlaySmeltingRecipeButton
    extends OverlayRecipeButton {
        private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay");
        private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
        private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
        private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/furnace_overlay_disabled_highlighted");

        public OverlaySmeltingRecipeButton(OverlayRecipeComponent overlayRecipeComponent, int x, int y, RecipeDisplayId id, RecipeDisplay recipe, ContextMap context, boolean isCraftable) {
            Objects.requireNonNull(overlayRecipeComponent);
            super(overlayRecipeComponent, x, y, id, isCraftable, OverlaySmeltingRecipeButton.calculateIngredientsPositions(recipe, context));
        }

        private static List<OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay recipe, ContextMap context) {
            FurnaceRecipeDisplay furnaceRecipe;
            List<ItemStack> items;
            if (recipe instanceof FurnaceRecipeDisplay && !(items = (furnaceRecipe = (FurnaceRecipeDisplay)recipe).ingredient().resolveForStacks(context)).isEmpty()) {
                return List.of(OverlaySmeltingRecipeButton.createGridPos(1, 1, items));
            }
            return List.of();
        }

        @Override
        protected Identifier getSprite(boolean isCraftable) {
            if (isCraftable) {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
            }
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class OverlayCraftingRecipeButton
    extends OverlayRecipeButton {
        private static final Identifier ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay");
        private static final Identifier HIGHLIGHTED_ENABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
        private static final Identifier DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
        private static final Identifier HIGHLIGHTED_DISABLED_SPRITE = Identifier.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted");
        private static final int GRID_WIDTH = 3;
        private static final int GRID_HEIGHT = 3;

        public OverlayCraftingRecipeButton(OverlayRecipeComponent overlayRecipeComponent, int x, int y, RecipeDisplayId id, RecipeDisplay recipe, ContextMap context, boolean isCraftable) {
            Objects.requireNonNull(overlayRecipeComponent);
            super(overlayRecipeComponent, x, y, id, isCraftable, OverlayCraftingRecipeButton.calculateIngredientsPositions(recipe, context));
        }

        private static List<OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay recipe, ContextMap context) {
            ArrayList<OverlayRecipeButton.Pos> result = new ArrayList<OverlayRecipeButton.Pos>();
            RecipeDisplay recipeDisplay = recipe;
            Objects.requireNonNull(recipeDisplay);
            RecipeDisplay recipeDisplay2 = recipeDisplay;
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ShapedCraftingRecipeDisplay.class, ShapelessCraftingRecipeDisplay.class}, (RecipeDisplay)recipeDisplay2, n)) {
                case 0: {
                    ShapedCraftingRecipeDisplay shaped = (ShapedCraftingRecipeDisplay)recipeDisplay2;
                    PlaceRecipeHelper.placeRecipe(3, 3, shaped.width(), shaped.height(), shaped.ingredients(), (ingredient, gridIndex, gridXPos, gridYPos) -> {
                        List<ItemStack> items = ingredient.resolveForStacks(context);
                        if (!items.isEmpty()) {
                            result.add(OverlayCraftingRecipeButton.createGridPos(gridXPos, gridYPos, items));
                        }
                    });
                    break;
                }
                case 1: {
                    ShapelessCraftingRecipeDisplay shapeless = (ShapelessCraftingRecipeDisplay)recipeDisplay2;
                    List<SlotDisplay> ingredients = shapeless.ingredients();
                    for (int i = 0; i < ingredients.size(); ++i) {
                        List<ItemStack> items = ingredients.get(i).resolveForStacks(context);
                        if (items.isEmpty()) continue;
                        result.add(OverlayCraftingRecipeButton.createGridPos(i % 3, i / 3, items));
                    }
                    break;
                }
            }
            return result;
        }

        @Override
        protected Identifier getSprite(boolean isCraftable) {
            if (isCraftable) {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
            }
            return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private abstract class OverlayRecipeButton
    extends AbstractWidget {
        private final RecipeDisplayId recipe;
        private final boolean isCraftable;
        private final List<Pos> slots;
        final /* synthetic */ OverlayRecipeComponent this$0;

        public OverlayRecipeButton(OverlayRecipeComponent overlayRecipeComponent, int x, int y, RecipeDisplayId recipe, boolean isCraftable, List<Pos> slots) {
            OverlayRecipeComponent overlayRecipeComponent2 = overlayRecipeComponent;
            Objects.requireNonNull(overlayRecipeComponent2);
            this.this$0 = overlayRecipeComponent2;
            super(x, y, 24, 24, CommonComponents.EMPTY);
            this.slots = slots;
            this.recipe = recipe;
            this.isCraftable = isCraftable;
        }

        protected static Pos createGridPos(int gridXPos, int gridYPos, List<ItemStack> itemStacks) {
            return new Pos(3 + gridXPos * 7, 3 + gridYPos * 7, itemStacks);
        }

        protected abstract Identifier getSprite(boolean var1);

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(this.isCraftable), this.getX(), this.getY(), this.width, this.height);
            float gridPosX = this.getX() + 2;
            float gridPosY = this.getY() + 2;
            for (Pos pos : this.slots) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(gridPosX + (float)pos.x, gridPosY + (float)pos.y);
                graphics.pose().scale(0.375f, 0.375f);
                graphics.pose().translate(-8.0f, -8.0f);
                graphics.item(pos.selectIngredient(this.this$0.slotSelectTime.currentIndex()), 0, 0);
                graphics.pose().popMatrix();
            }
        }

        @Environment(value=EnvType.CLIENT)
        protected record Pos(int x, int y, List<ItemStack> ingredients) {
            public Pos {
                if (ingredients.isEmpty()) {
                    throw new IllegalArgumentException("Ingredient list must be non-empty");
                }
            }

            public ItemStack selectIngredient(int currentIndex) {
                return this.ingredients.get(currentIndex % this.ingredients.size());
            }
        }
    }
}

