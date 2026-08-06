/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.dialog.input;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandler;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class InputControlHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MapCodec<? extends InputControl>, InputControlHandler<?>> HANDLERS = new HashMap();

    private static <T extends InputControl> void register(MapCodec<T> type, InputControlHandler<? super T> handler) {
        HANDLERS.put(type, handler);
    }

    private static <T extends InputControl> @Nullable InputControlHandler<T> get(T inputControl) {
        return HANDLERS.get(inputControl.mapCodec());
    }

    public static <T extends InputControl> void createHandler(T inputControl, Screen screen, InputControlHandler.Output outputConsumer) {
        InputControlHandler<T> handler = InputControlHandlers.get(inputControl);
        if (handler == null) {
            LOGGER.warn("Unrecognized input control {}", inputControl);
            return;
        }
        handler.addControl(inputControl, screen, outputConsumer);
    }

    public static void bootstrap() {
        InputControlHandlers.register(TextInput.MAP_CODEC, new TextInputHandler());
        InputControlHandlers.register(SingleOptionInput.MAP_CODEC, new SingleOptionHandler());
        InputControlHandlers.register(BooleanInput.MAP_CODEC, new BooleanHandler());
        InputControlHandlers.register(NumberRangeInput.MAP_CODEC, new NumberRangeHandler());
    }

    @Environment(value=EnvType.CLIENT)
    private static class TextInputHandler
    implements InputControlHandler<TextInput> {
        private TextInputHandler() {
        }

        @Override
        public void addControl(TextInput input, Screen screen, InputControlHandler.Output output) {
            Supplier<String> getter;
            AbstractWidget control;
            Font font = screen.getFont();
            if (input.multiline().isPresent()) {
                TextInput.MultilineOptions multiline = input.multiline().get();
                int computedHeight = multiline.height().orElseGet(() -> {
                    int lineCountToFit = multiline.maxLines().orElse(4);
                    return Math.min(font.lineHeight * lineCountToFit + 8, 512);
                });
                MultiLineEditBox editBox = MultiLineEditBox.builder().build(font, input.width(), computedHeight, CommonComponents.EMPTY);
                editBox.setCharacterLimit(input.maxLength());
                multiline.maxLines().ifPresent(editBox::setLineLimit);
                editBox.setValue(input.initial());
                control = editBox;
                getter = editBox::getValue;
            } else {
                EditBox editBox = new EditBox(font, input.width(), 20, input.label());
                editBox.setMaxLength(input.maxLength());
                editBox.setValue(input.initial());
                control = editBox;
                getter = editBox::getValue;
            }
            EditBox wrappedControl = input.labelVisible() ? CommonLayouts.labeledElement(font, control, input.label()) : control;
            output.accept(wrappedControl, new Action.ValueGetter(){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public String asTemplateSubstitution() {
                    return StringTag.escapeWithoutQuotes((String)getter.get());
                }

                @Override
                public Tag asTag() {
                    return StringTag.valueOf((String)getter.get());
                }
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class SingleOptionHandler
    implements InputControlHandler<SingleOptionInput> {
        private SingleOptionHandler() {
        }

        @Override
        public void addControl(SingleOptionInput input, Screen screen, InputControlHandler.Output output) {
            SingleOptionInput.Entry initial = input.initial().orElse(input.entries().getFirst());
            CycleButton.Builder<SingleOptionInput.Entry> controlBuilder = CycleButton.builder(SingleOptionInput.Entry::displayOrDefault, initial).withValues((Collection<SingleOptionInput.Entry>)input.entries()).displayState(!input.labelVisible() ? CycleButton.DisplayState.VALUE : CycleButton.DisplayState.NAME_AND_VALUE);
            CycleButton<SingleOptionInput.Entry> control = controlBuilder.create(0, 0, input.width(), 20, input.label());
            output.accept(control, Action.ValueGetter.of(() -> ((SingleOptionInput.Entry)control.getValue()).id()));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class BooleanHandler
    implements InputControlHandler<BooleanInput> {
        private BooleanHandler() {
        }

        @Override
        public void addControl(final BooleanInput input, Screen screen, InputControlHandler.Output output) {
            Font font = screen.getFont();
            final Checkbox control = Checkbox.builder(input.label(), font).selected(input.initial()).build();
            output.accept(control, new Action.ValueGetter(){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public String asTemplateSubstitution() {
                    return control.selected() ? input.onTrue() : input.onFalse();
                }

                @Override
                public Tag asTag() {
                    return ByteTag.valueOf(control.selected());
                }
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class NumberRangeHandler
    implements InputControlHandler<NumberRangeInput> {
        private NumberRangeHandler() {
        }

        @Override
        public void addControl(NumberRangeInput input, Screen screen, InputControlHandler.Output output) {
            float initialValue = input.rangeInfo().initialSliderValue();
            final SliderImpl control = new SliderImpl(input, initialValue);
            output.accept(control, new Action.ValueGetter(){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public String asTemplateSubstitution() {
                    return control.stringValueToSend();
                }

                @Override
                public Tag asTag() {
                    return FloatTag.valueOf(control.floatValueToSend());
                }
            });
        }

        @Environment(value=EnvType.CLIENT)
        private static class SliderImpl
        extends AbstractSliderButton {
            private final NumberRangeInput input;

            private SliderImpl(NumberRangeInput input, double initialSliderValue) {
                super(0, 0, input.width(), 20, SliderImpl.computeMessage(input, initialSliderValue), initialSliderValue);
                this.input = input;
            }

            @Override
            protected void updateMessage() {
                this.setMessage(SliderImpl.computeMessage(this.input, this.value));
            }

            @Override
            protected void applyValue() {
            }

            public String stringValueToSend() {
                return SliderImpl.sliderValueToString(this.input, this.value);
            }

            public float floatValueToSend() {
                return SliderImpl.scaledValue(this.input, this.value);
            }

            private static float scaledValue(NumberRangeInput input, double sliderValue) {
                return input.rangeInfo().computeScaledValue((float)sliderValue);
            }

            private static String sliderValueToString(NumberRangeInput input, double sliderValue) {
                return SliderImpl.valueToString(SliderImpl.scaledValue(input, sliderValue));
            }

            private static Component computeMessage(NumberRangeInput input, double sliderValue) {
                return input.computeLabel(SliderImpl.sliderValueToString(input, sliderValue));
            }

            private static String valueToString(float v) {
                int intV = (int)v;
                if ((float)intV == v) {
                    return Integer.toString(intV);
                }
                return Float.toString(v);
            }
        }
    }
}

