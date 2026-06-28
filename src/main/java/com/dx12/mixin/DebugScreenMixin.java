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
     * 在 DebugScreenOverlay 初始化后，替换 OpenGL 行
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        replaceOpenGLLine();
    }

    /**
     * drawGameInformation 每帧被调用，刷新文本行
     * 在调用后再次替换，确保持续覆盖
     */
    @Inject(method = "drawGameInformation", at = @At("RETURN"), require = 0)
    private void onDrawGameInformation(CallbackInfo ci) {
        replaceOpenGLLine();
    }

    private void replaceOpenGLLine() {
        try {
            if (lines == null) return;
            if (!D3D12Bridge.isD3D12Active()) return;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line != null && (line.contains("OpenGL") || line.startsWith("Display:"))) {
                    String d3dInfo = D3D12Bridge.isD3D12Ready() ?
                        D3D12Bridge.getD3D12Info() : "Not initialized";
                    String tag = line.contains("OpenGL") ? "D3D12" : "Display";
                    lines.set(i, tag + ": [D3D12] " + d3dInfo);
                    break;
                }
            }
        } catch (Exception e) {
            // 静默失败
        }
    }

    static {
        System.out.println("[GL4DX12] DebugScreenMixin loaded");
    }
}