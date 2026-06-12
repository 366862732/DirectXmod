package com.dx12;

public class DX12LibClient {
    
    // ????????
    private static boolean isLoaded = false;
    
    public static native boolean nativeInit();
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize();
    
    // ????????? Dx12Mod ???
    public static void init() {
        if (!isLoaded) {
            System.out.println("[DX12LibClient] Initializing...");
            isLoaded = true;
        }
    }
}