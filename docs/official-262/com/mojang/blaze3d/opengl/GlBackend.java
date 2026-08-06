/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.glfw.GLFW
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@Environment(value=EnvType.CLIENT)
public class GlBackend
implements GpuBackend {
    private static final int VERSION_MAJOR = 3;
    private static final int VERSION_MINOR = 3;

    @Override
    public String getName() {
        return "OpenGL";
    }

    @Override
    public void setWindowHints() {
        GLFW.glfwWindowHint((int)139265, (int)196609);
        GLFW.glfwWindowHint((int)139275, (int)221185);
        GLFW.glfwWindowHint((int)139266, (int)3);
        GLFW.glfwWindowHint((int)139267, (int)3);
        GLFW.glfwWindowHint((int)139272, (int)204801);
        GLFW.glfwWindowHint((int)139270, (int)1);
    }

    @Override
    public void handleWindowCreationErrors(@Nullable GLFWErrorCapture.Error error) throws BackendCreationException {
        if (error != null) {
            if (error.error() == 65542) {
                throw new BackendCreationException("Driver does not support OpenGL", BackendCreationException.Reason.OPENGL_MISSING);
            }
            if (error.error() == 65543) {
                throw new BackendCreationException("Driver does not support OpenGL 3.3", BackendCreationException.Reason.OPENGL_MISSING);
            }
            throw new BackendCreationException(String.format(Locale.ROOT, "GLFW_ERROR: 0x%X", error.error()), BackendCreationException.Reason.OPENGL_MISSING);
        }
        throw new BackendCreationException("Failed to create window with OpenGL context", BackendCreationException.Reason.OPENGL_MISSING);
    }

    @Override
    public GpuDevice createDevice(long window, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions, Runnable criticalShaderLoader) {
        if (MacosUtil.IS_MACOS) {
            MacosUtil.setWindowColorSpaceForOpenGLBecauseGLFWDoesnt(window);
        }
        return new GpuDevice(new GlDevice(window, defaultShaderSource, debugOptions), criticalShaderLoader);
    }
}

