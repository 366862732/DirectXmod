/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.report.AbuseReport
 *  com.mojang.authlib.minecraft.report.AbuseReportLimits
 *  com.mojang.authlib.minecraft.report.ReportedEntity
 *  com.mojang.datafixers.util.Either
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.NameReportScreen;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class NameReport
extends Report {
    private final String reportedName;

    private NameReport(UUID reportId, Instant createdAt, UUID reportedProfileId, String reportedName) {
        super(reportId, createdAt, reportedProfileId);
        this.reportedName = reportedName;
    }

    public String getReportedName() {
        return this.reportedName;
    }

    @Override
    public NameReport copy() {
        NameReport result = new NameReport(this.reportId, this.createdAt, this.reportedProfileId, this.reportedName);
        result.comments = this.comments;
        result.attested = this.attested;
        return result;
    }

    @Override
    public Screen createScreen(Screen lastScreen, ReportingContext context) {
        return new NameReportScreen(lastScreen, context, this);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder
    extends Report.Builder<NameReport> {
        public Builder(NameReport report, AbuseReportLimits limits) {
            super(report, limits);
        }

        public Builder(UUID reportedProfileId, String reportedName, AbuseReportLimits limits) {
            super(new NameReport(UUID.randomUUID(), Instant.now(), reportedProfileId, reportedName), limits);
        }

        @Override
        public boolean hasContent() {
            return StringUtils.isNotEmpty((CharSequence)this.comments());
        }

        @Override
        public @Nullable Report.CannotBuildReason checkBuildable() {
            if (((NameReport)this.report).comments.length() > this.limits.maxOpinionCommentsLength()) {
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
            ReportedEntity reportedEntity = new ReportedEntity(((NameReport)this.report).reportedProfileId);
            AbuseReport abuseReport = AbuseReport.name((String)((NameReport)this.report).comments, (ReportedEntity)reportedEntity, (Instant)((NameReport)this.report).createdAt);
            return Either.left((Object)new Report.Result(((NameReport)this.report).reportId, ReportType.USERNAME, abuseReport));
        }
    }
}

