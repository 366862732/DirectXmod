/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public record PlayerChatMessage(SignedMessageLink link, @Nullable MessageSignature signature, SignedMessageBody signedBody, @Nullable Component unsignedContent, FilterMask filterMask) {
    public static final MapCodec<PlayerChatMessage> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)SignedMessageLink.CODEC.fieldOf("link").forGetter(PlayerChatMessage::link), (App)MessageSignature.CODEC.optionalFieldOf("signature").forGetter(playerChatMessage -> Optional.ofNullable(playerChatMessage.signature)), (App)SignedMessageBody.MAP_CODEC.forGetter(PlayerChatMessage::signedBody), (App)ComponentSerialization.CODEC.optionalFieldOf("unsigned_content").forGetter(playerChatMessage -> Optional.ofNullable(playerChatMessage.unsignedContent)), (App)FilterMask.CODEC.optionalFieldOf("filter_mask", (Object)FilterMask.PASS_THROUGH).forGetter(PlayerChatMessage::filterMask)).apply((Applicative)i, (link, signature, signedBody, unsignedContent, filterMask) -> new PlayerChatMessage((SignedMessageLink)link, signature.orElse(null), (SignedMessageBody)signedBody, unsignedContent.orElse(null), (FilterMask)filterMask)));
    private static final UUID SYSTEM_SENDER = Util.NIL_UUID;
    public static final Duration MESSAGE_EXPIRES_AFTER_SERVER = Duration.ofMinutes(5L);
    public static final Duration MESSAGE_EXPIRES_AFTER_CLIENT = MESSAGE_EXPIRES_AFTER_SERVER.plus(Duration.ofMinutes(2L));

    public static PlayerChatMessage system(String content) {
        return PlayerChatMessage.unsigned(SYSTEM_SENDER, content);
    }

    public static PlayerChatMessage unsigned(UUID sender, String content) {
        SignedMessageBody body = SignedMessageBody.unsigned(content);
        SignedMessageLink link = SignedMessageLink.unsigned(sender);
        return new PlayerChatMessage(link, null, body, null, FilterMask.PASS_THROUGH);
    }

    public PlayerChatMessage withUnsignedContent(Component content) {
        Component unsignedContent = !content.equals(Component.literal(this.signedContent())) ? content : null;
        return new PlayerChatMessage(this.link, this.signature, this.signedBody, unsignedContent, this.filterMask);
    }

    public PlayerChatMessage removeUnsignedContent() {
        if (this.unsignedContent != null) {
            return new PlayerChatMessage(this.link, this.signature, this.signedBody, null, this.filterMask);
        }
        return this;
    }

    public PlayerChatMessage filter(FilterMask filterMask) {
        if (this.filterMask.equals(filterMask)) {
            return this;
        }
        return new PlayerChatMessage(this.link, this.signature, this.signedBody, this.unsignedContent, filterMask);
    }

    public PlayerChatMessage filter(boolean filtered) {
        return this.filter(filtered ? this.filterMask : FilterMask.PASS_THROUGH);
    }

    public PlayerChatMessage removeSignature() {
        SignedMessageBody body = SignedMessageBody.unsigned(this.signedContent());
        SignedMessageLink link = SignedMessageLink.unsigned(this.sender());
        return new PlayerChatMessage(link, null, body, this.unsignedContent, this.filterMask);
    }

    public static void updateSignature(SignatureUpdater.Output output, SignedMessageLink link, SignedMessageBody body) throws SignatureException {
        output.update(Ints.toByteArray((int)1));
        link.updateSignature(output);
        body.updateSignature(output);
    }

    public boolean verify(SignatureValidator signatureValidator) {
        return this.signature != null && this.signature.verify(signatureValidator, output -> PlayerChatMessage.updateSignature(output, this.link, this.signedBody));
    }

    public String signedContent() {
        return this.signedBody.content();
    }

    public Component decoratedContent() {
        return Objects.requireNonNullElseGet(this.unsignedContent, () -> Component.literal(this.signedContent()));
    }

    public Instant timeStamp() {
        return this.signedBody.timeStamp();
    }

    public long salt() {
        return this.signedBody.salt();
    }

    public boolean hasExpiredServer(Instant now) {
        return now.isAfter(this.timeStamp().plus(MESSAGE_EXPIRES_AFTER_SERVER));
    }

    public boolean hasExpiredClient(Instant now) {
        return now.isAfter(this.timeStamp().plus(MESSAGE_EXPIRES_AFTER_CLIENT));
    }

    public UUID sender() {
        return this.link.sender();
    }

    public boolean isSystem() {
        return this.sender().equals(SYSTEM_SENDER);
    }

    public boolean hasSignature() {
        return this.signature != null;
    }

    public boolean hasSignatureFrom(UUID profileId) {
        return this.hasSignature() && this.link.sender().equals(profileId);
    }

    public boolean isFullyFiltered() {
        return this.filterMask.isFullyFiltered();
    }

    public static String describeSigned(PlayerChatMessage message) {
        return "'" + message.signedBody.content() + "' @ " + String.valueOf(message.signedBody.timeStamp()) + "\n - From: " + String.valueOf(message.link.sender()) + "/" + String.valueOf(message.link.sessionId()) + ", message #" + message.link.index() + "\n - Salt: " + message.signedBody.salt() + "\n - Signature: " + MessageSignature.describe(message.signature) + "\n - Last Seen: [\n" + message.signedBody.lastSeen().entries().stream().map(signature -> "     " + MessageSignature.describe(signature) + "\n").collect(Collectors.joining()) + " ]\n";
    }
}

