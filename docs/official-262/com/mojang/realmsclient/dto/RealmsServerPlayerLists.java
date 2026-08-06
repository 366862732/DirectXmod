/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record RealmsServerPlayerLists(Map<Long, List<ResolvableProfile>> servers) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static RealmsServerPlayerLists parse(String json) {
        ImmutableMap.Builder elements = ImmutableMap.builder();
        try {
            JsonObject object = GsonHelper.parse(json);
            if (GsonHelper.isArrayNode(object, "lists")) {
                JsonArray jsonArray = object.getAsJsonArray("lists");
                for (JsonElement jsonElement : jsonArray) {
                    JsonElement element;
                    JsonObject node = jsonElement.getAsJsonObject();
                    String playerListString = JsonUtils.getStringOr("playerList", node, null);
                    List<Object> players = playerListString != null ? ((element = LenientJsonParser.parse(playerListString)).isJsonArray() ? RealmsServerPlayerLists.parsePlayers(element.getAsJsonArray()) : Lists.newArrayList()) : Lists.newArrayList();
                    elements.put((Object)JsonUtils.getLongOr("serverId", node, -1L), (Object)players);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Could not parse RealmsServerPlayerLists", (Throwable)e);
        }
        return new RealmsServerPlayerLists((Map<Long, List<ResolvableProfile>>)elements.build());
    }

    private static List<ResolvableProfile> parsePlayers(JsonArray array) {
        ArrayList<ResolvableProfile> profiles = new ArrayList<ResolvableProfile>(array.size());
        for (JsonElement element : array) {
            UUID playerId;
            if (!element.isJsonObject() || (playerId = JsonUtils.getUuidOr("playerId", element.getAsJsonObject(), null)) == null || Minecraft.getInstance().isLocalPlayer(playerId)) continue;
            profiles.add(ResolvableProfile.createUnresolved(playerId));
        }
        return profiles;
    }

    public List<ResolvableProfile> getProfileResultsFor(long serverId) {
        List<ResolvableProfile> profileResults = this.servers.get(serverId);
        if (profileResults != null) {
            return profileResults;
        }
        return List.of();
    }
}

