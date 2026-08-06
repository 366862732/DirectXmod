/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record Ops(Set<String> ops) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Ops parse(String json) {
        HashSet<String> ops = new HashSet<String>();
        try {
            JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
            JsonElement opsArray = jsonObject.get("ops");
            if (opsArray.isJsonArray()) {
                for (JsonElement opsElement : opsArray.getAsJsonArray()) {
                    ops.add(opsElement.getAsString());
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Could not parse Ops", (Throwable)e);
        }
        return new Ops(ops);
    }
}

