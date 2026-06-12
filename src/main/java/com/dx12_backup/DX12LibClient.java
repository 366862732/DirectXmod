package com.dx12;

public class DX12LibClient {
    // Native 方法声明 - 必须�?DLL 导出的函数完全匹�?    public static native boolean nativeInit(long hwnd, int width, int height);
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize(int width, int height);
    
    // 静态代码块：加�?DLL
    static {
        try {
            // 尝试从绝对路径加载（开发环境）
            System.load("D:\\dx12-lib-template-26.1.2\\src\\main\\resources\\native\\windows\\gl4dx12.dll");
            System.out.println("[GL4DX12] DLL loaded from absolute path");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[GL4DX12] Failed to load DLL: " + e);
        }
    }
}
