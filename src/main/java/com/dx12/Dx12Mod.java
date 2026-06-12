package com.dx12;

import net.fabricmc.api.ClientModInitializer;

public class Dx12Mod implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        System.out.println("========================================");
        System.out.println("[GL4DX12] Mod Initializing (Client)...");
        System.out.println("[GL4DX12] Minecraft Version: 26.1.2");
        System.out.println("[GL4DX12] Java Version: " + System.getProperty("java.version"));
        System.out.println("[GL4DX12] OS: " + System.getProperty("os.name"));
        System.out.println("========================================");
        
        // ??1: ??????????
        System.out.println("[GL4DX12] Step 1: Checking if DLL exists in JAR...");
        try {
            java.net.URL dllUrl = getClass().getResource("/native/windows/gl4dx12.dll");
            if (dllUrl != null) {
                System.out.println("[GL4DX12] ? DLL found in JAR at: " + dllUrl);
            } else {
                System.err.println("[GL4DX12] ? DLL NOT found in JAR at /native/windows/gl4dx12.dll");
            }
        } catch (Throwable t) {
            System.err.println("[GL4DX12] ? Failed to check DLL: " + t.getMessage());
        }
        
        // ??2: ?? Native ?
        System.out.println("[GL4DX12] Step 2: Loading native library...");
        try {
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("[GL4DX12] ? Native library loaded successfully");
        } catch (Throwable t) {
            System.err.println("[GL4DX12] ? Failed to load native library");
            t.printStackTrace();
        }
        
        // ??3: ?? nativeInit
        System.out.println("[GL4DX12] Step 3: Calling nativeInit()...");
        try {
            boolean result = DX12LibClient.nativeInit();
            System.out.println("[GL4DX12] ? nativeInit() returned: " + result);
            if (result) {
                System.out.println("[GL4DX12] ? DirectX 12 backend initialized!");
            } else {
                System.err.println("[GL4DX12] ? nativeInit() returned false - DX12 initialization failed");
            }
        } catch (Throwable t) {
            System.err.println("[GL4DX12] ? nativeInit() threw exception");
            t.printStackTrace();
        }
        
        System.out.println("========================================");
        System.out.println("[GL4DX12] Mod Initialization Complete");
        System.out.println("========================================");
    }
}