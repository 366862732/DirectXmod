/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.JsonAdapter
 *  com.google.gson.annotations.SerializedName
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.ServiceQuality;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record RealmsJoinInformation(@SerializedName(value="address") @Nullable String address, @SerializedName(value="resourcePackUrl") @Nullable String resourcePackUrl, @SerializedName(value="resourcePackHash") @Nullable String resourcePackHash, @SerializedName(value="sessionRegionData") @Nullable RegionData regionData) implements ReflectionBasedSerialization
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RealmsJoinInformation EMPTY = new RealmsJoinInformation(null, null, null, null);

    public static RealmsJoinInformation parse(GuardedSerializer gson, String json) {
        try {
            RealmsJoinInformation server = gson.fromJson(json, RealmsJoinInformation.class);
            if (server == null) {
                LOGGER.error("Could not parse RealmsServerAddress: {}", (Object)json);
                return EMPTY;
            }
            return server;
        }
        catch (Exception e) {
            LOGGER.error("Could not parse RealmsServerAddress", (Throwable)e);
            return EMPTY;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record RegionData(@SerializedName(value="regionName") @JsonAdapter(value=RealmsRegion.RealmsRegionJsonAdapter.class) @Nullable RealmsRegion region, @SerializedName(value="serviceQuality") @JsonAdapter(value=ServiceQuality.RealmsServiceQualityJsonAdapter.class) @Nullable ServiceQuality serviceQuality) implements ReflectionBasedSerialization
    {
    }
}

