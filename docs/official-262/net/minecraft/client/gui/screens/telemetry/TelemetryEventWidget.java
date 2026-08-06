/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.telemetry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.DoubleConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TelemetryEventWidget
extends AbstractTextAreaWidget {
    private static final int HEADER_HORIZONTAL_PADDING = 32;
    private static final String TELEMETRY_REQUIRED_TRANSLATION_KEY = "telemetry.event.required";
    private static final String TELEMETRY_OPTIONAL_TRANSLATION_KEY = "telemetry.event.optional";
    private static final String TELEMETRY_OPTIONAL_DISABLED_TRANSLATION_KEY = "telemetry.event.optional.disabled";
    private static final Component PROPERTY_TITLE = Component.translatable("telemetry_info.property_title").withStyle(ChatFormatting.UNDERLINE);
    private final Font font;
    private Content content;
    private @Nullable DoubleConsumer onScrolledListener;

    public TelemetryEventWidget(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, Component.empty(), AbstractScrollArea.defaultSettings(font.lineHeight));
        this.font = font;
        this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
    }

    public void onOptInChanged(boolean optIn) {
        this.content = this.buildContent(optIn);
        this.refreshScrollAmount();
    }

    public void updateLayout() {
        this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
        this.refreshScrollAmount();
    }

    private Content buildContent(boolean hasOptedIn) {
        ContentBuilder content = new ContentBuilder(this.containerWidth());
        ArrayList<TelemetryEventType> eventTypes = new ArrayList<TelemetryEventType>(TelemetryEventType.values());
        eventTypes.sort(Comparator.comparing(TelemetryEventType::isOptIn));
        for (int i = 0; i < eventTypes.size(); ++i) {
            TelemetryEventType eventType = (TelemetryEventType)eventTypes.get(i);
            boolean isDisabled = eventType.isOptIn() && !hasOptedIn;
            this.addEventType(content, eventType, isDisabled);
            if (i >= eventTypes.size() - 1) continue;
            content.addSpacer(this.font.lineHeight);
        }
        return content.build();
    }

    public void setOnScrolledListener(@Nullable DoubleConsumer listener) {
        this.onScrolledListener = listener;
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        if (this.onScrolledListener != null) {
            this.onScrolledListener.accept(this.scrollAmount());
        }
    }

    @Override
    protected int getInnerHeight() {
        return this.content.container().getHeight();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int top = this.getInnerTop();
        int left = this.getInnerLeft();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)left, (float)top);
        this.content.container().visitWidgets(widget -> widget.extractRenderState(graphics, mouseX, mouseY, a));
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.content.narration());
    }

    private Component grayOutIfDisabled(Component component, boolean isDisabled) {
        if (isDisabled) {
            return component.copy().withStyle(ChatFormatting.GRAY);
        }
        return component;
    }

    private void addEventType(ContentBuilder builder, TelemetryEventType eventType, boolean isDisabled) {
        String titleTranslationPattern = eventType.isOptIn() ? (isDisabled ? TELEMETRY_OPTIONAL_DISABLED_TRANSLATION_KEY : TELEMETRY_OPTIONAL_TRANSLATION_KEY) : TELEMETRY_REQUIRED_TRANSLATION_KEY;
        builder.addHeader(this.font, this.grayOutIfDisabled(Component.translatable(titleTranslationPattern, eventType.title()), isDisabled));
        builder.addHeader(this.font, eventType.description().withStyle(ChatFormatting.GRAY));
        builder.addSpacer(this.font.lineHeight / 2);
        builder.addLine(this.font, this.grayOutIfDisabled(PROPERTY_TITLE, isDisabled), 2);
        this.addEventTypeProperties(eventType, builder, isDisabled);
    }

    private void addEventTypeProperties(TelemetryEventType eventType, ContentBuilder content, boolean isDisabled) {
        for (TelemetryProperty<?> property : eventType.properties()) {
            content.addLine(this.font, this.grayOutIfDisabled(property.title(), isDisabled));
        }
    }

    private int containerWidth() {
        return this.width - this.totalInnerPadding();
    }

    @Environment(value=EnvType.CLIENT)
    private record Content(Layout container, Component narration) {
    }

    @Environment(value=EnvType.CLIENT)
    private static class ContentBuilder {
        private final int width;
        private final LinearLayout layout;
        private final MutableComponent narration = Component.empty();

        public ContentBuilder(int width) {
            this.width = width;
            this.layout = LinearLayout.vertical();
            this.layout.defaultCellSetting().alignHorizontallyLeft();
            this.layout.addChild(SpacerElement.width(width));
        }

        public void addLine(Font font, Component line) {
            this.addLine(font, line, 0);
        }

        public void addLine(Font font, Component line, int paddingBottom) {
            this.layout.addChild(new MultiLineTextWidget(line, font).setMaxWidth(this.width), s -> s.paddingBottom(paddingBottom));
            this.narration.append(line).append("\n");
        }

        public void addHeader(Font font, Component line) {
            this.layout.addChild(new MultiLineTextWidget(line, font).setMaxWidth(this.width - 64).setCentered(true), s -> s.alignHorizontallyCenter().paddingHorizontal(32));
            this.narration.append(line).append("\n");
        }

        public void addSpacer(int height) {
            this.layout.addChild(SpacerElement.height(height));
        }

        public Content build() {
            this.layout.arrangeElements();
            return new Content(this.layout, this.narration);
        }
    }
}

