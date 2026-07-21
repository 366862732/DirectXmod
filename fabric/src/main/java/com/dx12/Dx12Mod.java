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
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

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

    // Unified overlay GL resources (texture, PBO, mesh, shader)
    private static GlOverlayResources overlayResources = new GlOverlayResources();
    private static boolean releasePending = false;

    private static void resetOverlayResources(String reason, boolean dropPendingFrame) {
        LOGGER.warn("Reset overlay GL resources: {}", reason);
        if (dropPendingFrame) {
            pendingPixels = null;
            pendingWidth = 0;
            pendingHeight = 0;
        }
        releasePending = true;
    }

    private static void flushPendingRelease() {
        if (!releasePending) return;
        overlayResources.releaseAll();
        releasePending = false;
    }

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
                    resetOverlayResources("resource reload detected", true);
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
                    proj.perspective((float) Math.toRadians(70.0), aspect, 0.05f, 1000.0f, true);

                    Matrix4f mvp = new Matrix4f(proj);
                    mvp.mul(view);

                    float[] mvpArray = new float[16];
                    mvp.get(mvpArray);
                    D3D12Bridge.updateCamera(mvpArray);

                    // Pass camera world position to offset test geometry near the player.
                    D3D12Bridge.updateCameraPos(
                        (float) pos.x, (float) pos.y, (float) pos.z);

                    // Extract fog color from the level.
                    // Clamp to a minimum brightness so underground fog isn't fully black.
                    try {
                        Vec3 skyColor;
                        try {
                            Vec3 cameraPos = pos;
                            float partialTick = 1.0f;
                            var getSkyColorMethod = net.minecraft.world.level.Level.class
                                .getMethod("getSkyColor", net.minecraft.world.phys.Vec3.class, float.class);
                            skyColor = (Vec3) getSkyColorMethod.invoke(mc.level, cameraPos, partialTick);
                        } catch (Exception e) {
                            skyColor = new Vec3(0.53, 0.81, 0.92);
                        }
                        float fr = Math.max((float) skyColor.x, 0.20f);
                        float fg = Math.max((float) skyColor.y, 0.25f);
                        float fb = Math.max((float) skyColor.z, 0.35f);
                        LOGGER.info("Fog raw=({}) final=({})",
                            String.format("%.3f,%.3f,%.3f", skyColor.x, skyColor.y, skyColor.z),
                            String.format("%.3f,%.3f,%.3f", fr, fg, fb));
                        float fogDensity = 0.003f;
                        if (mc.level.isRaining()) fogDensity = 0.008f;
                        if (mc.level.isThundering()) fogDensity = 0.015f;
                        D3D12Bridge.nativeUpdateFog(fr, fg, fb, fogDensity);
                    } catch (Throwable fogEx) {
                        LOGGER.info("Fog fallback (exception): {}", fogEx.getMessage());
                        D3D12Bridge.nativeUpdateFog(0.53f, 0.81f, 0.92f, 0.003f);
                    }

                    // ─── Entity extraction ──────────────────────────
                    try {
                        // Use reflection to access entities (API differs across MC versions)
                        var entityList = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
                        try {
                            // Try Level.getEntities() + LevelEntityGetter via reflection
                            var getEntitiesMethod = net.minecraft.world.level.Level.class
                                .getDeclaredMethod("getEntities");
                            getEntitiesMethod.setAccessible(true);
                            var entityGetter = getEntitiesMethod.invoke(mc.level);
                            var getAllMethod = entityGetter.getClass().getMethod("getAll", java.util.List.class);
                            getAllMethod.invoke(entityGetter, entityList);
                        } catch (Exception ignore) {
                            // Entity iteration failed, skip
                        }
                        int count = entityList.size();
                        if (count > 0 && count <= 256) {
                            float[] entityData = new float[count * 9];
                            for (int i = 0; i < count; i++) {
                                var e = entityList.get(i);
                                var ePos = e.position();
                                var bb = e.getBoundingBox();
                                float w = (float) (bb.maxX - bb.minX);
                                float h = (float) (bb.maxY - bb.minY);
                                float d = (float) (bb.maxZ - bb.minZ);
                                int nameHash = e.getType().getDescription().getString().hashCode();
                                float er = ((nameHash >> 0) & 0xFF) / 255.0f;
                                float eg = ((nameHash >> 8) & 0xFF) / 255.0f;
                                float eb = ((nameHash >> 16) & 0xFF) / 255.0f;
                                int off = i * 9;
                                entityData[off]     = (float) ePos.x;
                                entityData[off + 1] = (float) ePos.y;
                                entityData[off + 2] = (float) ePos.z;
                                entityData[off + 3] = w;
                                entityData[off + 4] = h;
                                entityData[off + 5] = d;
                                entityData[off + 6] = er;
                                entityData[off + 7] = eg;
                                entityData[off + 8] = eb;
                            }
                            D3D12Bridge.nativeSetEntities(entityData);
                        } else {
                            D3D12Bridge.nativeSetEntities(new float[0]);
                        }
                    } catch (Throwable entityEx) {
                        D3D12Bridge.nativeSetEntities(new float[0]);
                    }

                    // ─── Particle extraction ────────────────────────
                    try {
                        var particleEngine = mc.particleEngine;
                        if (particleEngine != null) {
                            var particleList = new java.util.ArrayList<net.minecraft.client.particle.Particle>();
                            try {
                                var particlesField = net.minecraft.client.particle.ParticleEngine.class
                                    .getDeclaredField("particles");
                                particlesField.setAccessible(true);
                                @SuppressWarnings("unchecked")
                                var particleMap = (java.util.Map<?, ?>) particlesField.get(particleEngine);
                                for (var entry : particleMap.entrySet()) {
                                    var set = (java.util.Set<?>) entry.getValue();
                                    for (var p : set) {
                                        particleList.add((net.minecraft.client.particle.Particle) p);
                                    }
                                }
                            } catch (Exception ignored) {
                            }

                            if (!particleList.isEmpty() && particleList.size() <= 2048) {
                                float[] particleData = new float[particleList.size() * 8];
                                int idx = 0;
                                for (var p : particleList) {
                                    // Read particle fields via reflection (protected in MC 26.1.2)
                                    float px = 0f, py = 0f, pz = 0f;
                                    try {
                                        var xField = net.minecraft.client.particle.Particle.class
                                            .getDeclaredField("x");
                                        xField.setAccessible(true);
                                        px = xField.getFloat(p);
                                        var yField = net.minecraft.client.particle.Particle.class
                                            .getDeclaredField("y");
                                        yField.setAccessible(true);
                                        py = yField.getFloat(p);
                                        var zField = net.minecraft.client.particle.Particle.class
                                            .getDeclaredField("z");
                                        zField.setAccessible(true);
                                        pz = zField.getFloat(p);
                                    } catch (Exception ignored) {
                                    }
                                    float size = 4.0f;
                                    float pr = 1.0f, pg = 1.0f, pb = 1.0f, pa = 0.8f;
                                    int base = idx * 8;
                                    particleData[base]     = px;
                                    particleData[base + 1] = py;
                                    particleData[base + 2] = pz;
                                    particleData[base + 3] = size;
                                    particleData[base + 4] = pr;
                                    particleData[base + 5] = pg;
                                    particleData[base + 6] = pb;
                                    particleData[base + 7] = pa;
                                    idx++;
                                }
                                if (idx > 0) {
                                    float[] trimmed = new float[idx * 8];
                                    System.arraycopy(particleData, 0, trimmed, 0, idx * 8);
                                    D3D12Bridge.nativeSetParticles(trimmed);
                                } else {
                                    D3D12Bridge.nativeSetParticles(new float[0]);
                                }
                            } else {
                                D3D12Bridge.nativeSetParticles(new float[0]);
                            }
                        } else {
                            D3D12Bridge.nativeSetParticles(new float[0]);
                        }
                    } catch (Throwable particleEx) {
                        D3D12Bridge.nativeSetParticles(new float[0]);
                    }
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

        // Flush any pending resource releases (from reload/error recovery)
        flushPendingRelease();

        try {
            if (frameCount++ % 60 == 0) {
                LOGGER.info("Rendering frame: {} bytes (frame={})", bufferBytes, frameCount);
            }

            // Ensure all rendering resources are ready
            overlayResources.ensureReady(width, height);

            // Upload pixel data via PBO
            overlayResources.upload(pixels);

            // Draw with GL state save/restore
            withGlStateRestored(width, height, overlayResources::bindAndDraw);

        } catch (Throwable t) {
            resetOverlayResources("GL draw failed: " + t.getMessage(), false);
        }
    }

    // Save/restore GL state around a draw action (glPushAttrib deprecated in core profile)
    private static void withGlStateRestored(int width, int height, Runnable drawAction) {
        boolean scissorWasOn = glIsEnabled(GL_SCISSOR_TEST);
        boolean depthWasOn = glIsEnabled(GL_DEPTH_TEST);
        boolean blendWasOn = glIsEnabled(GL_BLEND);
        int[] oldViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, oldViewport);

        try {
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
            glViewport(0, 0, width, height);
            drawAction.run();
        } finally {
            if (scissorWasOn) glEnable(GL_SCISSOR_TEST);
            if (depthWasOn) glEnable(GL_DEPTH_TEST);
            if (blendWasOn) glEnable(GL_BLEND);
            glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
        }
    }

    // ---- Overlay GL resource lifecycle ---------------------------------

    /**
     * Owns all GL objects for offscreen overlay rendering:
     * texture (RGBA8), PBO (pixel upload), VAO+VBO+IBO (fullscreen quad mesh),
     * and shader program (passthrough texture blit).
     * Provides ensure/create, upload, draw, and full release lifecycle.
     */
    private static final class GlOverlayResources {
        int textureId;
        int pboId;
        int vaoId;
        int vboId;
        int iboId;
        int programId;
        int textureUniformLoc = -1;
        int width;
        int height;

        void ensureReady(int w, int h) {
            ensureTexture(w, h);
            ensureShader();
            ensureMesh();
        }

        void ensureTexture(int w, int h) {
            if (textureId != 0 && width == w && height == h) return;
            if (textureId != 0) glDeleteTextures(textureId);
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            width = w;
            height = h;
        }

        void ensureShader() {
            if (programId != 0) return;
            programId = createShaderProgram();
            textureUniformLoc = glGetUniformLocation(programId, "uTexture");
        }

        void ensureMesh() {
            if (vaoId != 0) return;
            vaoId = glGenVertexArrays();
            vboId = glGenBuffers();
            iboId = glGenBuffers();
            glBindVertexArray(vaoId);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, buildVertexBuffer(), GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, buildIndexBuffer(), GL_STATIC_DRAW);
            glBindVertexArray(0);
        }

        void upload(ByteBuffer pixels) {
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
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        }

        void bindAndDraw() {
            glUseProgram(programId);
            glUniform1i(textureUniformLoc, 0);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glBindVertexArray(vaoId);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0L);
            glBindVertexArray(0);
            glUseProgram(0);
        }

        void releaseAll() {
            if (textureId != 0) { glDeleteTextures(textureId); textureId = 0; }
            if (pboId != 0) { glDeleteBuffers(pboId); pboId = 0; }
            if (iboId != 0) { glDeleteBuffers(iboId); iboId = 0; }
            if (vboId != 0) { glDeleteBuffers(vboId); vboId = 0; }
            if (vaoId != 0) { glDeleteVertexArrays(vaoId); vaoId = 0; }
            if (programId != 0) { glDeleteProgram(programId); programId = 0; }
            textureUniformLoc = -1;
            width = 0;
            height = 0;
        }

        private static FloatBuffer buildVertexBuffer() {
            // Full-screen quad in NDC (-1 to 1)
            float[] data = {
                -1f, -1f,  0f,  0f, 0f,
                 1f, -1f,  0f,  1f, 0f,
                -1f,  1f,  0f,  0f, 1f,
                 1f,  1f,  0f,  1f, 1f,
            };
            FloatBuffer buf = BufferUtils.createFloatBuffer(data.length);
            buf.put(data).flip();
            return buf;
        }

        private static ByteBuffer buildIndexBuffer() {
            byte[] idx = { 0, 1, 2, 2, 1, 3 };
            ByteBuffer buf = BufferUtils.createByteBuffer(idx.length);
            buf.put(idx).flip();
            return buf;
        }

        // Compile and link passthrough texture shader program
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
}
