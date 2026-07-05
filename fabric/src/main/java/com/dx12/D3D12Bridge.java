package com.dx12;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

/**
 * JNI bridge for wgpu-mc Rust library.
 * Loads the native DLL and provides methods for Rust communication.
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
        
        // Try loading from the same directory as the JAR first
        String dllPath = getDllPath();
        if (dllPath != null) {
            System.load(dllPath);
        } else {
            // Fallback: try from library path
            System.loadLibrary(libName);
        }
    }

    private static String getDllPath() {
        // Look for DLL in common locations
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

    // === Native method declarations ===

    /** Initialize the Rust library */
    public static native void nativeInit();

    /** Test JNI communication: returns "Hello from Rust! You said: <input>" */
    public static native String nativeHello(String input);

    /** Get device info string from Rust side */
    public static native String nativeTestDeviceInfo();

    /** Set the Minecraft window HWND for wgpu surface creation */
    public static native void nativeSetWindow(long hwnd);

    /** Render a single frame via Rust/wgpu backend */
    public static native void nativeRenderFrame();

    // === Convenience methods ===

    private static boolean initialized = false;
    private static long cachedHwnd = 0;

    public static void init() {
        if (initialized) return;
        try {
            nativeInit();
            initialized = true;
            System.out.println("[D3D12Bridge] Rust JNI library loaded and initialized.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[D3D12Bridge] Failed to load Rust JNI library: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String sayHello(String msg) {
        if (!initialized) init();
        return nativeHello(msg);
    }

    public static String getDeviceInfo() {
        if (!initialized) init();
        return nativeTestDeviceInfo();
    }

    /**
     * Get the current Minecraft window HWND via LWJGL GLFW.
     * Returns 0 if not available.
     */
    public static long getWindowHandle() {
        long glfwWindow = GLFW.glfwGetCurrentContext();
        if (glfwWindow == 0) {
            com.dx12.Dx12Mod.LOGGER.warn("glfwGetCurrentContext returned 0");
            return 0;
        }
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        com.dx12.Dx12Mod.LOGGER.debug("glfwGetWin32Window returned: 0x{:016x}", hwnd);
        return hwnd;
    }

    /**
     * Set the window HWND for wgpu surface creation.
     * Called once during initialization.
     */
    public static void setWindow(long hwnd) {
        if (hwnd == 0) {
            com.dx12.Dx12Mod.LOGGER.warn("setWindow called with hwnd=0, skipping");
            return;
        }
        if (cachedHwnd == hwnd) return; // Already set
        cachedHwnd = hwnd;
        try {
            nativeSetWindow(hwnd);
            com.dx12.Dx12Mod.LOGGER.info("nativeSetWindow called with HWND: 0x{:016x}", hwnd);
        } catch (UnsatisfiedLinkError e) {
            com.dx12.Dx12Mod.LOGGER.error("nativeSetWindow not available: {}", e.getMessage());
        }
    }

    /**
     * Render a single frame via the Rust/wgpu backend.
     */
    public static void renderFrame() {
        if (!initialized) return;
        try {
            nativeRenderFrame();
        } catch (UnsatisfiedLinkError e) {
            com.dx12.Dx12Mod.LOGGER.warn("nativeRenderFrame not available: {}", e.getMessage());
        }
    }
}
