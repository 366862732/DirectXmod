/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.packs.PackResources;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ResourceLoadStateTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private @Nullable ReloadState reloadState;
    private int reloadCount;

    public void startReload(ReloadReason reloadReason, List<PackResources> packs) {
        ++this.reloadCount;
        if (this.reloadState != null && !this.reloadState.finished) {
            LOGGER.warn("Reload already ongoing, replacing");
        }
        this.reloadState = new ReloadState(reloadReason, (List)packs.stream().map(PackResources::packId).collect(ImmutableList.toImmutableList()));
    }

    public void startRecovery(Throwable reason) {
        if (this.reloadState == null) {
            LOGGER.warn("Trying to signal reload recovery, but nothing was started");
            this.reloadState = new ReloadState(ReloadReason.UNKNOWN, (List<String>)ImmutableList.of());
        }
        this.reloadState.recoveryReloadInfo = new RecoveryInfo(reason);
    }

    public void finishReload() {
        if (this.reloadState == null) {
            LOGGER.warn("Trying to finish reload, but nothing was started");
        } else {
            this.reloadState.finished = true;
        }
    }

    public void fillCrashReport(CrashReport report) {
        CrashReportCategory category = report.addCategory("Last reload");
        category.setDetail("Reload number", this.reloadCount);
        if (this.reloadState != null) {
            this.reloadState.fillCrashInfo(category);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ReloadState {
        private final ReloadReason reloadReason;
        private final List<String> packs;
        private @Nullable RecoveryInfo recoveryReloadInfo;
        private boolean finished;

        private ReloadState(ReloadReason reloadReason, List<String> packs) {
            this.reloadReason = reloadReason;
            this.packs = packs;
        }

        public void fillCrashInfo(CrashReportCategory category) {
            category.setDetail("Reload reason", this.reloadReason.name);
            category.setDetail("Finished", this.finished ? "Yes" : "No");
            category.setDetail("Packs", () -> String.join((CharSequence)", ", this.packs));
            if (this.recoveryReloadInfo != null) {
                this.recoveryReloadInfo.fillCrashInfo(category);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum ReloadReason {
        INITIAL("initial"),
        MANUAL("manual"),
        UNKNOWN("unknown");

        private final String name;

        private ReloadReason(String name) {
            this.name = name;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class RecoveryInfo {
        private final Throwable error;

        private RecoveryInfo(Throwable error) {
            this.error = error;
        }

        public void fillCrashInfo(CrashReportCategory category) {
            category.setDetail("Recovery", "Yes");
            category.setDetail("Recovery reason", () -> {
                StringWriter writer = new StringWriter();
                this.error.printStackTrace(new PrintWriter(writer));
                return writer.toString();
            });
        }
    }
}

