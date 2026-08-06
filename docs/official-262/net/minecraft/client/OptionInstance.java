/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public final class OptionInstance<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Enum<Boolean> BOOLEAN_VALUES = new Enum(ImmutableList.of((Object)Boolean.TRUE, (Object)Boolean.FALSE), Codec.BOOL);
    public static final CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (component, b) -> b != false ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    public static final ValueUpdateListener<Object> NO_ACTION = object -> {};
    private final TooltipSupplier<T> tooltip;
    private final Function<T, Component> toString;
    private final ValueSet<T> values;
    private final Codec<T> codec;
    private final T initialValue;
    private final ValueUpdateListener<? super T> onValueUpdate;
    private final Component caption;
    private T value;

    public static OptionInstance<Boolean> createBoolean(String captionId, boolean initialValue, ValueUpdateListener<? super Boolean> onValueUpdate) {
        return OptionInstance.createBoolean(captionId, OptionInstance.noTooltip(), initialValue, onValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(String captionId, boolean initialValue) {
        return OptionInstance.createBoolean(captionId, OptionInstance.noTooltip(), initialValue, NO_ACTION);
    }

    public static OptionInstance<Boolean> createBoolean(String captionId, TooltipSupplier<Boolean> tooltip, boolean initialValue) {
        return OptionInstance.createBoolean(captionId, tooltip, initialValue, NO_ACTION);
    }

    public static OptionInstance<Boolean> createBoolean(String captionId, TooltipSupplier<Boolean> tooltip, boolean initialValue, ValueUpdateListener<? super Boolean> onValueUpdate) {
        return OptionInstance.createBoolean(captionId, tooltip, BOOLEAN_TO_STRING, initialValue, onValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(String captionId, TooltipSupplier<Boolean> tooltip, CaptionBasedToString<Boolean> toString, boolean initialValue, ValueUpdateListener<? super Boolean> onValueUpdate) {
        return new OptionInstance<Boolean>(captionId, tooltip, toString, BOOLEAN_VALUES, initialValue, onValueUpdate);
    }

    public OptionInstance(String captionId, TooltipSupplier<T> tooltip, CaptionBasedToString<T> toString, ValueSet<T> values, T initialValue, ValueUpdateListener<? super T> onValueUpdate) {
        this(captionId, (TooltipSupplier<? super T>)tooltip, (CaptionBasedToString<? super T>)toString, (ValueSet<? super T>)values, (Codec<? super T>)values.codec(), (T)initialValue, onValueUpdate);
    }

    public OptionInstance(String captionId, TooltipSupplier<T> tooltip, CaptionBasedToString<T> toString, ValueSet<T> values, Codec<T> codec, T initialValue, ValueUpdateListener<? super T> onValueUpdate) {
        this.caption = Component.translatable(captionId);
        this.tooltip = tooltip;
        this.toString = value -> toString.toString(this.caption, value);
        this.values = values;
        this.codec = codec;
        this.initialValue = initialValue;
        this.onValueUpdate = onValueUpdate;
        this.value = this.initialValue;
    }

    public static <T> TooltipSupplier<T> noTooltip() {
        return object -> null;
    }

    public static <T> TooltipSupplier<T> cachedConstantTooltip(Component tooltipComponent) {
        return object -> Tooltip.create(tooltipComponent);
    }

    public AbstractWidget createButton(Options options) {
        return this.createButton(options, 0, 0, 150);
    }

    public AbstractWidget createButton(Options options, int x, int y, int width) {
        return this.createButton(options, x, y, width, NO_ACTION);
    }

    public AbstractWidget createButton(Options options, int x, int y, int width, ValueUpdateListener<? super T> onValueChanged) {
        return this.values.createButton(this.tooltip, options, x, y, width, onValueChanged).apply(this);
    }

    public T get() {
        return this.value;
    }

    public Codec<T> codec() {
        return this.codec;
    }

    public String toString() {
        return this.caption.getString();
    }

    public void set(T value) {
        Object newValue = this.values.validateValue(value).orElseGet(() -> {
            LOGGER.error("Illegal option value {} for {}", value, (Object)this.caption.getString());
            return this.initialValue;
        });
        if (!Minecraft.getInstance().isRunning()) {
            this.value = newValue;
            return;
        }
        if (!Objects.equals(this.value, newValue)) {
            this.value = newValue;
            this.onValueUpdate.valueChanged(newValue);
        }
    }

    public ValueSet<T> values() {
        return this.values;
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface TooltipSupplier<T> {
        public @Nullable Tooltip apply(T var1);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface ValueUpdateListener<T> {
        public void valueChanged(T var1);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface CaptionBasedToString<T> {
        public Component toString(Component var1, T var2);
    }

    @Environment(value=EnvType.CLIENT)
    public record Enum<T>(List<T> values, Codec<T> codec) implements CycleableValueSet<T>
    {
        @Override
        public Optional<T> validateValue(T value) {
            return this.values.contains(value) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface ValueSet<T> {
        public Function<OptionInstance<T>, AbstractWidget> createButton(TooltipSupplier<T> var1, Options var2, int var3, int var4, int var5, ValueUpdateListener<? super T> var6);

        public Optional<T> validateValue(T var1);

        public Codec<T> codec();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum UnitDouble implements SliderableValueSet<Double>
    {
        INSTANCE;


        @Override
        public Optional<Double> validateValue(Double value) {
            return value >= 0.0 && value <= 1.0 ? Optional.of(value) : Optional.empty();
        }

        @Override
        public double toSliderValue(Double value) {
            return value;
        }

        @Override
        public Double fromSliderValue(double slider) {
            return slider;
        }

        public <R> SliderableValueSet<R> xmap(final DoubleFunction<? extends R> to, final ToDoubleFunction<? super R> from) {
            return new SliderableValueSet<R>(this){
                final /* synthetic */ UnitDouble this$0;
                {
                    UnitDouble unitDouble = this$0;
                    Objects.requireNonNull(unitDouble);
                    this.this$0 = unitDouble;
                }

                @Override
                public Optional<R> validateValue(R value) {
                    return this.this$0.validateValue(from.applyAsDouble(value)).map(to::apply);
                }

                @Override
                public double toSliderValue(R value) {
                    return this.this$0.toSliderValue(from.applyAsDouble(value));
                }

                @Override
                public R fromSliderValue(double slider) {
                    return to.apply(this.this$0.fromSliderValue(slider));
                }

                @Override
                public Codec<R> codec() {
                    return this.this$0.codec().xmap(to::apply, from::applyAsDouble);
                }
            };
        }

        @Override
        public Codec<Double> codec() {
            return Codec.withAlternative((Codec)Codec.doubleRange((double)0.0, (double)1.0), (Codec)Codec.BOOL, b -> b != false ? 1.0 : 0.0);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record SliderableEnum<T>(List<T> values, Codec<T> codec) implements SliderableValueSet<T>
    {
        @Override
        public double toSliderValue(T value) {
            if (value == this.values.getFirst()) {
                return 0.0;
            }
            if (value == this.values.getLast()) {
                return 1.0;
            }
            return Mth.map((double)this.values.indexOf(value), 0.0, (double)(this.values.size() - 1), 0.0, 1.0);
        }

        @Override
        public Optional<T> next(T current) {
            int currentIntex = this.values.indexOf(current);
            int nextIndex = Mth.clamp(currentIntex + 1, 0, this.values.size() - 1);
            return Optional.of(this.values.get(nextIndex));
        }

        @Override
        public Optional<T> previous(T current) {
            int currentIntex = this.values.indexOf(current);
            int previousIndex = Mth.clamp(currentIntex - 1, 0, this.values.size() - 1);
            return Optional.of(this.values.get(previousIndex));
        }

        @Override
        public T fromSliderValue(double slider) {
            if (slider >= 1.0) {
                slider = 0.99999f;
            }
            int index = Mth.floor(Mth.map(slider, 0.0, 1.0, 0.0, (double)this.values.size()));
            return this.values.get(Mth.clamp(index, 0, this.values.size() - 1));
        }

        @Override
        public Optional<T> validateValue(T value) {
            int index = this.values.indexOf(value);
            return index > -1 ? Optional.of(value) : Optional.empty();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive) implements IntRangeBase,
    SliderableOrCyclableValueSet<Integer>
    {
        @Override
        public Optional<Integer> validateValue(Integer value) {
            return Optional.of(Mth.clamp(value, this.minInclusive(), this.maxInclusive()));
        }

        @Override
        public int maxInclusive() {
            return this.maxSupplier.getAsInt();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.INT.validate(value -> {
                int maxExclusive = this.encodableMaxInclusive + 1;
                if (value.compareTo(this.minInclusive) >= 0 && value.compareTo(maxExclusive) <= 0) {
                    return DataResult.success((Object)value);
                }
                return DataResult.error(() -> "Value " + value + " outside of range [" + this.minInclusive + ":" + maxExclusive + "]", (Object)value);
            });
        }

        @Override
        public boolean createCycleButton() {
            return true;
        }

        @Override
        public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements IntRangeBase
    {
        public IntRange(int minInclusive, int maxInclusive) {
            this(minInclusive, maxInclusive, true);
        }

        @Override
        public Optional<Integer> validateValue(Integer value) {
            return value.compareTo(this.minInclusive()) >= 0 && value.compareTo(this.maxInclusive()) <= 0 ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange((int)this.minInclusive, (int)(this.maxInclusive + 1));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface IntRangeBase
    extends SliderableValueSet<Integer> {
        public int minInclusive();

        public int maxInclusive();

        @Override
        default public Optional<Integer> next(Integer current) {
            return Optional.of(current + 1);
        }

        @Override
        default public Optional<Integer> previous(Integer current) {
            return Optional.of(current - 1);
        }

        @Override
        default public double toSliderValue(Integer value) {
            if (value.intValue() == this.minInclusive()) {
                return 0.0;
            }
            if (value.intValue() == this.maxInclusive()) {
                return 1.0;
            }
            return Mth.map((double)value.intValue() + 0.5, (double)this.minInclusive(), (double)this.maxInclusive() + 1.0, 0.0, 1.0);
        }

        @Override
        default public Integer fromSliderValue(double slider) {
            if (slider >= 1.0) {
                slider = 0.99999f;
            }
            return Mth.floor(Mth.map(slider, 0.0, 1.0, (double)this.minInclusive(), (double)this.maxInclusive() + 1.0));
        }

        default public <R> SliderableValueSet<R> xmap(final IntFunction<? extends R> to, final ToIntFunction<? super R> from, final boolean discrete) {
            return new SliderableValueSet<R>(this){
                final /* synthetic */ IntRangeBase this$0;
                {
                    IntRangeBase intRangeBase = this$0;
                    Objects.requireNonNull(intRangeBase);
                    this.this$0 = intRangeBase;
                }

                @Override
                public Optional<R> validateValue(R value) {
                    return this.this$0.validateValue(from.applyAsInt(value)).map(to::apply);
                }

                @Override
                public double toSliderValue(R value) {
                    return this.this$0.toSliderValue(from.applyAsInt(value));
                }

                @Override
                public Optional<R> next(R current) {
                    if (!discrete) {
                        return Optional.empty();
                    }
                    int currentIndex = from.applyAsInt(current);
                    return Optional.of(to.apply(this.this$0.validateValue(currentIndex + 1).orElse(currentIndex)));
                }

                @Override
                public Optional<R> previous(R current) {
                    if (!discrete) {
                        return Optional.empty();
                    }
                    int currentIndex = from.applyAsInt(current);
                    return Optional.of(to.apply(this.this$0.validateValue(currentIndex - 1).orElse(currentIndex)));
                }

                @Override
                public R fromSliderValue(double slider) {
                    return to.apply(this.this$0.fromSliderValue(slider));
                }

                @Override
                public Codec<R> codec() {
                    return this.this$0.codec().xmap(to::apply, from::applyAsInt);
                }
            };
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static final class OptionInstanceSliderButton<N>
    extends AbstractOptionSliderButton
    implements ResettableOptionWidget {
        private final OptionInstance<N> instance;
        private final SliderableValueSet<N> values;
        private final TooltipSupplier<N> tooltipSupplier;
        private final ValueUpdateListener<? super N> onValueChanged;
        private @Nullable Long delayedApplyAt;
        private final boolean applyValueImmediately;

        private OptionInstanceSliderButton(Options options, int x, int y, int width, int height, OptionInstance<N> instance, SliderableValueSet<N> values, TooltipSupplier<N> tooltipSupplier, ValueUpdateListener<? super N> onValueChanged, boolean applyValueImmediately) {
            super(options, x, y, width, height, values.toSliderValue(instance.get()));
            this.instance = instance;
            this.values = values;
            this.tooltipSupplier = tooltipSupplier;
            this.onValueChanged = onValueChanged;
            this.applyValueImmediately = applyValueImmediately;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.instance.toString.apply(this.values.fromSliderValue(this.value)));
            this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
        }

        @Override
        protected void applyValue() {
            if (this.applyValueImmediately) {
                this.applyUnsavedValue();
            } else {
                this.delayedApplyAt = Util.getMillis() + 600L;
            }
        }

        public void applyUnsavedValue() {
            N sliderValue = this.values.fromSliderValue(this.value);
            if (!Objects.equals(sliderValue, this.instance.get())) {
                this.instance.set(sliderValue);
                this.onValueChanged.valueChanged(this.instance.get());
            }
        }

        @Override
        public void resetValue() {
            if (this.value != this.values.toSliderValue(this.instance.get())) {
                this.value = this.values.toSliderValue(this.instance.get());
                this.delayedApplyAt = null;
                this.updateMessage();
            }
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
            if (this.delayedApplyAt != null && Util.getMillis() >= this.delayedApplyAt) {
                this.delayedApplyAt = null;
                this.applyUnsavedValue();
                this.resetValue();
            }
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            if (this.applyValueImmediately) {
                this.resetValue();
            }
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                this.canChangeValue = !this.canChangeValue;
                return true;
            }
            if (this.canChangeValue) {
                Optional<N> next;
                Optional<N> previous;
                boolean left = event.isLeft();
                boolean right = event.isRight();
                if (left && (previous = this.values.previous(this.values.fromSliderValue(this.value))).isPresent()) {
                    this.setValue(this.values.toSliderValue(previous.get()));
                    return true;
                }
                if (right && (next = this.values.next(this.values.fromSliderValue(this.value))).isPresent()) {
                    this.setValue(this.values.toSliderValue(next.get()));
                    return true;
                }
                if (left || right) {
                    float direction = left ? -1.0f : 1.0f;
                    this.setValue(this.value + (double)(direction / (float)(this.width - 8)));
                    return true;
                }
            }
            return false;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec) implements CycleableValueSet<T>
    {
        @Override
        public Optional<T> validateValue(T value) {
            return this.validateValue.apply(value);
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create((Collection)this.values.get());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record AltEnum<T>(List<T> values, List<T> altValues, BooleanSupplier altCondition, CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec) implements CycleableValueSet<T>
    {
        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
        }

        @Override
        public Optional<T> validateValue(T value) {
            return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(value) ? Optional.of(value) : Optional.empty();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface SliderableOrCyclableValueSet<T>
    extends SliderableValueSet<T>,
    CycleableValueSet<T> {
        public boolean createCycleButton();

        @Override
        default public Function<OptionInstance<T>, AbstractWidget> createButton(TooltipSupplier<T> tooltip, Options options, int x, int y, int width, ValueUpdateListener<? super T> onValueChanged) {
            if (this.createCycleButton()) {
                return CycleableValueSet.super.createButton(tooltip, options, x, y, width, onValueChanged);
            }
            return SliderableValueSet.super.createButton(tooltip, options, x, y, width, onValueChanged);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface CycleableValueSet<T>
    extends ValueSet<T> {
        public CycleButton.ValueListSupplier<T> valueListSupplier();

        default public ValueSetter<T> valueSetter() {
            return OptionInstance::set;
        }

        @Override
        default public Function<OptionInstance<T>, AbstractWidget> createButton(TooltipSupplier<T> tooltip, Options options, int x, int y, int width, ValueUpdateListener<? super T> onValueChanged) {
            return instance -> CycleButton.builder(instance.toString, instance::get).withValues(this.valueListSupplier()).withTooltip(tooltip).create(x, y, width, 20, instance.caption, (cycleButton, value) -> {
                this.valueSetter().set((OptionInstance<Object>)instance, value);
                options.save();
                onValueChanged.valueChanged(value);
            });
        }

        @Environment(value=EnvType.CLIENT)
        public static interface ValueSetter<T> {
            public void set(OptionInstance<T> var1, T var2);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface SliderableValueSet<T>
    extends ValueSet<T> {
        public double toSliderValue(T var1);

        default public Optional<T> next(T current) {
            return Optional.empty();
        }

        default public Optional<T> previous(T current) {
            return Optional.empty();
        }

        public T fromSliderValue(double var1);

        default public boolean applyValueImmediately() {
            return true;
        }

        @Override
        default public Function<OptionInstance<T>, AbstractWidget> createButton(TooltipSupplier<T> tooltip, Options options, int x, int y, int width, ValueUpdateListener<? super T> onValueChanged) {
            return instance -> new OptionInstanceSliderButton(options, x, y, width, 20, instance, this, tooltip, onValueChanged, this.applyValueImmediately());
        }
    }
}

