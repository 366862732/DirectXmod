/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.options;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.FontOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LanguageSelectScreen
extends OptionsSubScreen {
    private static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning").withColor(-4539718);
    private static final int FOOTER_HEIGHT = 53;
    private static final Component SEARCH_HINT = Component.translatable("gui.language.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final int SEARCH_BOX_HEIGHT = 15;
    private final LanguageManager languageManager;
    private @Nullable LanguageSelectionList languageSelectionList;
    private @Nullable EditBox search;

    public LanguageSelectScreen(Screen lastScreen, Options options, LanguageManager languageManager) {
        super(lastScreen, options, (Component)Component.translatable("options.language.title"));
        this.languageManager = languageManager;
        this.layout.setFooterHeight(53);
    }

    @Override
    protected void addTitle() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.search = header.addChild(new EditBox(this.font, 0, 0, 200, 15, Component.empty()));
        this.search.setHint(SEARCH_HINT);
        this.search.setResponder(string -> {
            if (this.languageSelectionList != null) {
                this.languageSelectionList.filterEntries((String)string);
            }
        });
        this.layout.setHeaderHeight((int)(12.0 + (double)this.font.lineHeight + 15.0));
    }

    @Override
    protected void setInitialFocus() {
        if (this.search != null) {
            this.setInitialFocus(this.search);
        } else {
            super.setInitialFocus();
        }
    }

    @Override
    protected void addContents() {
        this.languageSelectionList = this.layout.addToContents(new LanguageSelectionList(this, this.minecraft));
    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void addFooter() {
        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical()).spacing(8);
        footer.defaultCellSetting().alignHorizontallyCenter();
        footer.addChild(new StringWidget(WARNING_LABEL, this.font));
        LinearLayout bottomButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        bottomButtons.addChild(Button.builder(Component.translatable("options.font"), button -> this.minecraft.gui.setScreen(new FontOptionsScreen(this, this.options))).build());
        bottomButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).build());
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        if (this.languageSelectionList != null) {
            this.languageSelectionList.updateSize(this.width, this.layout);
        }
    }

    private void onDone() {
        if (this.languageSelectionList != null && this.languageSelectionList.getSelected() != null) {
            LanguageSelectionList.Entry selectedEntry = (LanguageSelectionList.Entry)this.languageSelectionList.getSelected();
            if (!selectedEntry.code.equals(this.languageManager.getSelected())) {
                this.languageManager.setSelected(selectedEntry.code);
                this.options.languageCode = selectedEntry.code;
                this.minecraft.reloadResourcePacks();
            }
        }
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Environment(value=EnvType.CLIENT)
    private class LanguageSelectionList
    extends ObjectSelectionList<Entry> {
        final /* synthetic */ LanguageSelectScreen this$0;

        public LanguageSelectionList(LanguageSelectScreen languageSelectScreen, Minecraft minecraft) {
            LanguageSelectScreen languageSelectScreen2 = languageSelectScreen;
            Objects.requireNonNull(languageSelectScreen2);
            this.this$0 = languageSelectScreen2;
            super(minecraft, languageSelectScreen.width, languageSelectScreen.height - 33 - 53, 33, 18);
            String selectedLanguage = languageSelectScreen.languageManager.getSelected();
            languageSelectScreen.languageManager.getLanguages().forEach((code, info) -> {
                Entry entry = new Entry(this, (String)code, (LanguageInfo)info);
                this.addEntry(entry);
                if (selectedLanguage.equals(code)) {
                    this.setSelected(entry);
                }
            });
            if (this.getSelected() != null) {
                this.centerScrollOn((Entry)this.getSelected());
            }
        }

        private void filterEntries(String filter) {
            SortedMap<String, LanguageInfo> languages = this.this$0.languageManager.getLanguages();
            List<Entry> filteredEntries = languages.entrySet().stream().filter(entry -> filter.isEmpty() || ((LanguageInfo)entry.getValue()).name().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)) || ((LanguageInfo)entry.getValue()).region().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))).map(entry -> new Entry(this, (String)entry.getKey(), (LanguageInfo)entry.getValue())).toList();
            this.replaceEntries(filteredEntries);
            this.refreshScrollAmount();
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Environment(value=EnvType.CLIENT)
        public class Entry
        extends ObjectSelectionList.Entry<Entry> {
            private final String code;
            private final Component language;
            final /* synthetic */ LanguageSelectionList this$1;

            public Entry(LanguageSelectionList this$1, String code, LanguageInfo language) {
                LanguageSelectionList languageSelectionList = this$1;
                Objects.requireNonNull(languageSelectionList);
                this.this$1 = languageSelectionList;
                this.code = code;
                this.language = language.toComponent();
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.centeredText(this.this$1.this$0.font, this.language, this.this$1.width / 2, this.getContentYMiddle() - ((LanguageSelectScreen)this.this$1.this$0).font.lineHeight / 2, -1);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    this.select();
                    this.this$1.this$0.onDone();
                    return true;
                }
                return super.keyPressed(event);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                this.select();
                if (doubleClick) {
                    this.this$1.this$0.onDone();
                }
                return super.mouseClicked(event, doubleClick);
            }

            private void select() {
                this.this$1.setSelected(this);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.language);
            }
        }
    }
}

