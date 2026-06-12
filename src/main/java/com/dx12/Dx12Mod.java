package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import com.dx12.client.D3D12Bridge;

public class Dx12Mod implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        System.out.println("========================================");
        System.out.println("[GL4DX12] Mod Initializing (Client)...");
        System.out.println("========================================");
        
        // ?? DLL
        try {
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("[GL4DX12] ? Native library loaded");
        } catch (Throwable t) {
            System.err.println("[GL4DX12] Failed to load DLL");
            t.printStackTrace();
            return;
        }
        
        // ????? 0 ?????????? D3D12 ???????????
        long hwnd = 0;
        int width = 800;
        int height = 600;
        System.out.println("[GL4DX12] Calling nativeInit with hwnd=" + hwnd + ", " + width + "x" + height);
        
        boolean result = D3D12Bridge.nativeInit(hwnd, width, height);
        System.out.println("[GL4DX12] nativeInit returned: " + result);
        
        if (result) {
            System.out.println("[GL4DX12] D3D12 backend initialized (test mode)");
        } else {
            System.err.println("[GL4DX12] D3D12 init failed (expected because hwnd=0)");
        }
    }
}