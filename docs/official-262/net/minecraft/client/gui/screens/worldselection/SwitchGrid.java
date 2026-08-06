/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.worldselection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
class SwitchGrid {
    private static final int DEFAULT_SWITCH_BUTTON_WIDTH = 44;
    private final List<LabeledSwitch> switches;
    private final Layout layout;

    private SwitchGrid(List<LabeledSwitch> switches, Layout layout) {
        this.switches = switches;
        this.layout = layout;
    }

    public Layout layout() {
        return this.layout;
    }

    public void refreshStates() {
        this.switches.forEach(LabeledSwitch::refreshState);
    }

    public static Builder builder(int width) {
        return new Builder(width);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final int width;
        private final List<SwitchBuilder> switchBuilders = new ArrayList<SwitchBuilder>();
        private int paddingLeft;
        private int rowSpacing = 4;
        private int rowCount;
        private Optional<InfoUnderneathSettings> infoUnderneath = Optional.empty();

        public Builder(int width) {
            this.width = width;
        }

        private void increaseRow() {
            ++this.rowCount;
        }

        public SwitchBuilder addSwitch(Component label, BooleanSupplier stateSupplier, Consumer<Boolean> onClicked) {
            SwitchBuilder switchBuilder = new SwitchBuilder(label, stateSupplier, onClicked, 44);
            this.switchBuilders.add(switchBuilder);
            return switchBuilder;
        }

        public Builder withPaddingLeft(int paddingLeft) {
            this.paddingLeft = paddingLeft;
            return this;
        }

        public Builder withRowSpacing(int rowSpacing) {
            this.rowSpacing = rowSpacing;
            return this;
        }

        public SwitchGrid build() {
            GridLayout switchGrid = new GridLayout().rowSpacing(this.rowSpacing);
            switchGrid.addChild(SpacerElement.width(this.width - 44), 0, 0);
            switchGrid.addChild(SpacerElement.width(44), 0, 1);
            ArrayList<LabeledSwitch> switches = new ArrayList<LabeledSwitch>();
            this.rowCount = 0;
            for (SwitchBuilder switchBuilder : this.switchBuilders) {
                switches.add(switchBuilder.build(this, switchGrid, 0));
            }
            switchGrid.arrangeElements();
            SwitchGrid result = new SwitchGrid(switches, switchGrid);
            result.refreshStates();
            return result;
        }

        public Builder withInfoUnderneath(int maxRows, boolean alwaysMaxHeight) {
            this.infoUnderneath = Optional.of(new InfoUnderneathSettings(maxRows, alwaysMaxHeight));
            return this;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record InfoUnderneathSettings(int maxInfoRows, boolean alwaysMaxHeight) {
    }

    @Environment(value=EnvType.CLIENT)
    private record LabeledSwitch(CycleButton<Boolean> button, BooleanSupplier stateSupplier, @Nullable BooleanSupplier isActiveCondition) {
        public void refreshState() {
            this.button.setValue(this.stateSupplier.getAsBoolean());
            if (this.isActiveCondition != null) {
                this.button.active = this.isActiveCondition.getAsBoolean();
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SwitchBuilder {
        private final Component label;
        private final BooleanSupplier stateSupplier;
        private final Consumer<Boolean> onClicked;
        private @Nullable Component info;
        private @Nullable BooleanSupplier isActiveCondition;
        private final int buttonWidth;

        private SwitchBuilder(Component label, BooleanSupplier stateSupplier, Consumer<Boolean> onClicked, int buttonWidth) {
            this.label = label;
            this.stateSupplier = stateSupplier;
            this.onClicked = onClicked;
            this.buttonWidth = buttonWidth;
        }

        public SwitchBuilder withIsActiveCondition(BooleanSupplier isActiveCondition) {
            this.isActiveCondition = isActiveCondition;
            return this;
        }

        public SwitchBuilder withInfo(Component info) {
            this.info = info;
            return this;
        }

        private LabeledSwitch build(Builder switchGridBuilder, GridLayout gridLayout, int startColumn) {
            boolean hasTooltip;
            switchGridBuilder.increaseRow();
            StringWidget labelWidget = new StringWidget(this.label, Minecraft.getInstance().font);
            gridLayout.addChild(labelWidget, switchGridBuilder.rowCount, startColumn, gridLayout.newCellSettings().align(0.0f, 0.5f).paddingLeft(switchGridBuilder.paddingLeft));
            Optional<InfoUnderneathSettings> infoUnderneath = switchGridBuilder.infoUnderneath;
            CycleButton.Builder<Boolean> buttonBuilder = CycleButton.onOffBuilder(this.stateSupplier.getAsBoolean());
            buttonBuilder.displayOnlyValue();
            boolean bl = hasTooltip = this.info != null && infoUnderneath.isEmpty();
            if (hasTooltip) {
                Tooltip tooltip = Tooltip.create(this.info);
                buttonBuilder.withTooltip(value -> tooltip);
            }
            if (this.info != null && !hasTooltip) {
                buttonBuilder.withCustomNarration(button -> CommonComponents.joinForNarration(this.label, button.createDefaultNarrationMessage(), this.info));
            } else {
                buttonBuilder.withCustomNarration(button -> CommonComponents.joinForNarration(this.label, button.createDefaultNarrationMessage()));
            }
            CycleButton<Boolean> button2 = buttonBuilder.create(0, 0, this.buttonWidth, 20, Component.empty(), (b, value) -> this.onClicked.accept((Boolean)value));
            if (this.isActiveCondition != null) {
                button2.active = this.isActiveCondition.getAsBoolean();
            }
            gridLayout.addChild(button2, switchGridBuilder.rowCount, startColumn + 1, gridLayout.newCellSettings().alignHorizontallyRight());
            if (this.info != null) {
                infoUnderneath.ifPresent(infoUnderneathSettings -> {
                    MutableComponent styledInfo = this.info.copy().withStyle(ChatFormatting.GRAY);
                    Font font = Minecraft.getInstance().font;
                    MultiLineTextWidget infoWidget = new MultiLineTextWidget(styledInfo, font);
                    infoWidget.setMaxWidth(switchGridBuilder.width - switchGridBuilder.paddingLeft - this.buttonWidth);
                    infoWidget.setMaxRows(infoUnderneathSettings.maxInfoRows());
                    switchGridBuilder.increaseRow();
                    int extraBottomPadding = infoUnderneathSettings.alwaysMaxHeight ? font.lineHeight * infoUnderneathSettings.maxInfoRows - infoWidget.getHeight() : 0;
                    gridLayout.addChild(infoWidget, switchGridBuilder.rowCount, startColumn, gridLayout.newCellSettings().paddingTop(-switchGridBuilder.rowSpacing).paddingBottom(extraBottomPadding));
                });
            }
            return new LabeledSwitch(button2, this.stateSupplier, this.isActiveCondition);
        }
    }
}

