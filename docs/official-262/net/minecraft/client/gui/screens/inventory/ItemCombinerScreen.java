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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public abstract class ItemCombinerScreen<T extends ItemCombinerMenu>
extends AbstractContainerScreen<T>
implements ContainerListener {
    private final Identifier menuResource;

    public ItemCombinerScreen(T menu, Inventory inventory, Component title, Identifier menuResource) {
        super(menu, inventory, title);
        this.menuResource = menuResource;
    }

    protected void subInit() {
    }

    @Override
    protected void init() {
        super.init();
        this.subInit();
        ((ItemCombinerMenu)this.menu).addSlotListener(this);
    }

    @Override
    public void removed() {
        super.removed();
        ((ItemCombinerMenu)this.menu).removeSlotListener(this);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.menuResource, this.leftPos, this.topPos, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
        this.extractErrorIcon(graphics, this.leftPos, this.topPos);
    }

    protected abstract void extractErrorIcon(GuiGraphicsExtractor var1, int var2, int var3);

    @Override
    public void dataChanged(AbstractContainerMenu container, int id, int value) {
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
    }
}

