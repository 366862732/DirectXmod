/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWMonitorCallback
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class MonitorManager
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Long2ObjectMap<Monitor> monitors = new Long2ObjectOpenHashMap();

    public MonitorManager() {
        GLFW.glfwSetMonitorCallback(this::onMonitorChange);
        PointerBuffer buffer = GLFW.glfwGetMonitors();
        if (buffer != null) {
            for (int i = 0; i < buffer.limit(); ++i) {
                Monitor monitor = Monitor.tryCreate(buffer.get(i));
                if (monitor == null) continue;
                this.monitors.put(buffer.get(i), (Object)monitor);
            }
        }
    }

    private void onMonitorChange(long monitorHandle, int event) {
        RenderSystem.assertOnRenderThread();
        if (event == 262145) {
            Monitor monitor = Monitor.tryCreate(monitorHandle);
            if (monitor != null) {
                this.monitors.put(monitorHandle, (Object)monitor);
                LOGGER.debug("Monitor {} connected. Current monitors: {}", (Object)monitor, this.monitors);
            }
        } else if (event == 262146) {
            Monitor monitor = (Monitor)this.monitors.remove(monitorHandle);
            LOGGER.debug("Monitor {} disconnected. Current monitors: {}", monitor != null ? monitor : Long.valueOf(monitorHandle), this.monitors);
        }
    }

    public @Nullable Monitor getMonitor(long monitor) {
        return (Monitor)this.monitors.get(monitor);
    }

    public @Nullable Monitor findBestMonitor(Window window) {
        long windowMonitor = GLFW.glfwGetWindowMonitor((long)window.handle());
        if (windowMonitor != 0L) {
            return this.getMonitor(windowMonitor);
        }
        int winMinX = window.getX();
        int winMaxX = winMinX + window.getScreenWidth();
        int winMinY = window.getY();
        int winMaxY = winMinY + window.getScreenHeight();
        int maxArea = -1;
        Monitor result = null;
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        LOGGER.debug("Selecting monitor - primary: {}, current monitors: {}", (Object)primaryMonitor, this.monitors);
        for (Monitor monitor : this.monitors.values()) {
            int sy;
            int monMinX = monitor.x();
            int monMaxX = monMinX + monitor.currentMode().getWidth();
            int monMinY = monitor.y();
            int monMaxY = monMinY + monitor.currentMode().getHeight();
            int minX = MonitorManager.clamp(winMinX, monMinX, monMaxX);
            int maxX = MonitorManager.clamp(winMaxX, monMinX, monMaxX);
            int minY = MonitorManager.clamp(winMinY, monMinY, monMaxY);
            int maxY = MonitorManager.clamp(winMaxY, monMinY, monMaxY);
            int sx = Math.max(0, maxX - minX);
            int area = sx * (sy = Math.max(0, maxY - minY));
            if (area > maxArea) {
                result = monitor;
                maxArea = area;
                continue;
            }
            if (area != maxArea || primaryMonitor != monitor.monitor()) continue;
            LOGGER.debug("Primary monitor {} is preferred to monitor {}", (Object)monitor, (Object)result);
            result = monitor;
        }
        LOGGER.debug("Selected monitor: {}", result);
        return result;
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        GLFWMonitorCallback callback = GLFW.glfwSetMonitorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}

