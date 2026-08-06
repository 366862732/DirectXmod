/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.item.component.ResolvableProfile;

@Environment(value=EnvType.CLIENT)
public class PlayerFaceWidget
extends AbstractWidget {
    private final ResolvableProfile skinProfile;

    public PlayerFaceWidget(int size, ResolvableProfile skinProfile) {
        super(0, 0, size, size, CommonComponents.EMPTY);
        this.skinProfile = skinProfile;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        PlayerFaceExtractor.extractRenderState(graphics, this.skinProfile, this.getX(), this.getY(), this.getWidth());
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}

