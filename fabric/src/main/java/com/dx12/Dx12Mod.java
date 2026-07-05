package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the GL4DX12 Fabric mod.
 * Initializes the Rust JNI bridge on client startup.
 */
public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");
        LOGGER.info("Using wgpu + Rust rendering engine (independent surface approach)");

        // Verify Mixin class exists
        try {
            Class.forName("com.dx12.mixin.GameRendererMixin");
            LOGGER.info("Mixin class GameRendererMixin found in JAR");
        } catch (ClassNotFoundException e) {
            LOGGER.error("FATAL: GameRendererMixin class NOT found in JAR!", e);
        }

        // Initialize Rust JNI bridge
        D3D12Bridge.init();

        // Test JNI communication
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        // Get device info
        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        LOGGER.info("GL4DX12 Mod initialized successfully!");
    }
}
