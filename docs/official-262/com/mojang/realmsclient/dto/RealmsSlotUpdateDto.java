/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record RealmsSlotUpdateDto(@SerializedName(value="slotId") int slotId, @SerializedName(value="spawnProtection") int spawnProtection, @SerializedName(value="forceGameMode") boolean forceGameMode, @SerializedName(value="difficulty") int difficulty, @SerializedName(value="gameMode") int gameMode, @SerializedName(value="slotName") String slotName, @SerializedName(value="version") String version, @SerializedName(value="compatibility") RealmsServer.Compatibility compatibility, @SerializedName(value="worldTemplateId") long templateId, @SerializedName(value="worldTemplateImage") @Nullable String templateImage, @SerializedName(value="hardcore") boolean hardcore) implements ReflectionBasedSerialization
{
    public RealmsSlotUpdateDto(int slotId, RealmsWorldOptions options, boolean hardcore) {
        this(slotId, options.spawnProtection, options.forceGameMode, options.difficulty, options.gameMode, options.getSlotName(slotId), options.version, options.compatibility, options.templateId, options.templateImage, hardcore);
    }
}

