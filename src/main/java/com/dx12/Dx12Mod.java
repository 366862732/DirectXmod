package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import com.dx12.client.D3D12Bridge;

public class Dx12Mod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("========================================");
        System.out.println("[GL4DX12] Mod Initializing (Client)...");
        System.out.println("========================================");

        // 1. Load native DLL
        try {
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("[GL4DX12] Native library loaded");
        } catch (Throwable t) {
            System.err.println("[GL4DX12] Failed to load DLL: " + t.getMessage());
            t.printStackTrace();
            return;
        }

        // 2. Initialize D3D12 (hwnd=0 means auto-find in C++)
        System.out.println("[GL4DX12] Initializing D3D12 (auto-find window)...");

        boolean result = D3D12Bridge.nativeInit(0, 0, 0);
        System.out.println("[GL4DX12] nativeInit returned: " + result);

        if (result) {
            System.out.println("[GL4DX12] D3D12 backend initialized successfully!");
        } else {
            System.err.println("[GL4DX12] D3D12 init FAILED - check C:\\temp\\gl4dx12_d3d12.log");
        }
    }
}
