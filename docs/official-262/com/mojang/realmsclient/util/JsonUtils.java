/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.util.UndashedUuid
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class JsonUtils {
    public static <T> T getRequired(String key, JsonObject node, Function<JsonObject, T> parser) {
        JsonElement property = node.get(key);
        if (property == null || property.isJsonNull()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        if (!property.isJsonObject()) {
            throw new IllegalStateException("Required property " + key + " was not a JsonObject as espected");
        }
        return parser.apply(property.getAsJsonObject());
    }

    public static <T> @Nullable T getOptional(String key, JsonObject node, Function<JsonObject, T> parser) {
        JsonElement property = node.get(key);
        if (property == null || property.isJsonNull()) {
            return null;
        }
        if (!property.isJsonObject()) {
            throw new IllegalStateException("Required property " + key + " was not a JsonObject as espected");
        }
        return parser.apply(property.getAsJsonObject());
    }

    public static String getRequiredString(String key, JsonObject node) {
        String result = JsonUtils.getStringOr(key, node, null);
        if (result == null) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return result;
    }

    @Contract(value="_,_,!null->!null;_,_,null->_")
    public static @Nullable String getStringOr(String key, JsonObject node, @Nullable String defaultValue) {
        JsonElement element = node.get(key);
        if (element != null) {
            return element.isJsonNull() ? defaultValue : element.getAsString();
        }
        return defaultValue;
    }

    @Contract(value="_,_,!null->!null;_,_,null->_")
    public static @Nullable UUID getUuidOr(String key, JsonObject node, @Nullable UUID defaultValue) {
        String uuidAsString = JsonUtils.getStringOr(key, node, null);
        if (uuidAsString == null) {
            return defaultValue;
        }
        return UndashedUuid.fromStringLenient((String)uuidAsString);
    }

    public static int getIntOr(String key, JsonObject node, int defaultValue) {
        JsonElement element = node.get(key);
        if (element != null) {
            return element.isJsonNull() ? defaultValue : element.getAsInt();
        }
        return defaultValue;
    }

    public static long getLongOr(String key, JsonObject node, long defaultValue) {
        JsonElement element = node.get(key);
        if (element != null) {
            return element.isJsonNull() ? defaultValue : element.getAsLong();
        }
        return defaultValue;
    }

    public static boolean getBooleanOr(String key, JsonObject node, boolean defaultValue) {
        JsonElement element = node.get(key);
        if (element != null) {
            return element.isJsonNull() ? defaultValue : element.getAsBoolean();
        }
        return defaultValue;
    }

    public static Instant getDateOr(String key, JsonObject node) {
        JsonElement element = node.get(key);
        if (element != null) {
            return Instant.ofEpochMilli(Long.parseLong(element.getAsString()));
        }
        return Instant.EPOCH;
    }
}

