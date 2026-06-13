package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Replaces OpenGL/Display lines on F3 debug screen with D3D12 adapter info.
 *
 * MC 26.1.2: DebugScreenOverlay.extractLines(GuiGraphicsExtractor m, List l, boolean left)
 * fills the list — left=false means right-side text (GPU/OpenGL/Display/Java).
 */
@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay", remap = false)
public class DebugScreenMixin {

    @Inject(method = "extractLines", at = @At("RETURN"), remap = false)
    private void onExtractLines(
            net.minecraft.client.gui.GuiGraphicsExtractor extractor,
            List<String> lines, boolean left, CallbackInfo ci) {
        if (lines == null || lines.isEmpty() || left) return;

        String d3d12Info = D3D12Bridge.getD3D12Info();
        boolean active = D3D12Bridge.isD3D12Active();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (active && d3d12Info != null) {
                if (line.startsWith("OpenGL:") || line.startsWith("OpenGL ")) {
                    lines.set(i, "\u00a7eD3D12\u00a7r: " + d3d12Info);
                } else if (line.startsWith("Display:")) {
                    String w = String.valueOf(D3D12Bridge.nativeGetWindowWidth());
                    String h = String.valueOf(D3D12Bridge.nativeGetWindowHeight());
                    lines.set(i, "D3D12 Overlay: " + w + "x" + h + " (click-through)");
                }
            } else {
                if (line.startsWith("OpenGL:") || line.startsWith("OpenGL ")) {
                    lines.set(i, line + "  \u00a77[D3D12 OFF — press F6]");
                }
            }
        }
    }

    static {
        System.out.println("[GL4DX12] DebugScreenMixin loaded — F3 overlay patched (extractLines hook)");
    }
}
