/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

@Environment(value=EnvType.CLIENT)
public class StonecutterScreen
extends AbstractContainerScreen<StonecutterMenu> {
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
    private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
    private static final int SCROLLER_FULL_HEIGHT = 54;
    private static final int RECIPES_X = 52;
    private static final int RECIPES_Y = 14;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private boolean displayRecipes;

    public StonecutterScreen(StonecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        menu.registerUpdateListener(this::containerChanged);
        --this.titleLabelY;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, xo, yo, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
        int sy = (int)(41.0f * this.scrollOffs);
        Identifier sprite = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        int scrollerXStart = xo + 119;
        int scrollerYStart = yo + 15;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, scrollerXStart, scrollerYStart + sy, 12, 15);
        if (mouseX >= scrollerXStart && mouseY >= scrollerYStart && mouseX < scrollerXStart + 12 && mouseY < scrollerYStart + 54) {
            if (this.isScrollBarActive()) {
                graphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            } else {
                graphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        }
        int x = this.leftPos + 52;
        int y = this.topPos + 14;
        int endIndex = this.startIndex + 12;
        this.extractButtons(graphics, mouseX, mouseY, x, y, endIndex);
        this.extractRecipes(graphics, x, y, endIndex);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.displayRecipes) {
            int edgeLeft = this.leftPos + 52;
            int edgeTop = this.topPos + 14;
            int endIndex = this.startIndex + 12;
            SelectableRecipe.SingleInputSet<StonecutterRecipe> visibleRecipes = ((StonecutterMenu)this.menu).getVisibleRecipes();
            for (int index = this.startIndex; index < endIndex && index < visibleRecipes.size(); ++index) {
                int posIndex = index - this.startIndex;
                int itemLeft = edgeLeft + posIndex % 4 * 16;
                int itemRight = edgeTop + posIndex / 4 * 18 + 2;
                if (mouseX < itemLeft || mouseX >= itemLeft + 16 || mouseY < itemRight || mouseY >= itemRight + 18) continue;
                ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
                SlotDisplay buttonIcon = visibleRecipes.entries().get(index).recipe().optionDisplay();
                graphics.setTooltipForNextFrame(this.font, buttonIcon.resolveForFirstStack(context), mouseX, mouseY);
            }
        }
    }

    private void extractButtons(GuiGraphicsExtractor graphics, int xm, int ym, int x, int y, int endIndex) {
        for (int index = this.startIndex; index < endIndex && index < ((StonecutterMenu)this.menu).getNumberOfVisibleRecipes(); ++index) {
            int posIndex = index - this.startIndex;
            int posX = x + posIndex % 4 * 16;
            int row = posIndex / 4;
            int posY = y + row * 18 + 2;
            Identifier sprite = index == ((StonecutterMenu)this.menu).getSelectedRecipeIndex() ? RECIPE_SELECTED_SPRITE : (xm >= posX && ym >= posY && xm < posX + 16 && ym < posY + 18 ? RECIPE_HIGHLIGHTED_SPRITE : RECIPE_SPRITE);
            int textureY = posY - 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, posX, textureY, 16, 18);
            if (xm < posX || ym < textureY || xm >= posX + 16 || ym >= textureY + 18) continue;
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void extractRecipes(GuiGraphicsExtractor graphics, int x, int y, int endIndex) {
        SelectableRecipe.SingleInputSet<StonecutterRecipe> visibleRecipes = ((StonecutterMenu)this.menu).getVisibleRecipes();
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        for (int index = this.startIndex; index < endIndex && index < visibleRecipes.size(); ++index) {
            int posIndex = index - this.startIndex;
            int posX = x + posIndex % 4 * 16;
            int row = posIndex / 4;
            int posY = y + row * 18 + 2;
            SlotDisplay buttonIcon = visibleRecipes.entries().get(index).recipe().optionDisplay();
            graphics.item(buttonIcon.resolveForFirstStack(context), posX, posY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.displayRecipes) {
            int xo = this.leftPos + 52;
            int yo = this.topPos + 14;
            int endIndex = this.startIndex + 12;
            for (int index = this.startIndex; index < endIndex; ++index) {
                int posIndex = index - this.startIndex;
                double xx = event.x() - (double)(xo + posIndex % 4 * 16);
                double yy = event.y() - (double)(yo + posIndex / 4 * 18);
                if (!(xx >= 0.0) || !(yy >= 0.0) || !(xx < 16.0) || !(yy < 18.0) || !((StonecutterMenu)this.menu).clickMenuButton(this.minecraft.player, index)) continue;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                this.minecraft.gameMode.handleInventoryButtonClick(((StonecutterMenu)this.menu).containerId, index);
                return true;
            }
            xo = this.leftPos + 119;
            yo = this.topPos + 9;
            if (event.x() >= (double)xo && event.x() < (double)(xo + 12) && event.y() >= (double)yo && event.y() < (double)(yo + 54)) {
                this.scrolling = true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrolling && this.isScrollBarActive()) {
            int yscr = this.topPos + 14;
            int yscr2 = yscr + 54;
            this.scrollOffs = ((float)event.y() - (float)yscr - 7.5f) / ((float)(yscr2 - yscr) - 15.0f);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0f, 1.0f);
            this.startIndex = (int)((double)(this.scrollOffs * (float)this.getOffscreenRows()) + 0.5) * 4;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }
        if (this.isScrollBarActive()) {
            int offscreenRows = this.getOffscreenRows();
            float scrolledDelta = (float)scrollY / (float)offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - scrolledDelta, 0.0f, 1.0f);
            this.startIndex = (int)((double)(this.scrollOffs * (float)offscreenRows) + 0.5) * 4;
        }
        return true;
    }

    private boolean isScrollBarActive() {
        return this.displayRecipes && ((StonecutterMenu)this.menu).getNumberOfVisibleRecipes() > 12;
    }

    protected int getOffscreenRows() {
        return (((StonecutterMenu)this.menu).getNumberOfVisibleRecipes() + 4 - 1) / 4 - 3;
    }

    private void containerChanged() {
        this.displayRecipes = ((StonecutterMenu)this.menu).hasInputItem();
        this.scrollOffs = 0.0f;
        this.startIndex = 0;
    }
}

