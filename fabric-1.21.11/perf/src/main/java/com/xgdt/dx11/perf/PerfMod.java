package com.xgdt.dx11.perf;

import net.fabricmc.api.ClientModInitializer;

/**
 * Entry point for the optional DirectX performance jar.
 *
 * <p>The core {@code gl4dx11} mod owns the D3D11 device and works without this
 * jar.  This entry point only initialises the performance layer, so the feature
 * can be removed by deleting one jar.</p>
 */
public final class PerfMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Load the config even when the D3D11 backend is off, so /dx11perf can
        // explain why the performance layer is inactive.
        PerfState.init();
    }
}
