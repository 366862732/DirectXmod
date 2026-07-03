package com.dx12;

/**
 * JNI bridge for wgpu-mc Rust library.
 * Loads the native DLL and provides methods for Rust communication.
 */
public class D3D12Bridge {
    // The Rust JNI library name (without lib/extension prefix)
    // On Windows: wgpu_mc_jni.dll
    // On Linux: libwgpu_mc_jni.so
    static {
        String libName;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            libName = "wgpu_mc_jni";
        } else if (os.contains("linux")) {
            libName = "wgpu_mc_jni";
        } else if (os.contains("mac")) {
            libName = "wgpu_mc_jni";
        } else {
            libName = "wgpu_mc_jni";
        }
        try {
            System.loadLibrary(libName);
        } catch (UnsatisfiedLinkError e) {
            // DLL might not be in library path; try explicit path
            String dllPath = getDllPath();
            if (dllPath != null) {
                System.load(dllPath);
            } else {
                throw e;
            }
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
                return path;
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

    // === Convenience methods ===

    private static boolean initialized = false;

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
}
