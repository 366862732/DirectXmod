/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonSyntaxException
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class GpuWarnlistManager
extends SimplePreparableReloadListener<Preparations> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier GPU_WARNLIST_LOCATION = Identifier.withDefaultNamespace("gpu_warnlist.json");
    private ImmutableMap<String, String> warnings = ImmutableMap.of();
    private boolean showWarning;
    private boolean warningDismissed;

    public boolean hasWarnings() {
        return !this.warnings.isEmpty();
    }

    public boolean willShowWarning() {
        return this.hasWarnings() && !this.warningDismissed;
    }

    public void showWarning() {
        this.showWarning = true;
    }

    public void dismissWarning() {
        this.warningDismissed = true;
    }

    public boolean isShowingWarning() {
        return this.showWarning && !this.warningDismissed;
    }

    public void resetWarnings() {
        this.showWarning = false;
        this.warningDismissed = false;
    }

    public @Nullable String getRendererWarnings() {
        return (String)this.warnings.get((Object)"renderer");
    }

    public @Nullable String getVersionWarnings() {
        return (String)this.warnings.get((Object)"version");
    }

    public @Nullable String getVendorWarnings() {
        return (String)this.warnings.get((Object)"vendor");
    }

    public @Nullable String getAllWarnings() {
        StringBuilder sb = new StringBuilder();
        this.warnings.forEach((k, v) -> sb.append((String)k).append(": ").append((String)v));
        return sb.isEmpty() ? null : sb.toString();
    }

    @Override
    protected Preparations prepare(ResourceManager manager, ProfilerFiller profiler) {
        ArrayList rendererPatterns = Lists.newArrayList();
        ArrayList versionPatterns = Lists.newArrayList();
        ArrayList vendorPatterns = Lists.newArrayList();
        JsonObject root = GpuWarnlistManager.parseJson(manager, profiler);
        if (root != null) {
            try (Zone ignored = profiler.zone("compile_regex");){
                GpuWarnlistManager.compilePatterns(root.getAsJsonArray("renderer"), rendererPatterns);
                GpuWarnlistManager.compilePatterns(root.getAsJsonArray("version"), versionPatterns);
                GpuWarnlistManager.compilePatterns(root.getAsJsonArray("vendor"), vendorPatterns);
            }
        }
        return new Preparations(rendererPatterns, versionPatterns, vendorPatterns);
    }

    @Override
    protected void apply(Preparations preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.warnings = preparations.apply();
    }

    private static void compilePatterns(JsonArray jsonArray, List<Pattern> patternList) {
        jsonArray.forEach(e -> patternList.add(Pattern.compile(e.getAsString(), 2)));
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static @Nullable JsonObject parseJson(ResourceManager manager, ProfilerFiller profiler) {
        try (Zone ignored = profiler.zone("parse_json");){
            JsonObject jsonObject;
            block14: {
                BufferedReader resource = manager.openAsReader(GPU_WARNLIST_LOCATION);
                try {
                    jsonObject = StrictJsonParser.parse(resource).getAsJsonObject();
                    if (resource == null) break block14;
                }
                catch (Throwable throwable) {
                    if (resource != null) {
                        try {
                            ((Reader)resource).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                ((Reader)resource).close();
            }
            return jsonObject;
        }
        catch (JsonSyntaxException | IOException e) {
            LOGGER.warn("Failed to load GPU warnlist", e);
            return null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    protected static final class Preparations {
        private final List<Pattern> rendererPatterns;
        private final List<Pattern> versionPatterns;
        private final List<Pattern> vendorPatterns;

        private Preparations(List<Pattern> rendererPatterns, List<Pattern> versionPatterns, List<Pattern> vendorPatterns) {
            this.rendererPatterns = rendererPatterns;
            this.versionPatterns = versionPatterns;
            this.vendorPatterns = vendorPatterns;
        }

        private static String matchAny(List<Pattern> patterns, String input) {
            ArrayList allMatches = Lists.newArrayList();
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(input);
                while (matcher.find()) {
                    allMatches.add(matcher.group());
                }
            }
            return String.join((CharSequence)", ", allMatches);
        }

        private ImmutableMap<String, String> apply() {
            ImmutableMap.Builder map = new ImmutableMap.Builder();
            GpuDevice device = RenderSystem.getDevice();
            DeviceInfo deviceInfo = device.getDeviceInfo();
            if (deviceInfo.backendName().equals("OpenGL")) {
                String vendorFails;
                String versionFails;
                String rendererFails = Preparations.matchAny(this.rendererPatterns, deviceInfo.name());
                if (!rendererFails.isEmpty()) {
                    map.put((Object)"renderer", (Object)rendererFails);
                }
                if (!(versionFails = Preparations.matchAny(this.versionPatterns, deviceInfo.driverInfo())).isEmpty()) {
                    map.put((Object)"version", (Object)versionFails);
                }
                if (!(vendorFails = Preparations.matchAny(this.vendorPatterns, deviceInfo.vendorName())).isEmpty()) {
                    map.put((Object)"vendor", (Object)vendorFails);
                }
            }
            return map.build();
        }
    }
}

