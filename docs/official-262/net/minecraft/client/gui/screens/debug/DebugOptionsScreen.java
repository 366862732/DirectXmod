/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.floats.FloatComparators
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatComparators;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugOptionsScreen
extends Screen {
    private static final Component TITLE = Component.translatable("debug.options.title");
    private static final Component SUBTITLE = Component.translatable("debug.options.warning").withColor(-2142128);
    private static final Component ENABLED_TEXT = Component.translatable("debug.entry.always");
    private static final Component IN_OVERLAY_TEXT = Component.translatable("debug.entry.overlay");
    private static final Component DISABLED_TEXT = CommonComponents.OPTION_OFF;
    private static final Component NOT_ALLOWED_TOOLTIP = Component.translatable("debug.options.notAllowed.tooltip");
    private static final Component SEARCH = Component.translatable("debug.options.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);
    private @Nullable OptionList optionList;
    private EditBox searchBox;
    private final List<Button> profileButtons = new ArrayList<Button>();

    public DebugOptionsScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
        this.optionList = new OptionList(this);
        int optionListWidth = this.optionList.getRowWidth();
        LinearLayout title = LinearLayout.horizontal().spacing(8);
        title.addChild(new SpacerElement(optionListWidth / 3, 1));
        title.addChild(new StringWidget(TITLE, this.font), title.newCellSettings().alignVerticallyMiddle());
        this.searchBox = new EditBox(this.font, 0, 0, optionListWidth / 3, 20, this.searchBox, SEARCH);
        this.searchBox.setResponder(value -> this.optionList.updateSearch((String)value));
        this.searchBox.setHint(SEARCH);
        title.addChild(this.searchBox);
        header.addChild(title, LayoutSettings::alignHorizontallyCenter);
        header.addChild(new MultiLineTextWidget(SUBTITLE, this.font).setMaxWidth(optionListWidth).setCentered(true), LayoutSettings::alignHorizontallyCenter);
        this.layout.addToContents(this.optionList);
        LinearLayout bottomButtons = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.addProfileButton(DebugScreenProfile.DEFAULT, bottomButtons);
        this.addProfileButton(DebugScreenProfile.PERFORMANCE, bottomButtons);
        bottomButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(60).build());
        DebugOptionsScreen debugOptionsScreen = this;
        this.layout.visitWidgets(x$0 -> debugOptionsScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    public void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        this.minecraft.gui.hud.extractDebugOverlay(graphics);
        super.extractBlurredBackground(graphics);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBox);
    }

    private void addProfileButton(DebugScreenProfile profile, LinearLayout bottomButtons) {
        Button profileButton = Button.builder(Component.translatable(profile.translationKey()), button -> {
            this.minecraft.debugEntries.loadProfile(profile);
            this.minecraft.debugEntries.save();
            this.optionList.refreshEntries();
            for (Button listButton : this.profileButtons) {
                listButton.active = true;
            }
            button.active = false;
        }).width(120).build();
        profileButton.active = !this.minecraft.debugEntries.isUsingProfile(profile);
        this.profileButtons.add(profileButton);
        bottomButtons.addChild(profileButton);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.optionList != null) {
            this.optionList.updateSize(this.width, this.layout);
        }
    }

    public @Nullable OptionList getOptionList() {
        return this.optionList;
    }

    @Environment(value=EnvType.CLIENT)
    public class OptionList
    extends ContainerObjectSelectionList<AbstractOptionEntry> {
        private static final Comparator<Map.Entry<Identifier, DebugScreenEntry>> COMPARATOR = (o1, o2) -> {
            int byCategory = FloatComparators.NATURAL_COMPARATOR.compare(((DebugScreenEntry)o1.getValue()).category().sortKey(), ((DebugScreenEntry)o2.getValue()).category().sortKey());
            if (byCategory != 0) {
                return byCategory;
            }
            return ((Identifier)o1.getKey()).compareTo((Identifier)o2.getKey());
        };
        private static final int ITEM_HEIGHT = 20;
        final /* synthetic */ DebugOptionsScreen this$0;

        public OptionList(DebugOptionsScreen this$0) {
            DebugOptionsScreen debugOptionsScreen = this$0;
            Objects.requireNonNull(debugOptionsScreen);
            this.this$0 = debugOptionsScreen;
            super(Minecraft.getInstance(), this$0.width, this$0.layout.getContentHeight(), this$0.layout.getHeaderHeight(), 20);
            this.updateSearch("");
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public int getRowWidth() {
            return 350;
        }

        public void refreshEntries() {
            this.children().forEach(AbstractOptionEntry::refreshEntry);
        }

        public void updateSearch(String value) {
            this.clearEntries();
            ArrayList<Map.Entry<Identifier, DebugScreenEntry>> all = new ArrayList<Map.Entry<Identifier, DebugScreenEntry>>(DebugScreenEntries.allEntries().entrySet());
            all.sort(COMPARATOR);
            DebugEntryCategory currentCategory = null;
            for (Map.Entry entry : all) {
                if (!((Identifier)entry.getKey()).getPath().contains(value)) continue;
                DebugEntryCategory newCategory = ((DebugScreenEntry)entry.getValue()).category();
                if (!newCategory.equals(currentCategory)) {
                    this.addEntry(new CategoryEntry(this.this$0, newCategory.label()));
                    currentCategory = newCategory;
                }
                this.addEntry(new OptionEntry(this.this$0, (Identifier)entry.getKey()));
            }
            this.notifyListUpdated();
        }

        private void notifyListUpdated() {
            this.refreshScrollAmount();
            this.this$0.triggerImmediateNarration(true);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class OptionEntry
    extends AbstractOptionEntry {
        private static final int BUTTON_WIDTH = 60;
        private final Identifier location;
        protected final List<AbstractWidget> children;
        private final CycleButton<Boolean> always;
        private final CycleButton<Boolean> overlay;
        private final CycleButton<Boolean> never;
        private final String name;
        private final boolean isAllowed;
        final /* synthetic */ DebugOptionsScreen this$0;

        public OptionEntry(DebugOptionsScreen debugOptionsScreen, Identifier location) {
            DebugOptionsScreen debugOptionsScreen2 = debugOptionsScreen;
            Objects.requireNonNull(debugOptionsScreen2);
            this.this$0 = debugOptionsScreen2;
            this.children = Lists.newArrayList();
            this.location = location;
            DebugScreenEntry entry = DebugScreenEntries.getEntry(location);
            this.isAllowed = entry != null && entry.isAllowed(debugOptionsScreen.minecraft.showOnlyReducedInfo());
            String name = location.getPath();
            this.name = this.isAllowed ? name : String.valueOf((Object)ChatFormatting.ITALIC) + name;
            this.always = CycleButton.booleanBuilder(ENABLED_TEXT.copy().withColor(-2142128), ENABLED_TEXT.copy().withColor(-4539718), false).displayOnlyValue().withCustomNarration(this::narrateButton).create(10, 5, 60, 16, Component.literal(name), (button, newValue) -> this.setValue(location, DebugScreenEntryStatus.ALWAYS_ON));
            this.overlay = CycleButton.booleanBuilder(IN_OVERLAY_TEXT.copy().withColor(-171), IN_OVERLAY_TEXT.copy().withColor(-4539718), false).displayOnlyValue().withCustomNarration(this::narrateButton).create(10, 5, 60, 16, Component.literal(name), (button, newValue) -> this.setValue(location, DebugScreenEntryStatus.IN_OVERLAY));
            this.never = CycleButton.booleanBuilder(DISABLED_TEXT.copy().withColor(-1), DISABLED_TEXT.copy().withColor(-4539718), false).displayOnlyValue().withCustomNarration(this::narrateButton).create(10, 5, 60, 16, Component.literal(name), (button, newValue) -> this.setValue(location, DebugScreenEntryStatus.NEVER));
            this.children.add(this.never);
            this.children.add(this.overlay);
            this.children.add(this.always);
            this.refreshEntry();
        }

        private MutableComponent narrateButton(CycleButton<Boolean> booleanCycleButton) {
            DebugScreenEntryStatus status = ((DebugOptionsScreen)this.this$0).minecraft.debugEntries.getStatus(this.location);
            MutableComponent current = Component.translatable("debug.entry.currently." + status.getSerializedName(), this.name);
            return CommonComponents.optionNameValue(current, booleanCycleButton.getMessage());
        }

        private void setValue(Identifier location, DebugScreenEntryStatus never) {
            ((DebugOptionsScreen)this.this$0).minecraft.debugEntries.setStatus(location, never);
            for (Button profileButton : this.this$0.profileButtons) {
                profileButton.active = true;
            }
            this.refreshEntry();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int x = this.getContentX();
            int y = this.getContentY();
            graphics.text(((DebugOptionsScreen)this.this$0).minecraft.font, this.name, x, y + 5, this.isAllowed ? -1 : -8355712);
            int buttonsStartX = x + this.getContentWidth() - this.never.getWidth() - this.overlay.getWidth() - this.always.getWidth();
            if (!this.isAllowed && hovered && mouseX < buttonsStartX) {
                graphics.setTooltipForNextFrame(NOT_ALLOWED_TOOLTIP, mouseX, mouseY);
            }
            this.never.setX(buttonsStartX);
            this.overlay.setX(this.never.getX() + this.never.getWidth());
            this.always.setX(this.overlay.getX() + this.overlay.getWidth());
            this.always.setY(y);
            this.overlay.setY(y);
            this.never.setY(y);
            this.always.extractRenderState(graphics, mouseX, mouseY, a);
            this.overlay.extractRenderState(graphics, mouseX, mouseY, a);
            this.never.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public void refreshEntry() {
            DebugScreenEntryStatus status = ((DebugOptionsScreen)this.this$0).minecraft.debugEntries.getStatus(this.location);
            this.always.setValue(status == DebugScreenEntryStatus.ALWAYS_ON);
            this.overlay.setValue(status == DebugScreenEntryStatus.IN_OVERLAY);
            this.never.setValue(status == DebugScreenEntryStatus.NEVER);
            this.always.active = this.always.getValue() == false;
            this.overlay.active = this.overlay.getValue() == false;
            this.never.active = this.never.getValue() == false;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class CategoryEntry
    extends AbstractOptionEntry {
        private final Component category;
        final /* synthetic */ DebugOptionsScreen this$0;

        public CategoryEntry(DebugOptionsScreen debugOptionsScreen, Component category) {
            DebugOptionsScreen debugOptionsScreen2 = debugOptionsScreen;
            Objects.requireNonNull(debugOptionsScreen2);
            this.this$0 = debugOptionsScreen2;
            this.category = category;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.centeredText(((DebugOptionsScreen)this.this$0).minecraft.font, this.category, this.getContentX() + this.getContentWidth() / 2, this.getContentY() + 5, -1);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of((Object)new NarratableEntry(this){
                final /* synthetic */ CategoryEntry this$1;
                {
                    CategoryEntry categoryEntry = this$1;
                    Objects.requireNonNull(categoryEntry);
                    this.this$1 = categoryEntry;
                }

                @Override
                public NarratableEntry.NarrationPriority narrationPriority() {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput output) {
                    output.add(NarratedElementType.TITLE, this.this$1.category);
                }
            });
        }

        @Override
        public void refreshEntry() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class AbstractOptionEntry
    extends ContainerObjectSelectionList.Entry<AbstractOptionEntry> {
        public abstract void refreshEntry();
    }
}

