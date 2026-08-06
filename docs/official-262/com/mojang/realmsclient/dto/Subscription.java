/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.time.Instant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record Subscription(Instant startDate, int daysLeft, SubscriptionType type) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Subscription parse(String json) {
        try {
            JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
            return new Subscription(JsonUtils.getDateOr("startDate", jsonObject), JsonUtils.getIntOr("daysLeft", jsonObject, 0), Subscription.typeFrom(JsonUtils.getStringOr("subscriptionType", jsonObject, null)));
        }
        catch (Exception e) {
            LOGGER.error("Could not parse Subscription", (Throwable)e);
            return new Subscription(Instant.EPOCH, 0, SubscriptionType.NORMAL);
        }
    }

    private static SubscriptionType typeFrom(@Nullable String subscriptionType) {
        try {
            if (subscriptionType != null) {
                return SubscriptionType.valueOf(subscriptionType);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return SubscriptionType.NORMAL;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum SubscriptionType {
        NORMAL,
        RECURRING;

    }
}

