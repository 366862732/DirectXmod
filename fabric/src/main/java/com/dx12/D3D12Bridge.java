package com.dx12;

import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

    // === Convenience methods ===
    private static boolean initialized = false;
    private static long cachedHwnd = 0;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

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
}
