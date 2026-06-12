package com.dx12;

public class DX12LibClient {
    
    static {
        try {
            System.out.println("Loading DX12LibClient");
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("DLL loaded successfully");
        } catch (Throwable t) {
            System.err.println("Failed to load DLL");
            t.printStackTrace();
        }
    }
    
    public static native boolean nativeInit();
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize();
}