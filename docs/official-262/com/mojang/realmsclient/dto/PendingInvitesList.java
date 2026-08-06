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
import com.mojang.realmsclient.dto.PendingInvite;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record PendingInvitesList(List<PendingInvite> pendingInvites) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static PendingInvitesList parse(String json) {
        ArrayList<PendingInvite> pendingInvites = new ArrayList<PendingInvite>();
        try {
            JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
            if (jsonObject.get("invites").isJsonArray()) {
                for (JsonElement element : jsonObject.get("invites").getAsJsonArray()) {
                    PendingInvite entry = PendingInvite.parse(element.getAsJsonObject());
                    if (entry == null) continue;
                    pendingInvites.add(entry);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Could not parse PendingInvitesList", (Throwable)e);
        }
        return new PendingInvitesList(pendingInvites);
    }
}

