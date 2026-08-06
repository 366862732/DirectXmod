/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@Environment(value=EnvType.CLIENT)
public class CyclingSlotBackground {
    private static final int ICON_CHANGE_TICK_RATE = 30;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TRANSITION_TICK_DURATION = 4;
    private final int slotIndex;
    private List<Identifier> icons = List.of();
    private int tick;
    private int iconIndex;

    public CyclingSlotBackground(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public void tick(List<Identifier> newIcons) {
        if (!this.icons.equals(newIcons)) {
            this.icons = newIcons;
            this.iconIndex = 0;
        }
        if (!this.icons.isEmpty() && ++this.tick % 30 == 0) {
            this.iconIndex = (this.iconIndex + 1) % this.icons.size();
        }
    }

    public void extractRenderState(AbstractContainerMenu menu, GuiGraphicsExtractor graphics, float a, int left, int top) {
        float alphaProgress;
        Slot slot = menu.getSlot(this.slotIndex);
        if (this.icons.isEmpty() || slot.hasItem()) {
            return;
        }
        boolean shouldTransition = this.icons.size() > 1 && this.tick >= 30;
        float f = alphaProgress = shouldTransition ? this.getIconTransitionTransparency(a) : 1.0f;
        if (alphaProgress < 1.0f) {
            int previousIconIndex = Math.floorMod(this.iconIndex - 1, this.icons.size());
            this.extractIcon(slot, this.icons.get(previousIconIndex), 1.0f - alphaProgress, graphics, left, top);
        }
        this.extractIcon(slot, this.icons.get(this.iconIndex), alphaProgress, graphics, left, top);
    }

    private void extractIcon(Slot slot, Identifier iconIdentifier, float alphaProgress, GuiGraphicsExtractor graphics, int left, int top) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconIdentifier, left + slot.x, top + slot.y, 16, 16, ARGB.white(alphaProgress));
    }

    private float getIconTransitionTransparency(float a) {
        float elapsedTransitionTime = (float)(this.tick % 30) + a;
        return Math.min(elapsedTransitionTime, 4.0f) / 4.0f;
    }
}

