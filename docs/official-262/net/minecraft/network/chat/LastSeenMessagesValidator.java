/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.List;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.LastSeenTrackedEntry;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;

public class LastSeenMessagesValidator {
    private final int lastSeenCount;
    private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList();
    private @Nullable MessageSignature lastPendingMessage;

    public LastSeenMessagesValidator(int lastSeenCount) {
        this.lastSeenCount = lastSeenCount;
        for (int i = 0; i < lastSeenCount; ++i) {
            this.trackedMessages.add(null);
        }
    }

    public void addPending(MessageSignature message) {
        if (!message.equals(this.lastPendingMessage)) {
            this.trackedMessages.add((Object)new LastSeenTrackedEntry(message, true));
            this.lastPendingMessage = message;
        }
    }

    public int trackedMessagesCount() {
        return this.trackedMessages.size();
    }

    public void applyOffset(int offset) throws ValidationException {
        int maxOffset = this.trackedMessages.size() - this.lastSeenCount;
        if (offset < 0 || offset > maxOffset) {
            throw new ValidationException("Advanced last seen window by " + offset + " messages, but expected at most " + maxOffset);
        }
        this.trackedMessages.removeElements(0, offset);
    }

    public LastSeenMessages applyUpdate(LastSeenMessages.Update update) throws ValidationException {
        this.applyOffset(update.offset());
        ObjectArrayList lastSeenEntries = new ObjectArrayList(update.acknowledged().cardinality());
        if (update.acknowledged().length() > this.lastSeenCount) {
            throw new ValidationException("Last seen update contained " + update.acknowledged().length() + " messages, but maximum window size is " + this.lastSeenCount);
        }
        for (int i = 0; i < this.lastSeenCount; ++i) {
            boolean acknowledged = update.acknowledged().get(i);
            LastSeenTrackedEntry message = (LastSeenTrackedEntry)this.trackedMessages.get(i);
            if (acknowledged) {
                if (message == null) {
                    throw new ValidationException("Last seen update acknowledged unknown or previously ignored message at index " + i);
                }
                this.trackedMessages.set(i, (Object)message.acknowledge());
                lastSeenEntries.add((Object)message.signature());
                continue;
            }
            if (message != null && !message.pending()) {
                throw new ValidationException("Last seen update ignored previously acknowledged message at index " + i + " and signature " + String.valueOf(message.signature()));
            }
            this.trackedMessages.set(i, null);
        }
        LastSeenMessages lastSeen = new LastSeenMessages((List<MessageSignature>)lastSeenEntries);
        if (!update.verifyChecksum(lastSeen)) {
            throw new ValidationException("Checksum mismatch on last seen update: the client and server must have desynced");
        }
        return lastSeen;
    }

    public static class ValidationException
    extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}

