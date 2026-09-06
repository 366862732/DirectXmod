package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Device;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P32 diagnostic: logs FogRenderer updateBuffer fog parameters every 60 frames.
 * Confirms renderDistanceStart/End and environmental fog alignment with depthFar.
 */
@Mixin(FogRenderer.class)
public class FogRendererUpdateDebugMixin {

    private int dx12_fogUpdateCount = 0;

    @Inject(method = "updateBuffer", at = @At("TAIL"), remap = false)
    private void dx12_fogUpdateDebug(FogData fog, CallbackInfo ci) {
        dx12_fogUpdateCount++;
        if (dx12_fogUpdateCount % 60 != 0 && dx12_fogUpdateCount <= 5) return;

        float rs = -1f, re = -1f, es = -1f, ee = -1f;
        try {
            java.lang.reflect.Field rsField = FogData.class.getDeclaredField("renderDistanceStart");
            rsField.setAccessible(true);
            rs = rsField.getFloat(fog);
            java.lang.reflect.Field reField = FogData.class.getDeclaredField("renderDistanceEnd");
            reField.setAccessible(true);
            re = reField.getFloat(fog);
            java.lang.reflect.Field esField = FogData.class.getDeclaredField("environmentalStart");
            esField.setAccessible(true);
            es = esField.getFloat(fog);
            java.lang.reflect.Field eeField = FogData.class.getDeclaredField("environmentalEnd");
            eeField.setAccessible(true);
            ee = eeField.getFloat(fog);
        } catch (Exception ignored) {}

        System.err.printf("[dx12-p32] FogRenderer.updateBuffer: renderDistStart=%.1f renderDistEnd=%.1f envStart=%.1f envEnd=%.1f backend=%s%n",
                rs, re, es, ee, Dx12Device.isInitialized() ? "DX12" : "NONE");
        System.err.flush();
    }
}
