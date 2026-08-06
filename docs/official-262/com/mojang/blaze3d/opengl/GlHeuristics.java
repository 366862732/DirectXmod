/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.opengl.GLCapabilities
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class GlHeuristics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> DEVICE_NAMES_THAT_IMPLY_CPU = List.of("mesa offscreen", "llvmpipe");
    private static final List<String> DEVICE_NAMES_THAT_IMPLY_VIRTUAL = List.of("virtgl");
    private final boolean isGlOnDx12;
    private final boolean isAmd;

    GlHeuristics(String deviceName) {
        this.isGlOnDx12 = GlHeuristics.isGlOnDx12(deviceName);
        this.isAmd = GlHeuristics.isAmd(deviceName);
    }

    public boolean isGlOnDx12() {
        return this.isGlOnDx12;
    }

    public boolean isAmd() {
        return this.isAmd;
    }

    private static boolean isGlOnDx12(String deviceName) {
        boolean isWindowsArm64 = Util.getPlatform() == Util.OS.WINDOWS && Util.isAarch64();
        return isWindowsArm64 || deviceName.startsWith("D3D12");
    }

    private static boolean isAmd(String deviceName) {
        return deviceName.contains("AMD");
    }

    private static int getMaxSupportedTextureSize() {
        int maxReported = GlStateManager._getInteger(3379);
        for (int texSize = Math.max(32768, maxReported); texSize >= 1024; texSize >>= 1) {
            GlStateManager._texImage2D(32868, 0, 6408, texSize, texSize, 0, 6408, 5121, null);
            int width = GlStateManager._getTexLevelParameter(32868, 0, 4096);
            if (width == 0) continue;
            return texSize;
        }
        int maxSupportedTextureSize = Math.max(maxReported, 1024);
        LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", (Object)maxSupportedTextureSize);
        return maxSupportedTextureSize;
    }

    public DeviceInfo createDeviceInfo(GLCapabilities capabilities, int maxSupportedAnisotropy, Set<String> enabledExtensions) {
        String renderer = GlStateManager._getString(7937);
        String vendor = GlStateManager._getString(7936);
        return new DeviceInfo(renderer, vendor, GlStateManager._getString(7938), capabilities.GL_ARB_clip_control, "OpenGL", 1.0f, new DeviceLimits(maxSupportedAnisotropy, GL33C.glGetInteger((int)35380), GlHeuristics.getMaxSupportedTextureSize(), Long.MAX_VALUE, 0, GL33C.glGetInteger((int)34852)), new DeviceFeatures(enabledExtensions.contains("GL_ARB_shader_draw_parameters"), false, true, enabledExtensions.contains("GL_ARB_multi_draw_indirect"), enabledExtensions.contains("GL_ARB_draw_indirect"), enabledExtensions.contains("GL_ARB_base_instance"), enabledExtensions.contains("GL_ARB_buffer_storage")), Collections.unmodifiableSet(enabledExtensions), new HintsAndWorkarounds(this.isGlOnDx12(), this.isAmd()), this.guessDeviceType(renderer.toLowerCase(Locale.ROOT), vendor.toLowerCase(Locale.ROOT)));
    }

    private DeviceType guessDeviceType(String renderer, String vendor) {
        if (vendor.contains("intel")) {
            if (renderer.contains("arc")) {
                return DeviceType.DISCRETE;
            }
            return DeviceType.INTEGRATED;
        }
        for (String string : DEVICE_NAMES_THAT_IMPLY_CPU) {
            if (!renderer.contains(string)) continue;
            return DeviceType.CPU;
        }
        for (String string : DEVICE_NAMES_THAT_IMPLY_VIRTUAL) {
            if (!renderer.contains(string)) continue;
            return DeviceType.VIRTUAL;
        }
        return DeviceType.OTHER;
    }
}

