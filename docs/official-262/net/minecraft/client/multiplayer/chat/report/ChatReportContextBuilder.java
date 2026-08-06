/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntCollection
 *  it.unimi.dsi.fastutil.ints.IntRBTreeSet
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.chat.report;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;

@Environment(value=EnvType.CLIENT)
public class ChatReportContextBuilder {
    private final int leadingCount;
    private final List<Collector> activeCollectors = new ArrayList<Collector>();

    public ChatReportContextBuilder(int leadingCount) {
        this.leadingCount = leadingCount;
    }

    public void collectAllContext(ChatLog chatLog, IntCollection roots, Handler handler) {
        IntRBTreeSet uncollectedRoots = new IntRBTreeSet(roots);
        for (int id = uncollectedRoots.lastInt(); id >= chatLog.start() && (this.isActive() || !uncollectedRoots.isEmpty()); --id) {
            LoggedChatEvent loggedChatEvent = chatLog.lookup(id);
            if (!(loggedChatEvent instanceof LoggedChatMessage.Player)) continue;
            LoggedChatMessage.Player event = (LoggedChatMessage.Player)loggedChatEvent;
            boolean context = this.acceptContext(event.message());
            if (uncollectedRoots.remove(id)) {
                this.trackContext(event.message());
                handler.accept(id, event);
                continue;
            }
            if (!context) continue;
            handler.accept(id, event);
        }
    }

    public void trackContext(PlayerChatMessage message) {
        this.activeCollectors.add(new Collector(this, message));
    }

    public boolean acceptContext(PlayerChatMessage message) {
        boolean collected = false;
        Iterator<Collector> iterator = this.activeCollectors.iterator();
        while (iterator.hasNext()) {
            Collector collector = iterator.next();
            if (!collector.accept(message)) continue;
            collected = true;
            if (!collector.isComplete()) continue;
            iterator.remove();
        }
        return collected;
    }

    public boolean isActive() {
        return !this.activeCollectors.isEmpty();
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Handler {
        public void accept(int var1, LoggedChatMessage.Player var2);
    }

    @Environment(value=EnvType.CLIENT)
    private class Collector {
        private final Set<MessageSignature> lastSeenSignatures;
        private PlayerChatMessage lastChainMessage;
        private boolean collectingChain;
        private int count;
        final /* synthetic */ ChatReportContextBuilder this$0;

        private Collector(ChatReportContextBuilder chatReportContextBuilder, PlayerChatMessage fromMessage) {
            ChatReportContextBuilder chatReportContextBuilder2 = chatReportContextBuilder;
            Objects.requireNonNull(chatReportContextBuilder2);
            this.this$0 = chatReportContextBuilder2;
            this.collectingChain = true;
            this.lastSeenSignatures = new ObjectOpenHashSet(fromMessage.signedBody().lastSeen().entries());
            this.lastChainMessage = fromMessage;
        }

        private boolean accept(PlayerChatMessage message) {
            if (message.equals(this.lastChainMessage)) {
                return false;
            }
            boolean selected = this.lastSeenSignatures.remove(message.signature());
            if (this.collectingChain && this.lastChainMessage.sender().equals(message.sender())) {
                if (this.lastChainMessage.link().isDescendantOf(message.link())) {
                    selected = true;
                    this.lastChainMessage = message;
                } else {
                    this.collectingChain = false;
                }
            }
            if (selected) {
                ++this.count;
            }
            return selected;
        }

        private boolean isComplete() {
            return this.count >= this.this$0.leadingCount || !this.collectingChain && this.lastSeenSignatures.isEmpty();
        }
    }
}

