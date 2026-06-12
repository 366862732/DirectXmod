package com.dx12.client;

import com.dx12.DX12LibClient;

public class D3D12Bridge {
    
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
}