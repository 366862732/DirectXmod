/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWErrorCallbackI
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.platform.Window;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringDecomposer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public class ClipboardManager {
    public static final int FORMAT_UNAVAILABLE = 65545;
    private final ByteBuffer clipboardScratchBuffer = BufferUtils.createByteBuffer((int)8192);

    public String getClipboard(Window window, GLFWErrorCallbackI errorCallback) {
        try (GLFWErrorScope ignored = new GLFWErrorScope(errorCallback);){
            String clipboard = GLFW.glfwGetClipboardString((long)window.handle());
            String string = clipboard = clipboard != null ? StringDecomposer.filterBrokenSurrogates(clipboard) : "";
            return string;
        }
    }

    private static void pushClipboard(Window window, ByteBuffer buffer, byte[] data) {
        buffer.clear();
        buffer.put(data);
        buffer.put((byte)0);
        buffer.flip();
        GLFW.glfwSetClipboardString((long)window.handle(), (ByteBuffer)buffer);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setClipboard(Window window, String clipboard) {
        byte[] encoded = clipboard.getBytes(StandardCharsets.UTF_8);
        int encodedLength = encoded.length + 1;
        if (encodedLength < this.clipboardScratchBuffer.capacity()) {
            ClipboardManager.pushClipboard(window, this.clipboardScratchBuffer, encoded);
        } else {
            ByteBuffer buffer = MemoryUtil.memAlloc((int)encodedLength);
            try {
                ClipboardManager.pushClipboard(window, buffer, encoded);
            }
            finally {
                MemoryUtil.memFree((ByteBuffer)buffer);
            }
        }
    }
}

