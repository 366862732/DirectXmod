/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.gui.screens;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class RealmsPopups {
    private static final int COLOR_INFO = 8226750;
    private static final Component INFO = Component.translatable("mco.info").withColor(8226750);
    private static final Component WARNING = Component.translatable("mco.warning").withColor(-65536);

    public static PopupScreen customPopupScreen(Screen backgroundScreen, Component popupTitle, Component popupMessage, Consumer<PopupScreen> onContinue) {
        return new PopupScreen.Builder(backgroundScreen, popupTitle).addMessage(popupMessage).addButton(CommonComponents.GUI_CONTINUE, onContinue).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build();
    }

    public static PopupScreen infoPopupScreen(Screen backgroundScreen, Component popupMessage, Consumer<PopupScreen> onContinue) {
        return new PopupScreen.Builder(backgroundScreen, INFO).addMessage(popupMessage).addButton(CommonComponents.GUI_CONTINUE, onContinue).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build();
    }

    public static PopupScreen warningPopupScreen(Screen backgroundScreen, Component popupMessage, Consumer<PopupScreen> onContinue) {
        return new PopupScreen.Builder(backgroundScreen, WARNING).addMessage(popupMessage).addButton(CommonComponents.GUI_CONTINUE, onContinue).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build();
    }

    public static PopupScreen warningAcknowledgePopupScreen(Screen backgroundScreen, Component popupMessage, Consumer<PopupScreen> onContinue) {
        return new PopupScreen.Builder(backgroundScreen, WARNING).addMessage(popupMessage).addButton(CommonComponents.GUI_OK, onContinue).build();
    }
}

