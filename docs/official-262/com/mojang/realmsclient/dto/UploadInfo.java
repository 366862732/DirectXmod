/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record UploadInfo(boolean worldClosed, @Nullable String token, URI uploadEndpoint) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_SCHEMA = "http://";
    private static final int DEFAULT_PORT = 8080;
    private static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");

    public static @Nullable UploadInfo parse(String json) {
        try {
            int endpointPort;
            URI uploadEndpoint;
            JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
            String endpointStr = JsonUtils.getStringOr("uploadEndpoint", jsonObject, null);
            if (endpointStr != null && (uploadEndpoint = UploadInfo.assembleUri(endpointStr, endpointPort = JsonUtils.getIntOr("port", jsonObject, -1))) != null) {
                boolean worldClosed = JsonUtils.getBooleanOr("worldClosed", jsonObject, false);
                String token = JsonUtils.getStringOr("token", jsonObject, null);
                return new UploadInfo(worldClosed, token, uploadEndpoint);
            }
        }
        catch (Exception e) {
            LOGGER.error("Could not parse UploadInfo", (Throwable)e);
        }
        return null;
    }

    @VisibleForTesting
    public static @Nullable URI assembleUri(String endpoint, int portOverride) {
        Matcher matcher = URI_SCHEMA_PATTERN.matcher(endpoint);
        String endpointWithSchema = UploadInfo.ensureEndpointSchema(endpoint, matcher);
        try {
            URI result = new URI(endpointWithSchema);
            int selectedPort = UploadInfo.selectPortOrDefault(portOverride, result.getPort());
            if (selectedPort != result.getPort()) {
                return new URI(result.getScheme(), result.getUserInfo(), result.getHost(), selectedPort, result.getPath(), result.getQuery(), result.getFragment());
            }
            return result;
        }
        catch (URISyntaxException e) {
            LOGGER.warn("Failed to parse URI {}", (Object)endpointWithSchema, (Object)e);
            return null;
        }
    }

    private static int selectPortOrDefault(int portOverride, int parsedPort) {
        if (portOverride != -1) {
            return portOverride;
        }
        if (parsedPort != -1) {
            return parsedPort;
        }
        return 8080;
    }

    private static String ensureEndpointSchema(String endpoint, Matcher matcher) {
        if (matcher.find()) {
            return endpoint;
        }
        return DEFAULT_SCHEMA + endpoint;
    }

    public static String createRequest(@Nullable String uploadToken) {
        JsonObject request = new JsonObject();
        if (uploadToken != null) {
            request.addProperty("token", uploadToken);
        }
        return request.toString();
    }
}

