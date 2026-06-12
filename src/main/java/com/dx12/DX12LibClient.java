package com.dx12;

public class DX12LibClient {
    
    static {
        System.out.println("[DX12LibClient] Static initializer started");
        System.out.println("[DX12LibClient] Attempting to load native library...");
        try {
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("[DX12LibClient] ? DLL loaded successfully in static block");
        } catch (Throwable t) {
            System.err.println("[DX12LibClient] ? Failed to load DLL in static block");
            t.printStackTrace();
        }
    }
    
    public static native boolean nativeInit();
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize();
    
    // ????????
    public static void test() {
        System.out.println("[DX12LibClient] test() called");
    }
}