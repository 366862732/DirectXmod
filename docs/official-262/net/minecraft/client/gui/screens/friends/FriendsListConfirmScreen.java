/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.booleans.BooleanConsumer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.friends;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PrivacyConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class FriendsListConfirmScreen
extends ConfirmScreen {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("friends/background_dark");
    private static final Component USAGE_FOCUSED = Component.translatable("narration.link.usage.focused");
    private static final Component USAGE_HOVERED = Component.translatable("narration.link.usage.hovered");
    private static final int PANEL_PADDING = 10;
    private static final int BG_BORDER_WIDTH = 8;

    public FriendsListConfirmScreen(BooleanConsumer callback, Component title, Component message, Component yesButton, Component noButton) {
        super(callback, title, message, yesButton, noButton);
    }

    @Override
    protected LayoutElement addMessage() {
        LinearLayout content = LinearLayout.vertical();
        content.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle().padding(10, 5, 10, 5);
        FocusableTextWidget focusable = FocusableTextWidget.builder(this.message, this.font).maxWidth(this.width - 180).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build();
        focusable.setMaxRows(15);
        focusable.setComponentClickHandler(style -> {
            ClickEvent patt1$temp = style.getClickEvent();
            if (patt1$temp instanceof ClickEvent.OpenUrl) {
                URI uri;
                ClickEvent.OpenUrl $b$0 = (ClickEvent.OpenUrl)patt1$temp;
                try {
                    URI patt2$temp;
                    uri = patt2$temp = $b$0.uri();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
                PrivacyConfirmLinkScreen.confirmLinkNow((Screen)this, uri);
            }
        });
        focusable.setNarrateMessage(false);
        focusable.setUsageNarration(USAGE_FOCUSED, USAGE_HOVERED);
        content.addChild(focusable);
        return content;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, this.layout.getX() - 10 - 8, this.layout.getY() - 10 - 8, this.layout.getWidth() + 20 + 16 + 1, this.layout.getHeight() + 20 + 16 + 1);
    }
}

