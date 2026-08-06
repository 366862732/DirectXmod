/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.JsonAdapter
 *  com.google.gson.annotations.SerializedName
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RegionSelectionPreferenceDto
implements ReflectionBasedSerialization {
    public static final RegionSelectionPreferenceDto DEFAULT = new RegionSelectionPreferenceDto(RegionSelectionPreference.AUTOMATIC_OWNER, null);
    @SerializedName(value="regionSelectionPreference")
    @JsonAdapter(value=RegionSelectionPreference.RegionSelectionPreferenceJsonAdapter.class)
    public final RegionSelectionPreference regionSelectionPreference;
    @SerializedName(value="preferredRegion")
    @JsonAdapter(value=RealmsRegion.RealmsRegionJsonAdapter.class)
    public @Nullable RealmsRegion preferredRegion;

    public RegionSelectionPreferenceDto(RegionSelectionPreference regionSelectionPreference, @Nullable RealmsRegion preferredRegion) {
        this.regionSelectionPreference = regionSelectionPreference;
        this.preferredRegion = preferredRegion;
    }

    public RegionSelectionPreferenceDto copy() {
        return new RegionSelectionPreferenceDto(this.regionSelectionPreference, this.preferredRegion);
    }
}

