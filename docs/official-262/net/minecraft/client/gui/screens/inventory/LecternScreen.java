/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.inventory;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class LecternScreen
extends BookViewScreen
implements MenuAccess<LecternMenu> {
    private static final int MENU_BUTTON_MARGIN = 4;
    private static final int MENU_BUTTON_SIZE = 98;
    private static final Component TAKE_BOOK_LABEL = Component.translatable("lectern.take_book");
    private final LecternMenu menu;
    private final ContainerListener listener = new ContainerListener(this){
        final /* synthetic */ LecternScreen this$0;
        {
            LecternScreen lecternScreen = this$0;
            Objects.requireNonNull(lecternScreen);
            this.this$0 = lecternScreen;
        }

        @Override
        public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
            this.this$0.bookChanged();
        }

        @Override
        public void dataChanged(AbstractContainerMenu container, int id, int value) {
            if (id == 0) {
                this.this$0.pageChanged();
            }
        }
    };

    public LecternScreen(LecternMenu menu, Inventory inventory, Component title) {
        this.menu = menu;
    }

    @Override
    public LecternMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();
        this.menu.addSlotListener(this.listener);
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        this.menu.removeSlotListener(this.listener);
    }

    @Override
    protected void createMenuControls() {
        if (this.minecraft.player.mayBuild()) {
            int buttonY = this.menuControlsTop();
            int middle = this.width / 2;
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).pos(middle - 98 - 2, buttonY).width(98).build());
            this.addRenderableWidget(Button.builder(TAKE_BOOK_LABEL, button -> this.sendButtonClick(3)).pos(middle + 2, buttonY).width(98).build());
        } else {
            super.createMenuControls();
        }
    }

    @Override
    protected void pageBack() {
        this.sendButtonClick(1);
    }

    @Override
    protected void pageForward() {
        this.sendButtonClick(2);
    }

    @Override
    protected boolean forcePage(int page) {
        if (page != this.menu.getPage()) {
            this.sendButtonClick(100 + page);
            return true;
        }
        return false;
    }

    private void sendButtonClick(int button) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void bookChanged() {
        ItemStack book = this.menu.getBook();
        this.setBookAccess(Objects.requireNonNullElse(BookViewScreen.BookAccess.fromItem(book), BookViewScreen.EMPTY_ACCESS));
    }

    private void pageChanged() {
        this.setPage(this.menu.getPage());
    }

    @Override
    protected void closeContainerOnServer() {
        this.minecraft.player.closeContainer();
    }
}

