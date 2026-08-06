/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import org.jspecify.annotations.Nullable;

public class MessageSignatureCache {
    public static final int NOT_FOUND = -1;
    private static final int DEFAULT_CAPACITY = 128;
    private final @Nullable MessageSignature[] entries;

    public MessageSignatureCache(int capacity) {
        this.entries = new MessageSignature[capacity];
    }

    public static MessageSignatureCache createDefault() {
        return new MessageSignatureCache(128);
    }

    public int pack(MessageSignature signature) {
        for (int i = 0; i < this.entries.length; ++i) {
            if (!signature.equals(this.entries[i])) continue;
            return i;
        }
        return -1;
    }

    public @Nullable MessageSignature unpack(int id) {
        return this.entries[id];
    }

    public void push(SignedMessageBody body, @Nullable MessageSignature signature) {
        List<MessageSignature> lastSeen = body.lastSeen().entries();
        ArrayDeque<MessageSignature> queue = new ArrayDeque<MessageSignature>(lastSeen.size() + 1);
        queue.addAll(lastSeen);
        if (signature != null) {
            queue.add(signature);
        }
        this.push(queue);
    }

    @VisibleForTesting
    void push(List<MessageSignature> entries) {
        this.push(new ArrayDeque<MessageSignature>(entries));
    }

    private void push(ArrayDeque<MessageSignature> queue) {
        ObjectOpenHashSet newEntries = new ObjectOpenHashSet(queue);
        for (int i = 0; !queue.isEmpty() && i < this.entries.length; ++i) {
            MessageSignature entry = this.entries[i];
            this.entries[i] = queue.removeLast();
            if (entry == null || newEntries.contains(entry)) continue;
            queue.addFirst(entry);
        }
    }
}

