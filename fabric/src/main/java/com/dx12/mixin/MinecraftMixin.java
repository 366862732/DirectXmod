package com.dx12.mixin;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;

import net.minecraft.client.Minecraft;

/**
 * Surface mode: D3D12 surface initialization at TAIL of Minecraft.runTick().
 *
 * MC 26.1.2 removed Window.updateDisplay(). The D3D12 swapchain surface is
 * (re)created here after a game tick, while the GL context is temporarily
 * detached — GL context detach/reattach keeps DXGI and WGL from contending
 * on the same HWND, eliminating the WDDM driver-level TDR (Timeout Detection
 * & Recovery) conflict.
 *
 * Per-frame D3D12 rendering + Present() happens in GameRendererMixin.render
 * TAIL (vsync-driven, once per rendered frame). runTick fires only ~20 Hz
 * (one per game tick), so Present was moved there in Phase 11h to unlock
 * full frame-rate.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    private static boolean minecraftMixinApplied = false;

    @Inject(method = "runTick", at = @At("TAIL"))
    private void onRunTickTail(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            if (!minecraftMixinApplied) {
                minecraftMixinApplied = true;
                com.dx12.Dx12Mod.LOGGER.info("[dx12-wm] MinecraftMixin applied — runTick TAIL");
            }

            // Get GLFW window and HWND BEFORE detaching GL context.
            // glfwGetWin32Window requires the GL context to be current.
            long glfwWindow = GLFW.glfwGetCurrentContext();
            if (glfwWindow == 0) return;
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);

            // Detach GL context to avoid DXGI ↔ WGL HWND contention
            // during D3D12 surface creation and Present().
            GLFW.glfwMakeContextCurrent(0);

            try {
                // Init D3D12 surface if needed (must be done without GL context bound)
                if (hwnd != 0) {
                    D3D12Bridge.setWindow(hwnd);
                }

                // Phase 11h: D3D12 Present moved to GameRendererMixin.render TAIL
                // (per-frame, vsync-driven). runTick fires ~20 Hz (one per game
                // tick), which used to cap D3D12 output at 20 FPS. Only surface
                // init stays here since it must run without a GL context bound.
            } finally {
                // Reattach GL context for the next tick's operations.
                GLFW.glfwMakeContextCurrent(glfwWindow);
            }
        }
    }
}
