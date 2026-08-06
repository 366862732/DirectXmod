/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;

@Environment(value=EnvType.CLIENT)
public class SkinCustomizationScreen
extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.skinCustomisation.title");

    public SkinCustomizationScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        ArrayList<AbstractWidget> widgets = new ArrayList<AbstractWidget>();
        for (PlayerModelPart part : PlayerModelPart.values()) {
            widgets.add(CycleButton.onOffBuilder(this.options.isModelPartEnabled(part)).create(part.getName(), (button, value) -> this.options.setModelPart(part, (boolean)value)));
        }
        widgets.add(this.options.mainHand().createButton(this.options));
        this.list.addSmall(widgets);
    }
}

