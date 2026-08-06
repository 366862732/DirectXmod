/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.math.Fraction
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.inventory.tooltip;

import com.mojang.serialization.DataResult;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ClientBundleTooltip
implements ClientTooltipComponent {
    private static final Identifier PROGRESSBAR_BORDER_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
    private static final Identifier PROGRESSBAR_FILL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
    private static final Identifier PROGRESSBAR_FULL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
    private static final Identifier SLOT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final int SLOT_MARGIN = 4;
    private static final int SLOT_SIZE = 24;
    private static final int GRID_WIDTH = 96;
    private static final int PROGRESSBAR_HEIGHT = 13;
    private static final int PROGRESSBAR_WIDTH = 96;
    private static final int PROGRESSBAR_BORDER = 1;
    private static final int PROGRESSBAR_FILL_MAX = 94;
    private static final int PROGRESSBAR_MARGIN_Y = 4;
    private static final Component BUNDLE_FULL_TEXT = Component.translatable("item.minecraft.bundle.full");
    private static final Component BUNDLE_EMPTY_TEXT = Component.translatable("item.minecraft.bundle.empty");
    private static final Component BUNDLE_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");
    private final BundleContents contents;

    public ClientBundleTooltip(BundleContents contents) {
        this.contents = contents;
    }

    @Override
    public int getHeight(Font font) {
        return this.contents.isEmpty() ? ClientBundleTooltip.getEmptyBundleBackgroundHeight(font) : this.backgroundHeight();
    }

    @Override
    public int getWidth(Font font) {
        return 96;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    private static int getEmptyBundleBackgroundHeight(Font font) {
        return ClientBundleTooltip.getEmptyBundleDescriptionTextHeight(font) + 13 + 8;
    }

    private int backgroundHeight() {
        return this.itemGridHeight() + 13 + 8;
    }

    private int itemGridHeight() {
        return this.gridSizeY() * 24;
    }

    private static int getContentXOffset(int tooltipWidth) {
        return (tooltipWidth - 96) / 2;
    }

    private int gridSizeY() {
        return Mth.positiveCeilDiv(this.slotCount(), 4);
    }

    private int slotCount() {
        return Math.min(12, this.contents.size());
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        DataResult<Fraction> weight = this.contents.weight();
        if (!weight.isError()) {
            if (this.contents.isEmpty()) {
                ClientBundleTooltip.extractEmptyBundleTooltip(font, x, y, w, h, graphics);
            } else {
                this.extractBundleWithItemsTooltip(font, x, y, w, h, graphics, (Fraction)weight.getOrThrow());
            }
        }
    }

    private static void extractEmptyBundleTooltip(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int left = x + ClientBundleTooltip.getContentXOffset(w);
        ClientBundleTooltip.extractEmptyBundleDescriptionText(left, y, font, graphics);
        ClientBundleTooltip.extractProgressbar(left, y + ClientBundleTooltip.getEmptyBundleDescriptionTextHeight(font) + 4, font, graphics, Fraction.ZERO);
    }

    private void extractBundleWithItemsTooltip(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics, Fraction weight) {
        boolean isOverflowing = this.contents.size() > 12;
        List<ItemStackTemplate> shownItems = this.getShownItems(this.contents.getNumberOfItemsToShow());
        int xStartPos = x + ClientBundleTooltip.getContentXOffset(w) + 96;
        int yStartPos = y + this.gridSizeY() * 24;
        int slotNumber = 1;
        for (int rowNumber = 1; rowNumber <= this.gridSizeY(); ++rowNumber) {
            for (int columnNumber = 1; columnNumber <= 4; ++columnNumber) {
                int drawX = xStartPos - columnNumber * 24;
                int drawY = yStartPos - rowNumber * 24;
                if (ClientBundleTooltip.shouldRenderSurplusText(isOverflowing, columnNumber, rowNumber)) {
                    ClientBundleTooltip.extractCount(drawX, drawY, this.getAmountOfHiddenItems(shownItems), font, graphics);
                    continue;
                }
                if (!ClientBundleTooltip.shouldRenderItemSlot(shownItems, slotNumber)) continue;
                this.extractSlot(slotNumber, drawX, drawY, shownItems, slotNumber, font, graphics);
                ++slotNumber;
            }
        }
        this.extractSelectedItemTooltip(font, graphics, x, y, w);
        ClientBundleTooltip.extractProgressbar(x + ClientBundleTooltip.getContentXOffset(w), y + this.itemGridHeight() + 4, font, graphics, weight);
    }

    private List<ItemStackTemplate> getShownItems(int amountOfItemsToShow) {
        int lastToDisplay = Math.min(this.contents.size(), amountOfItemsToShow);
        return this.contents.items().subList(0, lastToDisplay);
    }

    private static boolean shouldRenderSurplusText(boolean isOverflowing, int column, int row) {
        return isOverflowing && column * row == 1;
    }

    private static boolean shouldRenderItemSlot(List<? extends ItemInstance> shownItems, int slotNumber) {
        return shownItems.size() >= slotNumber;
    }

    private int getAmountOfHiddenItems(List<ItemStackTemplate> shownItems) {
        return this.contents.items().stream().skip(shownItems.size()).mapToInt(ItemInstance::count).sum();
    }

    private void extractSlot(int slotNumber, int drawX, int drawY, List<ItemStackTemplate> shownItems, int slotIndex, Font font, GuiGraphicsExtractor graphics) {
        int itemVisualOrderIndex = shownItems.size() - slotNumber;
        boolean hasHighlight = itemVisualOrderIndex == this.contents.getSelectedItemIndex();
        ItemStack item = shownItems.get(itemVisualOrderIndex).create();
        if (hasHighlight) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, drawX, drawY, 24, 24);
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, drawX, drawY, 24, 24);
        }
        graphics.item(item, drawX + 4, drawY + 4, slotIndex);
        graphics.itemDecorations(font, item, drawX + 4, drawY + 4);
        if (hasHighlight) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, drawX, drawY, 24, 24);
        }
    }

    private static void extractCount(int drawX, int drawY, int hiddenItemCount, Font font, GuiGraphicsExtractor graphics) {
        graphics.centeredText(font, "+" + hiddenItemCount, drawX + 12, drawY + 10, -1);
    }

    private void extractSelectedItemTooltip(Font font, GuiGraphicsExtractor graphics, int x, int y, int w) {
        ItemStackTemplate selectedItem = this.contents.getSelectedItem();
        if (selectedItem != null) {
            ItemStack itemStack = selectedItem.create();
            Component selectedItemName = itemStack.getStyledHoverName();
            int textWidth = font.width(selectedItemName.getVisualOrderText());
            int centerTooltip = x + w / 2 - 12;
            ClientTooltipComponent selectedItemNameTooltip = ClientTooltipComponent.create(selectedItemName.getVisualOrderText());
            graphics.tooltip(font, List.of(selectedItemNameTooltip), centerTooltip - textWidth / 2, y - 15, DefaultTooltipPositioner.INSTANCE, itemStack.get(DataComponents.TOOLTIP_STYLE));
        }
    }

    private static void extractProgressbar(int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction weight) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ClientBundleTooltip.getProgressBarTexture(weight), x + 1, y, ClientBundleTooltip.getProgressBarFill(weight), 13);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, x, y, 96, 13);
        Component progressBarFillText = ClientBundleTooltip.getProgressBarFillText(weight);
        if (progressBarFillText != null) {
            graphics.centeredText(font, progressBarFillText, x + 48, y + 3, -1);
        }
    }

    private static void extractEmptyBundleDescriptionText(int x, int y, Font font, GuiGraphicsExtractor graphics) {
        graphics.textWithWordWrap(font, BUNDLE_EMPTY_DESCRIPTION, x, y, 96, -5592406);
    }

    private static int getEmptyBundleDescriptionTextHeight(Font font) {
        return font.split(BUNDLE_EMPTY_DESCRIPTION, 96).size() * font.lineHeight;
    }

    private static int getProgressBarFill(Fraction weight) {
        return Mth.clamp(Mth.mulAndTruncate(weight, 94), 0, 94);
    }

    private static Identifier getProgressBarTexture(Fraction weight) {
        return weight.compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE;
    }

    private static @Nullable Component getProgressBarFillText(Fraction weight) {
        if (weight.compareTo(Fraction.ZERO) == 0) {
            return BUNDLE_EMPTY_TEXT;
        }
        if (weight.compareTo(Fraction.ONE) >= 0) {
            return BUNDLE_FULL_TEXT;
        }
        return null;
    }
}

