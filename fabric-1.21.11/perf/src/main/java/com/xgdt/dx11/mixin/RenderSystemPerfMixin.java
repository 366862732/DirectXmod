package com.xgdt.dx11.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx11.config.Dx11Config;
import com.xgdt.dx11.perf.PerfDebugLog;
import com.xgdt.dx11.perf.immediate.BufferPool;
import com.xgdt.dx11.perf.immediate.sign.SignTextCache;
import com.xgdt.dx11.perf.overlay.PerfOverlayStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Per-frame plumbing for the optional performance layer.
 *
 * <p>This lives in the performance jar, not in the core rendering mod, so the
 * core jar can run standalone.  The frame boundary releases pooled immediate
 * buffers, advances the overlay timers and drains the optional per-second
 * counter log.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(RenderSystem.class)
public class RenderSystemPerfMixin {
    /**
     * Frame boundary for the ImmediatelyFast-style buffer pool.  A feature
     * renderer that throws between borrow and release would otherwise leave a
     * native segment pinned for the session.
     */
    @Inject(method = "flipFrame", at = @At("HEAD"))
    private static void dx11$endImmediateFrame(CallbackInfo ci) {
        PerfDebugLog.maybeLog();
        PerfOverlayStats.onFrameBoundary();
        // Sign-text atlas maintenance also runs on the GL validation path (the
        // forced-immediate smoke test); only the buffer pool is backend-bound.
        // endFrame() must run even after the flag is turned off: that is what
        // releases an atlas that was created while the feature was enabled.
        if (Dx11Config.shouldUseDx11()) {
            BufferPool.endFrame();
        }
        SignTextCache.endFrame();
    }
}
