package com.dx12;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glIsVertexArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

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
    private static boolean firstTickLogged = false;
    private static int pendingWidth = 0;
    private static int pendingHeight = 0;
    private static ByteBuffer pendingPixels = null;
    private static long lastRenderTime = 0;
    private static long renderStartTime = 0;

    // Persistent shading resources (reused across frames)
    private static int vaoId = 0;
    private static int shaderProg = 0;
    private static int texUniformLoc = -1;
    private static boolean shaderValid = false;
    private static int texId = 0;
    private static boolean texAllocated = false;
    private static int texWidth = 0;
    private static int texHeight = 0;

    // PBO for safe texture upload — bypasses NVIDIA driver's client-memory DMA
    private static int pboId = 0;

    // Polling: track renderer init status to log transitions
    private static String lastRendererStatus = "not_started";

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
                    firstTickLogged = false;
                    initTime = now;
                    vaoId = 0;
                    shaderValid = false;
                    texAllocated = false;
                    texWidth = 0;
                    texHeight = 0;
                    pboId = 0;
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

            // Diagnostic: log when we actually start rendering
            if (!firstTickLogged) {
                LOGGER.info("First render tick: width={}, height={}", width, height);
                firstTickLogged = true;
            }

            // No throttle: render_frame() is fully async (non-blocking submit + triple-buffer readback).
            // GPU pipeline is kept busy without blocking the render thread.
            lastRenderTime = now;

            // Extract camera view-projection matrix from Minecraft.
            boolean inWorld = false;
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.level != null) {
                    inWorld = true; 
                    var player = mc.player;
                    var pos = player.getEyePosition();
                    float pitch = player.getXRot();
                    float yaw = player.getYRot();

                    Matrix4f view = new Matrix4f();
                    view.rotateX((float) Math.toRadians(pitch));
                    view.rotateY((float) Math.toRadians(yaw + 180.0));
                    view.translate((float) -pos.x, (float) -pos.y, (float) -pos.z);

                    float aspect = (float) width / (float) height;
                    Matrix4f proj = new Matrix4f();
                    proj.perspective((float) Math.toRadians(70.0), aspect, 0.05f, 1000.0f);

                    Matrix4f mvp = new Matrix4f(proj);
                    mvp.mul(view);

                    float[] mvpArray = new float[16];
                    mvp.get(mvpArray);
                    D3D12Bridge.updateCamera(mvpArray);

                    // Pass camera world position to offset test geometry near the player.
                    D3D12Bridge.updateCameraPos(
                        (float) pos.x, (float) pos.y, (float) pos.z);
                }
            } catch (Throwable t) {
                LOGGER.error("Camera extraction failed: {}", t.getMessage());
                return;
            }

            // Sync window size to Rust (safe, no GL/HWND access)
            try {
            D3D12Bridge.syncWindowSize(width, height);

            // Poll renderer init status, log state transitions
            String status = D3D12Bridge.getStatus();
            if (!status.equals(lastRendererStatus)) {
                LOGGER.info("Renderer status: {} → {}", lastRendererStatus, status);
                lastRendererStatus = status;
            }

            // Surface mode: only active when in-world (title screen uses offscreen mode).
            // MC's GL render is cancelled at GameRendererMixin HEAD.
            // Camera was updated above. renderFrame() is called later from
            // MinecraftMixin.runTick TAIL — D3D12 presents directly to window.
            // Skip renderFrame here to avoid duplicate rendering.
            if (D3D12Bridge.hasSurface() && inWorld) {
                return;
            }

            // Offscreen mode: render to texture, read back pixels for GL overlay
            ByteBuffer pixels = D3D12Bridge.renderFrame();
            if (pixels == null || !pixels.hasRemaining()) {
                if (frameCount == 0) LOGGER.warn("renderFrame returned null/empty (status={})", status);
                return;
            }

            // Store frame data for onPostRender (GL overlay in offscreen mode)
            pendingPixels = pixels;
            pendingWidth = width;
            pendingHeight = height;
            } catch (Throwable t) {
                LOGGER.error("Render tick failed: {}", t.getMessage(), t);
                return;
            }
        });
        LOGGER.info("GL4DX12 Mod initialized!");
    }

    // GL drawing: called by GameRendererMixin at TAIL of render() (offscreen mode only).
    // At this point ALL Minecraft rendering (world, entities, HUD) is complete,
    // our full-screen quad will be the last thing drawn this frame.
    // glPushAttrib/glPopAttrib removed — they are deprecated and crash on core profile.
    public static void onPostRender() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.screen != null) return;  // skip menus

        if (pendingPixels == null || !pendingPixels.hasRemaining()) return;

        int width = pendingWidth;
        int height = pendingHeight;
        ByteBuffer pixels = pendingPixels;
        pendingPixels = null;

        // Safety: skip frames where pixel buffer doesn't match expected dimensions.
        // This happens during window resize — old-resolution frames are served
        // from the render thread's cache until the first new-resolution frame arrives.
        int expectedBytes = width * height * 4;
        int bufferBytes = pixels.remaining();
        if (bufferBytes != expectedBytes) {
            if (frameCount < 10 || frameCount % 60 == 0) {
                LOGGER.info("Pixel buffer size mismatch: got={} expected={} ({}x{}) — skipping",
                    bufferBytes, expectedBytes, width, height);
            }
            return;
        }

        try {
            if (frameCount++ % 60 == 0) {
                LOGGER.info("Rendering frame: {} bytes (frame={})", bufferBytes, frameCount);
            }

            // Texture: allocate once, reallocate on resize
            if (!texAllocated || texWidth != width || texHeight != height) {
                if (texId != 0) glDeleteTextures(texId);
                texId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                texAllocated = true;
                texWidth = width;
                texHeight = height;
            }

            // Upload pixels via PBO
            int pboBytes = width * height * 4;
            if (pboId == 0) {
                pboId = glGenBuffers();
            }
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
            glBufferData(GL_PIXEL_UNPACK_BUFFER, (long) pboBytes, GL_STREAM_DRAW);
            pixels.rewind();
            ByteBuffer mapped = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY, (long) pboBytes, null);
            if (mapped != null) {
                mapped.put(pixels);
                glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
            }
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

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

            // Manually save/restore GL state (glPushAttrib deprecated in core profile)
            boolean scissorWasOn = glIsEnabled(GL_SCISSOR_TEST);
            boolean depthWasOn = glIsEnabled(GL_DEPTH_TEST);
            boolean blendWasOn = glIsEnabled(GL_BLEND);
            int[] oldViewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, oldViewport);

            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
            glViewport(0, 0, width, height);

            glUseProgram(shaderProg);
            glUniform1i(texUniformLoc, 0);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texId);
            glBindVertexArray(vaoId);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0L);

            glBindVertexArray(0);
            glUseProgram(0);

            // Restore GL state
            if (scissorWasOn) glEnable(GL_SCISSOR_TEST);
            if (depthWasOn) glEnable(GL_DEPTH_TEST);
            if (blendWasOn) glEnable(GL_BLEND);
            glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);

        } catch (Throwable t) {
            LOGGER.warn("GL draw failed: {}", t.getMessage());
            vaoId = 0;
            shaderValid = false;
            texAllocated = false;
            pboId = 0;
        }
    }

    // Create a fullscreen quad VAO (vertex + index buffers included)
    // Returns VAO id. Caller must glDeleteVertexArrays it when done.
    private static int createVAO() {
        // Full-screen quad in NDC (-1 to 1)
        float[] data = {
            -1f, -1f,  0f,  0f, 0f,
             1f, -1f,  0f,  1f, 0f,
            -1f,  1f,  0f,  0f, 1f,
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
