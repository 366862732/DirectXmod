package com.dx12.client;

import com.dx12.DX12LibClient;

public class D3D12Bridge {
    // 直接委托�?DX12LibClient �?native 方法
    public static boolean nativeInit(long hwnd, int width, int height) {
        return DX12LibClient.nativeInit(hwnd, width, height);
    }
    
    public static void nativeDestroy() { 
        DX12LibClient.nativeDestroy(); 
    }
    
    public static void nativeRender() { 
        DX12LibClient.nativeRender(); 
    }
    
    public static void nativePresent() { 
        DX12LibClient.nativePresent(); 
    }
    
    public static void nativeResize(int width, int height) { 
        DX12LibClient.nativeResize(width, height); 
    }
    
    // OpenGL 拦截方法（占位实现）
    public static int glGenBuffers() { 
        System.out.println("[GL4DX12] glGenBuffers called");
        return 1; 
    }
    
    public static void glBindBuffer(int target, int buffer) { 
        System.out.println("[GL4DX12] glBindBuffer: target=" + target + ", buffer=" + buffer);
    }
    
    public static void glBufferData(int target, java.nio.ByteBuffer data, int usage) { 
        System.out.println("[GL4DX12] glBufferData: target=" + target + ", size=" + (data != null ? data.remaining() : 0));
    }
}
