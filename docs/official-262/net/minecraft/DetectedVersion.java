/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

public class DetectedVersion {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final WorldVersion BUILT_IN = DetectedVersion.createBuiltIn(UUID.randomUUID().toString().replaceAll("-", ""), "Development Version");

    public static WorldVersion createBuiltIn(String id, String name) {
        return DetectedVersion.createBuiltIn(id, name, true);
    }

    public static WorldVersion createBuiltIn(String id, String name, boolean stable) {
        return new WorldVersion.Simple(id, name, new DataVersion(4903, "main"), SharedConstants.getProtocolVersion(), PackFormat.of(88, 0), PackFormat.of(107, 1), new Date(), stable);
    }

    private static WorldVersion createFromJson(JsonObject root) {
        JsonObject packVersion = GsonHelper.getAsJsonObject(root, "pack_version");
        return new WorldVersion.Simple(GsonHelper.getAsString(root, "id"), GsonHelper.getAsString(root, "name"), new DataVersion(GsonHelper.getAsInt(root, "world_version"), GsonHelper.getAsString(root, "series_id", "main")), GsonHelper.getAsInt(root, "protocol_version"), PackFormat.of(GsonHelper.getAsInt(packVersion, "resource_major"), GsonHelper.getAsInt(packVersion, "resource_minor")), PackFormat.of(GsonHelper.getAsInt(packVersion, "data_major"), GsonHelper.getAsInt(packVersion, "data_minor")), Date.from(ZonedDateTime.parse(GsonHelper.getAsString(root, "build_time")).toInstant()), GsonHelper.getAsBoolean(root, "stable"));
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static WorldVersion tryDetectVersion() {
        try (InputStream stream = DetectedVersion.class.getResourceAsStream("/version.json");){
            WorldVersion worldVersion;
            if (stream == null) {
                LOGGER.warn("Missing version information!");
                WorldVersion worldVersion2 = BUILT_IN;
                return worldVersion2;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);){
                worldVersion = DetectedVersion.createFromJson(GsonHelper.parse(reader));
            }
            return worldVersion;
        }
        catch (JsonParseException | IOException e) {
            throw new IllegalStateException("Game version information is corrupt", e);
        }
    }
}

