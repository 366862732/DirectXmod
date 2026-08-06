/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;

@Environment(value=EnvType.CLIENT)
public class RealmsGenericErrorScreen
extends RealmsScreen {
    private static final Component GENERIC_TITLE = Component.translatable("mco.errorMessage.generic");
    private final Screen nextScreen;
    private final Component detail;
    private MultiLineLabel splitDetail = MultiLineLabel.EMPTY;

    public RealmsGenericErrorScreen(RealmsServiceException realmsServiceException, Screen nextScreen) {
        this(ErrorMessage.forServiceError(realmsServiceException), nextScreen);
    }

    public RealmsGenericErrorScreen(Component message, Screen nextScreen) {
        this(new ErrorMessage(GENERIC_TITLE, message), nextScreen);
    }

    public RealmsGenericErrorScreen(Component title, Component message, Screen nextScreen) {
        this(new ErrorMessage(title, message), nextScreen);
    }

    private RealmsGenericErrorScreen(ErrorMessage message, Screen nextScreen) {
        super(message.title);
        this.nextScreen = nextScreen;
        this.detail = ComponentUtils.mergeStyles(message.detail, Style.EMPTY.withColor(-2142128));
    }

    @Override
    public void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 52, 200, 20).build());
        this.splitDetail = MultiLineLabel.create(this.font, this.detail, this.width * 3 / 4);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.nextScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.detail);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 80, -1);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        this.splitDetail.visitLines(TextAlignment.CENTER, this.width / 2, 100, this.minecraft.font.lineHeight, textRenderer);
    }

    @Environment(value=EnvType.CLIENT)
    private record ErrorMessage(Component title, Component detail) {
        private static ErrorMessage forServiceError(RealmsServiceException realmsServiceException) {
            RealmsError errorDetails = realmsServiceException.realmsError;
            return new ErrorMessage(Component.translatable("mco.errorMessage.realmsService.realmsError", errorDetails.errorCode()), errorDetails.errorMessage());
        }
    }
}

