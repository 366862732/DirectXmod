/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import java.util.List;
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

@Environment(value=EnvType.CLIENT)
public class UnsupportedGraphicsWarningScreen
extends Screen {
    private static final int BUTTON_PADDING = 20;
    private static final int BUTTON_MARGIN = 5;
    private static final int BUTTON_HEIGHT = 20;
    private final Component narrationMessage;
    private final List<Component> message;
    private final ImmutableList<ButtonOption> buttonOptions;
    private MultiLineLabel messageLines = MultiLineLabel.EMPTY;
    private int contentTop;
    private int buttonWidth;

    protected UnsupportedGraphicsWarningScreen(Component title, List<Component> message, ImmutableList<ButtonOption> buttonOptions) {
        super(title);
        this.message = message;
        this.narrationMessage = CommonComponents.joinForNarration(title, ComponentUtils.formatList(message, CommonComponents.EMPTY));
        this.buttonOptions = buttonOptions;
    }

    @Override
    public Component getNarrationMessage() {
        return this.narrationMessage;
    }

    @Override
    public void init() {
        for (ButtonOption buttonOption : this.buttonOptions) {
            this.buttonWidth = Math.max(this.buttonWidth, 20 + this.font.width(buttonOption.message) + 20);
        }
        int buttonAdvance = 5 + this.buttonWidth + 5;
        int contentWidth = buttonAdvance * this.buttonOptions.size();
        this.messageLines = MultiLineLabel.create(this.font, contentWidth, this.message.toArray(new Component[0]));
        int messageHeight = this.messageLines.getLineCount() * this.font.lineHeight;
        this.contentTop = (int)((double)this.height / 2.0 - (double)messageHeight / 2.0);
        int buttonTop = this.contentTop + messageHeight + this.font.lineHeight * 2;
        int x = (int)((double)this.width / 2.0 - (double)contentWidth / 2.0);
        for (ButtonOption buttonOption : this.buttonOptions) {
            this.addRenderableWidget(Button.builder(buttonOption.message, buttonOption.onPress).bounds(x, buttonTop, this.buttonWidth, 20).build());
            x += buttonAdvance;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        graphics.centeredText(this.font, this.title, this.width / 2, this.contentTop - this.font.lineHeight * 2, -1);
        this.messageLines.visitLines(TextAlignment.CENTER, this.width / 2, this.contentTop, this.font.lineHeight, textRenderer);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public static final class ButtonOption {
        private final Component message;
        private final Button.OnPress onPress;

        public ButtonOption(Component message, Button.OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }
    }
}

