package com.dx12;

public class DX12LibClient {
    
    private static boolean isLoaded = false;
    
    public static native boolean nativeInit(long hwnd, int width, int height);
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize(int width, int height);
    
    public static void init() {
        if (!isLoaded) {
            System.out.println("[DX12LibClient] Initializing...");
            isLoaded = true;
        }
    }
}