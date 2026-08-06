/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

public final class MutableComponent
implements Component {
    private final ComponentContents contents;
    private final List<Component> siblings;
    private Style style;
    private FormattedCharSequence visualOrderText = FormattedCharSequence.EMPTY;
    private @Nullable Language decomposedWith;

    MutableComponent(ComponentContents contents, List<Component> siblings, Style style) {
        this.contents = contents;
        this.siblings = siblings;
        this.style = style;
    }

    public static MutableComponent create(ComponentContents contents) {
        return new MutableComponent(contents, Lists.newArrayList(), Style.EMPTY);
    }

    @Override
    public ComponentContents getContents() {
        return this.contents;
    }

    @Override
    public List<Component> getSiblings() {
        return this.siblings;
    }

    public MutableComponent setStyle(Style style) {
        this.style = style;
        return this;
    }

    @Override
    public Style getStyle() {
        return this.style;
    }

    public MutableComponent append(String text) {
        if (text.isEmpty()) {
            return this;
        }
        return this.append(Component.literal(text));
    }

    public MutableComponent append(Component component) {
        this.siblings.add(component);
        return this;
    }

    public MutableComponent withStyle(UnaryOperator<Style> updater) {
        this.setStyle((Style)updater.apply(this.getStyle()));
        return this;
    }

    public MutableComponent withStyle(Style patch) {
        this.setStyle(patch.applyTo(this.getStyle()));
        return this;
    }

    public MutableComponent withStyle(ChatFormatting ... formats) {
        this.setStyle(this.getStyle().applyFormats(formats));
        return this;
    }

    public MutableComponent withStyle(ChatFormatting format) {
        this.setStyle(this.getStyle().applyFormat(format));
        return this;
    }

    public MutableComponent withColor(int color) {
        this.setStyle(this.getStyle().withColor(color));
        return this;
    }

    public MutableComponent withColor(TextColor color) {
        this.setStyle(this.getStyle().withColor(color));
        return this;
    }

    public MutableComponent withoutShadow() {
        this.setStyle(this.getStyle().withoutShadow());
        return this;
    }

    @Override
    public FormattedCharSequence getVisualOrderText() {
        Language currentLanguage = Language.getInstance();
        if (this.decomposedWith != currentLanguage) {
            this.visualOrderText = currentLanguage.getVisualOrder(this);
            this.decomposedWith = currentLanguage;
        }
        return this.visualOrderText;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MutableComponent)) return false;
        MutableComponent that = (MutableComponent)o;
        if (!this.contents.equals(that.contents)) return false;
        if (!this.style.equals(that.style)) return false;
        if (!this.siblings.equals(that.siblings)) return false;
        return true;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + this.contents.hashCode();
        result = 31 * result + this.style.hashCode();
        result = 31 * result + this.siblings.hashCode();
        return result;
    }

    public String toString() {
        boolean hasSiblings;
        StringBuilder result = new StringBuilder(this.contents.toString());
        boolean hasStyle = !this.style.isEmpty();
        boolean bl = hasSiblings = !this.siblings.isEmpty();
        if (hasStyle || hasSiblings) {
            result.append('[');
            if (hasStyle) {
                result.append("style=");
                result.append(this.style);
            }
            if (hasStyle && hasSiblings) {
                result.append(", ");
            }
            if (hasSiblings) {
                result.append("siblings=");
                result.append(this.siblings);
            }
            result.append(']');
        }
        return result.toString();
    }
}

