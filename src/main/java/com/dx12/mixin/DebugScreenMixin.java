package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Replace OpenGL/Display lines on F3 debug screen with D3D12 adapter info.
 *
 * MC 26.1.2: hooks render() at HEAD — the right-side text list is built via
 * extractLines() during graph extraction THEN drawRightText() renders it.
 * By hooking render() we intercept between build and draw, once per frame.
 */
@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay", remap = false)
public class DebugScreenMixin {

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void onRender(Object guiGraphics, CallbackInfo ci) {
        try {
            // Access the right-side text list via reflection
            // MC 26.1.2: DebugScreenOverlay stores it, built by extractLines()
            // The Sodium graph extraction runs before render(), so list is ready
            java.lang.reflect.Field f = this.getClass().getDeclaredField("rightText");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) f.get(this);
            if (lines == null || lines.isEmpty()) return;

            String info = D3D12Bridge.getD3D12Info();
            boolean active = D3D12Bridge.isD3D12Active();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null) continue;

                if (active && info != null) {
                    if (line.startsWith("OpenGL:")) {
                        lines.set(i, "\u00a7eD3D12\u00a7r: " + info);
                    } else if (line.startsWith("Display:")) {
                        lines.set(i, "D3D12 Overlay \u00a7aactive\u00a7r");
                    }
                } else {
                    if (line.startsWith("OpenGL:")) {
                        lines.set(i, line + " \u00a77[\u00a7cOFF\u00a77]");
                    }
                }
            }
        } catch (Exception ignore) {
            // Field name may differ — silent fallback
        }
    }

    static {
        System.out.println("[GL4DX12] DebugScreenMixin loaded (render hook)");
    }
}
