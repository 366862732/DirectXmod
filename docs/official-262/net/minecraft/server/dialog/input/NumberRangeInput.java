/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.dialog.input;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record NumberRangeInput(int width, Component label, String labelFormat, RangeInfo rangeInfo) implements InputControl
{
    public static final MapCodec<NumberRangeInput> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Dialog.WIDTH_CODEC.optionalFieldOf("width", (Object)200).forGetter(NumberRangeInput::width), (App)ComponentSerialization.CODEC.fieldOf("label").forGetter(NumberRangeInput::label), (App)Codec.STRING.optionalFieldOf("label_format", (Object)"options.generic_value").forGetter(NumberRangeInput::labelFormat), (App)RangeInfo.MAP_CODEC.forGetter(NumberRangeInput::rangeInfo)).apply((Applicative)i, NumberRangeInput::new));

    public MapCodec<NumberRangeInput> mapCodec() {
        return MAP_CODEC;
    }

    public Component computeLabel(String value) {
        return Component.translatable(this.labelFormat, this.label, value);
    }

    public record RangeInfo(float start, float end, Optional<Float> initial, Optional<Float> step) {
        public static final MapCodec<RangeInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("start").forGetter(RangeInfo::start), (App)Codec.FLOAT.fieldOf("end").forGetter(RangeInfo::end), (App)Codec.FLOAT.optionalFieldOf("initial").forGetter(RangeInfo::initial), (App)ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("step").forGetter(RangeInfo::step)).apply((Applicative)i, RangeInfo::new)).validate(range -> {
            if (range.initial.isPresent()) {
                double initial = range.initial.get().floatValue();
                double min = Math.min(range.start, range.end);
                double max = Math.max(range.start, range.end);
                if (initial < min || initial > max) {
                    return DataResult.error(() -> "Initial value " + initial + " is outside of range [" + min + ", " + max + "]");
                }
            }
            return DataResult.success((Object)range);
        });

        public float computeScaledValue(float sliderValue) {
            float deltaToInitial;
            int stepsOutsideInitial;
            float valueInRange = Mth.lerp(sliderValue, this.start, this.end);
            if (this.step.isEmpty()) {
                return valueInRange;
            }
            float step = this.step.get().floatValue();
            float initialValue = this.initialScaledValue();
            float result = initialValue + (float)(stepsOutsideInitial = Math.round((deltaToInitial = valueInRange - initialValue) / step)) * step;
            if (!this.isOutOfRange(result)) {
                return result;
            }
            int oneStepLess = stepsOutsideInitial - Mth.sign(stepsOutsideInitial);
            return initialValue + (float)oneStepLess * step;
        }

        private boolean isOutOfRange(float scaledValue) {
            float sliderPos = this.scaledValueToSlider(scaledValue);
            return (double)sliderPos < 0.0 || (double)sliderPos > 1.0;
        }

        private float initialScaledValue() {
            if (this.initial.isPresent()) {
                return this.initial.get().floatValue();
            }
            return (this.start + this.end) / 2.0f;
        }

        public float initialSliderValue() {
            float value = this.initialScaledValue();
            return this.scaledValueToSlider(value);
        }

        private float scaledValueToSlider(float value) {
            if (this.start == this.end) {
                return 0.5f;
            }
            return Mth.inverseLerp(value, this.start, this.end);
        }
    }
}

