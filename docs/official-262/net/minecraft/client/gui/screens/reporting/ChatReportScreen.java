/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.reporting;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.AbstractReportScreen;
import net.minecraft.client.gui.screens.reporting.ChatSelectionScreen;
import net.minecraft.client.gui.screens.reporting.ReportReasonSelectionScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class ChatReportScreen
extends AbstractReportScreen<ChatReport.Builder> {
    private static final Component TITLE = Component.translatable("gui.chatReport.title");
    private static final Component SELECT_CHAT_MESSAGE = Component.translatable("gui.chatReport.select_chat");
    private MultiLineEditBox commentBox;
    private Button selectMessagesButton;
    private Button selectReasonButton;

    private ChatReportScreen(Screen lastScreen, ReportingContext reportingContext, ChatReport.Builder reportBuilder) {
        super(TITLE, lastScreen, reportingContext, reportBuilder);
    }

    public ChatReportScreen(Screen lastScreen, ReportingContext reportingContext, UUID playerId) {
        this(lastScreen, reportingContext, new ChatReport.Builder(playerId, reportingContext.sender().reportLimits()));
    }

    public ChatReportScreen(Screen lastScreen, ReportingContext reportingContext, ChatReport draft) {
        this(lastScreen, reportingContext, new ChatReport.Builder(draft, reportingContext.sender().reportLimits()));
    }

    @Override
    protected void addContent() {
        this.selectMessagesButton = this.layout.addChild(Button.builder(SELECT_CHAT_MESSAGE, b -> this.minecraft.gui.setScreen(new ChatSelectionScreen(this, this.reportingContext, (ChatReport.Builder)this.reportBuilder, updatedReport -> {
            this.reportBuilder = updatedReport;
            this.onReportChanged();
        }))).width(280).build());
        this.selectReasonButton = Button.builder(SELECT_REASON, b -> this.minecraft.gui.setScreen(new ReportReasonSelectionScreen(this, ((ChatReport.Builder)this.reportBuilder).reason(), ReportType.CHAT, reason -> {
            ((ChatReport.Builder)this.reportBuilder).setReason((ReportReason)((Object)((Object)reason)));
            this.onReportChanged();
        }))).width(280).build();
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.selectReasonButton, OBSERVED_WHAT_LABEL));
        this.commentBox = this.createCommentBox(280, this.font.lineHeight * 8, comments -> {
            ((ChatReport.Builder)this.reportBuilder).setComments((String)comments);
            this.onReportChanged();
        });
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, s -> s.paddingBottom(12)));
    }

    @Override
    protected void onReportChanged() {
        IntSet reportedMessages = ((ChatReport.Builder)this.reportBuilder).reportedMessages();
        if (reportedMessages.isEmpty()) {
            this.selectMessagesButton.setMessage(SELECT_CHAT_MESSAGE);
        } else {
            this.selectMessagesButton.setMessage(Component.translatable("gui.chatReport.selected_chat", reportedMessages.size()));
        }
        ReportReason reportReason = ((ChatReport.Builder)this.reportBuilder).reason();
        if (reportReason != null) {
            this.selectReasonButton.setMessage(reportReason.title());
        } else {
            this.selectReasonButton.setMessage(SELECT_REASON);
        }
        super.onReportChanged();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (super.mouseReleased(event)) {
            return true;
        }
        return this.commentBox.mouseReleased(event);
    }
}

