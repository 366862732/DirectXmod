/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsPersistence {
    private static final String FILE_NAME = "realms_persistence.json";
    private static final GuardedSerializer GSON = new GuardedSerializer();
    private static final Logger LOGGER = LogUtils.getLogger();

    public RealmsPersistenceData read() {
        return RealmsPersistence.readFile();
    }

    public void save(RealmsPersistenceData data) {
        RealmsPersistence.writeFile(data);
    }

    public static RealmsPersistenceData readFile() {
        Path file = RealmsPersistence.getPathToData();
        try {
            String contents = Files.readString(file, StandardCharsets.UTF_8);
            RealmsPersistenceData realmsPersistenceData = GSON.fromJson(contents, RealmsPersistenceData.class);
            if (realmsPersistenceData != null) {
                return realmsPersistenceData;
            }
        }
        catch (NoSuchFileException contents) {
        }
        catch (Exception e) {
            LOGGER.warn("Failed to read Realms storage {}", (Object)file, (Object)e);
        }
        return new RealmsPersistenceData();
    }

    public static void writeFile(RealmsPersistenceData data) {
        Path file = RealmsPersistence.getPathToData();
        try {
            Files.writeString(file, (CharSequence)GSON.toJson(data), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static Path getPathToData() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FILE_NAME);
    }

    @Environment(value=EnvType.CLIENT)
    public static class RealmsPersistenceData
    implements ReflectionBasedSerialization {
        @SerializedName(value="newsLink")
        public @Nullable String newsLink;
        @SerializedName(value="hasUnreadNews")
        public boolean hasUnreadNews;
    }
}

