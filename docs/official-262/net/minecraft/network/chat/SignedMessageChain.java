/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SignedMessageChain {
    private static final Logger LOGGER = LogUtils.getLogger();
    private @Nullable SignedMessageLink nextLink;
    private Instant lastTimeStamp = Instant.EPOCH;

    public SignedMessageChain(UUID profileId, UUID sessionId) {
        this.nextLink = SignedMessageLink.root(profileId, sessionId);
    }

    public Encoder encoder(Signer signer) {
        return body -> {
            SignedMessageLink link = this.nextLink;
            if (link == null) {
                return null;
            }
            this.nextLink = link.advance();
            return new MessageSignature(signer.sign(output -> PlayerChatMessage.updateSignature(output, link, body)));
        };
    }

    public Decoder decoder(final ProfilePublicKey profilePublicKey) {
        final SignatureValidator signatureValidator = profilePublicKey.createSignatureValidator();
        return new Decoder(){
            final /* synthetic */ SignedMessageChain this$0;
            {
                SignedMessageChain signedMessageChain = this$0;
                Objects.requireNonNull(signedMessageChain);
                this.this$0 = signedMessageChain;
            }

            @Override
            public PlayerChatMessage unpack(@Nullable MessageSignature signature, SignedMessageBody body) throws DecodeException {
                if (signature == null) {
                    throw new DecodeException(DecodeException.MISSING_PROFILE_KEY);
                }
                if (profilePublicKey.data().hasExpired()) {
                    throw new DecodeException(DecodeException.EXPIRED_PROFILE_KEY);
                }
                SignedMessageLink link = this.this$0.nextLink;
                if (link == null) {
                    throw new DecodeException(DecodeException.CHAIN_BROKEN);
                }
                if (body.timeStamp().isBefore(this.this$0.lastTimeStamp)) {
                    this.setChainBroken();
                    throw new DecodeException(DecodeException.OUT_OF_ORDER_CHAT);
                }
                this.this$0.lastTimeStamp = body.timeStamp();
                PlayerChatMessage unpacked = new PlayerChatMessage(link, signature, body, null, FilterMask.PASS_THROUGH);
                if (!unpacked.verify(signatureValidator)) {
                    this.setChainBroken();
                    throw new DecodeException(DecodeException.INVALID_SIGNATURE);
                }
                if (unpacked.hasExpiredServer(Instant.now())) {
                    LOGGER.warn("Received expired chat: '{}'. Is the client/server system time unsynchronized?", (Object)body.content());
                }
                this.this$0.nextLink = link.advance();
                return unpacked;
            }

            @Override
            public void setChainBroken() {
                this.this$0.nextLink = null;
            }
        };
    }

    @FunctionalInterface
    public static interface Encoder {
        public static final Encoder UNSIGNED = body -> null;

        public @Nullable MessageSignature pack(SignedMessageBody var1);
    }

    public static class DecodeException
    extends ThrowingComponent {
        private static final Component MISSING_PROFILE_KEY = Component.translatable("chat.disabled.missingProfileKey");
        private static final Component CHAIN_BROKEN = Component.translatable("chat.disabled.chain_broken");
        private static final Component EXPIRED_PROFILE_KEY = Component.translatable("chat.disabled.expiredProfileKey");
        private static final Component INVALID_SIGNATURE = Component.translatable("chat.disabled.invalid_signature");
        private static final Component OUT_OF_ORDER_CHAT = Component.translatable("chat.disabled.out_of_order_chat");

        public DecodeException(Component component) {
            super(component);
        }
    }

    @FunctionalInterface
    public static interface Decoder {
        public static Decoder unsigned(UUID profileId, BooleanSupplier enforcesSecureChat) {
            return (signature, body) -> {
                if (enforcesSecureChat.getAsBoolean()) {
                    throw new DecodeException(DecodeException.MISSING_PROFILE_KEY);
                }
                return PlayerChatMessage.unsigned(profileId, body.content());
            };
        }

        public PlayerChatMessage unpack(@Nullable MessageSignature var1, SignedMessageBody var2) throws DecodeException;

        default public void setChainBroken() {
        }
    }
}

