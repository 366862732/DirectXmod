package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin {

    @Shadow
    private List<String> lines;

    /**
     * 在 DebugScreenOverlay 初始化后，直接修改 lines 列表
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        try {
            if (lines == null) return;
            
            // 替换 OpenGL 行
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line != null && line.contains("OpenGL")) {
                    String d3dInfo = D3D12Bridge.isD3D12Ready() ? 
                        D3D12Bridge.getD3D12Info() : "Not initialized";
                    String status = D3D12Bridge.isD3D12Active() ? "ACTIVE" : "INACTIVE";
                    lines.set(i, "D3D12: " + d3dInfo + " [" + status + "]");
                    System.out.println("[GL4DX12] Replaced OpenGL line with D3D12 info");
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[GL4DX12] Failed to modify debug overlay: " + e.getMessage());
        }
    }

    static {
        System.out.println("[GL4DX12] DebugScreenMixin loaded (init hook)");
    }
}