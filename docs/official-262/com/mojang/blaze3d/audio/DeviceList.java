/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.openal.ALC10
 *  org.lwjgl.openal.ALUtil
 */
package com.mojang.blaze3d.audio;

import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALUtil;

@Environment(value=EnvType.CLIENT)
public record DeviceList(@Nullable String defaultDevice, List<String> allDevices) {
    public static final DeviceList EMPTY = new DeviceList(null, List.of());

    public static DeviceList query() {
        if (!ALC10.alcIsExtensionPresent((long)0L, (CharSequence)"ALC_ENUMERATE_ALL_EXT")) {
            return EMPTY;
        }
        List<String> allDevices = Objects.requireNonNullElse(ALUtil.getStringList((long)0L, (int)4115), List.of());
        String defaultDevice = ALC10.alcGetString((long)0L, (int)4114);
        return new DeviceList(defaultDevice, allDevices);
    }
}

