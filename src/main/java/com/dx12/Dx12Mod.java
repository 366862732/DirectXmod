package com.dx12;

import net.fabricmc.api.ModInitializer;

public class Dx12Mod implements ModInitializer {
    
    @Override
    public void onInitialize() {
        System.out.println("=== GL4DX12 Mod Initializing ===");
        
        try {
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("DLL loaded successfully");
            
            boolean result = DX12LibClient.nativeInit();
            System.out.println("nativeInit returned: " + result);
            
        } catch (Throwable t) {
            System.err.println("Failed to initialize GL4DX12");
            t.printStackTrace();
        }
        
        System.out.println("=== GL4DX12 Mod Initialized ===");
    }
}