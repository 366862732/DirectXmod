package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Device;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P32 diagnostic: logs Camera depthFar and renderDistance every 60 frames.
 * Confirms the far plane alignment between Camera and FogRenderer.
 */
@Mixin(Camera.class)
public class CameraUpdateDebugMixin {

    private int dx12_cameraUpdateCount = 0;

    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private void dx12_cameraUpdateDebug(DeltaTracker deltaTracker, CallbackInfo ci) {
        dx12_cameraUpdateCount++;
        // P54 fix：原条件 `% 60 != 0 && <= 5` 逻辑反了——count>5 之后恒为 true，
        // 导致每帧都打印（+ 每帧反射），成为 System.err 同步 I/O 热点。
        // 改为「前 5 帧 + 之后每 60 帧」。
        if (dx12_cameraUpdateCount > 5 && dx12_cameraUpdateCount % 60 != 0) return;

        float depthFar;
        int renderDistance;
        try {
            java.lang.reflect.Field dfField = Camera.class.getDeclaredField("depthFar");
            dfField.setAccessible(true);
            depthFar = dfField.getFloat(this);
            java.lang.reflect.Field mcField = Camera.class.getDeclaredField("minecraft");
            mcField.setAccessible(true);
            Object mc = mcField.get(this);
            if (mc != null) {
                java.lang.reflect.Field optField = mc.getClass().getDeclaredField("options");
                optField.setAccessible(true);
                Object opts = optField.get(mc);
                if (opts != null) {
                    java.lang.reflect.Method erdMethod = opts.getClass().getMethod("getEffectiveRenderDistance");
                    renderDistance = ((Number) erdMethod.invoke(opts)).intValue();
                } else {
                    renderDistance = -1;
                }
            } else {
                renderDistance = -1;
            }
        } catch (Exception e) {
            depthFar = -1f;
            renderDistance = -1;
        }

        System.err.printf("[dx12-p32] Camera.update: depthFar=%.1f renderDistance(chunks)=%d depthFar/farPlaneRatio=%.1f backend=%s%n",
                depthFar, renderDistance,
                renderDistance > 0 ? depthFar / (float)(renderDistance * 16) : -1.0f,
                Dx12Device.isInitialized() ? "DX12" : "NONE");
        System.err.flush();
    }
}
