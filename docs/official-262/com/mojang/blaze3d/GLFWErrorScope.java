/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWErrorCallback
 *  org.lwjgl.glfw.GLFWErrorCallbackI
 */
package com.mojang.blaze3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;

@Environment(value=EnvType.CLIENT)
public class GLFWErrorScope
implements AutoCloseable {
    private final @Nullable GLFWErrorCallback previousCallback;
    private final GLFWErrorCallback expectedCallback;

    public GLFWErrorScope(GLFWErrorCallbackI callback) {
        this.expectedCallback = GLFWErrorCallback.create((GLFWErrorCallbackI)callback);
        this.previousCallback = GLFW.glfwSetErrorCallback((GLFWErrorCallbackI)this.expectedCallback);
    }

    @Override
    public void close() {
        GLFWErrorCallback currentCallback = GLFW.glfwSetErrorCallback((GLFWErrorCallbackI)this.previousCallback);
        if (currentCallback == null || currentCallback.address() != this.expectedCallback.address()) {
            throw new IllegalStateException("GLFW error callback has unexpectedly changed during this scope!");
        }
        currentCallback.close();
    }
}

