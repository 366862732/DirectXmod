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

public record TextInput(int width, Component label, boolean labelVisible, String initial, int maxLength, Optional<MultilineOptions> multiline) implements InputControl
{
    public static final MapCodec<TextInput> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Dialog.WIDTH_CODEC.optionalFieldOf("width", (Object)200).forGetter(TextInput::width), (App)ComponentSerialization.CODEC.fieldOf("label").forGetter(TextInput::label), (App)Codec.BOOL.optionalFieldOf("label_visible", (Object)true).forGetter(TextInput::labelVisible), (App)Codec.STRING.optionalFieldOf("initial", (Object)"").forGetter(TextInput::initial), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_length", (Object)32).forGetter(TextInput::maxLength), (App)MultilineOptions.CODEC.optionalFieldOf("multiline").forGetter(TextInput::multiline)).apply((Applicative)i, TextInput::new)).validate(o -> {
        if (o.initial.length() > o.maxLength()) {
            return DataResult.error(() -> "Default text length exceeds allowed size");
        }
        return DataResult.success((Object)o);
    });

    public MapCodec<TextInput> mapCodec() {
        return MAP_CODEC;
    }

    public record MultilineOptions(Optional<Integer> maxLines, Optional<Integer> height) {
        public static final int MAX_HEIGHT = 512;
        public static final Codec<MultilineOptions> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_lines").forGetter(MultilineOptions::maxLines), (App)ExtraCodecs.intRange(1, 512).optionalFieldOf("height").forGetter(MultilineOptions::height)).apply((Applicative)i, MultilineOptions::new));
    }
}

