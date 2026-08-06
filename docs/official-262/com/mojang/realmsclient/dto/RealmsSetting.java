/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record RealmsSetting(@SerializedName(value="name") String name, @SerializedName(value="value") String value) implements ReflectionBasedSerialization
{
    public static RealmsSetting hardcoreSetting(boolean hardcore) {
        return new RealmsSetting("hardcore", Boolean.toString(hardcore));
    }

    public static boolean isHardcore(List<RealmsSetting> settings) {
        for (RealmsSetting setting : settings) {
            if (!setting.name().equals("hardcore")) continue;
            return Boolean.parseBoolean(setting.value());
        }
        return false;
    }
}

