/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public final class Style {
    public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null, null);
    public static final int NO_SHADOW = 0;
    private final @Nullable TextColor color;
    private final @Nullable Integer shadowColor;
    private final @Nullable Boolean bold;
    private final @Nullable Boolean italic;
    private final @Nullable Boolean underlined;
    private final @Nullable Boolean strikethrough;
    private final @Nullable Boolean obfuscated;
    private final @Nullable ClickEvent clickEvent;
    private final @Nullable HoverEvent hoverEvent;
    private final @Nullable String insertion;
    private final @Nullable FontDescription font;

    private static Style create(Optional<TextColor> color, Optional<Integer> shadowColor, Optional<Boolean> bold, Optional<Boolean> italic, Optional<Boolean> underlined, Optional<Boolean> strikethrough, Optional<Boolean> obfuscated, Optional<ClickEvent> clickEvent, Optional<HoverEvent> hoverEvent, Optional<String> insertion, Optional<FontDescription> font) {
        Style result = new Style(color.orElse(null), shadowColor.orElse(null), bold.orElse(null), italic.orElse(null), underlined.orElse(null), strikethrough.orElse(null), obfuscated.orElse(null), clickEvent.orElse(null), hoverEvent.orElse(null), insertion.orElse(null), font.orElse(null));
        if (result.equals(EMPTY)) {
            return EMPTY;
        }
        return result;
    }

    private Style(@Nullable TextColor color, @Nullable Integer shadowColor, @Nullable Boolean bold, @Nullable Boolean italic, @Nullable Boolean underlined, @Nullable Boolean strikethrough, @Nullable Boolean obfuscated, @Nullable ClickEvent clickEvent, @Nullable HoverEvent hoverEvent, @Nullable String insertion, @Nullable FontDescription font) {
        this.color = color;
        this.shadowColor = shadowColor;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
        this.insertion = insertion;
        this.font = font;
    }

    public @Nullable TextColor getColor() {
        return this.color;
    }

    public @Nullable Integer getShadowColor() {
        return this.shadowColor;
    }

    public boolean isBold() {
        return this.bold == Boolean.TRUE;
    }

    public boolean isItalic() {
        return this.italic == Boolean.TRUE;
    }

    public boolean isStrikethrough() {
        return this.strikethrough == Boolean.TRUE;
    }

    public boolean isUnderlined() {
        return this.underlined == Boolean.TRUE;
    }

    public boolean isObfuscated() {
        return this.obfuscated == Boolean.TRUE;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public @Nullable ClickEvent getClickEvent() {
        return this.clickEvent;
    }

    public @Nullable HoverEvent getHoverEvent() {
        return this.hoverEvent;
    }

    public @Nullable String getInsertion() {
        return this.insertion;
    }

    public FontDescription getFont() {
        return this.font != null ? this.font : FontDescription.DEFAULT;
    }

    private static <T> Style checkEmptyAfterChange(Style newStyle, @Nullable T previous, @Nullable T next) {
        if (previous != null && next == null && newStyle.equals(EMPTY)) {
            return EMPTY;
        }
        return newStyle;
    }

    public Style withColor(@Nullable TextColor color) {
        if (Objects.equals(this.color, color)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.color, color);
    }

    public Style withColor(@Nullable ChatFormatting color) {
        return this.withColor(color != null ? TextColor.fromLegacyFormat(color) : null);
    }

    public Style withColor(int color) {
        return this.withColor(TextColor.fromRgb(color));
    }

    public Style withShadowColor(int shadowColor) {
        if (Objects.equals(this.shadowColor, shadowColor)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.shadowColor, shadowColor);
    }

    public Style withoutShadow() {
        return this.withShadowColor(0);
    }

    public Style withBold(@Nullable Boolean bold) {
        if (Objects.equals(this.bold, bold)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.bold, bold);
    }

    public Style withItalic(@Nullable Boolean italic) {
        if (Objects.equals(this.italic, italic)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.italic, italic);
    }

    public Style withUnderlined(@Nullable Boolean underlined) {
        if (Objects.equals(this.underlined, underlined)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.underlined, underlined);
    }

    public Style withStrikethrough(@Nullable Boolean strikethrough) {
        if (Objects.equals(this.strikethrough, strikethrough)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.strikethrough, strikethrough);
    }

    public Style withObfuscated(@Nullable Boolean obfuscated) {
        if (Objects.equals(this.obfuscated, obfuscated)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.obfuscated, obfuscated);
    }

    public Style withClickEvent(@Nullable ClickEvent clickEvent) {
        if (Objects.equals(this.clickEvent, clickEvent)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, clickEvent, this.hoverEvent, this.insertion, this.font), this.clickEvent, clickEvent);
    }

    public Style withHoverEvent(@Nullable HoverEvent hoverEvent) {
        if (Objects.equals(this.hoverEvent, hoverEvent)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, hoverEvent, this.insertion, this.font), this.hoverEvent, hoverEvent);
    }

    public Style withInsertion(@Nullable String insertion) {
        if (Objects.equals(this.insertion, insertion)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, insertion, this.font), this.insertion, insertion);
    }

    public Style withFont(@Nullable FontDescription font) {
        if (Objects.equals(this.font, font)) {
            return this;
        }
        return Style.checkEmptyAfterChange(new Style(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, font), this.font, font);
    }

    public Style applyFormat(ChatFormatting format) {
        TextColor color = this.color;
        Boolean bold = this.bold;
        Boolean italic = this.italic;
        Boolean strikethrough = this.strikethrough;
        Boolean underlined = this.underlined;
        Boolean obfuscated = this.obfuscated;
        switch (format) {
            case OBFUSCATED: {
                obfuscated = true;
                break;
            }
            case BOLD: {
                bold = true;
                break;
            }
            case STRIKETHROUGH: {
                strikethrough = true;
                break;
            }
            case UNDERLINE: {
                underlined = true;
                break;
            }
            case ITALIC: {
                italic = true;
                break;
            }
            case RESET: {
                return EMPTY;
            }
            default: {
                color = TextColor.fromLegacyFormat(format);
            }
        }
        return new Style(color, this.shadowColor, bold, italic, underlined, strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style applyLegacyFormat(ChatFormatting format) {
        TextColor color = this.color;
        Boolean bold = this.bold;
        Boolean italic = this.italic;
        Boolean strikethrough = this.strikethrough;
        Boolean underlined = this.underlined;
        Boolean obfuscated = this.obfuscated;
        switch (format) {
            case OBFUSCATED: {
                obfuscated = true;
                break;
            }
            case BOLD: {
                bold = true;
                break;
            }
            case STRIKETHROUGH: {
                strikethrough = true;
                break;
            }
            case UNDERLINE: {
                underlined = true;
                break;
            }
            case ITALIC: {
                italic = true;
                break;
            }
            case RESET: {
                return EMPTY;
            }
            default: {
                obfuscated = false;
                bold = false;
                strikethrough = false;
                underlined = false;
                italic = false;
                color = TextColor.fromLegacyFormat(format);
            }
        }
        return new Style(color, this.shadowColor, bold, italic, underlined, strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style applyFormats(ChatFormatting ... formats) {
        TextColor color = this.color;
        Boolean bold = this.bold;
        Boolean italic = this.italic;
        Boolean strikethrough = this.strikethrough;
        Boolean underlined = this.underlined;
        Boolean obfuscated = this.obfuscated;
        block8: for (ChatFormatting format : formats) {
            switch (format) {
                case OBFUSCATED: {
                    obfuscated = true;
                    continue block8;
                }
                case BOLD: {
                    bold = true;
                    continue block8;
                }
                case STRIKETHROUGH: {
                    strikethrough = true;
                    continue block8;
                }
                case UNDERLINE: {
                    underlined = true;
                    continue block8;
                }
                case ITALIC: {
                    italic = true;
                    continue block8;
                }
                case RESET: {
                    return EMPTY;
                }
                default: {
                    color = TextColor.fromLegacyFormat(format);
                }
            }
        }
        return new Style(color, this.shadowColor, bold, italic, underlined, strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
    }

    public Style applyTo(Style other) {
        if (this == EMPTY) {
            return other;
        }
        if (other == EMPTY) {
            return this;
        }
        return new Style(this.color != null ? this.color : other.color, this.shadowColor != null ? this.shadowColor : other.shadowColor, this.bold != null ? this.bold : other.bold, this.italic != null ? this.italic : other.italic, this.underlined != null ? this.underlined : other.underlined, this.strikethrough != null ? this.strikethrough : other.strikethrough, this.obfuscated != null ? this.obfuscated : other.obfuscated, this.clickEvent != null ? this.clickEvent : other.clickEvent, this.hoverEvent != null ? this.hoverEvent : other.hoverEvent, this.insertion != null ? this.insertion : other.insertion, this.font != null ? this.font : other.font);
    }

    public String toString() {
        final StringBuilder result = new StringBuilder("{");
        class Collector {
            private boolean isNotFirst;

            Collector() {
                Objects.requireNonNull(this$0);
            }

            private void prependSeparator() {
                if (this.isNotFirst) {
                    result.append(',');
                }
                this.isNotFirst = true;
            }

            private void addFlagString(String name, @Nullable Boolean value) {
                if (value != null) {
                    this.prependSeparator();
                    if (!value.booleanValue()) {
                        result.append('!');
                    }
                    result.append(name);
                }
            }

            private void addValueString(String name, @Nullable Object value) {
                if (value != null) {
                    this.prependSeparator();
                    result.append(name);
                    result.append('=');
                    result.append(value);
                }
            }
        }
        Collector collector = new Collector();
        collector.addValueString("color", this.color);
        collector.addValueString("shadowColor", this.shadowColor);
        collector.addFlagString("bold", this.bold);
        collector.addFlagString("italic", this.italic);
        collector.addFlagString("underlined", this.underlined);
        collector.addFlagString("strikethrough", this.strikethrough);
        collector.addFlagString("obfuscated", this.obfuscated);
        collector.addValueString("clickEvent", this.clickEvent);
        collector.addValueString("hoverEvent", this.hoverEvent);
        collector.addValueString("insertion", this.insertion);
        collector.addValueString("font", this.font);
        result.append("}");
        return result.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Style) {
            Style style = (Style)o;
            return this.bold == style.bold && Objects.equals(this.getColor(), style.getColor()) && Objects.equals(this.getShadowColor(), style.getShadowColor()) && this.italic == style.italic && this.obfuscated == style.obfuscated && this.strikethrough == style.strikethrough && this.underlined == style.underlined && Objects.equals(this.clickEvent, style.clickEvent) && Objects.equals(this.hoverEvent, style.hoverEvent) && Objects.equals(this.insertion, style.insertion) && Objects.equals(this.font, style.font);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion);
    }

    public static class Serializer {
        public static final MapCodec<Style> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)TextColor.CODEC.optionalFieldOf("color").forGetter(o -> Optional.ofNullable(o.color)), (App)ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("shadow_color").forGetter(o -> Optional.ofNullable(o.shadowColor)), (App)Codec.BOOL.optionalFieldOf("bold").forGetter(o -> Optional.ofNullable(o.bold)), (App)Codec.BOOL.optionalFieldOf("italic").forGetter(o -> Optional.ofNullable(o.italic)), (App)Codec.BOOL.optionalFieldOf("underlined").forGetter(o -> Optional.ofNullable(o.underlined)), (App)Codec.BOOL.optionalFieldOf("strikethrough").forGetter(o -> Optional.ofNullable(o.strikethrough)), (App)Codec.BOOL.optionalFieldOf("obfuscated").forGetter(o -> Optional.ofNullable(o.obfuscated)), (App)ClickEvent.CODEC.optionalFieldOf("click_event").forGetter(o -> Optional.ofNullable(o.clickEvent)), (App)HoverEvent.CODEC.optionalFieldOf("hover_event").forGetter(o -> Optional.ofNullable(o.hoverEvent)), (App)Codec.STRING.optionalFieldOf("insertion").forGetter(o -> Optional.ofNullable(o.insertion)), (App)FontDescription.CODEC.optionalFieldOf("font").forGetter(o -> Optional.ofNullable(o.font))).apply((Applicative)i, Style::create));
        public static final Codec<Style> CODEC = MAP_CODEC.codec();
        public static final StreamCodec<RegistryFriendlyByteBuf, Style> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
    }
}

