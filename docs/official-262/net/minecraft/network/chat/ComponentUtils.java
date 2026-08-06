/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.DataFixUtils
 *  javax.annotation.CheckReturnValue
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;

public class ComponentUtils {
    public static final String DEFAULT_SEPARATOR_TEXT = ", ";
    public static final Component DEFAULT_SEPARATOR = Component.literal(", ").withStyle(ChatFormatting.GRAY);
    public static final Component DEFAULT_NO_STYLE_SEPARATOR = Component.literal(", ");

    @CheckReturnValue
    public static MutableComponent mergeStyles(MutableComponent component, Style style) {
        if (style.isEmpty()) {
            return component;
        }
        Style inner = component.getStyle();
        if (inner.isEmpty()) {
            return component.setStyle(style);
        }
        if (inner.equals(style)) {
            return component;
        }
        return component.setStyle(inner.applyTo(style));
    }

    @CheckReturnValue
    public static Component mergeStyles(Component component, Style style) {
        if (style.isEmpty()) {
            return component;
        }
        Style inner = component.getStyle();
        if (inner.isEmpty()) {
            return component.copy().setStyle(style);
        }
        if (inner.equals(style)) {
            return component;
        }
        return component.copy().setStyle(inner.applyTo(style));
    }

    public static Optional<MutableComponent> resolve(ResolutionContext context, Optional<Component> component, int recursionDepth) throws CommandSyntaxException {
        return component.isPresent() ? Optional.of(ComponentUtils.resolve(context, component.get(), recursionDepth)) : Optional.empty();
    }

    public static MutableComponent resolve(ResolutionContext context, Component component) throws CommandSyntaxException {
        return ComponentUtils.resolve(context, component, 0);
    }

    public static MutableComponent resolve(ResolutionContext context, Component component, int recursionDepth) throws CommandSyntaxException {
        if (recursionDepth > context.depthLimit()) {
            return switch (context.depthLimitBehavior()) {
                default -> throw new MatchException(null, null);
                case ResolutionContext.LimitBehavior.DISCARD_REMAINING -> CommonComponents.ELLIPSIS.copy();
                case ResolutionContext.LimitBehavior.STOP_PROCESSING_AND_COPY_REMAINING -> component.copy();
            };
        }
        MutableComponent result = component.getContents().resolve(context, recursionDepth + 1);
        for (Component sibling : component.getSiblings()) {
            result.append(ComponentUtils.resolve(context, sibling, recursionDepth + 1));
        }
        return result.withStyle(ComponentUtils.resolveStyle(context, component.getStyle(), recursionDepth));
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static Style resolveStyle(ResolutionContext context, Style style, int recursionDepth) throws CommandSyntaxException {
        HoverEvent hoverEvent = style.getHoverEvent();
        if (!(hoverEvent instanceof HoverEvent.ShowText)) return style;
        HoverEvent.ShowText showText = (HoverEvent.ShowText)hoverEvent;
        try {
            Component component;
            Component text = component = showText.value();
            HoverEvent.ShowText resolved = new HoverEvent.ShowText(ComponentUtils.resolve(context, text, recursionDepth + 1));
            return style.withHoverEvent(resolved);
        }
        catch (Throwable throwable) {
            throw new MatchException(throwable.toString(), throwable);
        }
    }

    public static Component formatList(Collection<String> values) {
        return ComponentUtils.formatAndSortList(values, v -> Component.literal(v).withStyle(ChatFormatting.GREEN));
    }

    public static <T extends Comparable<T>> Component formatAndSortList(Collection<T> values, Function<T, Component> formatter) {
        if (values.isEmpty()) {
            return CommonComponents.EMPTY;
        }
        if (values.size() == 1) {
            return formatter.apply((Comparable)values.iterator().next());
        }
        ArrayList sorted = Lists.newArrayList(values);
        sorted.sort(Comparable::compareTo);
        return ComponentUtils.formatList(sorted, formatter);
    }

    public static <T> Component formatList(Collection<? extends T> values, Function<T, Component> formatter) {
        return ComponentUtils.formatList(values, DEFAULT_SEPARATOR, formatter);
    }

    public static <T> MutableComponent formatList(Collection<? extends T> values, Optional<? extends Component> separator, Function<T, Component> formatter) {
        return ComponentUtils.formatList(values, (Component)DataFixUtils.orElse(separator, (Object)DEFAULT_SEPARATOR), formatter);
    }

    public static Component formatList(Collection<? extends Component> values, Component separator) {
        return ComponentUtils.formatList(values, separator, Function.identity());
    }

    public static <T> MutableComponent formatList(Collection<? extends T> values, Component separator, Function<T, Component> formatter) {
        if (values.isEmpty()) {
            return Component.empty();
        }
        if (values.size() == 1) {
            return formatter.apply(values.iterator().next()).copy();
        }
        MutableComponent result = Component.empty();
        boolean first = true;
        for (T value : values) {
            if (!first) {
                result.append(separator);
            }
            result.append(formatter.apply(value));
            first = false;
        }
        return result;
    }

    public static MutableComponent wrapInSquareBrackets(Component inner) {
        return Component.translatable("chat.square_brackets", inner);
    }

    public static Component fromMessage(Message message) {
        if (message instanceof Component) {
            Component component = (Component)message;
            return component;
        }
        return Component.literal(message.getString());
    }

    public static boolean isTranslationResolvable(@Nullable Component component) {
        ComponentContents componentContents;
        if (component != null && (componentContents = component.getContents()) instanceof TranslatableContents) {
            TranslatableContents translatable = (TranslatableContents)componentContents;
            String key = translatable.getKey();
            String fallback = translatable.getFallback();
            return fallback != null || Language.getInstance().has(key);
        }
        return true;
    }

    public static MutableComponent copyOnClickText(String text) {
        return ComponentUtils.wrapInSquareBrackets(Component.literal(text).withStyle(s -> s.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.CopyToClipboard(text)).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))).withInsertion(text)));
    }
}

