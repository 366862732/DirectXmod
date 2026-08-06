/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

@Environment(value=EnvType.CLIENT)
public class ComponentRenderUtils {
    private static final FormattedCharSequence INDENT = FormattedCharSequence.codepoint(32, Style.EMPTY);

    private static String stripColor(String input) {
        return Minecraft.getInstance().options.chatColors().get() != false ? input : ChatFormatting.stripFormatting(input);
    }

    public static List<FormattedCharSequence> wrapComponents(FormattedText message, int maxWidth, Font font) {
        ComponentCollector collector = new ComponentCollector();
        message.visit((style, contents) -> {
            collector.append(FormattedText.of(ComponentRenderUtils.stripColor(contents), style));
            return Optional.empty();
        }, Style.EMPTY);
        ArrayList result = Lists.newArrayList();
        font.getSplitter().splitLines(collector.getResultOrEmpty(), maxWidth, Style.EMPTY, (text, wrapped) -> {
            FormattedCharSequence reorderedText = Language.getInstance().getVisualOrder((FormattedText)text);
            result.add(wrapped != false ? FormattedCharSequence.composite(INDENT, reorderedText) : reorderedText);
        });
        if (result.isEmpty()) {
            return Lists.newArrayList((Object[])new FormattedCharSequence[]{FormattedCharSequence.EMPTY});
        }
        return result;
    }

    public static FormattedCharSequence clipText(Component text, Font font, int width) {
        FormattedText clippedText = font.substrByWidth(text, width - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(clippedText, CommonComponents.ELLIPSIS));
    }
}

