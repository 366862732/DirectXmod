/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class OptionsList
extends ContainerObjectSelectionList<AbstractEntry> {
    private static final int BIG_BUTTON_WIDTH = 310;
    private static final int DEFAULT_ITEM_HEIGHT = 25;
    private final OptionsSubScreen screen;

    public OptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
        super(minecraft, width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 25);
        this.centerListVertically = false;
        this.screen = screen;
    }

    public void addBig(OptionInstance<?> option) {
        this.addEntry(Entry.big(this.minecraft.options, option, this.screen));
    }

    public void addBig(AbstractWidget widget) {
        this.addEntry(Entry.big(widget, this.screen));
    }

    public void addSmall(OptionInstance<?> ... options) {
        for (int i = 0; i < options.length; i += 2) {
            OptionInstance<?> secondOption = i < options.length - 1 ? options[i + 1] : null;
            this.addEntry(Entry.small(this.minecraft.options, options[i], secondOption, this.screen));
        }
    }

    public void addSmall(List<AbstractWidget> widgets) {
        for (int i = 0; i < widgets.size(); i += 2) {
            this.addSmall(widgets.get(i), i < widgets.size() - 1 ? widgets.get(i + 1) : null);
        }
    }

    public void addSmall(AbstractWidget firstOption, @Nullable AbstractWidget secondOption) {
        this.addEntry(Entry.small(firstOption, secondOption, this.screen));
    }

    public void addSmall(AbstractWidget firstOption, OptionInstance<?> firstOptionInstance, @Nullable AbstractWidget secondOption) {
        this.addEntry(Entry.small(firstOption, firstOptionInstance, secondOption, (Screen)this.screen));
    }

    public void addHeader(Component text) {
        int lineHeight = this.minecraft.font.lineHeight;
        int paddingTop = this.children().isEmpty() ? 0 : lineHeight * 2;
        this.addEntry(new HeaderEntry(this.screen, text, paddingTop), paddingTop + lineHeight + 4);
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    public @Nullable AbstractWidget findOption(OptionInstance<?> option) {
        for (AbstractEntry child : this.children()) {
            Entry entry;
            AbstractWidget widgetForOption;
            if (!(child instanceof Entry) || (widgetForOption = (entry = (Entry)child).findOption(option)) == null) continue;
            return widgetForOption;
        }
        return null;
    }

    public void applyUnsavedChanges() {
        for (AbstractEntry child : this.children()) {
            if (!(child instanceof Entry)) continue;
            Entry entry = (Entry)child;
            for (OptionInstanceWidget optionInstanceWidget : entry.children) {
                AbstractWidget abstractWidget;
                if (optionInstanceWidget.optionInstance() == null || !((abstractWidget = optionInstanceWidget.widget()) instanceof OptionInstance.OptionInstanceSliderButton)) continue;
                OptionInstance.OptionInstanceSliderButton optionSlider = (OptionInstance.OptionInstanceSliderButton)abstractWidget;
                optionSlider.applyUnsavedValue();
            }
        }
    }

    public void resetOption(OptionInstance<?> option) {
        for (AbstractEntry child : this.children()) {
            if (!(child instanceof Entry)) continue;
            Entry entry = (Entry)child;
            for (OptionInstanceWidget optionInstanceWidget : entry.children) {
                AbstractWidget abstractWidget;
                if (optionInstanceWidget.optionInstance() != option || !((abstractWidget = optionInstanceWidget.widget()) instanceof ResettableOptionWidget)) continue;
                ResettableOptionWidget resettableOptionWidget = (ResettableOptionWidget)((Object)abstractWidget);
                resettableOptionWidget.resetValue();
                return;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    protected static class Entry
    extends AbstractEntry {
        private final List<OptionInstanceWidget> children;
        private final Screen screen;
        private static final int X_OFFSET = 160;

        private Entry(List<OptionInstanceWidget> widgets, Screen screen) {
            this.children = widgets;
            this.screen = screen;
        }

        public static Entry big(Options options, OptionInstance<?> optionInstance, Screen screen) {
            return new Entry(List.of(new OptionInstanceWidget(optionInstance.createButton(options, 0, 0, 310), optionInstance)), screen);
        }

        public static Entry big(AbstractWidget widget, Screen screen) {
            widget.setWidth(310);
            return new Entry(List.of(new OptionInstanceWidget(widget, null)), screen);
        }

        public static Entry small(AbstractWidget leftWidget, @Nullable AbstractWidget rightWidget, Screen screen) {
            if (rightWidget == null) {
                return new Entry(List.of(new OptionInstanceWidget(leftWidget)), screen);
            }
            return new Entry(List.of(new OptionInstanceWidget(leftWidget), new OptionInstanceWidget(rightWidget)), screen);
        }

        public static Entry small(AbstractWidget leftWidget, OptionInstance<?> leftWidgetOptionInstance, @Nullable AbstractWidget rightWidget, Screen screen) {
            if (rightWidget == null) {
                return new Entry(List.of(new OptionInstanceWidget(leftWidget, leftWidgetOptionInstance)), screen);
            }
            return new Entry(List.of(new OptionInstanceWidget(leftWidget, leftWidgetOptionInstance), new OptionInstanceWidget(rightWidget)), screen);
        }

        public static Entry small(Options options, OptionInstance<?> optionA, @Nullable OptionInstance<?> optionB, OptionsSubScreen screen) {
            AbstractWidget buttonA = optionA.createButton(options);
            if (optionB == null) {
                return new Entry(List.of(new OptionInstanceWidget(buttonA, optionA)), screen);
            }
            return new Entry(List.of(new OptionInstanceWidget(buttonA, optionA), new OptionInstanceWidget(optionB.createButton(options), optionB)), screen);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int xOffset = 0;
            int x = this.screen.width / 2 - 155;
            for (OptionInstanceWidget optionInstanceWidget : this.children) {
                optionInstanceWidget.widget().setPosition(x + xOffset, this.getContentY());
                optionInstanceWidget.widget().extractRenderState(graphics, mouseX, mouseY, a);
                xOffset += 160;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Lists.transform(this.children, OptionInstanceWidget::widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Lists.transform(this.children, OptionInstanceWidget::widget);
        }

        public @Nullable AbstractWidget findOption(OptionInstance<?> option) {
            for (OptionInstanceWidget child : this.children) {
                if (child.optionInstance != option) continue;
                return child.widget();
            }
            return null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    protected static class HeaderEntry
    extends AbstractEntry {
        private final Screen screen;
        private final int paddingTop;
        private final StringWidget widget;

        protected HeaderEntry(Screen screen, Component text, int paddingTop) {
            this.screen = screen;
            this.paddingTop = paddingTop;
            this.widget = new StringWidget(text, screen.getFont());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + this.paddingTop);
            this.widget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }
    }

    @Environment(value=EnvType.CLIENT)
    protected static abstract class AbstractEntry
    extends ContainerObjectSelectionList.Entry<AbstractEntry> {
        protected AbstractEntry() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record OptionInstanceWidget(AbstractWidget widget, @Nullable OptionInstance<?> optionInstance) {
        public OptionInstanceWidget(AbstractWidget widget) {
            this(widget, null);
        }
    }
}

