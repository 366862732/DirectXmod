/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.time.Instant;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public enum ChatTrustLevel implements StringRepresentable
{
    SECURE("secure"),
    MODIFIED("modified"),
    NOT_SECURE("not_secure");

    public static final Codec<ChatTrustLevel> CODEC;
    private final String serializedName;

    private ChatTrustLevel(String serializedName) {
        this.serializedName = serializedName;
    }

    public static ChatTrustLevel evaluate(PlayerChatMessage message, Component decoratedMessage, Instant received) {
        if (!message.hasSignature() || message.hasExpiredClient(received)) {
            return NOT_SECURE;
        }
        if (ChatTrustLevel.isModified(message, decoratedMessage)) {
            return MODIFIED;
        }
        return SECURE;
    }

    private static boolean isModified(PlayerChatMessage message, Component decoratedMessage) {
        if (!decoratedMessage.getString().contains(message.signedContent())) {
            return true;
        }
        Component decoratedContent = message.unsignedContent();
        if (decoratedContent == null) {
            return false;
        }
        return ChatTrustLevel.containsModifiedStyle(decoratedContent);
    }

    private static boolean containsModifiedStyle(Component decoratedContent) {
        return decoratedContent.visit((style, contents) -> {
            if (ChatTrustLevel.isModifiedStyle(style)) {
                return Optional.of(true);
            }
            return Optional.empty();
        }, Style.EMPTY).orElse(false);
    }

    private static boolean isModifiedStyle(Style style) {
        return !style.getFont().equals(FontDescription.DEFAULT);
    }

    public boolean isNotSecure() {
        return this == NOT_SECURE;
    }

    public @Nullable GuiMessageTag createTag(PlayerChatMessage message) {
        return switch (this.ordinal()) {
            case 1 -> GuiMessageTag.chatModified(message.signedContent());
            case 2 -> GuiMessageTag.chatNotSecure();
            default -> null;
        };
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    static {
        CODEC = StringRepresentable.fromEnum(ChatTrustLevel::values);
    }
}

