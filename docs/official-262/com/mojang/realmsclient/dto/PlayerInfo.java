/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.JsonAdapter
 *  com.google.gson.annotations.SerializedName
 *  com.mojang.util.UUIDTypeAdapter
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class PlayerInfo
extends ValueObject
implements ReflectionBasedSerialization {
    @SerializedName(value="name")
    public final String name;
    @SerializedName(value="uuid")
    @JsonAdapter(value=UUIDTypeAdapter.class)
    public final UUID uuid;
    @SerializedName(value="operator")
    public boolean operator;
    @SerializedName(value="accepted")
    public final boolean accepted;
    @SerializedName(value="online")
    public final boolean online;

    public PlayerInfo(String name, UUID uuid, boolean operator, boolean accepted, boolean online) {
        this.name = name;
        this.uuid = uuid;
        this.operator = operator;
        this.accepted = accepted;
        this.online = online;
    }
}

