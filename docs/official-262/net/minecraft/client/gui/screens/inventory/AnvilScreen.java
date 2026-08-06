/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class AnvilScreen
extends ItemCombinerScreen<AnvilMenu> {
    private static final Identifier TEXT_FIELD_SPRITE = Identifier.withDefaultNamespace("container/anvil/text_field");
    private static final Identifier TEXT_FIELD_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/anvil/text_field_disabled");
    private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/anvil/error");
    private static final Identifier ANVIL_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/anvil.png");
    private static final Component TOO_EXPENSIVE_TEXT = Component.translatable("container.repair.expensive");
    private EditBox name;
    private final Player player;

    public AnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ANVIL_LOCATION);
        this.player = inventory.player;
        this.titleLabelX = 60;
    }

    @Override
    protected void subInit() {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        this.name = new EditBox(this.font, xo + 62, yo + 24, 103, 12, Component.translatable("container.repair"));
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setInvertHighlightedTextColor(false);
        this.name.setBordered(false);
        this.name.setMaxLength(50);
        this.name.setResponder(this::onNameChanged);
        this.name.setValue("");
        this.addRenderableWidget(this.name);
        this.name.setEditable(((AnvilMenu)this.menu).getSlot(0).hasItem());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.minecraft.player.experienceDisplayStartTick = this.minecraft.player.tickCount;
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.name);
    }

    @Override
    public void resize(int width, int height) {
        String oldEdit = this.name.getValue();
        this.init(width, height);
        this.name.setValue(oldEdit);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (this.name.keyPressed(event) || this.name.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(event);
    }

    private void onNameChanged(String name) {
        Slot slot = ((AnvilMenu)this.menu).getSlot(0);
        if (!slot.hasItem()) {
            return;
        }
        String newName = name;
        if (!slot.getItem().has(DataComponents.CUSTOM_NAME) && newName.equals(slot.getItem().getHoverName().getString())) {
            newName = "";
        }
        if (((AnvilMenu)this.menu).setItemName(newName)) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket(newName));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        int cost = ((AnvilMenu)this.menu).getCost();
        if (cost > 0) {
            Component line;
            int color = -8323296;
            if (cost >= 40 && !this.minecraft.player.hasInfiniteMaterials()) {
                line = TOO_EXPENSIVE_TEXT;
                color = -40864;
            } else if (!((AnvilMenu)this.menu).getSlot(2).hasItem()) {
                line = null;
            } else {
                line = Component.translatable("container.repair.cost", cost);
                if (!((AnvilMenu)this.menu).getSlot(2).mayPickup(this.player)) {
                    color = -40864;
                }
            }
            if (line != null) {
                int tx = this.imageWidth - 8 - this.font.width(line) - 2;
                int ty = 69;
                graphics.fill(tx - 2, 67, this.imageWidth - 8, 79, 0x4F000000);
                graphics.text(this.font, line, tx, 69, color);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ((AnvilMenu)this.menu).getSlot(0).hasItem() ? TEXT_FIELD_SPRITE : TEXT_FIELD_DISABLED_SPRITE, this.leftPos + 59, this.topPos + 20, 110, 16);
    }

    @Override
    protected void extractErrorIcon(GuiGraphicsExtractor graphics, int xo, int yo) {
        if ((((AnvilMenu)this.menu).getSlot(0).hasItem() || ((AnvilMenu)this.menu).getSlot(1).hasItem()) && !((AnvilMenu)this.menu).getSlot(((AnvilMenu)this.menu).getResultSlot()).hasItem()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, xo + 99, yo + 45, 28, 21);
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
        if (slotIndex == 0) {
            this.name.setValue(itemStack.isEmpty() ? "" : itemStack.getHoverName().getString());
            this.name.setEditable(!itemStack.isEmpty());
            this.setFocused(this.name);
        }
    }
}

