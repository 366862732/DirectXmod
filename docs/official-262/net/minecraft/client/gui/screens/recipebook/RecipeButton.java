/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SlotSelectTime;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

@Environment(value=EnvType.CLIENT)
public class RecipeButton
extends AbstractWidget {
    private static final Identifier SLOT_MANY_CRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_many_craftable");
    private static final Identifier SLOT_CRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier SLOT_MANY_UNCRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_many_uncraftable");
    private static final Identifier SLOT_UNCRAFTABLE_SPRITE = Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final float ANIMATION_TIME = 15.0f;
    private static final int BACKGROUND_SIZE = 25;
    private static final Component MORE_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.moreRecipes");
    private RecipeCollection collection = RecipeCollection.EMPTY;
    private List<ResolvedEntry> selectedEntries = List.of();
    private boolean allRecipesHaveSameResultDisplay;
    private final SlotSelectTime slotSelectTime;
    private float animationTime;

    public RecipeButton(SlotSelectTime slotSelectTime) {
        super(0, 0, 25, 25, CommonComponents.EMPTY);
        this.slotSelectTime = slotSelectTime;
    }

    public void init(RecipeCollection collection, boolean isFiltering, RecipeBookPage page, ContextMap resolutionContext) {
        this.collection = collection;
        List<RecipeDisplayEntry> fittingRecipes = collection.getSelectedRecipes(isFiltering ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY);
        this.selectedEntries = fittingRecipes.stream().map(entry -> new ResolvedEntry(entry.id(), entry.resultItems(resolutionContext))).toList();
        this.allRecipesHaveSameResultDisplay = RecipeButton.allRecipesHaveSameResultDisplay(this.selectedEntries);
        List<RecipeDisplayId> newlyShownRecipes = fittingRecipes.stream().map(RecipeDisplayEntry::id).filter(page.getRecipeBook()::willHighlight).toList();
        if (!newlyShownRecipes.isEmpty()) {
            newlyShownRecipes.forEach(page::recipeShown);
            this.animationTime = 15.0f;
        }
    }

    private static boolean allRecipesHaveSameResultDisplay(List<ResolvedEntry> entries) {
        Iterator itemsIterator = entries.stream().flatMap(e -> e.displayItems().stream()).iterator();
        if (!itemsIterator.hasNext()) {
            return true;
        }
        ItemStack firstItem = (ItemStack)itemsIterator.next();
        while (itemsIterator.hasNext()) {
            ItemStack nextItem = (ItemStack)itemsIterator.next();
            if (ItemStack.isSameItemSameComponents(firstItem, nextItem)) continue;
            return false;
        }
        return true;
    }

    public RecipeCollection getCollection() {
        return this.collection;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean shouldAnimate;
        Identifier sprite = this.collection.hasCraftable() ? (this.hasMultipleRecipes() ? SLOT_MANY_CRAFTABLE_SPRITE : SLOT_CRAFTABLE_SPRITE) : (this.hasMultipleRecipes() ? SLOT_MANY_UNCRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE);
        boolean bl = shouldAnimate = this.animationTime > 0.0f;
        if (shouldAnimate) {
            float squeeze = 1.0f + 0.1f * (float)Math.sin(this.animationTime / 15.0f * (float)Math.PI);
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12));
            graphics.pose().scale(squeeze, squeeze);
            graphics.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)));
            this.animationTime -= a;
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
        ItemStack currentItemStack = this.getDisplayStack();
        int offset = 4;
        if (this.hasMultipleRecipes() && this.allRecipesHaveSameResultDisplay) {
            graphics.item(currentItemStack, this.getX() + offset + 1, this.getY() + offset + 1, 0);
            --offset;
        }
        graphics.fakeItem(currentItemStack, this.getX() + offset, this.getY() + offset);
        if (shouldAnimate) {
            graphics.pose().popMatrix();
        }
    }

    private boolean hasMultipleRecipes() {
        return this.selectedEntries.size() > 1;
    }

    public boolean isOnlyOption() {
        return this.selectedEntries.size() == 1;
    }

    public RecipeDisplayId getCurrentRecipe() {
        int index = this.slotSelectTime.currentIndex() % this.selectedEntries.size();
        return this.selectedEntries.get((int)index).id;
    }

    public ItemStack getDisplayStack() {
        int currentIndex = this.slotSelectTime.currentIndex();
        int entryCount = this.selectedEntries.size();
        int offsetIndex = currentIndex / entryCount;
        int entryIndex = currentIndex - entryCount * offsetIndex;
        return this.selectedEntries.get(entryIndex).selectItem(offsetIndex);
    }

    public List<Component> getTooltipText(ItemStack displayStack) {
        ArrayList<Component> texts = new ArrayList<Component>(Screen.getTooltipFromItem(Minecraft.getInstance(), displayStack));
        if (this.hasMultipleRecipes()) {
            texts.add(MORE_RECIPES_TOOLTIP);
        }
        return texts;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, (Component)Component.translatable("narration.recipe", this.getDisplayStack().getHoverName()));
        if (this.hasMultipleRecipes()) {
            output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"), Component.translatable("narration.recipe.usage.more"));
        } else {
            output.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.button.usage.hovered"));
        }
    }

    @Override
    public int getWidth() {
        return 25;
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == 0 || buttonInfo.button() == 1;
    }

    @Environment(value=EnvType.CLIENT)
    private record ResolvedEntry(RecipeDisplayId id, List<ItemStack> displayItems) {
        public ItemStack selectItem(int index) {
            if (this.displayItems.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int offset = index % this.displayItems.size();
            return this.displayItems.get(offset);
        }
    }
}

