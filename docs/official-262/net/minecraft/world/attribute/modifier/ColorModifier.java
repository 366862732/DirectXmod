/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.attribute.modifier;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public interface ColorModifier<Argument>
extends AttributeModifier<Integer, Argument> {
    public static final ColorModifier<Integer> ALPHA_BLEND = new ColorModifier<Integer>(){

        @Override
        public Integer apply(Integer subject, Integer argument) {
            return ARGB.alphaBlend(subject, argument);
        }

        @Override
        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) {
            return ExtraCodecs.STRING_ARGB_COLOR;
        }

        @Override
        public LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> type) {
            return LerpFunction.ofColor();
        }
    };
    public static final ColorModifier<Integer> ADD = ARGB::addRgb;
    public static final ColorModifier<Integer> SUBTRACT = ARGB::subtractRgb;
    public static final ColorModifier<Integer> MULTIPLY_RGB = ARGB::multiply;
    public static final ColorModifier<Integer> MULTIPLY_ARGB = ARGB::multiply;
    public static final ColorModifier<BlendToGray> BLEND_TO_GRAY = new ColorModifier<BlendToGray>(){

        @Override
        public Integer apply(Integer subject, BlendToGray argument) {
            int multipliedGreyscale = ARGB.scaleRGB(ARGB.greyscale(subject), argument.brightness);
            return ARGB.srgbLerp(argument.factor, subject, multipliedGreyscale);
        }

        @Override
        public Codec<BlendToGray> argumentCodec(EnvironmentAttribute<Integer> type) {
            return BlendToGray.CODEC;
        }

        @Override
        public LerpFunction<BlendToGray> argumentKeyframeLerp(EnvironmentAttribute<Integer> type) {
            return (alpha, from, to) -> new BlendToGray(Mth.lerp(alpha, from.brightness, to.brightness), Mth.lerp(alpha, from.factor, to.factor));
        }
    };

    @FunctionalInterface
    public static interface RgbModifier
    extends ColorModifier<Integer> {
        @Override
        default public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) {
            return ExtraCodecs.STRING_RGB_COLOR;
        }

        @Override
        default public LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> type) {
            return LerpFunction.ofColor();
        }
    }

    @FunctionalInterface
    public static interface ArgbModifier
    extends ColorModifier<Integer> {
        @Override
        default public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) {
            return Codec.either(ExtraCodecs.STRING_ARGB_COLOR, ExtraCodecs.RGB_COLOR_CODEC).xmap(Either::unwrap, color -> ARGB.alpha(color) == 255 ? Either.right((Object)color) : Either.left((Object)color));
        }

        @Override
        default public LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> type) {
            return LerpFunction.ofColor();
        }
    }

    public record BlendToGray(float brightness, float factor) {
        public static final Codec<BlendToGray> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("brightness").forGetter(BlendToGray::brightness), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("factor").forGetter(BlendToGray::factor)).apply((Applicative)i, BlendToGray::new));
    }
}

