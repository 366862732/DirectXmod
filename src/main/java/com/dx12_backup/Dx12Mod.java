package com.dx12;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dx12Mod implements ModInitializer {
    public static final String MOD_ID = "gl4dx12";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("=== GL4DX12 Mod Initializing ===");
        
        // 测试 DLL 是否正常工作
        try {
            LOGGER.info("Testing nativeInit...");
            boolean result = DX12LibClient.nativeInit(0, 1920, 1080);
            LOGGER.info("nativeInit result: {}", result);
        } catch (Throwable t) {
            LOGGER.error("Failed to call native method", t);
        }
        
        LOGGER.info("GL4DX12 Mod Initialized!");
    }
}
