package com.dx12.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;

import net.minecraft.client.Minecraft;

/**
 * Surface mode: D3D12 Present() at TAIL of Minecraft.runTick().
 *
 * MC 26.1.2 removed Window.updateDisplay(). D3D12 renders + presents
 * AFTER the entire game tick (post GL swap) via the MinecraftMixin TAIL.
 *
 * To prevent GPU driver TDR (Timeout Detection & Recovery) from D3D12 and
 * OpenGL contending on the same HWND, the GL context is temporarily
 * detached (glfwMakeContextCurrent(0)) before calling D3D12 renderFrame(),
 * and reattached after. This ensures only one API accesses the HWND at
 * any time, eliminating the WDDM driver-level conflict.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "runTick", at = @At("TAIL"))
    private void onRunTickTail(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                // Temporarily detach GL context to avoid DXGI ↔ WGL HWND contention.
                // D3D12's Present() needs exclusive HWND access via DXGI.
                long glfwWindow = mc.getWindow().getWindow();
                GLFW.glfwMakeContextCurrent(0);

                try {
                    // Camera was updated in ClientTickEvents.END_CLIENT_TICK.
                    // renderFrame() dispatches to render_surface() → get_current_texture() → render → Present()
                    D3D12Bridge.renderFrame();
                } finally {
                    // Reattach GL context for the next tick's operations.
                    GLFW.glfwMakeContextCurrent(glfwWindow);
                }
            }
        }
    }
}
