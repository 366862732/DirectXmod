/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.serialization.DataResult
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractGameRulesScreen
extends Screen {
    protected static final Component TITLE = Component.translatable("editGamerule.title");
    private static final Component SEARCH_HINT = Component.translatable("gui.game_rule.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final int SEARCH_BOX_HEIGHT = 15;
    private final Set<RuleEntry> invalidEntries = Sets.newHashSet();
    private final Consumer<Optional<GameRules>> exitCallback;
    protected final HeaderAndFooterLayout layout;
    protected final GameRules gameRules;
    protected @Nullable EditBox searchBox;
    protected @Nullable RuleList ruleList;
    protected @Nullable Button doneButton;

    public AbstractGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> exitCallback) {
        super(TITLE);
        this.gameRules = gameRules;
        this.exitCallback = exitCallback;
        this.layout = new HeaderAndFooterLayout(this, (int)(12.0 + (double)this.font.lineHeight + 15.0), 33);
    }

    protected void createAndConfigureSearchBox(LinearLayout headerLayout) {
        this.searchBox = headerLayout.addChild(new EditBox(this.font, 200, 15, Component.empty()));
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setResponder(this::filterGameRules);
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(TITLE, this.font));
        this.createAndConfigureSearchBox(header);
        this.initContent();
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
        AbstractGameRulesScreen abstractGameRulesScreen = this;
        this.layout.visitWidgets(x$0 -> abstractGameRulesScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    protected abstract void initContent();

    protected abstract void onDone();

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.ruleList != null) {
            this.ruleList.updateSize(this.width, this.layout);
        }
    }

    @Override
    protected void setInitialFocus() {
        if (this.searchBox != null) {
            this.setInitialFocus(this.searchBox);
        }
    }

    private void markInvalid(RuleEntry invalidEntry) {
        this.invalidEntries.add(invalidEntry);
        this.updateDoneButton();
    }

    private void clearInvalid(RuleEntry invalidEntry) {
        this.invalidEntries.remove(invalidEntry);
        this.updateDoneButton();
    }

    private void updateDoneButton() {
        if (this.doneButton != null) {
            this.doneButton.active = this.invalidEntries.isEmpty();
        }
    }

    protected void closeAndDiscardChanges() {
        this.exitCallback.accept(Optional.empty());
    }

    protected void closeAndApplyChanges() {
        this.exitCallback.accept(Optional.of(this.gameRules));
    }

    protected void filterGameRules(String filter) {
        if (this.ruleList != null) {
            this.ruleList.populateChildren(filter);
            this.ruleList.setScrollAmount(0.0);
            this.repositionElements();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class RuleList
    extends ContainerObjectSelectionList<RuleEntry> {
        private static final int ITEM_HEIGHT = 24;
        private final GameRules gameRules;
        final /* synthetic */ AbstractGameRulesScreen this$0;

        public RuleList(AbstractGameRulesScreen this$0, GameRules gameRules) {
            AbstractGameRulesScreen abstractGameRulesScreen = this$0;
            Objects.requireNonNull(abstractGameRulesScreen);
            this.this$0 = abstractGameRulesScreen;
            super(Minecraft.getInstance(), this$0.width, this$0.layout.getContentHeight(), this$0.layout.getHeaderHeight(), 24);
            this.gameRules = gameRules;
            this.populateChildren("");
        }

        private void populateChildren(String filter) {
            this.clearEntries();
            final HashMap entries = Maps.newHashMap();
            final String lowerCaseFilter = filter.toLowerCase(Locale.ROOT);
            this.gameRules.visitGameRuleTypes(new GameRuleTypeVisitor(){
                final /* synthetic */ RuleList this$1;
                {
                    RuleList ruleList = this$1;
                    Objects.requireNonNull(ruleList);
                    this.this$1 = ruleList;
                }

                @Override
                public void visitBoolean(GameRule<Boolean> gameRule) {
                    this.addEntry(gameRule, (x$0, x$1, x$2, x$3) -> new BooleanRuleEntry(this.this$1.this$0, x$0, x$1, x$2, x$3));
                }

                @Override
                public void visitInteger(GameRule<Integer> gameRule) {
                    this.addEntry(gameRule, (x$0, x$1, x$2, x$3) -> new IntegerRuleEntry(this.this$1.this$0, x$0, x$1, x$2, x$3));
                }

                private <T> void addEntry(GameRule<T> gameRule, EntryFactory<T> factory) {
                    Object narration;
                    ImmutableList tooltip;
                    MutableComponent readableName = Component.translatable(gameRule.getDescriptionId());
                    String descriptionKey = gameRule.getDescriptionId() + ".description";
                    Optional<MutableComponent> optionalDescription = Optional.of(Component.translatable(descriptionKey)).filter(ComponentUtils::isTranslationResolvable);
                    if (!RuleList.matchesFilter(gameRule.id(), readableName.getString(), gameRule.category().label().getString(), optionalDescription, lowerCaseFilter)) {
                        return;
                    }
                    MutableComponent actualName = Component.literal(gameRule.id()).withStyle(ChatFormatting.YELLOW);
                    MutableComponent defaultValue = Component.translatable("editGamerule.default", Component.literal(gameRule.serialize(gameRule.defaultValue()))).withStyle(ChatFormatting.GRAY);
                    if (optionalDescription.isPresent()) {
                        ImmutableList.Builder result = ImmutableList.builder().add((Object)actualName.getVisualOrderText());
                        this.this$1.this$0.font.split(optionalDescription.get(), 150).forEach(arg_0 -> ((ImmutableList.Builder)result).add(arg_0));
                        tooltip = result.add((Object)defaultValue.getVisualOrderText()).build();
                        narration = optionalDescription.get().getString() + "\n" + defaultValue.getString();
                    } else {
                        tooltip = ImmutableList.of((Object)actualName.getVisualOrderText(), (Object)defaultValue.getVisualOrderText());
                        narration = defaultValue.getString();
                    }
                    entries.computeIfAbsent(gameRule.category(), k -> Maps.newHashMap()).put(gameRule, factory.create(readableName, (List<FormattedCharSequence>)tooltip, (String)narration, gameRule));
                }
            });
            entries.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getDescriptionId))).forEach(e -> {
                this.addEntry(new CategoryRuleEntry(this.this$0, ((GameRuleCategory)e.getKey()).label().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)));
                ((Map)e.getValue()).entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRule::getDescriptionId))).forEach(v -> this.addEntry((RuleEntry)v.getValue()));
            });
        }

        private static boolean matchesFilter(String gameRuleId, String readableName, String categoryName, Optional<MutableComponent> optionalDescription, String lowerCaseFilter) {
            return RuleList.toLowerCaseMatchesFilter(gameRuleId, lowerCaseFilter) || RuleList.toLowerCaseMatchesFilter(readableName, lowerCaseFilter) || RuleList.toLowerCaseMatchesFilter(categoryName, lowerCaseFilter) || optionalDescription.map(description -> RuleList.toLowerCaseMatchesFilter(description.getString(), lowerCaseFilter)).orElse(false) != false;
        }

        private static boolean toLowerCaseMatchesFilter(String gameRuleId, String lowerCaseFilter) {
            return gameRuleId.toLowerCase(Locale.ROOT).contains(lowerCaseFilter);
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
            RuleEntry hovered = (RuleEntry)this.getHovered();
            if (hovered != null && hovered.tooltip != null) {
                graphics.setTooltipForNextFrame(hovered.tooltip, mouseX, mouseY);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class IntegerRuleEntry
    extends GameRuleEntry {
        private final EditBox input;

        public IntegerRuleEntry(AbstractGameRulesScreen this$0, Component label, List<FormattedCharSequence> tooltip, String narration, GameRule<Integer> gameRule) {
            Objects.requireNonNull(this$0);
            super(this$0, tooltip, label);
            this.input = new EditBox(((AbstractGameRulesScreen)this$0).minecraft.font, 10, 5, 44, 20, label.copy().append("\n").append(narration).append("\n"));
            this.input.setValue(this$0.gameRules.getAsString(gameRule));
            this.input.setResponder(v -> {
                DataResult value = gameRule.deserialize((String)v);
                if (value.isSuccess()) {
                    this.input.setTextColor(-2039584);
                    this$0.clearInvalid(this);
                    this$0.gameRules.set(gameRule, (Integer)value.getOrThrow(), null);
                } else {
                    this.input.setTextColor(-65536);
                    this$0.markInvalid(this);
                }
            });
            this.children.add(this.input);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.extractLabel(graphics, this.getContentY(), this.getContentX());
            this.input.setX(this.getContentRight() - 45);
            this.input.setY(this.getContentY());
            this.input.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class BooleanRuleEntry
    extends GameRuleEntry {
        private final CycleButton<Boolean> checkbox;

        public BooleanRuleEntry(AbstractGameRulesScreen this$0, Component name, List<FormattedCharSequence> tooltip, String narration, GameRule<Boolean> gameRule) {
            Objects.requireNonNull(this$0);
            super(this$0, tooltip, name);
            this.checkbox = CycleButton.onOffBuilder(this$0.gameRules.get(gameRule)).displayOnlyValue().withCustomNarration(button -> button.createDefaultNarrationMessage().append("\n").append(narration)).create(10, 5, 44, 20, name, (button, newValue) -> this$0.gameRules.set(gameRule, newValue, null));
            this.children.add(this.checkbox);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.extractLabel(graphics, this.getContentY(), this.getContentX());
            this.checkbox.setX(this.getContentRight() - 45);
            this.checkbox.setY(this.getContentY());
            this.checkbox.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public abstract class GameRuleEntry
    extends RuleEntry {
        private final List<FormattedCharSequence> label;
        protected final List<AbstractWidget> children;
        final /* synthetic */ AbstractGameRulesScreen this$0;

        public GameRuleEntry(@Nullable AbstractGameRulesScreen this$0, List<FormattedCharSequence> tooltip, Component label) {
            AbstractGameRulesScreen abstractGameRulesScreen = this$0;
            Objects.requireNonNull(abstractGameRulesScreen);
            this.this$0 = abstractGameRulesScreen;
            super(tooltip);
            this.children = Lists.newArrayList();
            this.label = ((AbstractGameRulesScreen)this$0).minecraft.font.split(label, 170);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        protected void extractLabel(GuiGraphicsExtractor graphics, int rowTop, int rowLeft) {
            if (this.label.size() == 1) {
                graphics.text(((AbstractGameRulesScreen)this.this$0).minecraft.font, this.label.get(0), rowLeft, rowTop + 5, -1);
            } else if (this.label.size() >= 2) {
                graphics.text(((AbstractGameRulesScreen)this.this$0).minecraft.font, this.label.get(0), rowLeft, rowTop, -1);
                graphics.text(((AbstractGameRulesScreen)this.this$0).minecraft.font, this.label.get(1), rowLeft, rowTop + 10, -1);
            }
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface EntryFactory<T> {
        public RuleEntry create(Component var1, List<FormattedCharSequence> var2, String var3, GameRule<T> var4);
    }

    @Environment(value=EnvType.CLIENT)
    public class CategoryRuleEntry
    extends RuleEntry {
        private final Component label;
        final /* synthetic */ AbstractGameRulesScreen this$0;

        public CategoryRuleEntry(AbstractGameRulesScreen this$0, Component label) {
            AbstractGameRulesScreen abstractGameRulesScreen = this$0;
            Objects.requireNonNull(abstractGameRulesScreen);
            this.this$0 = abstractGameRulesScreen;
            super(null);
            this.label = label;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.centeredText(((AbstractGameRulesScreen)this.this$0).minecraft.font, this.label, this.getContentXMiddle(), this.getContentY() + 5, -1);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of((Object)new NarratableEntry(this){
                final /* synthetic */ CategoryRuleEntry this$1;
                {
                    CategoryRuleEntry categoryRuleEntry = this$1;
                    Objects.requireNonNull(categoryRuleEntry);
                    this.this$1 = categoryRuleEntry;
                }

                @Override
                public NarratableEntry.NarrationPriority narrationPriority() {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput output) {
                    output.add(NarratedElementType.TITLE, this.this$1.label);
                }
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class RuleEntry
    extends ContainerObjectSelectionList.Entry<RuleEntry> {
        private final @Nullable List<FormattedCharSequence> tooltip;

        public RuleEntry(@Nullable List<FormattedCharSequence> tooltip) {
            this.tooltip = tooltip;
        }
    }
}

