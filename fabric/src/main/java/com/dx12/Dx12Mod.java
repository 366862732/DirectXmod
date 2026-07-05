package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the GL4DX12 Fabric mod.
 * Uses wgpu/DX12 for rendering via a dedicated render thread.
 */
public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private static volatile boolean running = false;
    private static Thread renderThread;

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");
        LOGGER.info("Using wgpu/DX12 rendering engine");

        // Initialize Rust JNI bridge
        D3D12Bridge.init();

        // Test JNI communication
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        // Get device info
        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        // Start dedicated render thread
        running = true;
        renderThread = new Thread(Dx12Mod::renderLoop, "wgpu-render-thread");
        renderThread.setDaemon(true);
        renderThread.start();
        LOGGER.info("Render thread started");

        LOGGER.info("GL4DX12 Mod initialized successfully!");
    }

    private static void renderLoop() {
        while (running) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    // Sync window size
                    int width = client.getWindow().getWidth();
                    int height = client.getWindow().getHeight();
                    D3D12Bridge.syncWindowSize(width, height);

                    // Render frame via wgpu
                    D3D12Bridge.renderFrame();
                }
                // Sleep 1ms to avoid busy waiting
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.warn("Render loop error: {}", e.getMessage());
            }
        }
    }

    /**
     * Called when the game is shutting down.
     */
    public static void shutdown() {
        running = false;
        if (renderThread != null) {
            renderThread.interrupt();
        }
        LOGGER.info("Render thread stopped");
    }
}
