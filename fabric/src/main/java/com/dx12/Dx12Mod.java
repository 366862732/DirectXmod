package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

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
    private static int shaderProgram = 0;
    private static int texUniformLocation = -1;

    // Frame data shared between tick callback and HUD callback
    private static ByteBuffer pendingPixels = null;
    private static int pendingWidth = 0;
    private static int pendingHeight = 0;

    // Track whether GL resources are valid (Minecraft may destroy them when menu opens)
    private static boolean glResourcesValid = false;

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

            // Check if GL resources were destroyed by Minecraft (menu open, etc.)
            boolean resourcesLost = !glResourcesValid
                || !glIsTexture(texId)
                || !glIsVertexArray(vaoId)
                || shaderProgram == 0;

            if (resourcesLost) {
                textureCreated = false;
                vaoInitialized = false;
                shaderProgram = 0;
                texId = 0;
                vaoId = 0;
                vboId = 0;
                iboId = 0;
                glResourcesValid = false;
                LOGGER.info("GL resources lost, will recreate on next valid frame");
            }

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
                initShaderProgram();
            }

            // Skip pixel upload if shader wasn't created
            if (shaderProgram == 0) return;

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
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            // Guard: skip if no pending frame or resources invalid
            if (pendingPixels == null || !pendingPixels.hasRemaining() || !glResourcesValid) {
                return;
            }

            int width = pendingWidth;
            int height = pendingHeight;
            ByteBuffer pixels = pendingPixels;
            pendingPixels = null; // Consume the pending frame

            // Final check right before drawing
            if (!glIsTexture(texId) || !glIsVertexArray(vaoId) || shaderProgram == 0) {
                glResourcesValid = false;
                textureCreated = false;
                vaoInitialized = false;
                shaderProgram = 0;
                return;
            }

            try {
                // Upload pixels to texture
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                glBindTexture(GL_TEXTURE_2D, 0);

                // Draw fullscreen quad
                drawFullScreenQuad();
                glResourcesValid = true;
            } catch (Throwable t) {
                LOGGER.warn("OpenGL draw failed, resetting state: {}", t.getMessage());
                glResourcesValid = false;
                textureCreated = false;
                vaoInitialized = false;
                shaderProgram = 0;
                texId = 0;
                vaoId = 0;
                vboId = 0;
                iboId = 0;
            }

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

        // Fullscreen quad vertices: position (x,y,z) + texCoord (u,v)
        // Using screen-space coordinates (0 to 1) for simplicity
        float[] data = {
            // x,     y,     z,   u,   v
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
        if (!vaoInitialized || shaderProgram == 0) return;

        // Check for stale GL resources (Minecraft may have destroyed them)
        if (!glIsVertexArray(vaoId)) {
            LOGGER.warn("VAO {} was deleted by Minecraft, recreating", vaoId);
            vaoInitialized = false;
            initVAO();
            initShaderProgram();
            if (!vaoInitialized || shaderProgram == 0) return;
        }
        if (!glIsTexture(texId)) {
            LOGGER.warn("Texture {} was deleted, recreating", texId);
            textureCreated = false;
        }

        glUseProgram(shaderProgram);
        glUniform1i(texUniformLocation, 0);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindTexture(GL_TEXTURE_2D, texId);

        // Rebind VAO + buffers (Minecraft may have cleared them)
        glBindVertexArray(vaoId);

        // Bind IB explicitly (Core Profile requires it)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0L);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private static void initShaderProgram() {
        if (shaderProgram != 0) return;

        // Simple vertex shader: pass through position and texCoord
        String vertSrc =
            "#version 330 core\n" +
            "layout(location = 0) in vec3 aPos;\n" +
            "layout(location = 1) in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "void main(){\n" +
            "  gl_Position = vec4(aPos, 1.0);\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

        // Simple fragment shader: sample texture
        String fragSrc =
            "#version 330 core\n" +
            "in vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "out vec4 FragColor;\n" +
            "void main(){\n" +
            "  FragColor = texture(uTexture, vTexCoord);\n" +
            "}\n";

        int vertShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertShader, vertSrc);
        glCompileShader(vertShader);

        if (glGetShaderi(vertShader, GL_COMPILE_STATUS) == GL_FALSE) {
            LOGGER.error("Vertex shader compile failed: {}", glGetShaderInfoLog(vertShader));
            return;
        }

        int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragShader, fragSrc);
        glCompileShader(fragShader);

        if (glGetShaderi(fragShader, GL_COMPILE_STATUS) == GL_FALSE) {
            LOGGER.error("Fragment shader compile failed: {}", glGetShaderInfoLog(fragShader));
            glDeleteShader(vertShader);
            return;
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertShader);
        glAttachShader(shaderProgram, fragShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            LOGGER.error("Shader program link failed: {}", glGetProgramInfoLog(shaderProgram));
            glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        texUniformLocation = glGetUniformLocation(shaderProgram, "uTexture");

        glDeleteShader(vertShader);
        glDeleteShader(fragShader);

        LOGGER.info("Shader program created: prog={}, texLoc={}", shaderProgram, texUniformLocation);
    }
}
