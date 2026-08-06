/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonSyntaxException
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.FileUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.components.debug;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.File;
import java.io.IOException;
import java.lang.runtime.SwitchBootstraps;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.datafix.DataFixTypes;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class DebugScreenEntryList {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_DEBUG_PROFILE_VERSION = 4649;
    private final Map<Identifier, DebugScreenEntryStatus> allStatuses = new HashMap<Identifier, DebugScreenEntryStatus>();
    private final List<Identifier> currentlyEnabled = new ArrayList<Identifier>();
    private boolean isOverlayVisible = false;
    private @Nullable DebugScreenProfile profile;
    private final File debugProfileFile;
    private long currentlyEnabledVersion;
    private final Codec<SerializedOptions> codec;

    public DebugScreenEntryList(File workingDirectory, DataFixer dataFixer) {
        this.debugProfileFile = new File(workingDirectory, "debug-profile.json");
        this.codec = DataFixTypes.DEBUG_PROFILE.wrapCodec(SerializedOptions.CODEC, dataFixer, 4649);
        this.load();
    }

    public void load() {
        try {
            if (!this.debugProfileFile.isFile()) {
                this.resetToProfile(DebugScreenProfile.DEFAULT);
                this.rebuildCurrentList();
                return;
            }
            Dynamic data = new Dynamic((DynamicOps)JsonOps.INSTANCE, (Object)StrictJsonParser.parse(FileUtils.readFileToString((File)this.debugProfileFile, (Charset)StandardCharsets.UTF_8)));
            SerializedOptions serializedOptions = (SerializedOptions)this.codec.parse(data).getOrThrow(error -> new IOException("Could not parse debug profile JSON: " + error));
            if (serializedOptions.profile().isPresent()) {
                this.resetToProfile(serializedOptions.profile().get());
            } else {
                this.resetStatuses(serializedOptions.custom().orElse(Map.of()));
                this.profile = null;
            }
        }
        catch (JsonSyntaxException | IOException e) {
            LOGGER.error("Couldn't read debug profile file {}, resetting to default", (Object)this.debugProfileFile, (Object)e);
            this.resetToProfile(DebugScreenProfile.DEFAULT);
            this.save();
        }
        this.rebuildCurrentList();
    }

    private void resetStatuses(Map<Identifier, DebugScreenEntryStatus> newEntries) {
        this.allStatuses.clear();
        this.allStatuses.putAll(newEntries);
    }

    private void resetToProfile(DebugScreenProfile profile) {
        this.profile = profile;
        this.resetStatuses(DebugScreenEntries.PROFILES.get(profile));
    }

    public void loadProfile(DebugScreenProfile profile) {
        this.resetToProfile(profile);
        this.rebuildCurrentList();
    }

    public DebugScreenEntryStatus getStatus(Identifier location) {
        return this.allStatuses.getOrDefault(location, DebugScreenEntryStatus.NEVER);
    }

    public boolean isCurrentlyEnabled(Identifier location) {
        return this.currentlyEnabled.contains(location);
    }

    public void setStatus(Identifier location, DebugScreenEntryStatus status) {
        this.profile = null;
        this.allStatuses.put(location, status);
        this.rebuildCurrentList();
        this.save();
    }

    public boolean toggleStatus(Identifier location) {
        DebugScreenEntryStatus status;
        DebugScreenEntryStatus debugScreenEntryStatus = status = this.allStatuses.get(location);
        int n = 0;
        switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"ALWAYS_ON", "IN_OVERLAY", "NEVER"}, (DebugScreenEntryStatus)debugScreenEntryStatus, n)) {
            case 0: {
                this.setStatus(location, DebugScreenEntryStatus.NEVER);
                return false;
            }
            case 1: {
                if (this.isOverlayVisible) {
                    this.setStatus(location, DebugScreenEntryStatus.NEVER);
                    return false;
                }
                this.setStatus(location, DebugScreenEntryStatus.ALWAYS_ON);
                return true;
            }
            case 2: {
                if (this.isOverlayVisible) {
                    this.setStatus(location, DebugScreenEntryStatus.IN_OVERLAY);
                } else {
                    this.setStatus(location, DebugScreenEntryStatus.ALWAYS_ON);
                }
                return true;
            }
        }
        this.setStatus(location, DebugScreenEntryStatus.ALWAYS_ON);
        return true;
    }

    public Collection<Identifier> getCurrentlyEnabled() {
        return List.copyOf(this.currentlyEnabled);
    }

    public void toggleDebugOverlay() {
        this.setOverlayVisible(!this.isOverlayVisible);
    }

    public void setOverlayVisible(boolean visible) {
        if (this.isOverlayVisible != visible) {
            this.isOverlayVisible = visible;
            this.rebuildCurrentList();
        }
    }

    public boolean isOverlayVisible() {
        return this.isOverlayVisible;
    }

    public void rebuildCurrentList() {
        this.currentlyEnabled.clear();
        boolean isReducedDebugInfo = Minecraft.getInstance().showOnlyReducedInfo();
        this.allStatuses.forEach((key, value) -> {
            DebugScreenEntry debug;
            if ((value == DebugScreenEntryStatus.ALWAYS_ON || this.isOverlayVisible && value == DebugScreenEntryStatus.IN_OVERLAY) && (debug = DebugScreenEntries.getEntry(key)) != null && debug.isAllowed(isReducedDebugInfo)) {
                this.currentlyEnabled.add((Identifier)key);
            }
        });
        this.currentlyEnabled.sort(Comparator.naturalOrder());
        ++this.currentlyEnabledVersion;
    }

    public long getCurrentlyEnabledVersion() {
        return this.currentlyEnabledVersion;
    }

    public boolean isUsingProfile(DebugScreenProfile profile) {
        return this.profile == profile;
    }

    public void save() {
        SerializedOptions serializedOptions = new SerializedOptions(Optional.ofNullable(this.profile), this.profile == null ? Optional.of(this.allStatuses) : Optional.empty());
        try {
            FileUtils.writeStringToFile((File)this.debugProfileFile, (String)((JsonElement)this.codec.encodeStart((DynamicOps)JsonOps.INSTANCE, (Object)serializedOptions).getOrThrow()).toString(), (Charset)StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            LOGGER.error("Failed to save debug profile file {}", (Object)this.debugProfileFile, (Object)e);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record SerializedOptions(Optional<DebugScreenProfile> profile, Optional<Map<Identifier, DebugScreenEntryStatus>> custom) {
        private static final Codec<Map<Identifier, DebugScreenEntryStatus>> CUSTOM_ENTRIES_CODEC = Codec.unboundedMap(Identifier.CODEC, DebugScreenEntryStatus.CODEC);
        public static final Codec<SerializedOptions> CODEC = RecordCodecBuilder.create(i -> i.group((App)DebugScreenProfile.CODEC.optionalFieldOf("profile").forGetter(SerializedOptions::profile), (App)CUSTOM_ENTRIES_CODEC.optionalFieldOf("custom").forGetter(SerializedOptions::custom)).apply((Applicative)i, SerializedOptions::new));
    }
}

