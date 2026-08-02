package com.dx12;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

/**
 * JNI bridge for wgpu-mc Rust library.
 * Loads native DLL from dx12mod/ directory under the game working directory.
 */
public class D3D12Bridge {
    static {
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        try {
            String libName = "wgpu_mc_jni.dll";
            Path dllDir = getDllDir();
            Path dllPath = dllDir.resolve(libName);

            // Always extract DLL from JAR to ensure version match
            try (InputStream in = D3D12Bridge.class.getResourceAsStream("/" + libName)) {
                if (in != null) {
                    Files.copy(in, dllPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[D3D12Bridge] Extracted DLL to: " + dllPath);
                } else {
                    System.err.println("[D3D12Bridge] DLL not found in JAR resources");
                    return;
                }
            }

            System.load(dllPath.toAbsolutePath().toString());
            System.out.println("[D3D12Bridge] Native library loaded from: " + dllPath);
        } catch (Exception e) {
            System.err.println("[D3D12Bridge] Failed to load native library: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Path getDllDir() {
        // Use user.dir (launcher working directory)
        // For version-isolated launchers: D:\.minecraft\versions\<version>\dx12mod
        String userDir = System.getProperty("user.dir");
        Path dllDir = Path.of(userDir, "dx12mod");
        try {
            Files.createDirectories(dllDir);
        } catch (Exception e) {
            // Directory creation failed, continue anyway
        }
        return dllDir;
    }

    // === Native methods ===
    public static native void nativeInit();
    public static native String nativeHello(String input);
    public static native String nativeTestDeviceInfo();
    public static native void nativeSetWindow(long hwnd);
    public static native byte[] nativeRenderFrame();
    public static native void nativeResize(int width, int height);
    public static native void nativeUpdateCamera(float[] matrix);
    public static native void nativeUpdateCameraPos(float x, float y, float z);
    public static native void nativeUpdateFog(float r, float g, float b, float density);
    public static native void nativeSetAaMode(int mode);
    public static native void nativeUpdateSky(float topR, float topG, float topB,
        float horizonR, float horizonG, float horizonB, float sunAngle,
        float moonAngle, float starAngle, float starBrightness,
        float moonPhase, float rainBrightness);
    /** Update the procedural cloud layer (ARGB tint, height, wind offset). */
    public static native void nativeUpdateCloud(float r, float g, float b, float a,
        float height, float time);
    /** Set whether the camera is submerged (water fog over sky/clouds). */
    public static native void nativeUpdateUnderwater(boolean underwater);

    // ─── Entities & Particles ──────────────────────────────────

    /** Upload entity data for colored box rendering (9 floats per entity). */
    public static native void nativeSetEntities(float[] data);
    /** Upload particle data for point sprite rendering (8 floats per particle). */
    public static native void nativeSetParticles(float[] data);
    public static native void nativeSetFramePixels(java.nio.ByteBuffer buffer, int width, int height);
    public static native void nativeUploadChunkMesh(int sectionX, int sectionY, int sectionZ,
        int layer, java.nio.ByteBuffer buffer, int vertexCount, int vertexStride);
    public static native int nativeIsReady();
    public static native String nativeGetStatus();
    public static native boolean nativeHasSurface();
    public static native boolean nativeHasChunkGeometry();
    public static native void nativeClearChunkSection(int sectionX, int sectionY, int sectionZ);
    public static native void nativeUploadTerrainAtlas(java.nio.ByteBuffer buffer, int width, int height);
    public static native void nativeUploadLightmap(java.nio.ByteBuffer buffer, int width, int height);

    // === Convenience methods ===
    private static boolean initialized = false;
    private static long cachedHwnd = 0;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    // Phase 11g: entity/particle data is re-extracted every tick even when
    // unchanged. Cache the last payload and skip the JNI call + GPU write
    // when it is identical (mirrors the Phase 11d lightmap dedup).
    private static float[] lastEntityData = null;
    private static float[] lastParticleData = null;

    public static void setEntities(float[] data) {
        if (!initialized) return;
        if (Arrays.equals(lastEntityData, data)) return;
        lastEntityData = data;
        nativeSetEntities(data);
    }

    public static void setParticles(float[] data) {
        if (!initialized) return;
        if (Arrays.equals(lastParticleData, data)) return;
        lastParticleData = data;
        nativeSetParticles(data);
    }

    public static void init() {
        if (initialized) return;
        try {
            nativeInit();
            initialized = true;
            System.out.println("[D3D12Bridge] Rust JNI library initialized.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[D3D12Bridge] Failed to init Rust JNI library: " + e.getMessage());
        }
    }

    public static boolean isInitialized() { return initialized; }

    public static String sayHello(String msg) {
        if (!initialized) init();
        return nativeHello(msg);
    }

    public static String getDeviceInfo() {
        if (!initialized) init();
        return nativeTestDeviceInfo();
    }

    /**
     * Get the Minecraft window HWND via LWJGL GLFW (no Yarn mapping issues).
     */
    private static long lastCheckTime = 0;
    private static final long CACHE_TTL_MS = 5000;

    public static long getWindowHandle() {
        long now = System.currentTimeMillis();
        if (cachedHwnd != 0 && (now - lastCheckTime) < CACHE_TTL_MS) {
            return cachedHwnd;
        }

        try {
            // Get GLFW window pointer from current OpenGL context
            long glfwWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            if (glfwWindowPtr == 0) {
                Dx12Mod.LOGGER.warn("glfwGetCurrentContext returned 0");
                return 0;
            }

            // Convert GLFW window to Windows HWND
            long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(glfwWindowPtr);
            cachedHwnd = hwnd;
            lastCheckTime = now;
            return hwnd;
        } catch (Exception e) {
            Dx12Mod.LOGGER.error("Failed to get window handle: {}", e.getMessage());
            return 0;
        }
    }

    private static long lastSetHwnd = 0;

    public static void setWindow(long hwnd) {
        if (hwnd == 0) return;
        if (hwnd == lastSetHwnd) return;
        try {
            nativeSetWindow(hwnd);
            lastSetHwnd = hwnd;
        } catch (UnsatisfiedLinkError e) {
            Dx12Mod.LOGGER.error("nativeSetWindow not available: {}", e.getMessage());
        }
    }

    public static ByteBuffer renderFrame() {
        if (!initialized) return null;
        try {
            byte[] pixels = nativeRenderFrame();
            if (pixels == null || pixels.length == 0) {
                return null;
            }
            // Use BufferUtils (standard DirectByteBuffer) with 4KB padding.
            // NVIDIA driver (nvoglv64) reads texture data in page-sized DMA
            // transfers. When a texture row crosses a 4KB page boundary, the
            // driver continues reading the full page past our buffer, causing
            // ACCESS_VIOLATION. One extra page ensures the DMA never overruns.
            int paddedSize = pixels.length + 4096;
            ByteBuffer buf = BufferUtils.createByteBuffer(paddedSize);
            buf.put(pixels);
            buf.flip();
            buf.limit(pixels.length);
            return buf;
        } catch (UnsatisfiedLinkError e) {
            Dx12Mod.LOGGER.warn("nativeRenderFrame not available: {}", e.getMessage());
            return null;
        }
    }

    public static void syncWindowSize(int width, int height) {
        if (!initialized) return;
        if (lastWidth == width && lastHeight == height) return;
        Dx12Mod.LOGGER.info("Resize: {}x{} -> {}x{}", lastWidth, lastHeight, width, height);
        lastWidth = width;
        lastHeight = height;
        try {
            nativeResize(width, height);
        } catch (UnsatisfiedLinkError e) {
            Dx12Mod.LOGGER.warn("nativeResize not available: {}", e.getMessage());
        }
    }

    public static void updateCamera(float[] matrix) {
        if (!initialized) return;
        try {
            nativeUpdateCamera(matrix);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore if native lib not available
        }
    }

    public static void updateCameraPos(float x, float y, float z) {
        if (!initialized) return;
        try {
            nativeUpdateCameraPos(x, y, z);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Apply anti-aliasing mode to D3D12 renderer. */
    public static void setAaMode(int mode) {
        if (!initialized) return;
        try {
            nativeSetAaMode(mode);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Update D3D12 sky rendering parameters from MC sky data.
     *  Angles are in radians; moonPhase is the MoonPhase.index() 0..7;
     *  rainBrightness is 1 - rainLevel. */
    public static void updateSky(float topR, float topG, float topB,
            float horizonR, float horizonG, float horizonB, float sunAngle,
            float moonAngle, float starAngle, float starBrightness,
            float moonPhase, float rainBrightness) {
        if (!initialized) return;
        try {
            nativeUpdateSky(topR, topG, topB, horizonR, horizonG, horizonB,
                sunAngle, moonAngle, starAngle, starBrightness,
                moonPhase, rainBrightness);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Update the procedural cloud layer: ARGB tint, height, wind offset (blocks). */
    public static void updateCloud(float r, float g, float b, float a,
            float height, float time) {
        if (!initialized) return;
        try {
            nativeUpdateCloud(r, g, b, a, height, time);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Set whether the camera is submerged (applies water fog to sky/clouds). */
    public static void updateUnderwater(boolean underwater) {
        if (!initialized) return;
        try {
            nativeUpdateUnderwater(underwater);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Upload captured GL framebuffer pixels as a D3D12 texture. */
    public static void setFramePixels(java.nio.ByteBuffer buffer, int width, int height) {
        if (!initialized) return;
        try {
            nativeSetFramePixels(buffer, width, height);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Upload captured GL HUD/UI pixels as a D3D12 overlay texture for compositing. */
    public static void setHudPixels(java.nio.ByteBuffer buffer, int width, int height) {
        if (!initialized) return;
        try {
            nativeSetHudPixels(buffer, width, height);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    public static native void nativeSetHudPixels(java.nio.ByteBuffer buffer, int width, int height);

    /** Returns 1 if renderer ready, 0 if initializing, -1 if failed. */
    public static int isReady() {
        if (!initialized) return -1;
        try {
            return nativeIsReady();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /** Returns human-readable renderer status string. */
    public static String getStatus() {
        if (!initialized) return "not_initialized";
        try {
            return nativeGetStatus();
        } catch (UnsatisfiedLinkError e) {
            return "native_error: " + e.getMessage();
        }
    }

    /** Upload MC chunk section mesh to D3D12 for native rendering.
     *  @param layer MC chunk render layer: 0=SOLID, 1=CUTOUT, 2=TRANSLUCENT */
    public static void uploadChunkMesh(int sectionX, int sectionY, int sectionZ,
            int layer, java.nio.ByteBuffer buffer, int vertexCount, int vertexStride) {
        if (!initialized) return;
        try {
            nativeUploadChunkMesh(sectionX, sectionY, sectionZ, layer, buffer, vertexCount, vertexStride);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Clear all old meshes for a chunk section before recompilation. */
    public static void clearChunkSection(int sectionX, int sectionY, int sectionZ) {
        if (!initialized) return;
        try {
            nativeClearChunkSection(sectionX, sectionY, sectionZ);
        } catch (UnsatisfiedLinkError e) {
            // Silently ignore
        }
    }

    /** Returns true if D3D12 surface mode is active (swapchain presents directly). */
    public static boolean hasSurface() {
        if (!initialized) return false;
        try {
            return nativeHasSurface();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Returns true if any MC chunk geometry has been uploaded to D3D12. */
    public static boolean hasChunkGeometry() {
        if (!initialized) return false;
        try {
            return nativeHasChunkGeometry();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Upload MC terrain atlas texture to D3D12 for chunk rendering.
     *  Call after MC textures are fully loaded and the terrain atlas is stitched. */
    public static void uploadTerrainAtlas(java.nio.ByteBuffer buffer, int width, int height) {
        if (!initialized) return;
        try {
            nativeUploadTerrainAtlas(buffer, width, height);
        } catch (UnsatisfiedLinkError e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] uploadTerrainAtlas failed: {}", e.toString());
        }
    }

    /** Upload MC lightmap texture for dynamic block/sky lighting (16x16 per vanilla MC). */
    public static void uploadLightmap(java.nio.ByteBuffer buffer, int width, int height) {
        if (!initialized) return;
        try {
            nativeUploadLightmap(buffer, width, height);
        } catch (UnsatisfiedLinkError e) {
            Dx12Mod.LOGGER.warn("[dx12-wm] uploadLightmap failed: {}", e.toString());
        }
    }
}
