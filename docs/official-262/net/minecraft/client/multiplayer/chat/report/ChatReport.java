/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.authlib.minecraft.report.AbuseReport
 *  com.mojang.authlib.minecraft.report.AbuseReportLimits
 *  com.mojang.authlib.minecraft.report.ReportChatMessage
 *  com.mojang.authlib.minecraft.report.ReportEvidence
 *  com.mojang.authlib.minecraft.report.ReportedEntity
 *  com.mojang.datafixers.util.Either
 *  it.unimi.dsi.fastutil.ints.IntCollection
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer.chat.report;

import com.google.common.collect.Lists;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Optionull;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportContextBuilder;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChatReport
extends Report {
    private final IntSet reportedMessages = new IntOpenHashSet();

    private ChatReport(UUID reportId, Instant createdAt, UUID reportedProfileId) {
        super(reportId, createdAt, reportedProfileId);
    }

    public void toggleReported(int id, AbuseReportLimits limits) {
        if (this.reportedMessages.contains(id)) {
            this.reportedMessages.remove(id);
        } else if (this.reportedMessages.size() < limits.maxReportedMessageCount()) {
            this.reportedMessages.add(id);
        }
    }

    @Override
    public ChatReport copy() {
        ChatReport result = new ChatReport(this.reportId, this.createdAt, this.reportedProfileId);
        result.reportedMessages.addAll((IntCollection)this.reportedMessages);
        result.comments = this.comments;
        result.reason = this.reason;
        result.attested = this.attested;
        return result;
    }

    @Override
    public Screen createScreen(Screen lastScreen, ReportingContext context) {
        return new ChatReportScreen(lastScreen, context, this);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder
    extends Report.Builder<ChatReport> {
        public Builder(ChatReport report, AbuseReportLimits limits) {
            super(report, limits);
        }

        public Builder(UUID reportedProfileId, AbuseReportLimits limits) {
            super(new ChatReport(UUID.randomUUID(), Instant.now(), reportedProfileId), limits);
        }

        public IntSet reportedMessages() {
            return ((ChatReport)this.report).reportedMessages;
        }

        public void toggleReported(int id) {
            ((ChatReport)this.report).toggleReported(id, this.limits);
        }

        public boolean isReported(int id) {
            return ((ChatReport)this.report).reportedMessages.contains(id);
        }

        @Override
        public boolean hasContent() {
            return StringUtils.isNotEmpty((CharSequence)this.comments()) || !this.reportedMessages().isEmpty() || this.reason() != null;
        }

        @Override
        public @Nullable Report.CannotBuildReason checkBuildable() {
            if (((ChatReport)this.report).reportedMessages.isEmpty()) {
                return Report.CannotBuildReason.NO_REPORTED_MESSAGES;
            }
            if (((ChatReport)this.report).reportedMessages.size() > this.limits.maxReportedMessageCount()) {
                return Report.CannotBuildReason.TOO_MANY_MESSAGES;
            }
            if (((ChatReport)this.report).reason == null) {
                return Report.CannotBuildReason.NO_REASON;
            }
            if (((ChatReport)this.report).comments.length() > this.limits.maxOpinionCommentsLength()) {
                return Report.CannotBuildReason.COMMENT_TOO_LONG;
            }
            return super.checkBuildable();
        }

        @Override
        public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext reportingContext) {
            Report.CannotBuildReason error = this.checkBuildable();
            if (error != null) {
                return Either.right((Object)error);
            }
            String reason = Objects.requireNonNull(((ChatReport)this.report).reason).backendName();
            ReportEvidence evidence = this.buildEvidence(reportingContext);
            ReportedEntity reportedEntity = new ReportedEntity(((ChatReport)this.report).reportedProfileId);
            AbuseReport abuseReport = AbuseReport.chat((String)((ChatReport)this.report).comments, (String)reason, (ReportEvidence)evidence, (ReportedEntity)reportedEntity, (Instant)((ChatReport)this.report).createdAt);
            return Either.left((Object)new Report.Result(((ChatReport)this.report).reportId, ReportType.CHAT, abuseReport));
        }

        private ReportEvidence buildEvidence(ReportingContext reportingContext) {
            ArrayList allReportMessages = new ArrayList();
            ChatReportContextBuilder contextBuilder = new ChatReportContextBuilder(this.limits.leadingContextMessageCount());
            contextBuilder.collectAllContext(reportingContext.chatLog(), (IntCollection)((ChatReport)this.report).reportedMessages, (id, event) -> allReportMessages.add(this.buildReportedChatMessage(event, this.isReported(id))));
            return new ReportEvidence(Lists.reverse(allReportMessages));
        }

        private ReportChatMessage buildReportedChatMessage(LoggedChatMessage.Player chat, boolean reported) {
            SignedMessageLink link = chat.message().link();
            SignedMessageBody body = chat.message().signedBody();
            List<ByteBuffer> lastSeen = body.lastSeen().entries().stream().map(MessageSignature::asByteBuffer).toList();
            ByteBuffer signature = Optionull.map(chat.message().signature(), MessageSignature::asByteBuffer);
            return new ReportChatMessage(link.index(), link.sender(), link.sessionId(), body.timeStamp(), body.salt(), lastSeen, body.content(), signature, reported);
        }

        public Builder copy() {
            return new Builder(((ChatReport)this.report).copy(), this.limits);
        }
    }
}

