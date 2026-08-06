/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuDevice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface GpuBackend {
    public String getName();

    public void setWindowHints();

    public void handleWindowCreationErrors(GLFWErrorCapture.Error var1) throws BackendCreationException;

    public GpuDevice createDevice(long var1, ShaderSource var3, GpuDebugOptions var4, Runnable var5) throws BackendCreationException;
}

