/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GenericWaitingScreen
extends Screen {
    private static final int TITLE_Y = 80;
    private static final int MESSAGE_Y = 120;
    private static final int MESSAGE_MAX_WIDTH = 360;
    private final boolean showLoadingDots;
    private final Component messageText;
    private final Component buttonLabel;
    private final Runnable buttonCallback;
    private final boolean showButton;
    private final boolean closeOnEscape;
    private final MultiLineLabel message;
    private @Nullable Button button;
    private int disableButtonTicks;

    public static GenericWaitingScreen createWaitingWithoutButton(Component title, Component messageText) {
        return new GenericWaitingScreen(title, true, messageText, Component.empty(), () -> {}, 0, false, false);
    }

    public static GenericWaitingScreen createWaiting(Component title, Component buttonLabel, Runnable buttonCallback) {
        return new GenericWaitingScreen(title, true, Component.empty(), buttonLabel, buttonCallback, 0, true, false);
    }

    public static GenericWaitingScreen createCompleted(Component title, Component messageText, Component buttonLabel, Runnable buttonCallback) {
        return new GenericWaitingScreen(title, false, messageText, buttonLabel, buttonCallback, 20, true, true);
    }

    protected GenericWaitingScreen(Component title, boolean showLoadingDots, Component messageText, Component buttonLabel, Runnable buttonCallback, int disableButtonTicks, boolean showButton, boolean closeOnEscape) {
        super(title);
        this.showLoadingDots = showLoadingDots;
        this.messageText = messageText;
        this.buttonLabel = buttonLabel;
        this.buttonCallback = buttonCallback;
        this.disableButtonTicks = disableButtonTicks;
        this.showButton = showButton;
        this.closeOnEscape = closeOnEscape;
        this.message = MultiLineLabel.create(this.font, messageText, 360);
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 150;
        int buttonHeight = 20;
        int lineCount = this.message.getLineCount() + 1;
        int messageButtonSpacing = Math.max(lineCount, 5) * this.font.lineHeight;
        int buttonY = Math.min(120 + messageButtonSpacing, this.height - 40);
        if (this.showButton) {
            this.button = this.addRenderableWidget(Button.builder(this.buttonLabel, b -> this.onClose()).bounds((this.width - 150) / 2, buttonY, 150, 20).build());
        }
    }

    @Override
    public void tick() {
        if (this.disableButtonTicks > 0) {
            --this.disableButtonTicks;
        }
        if (this.button != null) {
            this.button.active = this.disableButtonTicks == 0;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        graphics.centeredText(this.font, this.title, this.width / 2, 80, -1);
        int messageY = 120;
        if (this.showLoadingDots) {
            String loadingDots = LoadingDotsText.get(Util.getMillis());
            graphics.centeredText(this.font, loadingDots, this.width / 2, messageY, -6250336);
            messageY += this.font.lineHeight + 3;
        }
        this.message.visitLines(TextAlignment.CENTER, this.width / 2, messageY, this.font.lineHeight, textRenderer);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.closeOnEscape && this.button != null && this.button.active;
    }

    @Override
    public void onClose() {
        this.buttonCallback.run();
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.title, this.messageText);
    }
}

