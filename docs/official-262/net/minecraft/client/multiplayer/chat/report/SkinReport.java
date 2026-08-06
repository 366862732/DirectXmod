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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.SkinReportScreen;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SkinReport
extends Report {
    private final Supplier<PlayerSkin> skinGetter;

    private SkinReport(UUID reportId, Instant createdAt, UUID reportedProfileId, Supplier<PlayerSkin> skinGetter) {
        super(reportId, createdAt, reportedProfileId);
        this.skinGetter = skinGetter;
    }

    public Supplier<PlayerSkin> getSkinGetter() {
        return this.skinGetter;
    }

    @Override
    public SkinReport copy() {
        SkinReport result = new SkinReport(this.reportId, this.createdAt, this.reportedProfileId, this.skinGetter);
        result.comments = this.comments;
        result.reason = this.reason;
        result.attested = this.attested;
        return result;
    }

    @Override
    public Screen createScreen(Screen lastScreen, ReportingContext context) {
        return new SkinReportScreen(lastScreen, context, this);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder
    extends Report.Builder<SkinReport> {
        public Builder(SkinReport report, AbuseReportLimits limits) {
            super(report, limits);
        }

        public Builder(UUID reportedProfileId, Supplier<PlayerSkin> skin, AbuseReportLimits limits) {
            super(new SkinReport(UUID.randomUUID(), Instant.now(), reportedProfileId, skin), limits);
        }

        @Override
        public boolean hasContent() {
            return StringUtils.isNotEmpty((CharSequence)this.comments()) || this.reason() != null;
        }

        @Override
        public @Nullable Report.CannotBuildReason checkBuildable() {
            if (((SkinReport)this.report).reason == null) {
                return Report.CannotBuildReason.NO_REASON;
            }
            if (((SkinReport)this.report).comments.length() > this.limits.maxOpinionCommentsLength()) {
                return Report.CannotBuildReason.COMMENT_TOO_LONG;
            }
            return super.checkBuildable();
        }

        @Override
        public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext reportingContext) {
            String string;
            Report.CannotBuildReason error = this.checkBuildable();
            if (error != null) {
                return Either.right((Object)error);
            }
            String reason = Objects.requireNonNull(((SkinReport)this.report).reason).backendName();
            ReportedEntity reportedEntity = new ReportedEntity(((SkinReport)this.report).reportedProfileId);
            PlayerSkin skin = ((SkinReport)this.report).skinGetter.get();
            ClientAsset.Texture texture = skin.body();
            if (texture instanceof ClientAsset.DownloadedTexture) {
                ClientAsset.DownloadedTexture downloadedTexture = (ClientAsset.DownloadedTexture)texture;
                string = downloadedTexture.url();
            } else {
                string = null;
            }
            String skinUrl = string;
            AbuseReport abuseReport = AbuseReport.skin((String)((SkinReport)this.report).comments, (String)reason, (String)skinUrl, (ReportedEntity)reportedEntity, (Instant)((SkinReport)this.report).createdAt);
            return Either.left((Object)new Report.Result(((SkinReport)this.report).reportId, ReportType.SKIN, abuseReport));
        }
    }
}

