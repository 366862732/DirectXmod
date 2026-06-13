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
 * Hooks extractLines() at RETURN — the right-side text list (left=false)
 * is built by extractLines(). Throttled: only processes every 30th call
 * to avoid per-pass flicker from Sodium's multi-pass graph extraction.
 */
@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay", remap = false)
public class DebugScreenMixin {

    @Inject(method = "extractLines", at = @At("RETURN"), remap = false)
    private void onExtractLines(
            net.minecraft.client.gui.GuiGraphicsExtractor extractor,
            List<String> lines, boolean left, CallbackInfo ci) {
        if (lines == null || lines.isEmpty() || left) return;

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
    }

    static {
        System.out.println("[GL4DX12] DebugScreenMixin loaded (extractLines)");
    }
}
