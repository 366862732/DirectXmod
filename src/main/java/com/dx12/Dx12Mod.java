package com.dx12;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class Dx12Mod implements ClientModInitializer {

    private static boolean f6WasDown = false;

    @Override
    public void onInitializeClient() {
        System.out.println("========================================");
        System.out.println("[GL4DX12] Mod Initializing (Client)...");
        System.out.println("========================================");

        // DLL loading is handled by DX12LibClient static initializer via FabricLoader
        System.out.println("[GL4DX12] DLL discovery delegated to DX12LibClient");


        // 2. Enumerate ALL RenderSystem static methods (one-time diagnostic)
        try {
            Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            System.out.println("[GL4DX12] === RenderSystem ALL static methods ===");
            for (java.lang.reflect.Method m : rs.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    StringBuilder sig = new StringBuilder("  static ");
                    sig.append(m.getReturnType().getSimpleName()).append(" ");
                    sig.append(m.getName()).append("(");
                    Class<?>[] params = m.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (i > 0) sig.append(", ");
                        sig.append(params[i].getSimpleName());
                    }
                    sig.append(")");
                    System.out.println(sig.toString());
                }
            }
            // Also check fields
            System.out.println("[GL4DX12] === RenderSystem static FIELDS containing 'matrix' or 'proj' ===");
            for (java.lang.reflect.Field f : rs.getDeclaredFields()) {
                String fn = f.getName().toLowerCase();
                if (fn.contains("matrix") || fn.contains("proj") || fn.contains("model") || fn.contains("view")) {
                    System.out.println("  " + java.lang.reflect.Modifier.toString(f.getModifiers())
                        + " " + f.getType().getSimpleName() + " " + f.getName());
                }
            }
            System.out.println("[GL4DX12] === End enum ===");
        } catch (Exception e) {
            System.out.println("[GL4DX12] Enum FAILED: " + e);
        }

        // 2b. Enumerate DebugScreenOverlay ALL methods (find correct hook target for F3 overlay)
        try {
            Class<?> dso = Class.forName("net.minecraft.client.gui.components.DebugScreenOverlay");
            System.out.println("[GL4DX12] === DebugScreenOverlay ALL methods ===");
            for (java.lang.reflect.Method m : dso.getDeclaredMethods()) {
                StringBuilder sig = new StringBuilder("  ");
                sig.append(java.lang.reflect.Modifier.toString(m.getModifiers())).append(" ");
                sig.append(m.getReturnType().getSimpleName()).append(" ");
                sig.append(m.getName()).append("(");
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sig.append(", ");
                    sig.append(params[i].getSimpleName());
                }
                sig.append(")");
                System.out.println(sig.toString());
            }
            System.out.println("=== End DSO enum ===");
            // Also dump DSO fields containing "Text" or "text"
            System.out.println("[GL4DX12] === DSO ALL FIELDS (incl parent) ===");
            for (Class<?> c = dso; c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    System.out.println("  [" + c.getSimpleName() + "] "
                        + f.getType().getSimpleName() + " " + f.getName());
                }
            }
            System.out.println("[GL4DX12] === End DSO fields ===");
        } catch (Exception e) {
            System.out.println("[GL4DX12] DSO Enum FAILED: " + e);
        }

        // 3. Register F6 toggle + per-tick matrix sync
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long window = client.getWindow().handle();
            boolean f6Down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F6) == GLFW.GLFW_PRESS;

            if (f6Down && !f6WasDown) {
                boolean ready = D3D12Bridge.isD3D12Ready();
                System.out.println("[GL4DX12] F6 pressed, isD3D12Ready=" + ready);
                if (ready) {
                    // ===== 关闭 D3D12 =====
                    System.out.println("[GL4DX12] Entering shutdown branch");
                    D3D12Bridge.shutdownDevice();
                    D3D12Bridge.setD3D12Active(false);
                    System.out.println("[GL4DX12] After shutdown, isD3D12Ready=" + D3D12Bridge.isD3D12Ready());
                } else {
                    // ===== 开启 D3D12 =====
                    System.out.println("[GL4DX12] Entering init branch");
                    long hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
                    D3D12Bridge.ensureDeviceInitialized(hwnd);
                    D3D12Bridge.setD3D12Active(true);
                    System.out.println("[GL4DX12] D3D12 activated successfully");
                }
            }
            f6WasDown = f6Down;

            // Matrices now synced inside renderFullFrame (triggered by LevelRendererMixin)
            // Reset geometry counter each tick (DLL clears per-frame)
            D3D12Bridge.resetTranslatedCounter();
        });

        System.out.println("[GL4DX12] Press F6 to toggle D3D12 rendering");
        System.out.println("[GL4DX12] Ready - waiting for activation...");
    }
}
