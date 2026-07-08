package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

    private static int texId = 0;
    private static int vaoId = 0;
    private static int vboId = 0;
    private static int iboId = 0;
    private static boolean vaoInitialized = false;
    private static int shaderProgram = 0;
    private static int texUniformLocation = -1;
    private static long frameCount = 0;

    // Track whether GL resources are valid (Minecraft may destroy them when menu opens)
    private static boolean glResourcesValid = false;

    // Startup delay: skip GL operations during Minecraft's resource loading phase
    // (Shader Loader, texture loading, etc. run on render thread and conflict with our GL calls)
    private static boolean renderReady = false;
    private static long initTime = 0;
    private static long lastTickTime = 0;

    private static final long LOADING_GAP_MS = 2000; // Tick gap > 2s indicates a resource reload
    private static final long RENDER_DELAY_MS = 10000; // Wait 10s after reload before rendering

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");

        D3D12Bridge.init();
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        initTime = System.currentTimeMillis();

        // Single unified tick callback:
        // 1. Get window info  2. Create/recreate GL resources
        // 3. Call Rust render  4. Upload pixels via glTexImage2D (safe)
        // 5. Draw fullscreen quad  6. Restore ALL Minecraft GL state
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long glfwWindow = GLFW.glfwGetCurrentContext();
            if (glfwWindow == 0) return;

            java.nio.IntBuffer wBuf = BufferUtils.createIntBuffer(1);
            java.nio.IntBuffer hBuf = BufferUtils.createIntBuffer(1);
            GLFW.glfwGetWindowSize(glfwWindow, wBuf, hBuf);
            int width = wBuf.get(0);
            int height = hBuf.get(0);
            if (width <= 0 || height <= 0) return;

            // Detect resource reloads: if tick gap > 2 seconds, Minecraft did a reload
            // Reset the render delay timer to avoid conflicts
            long now = System.currentTimeMillis();
            if (lastTickTime > 0 && (now - lastTickTime) > LOADING_GAP_MS) {
                if (renderReady) {
                    LOGGER.info("Resource reload detected ({} s gap), delaying rendering",
                        (now - lastTickTime) / 1000);
                    renderReady = false;
                    initTime = now;
                    glResourcesValid = false;
                }
            }
            lastTickTime = now;

            // Startup/reload delay: wait before attempting GL rendering
            // This avoids conflicts with Minecraft's resource loading phase
            if (!renderReady) {
                long elapsed = now - initTime;
                if (elapsed < RENDER_DELAY_MS) {
                    return;
                }
                renderReady = true;
                LOGGER.info("Render delay complete ({} s), enabling rendering", elapsed / 1000);
            }

            // ----- Save ALL Minecraft GL state -----
            int oldVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            int oldTex = glGetInteger(GL_TEXTURE_BINDING_2D);
            int oldProg = glGetInteger(GL_CURRENT_PROGRAM);
            int oldArrayBuf = glGetInteger(GL_ARRAY_BUFFER_BINDING);
            int oldElemBuf = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
            boolean oldBlend = glIsEnabled(GL_BLEND);
            boolean oldDepth = glIsEnabled(GL_DEPTH_TEST);

            try {
                // Check if VAO/shader destroyed by Minecraft (menu open, loading, etc.)
                if (glResourcesValid && (!glIsVertexArray(vaoId) || shaderProgram == 0)) {
                    LOGGER.info("GL resources lost, recreating");
                    vaoInitialized = false;
                    shaderProgram = 0;
                    vaoId = 0;
                    vboId = 0;
                    iboId = 0;
                    glResourcesValid = false;
                }

                // Create VAO + shader on first run or after loss
                if (!vaoInitialized || shaderProgram == 0) {
                    long hwnd = D3D12Bridge.getWindowHandle();
                    if (hwnd != 0) {
                        D3D12Bridge.setWindow(hwnd);
                        LOGGER.info("WGPU window HWND set: 0x%016x", hwnd);
                    }

                    initVAO();
                    initShaderProgram();
                    LOGGER.info("OpenGL resources initialized: vao={}, shader={}", vaoId, shaderProgram);
                    glResourcesValid = true;
                }

                if (shaderProgram == 0) return;

                D3D12Bridge.syncWindowSize(width, height);

                ByteBuffer pixels = D3D12Bridge.renderFrame();
                if (pixels == null || !pixels.hasRemaining()) return;

                if (frameCount++ % 60 == 0) {
                    LOGGER.info("Rendering frame: {} bytes, VAO={} (frames={})", pixels.remaining(), vaoId, frameCount);
                }

                // Delete old texture to avoid name conflicts with Minecraft's shader loading
                // (Minecraft may reuse our texture name for other GL objects during loading)
                if (texId != 0) {
                    glDeleteTextures(texId);
                }

                // Create fresh texture every frame - avoids stale/corrupted texture state
                texId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

                // Draw fullscreen quad
                glUseProgram(shaderProgram);
                glUniform1i(texUniformLocation, 0);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glBindVertexArray(vaoId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0L);
            } catch (Throwable t) {
                LOGGER.warn("GL draw failed: {}", t.getMessage());
                glResourcesValid = false;
                vaoInitialized = false;
                shaderProgram = 0;
                texId = 0;
                vaoId = 0;
                vboId = 0;
                iboId = 0;
            } finally {
                // ----- Restore ALL Minecraft GL state -----
                glBindVertexArray(oldVao);
                glBindBuffer(GL_ARRAY_BUFFER, oldArrayBuf);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, oldElemBuf);
                glBindTexture(GL_TEXTURE_2D, oldTex);
                glUseProgram(oldProg);
                if (oldBlend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
                if (oldDepth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
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
