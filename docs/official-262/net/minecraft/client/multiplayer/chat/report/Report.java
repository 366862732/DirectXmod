/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.report.AbuseReport
 *  com.mojang.authlib.minecraft.report.AbuseReportLimits
 *  com.mojang.datafixers.util.Either
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class Report {
    protected final UUID reportId;
    protected final Instant createdAt;
    protected final UUID reportedProfileId;
    protected String comments = "";
    protected @Nullable ReportReason reason;
    protected boolean attested;

    public Report(UUID reportId, Instant createdAt, UUID reportedProfileId) {
        this.reportId = reportId;
        this.createdAt = createdAt;
        this.reportedProfileId = reportedProfileId;
    }

    public boolean isReportedPlayer(UUID playerId) {
        return playerId.equals(this.reportedProfileId);
    }

    public abstract Report copy();

    public abstract Screen createScreen(Screen var1, ReportingContext var2);

    @Environment(value=EnvType.CLIENT)
    public record CannotBuildReason(Component message) {
        public static final CannotBuildReason NO_REASON = new CannotBuildReason(Component.translatable("gui.abuseReport.send.no_reason"));
        public static final CannotBuildReason NO_REPORTED_MESSAGES = new CannotBuildReason(Component.translatable("gui.chatReport.send.no_reported_messages"));
        public static final CannotBuildReason TOO_MANY_MESSAGES = new CannotBuildReason(Component.translatable("gui.chatReport.send.too_many_messages"));
        public static final CannotBuildReason COMMENT_TOO_LONG = new CannotBuildReason(Component.translatable("gui.abuseReport.send.comment_too_long"));
        public static final CannotBuildReason NOT_ATTESTED = new CannotBuildReason(Component.translatable("gui.abuseReport.send.not_attested"));

        public Tooltip tooltip() {
            return Tooltip.create(this.message);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Result(UUID id, ReportType reportType, AbuseReport report) {
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Builder<R extends Report> {
        protected final R report;
        protected final AbuseReportLimits limits;

        protected Builder(R report, AbuseReportLimits limits) {
            this.report = report;
            this.limits = limits;
        }

        public R report() {
            return this.report;
        }

        public UUID reportedProfileId() {
            return ((Report)this.report).reportedProfileId;
        }

        public String comments() {
            return ((Report)this.report).comments;
        }

        public boolean attested() {
            return ((Report)this.report()).attested;
        }

        public void setComments(String comments) {
            ((Report)this.report).comments = comments;
        }

        public @Nullable ReportReason reason() {
            return ((Report)this.report).reason;
        }

        public void setReason(ReportReason reason) {
            ((Report)this.report).reason = reason;
        }

        public void setAttested(boolean attested) {
            ((Report)this.report).attested = attested;
        }

        public abstract boolean hasContent();

        public @Nullable CannotBuildReason checkBuildable() {
            if (!((Report)this.report()).attested) {
                return CannotBuildReason.NOT_ATTESTED;
            }
            return null;
        }

        public abstract Either<Result, CannotBuildReason> build(ReportingContext var1);
    }
}

