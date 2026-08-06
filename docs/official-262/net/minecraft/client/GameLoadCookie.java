/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import com.mojang.realmsclient.client.RealmsClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.main.GameConfig;

@Environment(value=EnvType.CLIENT)
public record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
}

