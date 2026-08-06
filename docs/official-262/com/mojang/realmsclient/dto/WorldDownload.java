/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record WorldDownload(String downloadLink, String resourcePackUrl, String resourcePackHash) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static WorldDownload parse(String json) {
        JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
        try {
            return new WorldDownload(JsonUtils.getStringOr("downloadLink", jsonObject, ""), JsonUtils.getStringOr("resourcePackUrl", jsonObject, ""), JsonUtils.getStringOr("resourcePackHash", jsonObject, ""));
        }
        catch (Exception e) {
            LOGGER.error("Could not parse WorldDownload", (Throwable)e);
            return new WorldDownload("", "", "");
        }
    }
}

