package com.dx12;

public class DX12LibClient {

    private static boolean isLoaded = false;

    // ===== Core D3D12 lifecycle =====
    public static native boolean nativeInit(long hwnd, int width, int height);
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize(int width, int height);

    // ===== GL state bridge =====
    public static native void nativeSetClearColor(float r, float g, float b, float a);
    public static native boolean nativeIsInitialized();

    public static void init() {
        if (!isLoaded) {
            System.out.println("[DX12LibClient] Initializing...");
            isLoaded = true;
        }
    }
}
