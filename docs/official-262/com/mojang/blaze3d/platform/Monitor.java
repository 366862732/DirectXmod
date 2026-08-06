/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWVidMode
 *  org.lwjgl.glfw.GLFWVidMode$Buffer
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.platform;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.logging.LogUtils;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record Monitor(String monitorName, long monitor, List<VideoMode> videoModes, VideoMode currentMode, int x, int y) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    public static @Nullable Monitor tryCreate(long monitor) {
        GLFWErrorCapture glfwErrors = new GLFWErrorCapture();
        try {
            GLFWVidMode currentMode;
            int[] y;
            int[] x;
            ImmutableList.Builder videoModes;
            String monitorName;
            GLFWErrorScope gLFWErrorScope;
            block15: {
                GLFWVidMode.Buffer modes;
                block14: {
                    gLFWErrorScope = new GLFWErrorScope(glfwErrors);
                    monitorName = Monitor.queryMonitorName(monitor);
                    videoModes = ImmutableList.builder();
                    modes = GLFW.glfwGetVideoModes((long)monitor);
                    if (modes != null) break block14;
                    LOGGER.warn("Failed to query video modes of monitor {}", (Object)monitorName);
                    Monitor monitor2 = null;
                    gLFWErrorScope.close();
                    return monitor2;
                }
                try {
                    for (int i = modes.limit() - 1; i >= 0; --i) {
                        modes.position(i);
                        VideoMode mode = new VideoMode(modes);
                        if (mode.getRedBits() < 8 || mode.getGreenBits() < 8 || mode.getBlueBits() < 8) continue;
                        videoModes.add((Object)mode);
                    }
                    x = new int[1];
                    y = new int[1];
                    GLFW.glfwGetMonitorPos((long)monitor, (int[])x, (int[])y);
                    currentMode = GLFW.glfwGetVideoMode((long)monitor);
                    if (currentMode != null) break block15;
                    LOGGER.warn("Failed to query current video mode of monitor {}", (Object)monitorName);
                    Monitor monitor3 = null;
                    gLFWErrorScope.close();
                    return monitor3;
                }
                catch (Throwable throwable) {
                    try {
                        gLFWErrorScope.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
            }
            Monitor monitor4 = new Monitor(monitorName, monitor, (List<VideoMode>)videoModes.build(), new VideoMode(currentMode), x[0], y[0]);
            gLFWErrorScope.close();
            return monitor4;
        }
        finally {
            for (GLFWErrorCapture.Error error : glfwErrors) {
                LOGGER.error("GLFW error collected during monitor 0x{} query: {}", (Object)HEX_FORMAT.toHexDigits(monitor), (Object)error);
            }
        }
    }

    private static String queryMonitorName(long monitor) {
        String monitorName = Objects.requireNonNull(GLFW.glfwGetMonitorName((long)monitor), "unknown");
        return monitorName + "[0x" + HEX_FORMAT.toHexDigits(monitor) + "]";
    }

    public VideoMode getPreferredVidMode(Optional<VideoMode> expectedMode) {
        if (expectedMode.isPresent()) {
            VideoMode videoMode = expectedMode.get();
            for (VideoMode mode : this.videoModes) {
                if (!mode.equals(videoMode)) continue;
                return mode;
            }
        }
        return this.currentMode;
    }

    public int indexOfMode(VideoMode videoMode) {
        return this.videoModes.indexOf(videoMode);
    }

    public VideoMode mode(int mode) {
        return this.videoModes.get(mode);
    }

    public int modeCount() {
        return this.videoModes.size();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s(%s at (%d,%d))", this.monitorName, this.currentMode, this.x, this.y);
    }
}

