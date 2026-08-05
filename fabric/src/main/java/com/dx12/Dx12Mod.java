package com.dx12;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
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
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
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
    private static int getSkyColorFailCount = 0;
    private static long lastParticleLogMs = 0;

    // P1c fix #3: last keep radius (blocks) sent to the Rust renderer.
    private static float lastKeepRadiusBlocks = 288.0f;

    // P1c fix #4: cached render-distance resolution. getRenderDistanceBlocks()
    // is called every tick; reflection field/method scans are slow, so the
    // result is cached for 10 s (view distance can change at runtime).
    private static boolean renderDistanceResolved = false;
    private static float resolvedRenderDistanceBlocks = 288.0f;
    private static long lastRenderDistanceResolveMs = 0;

    /**
     * Effective render distance in blocks, resolved fully reflectively so the
     * mod compiles against any MC mapping layout. The Minecraft.options field
     * is looked up with getDeclaredField + setAccessible (getField only finds
     * public fields and silently fails on private layouts, which made the
     * keep radius fall back to 288 blocks — smaller than a 32-chunk view
     * distance and the renderer then evicted the visible horizon). Tries, in
     * order:
     *   Options.getEffectiveRenderDistance()        (1.20.x+)
     *   Options.renderDistance()                    (OptionInstance<Integer>, 1.20.5+)
     *   Minecraft.getEffectiveRenderDistance()      (older)
     * Failures are logged once so a wrong radius is diagnosable instead of
     * silently defaulting.
     */
    private static float getRenderDistanceBlocks(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (renderDistanceResolved && now - lastRenderDistanceResolveMs < 10_000) {
            return resolvedRenderDistanceBlocks;
        }
        float blocks = resolveRenderDistanceBlocks(mc);
        renderDistanceResolved = true;
        lastRenderDistanceResolveMs = now;
        resolvedRenderDistanceBlocks = blocks;
        return blocks;
    }

    private static float resolveRenderDistanceBlocks(Minecraft mc) {
        Object options = findOptionsField(mc);
        if (options != null) {
            // 1) Options.getEffectiveRenderDistance() → int chunks (1.20.x+)
            Float rd = invokeNumberMethod(options, "getEffectiveRenderDistance");
            if (rd != null) {
                LOGGER.info("[dx12-wm] Render distance (Options.getEffectiveRenderDistance): {} chunks", rd.intValue());
                return rd * 16.0f + 32.0f;
            }
            // 2) Options.renderDistance() → OptionInstance<Integer> (a Supplier)
            Object rdObj = invokeMethod(options, "renderDistance");
            if (rdObj != null) {
                if (rdObj instanceof Number n) {
                    LOGGER.info("[dx12-wm] Render distance (Options.renderDistance number): {} chunks", n.intValue());
                    return n.intValue() * 16.0f + 32.0f;
                }
                if (rdObj instanceof java.util.function.Supplier<?> s) {
                    Object val = s.get();
                    if (val instanceof Number n) {
                        LOGGER.info("[dx12-wm] Render distance (Options.renderDistance supplier): {} chunks", n.intValue());
                        return n.intValue() * 16.0f + 32.0f;
                    }
                }
            }
        }
        // 3) Minecraft.getEffectiveRenderDistance() → int chunks (older)
        Float rd = invokeNumberMethod(mc, "getEffectiveRenderDistance");
        if (rd != null) {
            LOGGER.info("[dx12-wm] Render distance (Minecraft.getEffectiveRenderDistance): {} chunks", rd.intValue());
            return rd * 16.0f + 32.0f;
        }
        LOGGER.warn("[dx12-wm] Render distance resolution failed; keep radius defaults to 288 blocks");
        return 288.0f;
    }

    /** Resolve the Minecraft.options slot (public or private, any layout). */
    private static Object findOptionsField(Minecraft mc) {
        try {
            Field f = Minecraft.class.getDeclaredField("options");
            f.setAccessible(true);
            return f.get(mc);
        } catch (Exception | LinkageError e) {
            LOGGER.warn("[dx12-wm] options field lookup failed: {}", e.toString());
        }
        // Fallback: scan declared fields for a net.minecraft.client.Options slot
        // (works even when the field is renamed in the runtime mapping).
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType().getName().endsWith("client.Options")) {
                    f.setAccessible(true);
                    return f.get(mc);
                }
            }
        } catch (Exception | LinkageError e) {
            LOGGER.warn("[dx12-wm] options field scan failed: {}", e.toString());
        }
        return null;
    }

    /** Invoke a no-arg method returning a Number (e.g. getEffectiveRenderDistance). */
    private static Float invokeNumberMethod(Object target, String name) {
        Object val = invokeMethod(target, name);
        if (val instanceof Number n) return (float) n.intValue();
        return null;
    }

    /** Invoke a no-arg method and return its raw result (null on failure). */
    private static Object invokeMethod(Object target, String name) {
        try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (Exception | LinkageError ignored) {
            return null;
        }
    }

    // Lightmap update throttling
    private static int lightmapTickCount = 0;
    private static final int LIGHTMAP_UPDATE_INTERVAL_TICKS = 5;

    // Phase 11d: cache last uploaded lightmap pixels.
    // Skip the JNI upload + GPU write_texture when content is unchanged.
    private static byte[] lastLightmapPixels = null;

    // Phase 11c: cached reflection handles for entity/particle extraction.
    // getDeclaredMethod/Field scans the class method table every call, which is
    // slow in a per-tick loop — resolve once and reuse.
    private static Method GET_ENTITIES_METHOD = null;
    private static Method GET_ALL_METHOD = null;
    private static Field PARTICLE_MAP_FIELD = null;
    private static Field PARTICLE_X_FIELD = null;
    private static Field PARTICLE_Y_FIELD = null;
    private static Field PARTICLE_Z_FIELD = null;
    private static boolean entityReflectionInit = false;
    private static boolean particleReflectionInit = false;

    /** Resolve Level.getEntities() + LevelEntityGetter.getAll(List) once. */
    private static void initEntityReflection() {
        if (entityReflectionInit) return;
        entityReflectionInit = true;
        try {
            GET_ENTITIES_METHOD = net.minecraft.world.level.Level.class.getDeclaredMethod("getEntities");
            GET_ENTITIES_METHOD.setAccessible(true);
        } catch (Throwable ignored) {
            GET_ENTITIES_METHOD = null;
        }
    }

    /** Resolve ParticleEngine.particles + Particle.x/y/z fields once. */
    private static void initParticleReflection() {
        if (particleReflectionInit) return;
        particleReflectionInit = true;
        try {
            PARTICLE_MAP_FIELD = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles");
            PARTICLE_MAP_FIELD.setAccessible(true);
            PARTICLE_X_FIELD = net.minecraft.client.particle.Particle.class.getDeclaredField("x");
            PARTICLE_X_FIELD.setAccessible(true);
            PARTICLE_Y_FIELD = net.minecraft.client.particle.Particle.class.getDeclaredField("y");
            PARTICLE_Y_FIELD.setAccessible(true);
            PARTICLE_Z_FIELD = net.minecraft.client.particle.Particle.class.getDeclaredField("z");
            PARTICLE_Z_FIELD.setAccessible(true);
        } catch (Throwable ignored) {
            PARTICLE_MAP_FIELD = null;
            PARTICLE_X_FIELD = null;
            PARTICLE_Y_FIELD = null;
            PARTICLE_Z_FIELD = null;
        }
    }

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

                    // Phase 11i: per-frame camera extraction moved to
                    // updateCameraFromRender() (called from GameRendererMixin.render
                    // TAIL at full frame rate). Camera here was 20 Hz (one per tick)
                    // while D3D12 now presents every frame — the mismatch caused
                    // visible stutter when moving.

                    // Extract fog color from the level for fog and sky rendering.
                    // MC 26.1.2 uses EnvironmentAttributes.SKY_COLOR (RGB 0xRRGGBB)
                    // via Level.environmentAttributes().getValue(attribute, pos).
                    // Older versions used Level.getSkyColor(Vec3, float) — kept as
                    // a reflective fallback for cross-version compatibility.
                    var player = mc.player;
                    Vec3 pos = player.getEyePosition();
                    Vec3 skyColor = null;
                    try {
                        // Direct call: compiled with official mappings and the runtime
                        // uses the same names, so no reflection is needed (reflection
                        // strings are also NOT remapped by loom, making them fragile).
                        var envSystem = mc.level.environmentAttributes();
                        Integer argb = envSystem.getValue(
                            net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR,
                            pos);
                        if (argb != null) {
                            // SKY_COLOR is RGB (0xRRGGBB, no alpha)
                            skyColor = new Vec3(
                                ((argb >> 16) & 0xFF) / 255.0,
                                ((argb >> 8) & 0xFF) / 255.0,
                                (argb & 0xFF) / 255.0);
                        }
                    } catch (Exception | LinkageError e) {
                        // Older versions: Level.getSkyColor(Vec3, float)
                        try {
                            var method = net.minecraft.world.level.Level.class
                                .getDeclaredMethod("getSkyColor", net.minecraft.world.phys.Vec3.class, float.class);
                            method.setAccessible(true);
                            skyColor = (Vec3) method.invoke(mc.level, pos, 1.0f);
                        } catch (Exception e2) {
                            if (getSkyColorFailCount % 60 == 0) {
                                LOGGER.info("getSkyColor fallback ({}): {}",
                                    getSkyColorFailCount, e2.getMessage());
                            }
                            getSkyColorFailCount++;
                        }
                    }
                    if (skyColor == null) {
                        skyColor = new Vec3(0.53, 0.81, 0.92);
                    }
                    float fr, fg, fb;
                    {
                        fr = Math.max((float) skyColor.x, 0.20f);
                        fg = Math.max((float) skyColor.y, 0.25f);
                        fb = Math.max((float) skyColor.z, 0.35f);
                        // P1d: detect the camera being underwater. Vanilla applies
                        // a dense blue water fog (#3F76E4) that fully obscures the
                        // sky dome and cloud layer from below the surface.
                        boolean underwater = false;
                        try {
                            underwater = mc.level.getBlockState(BlockPos.containing(pos))
                                .getFluidState().is(FluidTags.WATER);
                        } catch (Exception | LinkageError e) {
                            // Missing mappings — stay above water
                        }
                        float fogDensity = 0.001f;
                        if (underwater) {
                            fr = 0.247f;
                            fg = 0.463f;
                            fb = 0.894f;
                            fogDensity = 0.1f;
                        } else {
                            if (mc.level.isRaining()) fogDensity = 0.003f;
                            if (mc.level.isThundering()) fogDensity = 0.006f;
                        }
                        D3D12Bridge.nativeUpdateFog(fr, fg, fb, fogDensity);
                        D3D12Bridge.updateUnderwater(underwater);
                    }
                    // P1c fix #3: sync the fast-travel eviction keep radius to
                    // the effective render distance so the renderer never evicts
                    // terrain that is actually in view (the old fixed 288-block
                    // ring was smaller than a 32-chunk view distance and
                    // stripped the visible horizon).
                    {
                        float rdBlocks = getRenderDistanceBlocks(mc);
                        if (Math.abs(rdBlocks - lastKeepRadiusBlocks) > 0.5f) {
                            lastKeepRadiusBlocks = rdBlocks;
                            D3D12Bridge.updateKeepRadius(rdBlocks);
                        }
                    }
                    // ─── Sky dome update ────────────────────────────
                    // Push the MC-derived sky color to the Rust sky dome so the
                    // sky gradient follows the world (time of day / weather).
                    // Zenith is brightened from the fog color; horizon matches
                    // the fog color exactly (same as vanilla gradient).
                    {
                        float tr = Math.min(fr * 1.15f + 0.05f, 1.0f);
                        float tg = Math.min(fg * 1.15f + 0.05f, 1.0f);
                        float tb = Math.min(fb * 1.25f + 0.08f, 1.0f);
                        // P1a: real celestial data from MC's environment attribute
                        // system (same source vanilla SkyRenderer uses). Angles are
                        // in degrees; the Rust sky shader expects radians.
                        float sunAngle = 0.0f, moonAngle = (float) Math.PI;
                        float starAngle = 0.0f, starBrightness = 0.0f;
                        float moonPhase = 0.0f, rainBrightness = 1.0f;
                        try {
                            var env = mc.level.environmentAttributes();
                            Float sunDeg = env.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, pos);
                            Float moonDeg = env.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.MOON_ANGLE, pos);
                            Float starDeg = env.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.STAR_ANGLE, pos);
                            Float stars = env.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.STAR_BRIGHTNESS, pos);
                            var phase = env.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, pos);
                            if (sunDeg != null) sunAngle = (float) Math.toRadians(sunDeg);
                            if (moonDeg != null) moonAngle = (float) Math.toRadians(moonDeg);
                            if (starDeg != null) starAngle = (float) Math.toRadians(starDeg);
                            if (stars != null) starBrightness = Math.max(stars, 0.0f);
                            if (phase != null) moonPhase = phase.index();
                            rainBrightness = 1.0f - mc.level.getRainLevel(1.0f);
                        } catch (Exception | LinkageError celestialEx) {
                            // API mismatch → keep defaults (noon sun, no stars)
                        }
                        D3D12Bridge.updateSky(tr, tg, tb, fr, fg, fb,
                            sunAngle, moonAngle, starAngle, starBrightness,
                            moonPhase, rainBrightness);
                    }

                    // ─── Cloud layer update (P1b) ───────────────────
                    // Vanilla CloudRenderer: cloud color from CLOUD_COLOR (ARGB —
                    // alpha 0 disables the layer), height from CLOUD_HEIGHT, and
                    // the wind scroll offset = (gameTime % 102400 + partialTicks)
                    // * 0.03 blocks (256px texture * 12 blocks/cell * 400 ticks,
                    // 0.6 blocks/second wind).
                    {
                        float cloudR = 1.0f, cloudG = 1.0f, cloudB = 1.0f, cloudA = 1.0f;
                        float cloudHeight = 192.0f;
                        float cloudTime = 0.0f;
                        try {
                            var envC = mc.level.environmentAttributes();
                            Integer cloudArgb = envC.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.CLOUD_COLOR, pos);
                            Float cHeight = envC.getValue(
                                net.minecraft.world.attribute.EnvironmentAttributes.CLOUD_HEIGHT, pos);
                            if (cloudArgb != null) {
                                cloudA = ((cloudArgb >> 24) & 0xFF) / 255.0f;
                                cloudR = ((cloudArgb >> 16) & 0xFF) / 255.0f;
                                cloudG = ((cloudArgb >> 8) & 0xFF) / 255.0f;
                                cloudB = (cloudArgb & 0xFF) / 255.0f;
                            }
                            if (cHeight != null) cloudHeight = cHeight;
                            long gameTime = mc.level.getGameTime();
                            float partialTicks =
                                mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                            cloudTime = (float) ((gameTime % 102400L) + partialTicks) * 0.03f;
                        } catch (Exception | LinkageError cloudEx) {
                            // API mismatch → default white clouds at y=192, no scroll
                        }
                        D3D12Bridge.updateCloud(cloudR, cloudG, cloudB, cloudA,
                            cloudHeight, cloudTime);
                    }

                    // ─── Lightmap update ──────────────────────────
                    // Compute 16x16 lightmap from MC render state and upload to Rust.
                    // Throttled to every 5 ticks to reduce CPU/JNI overhead.
                    if (lightmapTickCount++ % LIGHTMAP_UPDATE_INTERVAL_TICKS == 0) {
                        try {
                            updateLightmap(mc);
                        } catch (Exception lmEx) {
                            if (lightmapTickCount % 60 == 0) {
                                LOGGER.warn("[dx12-wm] Lightmap update failed: {}", lmEx.getMessage());
                            }
                        }
                    }

                    // ─── Entity extraction ──────────────────────────
                    try {
                        // Use reflection to access entities (API differs across MC versions)
                        var entityList = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
                        try {
                            // Level.getEntities() + LevelEntityGetter.getAll(List) via cached reflection
                            initEntityReflection();
                            if (GET_ENTITIES_METHOD != null) {
                                var entityGetter = GET_ENTITIES_METHOD.invoke(mc.level);
                                if (GET_ALL_METHOD == null && entityGetter != null) {
                                    GET_ALL_METHOD = entityGetter.getClass().getMethod("getAll", java.util.List.class);
                                }
                                if (GET_ALL_METHOD != null) {
                                    GET_ALL_METHOD.invoke(entityGetter, entityList);
                                }
                            }
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
                            D3D12Bridge.setEntities(entityData);
                        } else {
                            D3D12Bridge.setEntities(new float[0]);
                        }
                    } catch (Throwable entityEx) {
                        D3D12Bridge.setEntities(new float[0]);
                    }

                    // ─── Particle extraction ────────────────────────
                    try {
                        var particleEngine = mc.particleEngine;
                        if (particleEngine != null) {
                            var particleList = new java.util.ArrayList<net.minecraft.client.particle.Particle>();
                            int particleTypes = 0;
                            try {
                                // ParticleEngine.particles map via cached reflection
                                initParticleReflection();
                                if (PARTICLE_MAP_FIELD != null) {
                                    @SuppressWarnings("unchecked")
                                    var particleMap = (java.util.Map<?, ?>) PARTICLE_MAP_FIELD.get(particleEngine);
                                    particleTypes = particleMap.size();
                                    for (var entry : particleMap.entrySet()) {
                                        // MC 26.1.2: values are ParticleGroup<?> (NOT a Set).
                                        // Casting to Set threw ClassCastException, which was
                                        // silently swallowed → particles were never uploaded.
                                        // ParticleGroup exposes getAll() → Queue<Particle>.
                                        var group = entry.getValue();
                                        if (group instanceof net.minecraft.client.particle.ParticleGroup<?> pg) {
                                            for (var p : pg.getAll()) {
                                                particleList.add((net.minecraft.client.particle.Particle) p);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                            }

                            if (!particleList.isEmpty() && particleList.size() <= 2048) {
                                float[] particleData = new float[particleList.size() * 8];
                                int idx = 0;
                                for (var p : particleList) {
                                    // Read particle fields via cached reflection (protected in MC 26.1.2)
                                    float px = 0f, py = 0f, pz = 0f;
                                    if (PARTICLE_X_FIELD != null) {
                                        try {
                                            px = PARTICLE_X_FIELD.getFloat(p);
                                            py = PARTICLE_Y_FIELD.getFloat(p);
                                            pz = PARTICLE_Z_FIELD.getFloat(p);
                                        } catch (Exception ignored) {
                                        }
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
                                    D3D12Bridge.setParticles(trimmed);
                                    long nowMs = System.currentTimeMillis();
                                    if (nowMs - lastParticleLogMs > 5000) {
                                        lastParticleLogMs = nowMs;
                                        LOGGER.info("[dx12-wm] Particles uploaded: {} ({} types)", idx, particleTypes);
                                    }
                                } else {
                                    D3D12Bridge.setParticles(new float[0]);
                                }
                            } else {
                                D3D12Bridge.setParticles(new float[0]);
                            }
                        } else {
                            D3D12Bridge.setParticles(new float[0]);
                        }
                    } catch (Throwable particleEx) {
                        D3D12Bridge.setParticles(new float[0]);
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

    // ─── Lightmap computation (replicates MC 26.1.2 core/lightmap.fsh on CPU) ───

    /// Compute 16x16 lightmap texture from MC's LightmapRenderState and upload to Rust.
    /// Uses the same algorithm as the core/lightmap.fsh shader.
    private static void updateLightmap(Minecraft mc) {
        // Access LightmapRenderState from GameRenderer's GameRenderState
        var gameState = mc.gameRenderer.getGameRenderState();
        var rs = gameState.lightmapRenderState;

        // Detect first-frame: render state fields are null before extract() runs.
        // blockLightTint is set by LightmapRenderStateExtractor.extract() during render.
        // Before that, all Vector3fc fields are null and float fields are 0.
        boolean firstFrame = rs.blockLightTint == null;
        float skyFactor = firstFrame ? 1.0f : rs.skyFactor;
        float blockFactor = firstFrame ? 1.4f : rs.blockFactor;
        Vector3fc blockLightTint = firstFrame ? new Vector3f(1.0f, 0.8f, 0.5f) : rs.blockLightTint;
        Vector3fc skyLightColor = firstFrame ? new Vector3f(1.0f, 1.0f, 1.0f) : rs.skyLightColor;
        Vector3fc ambientColor = firstFrame ? new Vector3f(0.0f, 0.0f, 0.0f) : rs.ambientColor;
        Vector3fc nightVisionColor = firstFrame ? new Vector3f(0.0f, 0.0f, 0.0f) : rs.nightVisionColor;

        float nightVisionFactor = rs.nightVisionEffectIntensity;
        float darknessScale = rs.darknessEffectScale;
        float bossOverlay = rs.bossOverlayWorldDarkening;
        float brightness = rs.brightness;

        // Build the 16x16 lightmap (RGBA8)
        byte[] pixels = new byte[16 * 16 * 4];
        for (int blockLight = 0; blockLight < 16; blockLight++) {
            for (int skyLight = 0; skyLight < 16; skyLight++) {
                float blockLevel = (float)blockLight / 15.0f;
                float skyLevel = (float)skyLight / 15.0f;

                float blockBright = getBrightness(blockLevel) * blockFactor;
                float skyBright = getBrightness(skyLevel) * skyFactor;

                // Ambient base (max of ambient color and night vision)
                float r = Math.max(ambientColor.x(), nightVisionColor.x() * nightVisionFactor);
                float g = Math.max(ambientColor.y(), nightVisionColor.y() * nightVisionFactor);
                float b = Math.max(ambientColor.z(), nightVisionColor.z() * nightVisionFactor);

                // Sky light contribution
                r += skyLightColor.x() * skyBright;
                g += skyLightColor.y() * skyBright;
                b += skyLightColor.z() * skyBright;

                // Block light with parabolic color mix
                // See lightmap.fsh: vec3 BlockLightColor = mix(BlockLightTint, vec3(1.0), 0.9 * parabolicMixFactor(block_level));
                float parabolicMix = (2.0f * blockLevel - 1.0f) * (2.0f * blockLevel - 1.0f);
                float blockTintR = blockLightTint.x() + (1.0f - blockLightTint.x()) * 0.9f * parabolicMix;
                float blockTintG = blockLightTint.y() + (1.0f - blockLightTint.y()) * 0.9f * parabolicMix;
                float blockTintB = blockLightTint.z() + (1.0f - blockLightTint.z()) * 0.9f * parabolicMix;

                r += blockTintR * blockBright;
                g += blockTintG * blockBright;
                b += blockTintB * blockBright;

                // Boss overlay darkening
                // color = mix(color, color * vec3(0.7, 0.6, 0.6), bossOverlay)
                if (bossOverlay > 0.0f) {
                    float darkR = r * 0.7f;
                    float darkG = g * 0.6f;
                    float darkB = b * 0.6f;
                    r = r + (darkR - r) * bossOverlay;
                    g = g + (darkG - g) * bossOverlay;
                    b = b + (darkB - b) * bossOverlay;
                }

                // Darkness effect scale (subtractive)
                r = Math.max(0.0f, r - darknessScale);
                g = Math.max(0.0f, g - darknessScale);
                b = Math.max(0.0f, b - darknessScale);

                // Clamp to [0, 1]
                r = Math.min(1.0f, r);
                g = Math.min(1.0f, g);
                b = Math.min(1.0f, b);

                // Brightness / notGamma adjustment
                // See lightmap.fsh: notGamma(vec3 color) — intensity-preserving gamma correction
                float maxComponent = Math.max(Math.max(r, g), b);
                if (maxComponent > 0.001f) {
                    float maxInverted = 1.0f - maxComponent;
                    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
                    float brightR = r * (maxScaled / maxComponent);
                    float brightG = g * (maxScaled / maxComponent);
                    float brightB = b * (maxScaled / maxComponent);
                    r = r + (brightR - r) * brightness;
                    g = g + (brightG - g) * brightness;
                    b = b + (brightB - b) * brightness;
                }

                // Store RGBA pixel (row-major: skyLight rows, blockLight columns)
                int idx = (skyLight * 16 + blockLight) * 4;
                pixels[idx]     = (byte)(Math.round(r * 255.0f) & 0xFF);
                pixels[idx + 1] = (byte)(Math.round(g * 255.0f) & 0xFF);
                pixels[idx + 2] = (byte)(Math.round(b * 255.0f) & 0xFF);
                pixels[idx + 3] = (byte)255;
            }
        }

        // Phase 11d: skip upload when the lightmap content is unchanged.
        // This avoids a JNI call + GPU write_texture every 5 ticks for
        // static lighting (e.g. a torch-lit area with constant sky).
        if (Arrays.equals(lastLightmapPixels, pixels)) {
            return;
        }
        lastLightmapPixels = pixels.clone();

        // Upload to Rust via JNI
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 16 * 4);
        buffer.put(pixels);
        buffer.flip();
        D3D12Bridge.uploadLightmap(buffer, 16, 16);
    }

    /// Replicate MC lightmap.fsh get_brightness function.
    /// Brightness curve: level / (4 - 3 * level)
    /// Maps 0→0 and 1→1 with a non-linear curve in between.
    private static float getBrightness(float level) {
        if (level <= 0.0f) return 0.0f;
        return level / (4.0f - 3.0f * level);
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

    /**
     * Phase 11i: extract the camera view-projection matrix once per RENDERED
     * frame (called from GameRendererMixin.render TAIL, full frame rate).
     * Previously this ran inside the tick callback (~20 Hz), causing visible
     * stutter now that D3D12 presents every frame. Sky/fog/lightmap updates
     * stay on the tick path (they change slowly).
     */
    public static void updateCameraFromRender() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            var window = mc.getWindow();
            int width = window.getWidth();
            int height = window.getHeight();
            if (width <= 0 || height <= 0) return;

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
            D3D12Bridge.updateCameraPos((float) pos.x, (float) pos.y, (float) pos.z);
        } catch (Throwable ignored) {
            // Camera extraction is best-effort per frame; failures are not fatal.
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
