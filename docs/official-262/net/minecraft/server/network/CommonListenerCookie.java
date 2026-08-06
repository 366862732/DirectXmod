/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 */
package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;

public record CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, boolean transferred) {
    public static CommonListenerCookie createInitial(GameProfile gameProfile, boolean transferred) {
        return new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), transferred);
    }
}

