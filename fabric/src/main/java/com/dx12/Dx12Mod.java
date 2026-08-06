package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GL4DX12 - Fabric client entry point.
 *
 * The wgpu-based overlay renderer (Plan C) has been archived and removed.
 * The current architecture hooks into the vanilla GPU backend loop instead:
 *
 *   PreferredGraphicsApiMixin  →  getBackendsToTry() includes Dx12Backend
 *   Dx12Backend (GpuBackend)   →  vanilla calls createDevice(window, ...)
 *   Dx12Native (JNI)           →  talks to the dx12-mc Rust D3D12 layer
 *
 * All renderer work happens inside the vanilla backend lifecycle, so this
 * class only needs to exist as the Fabric entry point.
 */
public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 initializing");
        LOGGER.info("DX12 backend is registered via PreferredGraphicsApiMixin; "
            + "creation is driven by the vanilla backend-selection loop");
    }
}
