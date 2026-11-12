package com.xgdt.dx11;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xgdt.dx11.config.Dx11Config;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the additive Minecraft 1.21.11 / D3D11 backend.
 *
 * <p>This module is deliberately independent from the 26.2 / D3D12 module. It
 * only activates when the user explicitly enables the backend and the D3D11
 * probe succeeds; otherwise the game uses the vanilla OpenGL path.</p>
 */
public final class Dx11Mod implements ClientModInitializer {
    public static final String MOD_ID = "gl4dx11";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Fully opt-in: do not touch Dx11Native (and therefore do not load
        // dx11_mc.dll) unless the user asked for the backend.
        if (!Boolean.getBoolean(Dx11Config.ENABLE_PROPERTY)) {
            LOGGER.info(
                "[dx11] D3D11 backend is opt-in; set -D{}={} to enable it.",
                Dx11Config.ENABLE_PROPERTY,
                "true");
            return;
        }

        if (!Dx11Config.isWindows()) {
            LOGGER.info("[dx11] Not running on Windows; D3D11 backend disabled.");
            return;
        }
        if (!Dx11Config.isDx11Requested()) {
            LOGGER.info("[dx11] Backend '{}' does not select D3D11.", Dx11Config.backend());
            return;
        }

        VertexFormat lineFormat = DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH;
        LOGGER.info(
            "[dx11] POSITION_COLOR_NORMAL_LINE_WIDTH stride={} lineWidthOffset={}",
            lineFormat.getVertexSize(),
            lineFormat.getOffset(VertexFormatElement.LINE_WIDTH));

        Dx11Config.ensureProbe();
        String result = Dx11Config.probeResult();
        if (result.startsWith("OK")) {
            LOGGER.info("[dx11] Probe: {}", result);
            LOGGER.info("[dx11] D3D11 backend selected.");
        } else {
            LOGGER.warn("[dx11] Probe: {}", result);
            LOGGER.warn("[dx11] D3D11 probe failed; using the vanilla OpenGL path.");
        }
    }
}
