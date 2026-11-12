package com.xgdt.dx11;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xgdt.dx11.config.Dx11Config;
import com.xgdt.dx11.dx11.Dx11Native;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the additive Minecraft 1.21.11 / D3D11 backend.
 *
 * <p>This module is deliberately independent from the 26.2 / D3D12 module. It
 * only activates when the D3D11 probe succeeds and the user explicitly enables
 * the backend. Until the backend is fully implemented the game continues to
 * use the vanilla OpenGL path.</p>
 */
public final class Dx11Mod implements ClientModInitializer {
    public static final String MOD_ID = "gl4dx11";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean(Dx11Config.ENABLE_PROPERTY)) {
            VertexFormat lineFormat = DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH;
            LOGGER.info(
                "[dx11] POSITION_COLOR_NORMAL_LINE_WIDTH stride={} lineWidthOffset={}",
                lineFormat.getVertexSize(),
                lineFormat.getOffset(VertexFormatElement.LINE_WIDTH));
        }

        if (!Dx11Config.isWindows()) {
            LOGGER.info("[dx11] Not running on Windows; D3D11 backend disabled.");
            return;
        }

        Dx11Config.ensureProbe();
        String result = Dx11Config.probeResult();
        if (result.startsWith("OK")) {
            LOGGER.info("[dx11] Probe: {}", result);
        } else {
            LOGGER.warn("[dx11] Probe: {}", result);
        }

        if (Dx11Config.shouldUseDx11()) {
            LOGGER.info("[dx11] D3D11 backend selected.");
        } else if (!Dx11Native.isLoaded()) {
            LOGGER.warn("[dx11] Native dx11_mc.dll was not loaded; using OpenGL.");
        } else {
            LOGGER.info("[dx11] Using vanilla OpenGL path.");
        }
    }
}
