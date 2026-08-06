/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Lifecycle
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class TextColor {
    private static final String CUSTOM_COLOR_PREFIX = "#";
    public static final Codec<TextColor> CODEC = Codec.STRING.comapFlatMap(TextColor::parseColor, TextColor::serialize);
    private static final Map<String, TextColor> NAMED_COLORS = new HashMap<String, TextColor>();
    public static final TextColor BLACK = TextColor.named("black", 0);
    public static final TextColor DARK_BLUE = TextColor.named("dark_blue", 170);
    public static final TextColor DARK_GREEN = TextColor.named("dark_green", 43520);
    public static final TextColor DARK_AQUA = TextColor.named("dark_aqua", 43690);
    public static final TextColor DARK_RED = TextColor.named("dark_red", 0xAA0000);
    public static final TextColor DARK_PURPLE = TextColor.named("dark_purple", 0xAA00AA);
    public static final TextColor GOLD = TextColor.named("gold", 0xFFAA00);
    public static final TextColor GRAY = TextColor.named("gray", 0xAAAAAA);
    public static final TextColor DARK_GRAY = TextColor.named("dark_gray", 0x555555);
    public static final TextColor BLUE = TextColor.named("blue", 0x5555FF);
    public static final TextColor GREEN = TextColor.named("green", 0x55FF55);
    public static final TextColor AQUA = TextColor.named("aqua", 0x55FFFF);
    public static final TextColor RED = TextColor.named("red", 0xFF5555);
    public static final TextColor LIGHT_PURPLE = TextColor.named("light_purple", 0xFF55FF);
    public static final TextColor YELLOW = TextColor.named("yellow", 0xFFFF55);
    public static final TextColor WHITE = TextColor.named("white", 0xFFFFFF);
    private final int value;
    private final @Nullable String name;

    private TextColor(int value, String name) {
        this.value = value & 0xFFFFFF;
        this.name = name;
    }

    private TextColor(int value) {
        this.value = value & 0xFFFFFF;
        this.name = null;
    }

    private static TextColor named(String name, int rgb) {
        TextColor result = new TextColor(rgb, name);
        NAMED_COLORS.put(name, result);
        return result;
    }

    public int getValue() {
        return this.value;
    }

    public String serialize() {
        return this.name != null ? this.name : this.formatValue();
    }

    private String formatValue() {
        return String.format(Locale.ROOT, "#%06X", this.value);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        TextColor other = (TextColor)o;
        return this.value == other.value;
    }

    public int hashCode() {
        return Objects.hash(this.value, this.name);
    }

    public String toString() {
        return this.serialize();
    }

    public static @Nullable TextColor fromLegacyFormat(ChatFormatting format) {
        return switch (format) {
            case ChatFormatting.BLACK -> BLACK;
            case ChatFormatting.DARK_BLUE -> DARK_BLUE;
            case ChatFormatting.DARK_GREEN -> DARK_GREEN;
            case ChatFormatting.DARK_AQUA -> DARK_AQUA;
            case ChatFormatting.DARK_RED -> DARK_RED;
            case ChatFormatting.DARK_PURPLE -> DARK_PURPLE;
            case ChatFormatting.GOLD -> GOLD;
            case ChatFormatting.GRAY -> GRAY;
            case ChatFormatting.DARK_GRAY -> DARK_GRAY;
            case ChatFormatting.BLUE -> BLUE;
            case ChatFormatting.GREEN -> GREEN;
            case ChatFormatting.AQUA -> AQUA;
            case ChatFormatting.RED -> RED;
            case ChatFormatting.LIGHT_PURPLE -> LIGHT_PURPLE;
            case ChatFormatting.YELLOW -> YELLOW;
            case ChatFormatting.WHITE -> WHITE;
            default -> null;
        };
    }

    public static TextColor fromRgb(int rgb) {
        return new TextColor(rgb);
    }

    public static DataResult<TextColor> parseColor(String color) {
        if (color.startsWith(CUSTOM_COLOR_PREFIX)) {
            try {
                int value = Integer.parseInt(color.substring(1), 16);
                if (value < 0 || value > 0xFFFFFF) {
                    return DataResult.error(() -> "Color value out of range: " + color);
                }
                return DataResult.success((Object)TextColor.fromRgb(value), (Lifecycle)Lifecycle.stable());
            }
            catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid color value: " + color);
            }
        }
        TextColor predefinedColor = NAMED_COLORS.get(color);
        if (predefinedColor == null) {
            return DataResult.error(() -> "Invalid color name: " + color);
        }
        return DataResult.success((Object)predefinedColor, (Lifecycle)Lifecycle.stable());
    }
}

