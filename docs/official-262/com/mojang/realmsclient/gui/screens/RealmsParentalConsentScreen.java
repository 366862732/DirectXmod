/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RealmsParentalConsentScreen
extends RealmsScreen {
    private static final Component MESSAGE = Component.translatable("mco.account.privacy.information");
    private static final int SPACING = 15;
    private final LinearLayout layout = LinearLayout.vertical();
    private final Screen lastScreen;
    private @Nullable MultiLineTextWidget textWidget;

    public RealmsParentalConsentScreen(Screen lastScreen) {
        super(GameNarrator.NO_TITLE);
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        this.layout.spacing(15).defaultCellSetting().alignHorizontallyCenter();
        this.textWidget = new MultiLineTextWidget(MESSAGE, this.font).setCentered(true);
        this.layout.addChild(this.textWidget);
        LinearLayout buttonLayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        MutableComponent privacyInfo = Component.translatable("mco.account.privacy.info.button");
        buttonLayout.addChild(Button.builder(privacyInfo, ConfirmLinkScreen.confirmLink((Screen)this, CommonLinks.GDPR)).build());
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        RealmsParentalConsentScreen realmsParentalConsentScreen = this;
        this.layout.visitWidgets(x$0 -> realmsParentalConsentScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Override
    protected void repositionElements() {
        if (this.textWidget != null) {
            this.textWidget.setMaxWidth(this.width - 15);
        }
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public Component getNarrationMessage() {
        return MESSAGE;
    }
}

