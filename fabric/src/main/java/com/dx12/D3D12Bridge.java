package com.dx12;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.nio.ByteBuffer;

/**
 * JNI bridge for wgpu-mc Rust library.
 */
public class D3D12Bridge {
    static {
        String libName;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            libName = "wgpu_mc_jni.dll";
        } else if (os.contains("linux")) {
            libName = "libwgpu_mc_jni.so";
        } else if (os.contains("mac")) {
            libName = "libwgpu_mc_jni.dylib";
        } else {
            libName = "wgpu_mc_jni";
        }
        
        String dllPath = getDllPath();
        if (dllPath != null) {
            System.load(dllPath);
        } else {
            System.loadLibrary(libName);
        }
    }

    private static String getDllPath() {
        // Try loading from the same directory as the JAR (mods folder)
        try {
            java.net.URL jarUrl = D3D12Bridge.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File jarFile = new java.io.File(jarUrl.toURI());
            java.io.File modsDir = jarFile.getParentFile();
            java.io.File dllInMods = new java.io.File(modsDir, "wgpu_mc_jni.dll");
            if (dllInMods.exists()) {
                return dllInMods.getAbsolutePath();
            }
        } catch (Exception e) {
            // Fall through to other paths
        }
        
        // Fallback: try common relative paths
        String[] candidates = {
            "dx12mod/wgpu_mc_jni.dll",
            ".minecraft/dx12mod/wgpu_mc_jni.dll",
            System.getProperty("user.dir") + "/dx12mod/wgpu_mc_jni.dll"
        };
        for (String path : candidates) {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    // === Native methods ===
    public static native void nativeInit();
    public static native String nativeHello(String input);
    public static native String nativeTestDeviceInfo();
    public static native void nativeSetWindow(long hwnd);
    public static native byte[] nativeRenderFrame();
    public static native void nativeResize(int width, int height);

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
            System.out.println("[D3D12Bridge] Rust JNI library loaded and initialized.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[D3D12Bridge] Failed to load Rust JNI library: " + e.getMessage());
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
     * Get the Minecraft window HWND.
     * Uses reflection to access the Window's handle field.
     */
    private static long lastCheckTime = 0;
    private static final long CACHE_TTL_MS = 5000;

    public static long getWindowHandle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0;
        }

        // Use cached value if still valid
        long now = System.currentTimeMillis();
        if (cachedHwnd != 0 && (now - lastCheckTime) < CACHE_TTL_MS) {
            return cachedHwnd;
        }

        try {
            // Access the GLFW window handle field (Yarn mapped name: field_5187)
            java.lang.reflect.Field field = client.getWindow().getClass().getDeclaredField("field_5187");
            field.setAccessible(true);
            long glfwWindow = field.getLong(client.getWindow());
            if (glfwWindow == 0) {
                Dx12Mod.LOGGER.warn("Window field_5187 is 0");
                return 0;
            }
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
            cachedHwnd = hwnd;
            lastCheckTime = now;
            return hwnd;
        } catch (NoSuchFieldException e) {
            Dx12Mod.LOGGER.error("Window.field_5187 not found: {}", e.getMessage());
            return 0;
        } catch (IllegalAccessException e) {
            Dx12Mod.LOGGER.error("Cannot access Window.field_5187: {}", e.getMessage());
            return 0;
        }
    }

    public static void setWindow(long hwnd) {
        if (hwnd == 0) {
            Dx12Mod.LOGGER.warn("setWindow called with hwnd=0, skipping");
            return;
        }
        if (cachedHwnd == hwnd) return;
        cachedHwnd = hwnd;
        try {
            nativeSetWindow(hwnd);
            Dx12Mod.LOGGER.info("nativeSetWindow called with HWND: 0x%016x", hwnd);
        } catch (UnsatisfiedLinkError e) {
            Dx12Mod.LOGGER.error("nativeSetWindow not available: {}", e.getMessage());
        }
    }

    public static ByteBuffer renderFrame() {
        if (!initialized) return null;
        try {
            byte[] pixels = nativeRenderFrame();
            if (pixels == null || pixels.length == 0) {
                Dx12Mod.LOGGER.info("nativeRenderFrame returned null or empty");
                return null;
            }
            ByteBuffer buf = BufferUtils.createByteBuffer(pixels.length);
            buf.put(pixels);
            buf.flip();
            Dx12Mod.LOGGER.info("renderFrame returned {} bytes", pixels.length);
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
}
