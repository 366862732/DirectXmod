/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.JsonAdapter
 *  com.google.gson.annotations.SerializedName
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.ServiceQuality;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record RegionDataDto(@SerializedName(value="regionName") @JsonAdapter(value=RealmsRegion.RealmsRegionJsonAdapter.class) RealmsRegion region, @SerializedName(value="serviceQuality") @JsonAdapter(value=ServiceQuality.RealmsServiceQualityJsonAdapter.class) ServiceQuality serviceQuality) implements ReflectionBasedSerialization
{
}

