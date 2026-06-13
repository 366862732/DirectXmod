package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

import com.dx12.client.D3D12Bridge;

public class Dx12Mod implements ClientModInitializer {

    private static boolean f6WasDown = false;

    @Override
    public void onInitializeClient() {
        System.out.println("========================================");
        System.out.println("[GL4DX12] Mod Initializing (Client)...");
        System.out.println("========================================");

        // 1. Load native DLL (no D3D12 device yet)
        try {
            NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
            System.out.println("[GL4DX12] Native library loaded");
        } catch (Throwable t) {
            System.err.println("[GL4DX12] Failed to load DLL: " + t.getMessage());
            t.printStackTrace();
            return;
        }

        // 2. Register F6 toggle + per-tick matrix sync
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long window = client.getWindow().handle();
            boolean f6Down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F6) == GLFW.GLFW_PRESS;

            if (f6Down && !f6WasDown) {
                if (D3D12Bridge.isD3D12Ready()) {
                    D3D12Bridge.shutdownDevice();
                } else {
                    long hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
                    D3D12Bridge.ensureDeviceInitialized(hwnd);
                }
            }
            f6WasDown = f6Down;

            // Sync matrices to DLL each tick
            D3D12Bridge.syncMatrices();
            // Reset geometry counter each tick (DLL clears per-frame)
            D3D12Bridge.resetTranslatedCounter();
        });

        System.out.println("[GL4DX12] Press F6 to toggle D3D12 rendering");
        System.out.println("[GL4DX12] Ready - waiting for activation...");
    }
}
