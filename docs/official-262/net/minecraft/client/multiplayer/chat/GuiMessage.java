/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer.chat;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag tag) {
    private static final int MESSAGE_TAG_MARGIN_LEFT = 4;

    public List<FormattedCharSequence> splitLines(Font font, int maxWidth) {
        if (this.tag != null && this.tag.icon() != null) {
            maxWidth -= this.tag.icon().width + 4 + 2;
        }
        return ComponentRenderUtils.wrapComponents(this.content, maxWidth, font);
    }

    @Environment(value=EnvType.CLIENT)
    public record Line(GuiMessage parent, FormattedCharSequence content, boolean endOfEntry) {
        public int getTagIconLeft(Font font) {
            return font.width(this.content) + 4;
        }

        public @Nullable GuiMessageTag tag() {
            return this.parent.tag;
        }

        public int addedTime() {
            return this.parent.addedTime;
        }
    }
}

