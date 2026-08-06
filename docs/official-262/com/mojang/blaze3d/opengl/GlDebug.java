/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.EvictingQueue
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.opengl.ARBDebugOutput
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.opengl.GLCapabilities
 *  org.lwjgl.opengl.GLDebugMessageARBCallback
 *  org.lwjgl.opengl.GLDebugMessageARBCallbackI
 *  org.lwjgl.opengl.GLDebugMessageCallback
 *  org.lwjgl.opengl.GLDebugMessageCallbackI
 *  org.lwjgl.opengl.KHRDebug
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.opengl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageARBCallbackI;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class GlDebug {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CIRCULAR_LOG_SIZE = 10;
    private final Queue<LogEntry> MESSAGE_BUFFER = EvictingQueue.create((int)10);
    private volatile @Nullable LogEntry lastEntry;
    private static final List<Integer> DEBUG_LEVELS = ImmutableList.of((Object)37190, (Object)37191, (Object)37192, (Object)33387);
    private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of((Object)37190, (Object)37191, (Object)37192);

    private static String printUnknownToken(int token) {
        return "Unknown (0x" + HexFormat.of().withUpperCase().toHexDigits(token) + ")";
    }

    public static String sourceToString(int source) {
        return switch (source) {
            case 33350 -> "API";
            case 33351 -> "WINDOW SYSTEM";
            case 33352 -> "SHADER COMPILER";
            case 33353 -> "THIRD PARTY";
            case 33354 -> "APPLICATION";
            case 33355 -> "OTHER";
            default -> GlDebug.printUnknownToken(source);
        };
    }

    public static String typeToString(int type) {
        return switch (type) {
            case 33356 -> "ERROR";
            case 33357 -> "DEPRECATED BEHAVIOR";
            case 33358 -> "UNDEFINED BEHAVIOR";
            case 33359 -> "PORTABILITY";
            case 33360 -> "PERFORMANCE";
            case 33361 -> "OTHER";
            case 33384 -> "MARKER";
            default -> GlDebug.printUnknownToken(type);
        };
    }

    public static String severityToString(int severity) {
        return switch (severity) {
            case 37190 -> "HIGH";
            case 37191 -> "MEDIUM";
            case 37192 -> "LOW";
            case 33387 -> "NOTIFICATION";
            default -> GlDebug.printUnknownToken(severity);
        };
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void printDebugLog(int source, int type, int id, int severity, int length, long message, long userParam) {
        LogEntry entry;
        String msg = GLDebugMessageCallback.getMessage((int)length, (long)message);
        Queue<LogEntry> queue = this.MESSAGE_BUFFER;
        synchronized (queue) {
            entry = this.lastEntry;
            if (entry == null || !entry.isSame(source, type, id, severity, msg)) {
                entry = new LogEntry(source, type, id, severity, msg);
                this.MESSAGE_BUFFER.add(entry);
                this.lastEntry = entry;
            } else {
                ++entry.count;
            }
        }
        LOGGER.info("OpenGL debug message: {}", (Object)entry);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<String> getLastOpenGlDebugMessages() {
        Queue<LogEntry> queue = this.MESSAGE_BUFFER;
        synchronized (queue) {
            ArrayList result = Lists.newArrayListWithCapacity((int)this.MESSAGE_BUFFER.size());
            for (LogEntry e : this.MESSAGE_BUFFER) {
                result.add(String.valueOf(e) + " x " + e.count);
            }
            return result;
        }
    }

    public static @Nullable GlDebug enableDebugCallback(int verbosity, boolean debugSynchronousGlLogs, Set<String> enabledExtensions) {
        if (verbosity <= 0) {
            return null;
        }
        GLCapabilities caps = GL.getCapabilities();
        if (caps.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
            GlDebug debug = new GlDebug();
            enabledExtensions.add("GL_KHR_debug");
            GL33C.glEnable((int)37600);
            if (debugSynchronousGlLogs) {
                GL33C.glEnable((int)33346);
            }
            for (int i = 0; i < DEBUG_LEVELS.size(); ++i) {
                boolean isEnabled = i < verbosity;
                KHRDebug.glDebugMessageControl((int)4352, (int)4352, (int)DEBUG_LEVELS.get(i), (int[])null, (boolean)isEnabled);
            }
            KHRDebug.glDebugMessageCallback((GLDebugMessageCallbackI)GLDebugMessageCallback.create(debug::printDebugLog), (long)0L);
            return debug;
        }
        if (caps.GL_ARB_debug_output && GlDevice.USE_GL_ARB_debug_output) {
            GlDebug debug = new GlDebug();
            enabledExtensions.add("GL_ARB_debug_output");
            if (debugSynchronousGlLogs) {
                GL33C.glEnable((int)33346);
            }
            for (int i = 0; i < DEBUG_LEVELS_ARB.size(); ++i) {
                boolean isEnabled = i < verbosity;
                ARBDebugOutput.glDebugMessageControlARB((int)4352, (int)4352, (int)DEBUG_LEVELS_ARB.get(i), (int[])null, (boolean)isEnabled);
            }
            ARBDebugOutput.glDebugMessageCallbackARB((GLDebugMessageARBCallbackI)GLDebugMessageARBCallback.create(debug::printDebugLog), (long)0L);
            return debug;
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    private static class LogEntry {
        private final int id;
        private final int source;
        private final int type;
        private final int severity;
        private final String message;
        private int count = 1;

        private LogEntry(int source, int type, int id, int severity, String message) {
            this.id = id;
            this.source = source;
            this.type = type;
            this.severity = severity;
            this.message = message;
        }

        private boolean isSame(int source, int type, int id, int severity, String message) {
            return type == this.type && source == this.source && id == this.id && severity == this.severity && message.equals(this.message);
        }

        public String toString() {
            return "id=" + this.id + ", source=" + GlDebug.sourceToString(this.source) + ", type=" + GlDebug.typeToString(this.type) + ", severity=" + GlDebug.severityToString(this.severity) + ", message='" + this.message + "'";
        }
    }
}

