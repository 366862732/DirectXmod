/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat.contents;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public interface PlainTextContents
extends ComponentContents {
    public static final MapCodec<PlainTextContents> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("text").forGetter(PlainTextContents::text)).apply((Applicative)i, PlainTextContents::create));
    public static final PlainTextContents EMPTY = new PlainTextContents(){

        public String toString() {
            return "empty";
        }

        @Override
        public String text() {
            return "";
        }
    };

    public static PlainTextContents create(String text) {
        return text.isEmpty() ? EMPTY : new LiteralContents(text);
    }

    public String text();

    default public MapCodec<PlainTextContents> codec() {
        return MAP_CODEC;
    }

    public record LiteralContents(String text) implements PlainTextContents
    {
        @Override
        public <T> Optional<T> visit(FormattedText.ContentConsumer<T> output) {
            return output.accept(this.text);
        }

        @Override
        public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> output, Style currentStyle) {
            return output.accept(currentStyle, this.text);
        }

        @Override
        public String toString() {
            return "literal{" + this.text + "}";
        }
    }
}

