/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.reporting;

import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportContextBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageLink;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChatSelectionLogFiller {
    private final ChatLog log;
    private final ChatReportContextBuilder contextBuilder;
    private final Predicate<LoggedChatMessage.Player> canReport;
    private @Nullable SignedMessageLink previousLink = null;
    private int eventId;
    private int missedCount;
    private @Nullable PlayerChatMessage lastMessage;

    public ChatSelectionLogFiller(ReportingContext reportingContext, Predicate<LoggedChatMessage.Player> canReport) {
        this.log = reportingContext.chatLog();
        this.contextBuilder = new ChatReportContextBuilder(reportingContext.sender().reportLimits().leadingContextMessageCount());
        this.canReport = canReport;
        this.eventId = this.log.end();
    }

    public void fillNextPage(int pageSize, Output output) {
        LoggedChatEvent event;
        int count = 0;
        while (count < pageSize && (event = this.log.lookup(this.eventId)) != null) {
            LoggedChatMessage.Player message;
            int eventId = this.eventId--;
            if (!(event instanceof LoggedChatMessage.Player) || (message = (LoggedChatMessage.Player)event).message().equals(this.lastMessage)) continue;
            if (this.acceptMessage(output, message)) {
                if (this.missedCount > 0) {
                    output.acceptDivider(Component.translatable("gui.chatSelection.fold", this.missedCount));
                    this.missedCount = 0;
                }
                output.acceptMessage(eventId, message);
                ++count;
            } else {
                ++this.missedCount;
            }
            this.lastMessage = message.message();
        }
    }

    private boolean acceptMessage(Output output, LoggedChatMessage.Player event) {
        PlayerChatMessage message = event.message();
        boolean context = this.contextBuilder.acceptContext(message);
        if (this.canReport.test(event)) {
            this.contextBuilder.trackContext(message);
            if (this.previousLink != null && !this.previousLink.isDescendantOf(message.link())) {
                output.acceptDivider(Component.translatable("gui.chatSelection.join", event.profile().name()).withStyle(ChatFormatting.YELLOW));
            }
            this.previousLink = message.link();
            return true;
        }
        return context;
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Output {
        public void acceptMessage(int var1, LoggedChatMessage.Player var2);

        public void acceptDivider(Component var1);
    }
}

