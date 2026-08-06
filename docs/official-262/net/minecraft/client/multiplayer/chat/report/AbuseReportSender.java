/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.exceptions.MinecraftClientException
 *  com.mojang.authlib.exceptions.MinecraftClientException$ErrorType
 *  com.mojang.authlib.exceptions.MinecraftClientHttpException
 *  com.mojang.authlib.minecraft.UserApiService
 *  com.mojang.authlib.minecraft.report.AbuseReport
 *  com.mojang.authlib.minecraft.report.AbuseReportLimits
 *  com.mojang.authlib.yggdrasil.request.AbuseReportRequest
 *  com.mojang.datafixers.util.Unit
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public interface AbuseReportSender {
    public static AbuseReportSender create(ReportEnvironment environment, UserApiService userApiService) {
        return new Services(environment, userApiService);
    }

    public CompletableFuture<Unit> send(UUID var1, ReportType var2, AbuseReport var3);

    public boolean isEnabled();

    default public AbuseReportLimits reportLimits() {
        return AbuseReportLimits.DEFAULTS;
    }

    @Environment(value=EnvType.CLIENT)
    public record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender
    {
        private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
        private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
        private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

        @Override
        public CompletableFuture<Unit> send(UUID id, ReportType reportType, AbuseReport report) {
            return CompletableFuture.supplyAsync(() -> {
                AbuseReportRequest request = new AbuseReportRequest(1, id, report, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo(), reportType.backendName());
                try {
                    this.userApiService.reportAbuse(request);
                    return Unit.INSTANCE;
                }
                catch (MinecraftClientHttpException e) {
                    Component description = this.getHttpErrorDescription(e);
                    throw new CompletionException(new SendException(description, (Throwable)e));
                }
                catch (MinecraftClientException e) {
                    Component description = this.getErrorDescription(e);
                    throw new CompletionException(new SendException(description, (Throwable)e));
                }
            }, Util.ioPool());
        }

        @Override
        public boolean isEnabled() {
            return this.userApiService.canSendReports();
        }

        private Component getHttpErrorDescription(MinecraftClientHttpException e) {
            return Component.translatable("gui.abuseReport.send.error_message", e.getMessage());
        }

        private Component getErrorDescription(MinecraftClientException e) {
            return switch (e.getType()) {
                default -> throw new MatchException(null, null);
                case MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
                case MinecraftClientException.ErrorType.HTTP_ERROR -> HTTP_ERROR_TEXT;
                case MinecraftClientException.ErrorType.JSON_ERROR -> JSON_ERROR_TEXT;
            };
        }

        @Override
        public AbuseReportLimits reportLimits() {
            return this.userApiService.getAbuseReportLimits();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SendException
    extends ThrowingComponent {
        public SendException(Component component, Throwable cause) {
            super(component, cause);
        }
    }
}

