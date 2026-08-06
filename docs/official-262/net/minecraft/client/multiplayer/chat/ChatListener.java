/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer.chat;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.StringDecomposer;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChatListener {
    private static final Component CHAT_VALIDATION_ERROR = Component.translatable("chat.validation_error").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
    private final Minecraft minecraft;
    private final Deque<Message> delayedMessageQueue = Queues.newArrayDeque();
    private long messageDelay;
    private long previousMessageTime;

    public ChatListener(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void tick() {
        if (this.minecraft.isPaused()) {
            if (this.messageDelay > 0L) {
                this.previousMessageTime += 50L;
            }
            return;
        }
        if (this.messageDelay == 0L) {
            if (!this.delayedMessageQueue.isEmpty()) {
                this.flushQueue();
            }
        } else if (Util.getMillis() >= this.previousMessageTime + this.messageDelay) {
            Message message;
            while ((message = this.delayedMessageQueue.poll()) != null && !message.accept()) {
            }
        }
    }

    public void setMessageDelay(double messageDelaySeconds) {
        long messageDelay = (long)(messageDelaySeconds * 1000.0);
        if (messageDelay == 0L && this.messageDelay > 0L && !this.minecraft.isPaused()) {
            this.flushQueue();
        }
        this.messageDelay = messageDelay;
    }

    public void acceptNextDelayedMessage() {
        this.delayedMessageQueue.remove().accept();
    }

    public long queueSize() {
        return this.delayedMessageQueue.size();
    }

    public void flushQueue() {
        this.delayedMessageQueue.forEach(Message::accept);
        this.delayedMessageQueue.clear();
        this.previousMessageTime = 0L;
    }

    public boolean removeFromDelayedMessageQueue(MessageSignature signature) {
        return this.delayedMessageQueue.removeIf(message -> signature.equals(message.signature()));
    }

    private boolean willDelayMessages() {
        return this.messageDelay > 0L && Util.getMillis() < this.previousMessageTime + this.messageDelay;
    }

    private void handleMessage(@Nullable MessageSignature signature, BooleanSupplier handler) {
        if (this.willDelayMessages()) {
            this.delayedMessageQueue.add(new Message(signature, handler));
        } else {
            handler.getAsBoolean();
        }
    }

    public void handlePlayerChatMessage(PlayerChatMessage message, GameProfile sender, ChatType.Bound boundChatType) {
        boolean onlyShowSecure = this.minecraft.options.onlyShowSecureChat().get();
        PlayerChatMessage displayedMessage = onlyShowSecure ? message.removeUnsignedContent() : message;
        Component decoratedMessage = boundChatType.decorate(displayedMessage.decoratedContent());
        Instant received = Instant.now();
        this.handleMessage(message.signature(), () -> {
            boolean wasShown = this.showMessageToPlayer(boundChatType, message, decoratedMessage, sender, onlyShowSecure, received);
            ClientPacketListener connection = this.minecraft.getConnection();
            if (connection != null && message.signature() != null) {
                connection.markMessageAsProcessed(message.signature(), wasShown);
            }
            return wasShown;
        });
    }

    public void handleChatMessageError(UUID senderId, @Nullable MessageSignature invalidSignature, ChatType.Bound boundChatType) {
        this.handleMessage(null, () -> {
            ClientPacketListener connection = this.minecraft.getConnection();
            if (connection != null && invalidSignature != null) {
                connection.markMessageAsProcessed(invalidSignature, false);
            }
            if (this.minecraft.isBlocked(senderId)) {
                return false;
            }
            if (this.minecraft.isFriendOnlyRestricted(senderId)) {
                return false;
            }
            LocalPlayer receiver = this.minecraft.player;
            if (receiver == null || !receiver.chatAbilities().canReceivePlayerMessages()) {
                return false;
            }
            Component decoratedMessage = boundChatType.decorate(CHAT_VALIDATION_ERROR);
            this.minecraft.gui.hud.getChat().addPlayerMessage(decoratedMessage, null, GuiMessageTag.chatError());
            this.minecraft.getNarrator().saySystemChatQueued(boundChatType.decorateNarration(CHAT_VALIDATION_ERROR));
            this.previousMessageTime = Util.getMillis();
            return true;
        });
    }

    public void handleDisguisedChatMessage(Component message, ChatType.Bound boundChatType) {
        Instant received = Instant.now();
        this.handleMessage(null, () -> {
            LocalPlayer receiver = this.minecraft.player;
            if (receiver == null || !receiver.chatAbilities().canReceivePlayerMessages()) {
                return false;
            }
            Component decoratedMessage = boundChatType.decorate(message);
            this.minecraft.gui.hud.getChat().addPlayerMessage(decoratedMessage, null, GuiMessageTag.system());
            this.narrateChatMessage(boundChatType, message);
            this.logSystemMessage(decoratedMessage, received);
            this.previousMessageTime = Util.getMillis();
            return true;
        });
    }

    private boolean showMessageToPlayer(ChatType.Bound boundChatType, PlayerChatMessage message, Component decoratedMessage, GameProfile sender, boolean onlyShowSecure, Instant received) {
        ChatTrustLevel trustLevel = this.evaluateTrustLevel(message, decoratedMessage, received);
        if (onlyShowSecure && trustLevel.isNotSecure()) {
            return false;
        }
        if (this.minecraft.isBlocked(message.sender()) || message.isFullyFiltered()) {
            return false;
        }
        if (this.minecraft.isFriendOnlyRestricted(message.sender())) {
            return false;
        }
        LocalPlayer receiver = this.minecraft.player;
        if (receiver == null || !receiver.chatAbilities().canReceivePlayerMessages()) {
            return false;
        }
        GuiMessageTag tag = trustLevel.createTag(message);
        MessageSignature signature = message.signature();
        FilterMask filterMask = message.filterMask();
        if (filterMask.isEmpty()) {
            this.minecraft.gui.hud.getChat().addPlayerMessage(decoratedMessage, signature, tag);
            this.narrateChatMessage(boundChatType, message.decoratedContent());
        } else {
            Component filteredContent = filterMask.applyWithFormatting(message.signedContent());
            if (filteredContent != null) {
                this.minecraft.gui.hud.getChat().addPlayerMessage(boundChatType.decorate(filteredContent), signature, tag);
                this.narrateChatMessage(boundChatType, filteredContent);
            }
        }
        this.logPlayerMessage(message, sender, trustLevel);
        this.previousMessageTime = Util.getMillis();
        return true;
    }

    private void narrateChatMessage(ChatType.Bound boundChatType, Component content) {
        this.minecraft.getNarrator().sayChatQueued(boundChatType.decorateNarration(content));
    }

    private ChatTrustLevel evaluateTrustLevel(PlayerChatMessage message, Component decoratedMessage, Instant received) {
        if (this.isSenderLocalPlayer(message.sender())) {
            return ChatTrustLevel.SECURE;
        }
        return ChatTrustLevel.evaluate(message, decoratedMessage, received);
    }

    private void logPlayerMessage(PlayerChatMessage message, GameProfile sender, ChatTrustLevel trustLevel) {
        ChatLog chatLog = this.minecraft.getReportingContext().chatLog();
        chatLog.push(LoggedChatMessage.player(sender, message, trustLevel));
    }

    private void logSystemMessage(Component message, Instant timeStamp) {
        ChatLog chatLog = this.minecraft.getReportingContext().chatLog();
        chatLog.push(LoggedChatMessage.system(message, timeStamp));
    }

    public void handleSystemMessage(Component message, boolean remote) {
        UUID guessedUUID = this.guessChatUUID(message);
        if (this.minecraft.options.hideMatchedNames().get().booleanValue() && this.minecraft.isBlocked(guessedUUID)) {
            return;
        }
        if (guessedUUID != Util.NIL_UUID && this.minecraft.isFriendOnlyRestricted(guessedUUID)) {
            return;
        }
        LocalPlayer receiver = this.minecraft.player;
        if (receiver == null || !receiver.chatAbilities().canReceiveSystemMessages()) {
            return;
        }
        if (remote) {
            this.minecraft.gui.hud.getChat().addServerSystemMessage(message);
            this.logSystemMessage(message, Instant.now());
        } else {
            this.minecraft.gui.hud.getChat().addClientSystemMessage(message);
        }
        this.minecraft.getNarrator().saySystemChatQueued(message);
    }

    public void handleOverlay(Component message) {
        this.minecraft.gui.hud.setOverlayMessage(message, false);
        this.minecraft.getNarrator().saySystemQueued(message);
    }

    private UUID guessChatUUID(Component message) {
        String noFormatMessage = StringDecomposer.getPlainText(message);
        String possibleMention = StringUtils.substringBetween((String)noFormatMessage, (String)"<", (String)">");
        if (possibleMention == null) {
            return Util.NIL_UUID;
        }
        return this.minecraft.getPlayerSocialManager().getDiscoveredUUID(possibleMention);
    }

    private boolean isSenderLocalPlayer(UUID senderProfileId) {
        if (this.minecraft.isLocalServer() && this.minecraft.player != null) {
            UUID localProfileId = this.minecraft.player.getGameProfile().id();
            return localProfileId.equals(senderProfileId);
        }
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    private record Message(@Nullable MessageSignature signature, BooleanSupplier handler) {
        public boolean accept() {
            return this.handler.getAsBoolean();
        }
    }
}

