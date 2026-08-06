/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CycleButton<T>
extends AbstractButton
implements ResettableOptionWidget {
    public static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = () -> Minecraft.getInstance().hasAltDown();
    private static final List<Boolean> BOOLEAN_OPTIONS = ImmutableList.of((Object)Boolean.TRUE, (Object)Boolean.FALSE);
    private final Supplier<T> defaultValueSupplier;
    private final Component name;
    private int index;
    private T value;
    private final ValueListSupplier<T> values;
    private final Function<T, Component> valueStringifier;
    private final Function<CycleButton<T>, MutableComponent> narrationProvider;
    private final OnValueChange<T> onValueChange;
    private final DisplayState displayState;
    private final OptionInstance.TooltipSupplier<T> tooltipSupplier;
    private final SpriteSupplier<T> spriteSupplier;

    private CycleButton(int x, int y, int width, int height, Component message, Component name, int index, T value, Supplier<T> defaultValueSupplier, ValueListSupplier<T> values, Function<T, Component> valueStringifier, Function<CycleButton<T>, MutableComponent> narrationProvider, OnValueChange<T> onValueChange, OptionInstance.TooltipSupplier<T> tooltipSupplier, DisplayState displayState, SpriteSupplier<T> spriteSupplier) {
        super(x, y, width, height, message);
        this.name = name;
        this.index = index;
        this.defaultValueSupplier = defaultValueSupplier;
        this.value = value;
        this.values = values;
        this.valueStringifier = valueStringifier;
        this.narrationProvider = narrationProvider;
        this.onValueChange = onValueChange;
        this.displayState = displayState;
        this.tooltipSupplier = tooltipSupplier;
        this.spriteSupplier = spriteSupplier;
        this.updateTooltip();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier sprite = this.spriteSupplier.apply(this, this.getValue());
        if (sprite != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        } else {
            this.extractDefaultSprite(graphics);
        }
        if (this.displayState != DisplayState.HIDE) {
            this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
        }
    }

    private void updateTooltip() {
        this.setTooltip(this.tooltipSupplier.apply(this.value));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (input.hasShiftDown()) {
            this.cycleValue(-1);
        } else {
            this.cycleValue(1);
        }
    }

    private void cycleValue(int delta) {
        List<T> list = this.values.getSelectedList();
        this.index = Mth.positiveModulo(this.index + delta, list.size());
        T newValue = list.get(this.index);
        this.updateValue(newValue);
        this.onValueChange.onValueChange(this, newValue);
    }

    private T getCycledValue(int delta) {
        List<T> list = this.values.getSelectedList();
        return list.get(Mth.positiveModulo(this.index + delta, list.size()));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY > 0.0) {
            this.cycleValue(-1);
        } else if (scrollY < 0.0) {
            this.cycleValue(1);
        }
        return true;
    }

    public void setValue(T newValue) {
        List<T> list = this.values.getSelectedList();
        int newIndex = list.indexOf(newValue);
        if (newIndex != -1) {
            this.index = newIndex;
        }
        this.updateValue(newValue);
    }

    @Override
    public void resetValue() {
        this.setValue(this.defaultValueSupplier.get());
    }

    private void updateValue(T newValue) {
        Component newMessage = this.createLabelForValue(newValue);
        this.setMessage(newMessage);
        this.value = newValue;
        this.updateTooltip();
    }

    private Component createLabelForValue(T newValue) {
        return this.displayState == DisplayState.VALUE ? this.valueStringifier.apply(newValue) : this.createFullName(newValue);
    }

    private MutableComponent createFullName(T newValue) {
        return CommonComponents.optionNameValue(this.name, this.valueStringifier.apply(newValue));
    }

    public T getValue() {
        return this.value;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return this.narrationProvider.apply(this);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, (Component)this.createNarrationMessage());
        if (this.active) {
            T nextValue = this.getCycledValue(1);
            Component nextValueText = this.createLabelForValue(nextValue);
            if (this.isFocused()) {
                output.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.cycle_button.usage.focused", nextValueText));
            } else {
                output.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.cycle_button.usage.hovered", nextValueText));
            }
        }
    }

    public MutableComponent createDefaultNarrationMessage() {
        return CycleButton.wrapDefaultNarrationMessage(this.displayState == DisplayState.VALUE ? this.createFullName(this.value) : this.getMessage());
    }

    public static <T> Builder<T> builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier) {
        return new Builder<T>(valueStringifier, defaultValueSupplier);
    }

    public static <T> Builder<T> builder(Function<T, Component> valueStringifier, T defaultValue) {
        return new Builder<Object>(valueStringifier, () -> defaultValue);
    }

    public static Builder<Boolean> booleanBuilder(Component trueText, Component falseText, boolean defaultValue) {
        return new Builder<Boolean>(b -> b == Boolean.TRUE ? trueText : falseText, () -> defaultValue).withValues((Collection<Boolean>)BOOLEAN_OPTIONS);
    }

    public static Builder<Boolean> onOffBuilder(boolean initialValue) {
        return new Builder<Boolean>(b -> b == Boolean.TRUE ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, () -> initialValue).withValues((Collection<Boolean>)BOOLEAN_OPTIONS);
    }

    @Environment(value=EnvType.CLIENT)
    public static interface ValueListSupplier<T> {
        public List<T> getSelectedList();

        public List<T> getDefaultList();

        public static <T> ValueListSupplier<T> create(Collection<T> values) {
            ImmutableList copy = ImmutableList.copyOf(values);
            return new ValueListSupplier<T>((List)copy){
                final /* synthetic */ List val$copy;
                {
                    this.val$copy = list;
                }

                @Override
                public List<T> getSelectedList() {
                    return this.val$copy;
                }

                @Override
                public List<T> getDefaultList() {
                    return this.val$copy;
                }
            };
        }

        public static <T> ValueListSupplier<T> create(final BooleanSupplier altSelector, List<T> defaultList, List<T> altList) {
            ImmutableList defaultCopy = ImmutableList.copyOf(defaultList);
            ImmutableList altCopy = ImmutableList.copyOf(altList);
            return new ValueListSupplier<T>((List)altCopy, (List)defaultCopy){
                final /* synthetic */ List val$altCopy;
                final /* synthetic */ List val$defaultCopy;
                {
                    this.val$altCopy = list;
                    this.val$defaultCopy = list2;
                }

                @Override
                public List<T> getSelectedList() {
                    return altSelector.getAsBoolean() ? this.val$altCopy : this.val$defaultCopy;
                }

                @Override
                public List<T> getDefaultList() {
                    return this.val$defaultCopy;
                }
            };
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface OnValueChange<T> {
        public void onValueChange(CycleButton<T> var1, T var2);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum DisplayState {
        NAME_AND_VALUE,
        VALUE,
        HIDE;

    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface SpriteSupplier<T> {
        public @Nullable Identifier apply(CycleButton<T> var1, T var2);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder<T> {
        private final Supplier<T> defaultValueSupplier;
        private final Function<T, Component> valueStringifier;
        private OptionInstance.TooltipSupplier<T> tooltipSupplier = value -> null;
        private SpriteSupplier<T> spriteSupplier = (button, value) -> null;
        private Function<CycleButton<T>, MutableComponent> narrationProvider = CycleButton::createDefaultNarrationMessage;
        private ValueListSupplier<T> values = ValueListSupplier.create(ImmutableList.of());
        private DisplayState displayState = DisplayState.NAME_AND_VALUE;

        public Builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier) {
            this.valueStringifier = valueStringifier;
            this.defaultValueSupplier = defaultValueSupplier;
        }

        public Builder<T> withValues(Collection<T> values) {
            return this.withValues(ValueListSupplier.create(values));
        }

        @SafeVarargs
        public final Builder<T> withValues(T ... values) {
            return this.withValues((Collection<T>)ImmutableList.copyOf((Object[])values));
        }

        public Builder<T> withValues(List<T> values, List<T> altValues) {
            return this.withValues(ValueListSupplier.create(DEFAULT_ALT_LIST_SELECTOR, values, altValues));
        }

        public Builder<T> withValues(BooleanSupplier altCondition, List<T> values, List<T> altValues) {
            return this.withValues(ValueListSupplier.create(altCondition, values, altValues));
        }

        public Builder<T> withValues(ValueListSupplier<T> valueListSupplier) {
            this.values = valueListSupplier;
            return this;
        }

        public Builder<T> withTooltip(OptionInstance.TooltipSupplier<T> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public Builder<T> withCustomNarration(Function<CycleButton<T>, MutableComponent> narrationProvider) {
            this.narrationProvider = narrationProvider;
            return this;
        }

        public Builder<T> withSprite(SpriteSupplier<T> spriteSupplier) {
            this.spriteSupplier = spriteSupplier;
            return this;
        }

        public Builder<T> displayState(DisplayState state) {
            this.displayState = state;
            return this;
        }

        public Builder<T> displayOnlyValue() {
            return this.displayState(DisplayState.VALUE);
        }

        public CycleButton<T> create(Component name, OnValueChange<T> valueChangeListener) {
            return this.create(0, 0, 150, 20, name, valueChangeListener);
        }

        public CycleButton<T> create(int x, int y, int width, int height, Component name) {
            return this.create(x, y, width, height, name, (button, value) -> {});
        }

        public CycleButton<T> create(int x, int y, int width, int height, Component name, OnValueChange<T> valueChangeListener) {
            List<T> values = this.values.getDefaultList();
            if (values.isEmpty()) {
                throw new IllegalStateException("No values for cycle button");
            }
            T initialValue = this.defaultValueSupplier.get();
            int initialIndex = values.indexOf(initialValue);
            Component valueText = this.valueStringifier.apply(initialValue);
            Component initialTitle = this.displayState == DisplayState.VALUE ? valueText : CommonComponents.optionNameValue(name, valueText);
            return new CycleButton<T>(x, y, width, height, initialTitle, name, initialIndex, initialValue, this.defaultValueSupplier, this.values, this.valueStringifier, this.narrationProvider, valueChangeListener, this.tooltipSupplier, this.displayState, this.spriteSupplier);
        }
    }
}

