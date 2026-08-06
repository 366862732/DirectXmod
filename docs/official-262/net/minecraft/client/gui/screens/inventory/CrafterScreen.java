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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class CrafterScreen
extends AbstractContainerScreen<CrafterMenu> {
    private static final Identifier DISABLED_SLOT_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
    private static final Identifier POWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
    private static final Identifier UNPOWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
    private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
    private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
    private final Player player;

    public CrafterScreen(CrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.player = inventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot instanceof CrafterSlot && !slot.hasItem() && !this.player.isSpectator()) {
            switch (containerInput) {
                case PICKUP: {
                    if (((CrafterMenu)this.menu).isSlotDisabled(slotId)) {
                        this.enableSlot(slotId);
                        break;
                    }
                    if (!((CrafterMenu)this.menu).getCarried().isEmpty()) break;
                    this.disableSlot(slotId);
                    break;
                }
                case SWAP: {
                    ItemStack playerInventoryItem = this.player.getInventory().getItem(buttonNum);
                    if (!((CrafterMenu)this.menu).isSlotDisabled(slotId) || playerInventoryItem.isEmpty()) break;
                    this.enableSlot(slotId);
                }
            }
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    private void enableSlot(int slotId) {
        this.updateSlotState(slotId, true);
    }

    private void disableSlot(int slotId) {
        this.updateSlotState(slotId, false);
    }

    private void updateSlotState(int slotId, boolean enabled) {
        ((CrafterMenu)this.menu).setSlotState(slotId, enabled);
        super.handleSlotStateChanged(slotId, ((CrafterMenu)this.menu).containerId, enabled);
        float pitch = enabled ? 1.0f : 0.75f;
        this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, pitch);
    }

    @Override
    public void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (slot instanceof CrafterSlot) {
            CrafterSlot crafterSlot = (CrafterSlot)slot;
            if (((CrafterMenu)this.menu).isSlotDisabled(slot.index)) {
                this.extractDisabledSlot(graphics, crafterSlot);
            } else {
                super.extractSlot(graphics, slot, mouseX, mouseY);
            }
            int x0 = this.leftPos + crafterSlot.x - 2;
            int y0 = this.topPos + crafterSlot.y - 2;
            if (mouseX > x0 && mouseY > y0 && mouseX < x0 + 19 && mouseY < y0 + 19) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        } else {
            super.extractSlot(graphics, slot, mouseX, mouseY);
        }
    }

    private void extractDisabledSlot(GuiGraphicsExtractor graphics, CrafterSlot cs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_LOCATION_SPRITE, cs.x - 1, cs.y - 1, 18, 18);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractRedstone(graphics);
        if (this.hoveredSlot instanceof CrafterSlot && !((CrafterMenu)this.menu).isSlotDisabled(this.hoveredSlot.index) && ((CrafterMenu)this.menu).getCarried().isEmpty() && !this.hoveredSlot.hasItem() && !this.player.isSpectator()) {
            graphics.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, mouseX, mouseY);
        }
    }

    private void extractRedstone(GuiGraphicsExtractor graphics) {
        int xo = this.width / 2 + 9;
        int yo = this.height / 2 - 48;
        Identifier redstoneArrowTexture = ((CrafterMenu)this.menu).isPowered() ? POWERED_REDSTONE_LOCATION_SPRITE : UNPOWERED_REDSTONE_LOCATION_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, redstoneArrowTexture, xo, yo, 16, 16);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, xo, yo, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }
}

