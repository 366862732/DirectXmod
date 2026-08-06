/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.booleans.BooleanConsumer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;

@Environment(value=EnvType.CLIENT)
public class RealmsConfirmScreen
extends RealmsScreen {
    protected final BooleanConsumer callback;
    private final Component title1;
    private final Component title2;

    public RealmsConfirmScreen(BooleanConsumer callback, Component title1, Component title2) {
        super(GameNarrator.NO_TITLE);
        this.callback = callback;
        this.title1 = title1;
        this.title2 = title2;
    }

    @Override
    public void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_YES, button -> this.callback.accept(true)).bounds(this.width / 2 - 105, RealmsConfirmScreen.row(9), 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_NO, button -> this.callback.accept(false)).bounds(this.width / 2 + 5, RealmsConfirmScreen.row(9), 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.centeredText(this.font, this.title1, this.width / 2, RealmsConfirmScreen.row(3), -1);
        graphics.centeredText(this.font, this.title2, this.width / 2, RealmsConfirmScreen.row(5), -1);
    }
}

