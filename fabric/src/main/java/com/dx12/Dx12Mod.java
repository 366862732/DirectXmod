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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

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

    private static long frameCount = 0;
    private static int pendingWidth = 0;
    private static int pendingHeight = 0;
    private static ByteBuffer pendingPixels = null;
    private static long lastRenderTime = 0;
    private static long renderStartTime = 0;
    private static long lastHwnd = 0;

    // Persistent shading resources (reused across frames)
    private static int vaoId = 0;
    private static int shaderProg = 0;
    private static int texUniformLoc = -1;
    private static boolean shaderValid = false;
    private static int texId = 0;
    private static boolean texAllocated = false;

    // Startup delay: skip GL operations during Minecraft's resource loading phase
    // (Shader Loader, texture loading, etc. run on render thread and conflict with our GL calls)
    private static boolean renderReady = false;
    private static long initTime = 0;
    private static long lastTickTime = 0;

    private static final long LOADING_GAP_MS = 2000; // Tick gap > 2s indicates a resource reload
    private static final long RENDER_DELAY_MS = 3000; // Wait 3s after reload before rendering

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");

        D3D12Bridge.init();
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        initTime = System.currentTimeMillis();

        // Tick callback: handle timing, reload detection, and Rust rendering
        // (No GL operations here — GL context may not be current in world)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long glfwWindow = GLFW.glfwGetCurrentContext();
            if (glfwWindow == 0) return;

            java.nio.IntBuffer wBuf = BufferUtils.createIntBuffer(1);
            java.nio.IntBuffer hBuf = BufferUtils.createIntBuffer(1);
            GLFW.glfwGetWindowSize(glfwWindow, wBuf, hBuf);
            int width = wBuf.get(0);
            int height = hBuf.get(0);
            if (width <= 0 || height <= 0) return;

            // Detect resource reloads
            long now = System.currentTimeMillis();
            if (lastTickTime > 0 && (now - lastTickTime) > LOADING_GAP_MS) {
                if (renderReady) {
                    LOGGER.info("Resource reload detected ({} s gap), delaying rendering",
                        (now - lastTickTime) / 1000);
                    renderReady = false;
                    initTime = now;
                    vaoId = 0;
                    shaderValid = false;
                    texAllocated = false;
                    pendingPixels = null;
                }
            }
            lastTickTime = now;

            if (!renderReady) {
                long elapsed = now - initTime;
                if (elapsed < RENDER_DELAY_MS) return;
                renderReady = true;
                renderStartTime = now;
                LOGGER.info("Render delay complete ({} s), starting render in 2s...", elapsed / 1000);
            }

            // 2-second buffer: wait after main delay before first renderFrame
            // Gives Minecraft's post-load GL state time to stabilize
            if (now - renderStartTime < 2000) return;

            // Throttle: only call wgpu renderFrame once per 100ms to avoid GPU contention
            if (lastRenderTime > 0 && (now - lastRenderTime) < 100) return;
            lastRenderTime = now;

            // Extract camera view-projection matrix from Minecraft
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.gameRenderer != null) {
                Camera camera = mc.gameRenderer.getCamera();
                if (camera != null) {
                    // Get camera position and rotation
                    var pos = camera.getPos();
                    float pitch = camera.getPitch();
                    float yaw = camera.getYaw();

                    // Build view matrix (look-at)
                    Matrix4f view = new Matrix4f();
                    view.rotateX((float) Math.toRadians(pitch));
                    view.rotateY((float) Math.toRadians(yaw + 180.0));
                    view.translate((float) -pos.x, (float) -pos.y, (float) -pos.z);

                    // Build projection matrix
                    float aspect = (float) width / (float) height;
                    Matrix4f proj = new Matrix4f();
                    proj.perspective((float) Math.toRadians(70.0), aspect, 0.05f, 1000.0f);

                    // MVP = projection * view
                    Matrix4f mvp = new Matrix4f(proj);
                    mvp.mul(view);

                    // Convert Matrix4f to float[16] column-major for JNI
                    float[] mvpArray = new float[16];
                    mvp.get(mvpArray);
                    D3D12Bridge.updateCamera(mvpArray);
                }
            }

            // Call Rust renderer — only update HWND when it changes
            long hwnd = D3D12Bridge.getWindowHandle();
            if (hwnd != 0 && hwnd != lastHwnd) {
                D3D12Bridge.setWindow(hwnd);
                lastHwnd = hwnd;
            }
            D3D12Bridge.syncWindowSize(width, height);

            ByteBuffer pixels = D3D12Bridge.renderFrame();
            if (pixels == null || !pixels.hasRemaining()) return;

            // Store frame data for HUD callback
            pendingPixels = pixels;
            pendingWidth = width;
            pendingHeight = height;
        });

        // HUD callback: GL drawing (OpenGL context IS current during HUD rendering)
        // Minecraft uses RenderSystem which resets GL state before each draw call,
        // so we DON'T need to save/restore state — just set ours, draw, unbind.
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            // Skip GL drawing when any GUI screen is open (pause menu, inventory, etc.)
            // Prevents nvoglv64 crash from GL state conflict with screen rendering
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.currentScreen != null) return;

            if (pendingPixels == null || !pendingPixels.hasRemaining()) return;

            int width = pendingWidth;
            int height = pendingHeight;
            ByteBuffer pixels = pendingPixels;
            pendingPixels = null; 

            // Safety: clamp upload size to actual buffer capacity
            // Prevents nvoglv64 ACCESS_VIOLATION when buffer doesn't match expected size
            int expectedBytes = width * height * 4;
            int bufferBytes = pixels.remaining();
            if (bufferBytes < expectedBytes) {
                if (width * 4 > 0) {
                    height = bufferBytes / (width * 4);
                }
                if (height <= 0) return;
            }

            try {
                if (frameCount++ % 60 == 0) {
                    LOGGER.info("Rendering frame: {} bytes (frame={})", bufferBytes, frameCount);
                }

                // Texture: allocate once, update every frame
                if (!texAllocated) {
                    if (texId != 0) glDeleteTextures(texId);
                    texId = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, texId);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                    texAllocated = true;
                }
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

                // Shader: persistent
                if (!shaderValid || shaderProg == 0) {
                    if (shaderProg != 0) glDeleteProgram(shaderProg);
                    shaderProg = createShaderProgram();
                    texUniformLoc = glGetUniformLocation(shaderProg, "uTexture");
                    shaderValid = (shaderProg != 0);
                }

                // VAO: persistent
                if (vaoId == 0 || !glIsVertexArray(vaoId)) {
                    if (vaoId != 0) glDeleteVertexArrays(vaoId);
                    vaoId = createVAO();
                }

                // Draw
                glUseProgram(shaderProg);
                glUniform1i(texUniformLoc, 0);
                glBindVertexArray(vaoId);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0L);

                // Unbind (Minecraft's RenderSystem will set its own state next)
                glBindVertexArray(0);
                glBindTexture(GL_TEXTURE_2D, 0);
                glUseProgram(0);

            } catch (Throwable t) {
                LOGGER.warn("GL draw failed: {}", t.getMessage());
                vaoId = 0;
                shaderValid = false;
                texAllocated = false;
            }
        });

        LOGGER.info("GL4DX12 Mod initialized!");
    }

    // Create a fullscreen quad VAO (vertex + index buffers included)
    // Returns VAO id. Caller must glDeleteVertexArrays it when done.
    private static int createVAO() {
        float[] data = {
             0f,  0f,  0f,  0f, 0f,
             1f,  0f,  0f,  1f, 0f,
             0f,  1f,  0f,  0f, 1f,
             1f,  1f,  0f,  1f, 1f,
        };
        byte[] idx = { 0, 1, 2, 2, 1, 3 };

        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(data.length);
        vertexBuffer.put(data).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        int ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        ByteBuffer ibBuf = BufferUtils.createByteBuffer(idx.length);
        ibBuf.put(idx).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ibBuf, GL_STATIC_DRAW);

        glBindVertexArray(0);
        return vao;
    }

    // Compile and link shader program. Returns program id.
    // Caller must glDeleteProgram it when done.
    private static int createShaderProgram() {
        String vertSrc =
            "#version 330 core\n" +
            "layout(location = 0) in vec3 aPos;\n" +
            "layout(location = 1) in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "void main(){ gl_Position = vec4(aPos, 1.0); vTexCoord = aTexCoord; }\n";

        String fragSrc =
            "#version 330 core\n" +
            "in vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "out vec4 FragColor;\n" +
            "void main(){ FragColor = texture(uTexture, vTexCoord); }\n";

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vertSrc);
        glCompileShader(vs);

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fragSrc);
        glCompileShader(fs);

        int prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glLinkProgram(prog);

        glDeleteShader(vs);
        glDeleteShader(fs);
        return prog;
    }
}
