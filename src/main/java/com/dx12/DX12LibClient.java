package com.dx12;

public class DX12LibClient {
    static {
        // DLL 将在 Dx12Mod 中手动加�?        System.out.println("[DX12LibClient] Class loaded");
    }
    
    public static native boolean nativeInit(long hwnd, int width, int height);
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize(int width, int height);
}
