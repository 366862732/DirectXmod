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
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record WorldTemplatePaginatedList(List<WorldTemplate> templates, int page, int size, int total) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public WorldTemplatePaginatedList(int size) {
        this(List.of(), 0, size, -1);
    }

    public boolean isLastPage() {
        return this.page * this.size >= this.total && this.page > 0 && this.total > 0 && this.size > 0;
    }

    public static WorldTemplatePaginatedList parse(String json) {
        ArrayList<WorldTemplate> templates = new ArrayList<WorldTemplate>();
        int page = 0;
        int size = 0;
        int total = 0;
        try {
            JsonObject object = LenientJsonParser.parse(json).getAsJsonObject();
            if (object.get("templates").isJsonArray()) {
                for (JsonElement element : object.get("templates").getAsJsonArray()) {
                    WorldTemplate template = WorldTemplate.parse(element.getAsJsonObject());
                    if (template == null) continue;
                    templates.add(template);
                }
            }
            page = JsonUtils.getIntOr("page", object, 0);
            size = JsonUtils.getIntOr("size", object, 0);
            total = JsonUtils.getIntOr("total", object, 0);
        }
        catch (Exception e) {
            LOGGER.error("Could not parse WorldTemplatePaginatedList", (Throwable)e);
        }
        return new WorldTemplatePaginatedList(templates, page, size, total);
    }
}

