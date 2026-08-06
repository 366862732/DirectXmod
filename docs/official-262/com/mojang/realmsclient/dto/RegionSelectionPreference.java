/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.TypeAdapter
 *  com.google.gson.stream.JsonReader
 *  com.google.gson.stream.JsonWriter
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public enum RegionSelectionPreference {
    AUTOMATIC_PLAYER(0, "realms.configuration.region_preference.automatic_player"),
    AUTOMATIC_OWNER(1, "realms.configuration.region_preference.automatic_owner"),
    MANUAL(2, "");

    public static final RegionSelectionPreference DEFAULT_SELECTION;
    public final int id;
    public final String translationKey;

    private RegionSelectionPreference(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    static {
        DEFAULT_SELECTION = AUTOMATIC_PLAYER;
    }

    @Environment(value=EnvType.CLIENT)
    public static class RegionSelectionPreferenceJsonAdapter
    extends TypeAdapter<RegionSelectionPreference> {
        private static final Logger LOGGER = LogUtils.getLogger();

        public void write(JsonWriter jsonWriter, RegionSelectionPreference regionSelectionPreference) throws IOException {
            jsonWriter.value((long)regionSelectionPreference.id);
        }

        public RegionSelectionPreference read(JsonReader jsonReader) throws IOException {
            int id = jsonReader.nextInt();
            for (RegionSelectionPreference value : RegionSelectionPreference.values()) {
                if (value.id != id) continue;
                return value;
            }
            LOGGER.warn("Unsupported RegionSelectionPreference {}", (Object)id);
            return DEFAULT_SELECTION;
        }
    }
}

