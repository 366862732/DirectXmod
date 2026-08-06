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
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.SignatureValidator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@FunctionalInterface
public interface SignedMessageValidator {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final SignedMessageValidator ACCEPT_UNSIGNED = PlayerChatMessage::removeSignature;
    public static final SignedMessageValidator REJECT_ALL = message -> {
        LOGGER.error("Received chat message from {}, but they have no chat session initialized and secure chat is enforced", (Object)message.sender());
        return null;
    };

    public @Nullable PlayerChatMessage updateAndValidate(PlayerChatMessage var1);

    public static class KeyBased
    implements SignedMessageValidator {
        private final SignatureValidator validator;
        private final BooleanSupplier expired;
        private @Nullable PlayerChatMessage lastMessage;
        private boolean isChainValid = true;

        public KeyBased(SignatureValidator validator, BooleanSupplier expired) {
            this.validator = validator;
            this.expired = expired;
        }

        private boolean validateChain(PlayerChatMessage message) {
            if (message.equals(this.lastMessage)) {
                return true;
            }
            if (this.lastMessage != null && !message.link().isDescendantOf(this.lastMessage.link())) {
                LOGGER.error("Received out-of-order chat message from {}: expected index > {} for session {}, but was {} for session {}", new Object[]{message.sender(), this.lastMessage.link().index(), this.lastMessage.link().sessionId(), message.link().index(), message.link().sessionId()});
                return false;
            }
            return true;
        }

        private boolean validate(PlayerChatMessage message) {
            if (this.expired.getAsBoolean()) {
                LOGGER.error("Received message with expired profile public key from {} with session {}", (Object)message.sender(), (Object)message.link().sessionId());
                return false;
            }
            if (!message.verify(this.validator)) {
                LOGGER.error("Received message with invalid signature (is the session wrong, or signature cache out of sync?): {}", (Object)PlayerChatMessage.describeSigned(message));
                return false;
            }
            return this.validateChain(message);
        }

        @Override
        public @Nullable PlayerChatMessage updateAndValidate(PlayerChatMessage message) {
            boolean bl = this.isChainValid = this.isChainValid && this.validate(message);
            if (!this.isChainValid) {
                return null;
            }
            this.lastMessage = message;
            return message;
        }
    }
}

