package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
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
    private static int iboId = 0;
    private static boolean vaoInitialized = false;

    // Frame data shared between tick callback and HUD callback
    private static ByteBuffer pendingPixels = null;
    private static int pendingWidth = 0;
    private static int pendingHeight = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");

        D3D12Bridge.init();
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        // Phase 1: Tick callback - get window info, create texture/VAO, call Rust renderer
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long glfwWindow = GLFW.glfwGetCurrentContext();
            if (glfwWindow == 0) return;

            java.nio.IntBuffer wBuf = BufferUtils.createIntBuffer(1);
            java.nio.IntBuffer hBuf = BufferUtils.createIntBuffer(1);
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
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glBindTexture(GL_TEXTURE_2D, 0);
                textureCreated = true;
                LOGGER.info("OpenGL texture created: {}", texId);

                initVAO();
            }

            D3D12Bridge.syncWindowSize(width, height);

            ByteBuffer pixels = D3D12Bridge.renderFrame();
            if (pixels == null || !pixels.hasRemaining()) return;

            LOGGER.info("Rendering frame: {} bytes, VAO={}", pixels.remaining(), vaoId);

            // Store frame data for HUD callback to draw
            pendingPixels = pixels;
            pendingWidth = width;
            pendingHeight = height;
        });

        // Phase 2: HUD callback - upload pixels and draw fullscreen quad
        // This runs during the HUD render phase when OpenGL context is guaranteed safe
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            if (pendingPixels == null || !pendingPixels.hasRemaining() || !vaoInitialized) {
                return;
            }

            int width = pendingWidth;
            int height = pendingHeight;
            ByteBuffer pixels = pendingPixels;
            pendingPixels = null; // Consume the pending frame

            // Upload pixels to texture
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);

            // Draw fullscreen quad
            drawFullScreenQuad();

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

        // Fullscreen quad vertices in clip space (-1 to +1), with tex coords
        float[] data = {
            // x,     y,     z,   u,   v
            -1f, -1f,  0f,  0f, 0f,  // bottom-left
             1f, -1f,  0f,  1f, 0f,  // bottom-right
            -1f,  1f,  0f,  0f, 1f,  // top-left
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
        iboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
        ByteBuffer ibBuf = BufferUtils.createByteBuffer(idx.length);
        ibBuf.put(idx).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ibBuf, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        vaoInitialized = true;
        LOGGER.info("VAO initialized: vao={}, vbo={}, ibo={}", vaoId, vboId, iboId);
    }

    private static void drawFullScreenQuad() {
        if (!vaoInitialized) return;

        // Pure modern OpenGL - no matrix transforms, no attrib pushes
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBlendEquation(GL_FUNC_ADD);

        glBindTexture(GL_TEXTURE_2D, texId);
        glColor4f(1, 1, 1, 1);

        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }
}
