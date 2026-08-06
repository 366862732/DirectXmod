/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.Version
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWErrorCallback
 *  org.lwjgl.glfw.GLFWErrorCallbackI
 *  org.lwjgl.glfw.GLFWVidMode
 *  org.slf4j.Logger
 *  oshi.SystemInfo
 *  oshi.hardware.CentralProcessor
 */
package com.mojang.blaze3d.platform;

import com.google.common.base.Joiner;
import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import org.jspecify.annotations.Nullable;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

@Environment(value=EnvType.CLIENT)
public class GLX {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int glfwPlatformType = 393221;
    private static @Nullable String cpuInfo;

    public static int _getRefreshRate(Window window) {
        RenderSystem.assertOnRenderThread();
        long monitor = GLFW.glfwGetWindowMonitor((long)window.handle());
        if (monitor == 0L) {
            monitor = GLFW.glfwGetPrimaryMonitor();
        }
        GLFWVidMode videoMode = monitor == 0L ? null : GLFW.glfwGetVideoMode((long)monitor);
        return videoMode == null ? 0 : videoMode.refreshRate();
    }

    public static String _getLWJGLVersion() {
        return Version.getVersion();
    }

    public static LongSupplier _initGlfw() {
        LongSupplier timeSource;
        Window.checkGlfwError((error, description) -> {
            throw new IllegalStateException(String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", error, description));
        });
        GLFWErrorCapture collectedErrors = new GLFWErrorCapture();
        try (GLFWErrorScope gLFWErrorScope = new GLFWErrorScope(collectedErrors);){
            if (GLFW.glfwPlatformSupported((int)393219) && GLFW.glfwPlatformSupported((int)393220) && !SharedConstants.DEBUG_PREFER_WAYLAND) {
                GLFW.glfwInitHint((int)327683, (int)393220);
            }
            if (!GLFW.glfwInit()) {
                throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on((String)",").join((Iterable)collectedErrors));
            }
            timeSource = () -> (long)(GLFW.glfwGetTime() * 1.0E9);
            glfwPlatformType = GLFW.glfwGetPlatform();
        }
        for (GLFWErrorCapture.Error error2 : collectedErrors) {
            LOGGER.error("GLFW error collected during initialization: {}", (Object)error2);
        }
        return timeSource;
    }

    public static int getGlfwPlatform() {
        return glfwPlatformType;
    }

    public static void _setGlfwErrorCallback(GLFWErrorCallbackI onFullscreenError) {
        GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback((GLFWErrorCallbackI)onFullscreenError);
        if (previousCallback != null) {
            previousCallback.free();
        }
    }

    public static boolean _shouldClose(Window window) {
        return GLFW.glfwWindowShouldClose((long)window.handle());
    }

    public static String _getCpuInfo() {
        if (cpuInfo == null) {
            cpuInfo = "<unknown>";
            try {
                CentralProcessor processor = new SystemInfo().getHardware().getProcessor();
                cpuInfo = String.format(Locale.ROOT, "%dx %s", processor.getLogicalProcessorCount(), processor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return cpuInfo;
    }

    public static <T> T make(Supplier<T> factory) {
        return factory.get();
    }

    public static int glfwBool(boolean value) {
        return value ? 1 : 0;
    }
}

