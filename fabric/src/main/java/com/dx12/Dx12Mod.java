package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * GL4DX12 - Cross-version compatible Fabric mod.
 *
 * Design principles:
 * - No Mixin, no @Inject, no @Shadow
 * - Uses ClientModInitializer (Fabric Loader core) + ClientTickEvents (Fabric API)
 * - All window/HWND/size access via LWJGL GLFW (no Yarn mapping issues)
 * - Rust wgpu engine is completely independent of Minecraft
 * - Java side only: pass HWND to Rust, upload pixels via OpenGL
 */
public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private static boolean textureCreated = false;
    private static int texId = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static boolean vaoInitialized = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");

        D3D12Bridge.init();
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        // Register render tick loop - executes on Render thread
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Get GLFW window from current OpenGL context
            // This returns 0 before the first render frame, so we skip early ticks
            long glfwWindow = GLFW.glfwGetCurrentContext();
            if (glfwWindow == 0) return;

            // Get framebuffer size using native GLFW API
            // glfwGetWindowSize returns width and height via IntBuffer pointers
            java.nio.IntBuffer wBuf = java.nio.IntBuffer.allocate(1);
            java.nio.IntBuffer hBuf = java.nio.IntBuffer.allocate(1);
            GLFW.glfwGetWindowSize(glfwWindow, wBuf, hBuf);
            int width = wBuf.get(0);
            int height = hBuf.get(0);
            if (width <= 0 || height <= 0) return;

            if (!textureCreated) {
                long hwnd = D3D12Bridge.getWindowHandle();
                if (hwnd != 0) {
                    D3D12Bridge.setWindow(hwnd);
                    LOGGER.info("WGPU window HWND set: 0x%016x", hwnd);
                }

                texId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexImage2D(GL_TEXTURE_2D, 0, GL12.GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 10497);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 10497);
                glBindTexture(GL_TEXTURE_2D, 0);
                textureCreated = true;
                LOGGER.info("OpenGL texture created: {}", texId);

                initVAO();
            }

            D3D12Bridge.syncWindowSize(width, height);

            ByteBuffer pixels = D3D12Bridge.renderFrame();
            if (pixels == null || !pixels.hasRemaining()) return;

            LOGGER.info("Rendering frame: {} bytes, VAO={}", pixels.remaining(), vaoId);

            // Upload pixels to texture
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);

            // Draw fullscreen quad using VAO
            drawFullScreenQuad(width, height);

            // Check for GL errors
            int err = glGetError();
            if (err != GL_NO_ERROR) {
                LOGGER.error("GL error after draw: 0x{:08x}", err);
            }
        });

        LOGGER.info("GL4DX12 Mod initialized!");
    }

    private static void initVAO() {
        if (vaoInitialized) return;

        // Interleaved vertex data: x, y, z, u, v (5 floats = 20 bytes per vertex)
        float[] data = {
             0f,  0f,  0f,  0f, 0f,  // bottom-left
             1f,  0f,  0f,  1f, 0f,  // bottom-right
             0f,  1f,  0f,  0f, 1f,  // top-left
             1f,  1f,  0f,  1f, 1f,  // top-right
        };
        byte[] idx = { 0, 1, 2, 2, 1, 3 };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(data.length);
        vertexBuffer.put(data).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Position attribute (location 0): 3 floats, stride 20, offset 0
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // TexCoord attribute (location 1): 2 floats, stride 20, offset 12
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Index buffer
        int ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        ByteBuffer ibBuf = BufferUtils.createByteBuffer(idx.length);
        ibBuf.put(idx).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ibBuf, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        vaoInitialized = true;
        LOGGER.info("VAO initialized: vao={}, vbo={}, ibo={}", vaoId, vboId, ibo);
    }

    private static void drawFullScreenQuad(int width, int height) {
        if (!vaoInitialized) return;

        glPushAttrib(GL_ALL_ATTRIB_BITS);

        // Setup orthographic projection matching vertex coordinates
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, 0, height, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindTexture(GL_TEXTURE_2D, texId);
        glColor4f(1, 1, 1, 1);

        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glPopAttrib();
    }
}
