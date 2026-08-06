/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.spectator;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.spectator.RootSpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuListener;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

@Environment(value=EnvType.CLIENT)
public class SpectatorMenu {
    private static final Identifier CLOSE_SPRITE = Identifier.withDefaultNamespace("spectator/close");
    private static final Identifier SCROLL_LEFT_SPRITE = Identifier.withDefaultNamespace("spectator/scroll_left");
    private static final Identifier SCROLL_RIGHT_SPRITE = Identifier.withDefaultNamespace("spectator/scroll_right");
    private static final SpectatorMenuItem CLOSE_ITEM = new CloseSpectatorItem();
    private static final SpectatorMenuItem SCROLL_LEFT = new ScrollMenuItem(-1, true);
    private static final SpectatorMenuItem SCROLL_RIGHT_ENABLED = new ScrollMenuItem(1, true);
    private static final SpectatorMenuItem SCROLL_RIGHT_DISABLED = new ScrollMenuItem(1, false);
    private static final int MAX_PER_PAGE = 8;
    private static final Component CLOSE_MENU_TEXT = Component.translatable("spectatorMenu.close");
    private static final Component PREVIOUS_PAGE_TEXT = Component.translatable("spectatorMenu.previous_page");
    private static final Component NEXT_PAGE_TEXT = Component.translatable("spectatorMenu.next_page");
    public static final SpectatorMenuItem EMPTY_SLOT = new SpectatorMenuItem(){

        @Override
        public void selectItem(SpectatorMenu menu) {
        }

        @Override
        public Component getName() {
            return CommonComponents.EMPTY;
        }

        @Override
        public void extractIcon(GuiGraphicsExtractor graphics, float brightness, float alpha) {
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    };
    private final SpectatorMenuListener listener;
    private SpectatorMenuCategory category = new RootSpectatorMenuCategory();
    private int selectedSlot = -1;
    private int page;

    public SpectatorMenu(SpectatorMenuListener listener) {
        this.listener = listener;
    }

    public SpectatorMenuItem getItem(int slot) {
        int index = slot + this.page * 6;
        if (this.page > 0 && slot == 0) {
            return SCROLL_LEFT;
        }
        if (slot == 7) {
            if (index < this.category.getItems().size()) {
                return SCROLL_RIGHT_ENABLED;
            }
            return SCROLL_RIGHT_DISABLED;
        }
        if (slot == 8) {
            return CLOSE_ITEM;
        }
        if (index < 0 || index >= this.category.getItems().size()) {
            return EMPTY_SLOT;
        }
        return (SpectatorMenuItem)MoreObjects.firstNonNull((Object)this.category.getItems().get(index), (Object)EMPTY_SLOT);
    }

    public List<SpectatorMenuItem> getItems() {
        ArrayList items = Lists.newArrayList();
        for (int i = 0; i <= 8; ++i) {
            items.add(this.getItem(i));
        }
        return items;
    }

    public SpectatorMenuItem getSelectedItem() {
        return this.getItem(this.selectedSlot);
    }

    public SpectatorMenuCategory getSelectedCategory() {
        return this.category;
    }

    public void selectSlot(int slot) {
        SpectatorMenuItem item = this.getItem(slot);
        if (item != EMPTY_SLOT) {
            if (this.selectedSlot == slot && item.isEnabled()) {
                item.selectItem(this);
            } else {
                this.selectedSlot = slot;
            }
        }
    }

    public void exit() {
        this.listener.onSpectatorMenuClosed(this);
    }

    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    public void selectCategory(SpectatorMenuCategory category) {
        this.category = category;
        this.selectedSlot = -1;
        this.page = 0;
    }

    public SpectatorPage getCurrentPage() {
        return new SpectatorPage(this.getItems(), this.selectedSlot);
    }

    @Environment(value=EnvType.CLIENT)
    private static class CloseSpectatorItem
    implements SpectatorMenuItem {
        private CloseSpectatorItem() {
        }

        @Override
        public void selectItem(SpectatorMenu menu) {
            menu.exit();
        }

        @Override
        public Component getName() {
            return CLOSE_MENU_TEXT;
        }

        @Override
        public void extractIcon(GuiGraphicsExtractor graphics, float brightness, float alpha) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CLOSE_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat(alpha, brightness, brightness, brightness));
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ScrollMenuItem
    implements SpectatorMenuItem {
        private final int direction;
        private final boolean enabled;

        public ScrollMenuItem(int direction, boolean enabled) {
            this.direction = direction;
            this.enabled = enabled;
        }

        @Override
        public void selectItem(SpectatorMenu menu) {
            menu.page += this.direction;
        }

        @Override
        public Component getName() {
            return this.direction < 0 ? PREVIOUS_PAGE_TEXT : NEXT_PAGE_TEXT;
        }

        @Override
        public void extractIcon(GuiGraphicsExtractor graphics, float brightness, float alpha) {
            int color = ARGB.colorFromFloat(alpha, brightness, brightness, brightness);
            if (this.direction < 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLL_LEFT_SPRITE, 0, 0, 16, 16, color);
            } else {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLL_RIGHT_SPRITE, 0, 0, 16, 16, color);
            }
        }

        @Override
        public boolean isEnabled() {
            return this.enabled;
        }
    }
}

