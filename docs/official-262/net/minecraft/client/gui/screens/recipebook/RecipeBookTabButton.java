/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.recipebook;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

@Environment(value=EnvType.CLIENT)
public class RecipeBookTabButton
extends ImageButton {
    private static final WidgetSprites SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("recipe_book/tab"), Identifier.withDefaultNamespace("recipe_book/tab_selected"));
    public static final int WIDTH = 35;
    public static final int HEIGHT = 27;
    private final RecipeBookComponent.TabInfo tabInfo;
    private static final float ANIMATION_TIME = 15.0f;
    private float animationTime;
    private boolean selected = false;

    public RecipeBookTabButton(int x, int y, RecipeBookComponent.TabInfo tabInfo, Button.OnPress onPress) {
        super(x, y, 35, 27, SPRITES, onPress);
        this.tabInfo = tabInfo;
    }

    public void startAnimation(ClientRecipeBook recipeBook, boolean isFiltering) {
        RecipeCollection.CraftableStatus recipesToShow = isFiltering ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY;
        List<RecipeCollection> collection = recipeBook.getCollection(this.tabInfo.category());
        for (RecipeCollection recipeCollection : collection) {
            for (RecipeDisplayEntry recipe : recipeCollection.getSelectedRecipes(recipesToShow)) {
                if (!recipeBook.willHighlight(recipe.id())) continue;
                this.animationTime = 15.0f;
                return;
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.animationTime > 0.0f) {
            float squeeze = 1.0f + 0.1f * (float)Math.sin(this.animationTime / 15.0f * (float)Math.PI);
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12));
            graphics.pose().scale(1.0f, squeeze);
            graphics.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)));
        }
        Identifier sprite = this.sprites.get(true, this.selected);
        int xPos = this.getX();
        if (this.selected) {
            xPos -= 2;
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xPos, this.getY(), this.width, this.height);
        this.extractIcon(graphics);
        if (this.animationTime > 0.0f) {
            graphics.pose().popMatrix();
            this.animationTime -= a;
        }
    }

    @Override
    protected void handleCursor(GuiGraphicsExtractor graphics) {
        if (!this.selected) {
            super.handleCursor(graphics);
        }
    }

    private void extractIcon(GuiGraphicsExtractor graphics) {
        int moveLeft;
        int n = moveLeft = this.selected ? -2 : 0;
        if (this.tabInfo.secondaryIcon().isPresent()) {
            graphics.fakeItem(this.tabInfo.primaryIcon(), this.getX() + 3 + moveLeft, this.getY() + 5);
            graphics.fakeItem(this.tabInfo.secondaryIcon().get(), this.getX() + 14 + moveLeft, this.getY() + 5);
        } else {
            graphics.fakeItem(this.tabInfo.primaryIcon(), this.getX() + 9 + moveLeft, this.getY() + 5);
        }
    }

    public ExtendedRecipeBookCategory getCategory() {
        return this.tabInfo.category();
    }

    public boolean updateVisibility(ClientRecipeBook book) {
        List<RecipeCollection> collections = book.getCollection(this.tabInfo.category());
        this.visible = false;
        for (RecipeCollection collection : collections) {
            if (!collection.hasAnySelected()) continue;
            this.visible = true;
            break;
        }
        return this.visible;
    }

    public void select() {
        this.selected = true;
    }

    public void unselect() {
        this.selected = false;
    }
}

