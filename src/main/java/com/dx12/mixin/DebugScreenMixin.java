package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Replaces the "OpenGL" and "Display" entries on Minecraft's F3 debug screen
 * with D3D12 adapter info when the mod is active.
 */
@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay", remap = false)
public class DebugScreenMixin {

    @Inject(method = "getSystemInformation", at = @At("RETURN"), remap = false)
    private void onGetSystemInformation(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();
        if (lines == null || lines.isEmpty()) return;

        String d3d12Info = D3D12Bridge.getD3D12Info();
        boolean active = D3D12Bridge.isD3D12Active();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (active && d3d12Info != null) {
                // Replace "OpenGL: ..." line with D3D12 info
                if (line.startsWith("OpenGL:") || line.startsWith("OpenGL ")) {
                    lines.set(i, "§eD3D12§r: " + d3d12Info);
                }
                // Replace "Display: ..." line with overlay status
                else if (line.startsWith("Display:")) {
                    String w = String.valueOf(D3D12Bridge.nativeGetWindowWidth());
                    String h = String.valueOf(D3D12Bridge.nativeGetWindowHeight());
                    lines.set(i, "D3D12 Overlay: " + w + "x" + h + " (click-through)");
                }
            } else {
                // D3D12 not active — mark OpenGL line
                if (line.startsWith("OpenGL:") || line.startsWith("OpenGL ")) {
                    lines.set(i, line + "  §7[D3D12 OFF — press F6]");
                }
            }
        }
    }

    static {
        System.out.println("[GL4DX12] DebugScreenMixin loaded — F3 overlay patched");
    }
}
