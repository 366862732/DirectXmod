package com.dx12; 
 
import net.fabricmc.api.ClientModInitializer; 
 
public class Dx12Mod implements ClientModInitializer { 
    @Override 
    public void onInitializeClient() { 
        System.out.println("[GL4DX12] Mod loaded successfully!"); 
    } 
} 
