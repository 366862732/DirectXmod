/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.glfw.GLFW
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.SurfaceException;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(value=EnvType.CLIENT)
public class GlSurface
implements GpuSurfaceBackend {
    private static final Set<GpuSurface.PresentMode> SUPPORTED_PRESENT_MODES = EnumSet.of(GpuSurface.PresentMode.FIFO, GpuSurface.PresentMode.IMMEDIATE);
    private final long windowHandle;
    private int swapchainWidth;
    private int swapchainHeight;

    public GlSurface(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    @Override
    public void configure(GpuSurface.Configuration config) throws SurfaceException {
        GLFW.glfwSwapInterval((int)(config.presentMode() == GpuSurface.PresentMode.FIFO ? 1 : 0));
        this.swapchainWidth = config.width();
        this.swapchainHeight = config.height();
    }

    @Override
    public boolean isSuboptimal() {
        return false;
    }

    @Override
    public void acquireNextTexture() {
    }

    @Override
    public void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView textureView) {
        ((GlCommandEncoder)commandEncoder).presentTexture(textureView, this.swapchainWidth, this.swapchainHeight);
    }

    @Override
    public void present() {
        GLFW.glfwSwapBuffers((long)this.windowHandle);
    }

    @Override
    public void close() {
    }

    @Override
    public Collection<GpuSurface.PresentMode> supportedPresentModes() {
        return SUPPORTED_PRESENT_MODES;
    }
}

